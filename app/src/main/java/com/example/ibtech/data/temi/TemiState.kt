package com.example.ibtech.data.temi

/**
 * temi SDK 서비스 연결 상태.
 *
 * `Robot.getInstance()` 호출이 성공했다고 해서 로봇을 쓸 수 있는 것이 아니라,
 * `onRobotReady(true)` 콜백이 들어온 뒤에야 SDK 호출이 실제로 동작한다.
 */
sealed interface TemiConnectionState {

    /** SDK 인스턴스는 확보했지만 아직 `onRobotReady(true)` 를 받지 못한 상태. */
    data object Connecting : TemiConnectionState

    /** `onRobotReady(true)` 수신 완료. SDK 호출 가능. */
    data object Ready : TemiConnectionState

    /** temi 가 아닌 기기에서 실행되는 등, SDK 를 사용할 수 없는 상태. */
    data object Unavailable : TemiConnectionState
}

/**
 * 이동이 정상 종료되지 않은 사유.
 *
 * 표시 문구는 `strings.xml` 에서 UI 계층이 매핑한다. 데이터 계층은 사유만 구분한다.
 */
enum class NavigationIssue {
    /** 사용자가 중지 버튼을 눌러 취소. */
    USER_STOPPED,

    /** 조이스틱 개입 등 외부 입력으로 자율주행이 취소. */
    EXTERNAL_INTERRUPTION,

    /** 명령 후 상태 콜백이 오지 않음 (타임아웃 폴백). */
    TIMEOUT,

    /** 경로 실패·장애물 등 SDK 가 보고한 실패. */
    SDK_ERROR
}

/**
 * temi 이동 상태를 앱의 단일 상태로 정규화한 모델.
 * (temi SDK 가이드 17장 "권장 상태 머신")
 *
 * UI 버튼은 이 상태를 직접 확정하지 않고 명령만 발생시킨다.
 * 상태 전환은 오직 SDK 콜백 또는 타임아웃 폴백([TemiRepository] 내부)에서만 일어난다.
 */
sealed interface NavigationState {

    /** 대기 중. 진행 중인 주행 없음. */
    data object Idle : NavigationState

    /** `goTo` 를 호출했지만 아직 SDK 상태 콜백이 오지 않은 구간. */
    data class Requested(val target: String) : NavigationState

    /** SDK 가 주행 중임을 알린 상태. [sdkStatus] 는 원시 상태 문자열. */
    data class Moving(val target: String, val sdkStatus: String) : NavigationState

    /** 주행이 도중에 취소된 상태. */
    data class Interrupted(val target: String, val issue: NavigationIssue) : NavigationState

    /** 목적지 도착. */
    data class Arrived(val target: String) : NavigationState

    /** 주행이 실패한 상태. [sdkMessage] 는 진단용 원문이며 UI 표시는 [issue] 로 판단한다. */
    data class Failed(
        val target: String,
        val issue: NavigationIssue,
        val code: Int,
        val sdkMessage: String
    ) : NavigationState

    /** 현재 이동이 진행 중인지 여부. 중지 버튼 활성화 판단 등에 사용한다. */
    val isBusy: Boolean
        get() = this is Requested || this is Moving

    /** 이 상태가 가리키는 목적지. 대기 중이면 null. */
    val targetOrNull: String?
        get() = when (this) {
            is Requested -> target
            is Moving -> target
            is Interrupted -> target
            is Arrived -> target
            is Failed -> target
            Idle -> null
        }
}

/**
 * 앱이 필요로 하는 temi 전용 권한.
 *
 * 일반 Android 권한이 아니라 temi Launcher 가 관리한다. UI 가 SDK 타입을 직접 알 필요가 없도록
 * 앱 자체 enum 으로 감싸고, SDK enum 매핑은 [TemiRepository] 안에서만 한다.
 */
enum class TemiFeaturePermission {
    /** 지도 데이터와 POI(저장 위치) 접근. */
    MAP
}

/**
 * temi 권한 승인 현황.
 *
 * 권한 다이얼로그를 띄웠다는 사실을 승인 완료로 간주하면 안 되므로,
 * [granted] 는 오직 `checkSelfPermission` 결과와 승인 콜백으로만 갱신한다.
 */
data class TemiPermissionStatus(
    val granted: Set<TemiFeaturePermission> = emptySet(),
    /** 권한 요청을 보내고 결과 콜백을 기다리는 중인지. */
    val requestInFlight: Boolean = false,
    /** 한 번이라도 확인을 마쳤는지. 확인 전에는 '미승인' 안내를 띄우지 않는다. */
    val checked: Boolean = false
) {
    val missing: List<TemiFeaturePermission>
        get() = TemiFeaturePermission.entries.filterNot { it in granted }

    val allGranted: Boolean
        get() = missing.isEmpty()
}

/** 배터리 잔량과 충전 여부. */
data class BatteryStatus(
    val percentage: Int,
    val isCharging: Boolean
)

/**
 * TTS 발화 상태. 화면 전환·타임아웃과 음성 완료 정책을 분리하기 위해 별도 상태로 둔다.
 */
sealed interface SpeechState {

    /** 발화 중 아님. */
    data object Idle : SpeechState

    /** 발화 중이거나 큐에서 대기 중. */
    data class Speaking(val text: String) : SpeechState

    /** 발화 실패. [sdkStatus] 는 원시 상태(ERROR / NOT_ALLOWED). */
    data class Failed(val text: String, val sdkStatus: String) : SpeechState
}
