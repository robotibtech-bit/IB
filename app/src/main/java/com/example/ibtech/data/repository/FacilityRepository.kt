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
     * `isEnabled == true && floor != UNSET_FLOOR`만 노출한다 — 동기화 중간 상태(미설정/신규/
     * 삭제 확인 대기)는 이용자에게 보이지 않는다(6.2절).
     */
    val visibleFacilities: Flow<List<Facility>> = allFacilities.map { facilities ->
        facilities
            .filter { it.isEnabled && it.floor != Facility.UNSET_FLOOR }
            .sortedBy { it.sortOrder }
    }

    /** temi가 보고한 최신 POI 이름 목록으로 로컬 시설 목록을 동기화한다(6.2절). */
    suspend fun syncWithRobot(remotePois: List<String>) {
        val current = allFacilities.first()
        val merged = SyncPoiUseCase(current, remotePois)
        save(merged)
    }

    suspend fun getFacility(id: String): Facility? =
        allFacilities.first().firstOrNull { it.id == id }

    /**
     * 개발자 메뉴 전용 (`BuildConfig.DEBUG`, 로드맵 5단계): 관리자 화면(10단계)이 아직 없어
     * Fake 모드 POI가 항상 "미설정" 상태로 숨겨지는 문제를 우회한다. 이미 있는 POI면 관리자
     * 메타데이터만 갱신하고, 없으면 곧바로 노출 가능한 상태로 만든다 — 개발자가 굳이 먼저
     * `facility_list`를 열어 동기화를 유도할 필요가 없게 한다.
     */
    suspend fun configureForDevMode(poiName: String, floor: Int, guideMode: GuideMode) {
        val current = allFacilities.first()
        val updated = if (current.any { it.sourcePoiName == poiName }) {
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
        save(updated)
    }

    private suspend fun save(facilities: List<Facility>) {
        dataStore.edit { prefs ->
            prefs[FacilityDataStoreKeys.FACILITIES_JSON] = FacilityJsonMapper.toJson(facilities)
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
