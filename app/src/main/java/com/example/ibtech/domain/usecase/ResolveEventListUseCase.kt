package com.example.ibtech.domain.usecase

import com.example.ibtech.domain.model.LibraryEvent

/**
 * 오늘/예정 행사 분류 (요구사항 명세서 3.5절, 로드맵 9단계 "오늘/예정 행사 목록").
 *
 * "yyyy-MM-dd" 문자열의 사전식 비교만으로 오늘 여부·종료 여부를 가른다(달력 계산이나
 * `java.time` 없이도 충분하다). [today]를 인자로 받아 실제 기기 시각과 분리해 순수 함수로
 * 테스트한다.
 */
object ResolveEventListUseCase {

    operator fun invoke(events: List<LibraryEvent>, today: String): EventListResult {
        val active = events
            .filter { it.isEnabled }
            .filter { (it.endDate ?: it.startDate) >= today }

        val (todayEvents, upcomingEvents) = active.partition { it.startDate <= today }

        return EventListResult(
            todayEvents = todayEvents.sortedBy { it.sortOrder },
            upcomingEvents = upcomingEvents.sortedBy { it.startDate }
        )
    }
}

data class EventListResult(
    val todayEvents: List<LibraryEvent>,
    val upcomingEvents: List<LibraryEvent>
)
