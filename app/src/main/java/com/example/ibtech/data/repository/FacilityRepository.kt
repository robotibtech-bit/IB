package com.example.ibtech.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.example.ibtech.data.datastore.FacilityDataStoreKeys
import com.example.ibtech.data.datastore.facilityDataStore
import com.example.ibtech.domain.model.Facility
import com.example.ibtech.domain.model.FacilitySyncStatus
import com.example.ibtech.domain.model.GuideMode
import com.example.ibtech.domain.usecase.SyncPoiUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * [Facility] 목록의 단일 접근 지점 (요구사항 명세서 2.3절, 6.2절).
 *
 * [SettingsRepository]와 같은 이유로 싱글턴 + 인터페이스 없는 직접 클래스로 둔다. 저장 방식은
 * 이 클래스 안에만 캡슐화되어 있으므로, 이후 Room으로 바꾸더라도 [FacilityRepository]를
 * 참조하는 ViewModel 코드는 바뀌지 않는다.
 */
class FacilityRepository private constructor(
    private val dataStore: DataStore<Preferences>
) {

    /** 관리자 메타데이터를 포함한 전체 목록(미설정/비활성 포함). 10단계 관리자 화면이 구독한다. */
    val allFacilities: Flow<List<Facility>> = dataStore.data.map { prefs ->
        FacilityJsonMapper.fromJson(prefs[FacilityDataStoreKeys.FACILITIES_JSON].orEmpty())
    }

    /**
     * 이용자 화면(`facility_list`)이 구독하는 목록.
     * `isEnabled == true && floor != UNSET_FLOOR`만 노출한다 — "미설정"(신규 POI로 아직 관리자가
     * 층/노출을 설정하지 않은 상태)은 이용자에게 보이지 않는다(6.2절).
     *
     * [FacilitySyncStatus.NOT_FOUND_ON_TEMI](Temi 지도에서 더 이상 조회되지 않지만 관리자가 아직
     * 삭제하지 않은 상태)는 이미 `isEnabled == true`로 설정돼 있었다면 이 필터를 그대로 통과한다
     * — 의도적으로 그렇다. [com.example.ibtech.ui.facility.FacilityListScreen]의 `FacilityCard`가
     * 이 상태를 "위치 정보 없음" 배지로 보여주는 것을 전제로 하므로, 여기서 걸러내면 그 배지
     * 기능이 죽는다.
     */
    val visibleFacilities: Flow<List<Facility>> = allFacilities.map { facilities ->
        facilities
            .filter { it.isEnabled && it.floor != Facility.UNSET_FLOOR }
            // 지상 층 오름차순(1, 2, 3…) 다음에 지하 층(지하1, 지하2…)이 오고, 같은 층 안에서는
            // 관리자가 지정한 정렬 순서(sortOrder)를 따른다(요청: "1234층 다음에 지하1층이
            // 표시되게") — floor 원값 그대로 오름차순 정렬하면 음수인 지하 층이 맨 앞에 온다.
            .sortedWith(
                compareBy(
                    { Facility.floorSortGroup(it.floor) },
                    { Facility.floorSortValue(it.floor) },
                    { it.sortOrder }
                )
            )
    }

    /** temi가 보고한 최신 POI 이름 목록으로 로컬 시설 목록을 동기화한다(6.2절). */
    suspend fun syncWithRobot(remotePois: List<String>) {
        readModifyWrite { current -> SyncPoiUseCase(current, remotePois) }
    }

    /** 저장된 시설이 없으면(최초 실행) [DefaultFacilityContent]로 1회 채운다. 이미 시드했으면
     * 아무 것도 하지 않는다 — "목록이 비었는지"가 아니라 별도 플래그로 판단한다([syncWithRobot]과
     * 동시에 일어나 목록이 먼저 채워지는 경쟁 상태를 피하기 위해, [UsageRepository.ensureSeeded]와
     * 다른 방식을 쓴다). 이미 같은 id로 등록된 시설(먼저 끝난 동기화 등)은 덮어쓰지 않는다. */
    suspend fun ensureSeeded() {
        dataStore.edit { prefs ->
            if (prefs[FacilityDataStoreKeys.DEFAULT_FACILITIES_SEEDED] == true) return@edit

            val current = FacilityJsonMapper.fromJson(prefs[FacilityDataStoreKeys.FACILITIES_JSON].orEmpty())
            val existingIds = current.map { it.id }.toSet()
            val defaults = DefaultFacilityContent.build().filterNot { it.id in existingIds }

            prefs[FacilityDataStoreKeys.FACILITIES_JSON] = FacilityJsonMapper.toJson(current + defaults)
            prefs[FacilityDataStoreKeys.DEFAULT_FACILITIES_SEEDED] = true
        }
    }

    suspend fun getFacility(id: String): Facility? =
        allFacilities.first().firstOrNull { it.id == id }

    /** 관리자 화면(10단계)이 표시명·층·설명·안내방식·아이콘·노출여부·순서를 저장할 때 쓴다. */
    suspend fun updateFacility(facility: Facility) {
        readModifyWrite { current ->
            current.map { if (it.id == facility.id) facility else it }
        }
    }

    /** [FacilitySyncStatus.NOT_FOUND_ON_TEMI] 시설을 관리자가 확인 후 제거할 때 쓴다. */
    suspend fun deleteFacility(id: String) {
        readModifyWrite { current -> current.filterNot { it.id == id } }
    }

    /** [BackupRepository] 복구 전용: 백업 목록으로 전체를 대체한다. */
    suspend fun replaceAll(facilities: List<Facility>) {
        save(facilities)
    }

    /**
     * 개발자 메뉴 전용 (`BuildConfig.DEBUG`, 로드맵 5단계): 관리자 화면(10단계)이 아직 없어
     * Fake 모드 POI가 항상 "미설정" 상태로 숨겨지는 문제를 우회한다. 이미 있는 POI면 관리자
     * 메타데이터만 갱신하고, 없으면 곧바로 노출 가능한 상태로 만든다 — 개발자가 굳이 먼저
     * `facility_list`를 열어 동기화를 유도할 필요가 없게 한다.
     */
    suspend fun configureForDevMode(poiName: String, floor: Int, guideMode: GuideMode) {
        readModifyWrite { current ->
            if (current.any { it.sourcePoiName == poiName }) {
                current.map { facility ->
                    if (facility.sourcePoiName == poiName) {
                        facility.copy(floor = floor, guideMode = guideMode, isEnabled = true)
                    } else {
                        facility
                    }
                }
            } else {
                current + Facility(
                    id = poiName,
                    sourcePoiName = poiName,
                    name = poiName,
                    floor = floor,
                    guideMode = guideMode,
                    isEnabled = true,
                    syncStatus = FacilitySyncStatus.SYNCED
                )
            }
        }
    }

    private suspend fun save(facilities: List<Facility>) {
        dataStore.edit { prefs ->
            prefs[FacilityDataStoreKeys.FACILITIES_JSON] = FacilityJsonMapper.toJson(facilities)
        }
    }

    /**
     * 읽기→계산→쓰기를 하나의 DataStore 트랜잭션 안에서 수행한다.
     *
     * 기존에는 `allFacilities.first()`로 바깥에서 읽은 뒤 별도의 `dataStore.edit{}`로 저장했다.
     * 관리자 저장과 Temi POI 자동 동기화가 동시에 일어나면 둘 다 같은 옛 목록을 읽고 나중
     * 저장이 앞 저장을 조용히 덮어쓸 수 있었다. `DataStore.edit{}`의 transform 블록은 항상
     * 직전까지 커밋된 최신 Preferences를 받고 순차적으로만 실행되므로, 읽기와 쓰기를 같은
     * 블록 안에 두면 두 저장이 서로의 변경을 덮어쓰지 않는다.
     */
    private suspend fun readModifyWrite(transform: (List<Facility>) -> List<Facility>) {
        dataStore.edit { prefs ->
            val current = FacilityJsonMapper.fromJson(prefs[FacilityDataStoreKeys.FACILITIES_JSON].orEmpty())
            prefs[FacilityDataStoreKeys.FACILITIES_JSON] = FacilityJsonMapper.toJson(transform(current))
        }
    }

    companion object {
        @Volatile
        private var instance: FacilityRepository? = null

        fun getInstance(context: Context): FacilityRepository =
            instance ?: synchronized(this) {
                instance ?: FacilityRepository(context.applicationContext.facilityDataStore)
                    .also { instance = it }
            }
    }
}
