package com.example.ibtech.domain.usecase

import com.example.ibtech.domain.model.EscortBlockReason
import com.example.ibtech.domain.model.EscortGate
import com.example.ibtech.domain.model.Facility
import com.example.ibtech.robot.TemiConnectionState
import com.example.ibtech.robot.TemiFeaturePermission
import com.example.ibtech.robot.TemiPermissionStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class CanStartEscortUseCaseTest {

    private val baseFloor = 1
    private val poiName = "poi-1"

    private val facility = Facility(
        id = poiName,
        sourcePoiName = poiName,
        name = "테스트 시설",
        floor = baseFloor,
        isEnabled = true
    )

    private val readySnapshot = RobotSnapshot(
        connectionState = TemiConnectionState.Ready,
        permissionStatus = TemiPermissionStatus(granted = setOf(TemiFeaturePermission.MAP), checked = true),
        knownLocations = listOf(poiName),
        isNavigationBusy = false
    )

    @Test
    fun `all conditions satisfied allows escort`() {
        val result = CanStartEscortUseCase(facility, readySnapshot)
        assertEquals(EscortGate.Allowed, result)
    }

    @Test
    fun `sdk not ready blocks`() {
        val snapshot = readySnapshot.copy(connectionState = TemiConnectionState.Connecting)
        val result = CanStartEscortUseCase(facility, snapshot)
        assertEquals(EscortGate.Blocked(EscortBlockReason.SDK_NOT_READY), result)
    }

    @Test
    fun `missing permission blocks`() {
        val snapshot = readySnapshot.copy(permissionStatus = TemiPermissionStatus(granted = emptySet(), checked = true))
        val result = CanStartEscortUseCase(facility, snapshot)
        assertEquals(EscortGate.Blocked(EscortBlockReason.PERMISSION), result)
    }

    @Test
    fun `poi missing from known locations blocks`() {
        val snapshot = readySnapshot.copy(knownLocations = emptyList())
        val result = CanStartEscortUseCase(facility, snapshot)
        assertEquals(EscortGate.Blocked(EscortBlockReason.POI_MISSING), result)
    }

    @Test
    fun `already moving blocks`() {
        val snapshot = readySnapshot.copy(isNavigationBusy = true)
        val result = CanStartEscortUseCase(facility, snapshot)
        assertEquals(EscortGate.Blocked(EscortBlockReason.ALREADY_MOVING), result)
    }

    @Test
    fun `different floor no longer blocks as long as the POI is reachable`() {
        // sourcePoiName은 관리자가 엘리베이터 POI로 등록해 두는 시나리오 — 로봇 지도에는
        // 여전히 존재하므로(knownLocations에 포함) 층이 달라도 동행이 허용되어야 한다.
        val otherFloorFacility = facility.copy(floor = 2)
        val result = CanStartEscortUseCase(otherFloorFacility, readySnapshot)
        assertEquals(EscortGate.Allowed, result)
    }
}
