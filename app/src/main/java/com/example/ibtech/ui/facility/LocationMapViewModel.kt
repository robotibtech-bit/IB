package com.example.ibtech.ui.facility

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.example.ibtech.data.repository.FacilityRepository
import com.example.ibtech.domain.model.Facility
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** 위치만 보기 화면 상태 (요구사항 명세서 2.5절). */
data class LocationMapUiState(
    val isLoaded: Boolean = false,
    val facility: Facility? = null
)

class LocationMapViewModel(
    application: Application,
    savedStateHandle: SavedStateHandle
) : AndroidViewModel(application) {

    private val facilityId: String = checkNotNull(savedStateHandle["facilityId"]) {
        "facility_map 라우트에 facilityId 인자가 없습니다."
    }

    private val facilityRepository = FacilityRepository.getInstance(application)

    val uiState: StateFlow<LocationMapUiState> = facilityRepository.allFacilities
        .map { list -> LocationMapUiState(isLoaded = true, facility = list.firstOrNull { it.id == facilityId }) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = LocationMapUiState()
        )
}
