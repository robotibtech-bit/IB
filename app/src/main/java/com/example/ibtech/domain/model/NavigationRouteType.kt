package com.example.ibtech.domain.model

/**
 * 시설로 이동할 때 실제 `goTo()` 대상을 어떻게 정할지 (POI GOTO 경로 처리 요구사항).
 *
 * 사용자가 선택한 시설([Facility])과 실제 로봇이 이동하는 POI는 분리되어 있다 —
 * [com.example.ibtech.domain.usecase.ResolveNavigationTargetUseCase]가 이 값을 기준으로
 * 실제 `goTo()` 문자열을 정한다. 층수·방향·안내 문구·이미지는 이 값과 무관하게 항상
 * 원래 선택한 [Facility] 기준으로만 결정된다.
 */
enum class NavigationRouteType {
    /** 선택한 시설의 [Facility.sourcePoiName]으로 바로 이동. */
    DIRECT,

    /** 공용 "엘리베이터" POI까지만 이동(타 층 일반 시설). */
    ELEVATOR,

    /** 공용 "연결통로" POI까지만 이동(1층 연결통로 대상, 계단·지하계단 대상 포함). */
    CORRIDOR,

    /** 관리자가 [Facility.navigationTargetOverride]로 직접 지정한 POI로 이동 — 위 세 규칙보다
     * 우선한다. */
    CUSTOM
}
