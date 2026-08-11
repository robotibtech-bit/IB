package com.example.ibtech.ui.kids

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.example.ibtech.data.repository.DefaultBookTasteQuestions
import com.example.ibtech.data.repository.FacilityRepository
import com.example.ibtech.data.repository.KidsContentRepository
import com.example.ibtech.data.repository.StatsRepository
import com.example.ibtech.domain.model.BookTasteOption
import com.example.ibtech.domain.model.BookTasteQuestion
import com.example.ibtech.domain.model.Facility
import com.example.ibtech.domain.model.RecommendedBook
import com.example.ibtech.domain.model.StatEventType
import com.example.ibtech.domain.usecase.BookTasteEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** 추천도서 화면 상태 (요구사항 명세서 2.14절). */
data class BookRecommendationUiState(
    val isLoaded: Boolean = false,
    // "직접 골라볼래요"(BROWSE_ALL) 전용 — 기존 연령/주제 필터 그대로.
    val books: List<RecommendedBook> = emptyList(),
    val ageGroupOptions: List<String> = emptyList(),
    val topicOptions: List<String> = emptyList(),
    val selectedAgeGroup: String? = null,
    val selectedTopic: String? = null,
    val childrenFacility: Facility? = null,
    // 취향 퀴즈(기획 문서 "2. 나에게 맞는 책" 절) — 고정 6문항 + 점수 채점.
    val tasteQuestions: List<BookTasteQuestion> = emptyList(),
    val tasteQuestionIndex: Int = 0,
    val recommendedBooks: List<RecommendedBook> = emptyList(),
    val resultCharacterKey: String? = null
)

private data class TasteQuizState(
    val questionIndex: Int = 0,
    val scores: Map<String, Int> = emptyMap(),
    val shownBookIds: Set<String> = emptySet()
)

class BookRecommendationViewModel(
    application: Application,
    savedStateHandle: SavedStateHandle
) : AndroidViewModel(application) {

    private val kidsContentRepository = KidsContentRepository.getInstance(application)
    private val facilityRepository = FacilityRepository.getInstance(application)
    private val statsRepository = StatsRepository.getInstance(application)

    // 라우트 쿼리 인자를 초깃값으로만 쓰고, 이후 필터 조작은 화면 안에서 독립적으로 유지한다.
    private val selectedAgeGroup = MutableStateFlow(savedStateHandle.get<String>("ageGroup"))
    private val selectedTopic = MutableStateFlow(savedStateHandle.get<String>("topic"))

    // 취향 퀴즈 6문항은 고정 콘텐츠라 반응형 Flow가 아니라 한 번만 만든다.
    private val tasteQuestions = DefaultBookTasteQuestions.build(application)
    private val tasteQuizState = MutableStateFlow(TasteQuizState())

    init {
        // 추천도서 조회수 (요구사항 4.3절, 로드맵 11단계). 화면에 처음 진입했을 때 노출된
        // 책들만 기록한다 — 필터를 바꿀 때마다 다시 세면 "조회"가 아니라 "필터 조작 횟수"가 된다.
        viewModelScope.launch {
            val initialBooks = kidsContentRepository.books.first().filter { it.isEnabled }
            initialBooks.forEach { book ->
                statsRepository.logEvent(StatEventType.BOOK_VIEW, book.title)
            }
        }
    }

    val uiState: StateFlow<BookRecommendationUiState> = combine(
        kidsContentRepository.books,
        facilityRepository.visibleFacilities,
        selectedAgeGroup,
        selectedTopic,
        tasteQuizState
    ) { allBooks, facilities, ageGroup, topic, taste ->
        val enabled = allBooks.filter { it.isEnabled }
        val filtered = enabled
            .filter { book -> ageGroup == null || book.ageGroup == ageGroup }
            .filter { book -> topic == null || book.topic == topic }
            .sortedBy { it.sortOrder }

        val quizComplete = taste.questionIndex >= tasteQuestions.size
        val recommended = if (quizComplete) {
            BookTasteEngine.recommend(enabled, taste.scores, taste.shownBookIds)
        } else {
            emptyList()
        }

        BookRecommendationUiState(
            isLoaded = true,
            books = filtered,
            ageGroupOptions = enabled.mapNotNull { it.ageGroup }.distinct().sorted(),
            topicOptions = enabled.mapNotNull { it.topic }.distinct().sorted(),
            selectedAgeGroup = ageGroup,
            selectedTopic = topic,
            childrenFacility = facilities.findChildrenFacility(),
            tasteQuestions = tasteQuestions,
            tasteQuestionIndex = taste.questionIndex,
            recommendedBooks = recommended,
            resultCharacterKey = if (quizComplete) BookTasteEngine.resultCharacterKey(taste.scores) else null
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = BookRecommendationUiState()
    )

    fun onSelectAgeGroup(ageGroup: String?) {
        selectedAgeGroup.value = ageGroup
    }

    fun onSelectTopic(topic: String?) {
        selectedTopic.value = topic
    }

    fun onResetFilters() {
        selectedAgeGroup.value = null
        selectedTopic.value = null
    }

    fun onAnswerTasteQuestion(option: BookTasteOption) {
        tasteQuizState.update {
            it.copy(
                questionIndex = it.questionIndex + 1,
                scores = BookTasteEngine.mergeScores(it.scores, option.scoreDelta)
            )
        }
    }

    /** "다른 책도 보여줘!" — 방금 보여준 책들을 제외 목록에 더해 다음 후보를 다시 고른다. */
    fun onShowMoreBooks() {
        val justShown = uiState.value.recommendedBooks.map { it.id }.toSet()
        tasteQuizState.update { it.copy(shownBookIds = it.shownBookIds + justShown) }
    }

    fun onRestartTaste() {
        tasteQuizState.value = TasteQuizState()
    }
}
