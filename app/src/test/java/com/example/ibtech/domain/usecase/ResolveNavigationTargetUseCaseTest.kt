package com.example.ibtech.domain.usecase

import com.example.ibtech.domain.model.Facility
import com.example.ibtech.domain.model.NavigationRouteType
import org.junit.Assert.assertEquals
import org.junit.Test

class ResolveNavigationTargetUseCaseTest {

    private val baseFloor = 1

    private fun facility(id: String, floor: Int) = Facility(
        id = id,
        sourcePoiName = id,
        name = id,
        floor = floor,
        isEnabled = true
    )

    @Test
    fun `base floor general facility goes direct to its own poi`() {
        val facility = facility("어린이자료실", floor = baseFloor)

        assertEquals(NavigationRouteType.DIRECT, ResolveNavigationTargetUseCase.routeType(facility, baseFloor))
        assertEquals("어린이자료실", ResolveNavigationTargetUseCase.poiName(facility, baseFloor))
    }

    @Test
    fun `other floor general facility goes to elevator`() {
        val facility = facility("종합자료실", floor = 3)

        assertEquals(NavigationRouteType.ELEVATOR, ResolveNavigationTargetUseCase.routeType(facility, baseFloor))
        assertEquals("엘리베이터", ResolveNavigationTargetUseCase.poiName(facility, baseFloor))
    }

    @Test
    fun `corridor override target goes to corridor regardless of floor`() {
        val facility = facility("동아리실1", floor = baseFloor)

        assertEquals(NavigationRouteType.CORRIDOR, ResolveNavigationTargetUseCase.routeType(facility, baseFloor))
        assertEquals("연결통로", ResolveNavigationTargetUseCase.poiName(facility, baseFloor))
    }

    @Test
    fun `stairs override target goes to corridor, not elevator`() {
        val facility = facility("1열람실", floor = 2)

        assertEquals(NavigationRouteType.CORRIDOR, ResolveNavigationTargetUseCase.routeType(facility, baseFloor))
        assertEquals("연결통로", ResolveNavigationTargetUseCase.poiName(facility, baseFloor))
    }

    @Test
    fun `basement stairs override target goes to corridor`() {
        val facility = facility("도시락존", floor = -1)

        assertEquals(NavigationRouteType.CORRIDOR, ResolveNavigationTargetUseCase.routeType(facility, baseFloor))
        assertEquals("연결통로", ResolveNavigationTargetUseCase.poiName(facility, baseFloor))
    }

    @Test
    fun `corridor override takes priority even when the facility is also an elevator target`() {
        // 실제로 겹치는 시설은 없지만, 우선순위 규칙(연결통로가 1순위) 자체를 명시적으로 검증한다.
        val facility = facility("동아리실1", floor = 5)

        assertEquals(NavigationRouteType.CORRIDOR, ResolveNavigationTargetUseCase.routeType(facility, baseFloor))
    }
}
