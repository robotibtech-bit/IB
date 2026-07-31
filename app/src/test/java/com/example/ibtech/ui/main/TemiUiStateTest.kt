package com.example.ibtech.ui.main

import com.example.ibtech.data.temi.BatteryStatus
import com.example.ibtech.data.temi.NavigationIssue
import com.example.ibtech.data.temi.NavigationState
import com.example.ibtech.data.temi.SpeechState
import com.example.ibtech.data.temi.TemiConnectionState
import com.example.ibtech.data.temi.TemiFeaturePermission
import com.example.ibtech.data.temi.TemiPermissionStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 이동/대기 상태 파생 규칙 검증. SDK 없이 순수 로직만 확인한다.
 */
class TemiUiStateTest {

    /** 준비 완료 + 지도 권한 승인까지 끝난 정상 상태. */
    private val ready = TemiUiState(
        connection = TemiConnectionState.Ready,
        permissions = TemiPermissionStatus(
            granted = setOf(TemiFeaturePermission.MAP),
            checked = true
        )
    )

    @Test
    fun `연결 전에는 CONNECTING 이고 명령을 보낼 수 없다`() {
        val state = TemiUiState(connection = TemiConnectionState.Connecting)

        assertEquals(RobotActivity.CONNECTING, state.activity)
        assertFalse(state.isReady)
        assertFalse(state.canSendCommand)
    }

    @Test
    fun `SDK 를 쓸 수 없으면 UNAVAILABLE 이다`() {
        val state = TemiUiState(connection = TemiConnectionState.Unavailable)

        assertEquals(RobotActivity.UNAVAILABLE, state.activity)
        assertFalse(state.canSendCommand)
    }

    @Test
    fun `준비 완료 후 진행 중인 작업이 없으면 STANDBY 다`() {
        assertEquals(RobotActivity.STANDBY, ready.activity)
        assertTrue(ready.canSendCommand)
        assertFalse(ready.isMoving)
        assertNull(ready.currentTarget)
    }

    @Test
    fun `goTo 요청 직후 콜백 전에도 이동 중으로 본다`() {
        val state = ready.copy(navigation = NavigationState.Requested("로비"))

        assertEquals(RobotActivity.MOVING, state.activity)
        assertTrue(state.isMoving)
        assertEquals("로비", state.currentTarget)
        // 이동 중에는 새 이동 명령을 막는다.
        assertFalse(state.canSendCommand)
    }

    @Test
    fun `주행 상태 콜백이 오면 이동 중이 유지된다`() {
        val state = ready.copy(navigation = NavigationState.Moving("사무실", "going"))

        assertEquals(RobotActivity.MOVING, state.activity)
        assertEquals("사무실", state.currentTarget)
    }

    @Test
    fun `도착하면 이동이 끝나고 대기로 돌아간다`() {
        val state = ready.copy(navigation = NavigationState.Arrived("사무실"))

        assertFalse(state.isMoving)
        assertEquals(RobotActivity.STANDBY, state.activity)
        assertTrue(state.canSendCommand)
        // 목적지는 결과 표시용으로 남는다.
        assertEquals("사무실", state.currentTarget)
    }

    @Test
    fun `이동 실패도 이동 중으로 남지 않는다`() {
        val state = ready.copy(
            navigation = NavigationState.Failed(
                target = "로비",
                issue = NavigationIssue.TIMEOUT,
                code = -1,
                sdkMessage = "no goTo status callback"
            )
        )

        assertFalse(state.isMoving)
        assertTrue(state.canSendCommand)
    }

    @Test
    fun `사용자 중지는 Interrupted 로 구분된다`() {
        val navigation = NavigationState.Interrupted("로비", NavigationIssue.USER_STOPPED)
        val state = ready.copy(navigation = navigation)

        assertFalse(state.isMoving)
        assertEquals(NavigationIssue.USER_STOPPED, navigation.issue)
    }

    @Test
    fun `발화 중에는 SPEAKING 이다`() {
        val state = ready.copy(speech = SpeechState.Speaking("안녕하세요"))

        assertEquals(RobotActivity.SPEAKING, state.activity)
        assertTrue(state.isSpeaking)
    }

    @Test
    fun `이동과 발화가 겹치면 이동을 우선 표시한다`() {
        val state = ready.copy(
            navigation = NavigationState.Moving("로비", "going"),
            speech = SpeechState.Speaking("이동합니다")
        )

        assertEquals(RobotActivity.MOVING, state.activity)
    }

    @Test
    fun `권한 확인 전에는 미승인 안내를 띄우지 않는다`() {
        val state = TemiUiState(
            connection = TemiConnectionState.Ready,
            permissions = TemiPermissionStatus(checked = false)
        )

        assertFalse(state.needsPermission)
    }

    @Test
    fun `권한 확인 후 미승인이면 안내를 띄우고 이동을 막는다`() {
        val state = TemiUiState(
            connection = TemiConnectionState.Ready,
            permissions = TemiPermissionStatus(granted = emptySet(), checked = true),
            locations = listOf("로비")
        )

        assertTrue(state.needsPermission)
        assertFalse(state.canNavigate)
        // 권한과 무관한 발화는 계속 가능해야 한다.
        assertTrue(state.isReady)
    }

    @Test
    fun `권한이 승인되면 이동할 수 있다`() {
        assertFalse(ready.needsPermission)
        assertTrue(ready.canNavigate)
    }

    @Test
    fun `이동 중에는 권한이 있어도 새 이동 명령을 막는다`() {
        val state = ready.copy(navigation = NavigationState.Moving("로비", "going"))

        assertFalse(state.canNavigate)
    }

    @Test
    fun `지도에 없는 POI 는 이동 대상이 아니다`() {
        val state = ready.copy(locations = listOf("로비", "사무실"))

        assertTrue(state.hasLocation("로비"))
        assertTrue(state.hasLocation("사무실"))
        assertFalse(state.hasLocation("옥상"))
    }

    @Test
    fun `POI 이름 비교는 대소문자를 구분하지 않는다`() {
        val state = ready.copy(locations = listOf("Lobby"))

        assertTrue(state.hasLocation("lobby"))
        assertTrue(state.hasLocation("LOBBY"))
    }

    @Test
    fun `배터리 값은 받기 전까지 null 이다`() {
        assertNull(ready.battery)
        assertEquals(87, ready.copy(battery = BatteryStatus(87, false)).battery?.percentage)
    }

    @Test
    fun `POI 목록이 비어 있으면 안내가 필요하다`() {
        assertTrue(ready.hasNoLocations)
        assertFalse(ready.copy(locations = listOf("로비", "사무실")).hasNoLocations)
        // 연결 전에는 비어 있는 것이 정상이므로 안내하지 않는다.
        assertFalse(TemiUiState().hasNoLocations)
    }
}
