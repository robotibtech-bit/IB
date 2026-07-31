package com.example.ibtech.data.temi

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 테스트용 [TemiController].
 *
 * 실제 로봇 없이 SDK 콜백이 도착한 상황을 흉내 낸다.
 * 명령은 [commandsSucceed] 로 성공/실패를 바꿀 수 있고, 호출 내역은 [calls] 에 쌓인다.
 */
class FakeTemiController : TemiController {

    private val _connectionState = MutableStateFlow<TemiConnectionState>(TemiConnectionState.Connecting)
    override val connectionState: StateFlow<TemiConnectionState> = _connectionState.asStateFlow()

    private val _navigationState = MutableStateFlow<NavigationState>(NavigationState.Idle)
    override val navigationState: StateFlow<NavigationState> = _navigationState.asStateFlow()

    private val _speechState = MutableStateFlow<SpeechState>(SpeechState.Idle)
    override val speechState: StateFlow<SpeechState> = _speechState.asStateFlow()

    private val _locations = MutableStateFlow<List<String>>(emptyList())
    override val locations: StateFlow<List<String>> = _locations.asStateFlow()

    private val _permissionStatus = MutableStateFlow(TemiPermissionStatus())
    override val permissionStatus: StateFlow<TemiPermissionStatus> = _permissionStatus.asStateFlow()

    private val _batteryStatus = MutableStateFlow<BatteryStatus?>(null)
    override val batteryStatus: StateFlow<BatteryStatus?> = _batteryStatus.asStateFlow()

    private val _sdkErrors = MutableSharedFlow<Int>(replay = 0, extraBufferCapacity = 8)
    override val sdkErrors: SharedFlow<Int> = _sdkErrors.asSharedFlow()

    /** SDK 호출이 성공하는지. false 면 로봇이 준비되지 않은 상황을 흉내 낸다. */
    var commandsSucceed = true

    /** 호출된 명령 이름 목록. 순서까지 검증할 수 있다. */
    val calls = mutableListOf<String>()

    // ------------------------------------------------------------------
    // 테스트에서 상황을 만들기 위한 헬퍼
    // ------------------------------------------------------------------

    /** 준비 완료 + 지도 권한 승인 + POI 목록까지 갖춘 정상 상태로 만든다. */
    fun becomeReady(locations: List<String> = listOf("로비", "사무실")) {
        _connectionState.value = TemiConnectionState.Ready
        _permissionStatus.value = TemiPermissionStatus(
            granted = setOf(TemiFeaturePermission.MAP),
            checked = true
        )
        _locations.value = locations
    }

    /** 준비는 됐지만 권한만 없는 상태. */
    fun becomeReadyWithoutPermission(locations: List<String> = listOf("로비")) {
        _connectionState.value = TemiConnectionState.Ready
        _permissionStatus.value = TemiPermissionStatus(granted = emptySet(), checked = true)
        _locations.value = locations
    }

    fun emitNavigationState(state: NavigationState) {
        _navigationState.value = state
    }

    fun emitSpeechState(state: SpeechState) {
        _speechState.value = state
    }

    fun emitPermissionStatus(status: TemiPermissionStatus) {
        _permissionStatus.value = status
    }

    suspend fun emitSdkError(code: Int) {
        _sdkErrors.emit(code)
    }

    // ------------------------------------------------------------------
    // TemiController
    // ------------------------------------------------------------------

    override fun goTo(location: String): Boolean {
        calls += "goTo:$location"
        if (!commandsSucceed) return false
        _navigationState.value = NavigationState.Requested(location)
        return true
    }

    override fun stopMovement(): Boolean {
        calls += "stopMovement"
        return commandsSucceed
    }

    override fun speak(text: String, showOnScreen: Boolean): Boolean {
        calls += "speak:$text"
        return commandsSucceed
    }

    override fun cancelSpeech(): Boolean {
        calls += "cancelSpeech"
        return commandsSucceed
    }

    override fun refreshLocations(): Boolean {
        calls += "refreshLocations"
        return commandsSucceed
    }

    override fun refreshBattery(): Boolean {
        calls += "refreshBattery"
        return commandsSucceed
    }

    override fun refreshPermissions(): Boolean {
        calls += "refreshPermissions"
        return commandsSucceed
    }

    override fun requestMissingPermissions(): Boolean {
        calls += "requestMissingPermissions"
        if (!commandsSucceed) return false
        _permissionStatus.value = _permissionStatus.value.copy(requestInFlight = true)
        return true
    }

    override fun reportNavigationTimeout(target: String) {
        calls += "reportNavigationTimeout:$target"
        val current = _navigationState.value
        if (current is NavigationState.Requested && current.target == target) {
            _navigationState.value = NavigationState.Failed(
                target = target,
                issue = NavigationIssue.TIMEOUT,
                code = -1,
                sdkMessage = "timeout"
            )
        }
    }

    override fun reportPermissionRequestTimeout() {
        calls += "reportPermissionRequestTimeout"
        if (!_permissionStatus.value.requestInFlight) return
        _permissionStatus.value = _permissionStatus.value.copy(
            requestInFlight = false,
            checked = true
        )
    }

    override fun reportStopTimeout() {
        calls += "reportStopTimeout"
        val current = _navigationState.value
        if (current.isBusy) {
            _navigationState.value = NavigationState.Interrupted(
                target = current.targetOrNull.orEmpty(),
                issue = NavigationIssue.USER_STOPPED
            )
        }
    }

    override fun consumeNavigationResult() {
        calls += "consumeNavigationResult"
        if (!_navigationState.value.isBusy) {
            _navigationState.value = NavigationState.Idle
        }
    }

    override fun consumeSpeechResult() {
        calls += "consumeSpeechResult"
        if (_speechState.value is SpeechState.Failed) {
            _speechState.value = SpeechState.Idle
        }
    }
}
