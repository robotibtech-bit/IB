package com.example.ibtech.ui.kids

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ibtech.data.repository.KidsContentRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * 퀴즈 주제 카드 하나. [questionCount]가 0이면 이 주제에 등록된 활성 문제가 없다는 뜻이라
 * `QuizCategoryScreen`이 카드를 비활성 + "문제 준비 중"으로 그린다(요구사항 2.11절).
 */
data class QuizCategoryItem(
    val name: String,
    val questionCount: Int
)

data class QuizCategoryUiState(
    val isLoaded: Boolean = false,
    val categories: List<QuizCategoryItem> = emptyList()
)

class QuizCategoryViewModel(application: Application) : AndroidViewModel(application) {

    private val kidsContentRepository = KidsContentRepository.getInstance(application)

    val uiState: StateFlow<QuizCategoryUiState> = kidsContentRepository.quizQuestions
        .map { questions ->
            // 비활성 문제만 있는 카테고리도 카드 자체는 보여야 "문제 준비 중"을 표시할 수
            // 있으므로, 카테고리 존재 여부는 전체 문제에서, 개수는 활성 문제에서만 센다.
            val categories = questions
                .groupBy { it.category }
                .map { (name, list) -> QuizCategoryItem(name, list.count { it.isEnabled }) }
                .sortedBy { it.name }
            QuizCategoryUiState(isLoaded = true, categories = categories)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = QuizCategoryUiState()
        )
}
