package com.example.ibtech.domain.model

/**
 * 엘리베이터를 이용해야 하는 일부 2층 시설은 로봇이 엘리베이터 앞(도착 층)까지 안내한 뒤,
 * 그림 안내도로 나머지 길을 짚어준다 — [StairsWayfindingOverride](계단 전용)와 같은 방식이되
 * 계단이 아니라 엘리베이터를 이용한다는 점이 다르다. 문구는 이미 있는 기본 층간 안내 문구
 * ("엘리베이터를 이용해 N층으로 올라가서/내려가서 [방향]으로 이동하시면 있습니다",
 * [com.example.ibtech.ui.facility.buildFloorDirectionGuideText])가 그대로 맞아 별도 문구
 * 분기가 필요 없다 — 여기서는 도착 후 보여줄 안내도 이미지만 판단한다.
 *
 * [Facility.id](= [Facility.sourcePoiName], 관리자가 temi 지도에 등록한 실제 POI 이름) 기준
 * 매핑이다. 안내도 파일명과 실제 POI 이름이 다를 수 있어([StairsWayfindingOverride]의
 * "3열람실(스마트)" 사례처럼) 사용자가 로봇 지도에서 직접 확인한 이름을 키로 썼다 — "모두공간+"는
 * 지도에 "모두공간"으로, "4열람실(스마트)"는 "4열람실"로 등록돼 있다(사용자 확인, 2026-08-13).
 * 나머지는 아직 동기화된 POI 목록에 없어 확인하지 못했다 — 새 POI가 동기화된 뒤 관리자 화면
 * 표시명이 아래 키와 다르면 그 값으로 고쳐야 한다.
 */
object ElevatorWayfindingOverride {

    private val IMAGE_ASSET_BY_FACILITY_ID = mapOf(
        "4열람실" to "4열람실(스마트).png",
        "가을강의실" to "가을강의실.png",
        "겨울강의실" to "겨울강의실.png",
        "동아리실2" to "동아리실2.png",
        "디지털자료실" to "디지털자료실.png",
        "모두공간" to "모두공간+.png",
        "온마을스튜디오" to "온마을스튜디오.png",
        "종합자료실" to "종합자료실.png",
        "참고도서" to "참고도서.png",
        "청소년상담실" to "청소년상담실.png",
        "통합사무실" to "통합사무실.png"
    )

    fun appliesTo(facilityId: String): Boolean = facilityId in IMAGE_ASSET_BY_FACILITY_ID

    /** 이 override가 다루는 시설 id 전체 — [com.example.ibtech.data.repository.DefaultFacilityContent]가
     * 디폴트 시설을 만들 때 같은 이름을 다시 하드코딩하지 않고 여기서 가져다 쓴다. */
    fun facilityIds(): Set<String> = IMAGE_ASSET_BY_FACILITY_ID.keys

    /** `assets/elevator/`를 기준으로 한 안내도 이미지 경로. 대상이 아니면 null. */
    fun wayfindingImageAssetPath(facilityId: String): String? =
        IMAGE_ASSET_BY_FACILITY_ID[facilityId]?.let { "elevator/$it" }
}
