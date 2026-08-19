package com.example.ibtech.domain.model

/**
 * 2층 열람실 등 일부 시설은 엘리베이터가 아니라 여름강의실(1층) 옆에 있는 특정 계단으로만
 * 올라갈 수 있다. 로봇은 그 계단 앞(1층)까지만 동행하고, 그 뒤로는 그림 안내도를 보여줘서
 * 이용자가 직접 계단을 올라가 찾아가게 한다 — [WayfindingCorridorOverride](같은 층 연결통로)와
 * 같은 방식이지만 목적지가 다른 층이라는 점이 다르다.
 *
 * [Facility.floor]는 실제 목적지 층(예: 2)으로, [Facility.sourcePoiName]은 관리자가 temi
 * 지도에서 그 계단 앞의 실제 POI로 등록해 둔다 — 이 목록은 "도착 후 어떤 안내도 이미지를
 * 보여줄지"와 "문구에 이 계단을 짚어 안내할지"만 [Facility.id] 기준으로 판단하며, 이동 자체는
 * 리다이렉트 없이 `goTo(facility.sourcePoiName)` 그대로 처리된다.
 */
object StairsWayfindingOverride {

    /** [Facility.id] → `assets/stairs/` 안의 안내도 이미지 파일명. */
    // 안내도 이미지 파일명은 "3열람실(스마트).png"이지만, 실제 temi 지도에 등록된 POI
    // 이름(= Facility.id)은 "3열람실"이다(실제 로봇에서 확인) — 키는 실제 POI 이름을 따른다.
    private val IMAGE_ASSET_BY_FACILITY_ID = mapOf(
        "1열람실" to "1열람실.png",
        "2열람실" to "2열람실.png",
        "3열람실" to "3열람실(스마트).png",
        "쉬어yo" to "쉬어yo.png"
    )

    fun appliesTo(facilityId: String): Boolean = facilityId in IMAGE_ASSET_BY_FACILITY_ID

    /** 이 override가 다루는 시설 id 전체 — [com.example.ibtech.data.repository.DefaultFacilityContent]가
     * 디폴트 시설을 만들 때 같은 이름을 다시 하드코딩하지 않고 여기서 가져다 쓴다. */
    fun facilityIds(): Set<String> = IMAGE_ASSET_BY_FACILITY_ID.keys

    /** `assets/stairs/`를 기준으로 한 안내도 이미지 경로. 대상이 아니면 null. */
    fun wayfindingImageAssetPath(facilityId: String): String? =
        IMAGE_ASSET_BY_FACILITY_ID[facilityId]?.let { "stairs/$it" }
}
