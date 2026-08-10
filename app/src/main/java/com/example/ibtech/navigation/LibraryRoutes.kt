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

    fun facilityAdminEdit(facilityId: String): String = "facility_admin_edit/${Uri.encode(facilityId)}"
}
