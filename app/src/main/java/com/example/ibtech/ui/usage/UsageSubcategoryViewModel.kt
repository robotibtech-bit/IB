package com.example.ibtech.ui.usage

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.example.ibtech.data.repository.UsageRepository
import com.example.ibtech.domain.model.UsageTopic
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** 이용방법 세부 항목 목록 상태 (요구사항 명세서 2.8절). */
data class UsageSubcategoryUiState(
    val isLoaded: Boolean = false,
    val category: UsageTopic? = null,
    val subtopics: List<UsageTopic> = emptyList()
)

class UsageSubcategoryViewModel(
    application: Application,
    savedStateHandle: SavedStateHandle
) : AndroidViewModel(application) {

    private val categoryId: String = checkNotNull(savedStateHandle["categoryId"]) {
        "usage_subcategory 라우트에 categoryId 인자가 없습니다."
    }

    private val usageRepository = UsageRepository.getInstance(application)

    val uiState: StateFlow<UsageSubcategoryUiState> = usageRepository.topics
        .map { topics ->
            UsageSubcategoryUiState(
                isLoaded = true,
                category = topics.firstOrNull { it.id == categoryId && it.parentId == null },
                subtopics = topics
                    .filter { it.parentId == categoryId && it.isEnabled }
                    .sortedBy { it.sortOrder }
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = UsageSubcategoryUiState()
        )
}
