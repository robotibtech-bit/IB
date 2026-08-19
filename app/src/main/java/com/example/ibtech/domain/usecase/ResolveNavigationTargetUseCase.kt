package com.example.ibtech.domain.usecase

import com.example.ibtech.domain.model.BasementStairsWayfindingOverride
import com.example.ibtech.domain.model.Facility
import com.example.ibtech.domain.model.NavigationRouteType
import com.example.ibtech.domain.model.StairsWayfindingOverride
import com.example.ibtech.domain.model.WayfindingCorridorOverride

/**
 * 사용자가 선택한 [Facility]와 실제 로봇이 `goTo()`할 POI를 분리해서 결정한다
 * (POI GOTO 경로 처리 최종 요구사항 5·8절).
 *
 * 순수 함수다 — 판단에 필요한 건 시설 정보와 기준층뿐이라 유닛 테스트로 우선순위를 검증한다.
 * 실제 이동에 쓰는 특수 경유지 POI는 [ELEVATOR_POI_NAME]/[CORRIDOR_POI_NAME] 두 개뿐이며,
 * `계단` POI로는 이동하지 않는다.
 *
 * [StairsWayfindingOverride]/[BasementStairsWayfindingOverride] 대상도 [WayfindingCorridorOverride]와
 * 같은 연결통로로 이동한다 — 로봇이 실제로 데려다주는 지점이 원래부터 연결통로였고
 * ([com.example.ibtech.ui.facility.wayfindingImageAssetPaths]의 계단 안내도가 "연결통로 도착
 * 지점에서 계단 앞까지"를 그리는 것도 이 때문이다), 안내 문구는 그대로 유지한다(사용자 확인,
 * 2026-08-18: "계단 대상시설은 연결통로를 통해서 가는 위치가 맞다 멘트는 유지하고 goto는
 * 연결통로로 한다"). 안내 문구·이미지는 이 유스케이스와 무관하게 항상 [Facility] 기준으로
 * [com.example.ibtech.ui.facility.buildFloorDirectionGuideText] 등이 결정한다.
 *
 * 우선순위:
 * 1. [WayfindingCorridorOverride]/[StairsWayfindingOverride]/[BasementStairsWayfindingOverride]
 *    대상이면 층과 무관하게 연결통로로 이동.
 * 2. 그 외 기준층이 아니면 엘리베이터로 이동.
 * 3. 그 외(기준층 일반 시설)는 [Facility.sourcePoiName]으로 직접 이동.
 */
object ResolveNavigationTargetUseCase {

    /** 실제 Temi 지도에 관리자가 등록해 두는 이동용 POI 이름. 앱이 자동으로 만들지 않는다. */
    const val ELEVATOR_POI_NAME = "엘리베이터"
    const val CORRIDOR_POI_NAME = "연결통로"

    fun routeType(facility: Facility, baseFloor: Int): NavigationRouteType = when {
        WayfindingCorridorOverride.appliesTo(facility.id) ||
            StairsWayfindingOverride.appliesTo(facility.id) ||
            BasementStairsWayfindingOverride.appliesTo(facility.id) -> NavigationRouteType.CORRIDOR

        facility.floor != baseFloor -> NavigationRouteType.ELEVATOR

        else -> NavigationRouteType.DIRECT
    }

    /** 실제 `goTo()`에 넘길 POI 이름. */
    fun poiName(facility: Facility, baseFloor: Int): String =
        when (routeType(facility, baseFloor)) {
            NavigationRouteType.CORRIDOR -> CORRIDOR_POI_NAME
            NavigationRouteType.ELEVATOR -> ELEVATOR_POI_NAME
            NavigationRouteType.DIRECT -> facility.sourcePoiName
        }
}
