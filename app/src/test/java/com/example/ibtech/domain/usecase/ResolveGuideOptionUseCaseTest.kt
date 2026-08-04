package com.example.ibtech.domain.usecase

import com.example.ibtech.domain.model.Facility
import com.example.ibtech.domain.model.GuideMode
import com.example.ibtech.domain.model.GuideOptionSet
import org.junit.Assert.assertEquals
import org.junit.Test

class ResolveGuideOptionUseCaseTest {

    private val baseFloor = 1

    private fun facility(
        floor: Int = baseFloor,
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
    fun `same floor with ESCORT allows escort and location`() {
        val result = ResolveGuideOptionUseCase(facility(floor = 1, guideMode = GuideMode.ESCORT), baseFloor)
        assertEquals(GuideOptionSet.EscortAndLocationOnly, result)
    }

    @Test
    fun `different floor with ESCORT falls back to location only`() {
        val result = ResolveGuideOptionUseCase(facility(floor = 2, guideMode = GuideMode.ESCORT), baseFloor)
        assertEquals(GuideOptionSet.LocationOnlyWithDirections, result)
    }

    @Test
    fun `LOCATION_ONLY is always location only regardless of floor`() {
        val sameFloor = ResolveGuideOptionUseCase(facility(floor = 1, guideMode = GuideMode.LOCATION_ONLY), baseFloor)
        val otherFloor = ResolveGuideOptionUseCase(facility(floor = 3, guideMode = GuideMode.LOCATION_ONLY), baseFloor)
        assertEquals(GuideOptionSet.LocationOnlyWithDirections, sameFloor)
        assertEquals(GuideOptionSet.LocationOnlyWithDirections, otherFloor)
    }

    @Test
    fun `BOTH behaves like ESCORT for floor comparison`() {
        val sameFloor = ResolveGuideOptionUseCase(facility(floor = 1, guideMode = GuideMode.BOTH), baseFloor)
        val otherFloor = ResolveGuideOptionUseCase(facility(floor = 4, guideMode = GuideMode.BOTH), baseFloor)
        assertEquals(GuideOptionSet.EscortAndLocationOnly, sameFloor)
        assertEquals(GuideOptionSet.LocationOnlyWithDirections, otherFloor)
    }

    @Test
    fun `unset floor is unconfigured regardless of guide mode`() {
        val result = ResolveGuideOptionUseCase(facility(floor = Facility.UNSET_FLOOR), baseFloor)
        assertEquals(GuideOptionSet.TemiEscortOnlyUnconfigured, result)
    }

    @Test
    fun `disabled facility is hidden even if floor and mode are valid`() {
        val result = ResolveGuideOptionUseCase(facility(floor = 1, isEnabled = false), baseFloor)
        assertEquals(GuideOptionSet.Hidden, result)
    }

    @Test
    fun `works for an arbitrary non-standard floor value without crashing`() {
        val result = ResolveGuideOptionUseCase(facility(floor = -99, guideMode = GuideMode.ESCORT), baseFloor)
        assertEquals(GuideOptionSet.LocationOnlyWithDirections, result)
    }
}
