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
    /** 기준층(baseFloor)이 아닌 시설에서 "엘리베이터 기준 좌/우/정면 어디인지"를 관리자가
     * 고정된 3개 값 중에서 고른다(요구사항: 타 층 안내 고도화). */
    val direction: FacilityDirection? = null,
    val directionText: String? = null,
    val mapImagePath: String? = null,
    val iconKey: String? = null,
    val isEnabled: Boolean = false,
    /** 시설 안내 첫 화면에 큰 카드로 노출할지 여부(관리자 설정, 12단계). 노출 개수는
     * [com.example.ibtech.domain.model.LibrarySettings.featuredFacilityCount]가 정한다. */
    val isFeatured: Boolean = false,
    val sortOrder: Int = 0,
    val syncStatus: FacilitySyncStatus = FacilitySyncStatus.SYNCED,
    /** 실제 로봇이 `goTo()`할 POI를 관리자가 이 시설 자체(=[sourcePoiName]) 대신 다른 곳으로
     * 지정한 값(POI GOTO 경로 처리 요구사항, 관리자 수동 지정 확장). null이면 지금까지처럼
     * [com.example.ibtech.domain.usecase.ResolveNavigationTargetUseCase]의 자동 판단
     * (연결통로/엘리베이터 하드코딩 목록 → 타 층 여부 → 시설 자체)을 그대로 따른다 — 새 POI가
     * 지도에 추가되면 위치 이름과 이동 목적지가 기본적으로 같은 곳이 되는 것과 같다. */
    val navigationTargetOverride: String? = null
) {
    companion object {
        /** 관리자가 층을 아직 지정하지 않은 신규 POI의 기본값. */
        const val UNSET_FLOOR = Int.MIN_VALUE

        /**
         * 시설 목록 정렬용 그룹 값(요청: "1234층 다음에 지하1층이 표시되게") — 오름차순으로
         * 정렬하면 미설정 → 지상 층(1, 2, 3…) → 지하 층(지하1, 지하2…) 순서가 된다. [floorSortValue]와
         * 함께 써야 한다. [com.example.ibtech.data.repository.FacilityRepository.visibleFacilities]
         * (이용자 목록)와 [com.example.ibtech.ui.admin.FacilityAdminViewModel](관리자 목록) 양쪽에서 쓴다.
         */
        fun floorSortGroup(floor: Int): Int = when {
            floor == UNSET_FLOOR -> 0
            floor > 0 -> 1
            else -> 2
        }

        /**
         * 그룹 안에서의 오름차순 정렬 값. 지상 층은 floor 그대로(1, 2, 3…), 지하 층은 깊이
         * (-floor, 즉 지하1층=1, 지하2층=2…)를 써서 지하1층이 지하2층보다 먼저 오게 한다 — floor
         * 원값 그대로 정렬하면 -2가 -1보다 앞에 와 순서가 뒤집힌다.
         */
        fun floorSortValue(floor: Int): Int = if (floor > 0) floor else -floor
    }
}

/** 안내 방식. 관리자가 POI별로 지정한다(요구사항 2.1/3.2절). */
enum class GuideMode { ESCORT, LOCATION_ONLY, BOTH }

/** 기준층이 아닌 시설의 실제 위치가 엘리베이터를 기준으로 어느 방향인지. */
enum class FacilityDirection { RIGHT, FRONT, LEFT }

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
