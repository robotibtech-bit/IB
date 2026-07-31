package com.example.ibtech.ui.main

import com.example.ibtech.data.temi.BatteryStatus
import com.example.ibtech.data.temi.NavigationState
import com.example.ibtech.data.temi.SpeechState
import com.example.ibtech.data.temi.TemiConnectionState
import com.example.ibtech.data.temi.TemiPermissionStatus

/**
 * 로봇이 지금 무엇을 하고 있는지에 대한 한 줄 요약.
 * 대시보드의 상태 배지가 이 값 하나만 보고 그려지도록 한다.
 */
enum class RobotActivity {
    /** SDK 를 쓸 수 없는 기기이거나 연결 실패. */
    UNAVAILABLE,

    /** `onRobotReady` 대기 중. */
    CONNECTING,

    /** 대기 중. 명령을 받을 수 있음. */
    STANDBY,

    /** 목적지로 이동 중. */
    MOVING,

    /** 발화 중. */
    SPEAKING
}

/**
 * 화면이 필요로 하는 상태를 하나로 합친 모델.
 * SDK 콜백은 [com.example.ibtech.data.temi.TemiRepository] 에서 정규화된 뒤 여기로 흘러온다.
 */
data class TemiUiState(
    val connection: TemiConnectionState = TemiConnectionState.Connecting,
    val navigation: NavigationState = NavigationState.Idle,
    val speech: SpeechState = SpeechState.Idle,
    /** temi 지도에 저장된 POI 목록. */
    val locations: List<String> = emptyList(),
    val permissions: TemiPermissionStatus = TemiPermissionStatus(),
    val battery: BatteryStatus? = null
) {
    /** SDK 명령을 보낼 수 있는 상태인지. */
    val isReady: Boolean
        get() = connection is TemiConnectionState.Ready

    /** 권한 안내 카드를 띄워야 하는지. 확인이 끝나기 전에는 띄우지 않는다. */
    val needsPermission: Boolean
        get() = isReady && permissions.checked && !permissions.allGranted

    /** 이동 중(요청 직후 포함)인지. 중지 버튼 활성화 판단에 사용한다. */
    val isMoving: Boolean
        get() = navigation.isBusy

    val isSpeaking: Boolean
        get() = speech is SpeechState.Speaking

    /** 현재 목적지. 이동 중이 아니면 null. */
    val currentTarget: String?
        get() = navigation.targetOrNull

    /** POI 목록을 아직 한 번도 받지 못했는지. */
    val hasNoLocations: Boolean
        get() = isReady && locations.isEmpty()

    /**
     * 명령 버튼을 눌러도 되는 상태인지.
     * 이동 중에는 새 이동 명령을 막고, 중지 버튼만 열어 둔다.
     */
    val canSendCommand: Boolean
        get() = isReady && !isMoving

    /** 이동 명령을 보낼 수 있는지. 지도 권한이 없으면 POI 이동을 막는다. */
    val canNavigate: Boolean
        get() = canSendCommand && permissions.allGranted

    /** [location] 이 실제 temi 지도에 존재하는 POI 인지. */
    fun hasLocation(location: String): Boolean =
        locations.any { it.equals(location, ignoreCase = true) }

    val activity: RobotActivity
        get() = when {
            connection is TemiConnectionState.Unavailable -> RobotActivity.UNAVAILABLE
            connection is TemiConnectionState.Connecting -> RobotActivity.CONNECTING
            isMoving -> RobotActivity.MOVING
            isSpeaking -> RobotActivity.SPEAKING
            else -> RobotActivity.STANDBY
        }
}
