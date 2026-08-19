package com.example.ibtech.data.repository

import com.example.ibtech.domain.model.BasementStairsWayfindingOverride
import com.example.ibtech.domain.model.ElevatorWayfindingOverride
import com.example.ibtech.domain.model.Facility
import com.example.ibtech.domain.model.FacilityDirection
import com.example.ibtech.domain.model.FacilitySyncStatus
import com.example.ibtech.domain.model.GuideMode
import com.example.ibtech.domain.model.StairsWayfindingOverride
import com.example.ibtech.domain.model.WayfindingCorridorOverride

/**
 * 앱 최초 설치 시 등록해 두는 신트리도서관 주요 시설 디폴트 데이터 (POI GOTO 경로 처리 요구사항
 * 9절 "관리자가 시설을 하나씩 처음부터 입력하지 않아도 되는 구조").
 *
 * [FacilityRepository.ensureSeeded]가 저장소가 비어 있을 때 1회만 이 목록을 채운다 — 여기서
 * 만드는 건 앱 내부 시설 데이터일 뿐, Temi 지도에 실제 POI를 만들지는 않는다(요구사항 9절).
 * 관리자가 temi 지도에 같은 이름의 POI를 등록하면 [SyncPoiUseCase]가 이 데이터와 자동으로
 * 맞춰진다(`sourcePoiName` 기준 매칭) — 아직 등록 전이면 [FacilitySyncStatus.NOT_FOUND_ON_TEMI]로
 * "위치 정보 없음" 배지만 뜨고 설정은 그대로 남는다.
 *
 * 시설명은 [WayfindingCorridorOverride]/[ElevatorWayfindingOverride]/[StairsWayfindingOverride]/
 * [BasementStairsWayfindingOverride]의 `facilityIds()`를 그대로 가져다 쓴다 — 같은 이름을 이 파일에
 * 다시 하드코딩하지 않는다.
 *
 * 층수는 실제로 근거가 있는 곳만 채운다(strings.xml 이용시간 안내표, override 파일들의 문서
 * 주석 — "1층 일부 시설", "지하 1층을 나타내는 값으로 -1", 사용자가 직접 확인해 준 나머지
 * 층수) — 근거 없는 층수를 지어내지 않는다.
 */
object DefaultFacilityContent {

    /** [StairsWayfindingOverride]/[ElevatorWayfindingOverride] 대상의 실제 층수(사용자 확인,
     * 2026-08-19). strings.xml 이용시간 안내표 근거인 열람실·자료실 계열 외 나머지는 사용자가
     * 직접 알려준 값이다. */
    private val KNOWN_FLOOR_BY_NAME = mapOf(
        "1열람실" to 2,
        "2열람실" to 2,
        "3열람실" to 2,
        "4열람실" to 3,
        "디지털자료실" to 2,
        "종합자료실" to 3,
        "쉬어yo" to 2,
        "모두공간" to 2,
        "온마을스튜디오" to 2,
        "참고도서" to 3,
        "청소년상담실" to 3,
        "통합사무실" to 4,
        "가을강의실" to 4,
        "겨울강의실" to 4,
        "동아리실2" to 4
    )

    /** 사용자가 확인해 준 시설별 실제 방향(2026-08-19) — 여기 없는 시설은 기본값인 정면을 쓴다. */
    private val KNOWN_DIRECTION_BY_NAME: Map<String, FacilityDirection> = buildMap {
        listOf(
            "동아리실1", "여름강의실", "모두공간", "2열람실", "참고도서",
            "기계실전기실", "청소년상담실", "겨울강의실", "가을강의실", "동아리실2"
        ).forEach { put(it, FacilityDirection.LEFT) }
        listOf(
            "뉴스콕", "문서고", "관리과", "3열람실", "디지털자료실",
            "온마을스튜디오", "통합사무실"
        ).forEach { put(it, FacilityDirection.RIGHT) }
    }

    private fun directionFor(name: String) = KNOWN_DIRECTION_BY_NAME[name] ?: FacilityDirection.FRONT

    fun build(): List<Facility> {
        var order = 0
        fun next() = order++

        // 1순위: WayfindingCorridorOverride 대상 — "1층 일부 시설"(문서 주석 근거).
        val corridor = WayfindingCorridorOverride.facilityIds().map { name ->
            defaultFacility(name, floor = 1, sortOrder = next(), direction = directionFor(name))
        }
        // strings.xml 이용시간표에 명시된 1층 시설.
        val kidsRoom = listOf(defaultFacility("어린이자료실", floor = 1, sortOrder = next(), direction = directionFor("어린이자료실")))
        // 계단 대상 — 실제 로봇 이동은 연결통로 경유(사용자 확인, 2026-08-18).
        val stairs = StairsWayfindingOverride.facilityIds().map { name ->
            defaultFacility(
                name,
                floor = KNOWN_FLOOR_BY_NAME[name] ?: Facility.UNSET_FLOOR,
                sortOrder = next(),
                direction = directionFor(name)
            )
        }
        // 지하계단 대상 — "지하 1층을 나타내는 값으로 -1"(문서 주석 근거).
        val basement = BasementStairsWayfindingOverride.facilityIds().map { name ->
            defaultFacility(name, floor = -1, sortOrder = next(), direction = directionFor(name))
        }
        // 엘리베이터 대상.
        val elevator = ElevatorWayfindingOverride.facilityIds().map { name ->
            defaultFacility(
                name,
                floor = KNOWN_FLOOR_BY_NAME[name] ?: Facility.UNSET_FLOOR,
                sortOrder = next(),
                direction = directionFor(name)
            )
        }

        return corridor + kidsRoom + stairs + basement + elevator
    }

    /** [SyncPoiUseCase]가 신규 POI에 쓰는 기본값(안내 방식 동행+위치, 노출 켬)과 맞춰 둔다 —
     * 디폴트로 등록된 시설과 나중에 새로 동기화되는 시설이 다르게 보이지 않도록. 방향은
     * [KNOWN_DIRECTION_BY_NAME]에 근거가 있는 시설만 좌/우로 두고 나머지는 정면이 기본값이다.
     * [FacilitySyncStatus.NOT_FOUND_ON_TEMI]로 시작한다 — 아직 실제 temi POI와 맞춰본 적이
     * 없으므로, 확인 전까지는 "위치 정보 없음"으로 정직하게 표시하고 실제 동기화([SyncPoiUseCase])가
     * 일어나는 순간 SYNCED로 자동 갱신된다. */
    private fun defaultFacility(
        name: String,
        floor: Int,
        sortOrder: Int,
        direction: FacilityDirection = FacilityDirection.FRONT
    ) = Facility(
        id = name,
        sourcePoiName = name,
        name = name,
        floor = floor,
        guideMode = GuideMode.BOTH,
        direction = direction,
        isEnabled = true,
        sortOrder = sortOrder,
        syncStatus = FacilitySyncStatus.NOT_FOUND_ON_TEMI
    )
}
