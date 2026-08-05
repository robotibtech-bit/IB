package com.example.ibtech.ui.admin

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.example.ibtech.R
import com.example.ibtech.data.repository.FacilityRepository
import com.example.ibtech.domain.model.Facility
import com.example.ibtech.domain.model.GuideMode
import com.example.ibtech.domain.usecase.FacilityAdminValidation
import com.example.ibtech.domain.usecase.ValidateFacilityAdminInputUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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

    /** 저장 시 여기 담긴 원본에 `.copy(...)`만 해서 [Facility.sourcePoiName]/[Facility.syncStatus]
     * 등 이 화면이 다루지 않는 필드를 그대로 보존한다. */
    private var loadedFacility: Facility? = null

    private val _uiState = MutableStateFlow(FacilityAdminEditUiState())
    val uiState: StateFlow<FacilityAdminEditUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val facility = facilityRepository.getFacility(facilityId)
            loadedFacility = facility
            _uiState.update {
                if (facility == null) {
                    it.copy(isLoaded = true, found = false)
                } else {
                    it.copy(
                        isLoaded = true,
                        found = true,
                        name = facility.name,
                        floorText = if (facility.floor == Facility.UNSET_FLOOR) "" else facility.floor.toString(),
                        description = facility.shortDescription,
                        guideMode = facility.guideMode,
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
