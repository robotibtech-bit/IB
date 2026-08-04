package com.example.ibtech.robot

import android.app.Activity
import android.content.pm.PackageManager
import android.util.Log
import com.robotemi.sdk.BatteryData
import com.robotemi.sdk.Robot
import com.robotemi.sdk.TtsRequest
import com.robotemi.sdk.exception.OnSdkExceptionListener
import com.robotemi.sdk.exception.SdkException
import com.robotemi.sdk.listeners.OnBatteryStatusChangedListener
import com.robotemi.sdk.listeners.OnGoToLocationStatusChangedListener
import com.robotemi.sdk.listeners.OnLocationsUpdatedListener
import com.robotemi.sdk.listeners.OnRobotReadyListener
import com.robotemi.sdk.permission.OnRequestPermissionResultListener
import com.robotemi.sdk.permission.Permission
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.lang.ref.WeakReference
import java.util.UUID

/**
 * temi `Robot` 인스턴스와 SDK 리스너를 한곳에 캡슐화한 저장소.
 *
 * 앱의 다른 계층(ViewModel / Compose)은 이 클래스만 바라보며, `Robot` 을 직접 만지지 않는다.
 * 원칙:
 * - 모든 SDK 콜백은 여기서 받아 [StateFlow] 단일 상태로 정규화한다.
 * - `addListener` 와 `removeListener` 는 [onStart] / [onStop] 대칭 쌍으로만 호출한다.
 *   (중복 등록은 콜백 중복 처리·팝업 중복의 원인)
 * - 모든 SDK 호출 전에 인스턴스와 준비 상태를 확인한다.
 * - 사용자에게 보여줄 문구는 만들지 않는다. 사유는 [NavigationIssue] 로만 구분하고
 *   문구 매핑은 UI 계층이 `strings.xml` 로 처리한다.
 */
class TemiRepository private constructor() :
    TemiController,
    OnSdkExceptionListener,
    OnRobotReadyListener,
    OnGoToLocationStatusChangedListener,
    OnLocationsUpdatedListener,
    OnBatteryStatusChangedListener,
    OnRequestPermissionResultListener,
    Robot.TtsListener {

    /**
     * temi 가 아닌 기기에서는 인스턴스 획득 자체가 실패할 수 있으므로 null 을 허용한다.
     * 이후 모든 호출은 [withRobot] 를 통해서만 이뤄진다.
     */
    private val robot: Robot? = runCatching { Robot.getInstance() }
        .onFailure { Log.w(TAG, "temi SDK 인스턴스를 가져오지 못했습니다.", it) }
        .getOrNull()

    private val _connectionState = MutableStateFlow<TemiConnectionState>(
        if (robot == null) TemiConnectionState.Unavailable else TemiConnectionState.Connecting
    )

    /** SDK 서비스 연결 상태. */
    override val connectionState: StateFlow<TemiConnectionState> = _connectionState.asStateFlow()

    private val _navigationState = MutableStateFlow<NavigationState>(NavigationState.Idle)

    /** 정규화된 이동 상태. 이동 UI 의 단일 진실 공급원. */
    override val navigationState: StateFlow<NavigationState> = _navigationState.asStateFlow()

    private val _speechState = MutableStateFlow<SpeechState>(SpeechState.Idle)

    /** TTS 발화 상태. */
    override val speechState: StateFlow<SpeechState> = _speechState.asStateFlow()

    private val _locations = MutableStateFlow<List<String>>(emptyList())

    /** temi 지도에 저장된 POI(위치) 이름 목록. */
    override val locations: StateFlow<List<String>> = _locations.asStateFlow()

    private val _permissionStatus = MutableStateFlow(TemiPermissionStatus())

    /** temi 전용 권한 승인 현황. */
    override val permissionStatus: StateFlow<TemiPermissionStatus> = _permissionStatus.asStateFlow()

    private val _batteryStatus = MutableStateFlow<BatteryStatus?>(null)

    /** 배터리 잔량. 아직 값을 받지 못했으면 null. */
    override val batteryStatus: StateFlow<BatteryStatus?> = _batteryStatus.asStateFlow()

    // 오류는 상태가 아니라 1회성 사건이므로 SharedFlow 로 흘린다.
    // 구독자가 없을 때 버려지도록 replay = 0.
    private val _sdkErrors = MutableSharedFlow<Int>(replay = 0, extraBufferCapacity = 8)

    /** SDK 가 보고한 오류 코드. */
    override val sdkErrors: SharedFlow<Int> = _sdkErrors.asSharedFlow()

    private var hostActivity: WeakReference<Activity>? = null
    private var listenersRegistered = false

    /** 사용자가 [stopMovement] 로 직접 멈춘 것인지 구분하기 위한 플래그. */
    private var stopRequested = false

    /** 우리가 발행한 TTS 만 상태에 반영하기 위한 요청 ID. */
    private var pendingTtsId: UUID? = null

    /** SDK 가 준비된 상태인지 여부. */
    val isReady: Boolean
        get() = _connectionState.value is TemiConnectionState.Ready

    // ---------------------------------------------------------------------
    // 생명주기 — Activity 의 onStart / onStop 에서 반드시 쌍으로 호출한다.
    // ---------------------------------------------------------------------

    fun onStart(activity: Activity) {
        val robot = this.robot ?: return
        if (listenersRegistered) return

        hostActivity = WeakReference(activity)
        robot.addOnRobotReadyListener(this)
        robot.addOnGoToLocationStatusChangedListener(this)
        robot.addOnLocationsUpdatedListener(this)
        robot.addOnBatteryStatusChangedListener(this)
        robot.addOnRequestPermissionResultListener(this)
        robot.addOnSdkExceptionListener(this)
        robot.addTtsListener(this)
        listenersRegistered = true
    }

    fun onStop() {
        val robot = this.robot ?: return
        if (!listenersRegistered) return

        robot.removeTtsListener(this)
        robot.removeOnSdkExceptionListener(this)
        robot.removeOnRequestPermissionResultListener(this)
        robot.removeOnBatteryStatusChangedListener(this)
        // SDK 메서드명이 add 는 `Updated`, remove 는 `Update` 로 비대칭이다. 오타 아님.
        robot.removeOnLocationsUpdateListener(this)
        robot.removeOnGoToLocationStatusChangedListener(this)
        robot.removeOnRobotReadyListener(this)
        listenersRegistered = false
        hostActivity = null
    }

    // ---------------------------------------------------------------------
    // SDK 콜백
    // ---------------------------------------------------------------------

    override fun onRobotReady(isReady: Boolean) {
        if (!isReady) {
            _connectionState.value = TemiConnectionState.Connecting
            return
        }

        val activity = hostActivity?.get()
        if (activity == null) {
            Log.w(TAG, "onRobotReady 시점에 Activity 가 없어 onStart 를 건너뜁니다.")
            return
        }

        // Launcher 에 현재 스킬 화면을 알린다. meta-data(SKILL) 를 함께 전달해야 한다.
        runCatching {
            val info = activity.packageManager.getActivityInfo(
                activity.componentName,
                PackageManager.GET_META_DATA
            )
            robot?.onStart(info)
        }.onFailure { Log.e(TAG, "robot.onStart 실패", it) }

        _connectionState.value = TemiConnectionState.Ready
        verifyPermissionDeclaration(activity)
        refreshPermissions()
        refreshLocations()
        refreshBattery()

        // 시설 목록이 빈 화면으로 보일 때 원인이 "POI 없음"인지 "MAP 권한 미승인"인지를
        // 화면 상태만으로는 구분할 수 없어 연결 시점에 한 번 요약해 남긴다.
        Log.i(
            TAG,
            "temi 연결 완료. locations=${_locations.value.size}개 " +
                "${_locations.value}, mapPermissionGranted=${_permissionStatus.value.granted}"
        )
    }

    /**
     * Manifest 선언을 진단한다.
     *
     * temi 권한을 `<uses-permission>` 으로 선언하면 Launcher 가 인식하지 못해
     * `requestPermissions()` 를 불러도 팝업이 뜨지 않고 조용히 무시된다.
     * 원인을 찾기 어려운 실패라 준비 시점에 한 번 확인하고 로그를 남긴다.
     */
    private fun verifyPermissionDeclaration(activity: Activity) {
        val declared = runCatching {
            activity.packageManager
                .getApplicationInfo(activity.packageName, PackageManager.GET_META_DATA)
                .metaData
                ?.getString(METADATA_PERMISSIONS)
        }.getOrNull()

        if (declared.isNullOrBlank()) {
            Log.e(
                TAG,
                "AndroidManifest 의 <application> 에 $METADATA_PERMISSIONS meta-data 가 없습니다. " +
                    "권한 요청 팝업이 뜨지 않습니다."
            )
            return
        }

        TemiFeaturePermission.entries
            .filterNot { declared.contains(it.toSdk().value) }
            .forEach {
                Log.e(TAG, "${it.name} 권한이 manifest 에 선언되지 않았습니다. declared=$declared")
            }
    }

    override fun onSdkError(sdkException: SdkException) {
        Log.e(TAG, "SDK error code=${sdkException.code} message=${sdkException.message}")

        // 권한 거부로 실패했다면 승인 현황을 다시 확인해 화면 안내를 맞춘다.
        if (sdkException.code == SdkException.CODE_PERMISSION_DENIED) {
            refreshPermissions()
        }
        _sdkErrors.tryEmit(sdkException.code)
    }

    override fun onBatteryStatusChanged(batteryData: BatteryData?) {
        batteryData ?: return
        _batteryStatus.value = BatteryStatus(
            // Kotlin 프로퍼티명은 level (JVM 에는 getBatteryPercentage() 로 노출된다).
            percentage = batteryData.level,
            isCharging = batteryData.isCharging
        )
    }

    override fun onRequestPermissionResult(
        permission: Permission,
        grantResult: Int,
        requestCode: Int
    ) {
        Log.d(TAG, "permission=${permission.name} result=$grantResult code=$requestCode")

        val feature = permission.toFeature()
        if (feature == null) {
            // 우리가 요청하지 않은 권한 결과는 무시한다.
            _permissionStatus.update { it.copy(requestInFlight = false) }
            return
        }

        _permissionStatus.update { current ->
            val granted = if (grantResult == Permission.GRANTED) {
                current.granted + feature
            } else {
                current.granted - feature
            }
            current.copy(granted = granted, requestInFlight = false, checked = true)
        }

        // 지도 권한을 새로 받았다면 POI 목록을 다시 읽는다.
        if (feature == TemiFeaturePermission.MAP && grantResult == Permission.GRANTED) {
            refreshLocations()
        }
    }

    override fun onGoToLocationStatusChanged(
        location: String,
        status: String,
        descriptionId: Int,
        description: String
    ) {
        // 진단을 위해 원시 상태 코드를 그대로 남긴다.
        Log.d(TAG, "goTo status=$status location=$location id=$descriptionId desc=$description")

        _navigationState.value = when (status.lowercase()) {
            STATUS_START, STATUS_CALCULATING, STATUS_GOING ->
                NavigationState.Moving(location, status)

            STATUS_COMPLETE -> {
                stopRequested = false
                NavigationState.Arrived(location)
            }

            STATUS_ABORT -> {
                // 사용자가 직접 멈춘 경우와 외부 개입을 구분한다.
                val issue = if (stopRequested) {
                    NavigationIssue.USER_STOPPED
                } else {
                    NavigationIssue.EXTERNAL_INTERRUPTION
                }
                stopRequested = false
                NavigationState.Interrupted(location, issue)
            }

            else -> {
                stopRequested = false
                NavigationState.Failed(
                    target = location,
                    issue = NavigationIssue.SDK_ERROR,
                    code = descriptionId,
                    sdkMessage = description
                )
            }
        }
    }

    override fun onLocationsUpdated(locations: List<String>) {
        _locations.value = locations
    }

    override fun onTtsStatusChanged(ttsRequest: TtsRequest) {
        // 다른 화면·다른 앱이 발행한 TTS 상태는 무시한다.
        if (ttsRequest.id != pendingTtsId) return

        _speechState.value = when (ttsRequest.status) {
            TtsRequest.Status.PENDING,
            TtsRequest.Status.PROCESSING,
            TtsRequest.Status.STARTED -> SpeechState.Speaking(ttsRequest.speech)

            TtsRequest.Status.COMPLETED,
            TtsRequest.Status.CANCELED -> {
                pendingTtsId = null
                SpeechState.Idle
            }

            TtsRequest.Status.ERROR,
            TtsRequest.Status.NOT_ALLOWED -> {
                pendingTtsId = null
                SpeechState.Failed(ttsRequest.speech, ttsRequest.status.name)
            }
        }
    }

    // ---------------------------------------------------------------------
    // 명령 — 모두 준비 상태를 확인한 뒤 호출한다.
    // ---------------------------------------------------------------------

    /** 저장된 POI 로 이동을 요청한다. 실제 이동 여부는 상태 콜백으로만 확정된다. */
    override fun goTo(location: String): Boolean = withRobot { robot ->
        stopRequested = false
        _navigationState.value = NavigationState.Requested(location)
        robot.goTo(location)
    }

    /** 진행 중인 모든 이동을 중지한다. 최종 상태는 콜백에서 정리된다. */
    override fun stopMovement(): Boolean = withRobot { robot ->
        stopRequested = true
        robot.stopMovement()
    }

    /** temi 가 [text] 를 발화한다. */
    // 기본값은 인터페이스(TemiController)에만 둔다.
    override fun speak(text: String, showOnScreen: Boolean): Boolean = withRobot { robot ->
        val request = TtsRequest.create(
            speech = text,
            isShowOnConversationLayer = showOnScreen
        )
        pendingTtsId = request.id
        _speechState.value = SpeechState.Speaking(text)
        robot.speak(request)
    }

    /** 진행 중인 TTS 와 대기 큐를 모두 취소한다. */
    override fun cancelSpeech(): Boolean = withRobot { robot ->
        robot.cancelAllTtsRequests()
        pendingTtsId = null
        _speechState.value = SpeechState.Idle
    }

    /** temi 지도의 POI 목록을 다시 읽어온다. */
    override fun refreshLocations(): Boolean = withRobot { robot ->
        _locations.value = robot.locations
    }

    /** 배터리 잔량을 즉시 조회한다. 이후 변화는 리스너로 갱신된다. */
    override fun refreshBattery(): Boolean = withRobot { robot ->
        robot.batteryData?.let {
            _batteryStatus.value = BatteryStatus(it.level, it.isCharging)
        }
    }

    // ---------------------------------------------------------------------
    // 권한 — 선언만으로는 부족하고 사용자가 실행 중에 승인해야 한다.
    // ---------------------------------------------------------------------

    /** 현재 승인 상태를 SDK 에 직접 조회해 갱신한다. */
    override fun refreshPermissions(): Boolean = withRobot { robot ->
        val granted = queryGrantedPermissions(robot)
        _permissionStatus.update {
            it.copy(granted = granted, checked = true)
        }
    }

    /**
     * 권한 요청 결과 콜백이 오지 않을 때의 폴백.
     *
     * 이게 없으면 `requestInFlight` 가 계속 true 로 남아 화면의 '권한 허용' 버튼이 영구히 잠긴다.
     * 실제 승인 상태를 다시 읽어 반영하므로, 사용자가 승인했는데 콜백만 유실된 경우도 복구된다.
     */
    override fun reportPermissionRequestTimeout() {
        if (!_permissionStatus.value.requestInFlight) return
        Log.w(TAG, "권한 요청 결과 콜백 타임아웃. 승인 상태를 다시 조회합니다.")

        val robot = this.robot
        val granted = if (robot != null && isReady) {
            queryGrantedPermissions(robot)
        } else {
            _permissionStatus.value.granted
        }
        _permissionStatus.update {
            it.copy(granted = granted, requestInFlight = false, checked = true)
        }
    }

    private fun queryGrantedPermissions(robot: Robot): Set<TemiFeaturePermission> =
        TemiFeaturePermission.entries
            .filter {
                runCatching { robot.checkSelfPermission(it.toSdk()) == Permission.GRANTED }
                    .getOrDefault(false)
            }
            .toSet()

    /**
     * 아직 승인되지 않은 권한을 요청한다.
     * 결과는 [onRequestPermissionResult] 로만 확정되며, 다이얼로그 표시 자체는 승인이 아니다.
     *
     * @return 요청을 보냈으면 true. 이미 전부 승인됐거나 요청이 진행 중이면 false.
     */
    override fun requestMissingPermissions(): Boolean {
        val status = _permissionStatus.value
        if (status.requestInFlight) return false

        val missing = status.missing
        if (missing.isEmpty()) return false

        val sent = withRobot { robot ->
            robot.requestPermissions(missing.map { it.toSdk() }, PERMISSION_REQUEST_CODE)
        }
        if (sent) {
            _permissionStatus.update { it.copy(requestInFlight = true) }
        }
        return sent
    }

    private fun TemiFeaturePermission.toSdk(): Permission = when (this) {
        TemiFeaturePermission.MAP -> Permission.MAP
    }

    private fun Permission.toFeature(): TemiFeaturePermission? = when (this) {
        Permission.MAP -> TemiFeaturePermission.MAP
        else -> null
    }

    // ---------------------------------------------------------------------
    // 타임아웃 폴백 — SDK 호출 성공이 곧 주행 시작은 아니므로 콜백 누락에 대비한다.
    // ---------------------------------------------------------------------

    /** `goTo` 후 상태 콜백이 오지 않은 채 [target] 요청 상태로 남아 있으면 실패로 정리한다. */
    override fun reportNavigationTimeout(target: String) {
        val current = _navigationState.value
        if (current is NavigationState.Requested && current.target == target) {
            Log.w(TAG, "goTo 상태 콜백 타임아웃. target=$target")
            _navigationState.value = NavigationState.Failed(
                target = target,
                issue = NavigationIssue.TIMEOUT,
                code = CODE_NO_SDK_RESPONSE,
                sdkMessage = "no goTo status callback"
            )
        }
    }

    /** `stopMovement` 후 최종 콜백이 오지 않으면 UI 가 영구 '이동 중' 에 남지 않도록 정리한다. */
    override fun reportStopTimeout() {
        val current = _navigationState.value
        if (stopRequested && current.isBusy) {
            Log.w(TAG, "stopMovement 상태 콜백 타임아웃.")
            stopRequested = false
            _navigationState.value = NavigationState.Interrupted(
                target = current.targetOrNull.orEmpty(),
                issue = NavigationIssue.USER_STOPPED
            )
        }
    }

    /** 이동 결과 메시지를 UI 가 소비한 뒤 호출한다. 진행 중이면 아무 것도 하지 않는다. */
    override fun consumeNavigationResult() {
        if (!_navigationState.value.isBusy) {
            _navigationState.value = NavigationState.Idle
        }
    }

    /** 발화 실패 메시지를 UI 가 소비한 뒤 호출한다. */
    override fun consumeSpeechResult() {
        if (_speechState.value is SpeechState.Failed) {
            _speechState.value = SpeechState.Idle
        }
    }

    /**
     * 인스턴스와 준비 상태를 확인한 뒤 [block] 을 실행한다.
     * @return 실제로 SDK 를 호출했으면 true.
     */
    private fun withRobot(block: (Robot) -> Unit): Boolean {
        val robot = this.robot
        if (robot == null || !isReady) {
            Log.w(TAG, "temi 가 준비되지 않아 호출을 무시합니다. state=${_connectionState.value}")
            return false
        }
        return runCatching { block(robot) }
            .onFailure { Log.e(TAG, "temi SDK 호출 실패", it) }
            .isSuccess
    }

    companion object {
        private const val TAG = "TemiRepository"

        /** 타임아웃 폴백으로 만든 실패임을 로그에서 구분하기 위한 자체 코드. */
        const val CODE_NO_SDK_RESPONSE = -1

        /** temi 권한 요청 식별 코드. */
        private const val PERMISSION_REQUEST_CODE = 1001

        /** SDK 가 읽는 application meta-data 이름. (`@string/metadata_permissions` 와 같은 값) */
        private const val METADATA_PERMISSIONS = "com.robotemi.sdk.metadata.PERMISSIONS"

        // SDK 가 내려주는 원시 상태 문자열. 버전에 따른 상수명 변경 영향을 받지 않도록 직접 정의한다.
        private const val STATUS_START = "start"
        private const val STATUS_CALCULATING = "calculating"
        private const val STATUS_GOING = "going"
        private const val STATUS_COMPLETE = "complete"
        private const val STATUS_ABORT = "abort"

        @Volatile
        private var instance: TemiRepository? = null

        fun getInstance(): TemiRepository =
            instance ?: synchronized(this) {
                instance ?: TemiRepository().also { instance = it }
            }
    }
}
