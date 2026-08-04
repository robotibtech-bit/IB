package com.example.ibtech.ui.kids

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ibtech.data.repository.KidsContentRepository
import com.example.ibtech.domain.model.LibraryEtiquetteTip
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class LibraryEtiquetteUiState(
    val isLoaded: Boolean = false,
    val tips: List<LibraryEtiquetteTip> = emptyList()
)

class LibraryEtiquetteViewModel(application: Application) : AndroidViewModel(application) {

    private val kidsContentRepository = KidsContentRepository.getInstance(application)

    val uiState: StateFlow<LibraryEtiquetteUiState> = kidsContentRepository.etiquetteTips
        .map { tips ->
            LibraryEtiquetteUiState(
                isLoaded = true,
                tips = tips.filter { it.isEnabled }.sortedBy { it.sortOrder }
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = LibraryEtiquetteUiState()
        )
}
