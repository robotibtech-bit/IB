package com.example.ibtech.domain.model

/**
 * `동행 안내` 버튼을 실제로 눌러도 되는지 판단한 결과 (요구사항 명세서 4장 `canStartEscort`).
 *
 * [GuideOptionSet]이 "이 화면에 어떤 버튼을 보여줄지"를 정한다면, [EscortGate]는 그중
 * `동행 안내` 버튼이 "지금 눌러도 되는지(활성/비활성)"를 정한다.
 */
sealed interface EscortGate {
    data object Allowed : EscortGate
    data class Blocked(val reason: EscortBlockReason) : EscortGate
}

enum class EscortBlockReason {
    /** temi SDK가 준비되지 않음(`TemiConnectionState.Ready`가 아님). */
    SDK_NOT_READY,

    /** 지도/위치 권한이 아직 승인되지 않음. */
    PERMISSION,

    /** `sourcePoiName`이 temi가 현재 알고 있는 위치 목록에 없음. */
    POI_MISSING,

    /** 이미 다른 이동이 진행 중(`NavigationState.isBusy`). */
    ALREADY_MOVING
}
