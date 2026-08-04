package com.example.ibtech.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ibtech.data.repository.SettingsRepository
import com.example.ibtech.domain.model.LibrarySettings
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * 홈 화면 상태. [isLoaded]가 true가 되기 전에는 DataStore 첫 값을 아직 못 받은 것이다.
 */
data class HomeUiState(
    val isLoaded: Boolean = false,
    val settings: LibrarySettings = LibrarySettings()
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsRepository = SettingsRepository.getInstance(application)

    val uiState: StateFlow<HomeUiState> = settingsRepository.settings
        .map { HomeUiState(isLoaded = true, settings = it) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = HomeUiState()
        )
}
