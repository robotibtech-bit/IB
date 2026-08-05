package com.example.ibtech.ui.facility

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.example.ibtech.data.repository.FacilityRepository
import com.example.ibtech.data.repository.SettingsRepository
import com.example.ibtech.domain.model.Facility
import com.example.ibtech.robot.TemiConnectionState
import com.example.ibtech.robot.TemiControllerProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

/**
 * 시설 목록 화면 상태 (요구사항 명세서 2.3절, 12단계 개편).
 *
 * [featuredFacilities]는 관리자가 "대표 장소"로 표시하고([Facility.isFeatured]) 관리자 설정
 * 개수만큼([com.example.ibtech.domain.model.LibrarySettings.featuredFacilityCount]) 자른 목록,
 * [allFacilities]는 검색어로 걸러진 노출 가능한 시설 전체다. [hasAnyFacility]는 검색어와 무관하게
 * 노출 가능한 시설이 하나라도 있는지를 나타낸다 — "등록된 시설이 없습니다"(전체 빈 데이터)와
 * "검색 결과가 없습니다"(필터링 결과 없음)를 구분하기 위해 둔다.
 */
data class FacilityListUiState(
    val isLoaded: Boolean = false,
    val featuredFacilities: List<Facility> = emptyList(),
    val allFacilities: List<Facility> = emptyList(),
    val hasAnyFacility: Boolean = false,
    val query: String = "",
    val isSearchVisible: Boolean = false
)

class FacilityListViewModel(
    application: Application,
    savedStateHandle: SavedStateHandle
) : AndroidViewModel(application) {

    private val facilityRepository = FacilityRepository.getInstance(application)
    private val settingsRepository = SettingsRepository.getInstance(application)
    private val temiController = TemiControllerProvider.current

    private val initialQuery = savedStateHandle.get<String>("query").orEmpty()
    private val queryState = MutableStateFlow(initialQuery)
    private val searchVisibleState = MutableStateFlow(initialQuery.isNotBlank())

    val uiState: StateFlow<FacilityListUiState> = combine(
        facilityRepository.visibleFacilities,
        settingsRepository.settings,
        queryState,
        searchVisibleState
    ) { facilities, settings, query, searchVisible ->
        val featured = facilities
            .filter { it.isFeatured }
            .sortedBy { it.sortOrder }
            .take(settings.featuredFacilityCount)
        val filtered = if (query.isBlank()) {
            facilities
        } else {
            facilities.filter { it.name.contains(query, ignoreCase = true) }
        }
        FacilityListUiState(
            isLoaded = true,
            featuredFacilities = featured,
            allFacilities = filtered,
            hasAnyFacility = facilities.isNotEmpty(),
            query = query,
            isSearchVisible = searchVisible
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = FacilityListUiState(query = initialQuery, isSearchVisible = initialQuery.isNotBlank())
    )

    init {
        // temi 연결 상태가 Ready가 된 시점, 그리고 이후 POI 목록이 바뀔 때마다 다시 동기화한다
        // (요구사항 6.2절 "추가·이름 변경·삭제되면 동기화 후 목록에 반영").
        temiController.connectionState
            .combine(temiController.locations) { state, locations -> state to locations }
            .distinctUntilChanged()
            .onEach { (state, locations) ->
                if (state is TemiConnectionState.Ready) {
                    facilityRepository.syncWithRobot(locations)
                }
            }
            .launchIn(viewModelScope)
    }

    fun onQueryChange(newQuery: String) {
        queryState.value = newQuery
    }

    fun onToggleSearch() {
        searchVisibleState.update { visible ->
            val next = !visible
            if (!next) queryState.value = ""
            next
        }
    }
}
