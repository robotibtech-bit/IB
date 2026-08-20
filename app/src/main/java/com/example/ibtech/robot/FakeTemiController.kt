package com.example.ibtech.robot

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** `goTo` 호출이 어떤 결과로 끝날지. 개발자 메뉴([FakeTemiController.nextOutcome])가 갈아 끼운다. */
enum class FakeOutcome { SUCCESS, FAILED, EXTERNAL_INTERRUPTION }

/**
 * 실기 없이 동행 흐름을 개발·테스트하기 위한 [TemiController] 가짜 구현 (로드맵 5단계).
 *
 * [TemiControllerProvider]가 개발자 메뉴 설정에 따라 [TemiRepository] 대신 이 클래스를
 * 돌려준다. 실제 SDK 콜백처럼 항상 비동기(코루틴 지연)로 상태가 바뀌게 만들어, 화면 쪽 코드가
 * "즉시 값이 바뀌는" 가짜 구현에만 맞는 타이밍 버그를 갖지 않도록 한다.
 */
class FakeTemiController(
    initialLocations: List<String> = emptyList()
) : TemiController {

    var nextOutcome: FakeOutcome = FakeOutcome.SUCCESS

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var movementJob: Job? = null
    private var stopRequested = false

    private val _connectionState = MutableStateFlow<TemiConnectionState>(TemiConnectionState.Ready)
    override val connectionState: StateFlow<TemiConnectionState> = _connectionState.asStateFlow()

    private val _navigationState = MutableStateFlow<NavigationState>(NavigationState.Idle)
    override val navigationState: StateFlow<NavigationState> = _navigationState.asStateFlow()

    private val _speechState = MutableStateFlow<SpeechState>(SpeechState.Idle)
    override val speechState: StateFlow<SpeechState> = _speechState.asStateFlow()

    private val _locations = MutableStateFlow(initialLocations)
    override val locations: StateFlow<List<String>> = _locations.asStateFlow()

    private val _permissionStatus = MutableStateFlow(
        TemiPermissionStatus(
            granted = setOf(TemiFeaturePermission.MAP, TemiFeaturePermission.SETTINGS),
            checked = true
        )
    )

    /** 개발자 메뉴/테스트에서 확인할 수 있도록 마지막으로 적용된 값을 기억한다. */
    var lastAppliedVolume: Int? = null
        private set
    var lastAppliedVolumeLocked: Boolean = false
        private set
    override val permissionStatus: StateFlow<TemiPermissionStatus> = _permissionStatus.asStateFlow()

    private val _batteryStatus =
        MutableStateFlow<BatteryStatus?>(BatteryStatus(percentage = 80, isCharging = false))
    override val batteryStatus: StateFlow<BatteryStatus?> = _batteryStatus.asStateFlow()

    private val _sdkErrors = MutableSharedFlow<Int>(replay = 0, extraBufferCapacity = 8)
    override val sdkErrors: SharedFlow<Int> = _sdkErrors.asSharedFlow()

    private val _listeningState = MutableStateFlow<ListeningState>(ListeningState.Idle)
    override val listeningState: StateFlow<ListeningState> = _listeningState.asStateFlow()

    private val _asrResults = MutableSharedFlow<String>(replay = 0, extraBufferCapacity = 4)
    override val asrResults: SharedFlow<String> = _asrResults.asSharedFlow()

    /**
     * [askQuestion] 이 흘려보낼 가짜 인식 결과. 개발자 메뉴/테스트에서 갈아 끼운다.
     * null 이면 "못 알아들었다"로 처리해 아무 결과도 흘리지 않는다.
     */
    var nextAsrResult: String? = "공룡 나오는 책"

    /** 개발자 메뉴가 시설 목록 동기화에 쓸 가짜 POI를 채운다. */
    fun setLocations(locations: List<String>) {
        _locations.value = locations
    }

    override fun goTo(location: String): Boolean {
        if (_navigationState.value.isBusy) return false
        stopRequested = false
        _navigationState.value = NavigationState.Requested(location)

        movementJob?.cancel()
        movementJob = scope.launch {
            delay(MOVING_DELAY_MILLIS)
            if (stopRequested) return@launch
            _navigationState.value = NavigationState.Moving(location, "going")

            delay(OUTCOME_DELAY_MILLIS)
            if (stopRequested) return@launch

            _navigationState.value = when (nextOutcome) {
                FakeOutcome.SUCCESS -> NavigationState.Arrived(location)
                FakeOutcome.FAILED -> NavigationState.Failed(
                    target = location,
                    issue = NavigationIssue.SDK_ERROR,
                    code = FAKE_FAILURE_CODE,
                    sdkMessage = "fake failure"
                )
                FakeOutcome.EXTERNAL_INTERRUPTION -> NavigationState.Interrupted(
                    target = location,
                    issue = NavigationIssue.EXTERNAL_INTERRUPTION
                )
            }
        }
        return true
    }

    override fun stopMovement(): Boolean {
        if (!_navigationState.value.isBusy) return false
        stopRequested = true
        movementJob?.cancel()
        val target = _navigationState.value.targetOrNull.orEmpty()
        _navigationState.value = NavigationState.Interrupted(target, NavigationIssue.USER_STOPPED)
        return true
    }

    override fun speak(text: String, showOnScreen: Boolean): Boolean {
        _speechState.value = SpeechState.Speaking(text)
        scope.launch {
            delay(SPEECH_DELAY_MILLIS)
            if (_speechState.value == SpeechState.Speaking(text)) {
                _speechState.value = SpeechState.Idle
            }
        }
        return true
    }

    override fun cancelSpeech(): Boolean {
        _speechState.value = SpeechState.Idle
        return true
    }

    /**
     * 실기의 대화 흐름을 흉내낸다: 질문 발화 → 듣기 → 결과.
     * 실제 SDK 처럼 항상 비동기로 상태를 바꿔, 화면이 "즉시 결과가 오는" 타이밍에만
     * 맞는 버그를 갖지 않게 한다.
     */
    private var conversationJob: Job? = null

    override fun askQuestion(question: String): Boolean {
        conversationJob?.cancel()
        _listeningState.value = ListeningState.Speaking
        conversationJob = scope.launch {
            delay(SPEECH_DELAY_MILLIS)
            _listeningState.value = ListeningState.Listening
            delay(LISTEN_DELAY_MILLIS)
            _listeningState.value = ListeningState.Thinking
            delay(SPEECH_DELAY_MILLIS)
            _listeningState.value = ListeningState.Idle
            nextAsrResult?.takeIf { it.isNotBlank() }?.let { _asrResults.emit(it) }
        }
        return true
    }

    override fun finishConversation(): Boolean {
        conversationJob?.cancel()
        _listeningState.value = ListeningState.Idle
        return true
    }

    override fun refreshLocations(): Boolean = true

    override fun refreshBattery(): Boolean = true

    override fun refreshPermissions(): Boolean = true

    override fun requestMissingPermissions(): Boolean = false

    override fun applyVolumeSettings(volume: Int, locked: Boolean): Boolean {
        lastAppliedVolume = volume
        lastAppliedVolumeLocked = locked
        return true
    }

    override fun reportNavigationTimeout(target: String) {
        val current = _navigationState.value
        if (current is NavigationState.Requested && current.target == target) {
            _navigationState.value = NavigationState.Failed(
                target = target,
                issue = NavigationIssue.TIMEOUT,
                code = FAKE_FAILURE_CODE,
                sdkMessage = "fake timeout"
            )
        }
    }

    override fun reportStopTimeout() {
        val current = _navigationState.value
        if (stopRequested && current.isBusy) {
            stopRequested = false
            _navigationState.value = NavigationState.Interrupted(
                target = current.targetOrNull.orEmpty(),
                issue = NavigationIssue.USER_STOPPED
            )
        }
    }

    override fun reportPermissionRequestTimeout() {
        _permissionStatus.update { it.copy(requestInFlight = false) }
    }

    override fun consumeNavigationResult() {
        if (!_navigationState.value.isBusy) {
            _navigationState.value = NavigationState.Idle
        }
    }

    override fun consumeSpeechResult() {
        if (_speechState.value is SpeechState.Failed) {
            _speechState.value = SpeechState.Idle
        }
    }

    companion object {
        private const val MOVING_DELAY_MILLIS = 400L
        private const val OUTCOME_DELAY_MILLIS = 1_600L
        private const val SPEECH_DELAY_MILLIS = 800L

        /** 사용자가 말하는 데 걸리는 시간. 실기의 듣기 구간을 흉내내기 위한 값이다. */
        private const val LISTEN_DELAY_MILLIS = 1_200L
        private const val FAKE_FAILURE_CODE = -100
    }
}
