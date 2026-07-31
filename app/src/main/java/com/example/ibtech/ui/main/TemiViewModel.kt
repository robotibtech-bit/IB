package com.example.ibtech.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ibtech.data.temi.NavigationState
import com.example.ibtech.data.temi.SpeechState
import com.example.ibtech.data.temi.TemiController
import com.example.ibtech.data.temi.TemiRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * temi 이동/발화 명령과 상태를 화면에 연결하는 ViewModel.
 *
 * 버튼은 상태를 직접 확정하지 않고 명령만 발생시킨다. 상태 전환은 SDK 콜백에서만 일어나며,
 * 콜백이 오지 않는 경우를 대비해 타임아웃 폴백을 건다.
 *
 * 실패는 두 갈래로 나눠 다룬다.
 * - 지속되는 상황은 [uiState] 로 (예: 권한 없음 → 카드 표시, 버튼 비활성)
 * - 한 번만 알리면 되는 사건은 [alerts] 로 (예: 이동 실패 → 스낵바)
 */
class TemiViewModel(
    private val repository: TemiController = TemiRepository.getInstance()
) : ViewModel() {

    /** 흐름이 6개라 타입 안전한 combine 오버로드(최대 5개)를 쓰려고 두 단계로 나눈다. */
    private val robotStatus = combine(
        repository.connectionState,
        repository.navigationState,
        repository.speechState
    ) { connection, navigation, speech -> Triple(connection, navigation, speech) }

    val uiState: StateFlow<TemiUiState> = combine(
        robotStatus,
        repository.locations,
        repository.permissionStatus,
        repository.batteryStatus
    ) { status, locations, permissions, battery ->
        TemiUiState(
            connection = status.first,
            navigation = status.second,
            speech = status.third,
            locations = locations,
            permissions = permissions,
            battery = battery
        )
    }.stateIn(
        scope = viewModelScope,
        // ViewModel 이 명령을 거를 때 uiState.value 를 직접 읽으므로 항상 최신이어야 한다.
        // WhileSubscribed 를 쓰면 화면 구독 전에는 초기값이 남아 잘못된 판단을 하게 된다.
        started = SharingStarted.Eagerly,
        initialValue = TemiUiState()
    )

    private val _alerts = MutableSharedFlow<UiAlert>(replay = 0, extraBufferCapacity = 8)

    /** 한 번만 보여줄 안내. 화면이 없을 때 발생한 안내는 버려진다. */
    val alerts: SharedFlow<UiAlert> = _alerts.asSharedFlow()

    private var navigationTimeoutJob: Job? = null
    private var permissionTimeoutJob: Job? = null

    /** 자동 권한 요청은 프로세스당 한 번만. 거부한 사용자에게 반복해서 묻지 않는다. */
    private var hasAutoRequestedPermissions = false

    init {
        observeNavigationResults()
        observeSpeechResults()
        observePermissionResults()
        observeSdkErrors()
    }

    /** 권한 요청이 끝났는데도 승인되지 않았으면 거부로 보고 한 번 안내한다. */
    private fun observePermissionResults() {
        viewModelScope.launch {
            var wasInFlight = false
            repository.permissionStatus.collect { status ->
                if (wasInFlight && !status.requestInFlight && !status.allGranted) {
                    _alerts.tryEmit(UiAlert.PermissionDenied)
                }
                wasInFlight = status.requestInFlight
            }
        }
    }

    /**
     * 이동이 끝난 순간(도착·중단·실패)을 안내로 바꾸고 상태를 대기로 되돌린다.
     *
     * 상태를 곧바로 비우기 때문에 같은 결과가 두 번 안내되지 않는다.
     */
    private fun observeNavigationResults() {
        viewModelScope.launch {
            repository.navigationState.collect { state ->
                when (state) {
                    is NavigationState.Arrived -> {
                        _alerts.tryEmit(UiAlert.Arrived(state.target))
                        repository.consumeNavigationResult()
                    }

                    is NavigationState.Interrupted -> {
                        _alerts.tryEmit(UiAlert.NavigationFailed(state.issue))
                        repository.consumeNavigationResult()
                    }

                    is NavigationState.Failed -> {
                        _alerts.tryEmit(UiAlert.NavigationFailed(state.issue))
                        repository.consumeNavigationResult()
                    }

                    // 진행 중 상태는 화면이 uiState 로 계속 보여준다.
                    is NavigationState.Requested,
                    is NavigationState.Moving,
                    NavigationState.Idle -> Unit
                }
            }
        }
    }

    private fun observeSpeechResults() {
        viewModelScope.launch {
            repository.speechState.collect { state ->
                if (state is SpeechState.Failed) {
                    _alerts.tryEmit(UiAlert.SpeechFailed)
                    repository.consumeSpeechResult()
                }
            }
        }
    }

    private fun observeSdkErrors() {
        viewModelScope.launch {
            repository.sdkErrors.collect { code ->
                _alerts.tryEmit(UiAlert.SdkError(code))
            }
        }
    }

    /**
     * [location] POI 로 이동을 요청한다.
     *
     * 보낼 수 없는 상황이면 이유를 [alerts] 로 알린다. 버튼이 비활성이어도
     * POI 목록 탭 등 다른 경로로 들어올 수 있으므로 여기서 다시 검사한다.
     *
     * @return 명령을 실제로 보냈으면 true.
     */
    fun goTo(location: String): Boolean {
        val state = uiState.value

        when {
            !state.isReady -> {
                _alerts.tryEmit(UiAlert.RobotNotReady)
                return false
            }

            !state.permissions.allGranted -> {
                _alerts.tryEmit(UiAlert.PermissionRequired)
                return false
            }

            !state.hasLocation(location) -> {
                _alerts.tryEmit(UiAlert.DestinationUnreachable(location))
                return false
            }

            // 이미 이동 중이면 조용히 무시한다. 중지 버튼으로만 벗어난다.
            state.isMoving -> return false
        }

        if (!repository.goTo(location)) {
            _alerts.tryEmit(UiAlert.RobotNotReady)
            return false
        }

        navigationTimeoutJob?.cancel()
        navigationTimeoutJob = viewModelScope.launch {
            delay(GO_TO_START_TIMEOUT_MS)
            // 여전히 Requested 상태로 남아 있을 때만 실패로 정리된다.
            repository.reportNavigationTimeout(location)
        }
        return true
    }

    /** 진행 중인 이동을 중지한다. */
    fun stopMovement(): Boolean {
        if (!uiState.value.isMoving) return false
        if (!repository.stopMovement()) {
            _alerts.tryEmit(UiAlert.RobotNotReady)
            return false
        }

        navigationTimeoutJob?.cancel()
        navigationTimeoutJob = viewModelScope.launch {
            delay(STOP_CONFIRM_TIMEOUT_MS)
            repository.reportStopTimeout()
        }
        return true
    }

    /** [text] 를 발화한다. */
    fun speak(text: String, showOnScreen: Boolean = false): Boolean {
        if (text.isBlank()) return false
        if (!uiState.value.isReady) {
            _alerts.tryEmit(UiAlert.RobotNotReady)
            return false
        }
        if (!repository.speak(text, showOnScreen)) {
            _alerts.tryEmit(UiAlert.SpeechFailed)
            return false
        }
        return true
    }

    /** 진행 중인 발화를 취소한다. */
    fun cancelSpeech(): Boolean = repository.cancelSpeech()

    /** temi 지도의 POI 목록을 다시 읽어온다. */
    fun refreshLocations(): Boolean {
        if (!uiState.value.isReady) {
            _alerts.tryEmit(UiAlert.RobotNotReady)
            return false
        }
        return repository.refreshLocations()
    }

    /**
     * 준비가 끝난 뒤 한 번만 자동으로 권한을 요청한다.
     *
     * 사용자가 거부했을 때 요청이 반복되지 않도록 프로세스당 1회로 제한하고,
     * 이후에는 화면의 '권한 허용' 버튼([requestPermissions])으로만 다시 요청한다.
     */
    fun requestPermissionsOnce() {
        if (hasAutoRequestedPermissions) return
        val state = uiState.value
        if (!state.isReady || !state.permissions.checked) return

        hasAutoRequestedPermissions = true
        if (!state.permissions.allGranted) {
            requestPermissions()
        }
    }

    /**
     * 아직 승인되지 않은 권한을 요청한다. (자동 1회 요청과 화면의 '권한 허용' 버튼이 함께 쓴다.)
     *
     * 결과 콜백이 오지 않아도 요청 중 표시가 풀리도록 타임아웃 폴백을 건다.
     */
    fun requestPermissions(): Boolean {
        if (!uiState.value.isReady) {
            _alerts.tryEmit(UiAlert.RobotNotReady)
            return false
        }
        if (!repository.requestMissingPermissions()) {
            _alerts.tryEmit(UiAlert.RobotNotReady)
            return false
        }

        permissionTimeoutJob?.cancel()
        permissionTimeoutJob = viewModelScope.launch {
            delay(PERMISSION_RESULT_TIMEOUT_MS)
            repository.reportPermissionRequestTimeout()
        }
        return true
    }

    /** 승인 상태를 SDK 에 다시 조회한다. (설정 화면에서 바꾸고 돌아온 경우) */
    fun refreshPermissions(): Boolean = repository.refreshPermissions()

    override fun onCleared() {
        navigationTimeoutJob?.cancel()
        permissionTimeoutJob?.cancel()
        super.onCleared()
    }

    private companion object {
        /** 구독자가 사라진 뒤 상태 수집을 유지하는 시간(화면 회전 대비). */
        const val STOP_TIMEOUT_MS = 5_000L

        /** goTo 호출 후 첫 상태 콜백을 기다리는 한계. */
        const val GO_TO_START_TIMEOUT_MS = 10_000L

        /** stopMovement 후 최종 콜백을 기다리는 한계. */
        const val STOP_CONFIRM_TIMEOUT_MS = 5_000L

        /**
         * 권한 요청 결과를 기다리는 한계.
         * 사용자가 팝업을 읽고 누를 시간을 줘야 하므로 넉넉하게 잡는다.
         */
        const val PERMISSION_RESULT_TIMEOUT_MS = 60_000L
    }
}
