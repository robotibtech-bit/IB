package com.example.ibtech.domain.usecase

import com.example.ibtech.domain.model.EscortBlockReason
import com.example.ibtech.domain.model.EscortGate
import com.example.ibtech.domain.model.Facility
import com.example.ibtech.robot.TemiConnectionState
import com.example.ibtech.robot.TemiFeaturePermission
import com.example.ibtech.robot.TemiPermissionStatus

/**
 * `동행 안내` 버튼을 지금 눌러도 되는지 판단할 때 필요한 로봇 상태 스냅샷.
 *
 * [com.example.ibtech.robot.TemiController]의 여러 StateFlow를 ViewModel이 한 시점의
 * 값으로 모아 넘긴다 — 유스케이스가 SDK/Flow를 직접 알 필요가 없도록 분리한다.
 */
data class RobotSnapshot(
    val connectionState: TemiConnectionState,
    val permissionStatus: TemiPermissionStatus,
    val knownLocations: List<String>,
    val isNavigationBusy: Boolean
)

/**
 * (요구사항 명세서 4장 `canStartEscort` 의사코드)
 *
 * 순서가 중요하다 — 앞선 조건일수록 더 근본적인 차단 사유이므로 먼저 검사한다.
 */
object CanStartEscortUseCase {

    operator fun invoke(facility: Facility, robot: RobotSnapshot, baseFloor: Int): EscortGate {
        if (robot.connectionState != TemiConnectionState.Ready) {
            return EscortGate.Blocked(EscortBlockReason.SDK_NOT_READY)
        }
        // 동행 안내는 지도/POI 접근(MAP)만 있으면 된다 — 음량 고정 등 다른 기능이 쓰는
        // SETTINGS 권한까지 요구하면 그 권한을 안 받았다고 동행까지 막히는 건 잘못된 커플링이다.
        if (TemiFeaturePermission.MAP !in robot.permissionStatus.granted) {
            return EscortGate.Blocked(EscortBlockReason.PERMISSION)
        }
        // 실제로 goTo() 할 POI는 facility.sourcePoiName이 아닐 수 있다(POI GOTO 경로 처리
        // 요구사항 — 타 층·기존 계단/지하계단 대상은 엘리베이터로, 연결통로 대상은 연결통로로
        // 이동한다). 존재 확인도 실제 이동 대상 기준으로 해야 한다 — sourcePoiName만 확인하면
        // 정작 필요한 엘리베이터/연결통로 POI가 없어도 통과시켜 버린다.
        val targetPoi = ResolveNavigationTargetUseCase.poiName(facility, baseFloor)
        if (!robot.knownLocations.contains(targetPoi)) {
            return EscortGate.Blocked(EscortBlockReason.POI_MISSING)
        }
        if (robot.isNavigationBusy) {
            return EscortGate.Blocked(EscortBlockReason.ALREADY_MOVING)
        }
        return EscortGate.Allowed
    }
}
