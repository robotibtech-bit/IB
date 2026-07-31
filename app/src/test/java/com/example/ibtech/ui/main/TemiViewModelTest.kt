package com.example.ibtech.ui.main

import com.example.ibtech.data.temi.FakeTemiController
import com.example.ibtech.data.temi.NavigationIssue
import com.example.ibtech.data.temi.NavigationState
import com.example.ibtech.data.temi.SpeechState
import com.example.ibtech.data.temi.TemiPermissionStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * 명령 거절 사유와 실패 안내 흐름 검증.
 *
 * 실제 로봇 없이 [FakeTemiController] 로 SDK 콜백 상황을 만든다.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TemiViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var controller: FakeTemiController

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        controller = FakeTemiController()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    /**
     * 알림을 모으면서 [block] 을 실행한다.
     *
     * alerts 는 replay 가 없으므로 [block] 보다 먼저 구독이 시작돼야 한다.
     * `launch` 만으로는 구독이 시작되지 않아 [runCurrent] 로 한 번 흘려준다.
     */
    private suspend fun TestScope.collectAlerts(
        viewModel: TemiViewModel,
        block: suspend () -> Unit
    ): List<UiAlert> {
        val alerts = mutableListOf<UiAlert>()
        val job = launch { viewModel.alerts.toList(alerts) }
        runCurrent()
        block()
        runCurrent()
        job.cancel()
        return alerts
    }

    // ------------------------------------------------------------------
    // 명령 거절
    // ------------------------------------------------------------------

    @Test
    fun `준비 전 이동 명령은 거절하고 안내한다`() = runTest {
        val viewModel = TemiViewModel(controller)
        advanceUntilIdle()

        val alerts = collectAlerts(viewModel) {
            assertFalse(viewModel.goTo("로비"))
            advanceUntilIdle()
        }

        assertEquals(listOf(UiAlert.RobotNotReady), alerts)
        // SDK 호출까지 가지 않아야 한다.
        assertFalse(controller.calls.any { it.startsWith("goTo") })
    }

    @Test
    fun `권한이 없으면 이동을 거절하고 권한 안내를 띄운다`() = runTest {
        controller.becomeReadyWithoutPermission(locations = listOf("로비"))
        val viewModel = TemiViewModel(controller)
        advanceUntilIdle()

        val alerts = collectAlerts(viewModel) {
            assertFalse(viewModel.goTo("로비"))
            advanceUntilIdle()
        }

        assertEquals(listOf(UiAlert.PermissionRequired), alerts)
        assertFalse(controller.calls.any { it.startsWith("goTo") })
    }

    @Test
    fun `지도에 없는 목적지는 도달 불가로 안내한다`() = runTest {
        controller.becomeReady(locations = listOf("로비", "사무실"))
        val viewModel = TemiViewModel(controller)
        advanceUntilIdle()

        val alerts = collectAlerts(viewModel) {
            assertFalse(viewModel.goTo("옥상"))
            advanceUntilIdle()
        }

        assertEquals(listOf(UiAlert.DestinationUnreachable("옥상")), alerts)
        assertFalse(controller.calls.any { it.startsWith("goTo") })
    }

    @Test
    fun `SDK 호출이 실패하면 준비되지 않음으로 안내한다`() = runTest {
        controller.becomeReady()
        controller.commandsSucceed = false
        val viewModel = TemiViewModel(controller)
        advanceUntilIdle()

        val alerts = collectAlerts(viewModel) {
            assertFalse(viewModel.goTo("로비"))
            advanceUntilIdle()
        }

        assertEquals(listOf(UiAlert.RobotNotReady), alerts)
        // 호출은 시도했다.
        assertTrue(controller.calls.contains("goTo:로비"))
    }

    @Test
    fun `정상 상태에서는 이동 명령이 통과한다`() = runTest {
        controller.becomeReady()
        val viewModel = TemiViewModel(controller)
        advanceUntilIdle()

        assertTrue(viewModel.goTo("로비"))
        // advanceUntilIdle 은 10초 타임아웃까지 흘려버리므로 즉시 처리분만 돌린다.
        runCurrent()

        assertTrue(controller.calls.contains("goTo:로비"))
        assertEquals("로비", viewModel.uiState.value.currentTarget)
    }

    @Test
    fun `이동 중에는 새 이동 명령을 조용히 무시한다`() = runTest {
        controller.becomeReady()
        val viewModel = TemiViewModel(controller)
        advanceUntilIdle()
        controller.emitNavigationState(NavigationState.Moving("로비", "going"))
        advanceUntilIdle()

        val alerts = collectAlerts(viewModel) {
            assertFalse(viewModel.goTo("사무실"))
            advanceUntilIdle()
        }

        // 이미 이동 중인 것은 오류가 아니므로 안내하지 않는다.
        assertTrue(alerts.isEmpty())
        assertFalse(controller.calls.contains("goTo:사무실"))
    }

    // ------------------------------------------------------------------
    // 결과 안내
    // ------------------------------------------------------------------

    @Test
    fun `도착하면 안내하고 상태를 대기로 되돌린다`() = runTest {
        controller.becomeReady()
        val viewModel = TemiViewModel(controller)
        advanceUntilIdle()

        val alerts = collectAlerts(viewModel) {
            controller.emitNavigationState(NavigationState.Arrived("로비"))
            advanceUntilIdle()
        }

        assertEquals(listOf(UiAlert.Arrived("로비")), alerts)
        assertEquals(NavigationState.Idle, controller.navigationState.value)
    }

    @Test
    fun `이동 실패는 사유와 함께 안내한다`() = runTest {
        controller.becomeReady()
        val viewModel = TemiViewModel(controller)
        advanceUntilIdle()

        val alerts = collectAlerts(viewModel) {
            controller.emitNavigationState(
                NavigationState.Failed("로비", NavigationIssue.SDK_ERROR, 10001, "blocked")
            )
            advanceUntilIdle()
        }

        assertEquals(listOf(UiAlert.NavigationFailed(NavigationIssue.SDK_ERROR)), alerts)
        assertEquals(NavigationState.Idle, controller.navigationState.value)
    }

    @Test
    fun `같은 결과를 두 번 안내하지 않는다`() = runTest {
        controller.becomeReady()
        val viewModel = TemiViewModel(controller)
        advanceUntilIdle()

        val alerts = collectAlerts(viewModel) {
            controller.emitNavigationState(NavigationState.Arrived("로비"))
            advanceUntilIdle()
            // 상태가 Idle 로 정리됐으므로 다시 방출되지 않는다.
            advanceUntilIdle()
        }

        assertEquals(1, alerts.size)
    }

    @Test
    fun `발화 실패를 안내하고 상태를 정리한다`() = runTest {
        controller.becomeReady()
        val viewModel = TemiViewModel(controller)
        advanceUntilIdle()

        val alerts = collectAlerts(viewModel) {
            controller.emitSpeechState(SpeechState.Failed("안녕하세요", "ERROR"))
            advanceUntilIdle()
        }

        assertEquals(listOf(UiAlert.SpeechFailed), alerts)
        assertEquals(SpeechState.Idle, controller.speechState.value)
    }

    @Test
    fun `SDK 오류 코드를 안내한다`() = runTest {
        controller.becomeReady()
        val viewModel = TemiViewModel(controller)
        advanceUntilIdle()

        val alerts = collectAlerts(viewModel) {
            controller.emitSdkError(403)
            advanceUntilIdle()
        }

        assertEquals(listOf(UiAlert.SdkError(403)), alerts)
    }

    // ------------------------------------------------------------------
    // 타임아웃 폴백
    // ------------------------------------------------------------------

    @Test
    fun `이동 상태 콜백이 없으면 타임아웃으로 정리한다`() = runTest {
        controller.becomeReady()
        val viewModel = TemiViewModel(controller)
        advanceUntilIdle()

        val alerts = collectAlerts(viewModel) {
            assertTrue(viewModel.goTo("로비"))
            // 콜백이 오지 않은 채 시간만 흐른다.
            advanceTimeBy(11_000)
            advanceUntilIdle()
        }

        assertEquals(listOf(UiAlert.NavigationFailed(NavigationIssue.TIMEOUT)), alerts)
        assertEquals(NavigationState.Idle, controller.navigationState.value)
    }

    @Test
    fun `주행이 시작되면 타임아웃 폴백이 상태를 건드리지 않는다`() = runTest {
        controller.becomeReady()
        val viewModel = TemiViewModel(controller)
        advanceUntilIdle()

        assertTrue(viewModel.goTo("로비"))
        controller.emitNavigationState(NavigationState.Moving("로비", "going"))
        advanceTimeBy(11_000)
        advanceUntilIdle()

        // 폴백은 호출되지만 Requested 가 아니므로 아무 것도 바꾸지 않는다.
        assertTrue(controller.calls.contains("reportNavigationTimeout:로비"))
        assertEquals(
            NavigationState.Moving("로비", "going"),
            controller.navigationState.value
        )
    }

    @Test
    fun `이동 중이 아니면 중지 명령을 보내지 않는다`() = runTest {
        controller.becomeReady()
        val viewModel = TemiViewModel(controller)
        advanceUntilIdle()

        assertFalse(viewModel.stopMovement())
        advanceUntilIdle()

        assertFalse(controller.calls.contains("stopMovement"))
    }

    // ------------------------------------------------------------------
    // 권한
    // ------------------------------------------------------------------

    @Test
    fun `자동 권한 요청은 한 번만 보낸다`() = runTest {
        controller.becomeReadyWithoutPermission()
        val viewModel = TemiViewModel(controller)
        advanceUntilIdle()

        viewModel.requestPermissionsOnce()
        viewModel.requestPermissionsOnce()
        viewModel.requestPermissionsOnce()
        advanceUntilIdle()

        assertEquals(1, controller.calls.count { it == "requestMissingPermissions" })
    }

    @Test
    fun `이미 승인됐으면 자동 요청을 보내지 않는다`() = runTest {
        controller.becomeReady()
        val viewModel = TemiViewModel(controller)
        advanceUntilIdle()

        viewModel.requestPermissionsOnce()
        advanceUntilIdle()

        assertFalse(controller.calls.contains("requestMissingPermissions"))
    }

    @Test
    fun `준비 전에는 권한을 요청하지 않고 안내한다`() = runTest {
        val viewModel = TemiViewModel(controller)
        advanceUntilIdle()

        val alerts = collectAlerts(viewModel) {
            assertFalse(viewModel.requestPermissions())
            runCurrent()
        }

        assertEquals(listOf(UiAlert.RobotNotReady), alerts)
        assertFalse(controller.calls.contains("requestMissingPermissions"))
    }

    @Test
    fun `권한 결과 콜백이 없으면 요청 중 표시를 푼다`() = runTest {
        controller.becomeReadyWithoutPermission()
        val viewModel = TemiViewModel(controller)
        advanceUntilIdle()

        assertTrue(viewModel.requestPermissions())
        runCurrent()
        // 결과 콜백을 기다리는 동안은 요청 중이라 버튼이 잠긴다.
        assertTrue(viewModel.uiState.value.permissions.requestInFlight)

        advanceTimeBy(61_000)
        runCurrent()

        // 폴백이 잠금을 풀어 다시 시도할 수 있어야 한다.
        assertTrue(controller.calls.contains("reportPermissionRequestTimeout"))
        assertFalse(viewModel.uiState.value.permissions.requestInFlight)
    }

    @Test
    fun `권한 요청이 승인 없이 끝나면 거부로 안내한다`() = runTest {
        controller.becomeReadyWithoutPermission()
        val viewModel = TemiViewModel(controller)
        advanceUntilIdle()

        val alerts = collectAlerts(viewModel) {
            // 요청 중 → 요청 종료(미승인)
            controller.emitPermissionStatus(
                TemiPermissionStatus(checked = true, requestInFlight = true)
            )
            advanceUntilIdle()
            controller.emitPermissionStatus(
                TemiPermissionStatus(checked = true, requestInFlight = false)
            )
            advanceUntilIdle()
        }

        assertEquals(listOf(UiAlert.PermissionDenied), alerts)
    }
}
