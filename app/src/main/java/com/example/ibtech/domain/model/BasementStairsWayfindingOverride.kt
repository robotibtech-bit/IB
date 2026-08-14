package com.example.ibtech.domain.model

/**
 * 지하 1층 일부 시설은 [StairsWayfindingOverride](2층 열람실행 계단)와 **같은 계단**(여자화장실
 * 옆)을 쓴다 — 방향만 반대다(사용자 확인: "1열람실을 안내할 때와 똑같은 루틴으로 안내해야해
 * 층만 지하로 바뀐거야 1층 여름강의실 옆 계단을 이용하는거"). 그래서 안내도 이미지도
 * [StairsWayfindingOverride]와 동일하게 계단 앞까지 가는 길을 보여주는 공용 이미지
 * ([com.example.ibtech.ui.facility.wayfindingImageAssetPaths]의 STAIRS_LANDING_IMAGE_PATH)를
 * 먼저 보여주고 이 시설 전용 지하 안내도를 이어 붙인다. 안내 문구만 "올라가셔서" 대신
 * "내려가셔서"를 쓰도록 별도로 뒀다("여름강의실 옆에 있는 계단을 이용해 지하 1층으로
 * 내려가셔서 …", [com.example.ibtech.ui.facility.buildFloorDirectionGuideText]).
 *
 * [Facility.floor]는 지하 1층을 나타내는 값으로 `-1`을 쓴다(관리자가 시설 편집에서 직접 입력) —
 * 층수 배지([com.example.ibtech.ui.facility.formatFloorBadge])가 이 경우 "지하 1층"으로 보여준다.
 *
 * "보존서고2"는 원본 안내도 3장 중 하나에 포함돼 있었지만, 이미 [WayfindingCorridorOverride]에
 * 1층 연결통로 시설로 등록돼 있어(실제 운영 결정: 1층 등록을 유지) 여기서는 제외했다 — 이미지
 * 파일도 assets에 넣지 않았다.
 */
object BasementStairsWayfindingOverride {

    private val IMAGE_ASSET_BY_FACILITY_ID = mapOf(
        "기계실전기실" to "기계실전기실.png",
        "도시락존" to "도시락존.png"
    )

    fun appliesTo(facilityId: String): Boolean = facilityId in IMAGE_ASSET_BY_FACILITY_ID

    /** `assets/basement/`를 기준으로 한 안내도 이미지 경로. 대상이 아니면 null. */
    fun wayfindingImageAssetPath(facilityId: String): String? =
        IMAGE_ASSET_BY_FACILITY_ID[facilityId]?.let { "basement/$it" }
}
