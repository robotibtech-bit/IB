package com.example.ibtech.domain.model

/**
 * 시설(POI) 도메인 모델 (로드맵 6장, 요구사항 명세서 2.3~2.6절).
 *
 * [id]는 [sourcePoiName]을 그대로 쓴다 — Temi POI 동기화의 매칭 키와 같아 별도 UUID가
 * 필요 없다. [floor]가 [UNSET_FLOOR]면 관리자가 아직 층·안내방식을 설정하지 않은
 * "미설정" 상태이며(3.2절), 이 상태는 [FacilityRepository.visibleFacilities]에서 걸러진다.
 */
data class Facility(
    val id: String,
    val sourcePoiName: String,
    val name: String,
    val floor: Int = UNSET_FLOOR,
    val shortDescription: String = "",
    val guideMode: GuideMode = GuideMode.LOCATION_ONLY,
    val directionText: String? = null,
    val mapImagePath: String? = null,
    val iconKey: String? = null,
    val isEnabled: Boolean = false,
    /** 시설 안내 첫 화면에 큰 카드로 노출할지 여부(관리자 설정, 12단계). 노출 개수는
     * [com.example.ibtech.domain.model.LibrarySettings.featuredFacilityCount]가 정한다. */
    val isFeatured: Boolean = false,
    val sortOrder: Int = 0,
    val syncStatus: FacilitySyncStatus = FacilitySyncStatus.SYNCED
) {
    companion object {
        /** 관리자가 층을 아직 지정하지 않은 신규 POI의 기본값. */
        const val UNSET_FLOOR = Int.MIN_VALUE
    }
}

/** 안내 방식. 관리자가 POI별로 지정한다(요구사항 2.1/3.2절). */
enum class GuideMode { ESCORT, LOCATION_ONLY, BOTH }

/**
 * Temi POI 동기화 상태 (요구사항 명세서 6.2절).
 * 이용자 화면 노출 여부와는 무관하며, 관리자 화면(10단계)에서 신규/변경/삭제 3구간 표시에 쓰인다.
 */
enum class FacilitySyncStatus {
    /** 최신 동기화에서 Temi 지도에 그대로 존재. */
    SYNCED,

    /** 이번 동기화에서 새로 발견된 POI. */
    NEW,

    /** 로컬에는 있으나 Temi 지도에서 더 이상 조회되지 않음. 관리자 확인 전까지 삭제하지 않는다. */
    NOT_FOUND_ON_TEMI
}
