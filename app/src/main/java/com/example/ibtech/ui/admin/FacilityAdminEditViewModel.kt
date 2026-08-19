package com.example.ibtech.ui.admin

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.example.ibtech.R
import com.example.ibtech.data.repository.FacilityRepository
import com.example.ibtech.data.repository.SettingsRepository
import com.example.ibtech.domain.model.Facility
import com.example.ibtech.domain.model.FacilityDirection
import com.example.ibtech.domain.model.GuideMode
import com.example.ibtech.domain.model.LibrarySettings
import com.example.ibtech.domain.model.WayfindingCorridorOverride
import com.example.ibtech.domain.usecase.FacilityAdminValidation
import com.example.ibtech.domain.usecase.ValidateFacilityAdminInputUseCase
import com.example.ibtech.robot.TemiControllerProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** 시설 편집 화면 상태 (로드맵 10단계). */
data class FacilityAdminEditUiState(
    val isLoaded: Boolean = false,
    val found: Boolean = false,
    val name: String = "",
    val floorText: String = "",
    val description: String = "",
    val guideMode: GuideMode = GuideMode.LOCATION_ONLY,
    val direction: FacilityDirection? = null,
    /** 연결통로 안내도 대상 시설([WayfindingCorridorOverride])은 기준층이어도 방향을 고를 수
     * 있다(요구사항: "기준층이 아닌 목적지와 동일하게 방향을 선택"). */
    val allowDirectionOnBaseFloor: Boolean = false,
    val baseFloor: Int = LibrarySettings.DEFAULT_BASE_FLOOR,
    /** 관리자가 이 시설의 실제 이동 목적지를 직접 지정한 값(null이면 자동 판단 — 요청:
     * "위치이름과 가야할곳이 디폴트로는 같은곳이 되고, 사용자가 추가로 가야할곳을 다른곳으로
     * 지정할수있게"). [com.example.ibtech.domain.usecase.ResolveNavigationTargetUseCase] 참고. */
    val navigationTargetOverride: String? = null,
    /** 드롭다운에 고를 수 있게 보여줄, 현재 temi 지도에 등록된 POI 이름 목록. */
    val knownLocations: List<String> = emptyList(),
    val iconKey: String? = null,
    val isEnabled: Boolean = false,
    val isFeatured: Boolean = false,
    val sortOrderText: String = "0",
    val nameError: String? = null,
    val floorError: String? = null,
    val saveCompleted: Boolean = false
)

class FacilityAdminEditViewModel(
    application: Application,
    savedStateHandle: SavedStateHandle
) : AndroidViewModel(application) {

    private val facilityId: String = checkNotNull(savedStateHandle["facilityId"]) {
        "facility_admin_edit 라우트에 facilityId 인자가 없습니다."
    }

    private val facilityRepository = FacilityRepository.getInstance(application)
    private val settingsRepository = SettingsRepository.getInstance(application)
    private val temiController = TemiControllerProvider.current

    /** 저장 시 여기 담긴 원본에 `.copy(...)`만 해서 [Facility.sourcePoiName]/[Facility.syncStatus]
     * 등 이 화면이 다루지 않는 필드를 그대로 보존한다. */
    private var loadedFacility: Facility? = null

    private val _uiState = MutableStateFlow(FacilityAdminEditUiState())
    val uiState: StateFlow<FacilityAdminEditUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val baseFloor = settingsRepository.settings.first().baseFloor
            val facility = facilityRepository.getFacility(facilityId)
            loadedFacility = facility
            _uiState.update {
                if (facility == null) {
                    it.copy(isLoaded = true, found = false, baseFloor = baseFloor)
                } else {
                    it.copy(
                        isLoaded = true,
                        found = true,
                        name = facility.name,
                        floorText = if (facility.floor == Facility.UNSET_FLOOR) "" else facility.floor.toString(),
                        description = facility.shortDescription,
                        guideMode = facility.guideMode,
                        direction = facility.direction,
                        allowDirectionOnBaseFloor = WayfindingCorridorOverride.appliesTo(facilityId),
                        baseFloor = baseFloor,
                        navigationTargetOverride = facility.navigationTargetOverride,
                        knownLocations = temiController.locations.value,
                        iconKey = facility.iconKey,
                        isEnabled = facility.isEnabled,
                        isFeatured = facility.isFeatured,
                        sortOrderText = facility.sortOrder.toString()
                    )
                }
            }
        }
    }

    fun onNameChange(value: String) {
        _uiState.update { it.copy(name = value, nameError = null) }
    }

    fun onFloorChange(value: String) {
        _uiState.update { it.copy(floorText = value, floorError = null) }
    }

    fun onDescriptionChange(value: String) {
        _uiState.update { it.copy(description = value) }
    }

    fun onGuideModeChange(mode: GuideMode) {
        _uiState.update { it.copy(guideMode = mode) }
    }

    fun onDirectionChange(direction: FacilityDirection?) {
        _uiState.update { it.copy(direction = direction) }
    }

    fun onNavigationTargetOverrideChange(value: String?) {
        _uiState.update { it.copy(navigationTargetOverride = value) }
    }

    fun onIconKeyChange(key: String?) {
        _uiState.update { it.copy(iconKey = key) }
    }

    fun onEnabledChange(value: Boolean) {
        _uiState.update { it.copy(isEnabled = value) }
    }

    fun onFeaturedChange(value: Boolean) {
        _uiState.update { it.copy(isFeatured = value) }
    }

    fun onSortOrderChange(value: String) {
        _uiState.update { it.copy(sortOrderText = value) }
    }

    fun onSave() {
        val state = _uiState.value
        val original = loadedFacility ?: return
        val app = getApplication<Application>()

        when (val validation = ValidateFacilityAdminInputUseCase(state.name, state.floorText)) {
            is FacilityAdminValidation.BlankName -> {
                _uiState.update { it.copy(nameError = app.getString(R.string.admin_error_blank_name)) }
            }

            is FacilityAdminValidation.InvalidFloor -> {
                _uiState.update { it.copy(floorError = app.getString(R.string.admin_error_invalid_number)) }
            }

            is FacilityAdminValidation.Valid -> {
                val sortOrder = state.sortOrderText.trim().toIntOrNull() ?: 0
                val updated = original.copy(
                    name = state.name.trim(),
                    floor = validation.floor,
                    shortDescription = state.description.trim(),
                    guideMode = state.guideMode,
                    // 기준층으로 되돌리면 방향 값은 보통 의미가 없어지므로 함께 지운다 — 단,
                    // 연결통로 안내도 대상 시설(allowDirectionOnBaseFloor)은 기준층이어도 방향을
                    // 그대로 쓰므로 지우지 않는다.
                    direction = state.direction.takeIf {
                        validation.floor != state.baseFloor || state.allowDirectionOnBaseFloor
                    },
                    navigationTargetOverride = state.navigationTargetOverride,
                    iconKey = state.iconKey,
                    isEnabled = state.isEnabled,
                    isFeatured = state.isFeatured,
                    sortOrder = sortOrder
                )
                viewModelScope.launch {
                    facilityRepository.updateFacility(updated)
                    _uiState.update { it.copy(saveCompleted = true) }
                }
            }
        }
    }
}
