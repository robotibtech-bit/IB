package com.example.ibtech.ui.admin

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ibtech.data.repository.FacilityRepository
import com.example.ibtech.domain.model.Facility
import com.example.ibtech.robot.TemiControllerProvider
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** 시설 관리 목록 화면 상태 (로드맵 10단계). */
data class FacilityAdminUiState(
    val isLoaded: Boolean = false,
    val facilities: List<Facility> = emptyList()
)

/**
 * [onRefreshPoi]는 `refreshLocations()`로 SDK에 새 조회만 요청한다 — 실제 목록은 비동기로
 * [com.example.ibtech.robot.TemiController.locations]에 반영되므로, 이 화면이 살아있는 동안
 * 그 흐름을 계속 관찰하며 변경될 때마다 [FacilityRepository.syncWithRobot]으로 병합한다
 * (6단계 `SyncPoiUseCase`와 동일한 로직 재사용).
 */
class FacilityAdminViewModel(application: Application) : AndroidViewModel(application) {

    private val facilityRepository = FacilityRepository.getInstance(application)

    val uiState: StateFlow<FacilityAdminUiState> = facilityRepository.allFacilities
        .map { facilities ->
            // 이용자 목록([FacilityRepository.visibleFacilities])과 같은 기준으로 맞춘다(요청:
            // "관리자 페이지 시설관리 위치도 층순으로" / "1234층 다음에 지하1층이 표시되게") —
            // 미설정(Facility.floorSortGroup 0) 시설이 가장 앞에 모여 관리자 눈에 먼저 띄고, 그
            // 다음 지상 층 오름차순, 마지막이 지하 층이다.
            val sorted = facilities.sortedWith(
                compareBy(
                    { Facility.floorSortGroup(it.floor) },
                    { Facility.floorSortValue(it.floor) },
                    { it.sortOrder }
                )
            )
            FacilityAdminUiState(isLoaded = true, facilities = sorted)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = FacilityAdminUiState()
        )

    init {
        viewModelScope.launch {
            TemiControllerProvider.current.locations.collect { locations ->
                if (locations.isNotEmpty()) {
                    facilityRepository.syncWithRobot(locations)
                }
            }
        }
    }

    fun onRefreshPoi() {
        TemiControllerProvider.current.refreshLocations()
    }

    fun onDeleteFacility(id: String) {
        viewModelScope.launch { facilityRepository.deleteFacility(id) }
    }
}
