package com.example.ibtech.ui.kids

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.example.ibtech.data.repository.FacilityRepository
import com.example.ibtech.data.repository.KidsContentRepository
import com.example.ibtech.domain.model.Facility
import com.example.ibtech.domain.model.RecommendedBook
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 퀴즈 결과 화면 상태 (요구사항 명세서 2.13절).
 *
 * 정답 수/전체 수/추천도서 id는 [QuizViewModel] 세션의 결과값을 라우트 쿼리 인자로 그대로
 * 전달받는다(이 앱의 기존 관례 — `facility_list?query=`와 동일하게, 복잡한 객체 대신 원시값만
 * 라우트로 넘긴다).
 */
data class QuizResultUiState(
    val isLoaded: Boolean = false,
    val category: String = "",
    val correctCount: Int = 0,
    val totalCount: Int = 0,
    val recommendedBooks: List<RecommendedBook> = emptyList(),
    val childrenFacility: Facility? = null
)

class QuizResultViewModel(
    application: Application,
    savedStateHandle: SavedStateHandle
) : AndroidViewModel(application) {

    private val category: String = checkNotNull(savedStateHandle["category"]) {
        "kids_quiz_result 라우트에 category 인자가 없습니다."
    }
    private val bookIds: List<String> = savedStateHandle.get<String>("bookIds")
        .orEmpty()
        .split(",")
        .filter { it.isNotBlank() }

    private val kidsContentRepository = KidsContentRepository.getInstance(application)
    private val facilityRepository = FacilityRepository.getInstance(application)

    private val _uiState = MutableStateFlow(
        QuizResultUiState(
            category = category,
            correctCount = savedStateHandle.get<Int>("correct") ?: 0,
            totalCount = savedStateHandle.get<Int>("total") ?: 0
        )
    )
    val uiState: StateFlow<QuizResultUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val books = bookIds.distinct().mapNotNull { kidsContentRepository.getBook(it) }
            val childrenFacility = facilityRepository.visibleFacilities.first().findChildrenFacility()
            _uiState.update {
                it.copy(isLoaded = true, recommendedBooks = books, childrenFacility = childrenFacility)
            }
        }
    }
}
