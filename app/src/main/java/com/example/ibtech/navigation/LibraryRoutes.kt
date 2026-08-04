package com.example.ibtech.navigation

import android.net.Uri

/**
 * 라우트 상수 (요구사항 명세서 `docs/01_requirements_spec.md` 1절).
 *
 * 5단계에서 `facility_navigation`을 실제 상태 머신 화면으로 채우고, `BuildConfig.DEBUG`
 * 전용 `dev_menu`를 추가했다. 나머지 라우트(이용방법 세부, 어린이 콘텐츠 하위, 행사 상세,
 * 관리자 등)는 각 기능이 구현되는 단계(7/8/9/10단계)에서 이 파일에 추가한다.
 */
object LibraryRoutes {
    const val HOME = "home"

    /** 그래프 등록용 패턴. 실제 이동에는 [facilityList]를 쓴다. */
    const val FACILITY_LIST = "facility_list?query={query}"
    const val FACILITY_DETAIL = "facility_detail/{facilityId}"
    const val FACILITY_MAP = "facility_map/{facilityId}"
    const val FACILITY_NAVIGATION = "facility_navigation/{facilityId}"

    const val USAGE_CATEGORY = "usage_category"
    const val KIDS_MENU = "kids_menu"
    const val EVENTS = "events"

    /** 개발자 메뉴(`BuildConfig.DEBUG` 전용). release 빌드에는 진입 버튼 자체가 없다. */
    const val DEV_MENU = "dev_menu"

    fun facilityList(query: String? = null): String =
        if (query.isNullOrBlank()) "facility_list" else "facility_list?query=${Uri.encode(query)}"

    fun facilityDetail(facilityId: String): String = "facility_detail/${Uri.encode(facilityId)}"

    fun facilityMap(facilityId: String): String = "facility_map/${Uri.encode(facilityId)}"

    fun facilityNavigation(facilityId: String): String =
        "facility_navigation/${Uri.encode(facilityId)}"
}
