package com.example.ibtech.navigation

import android.net.Uri

/**
 * 라우트 상수 (요구사항 명세서 `docs/01_requirements_spec.md` 1절).
 *
 * 5단계에서 `facility_navigation`을 실제 상태 머신 화면으로 채우고, `BuildConfig.DEBUG`
 * 전용 `dev_menu`를 추가했다. 9단계에서 `event_detail`을 추가했다. 10단계에서 `admin_*` 라우트를
 * 추가했다.
 */
object LibraryRoutes {
    const val HOME = "home"

    /** 그래프 등록용 패턴. 실제 이동에는 [facilityList]를 쓴다. */
    const val FACILITY_LIST = "facility_list?query={query}"
    const val FACILITY_DETAIL = "facility_detail/{facilityId}"
    const val FACILITY_MAP = "facility_map/{facilityId}"
    const val FACILITY_NAVIGATION = "facility_navigation/{facilityId}"

    const val USAGE_CATEGORY = "usage_category"
    const val USAGE_SUBCATEGORY = "usage_subcategory/{categoryId}"
    const val USAGE_ANSWER = "usage_answer/{topicId}"
    const val KIDS_MENU = "kids_menu"
    const val KIDS_QUIZ_CATEGORY = "kids_quiz_category"
    const val KIDS_QUIZ_PLAY = "kids_quiz_play/{category}"

    /** 그래프 등록용 패턴. 실제 이동에는 [kidsQuizResult]를 쓴다. */
    const val KIDS_QUIZ_RESULT = "kids_quiz_result/{category}?correct={correct}&total={total}&bookIds={bookIds}"
    const val KIDS_BOOK_RECOMMENDATION = "kids_book_recommendation?ageGroup={ageGroup}&topic={topic}"
    const val KIDS_ETIQUETTE = "kids_etiquette"
    const val EVENTS = "events"
    const val EVENT_DETAIL = "event_detail/{eventId}"

    /** 실시간 좌석 · 예약현황 메뉴(홈 하단 신규 버튼). 하위 두 카드(디지털자료실 예약현황,
     * 열람실 좌석현황)는 별도 라우트 없이 기존 [WEB_VIEW]로 바로 이동한다. */
    const val SEAT_STATUS_MENU = "seat_status_menu"

    /** 책 찾기(홈 신규 버튼). 검색은 외부 도서검색 서버가 처리한다. */
    const val BOOK_SEARCH = "book_search"

    /**
     * 서가 안내. 그래프 등록용 패턴이며 실제 이동에는 [shelfNavigation]을 쓴다.
     *
     * 책 정보를 전부 인자로 넘기는 이유 — 이 화면만 다시 열려도(프로세스 종료 후 복원 등)
     * 서버를 다시 부르지 않고 그릴 수 있어야 한다.
     */
    const val SHELF_NAVIGATION =
        "shelf_navigation?bookId={bookId}&title={title}&callNo={callNo}&location={location}" +
            "&shelfLabel={shelfLabel}&room={room}&floor={floor}&estimated={estimated}"

    /** 개발자 메뉴(`BuildConfig.DEBUG` 전용). release 빌드에는 진입 버튼 자체가 없다. */
    const val DEV_MENU = "dev_menu"

    /** 관리자 화면(로드맵 10단계). 비밀번호로 보호되며 release 빌드에도 진입 버튼이 있다. */
    const val ADMIN_LOGIN = "admin_login"
    const val ADMIN_HOME = "admin_home"
    const val FACILITY_ADMIN = "facility_admin"
    const val FACILITY_ADMIN_EDIT = "facility_admin_edit/{facilityId}"
    const val USAGE_INFO_ADMIN = "usage_info_admin"
    const val KIDS_CONTENT_ADMIN = "kids_content_admin"
    const val EVENT_ADMIN = "event_admin"
    const val SETTINGS_ADMIN = "settings_admin"
    const val STATISTICS = "statistics"
    const val APP_UPDATE = "app_update"

    /** 그래프 등록용 패턴. 실제 이동에는 [webView]를 쓴다. */
    const val WEB_VIEW = "web_view?url={url}&title={title}"

    fun facilityList(query: String? = null): String =
        if (query.isNullOrBlank()) "facility_list" else "facility_list?query=${Uri.encode(query)}"

    fun facilityDetail(facilityId: String): String = "facility_detail/${Uri.encode(facilityId)}"

    fun facilityMap(facilityId: String): String = "facility_map/${Uri.encode(facilityId)}"

    fun facilityNavigation(facilityId: String): String =
        "facility_navigation/${Uri.encode(facilityId)}"

    fun usageSubcategory(categoryId: String): String = "usage_subcategory/${Uri.encode(categoryId)}"

    fun usageAnswer(topicId: String): String = "usage_answer/${Uri.encode(topicId)}"

    fun kidsQuizPlay(category: String): String = "kids_quiz_play/${Uri.encode(category)}"

    fun kidsQuizResult(category: String, correct: Int, total: Int, bookIds: List<String>): String {
        val bookIdsParam = Uri.encode(bookIds.joinToString(","))
        return "kids_quiz_result/${Uri.encode(category)}?correct=$correct&total=$total&bookIds=$bookIdsParam"
    }

    fun kidsBookRecommendation(ageGroup: String? = null, topic: String? = null): String {
        val params = buildList {
            if (!ageGroup.isNullOrBlank()) add("ageGroup=${Uri.encode(ageGroup)}")
            if (!topic.isNullOrBlank()) add("topic=${Uri.encode(topic)}")
        }
        return if (params.isEmpty()) "kids_book_recommendation" else "kids_book_recommendation?${params.joinToString("&")}"
    }

    fun eventDetail(eventId: String): String = "event_detail/${Uri.encode(eventId)}"

    /** 층은 문자열로 넘긴다 — 서버가 층을 모를 수 있어(빈 문자열) Int 인자로는 표현하지 못한다. */
    fun shelfNavigation(
        bookId: String,
        title: String,
        callNo: String,
        location: String,
        shelfLabel: String,
        room: String,
        floor: Int?,
        estimated: Boolean
    ): String = "shelf_navigation?bookId=${Uri.encode(bookId)}" +
        "&title=${Uri.encode(title)}" +
        "&callNo=${Uri.encode(callNo)}" +
        "&location=${Uri.encode(location)}" +
        "&shelfLabel=${Uri.encode(shelfLabel)}" +
        "&room=${Uri.encode(room)}" +
        "&floor=${floor?.toString().orEmpty()}" +
        "&estimated=${if (estimated) "1" else "0"}"

    fun facilityAdminEdit(facilityId: String): String = "facility_admin_edit/${Uri.encode(facilityId)}"

    fun webView(url: String, title: String = ""): String =
        "web_view?url=${Uri.encode(url)}&title=${Uri.encode(title)}"
}
