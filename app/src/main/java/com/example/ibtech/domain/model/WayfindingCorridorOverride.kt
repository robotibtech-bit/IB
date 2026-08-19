package com.example.ibtech.domain.model

/**
 * 1층 일부 시설은 로봇이 실제 문 앞까지 들어갈 수 없어(또는 그렇게 하지 않기로 정해서),
 * 동행 안내를 공용 연결통로 지점까지만 하고, 그 뒤로는 그림 안내도를 보여줘서 이용자가
 * 직접 찾아가게 한다.
 *
 * 이 지점 이동은 별도 리다이렉트가 아니라 [Facility.sourcePoiName] 자체로 처리한다 — 관리자가
 * temi 지도에서 이 POI들 각각의 실제 좌표를 연결통로 지점과 같은 곳으로 등록해 두기
 * 때문이다(더 이상 "연결통로"라는 별도 이름의 공용 POI는 없다). 그래서 `goTo(facility.
 * sourcePoiName)`을 그대로 호출하면 알아서 그 지점으로 이동한다 — 여기서는 도착 후 "안내도
 * 이미지를 보여줄 대상인지"만 [Facility.id] 기준으로 판단한다.
 *
 * "화장실"은 목록에서 제외했다 — 연결통로 쪽 화장실 대신 주출입구 쪽 메인 POI 화장실로
 * 안내하기로 해서, 안내도 이미지도 함께 삭제했다.
 */
object WayfindingCorridorOverride {

    /** [Facility.id] → `assets/wayfinding/` 안의 안내도 이미지 파일명. */
    private val IMAGE_ASSET_BY_FACILITY_ID = mapOf(
        "동아리실1" to "01_동아리실1.png",
        "여름강의실" to "02_여름강의실.png",
        "뉴스콕" to "03_뉴스콕.png",
        "문서고" to "04_문서고.png",
        "관리과" to "05_관리과.png",
        "보존서고2" to "06_보존서고2.png",
        "순회문고" to "07_순회문고.png",
        "시설관리실" to "08_시설관리실.png",
        "당직실" to "09_당직실.png",
        "관장실" to "10_관장실.png"
    )

    fun appliesTo(facilityId: String): Boolean = facilityId in IMAGE_ASSET_BY_FACILITY_ID

    /** 이 override가 다루는 시설 id 전체 — [com.example.ibtech.data.repository.DefaultFacilityContent]가
     * 디폴트 시설을 만들 때 같은 이름을 다시 하드코딩하지 않고 여기서 가져다 쓴다. */
    fun facilityIds(): Set<String> = IMAGE_ASSET_BY_FACILITY_ID.keys

    /** `assets/wayfinding/`를 기준으로 한 안내도 이미지 경로. 대상이 아니면 null. */
    fun wayfindingImageAssetPath(facilityId: String): String? =
        IMAGE_ASSET_BY_FACILITY_ID[facilityId]?.let { "wayfinding/$it" }
}
