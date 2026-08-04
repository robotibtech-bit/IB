package com.example.ibtech.domain.usecase

import com.example.ibtech.domain.model.Facility
import com.example.ibtech.domain.model.GuideMode
import com.example.ibtech.domain.model.GuideOptionSet

/**
 * 시설 상세 화면에 어떤 안내 방식 버튼을 보여줄지 결정한다 (요구사항 명세서 4장 의사코드).
 *
 * 순수 함수다 — SDK/저장소를 참조하지 않으므로 유닛 테스트로 4개 분기를 모두 검증한다.
 */
object ResolveGuideOptionUseCase {

    operator fun invoke(facility: Facility, baseFloor: Int): GuideOptionSet {
        if (!facility.isEnabled) return GuideOptionSet.Hidden
        if (facility.floor == Facility.UNSET_FLOOR) return GuideOptionSet.TemiEscortOnlyUnconfigured

        // 관리자가 명시적으로 지정한 안내 방식이 최우선이며, 타 층 동행은 항상 차단한다(안전장치).
        return when (facility.guideMode) {
            GuideMode.ESCORT ->
                if (facility.floor == baseFloor) GuideOptionSet.EscortAndLocationOnly
                else GuideOptionSet.LocationOnlyWithDirections

            GuideMode.LOCATION_ONLY -> GuideOptionSet.LocationOnlyWithDirections

            GuideMode.BOTH ->
                if (facility.floor == baseFloor) GuideOptionSet.EscortAndLocationOnly
                else GuideOptionSet.LocationOnlyWithDirections
        }
    }
}
