package com.example.ibtech.ui.usage

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ibtech.data.repository.UsageRepository
import com.example.ibtech.domain.usecase.ResolveUsageCategoryItemsUseCase
import com.example.ibtech.domain.usecase.UsageCategoryItem
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** 이용방법 1차 메뉴 상태 (요구사항 명세서 2.7절). */
data class UsageCategoryUiState(
    val isLoaded: Boolean = false,
    val categories: List<UsageCategoryItem> = emptyList()
)

class UsageCategoryViewModel(application: Application) : AndroidViewModel(application) {

    private val usageRepository = UsageRepository.getInstance(application)

    val uiState: StateFlow<UsageCategoryUiState> = usageRepository.topics
        .map { topics ->
            UsageCategoryUiState(isLoaded = true, categories = ResolveUsageCategoryItemsUseCase(topics))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = UsageCategoryUiState()
        )

    init {
        // 최초 실행이면 기본 콘텐츠로 1회 채운다(4단계 FacilityListViewModel의 최초 동기화
        // 트리거와 같은 자리) — 이후에는 관리자 화면(10단계)이 이 저장소를 직접 편집한다.
        viewModelScope.launch {
            usageRepository.ensureSeeded(application)
        }
    }
}
