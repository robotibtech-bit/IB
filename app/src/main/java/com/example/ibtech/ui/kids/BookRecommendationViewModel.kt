package com.example.ibtech.ui.kids

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.example.ibtech.data.repository.FacilityRepository
import com.example.ibtech.data.repository.KidsContentRepository
import com.example.ibtech.data.repository.StatsRepository
import com.example.ibtech.domain.model.Facility
import com.example.ibtech.domain.model.RecommendedBook
import com.example.ibtech.domain.model.StatEventType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** 추천도서 화면 상태 (요구사항 명세서 2.14절). */
data class BookRecommendationUiState(
    val isLoaded: Boolean = false,
    val books: List<RecommendedBook> = emptyList(),
    val ageGroupOptions: List<String> = emptyList(),
    val topicOptions: List<String> = emptyList(),
    val selectedAgeGroup: String? = null,
    val selectedTopic: String? = null,
    val childrenFacility: Facility? = null
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
        selectedTopic
    ) { allBooks, facilities, ageGroup, topic ->
        val enabled = allBooks.filter { it.isEnabled }
        val filtered = enabled
            .filter { book -> ageGroup == null || book.ageGroup == ageGroup }
            .filter { book -> topic == null || book.topic == topic }
            .sortedBy { it.sortOrder }

        BookRecommendationUiState(
            isLoaded = true,
            books = filtered,
            ageGroupOptions = enabled.mapNotNull { it.ageGroup }.distinct().sorted(),
            topicOptions = enabled.mapNotNull { it.topic }.distinct().sorted(),
            selectedAgeGroup = ageGroup,
            selectedTopic = topic,
            childrenFacility = facilities.findChildrenFacility()
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
}
