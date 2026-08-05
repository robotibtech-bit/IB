package com.example.ibtech.domain.usecase

import java.util.Calendar

/** 통계 화면의 기간 선택지 (로드맵 11단계 "오늘/주간/월간"). */
enum class StatsPeriod { TODAY, WEEK, MONTH }

/**
 * 조회 기간의 시작 시각(epoch millis)을 계산한다.
 *
 * [now]를 인자로 받아 실제 기기 시각과 분리해 순수 함수로 테스트한다. 주간은
 * `Calendar.firstDayOfWeek`(로케일 기준) 기준 이번 주 첫날 0시부터, 월간은 이번 달 1일 0시부터다.
 */
object ResolvePeriodStartUseCase {
    operator fun invoke(period: StatsPeriod, now: Long): Long {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        when (period) {
            StatsPeriod.TODAY -> Unit
            StatsPeriod.WEEK -> calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
            StatsPeriod.MONTH -> calendar.set(Calendar.DAY_OF_MONTH, 1)
        }
        return calendar.timeInMillis
    }
}
