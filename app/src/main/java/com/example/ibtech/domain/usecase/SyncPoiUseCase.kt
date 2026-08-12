package com.example.ibtech.domain.usecase

import com.example.ibtech.domain.model.Facility
import com.example.ibtech.domain.model.FacilityDirection
import com.example.ibtech.domain.model.FacilitySyncStatus
import com.example.ibtech.domain.model.GuideMode

/**
 * 로컬 시설 목록을 temi가 보고한 최신 POI 목록과 맞춘다 (요구사항 명세서 6.2절).
 *
 * 순수 함수다 — 저장소 I/O는 [com.example.ibtech.data.repository.FacilityRepository]가 맡고,
 * 이 함수는 병합 규칙만 담당해 유닛 테스트로 신규/변경/삭제 3분기를 검증한다.
 *
 * 규칙:
 * - 신규(remote에만 있음): "미설정" 상태([Facility.UNSET_FLOOR], 안내 방식 동행+위치, 방향 정면,
 *   노출 켬)로 추가한다 — 관리자가 매번 똑같이 설정하지 않도록. 층이 여전히 미설정이라 노출을
 *   켜 둬도 관리자가 층을 지정하기 전까지는 [FacilityRepository.visibleFacilities]가 걸러내므로
 *   실제로 보이지 않는다.
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
                    floor = Facility.UNSET_FLOOR,
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
