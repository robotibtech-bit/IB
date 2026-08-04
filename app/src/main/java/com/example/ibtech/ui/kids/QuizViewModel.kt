package com.example.ibtech.ui.kids

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.example.ibtech.R
import com.example.ibtech.data.repository.KidsContentRepository
import com.example.ibtech.domain.model.QuizQuestion
import com.example.ibtech.domain.usecase.SelectQuizQuestionsUseCase
import com.example.ibtech.robot.TemiControllerProvider
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 퀴즈 진행 화면 상태 (요구사항 명세서 2.12절).
 *
 * [questions]는 세션 시작 시 [SelectQuizQuestionsUseCase]로 한 번만 뽑아 고정한다 — 이후
 * 진행 중에는 이 목록을 순서대로 소비하기만 하므로 "세션 내 중복 재출제 없음"이 별도 상태
 * 추적 없이 성립한다. 화면을 벗어나면(홈 이동 등) 이 ViewModel 자체가 파기되어 세션도 함께
 * 사라진다 — Compose Navigation의 기본 스코프 규칙만으로 "중간 홈 복귀 시 세션 폐기"가
 * 만족된다.
 */
data class QuizUiState(
    val isLoaded: Boolean = false,
    val category: String = "",
    val questions: List<QuizQuestion> = emptyList(),
    val currentIndex: Int = 0,
    val selectedChoiceIndex: Int? = null,
    val correctCount: Int = 0,
    val correctRecommendedBookIds: List<String> = emptyList(),
    val isFinished: Boolean = false
) {
    val currentQuestion: QuizQuestion?
        get() = questions.getOrNull(currentIndex)

    val totalCount: Int
        get() = questions.size
}

class QuizViewModel(
    application: Application,
    savedStateHandle: SavedStateHandle
) : AndroidViewModel(application) {

    private val category: String = checkNotNull(savedStateHandle["category"]) {
        "kids_quiz_play 라우트에 category 인자가 없습니다."
    }

    private val kidsContentRepository = KidsContentRepository.getInstance(application)
    private val temiController = TemiControllerProvider.current

    private val _uiState = MutableStateFlow(QuizUiState(category = category))
    val uiState: StateFlow<QuizUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val all = kidsContentRepository.quizQuestions.first()
            val selected = SelectQuizQuestionsUseCase(all, category)
            _uiState.update { it.copy(isLoaded = true, questions = selected) }
        }
    }

    /** 보기 선택. 이미 답한 문제에 대한 중복 입력은 무시한다(연속 터치 방지, 3.8절). */
    fun onSelectChoice(index: Int) {
        val state = _uiState.value
        if (state.selectedChoiceIndex != null) return
        val question = state.currentQuestion ?: return
        val isCorrect = index == question.correctIndex

        _uiState.update {
            it.copy(
                selectedChoiceIndex = index,
                correctCount = it.correctCount + if (isCorrect) 1 else 0,
                correctRecommendedBookIds = if (isCorrect) {
                    it.correctRecommendedBookIds + question.recommendedBookIds
                } else {
                    it.correctRecommendedBookIds
                }
            )
        }

        val speechRes = if (isCorrect) R.string.quiz_speech_correct else R.string.quiz_speech_incorrect
        temiController.speak(getApplication<Application>().getString(speechRes))

        viewModelScope.launch {
            delay(ANSWER_FEEDBACK_DELAY_MILLIS)
            advanceToNextQuestion()
        }
    }

    private fun advanceToNextQuestion() {
        _uiState.update { state ->
            val nextIndex = state.currentIndex + 1
            if (nextIndex >= state.questions.size) {
                state.copy(isFinished = true)
            } else {
                state.copy(currentIndex = nextIndex, selectedChoiceIndex = null)
            }
        }
    }

    companion object {
        private const val ANSWER_FEEDBACK_DELAY_MILLIS = 1_600L
    }
}
