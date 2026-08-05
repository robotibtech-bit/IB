package com.example.ibtech.domain.usecase

import com.example.ibtech.domain.model.StatEvent
import com.example.ibtech.domain.model.StatEventType

/**
 * 통계 화면 집계 (요구사항 명세서 4.3절, 로드맵 11단계).
 *
 * 순수 함수 — 기간 필터링은 호출부가 [ResolvePeriodStartUseCase]로 이미 걸러 넘긴 이벤트
 * 목록을 받아 개수만 센다.
 */
object AggregateStatsUseCase {

    operator fun invoke(events: List<StatEvent>): StatsSummary {
        fun countsByLabel(type: StatEventType): List<LabelCount> =
            events.filter { it.type == type }
                .groupingBy { it.label.orEmpty() }
                .eachCount()
                .map { (label, count) -> LabelCount(label, count) }
                .sortedByDescending { it.count }

        val quizCompletions = events.filter { it.type == StatEventType.QUIZ_COMPLETE }
        val totalCorrect = quizCompletions.sumOf { it.correctCount ?: 0 }
        val totalQuestions = quizCompletions.sumOf { it.totalCount ?: 0 }

        return StatsSummary(
            menuSelectCounts = countsByLabel(StatEventType.MENU_SELECT),
            facilityRequestCounts = countsByLabel(StatEventType.FACILITY_REQUEST),
            escortCount = events.count { it.type == StatEventType.ESCORT_START },
            locationOnlyCount = events.count { it.type == StatEventType.LOCATION_ONLY_START },
            navSuccessCount = events.count { it.type == StatEventType.NAV_SUCCESS },
            navCancelledCount = events.count { it.type == StatEventType.NAV_CANCELLED },
            navFailedCount = events.count { it.type == StatEventType.NAV_FAILED },
            usageTopicCounts = countsByLabel(StatEventType.USAGE_TOPIC_VIEW),
            staffHelpCount = events.count { it.type == StatEventType.STAFF_HELP_REQUEST },
            quizStartCount = events.count { it.type == StatEventType.QUIZ_START },
            quizCompleteCount = quizCompletions.size,
            quizAccuracyPercent = if (totalQuestions > 0) totalCorrect * 100 / totalQuestions else 0,
            bookViewCounts = countsByLabel(StatEventType.BOOK_VIEW),
            eventViewCounts = countsByLabel(StatEventType.EVENT_VIEW),
            eventQrOpenCount = events.count { it.type == StatEventType.EVENT_QR_OPEN }
        )
    }
}

data class LabelCount(val label: String, val count: Int)

data class StatsSummary(
    val menuSelectCounts: List<LabelCount> = emptyList(),
    val facilityRequestCounts: List<LabelCount> = emptyList(),
    val escortCount: Int = 0,
    val locationOnlyCount: Int = 0,
    val navSuccessCount: Int = 0,
    val navCancelledCount: Int = 0,
    val navFailedCount: Int = 0,
    val usageTopicCounts: List<LabelCount> = emptyList(),
    val staffHelpCount: Int = 0,
    val quizStartCount: Int = 0,
    val quizCompleteCount: Int = 0,
    val quizAccuracyPercent: Int = 0,
    val bookViewCounts: List<LabelCount> = emptyList(),
    val eventViewCounts: List<LabelCount> = emptyList(),
    val eventQrOpenCount: Int = 0
)
