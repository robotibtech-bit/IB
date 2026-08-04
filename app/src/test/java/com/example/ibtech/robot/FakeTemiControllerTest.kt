package com.example.ibtech.robot

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * [FakeTemiController]가 실제 SDK 콜백과 같은 모양(비동기 지연 전이)으로 동작하는지, 그리고
 * [TemiController] 계약(타임아웃 폴백 등)을 지키는지 검증한다.
 *
 * 내부적으로 짧은 실제 지연(`delay`)을 코루틴으로 흘려보내는 구조라, 결정론적으로 만들려면
 * 디스패처 주입이 필요하지만 이 클래스는 개발자 도구용이라 실제 시간(수 초 이내)으로 기다려도
 * 테스트 비용이 크지 않다.
 */
class FakeTemiControllerTest {

    @Test
    fun `goTo transitions through moving to arrived on success`() = runBlocking {
        val controller = FakeTemiController(initialLocations = listOf("poi-1"))
        controller.nextOutcome = FakeOutcome.SUCCESS

        assertTrue(controller.goTo("poi-1"))

        val arrived = withTimeout(5_000) {
            controller.navigationState.first { it is NavigationState.Arrived }
        }
        assertEquals("poi-1", (arrived as NavigationState.Arrived).target)
    }

    @Test
    fun `goTo ends in Failed when outcome is FAILED`() = runBlocking {
        val controller = FakeTemiController()
        controller.nextOutcome = FakeOutcome.FAILED

        controller.goTo("poi-2")

        val failed = withTimeout(5_000) {
            controller.navigationState.first { it is NavigationState.Failed }
        }
        assertEquals(NavigationIssue.SDK_ERROR, (failed as NavigationState.Failed).issue)
    }

    @Test
    fun `goTo ends in interruption when outcome is EXTERNAL_INTERRUPTION`() = runBlocking {
        val controller = FakeTemiController()
        controller.nextOutcome = FakeOutcome.EXTERNAL_INTERRUPTION

        controller.goTo("poi-3")

        val interrupted = withTimeout(5_000) {
            controller.navigationState.first { it is NavigationState.Interrupted }
        }
        assertEquals(
            NavigationIssue.EXTERNAL_INTERRUPTION,
            (interrupted as NavigationState.Interrupted).issue
        )
    }

    @Test
    fun `stopMovement interrupts an in-progress goTo as USER_STOPPED`() = runBlocking {
        val controller = FakeTemiController()
        controller.nextOutcome = FakeOutcome.SUCCESS
        controller.goTo("poi-4")

        withTimeout(5_000) {
            controller.navigationState.first { it is NavigationState.Moving }
        }
        assertTrue(controller.stopMovement())

        val state = controller.navigationState.value
        assertTrue(state is NavigationState.Interrupted)
        assertEquals(NavigationIssue.USER_STOPPED, (state as NavigationState.Interrupted).issue)
    }

    @Test
    fun `stopMovement returns false when nothing is moving`() {
        val controller = FakeTemiController()
        assertFalse(controller.stopMovement())
    }

    @Test
    fun `goTo returns false when already busy`() {
        val controller = FakeTemiController()
        assertTrue(controller.goTo("poi-5"))
        assertFalse(controller.goTo("poi-6"))
    }

    @Test
    fun `reportNavigationTimeout resolves a stuck Requested state to Failed`() {
        val controller = FakeTemiController()
        controller.goTo("poi-7")

        controller.reportNavigationTimeout("poi-7")

        val state = controller.navigationState.value
        assertTrue(state is NavigationState.Failed)
        assertEquals(NavigationIssue.TIMEOUT, (state as NavigationState.Failed).issue)
    }

    @Test
    fun `reportStopTimeout is a no-op when nothing is in flight`() {
        val controller = FakeTemiController()
        controller.reportStopTimeout()
        assertEquals(NavigationState.Idle, controller.navigationState.value)
    }
}
