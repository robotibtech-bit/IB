package com.example.ibtech.domain.usecase

import java.util.Calendar
import org.junit.Assert.assertEquals
import org.junit.Test

class ResolvePeriodStartUseCaseTest {

    private fun calendarOf(year: Int, month: Int, day: Int, hour: Int = 0, minute: Int = 0): Calendar =
        Calendar.getInstance().apply {
            clear()
            set(year, month, day, hour, minute)
        }

    @Test
    fun `today start is midnight of the same day`() {
        val now = calendarOf(2026, Calendar.AUGUST, 5, hour = 14, minute = 30).timeInMillis
        val expected = calendarOf(2026, Calendar.AUGUST, 5).timeInMillis

        val start = ResolvePeriodStartUseCase(StatsPeriod.TODAY, now)

        assertEquals(expected, start)
    }

    @Test
    fun `month start is the first day of the month at midnight`() {
        val now = calendarOf(2026, Calendar.AUGUST, 5, hour = 14, minute = 30).timeInMillis
        val expected = calendarOf(2026, Calendar.AUGUST, 1).timeInMillis

        val start = ResolvePeriodStartUseCase(StatsPeriod.MONTH, now)

        assertEquals(expected, start)
    }

    @Test
    fun `week start never falls after now`() {
        val now = calendarOf(2026, Calendar.AUGUST, 5, hour = 14, minute = 30).timeInMillis

        val start = ResolvePeriodStartUseCase(StatsPeriod.WEEK, now)

        assert(start <= now) { "week start ($start) should not be after now ($now)" }
    }

    @Test
    fun `week start is at most 6 days before now`() {
        val now = calendarOf(2026, Calendar.AUGUST, 5, hour = 14, minute = 30).timeInMillis

        val start = ResolvePeriodStartUseCase(StatsPeriod.WEEK, now)

        val sixDaysMillis = 6L * 24 * 60 * 60 * 1000
        assert(now - start <= sixDaysMillis) { "week start should be within 6 days of now" }
    }
}
