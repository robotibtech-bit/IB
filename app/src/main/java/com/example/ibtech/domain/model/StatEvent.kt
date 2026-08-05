package com.example.ibtech.domain.model

/**
 * 통계 이벤트 한 건 (요구사항 명세서 4.3절, 로드맵 11단계).
 *
 * 개인정보 없이 메뉴/시설/콘텐츠의 이름([label])과 발생 시각만 담는다. [correctCount]/
 * [totalCount]는 [StatEventType.QUIZ_COMPLETE]에서만 쓰는 정답률 계산용 값이다.
 */
data class StatEvent(
    val type: StatEventType,
    val label: String? = null,
    val correctCount: Int? = null,
    val totalCount: Int? = null,
    val timestamp: Long
)

/** 요구사항 4.3절에 나열된 집계 대상 이벤트 종류. */
enum class StatEventType {
    /** 메인 화면 4개 방문 목적 버튼 선택. */
    MENU_SELECT,

    /** 시설 상세에서 동행/위치만 보기 중 하나라도 요청. */
    FACILITY_REQUEST,
    ESCORT_START,
    LOCATION_ONLY_START,

    NAV_SUCCESS,
    NAV_CANCELLED,
    NAV_FAILED,

    USAGE_TOPIC_VIEW,
    STAFF_HELP_REQUEST,

    QUIZ_START,
    QUIZ_COMPLETE,

    BOOK_VIEW,

    EVENT_VIEW,
    EVENT_QR_OPEN
}
