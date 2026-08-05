package com.example.ibtech.domain.usecase

import com.example.ibtech.domain.model.LibraryEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ResolveEventListUseCaseTest {

    private fun event(
        id: String,
        startDate: String,
        endDate: String? = null,
        sortOrder: Int = 0,
        isEnabled: Boolean = true
    ) = LibraryEvent(
        id = id,
        title = id,
        startDate = startDate,
        endDate = endDate,
        place = "장소",
        sortOrder = sortOrder,
        isEnabled = isEnabled
    )

    @Test
    fun `event starting today is grouped as today event`() {
        val events = listOf(event(id = "a", startDate = "2026-08-05"))

        val result = ResolveEventListUseCase(events, today = "2026-08-05")

        assertEquals(listOf("a"), result.todayEvents.map { it.id })
        assertTrue(result.upcomingEvents.isEmpty())
    }

    @Test
    fun `multi-day event covering today is grouped as today event`() {
        val events = listOf(event(id = "a", startDate = "2026-08-03", endDate = "2026-08-07"))

        val result = ResolveEventListUseCase(events, today = "2026-08-05")

        assertEquals(listOf("a"), result.todayEvents.map { it.id })
    }

    @Test
    fun `event starting after today is grouped as upcoming and sorted by date`() {
        val events = listOf(
            event(id = "later", startDate = "2026-08-10"),
            event(id = "sooner", startDate = "2026-08-06")
        )

        val result = ResolveEventListUseCase(events, today = "2026-08-05")

        assertTrue(result.todayEvents.isEmpty())
        assertEquals(listOf("sooner", "later"), result.upcomingEvents.map { it.id })
    }

    @Test
    fun `event that already ended is excluded entirely`() {
        val events = listOf(event(id = "past", startDate = "2026-08-01", endDate = "2026-08-03"))

        val result = ResolveEventListUseCase(events, today = "2026-08-05")

        assertTrue(result.todayEvents.isEmpty())
        assertTrue(result.upcomingEvents.isEmpty())
    }

    @Test
    fun `disabled events are excluded`() {
        val events = listOf(event(id = "hidden", startDate = "2026-08-05", isEnabled = false))

        val result = ResolveEventListUseCase(events, today = "2026-08-05")

        assertTrue(result.todayEvents.isEmpty())
        assertTrue(result.upcomingEvents.isEmpty())
    }

    @Test
    fun `today events are sorted by sortOrder`() {
        val events = listOf(
            event(id = "second", startDate = "2026-08-05", sortOrder = 1),
            event(id = "first", startDate = "2026-08-05", sortOrder = 0)
        )

        val result = ResolveEventListUseCase(events, today = "2026-08-05")

        assertEquals(listOf("first", "second"), result.todayEvents.map { it.id })
    }
}
