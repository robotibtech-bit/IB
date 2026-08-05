package com.example.ibtech.domain.usecase

import com.example.ibtech.domain.model.StatEvent
import com.example.ibtech.domain.model.StatEventType
import org.junit.Assert.assertEquals
import org.junit.Test

class AggregateStatsUseCaseTest {

    private fun event(
        type: StatEventType,
        label: String? = null,
        correctCount: Int? = null,
        totalCount: Int? = null
    ) = StatEvent(type = type, label = label, correctCount = correctCount, totalCount = totalCount, timestamp = 0L)

    @Test
    fun `counts events by label and sorts by count descending`() {
        val events = listOf(
            event(StatEventType.MENU_SELECT, "시설을 찾고 있어요"),
            event(StatEventType.MENU_SELECT, "시설을 찾고 있어요"),
            event(StatEventType.MENU_SELECT, "오늘의 행사")
        )

        val result = AggregateStatsUseCase(events)

        assertEquals(
            listOf(LabelCount("시설을 찾고 있어요", 2), LabelCount("오늘의 행사", 1)),
            result.menuSelectCounts
        )
    }

    @Test
    fun `counts navigation outcomes separately`() {
        val events = listOf(
            event(StatEventType.NAV_SUCCESS),
            event(StatEventType.NAV_SUCCESS),
            event(StatEventType.NAV_CANCELLED),
            event(StatEventType.NAV_FAILED)
        )

        val result = AggregateStatsUseCase(events)

        assertEquals(2, result.navSuccessCount)
        assertEquals(1, result.navCancelledCount)
        assertEquals(1, result.navFailedCount)
    }

    @Test
    fun `computes quiz accuracy percent from completed sessions`() {
        val events = listOf(
            event(StatEventType.QUIZ_COMPLETE, "동물", correctCount = 2, totalCount = 3),
            event(StatEventType.QUIZ_COMPLETE, "동화", correctCount = 1, totalCount = 3)
        )

        val result = AggregateStatsUseCase(events)

        assertEquals(2, result.quizCompleteCount)
        assertEquals(50, result.quizAccuracyPercent)
    }

    @Test
    fun `quiz accuracy is zero when no quiz completed`() {
        val result = AggregateStatsUseCase(emptyList())

        assertEquals(0, result.quizAccuracyPercent)
    }
}
