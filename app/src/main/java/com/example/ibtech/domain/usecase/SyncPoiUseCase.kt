package com.example.ibtech.domain.usecase

import com.example.ibtech.domain.model.Facility
import com.example.ibtech.domain.model.FacilityDirection
import com.example.ibtech.domain.model.FacilitySyncStatus
import com.example.ibtech.domain.model.GuideMode
import com.example.ibtech.domain.model.LibrarySettings

/**
 * 로컬 시설 목록을 temi가 보고한 최신 POI 목록과 맞춘다 (요구사항 명세서 6.2절).
 *
 * 순수 함수다 — 저장소 I/O는 [com.example.ibtech.data.repository.FacilityRepository]가 맡고,
 * 이 함수는 병합 규칙만 담당해 유닛 테스트로 신규/변경/삭제 3분기를 검증한다.
 *
 * 규칙:
 * - 신규(remote에만 있음): 기준층([LibrarySettings.DEFAULT_BASE_FLOOR], 안내 방식 동행+위치,
 *   방향 정면, 노출 켬)으로 추가한다 — 관리자가 temi 지도에 POI를 등록하면 별도 설정 없이도
 *   바로 이용자 화면에 노출된다(요청: "poi를 등록하면 자동으로... 1층으로 등록되게끔"). 실제
 *   층이 기준층이 아니면 관리자가 시설 관리에서 직접 고쳐야 한다 — 여기서는 "미설정으로 숨어
 *   있어 존재조차 모르는 상태"보다 "일단 보이되 층이 틀릴 수 있는 상태"를 택한 것이다.
 * - 기존(양쪽에 있음): 관리자 메타데이터(표시명/층/설명/안내방식/아이콘/노출여부/순서)는
 *   절대 덮어쓰지 않는다. `syncStatus`만 [FacilitySyncStatus.SYNCED]로 갱신한다.
 * - 사라짐(local에만 있음): 즉시 삭제하지 않고 [FacilitySyncStatus.NOT_FOUND_ON_TEMI]로 표시한다.
 */
object SyncPoiUseCase {

    operator fun invoke(local: List<Facility>, remote: List<String>): List<Facility> {
        val localByPoiName = local.associateBy { it.sourcePoiName }
        val remoteSet = remote.toSet()

        val merged = remote.map { poiName ->
            val existing = localByPoiName[poiName]
            if (existing != null) {
                existing.copy(syncStatus = FacilitySyncStatus.SYNCED)
            } else {
                Facility(
                    id = poiName,
                    sourcePoiName = poiName,
                    name = poiName,
                    floor = LibrarySettings.DEFAULT_BASE_FLOOR,
                    guideMode = GuideMode.BOTH,
                    direction = FacilityDirection.FRONT,
                    isEnabled = true,
                    syncStatus = FacilitySyncStatus.NEW
                )
            }
        }

        val disappeared = local
            .filterNot { it.sourcePoiName in remoteSet }
            .map { it.copy(syncStatus = FacilitySyncStatus.NOT_FOUND_ON_TEMI) }

        return merged + disappeared
    }
}
