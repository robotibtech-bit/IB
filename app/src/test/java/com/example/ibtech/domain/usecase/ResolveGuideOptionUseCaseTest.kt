package com.example.ibtech.domain.usecase

import com.example.ibtech.domain.model.Facility
import com.example.ibtech.domain.model.GuideMode
import com.example.ibtech.domain.model.GuideOptionSet
import org.junit.Assert.assertEquals
import org.junit.Test

class ResolveGuideOptionUseCaseTest {

    private fun facility(
        floor: Int = 1,
        guideMode: GuideMode = GuideMode.ESCORT,
        isEnabled: Boolean = true
    ) = Facility(
        id = "poi-1",
        sourcePoiName = "poi-1",
        name = "테스트 시설",
        floor = floor,
        guideMode = guideMode,
        isEnabled = isEnabled
    )

    @Test
    fun `same floor with ESCORT allows escort only, not location`() {
        // ESCORT는 관리자가 위치 안내를 명시적으로 함께 켜지 않은 것이므로 위치 버튼을 숨긴다.
        val result = ResolveGuideOptionUseCase(facility(floor = 1, guideMode = GuideMode.ESCORT))
        assertEquals(GuideOptionSet.EscortOnly, result)
    }

    @Test
    fun `different floor with ESCORT still allows escort only`() {
        // sourcePoiName이 엘리베이터 POI를 가리키도록 등록되어 있으면 타 층도 동행이 가능하다.
        val result = ResolveGuideOptionUseCase(facility(floor = 2, guideMode = GuideMode.ESCORT))
        assertEquals(GuideOptionSet.EscortOnly, result)
    }

    @Test
    fun `LOCATION_ONLY is always location only regardless of floor`() {
        val sameFloor = ResolveGuideOptionUseCase(facility(floor = 1, guideMode = GuideMode.LOCATION_ONLY))
        val otherFloor = ResolveGuideOptionUseCase(facility(floor = 3, guideMode = GuideMode.LOCATION_ONLY))
        assertEquals(GuideOptionSet.LocationOnlyWithDirections, sameFloor)
        assertEquals(GuideOptionSet.LocationOnlyWithDirections, otherFloor)
    }

    @Test
    fun `BOTH allows escort and location regardless of floor`() {
        val sameFloor = ResolveGuideOptionUseCase(facility(floor = 1, guideMode = GuideMode.BOTH))
        val otherFloor = ResolveGuideOptionUseCase(facility(floor = 4, guideMode = GuideMode.BOTH))
        assertEquals(GuideOptionSet.EscortAndLocationOnly, sameFloor)
        assertEquals(GuideOptionSet.EscortAndLocationOnly, otherFloor)
    }

    @Test
    fun `unset floor is unconfigured regardless of guide mode`() {
        val result = ResolveGuideOptionUseCase(facility(floor = Facility.UNSET_FLOOR))
        assertEquals(GuideOptionSet.TemiEscortOnlyUnconfigured, result)
    }

    @Test
    fun `disabled facility is hidden even if floor and mode are valid`() {
        val result = ResolveGuideOptionUseCase(facility(floor = 1, isEnabled = false))
        assertEquals(GuideOptionSet.Hidden, result)
    }

    @Test
    fun `works for an arbitrary non-standard floor value without crashing`() {
        val result = ResolveGuideOptionUseCase(facility(floor = -99, guideMode = GuideMode.ESCORT))
        assertEquals(GuideOptionSet.EscortOnly, result)
    }
}
