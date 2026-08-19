package com.example.ibtech.ui.facility

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.example.ibtech.data.repository.FacilityRepository
import com.example.ibtech.data.repository.SettingsRepository
import com.example.ibtech.domain.model.Facility
import com.example.ibtech.domain.model.LibrarySettings
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
import kotlinx.coroutines.launch

/**
 * 시설 목록 화면 상태 (요구사항 명세서 2.3절, 12단계 개편).
 *
 * [featuredFacilities]는 관리자가 "대표 장소"로 표시하고([Facility.isFeatured]) 관리자 설정
 * 개수만큼([com.example.ibtech.domain.model.LibrarySettings.featuredFacilityCount]) 자른 목록,
 * [allFacilities]는 검색어로 걸러진 노출 가능한 시설 전체다. [hasAnyFacility]는 검색어와 무관하게
 * 노출 가능한 시설이 하나라도 있는지를 나타낸다 — "등록된 시설이 없습니다"(전체 빈 데이터)와
 * "검색 결과가 없습니다"(필터링 결과 없음)를 구분하기 위해 둔다.
 *
 * [knownLocations]/[baseFloor]는 [FacilityCard]가 "위치 정보 없음" 배지를 판단할 때 쓴다 —
 * POI GOTO 경로 처리 요구사항 이후로는 시설 자체의 `sourcePoiName`이 아니라 실제 이동 대상
 * (엘리베이터/연결통로일 수 있음)이 temi 지도에 있는지를 봐야 정확하다.
 */
data class FacilityListUiState(
    val isLoaded: Boolean = false,
    val featuredFacilities: List<Facility> = emptyList(),
    val allFacilities: List<Facility> = emptyList(),
    val hasAnyFacility: Boolean = false,
    val query: String = "",
    val isSearchVisible: Boolean = false,
    val knownLocations: List<String> = emptyList(),
    val baseFloor: Int = LibrarySettings.DEFAULT_BASE_FLOOR
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
    // 대표 장소(자주 쓰는 곳) 그리드 화면은 일단 막아뒀다(요구사항: "시설안내 버튼을 누르면
    // 바로 다른 장소 찾기 화면으로") — 시설 목록에 들어오면 항상 전체 검색 목록부터 보여준다.
    // [FacilityListScreen]의 FEATURED_FACILITY_GRID_ENABLED와 함께 되돌리면 원래대로 복구된다.
    private val searchVisibleState = MutableStateFlow(true)

    val uiState: StateFlow<FacilityListUiState> = combine(
        facilityRepository.visibleFacilities,
        settingsRepository.settings,
        queryState,
        searchVisibleState,
        temiController.locations
    ) { facilities, settings, query, searchVisible, locations ->
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
            isSearchVisible = searchVisible,
            knownLocations = locations,
            baseFloor = settings.baseFloor
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = FacilityListUiState(query = initialQuery, isSearchVisible = true)
    )

    init {
        viewModelScope.launch {
            // 디폴트 시설 데이터(요구사항 9절)를 실제 POI 동기화보다 먼저 채운다 — 순서를
            // 보장해야 두 작업이 같은 id로 경쟁하지 않는다(FacilityRepository.ensureSeeded 참고).
            facilityRepository.ensureSeeded()

            // temi 연결 상태가 Ready가 된 시점, 그리고 이후 POI 목록이 바뀔 때마다 다시
            // 동기화한다(요구사항 6.2절 "추가·이름 변경·삭제되면 동기화 후 목록에 반영").
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
