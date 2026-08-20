package com.example.ibtech.ui.booksearch

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ibtech.data.booksearch.BookSearchApi
import com.example.ibtech.data.booksearch.BookSearchException
import com.example.ibtech.data.booksearch.BookSearchIssue
import com.example.ibtech.data.repository.SettingsRepository
import com.example.ibtech.domain.model.BookHit
import com.example.ibtech.domain.model.LibrarySettings
import com.example.ibtech.robot.ListeningState
import com.example.ibtech.robot.TemiController
import com.example.ibtech.robot.TemiControllerProvider
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 책 찾기 화면 상태.
 *
 * [keyword]는 입력창의 현재 값이고 [submittedQuery]는 실제로 검색한 문장이다. 둘을 나눈
 * 이유 — 결과를 보는 중에 사용자가 입력창을 고쳐도, 화면에 뜬 결과가 어떤 말로 찾은
 * 것인지는 그대로 유지돼야 하기 때문이다.
 */
data class BookSearchUiState(
    val keyword: String = "",
    val submittedQuery: String = "",
    val isSearching: Boolean = false,
    val hasSearched: Boolean = false,
    val hits: List<BookHit> = emptyList(),
    /** 서버가 뽑아낸 검색어. "이렇게 찾았어요"로 보여 준다. */
    val planKeywords: List<String> = emptyList(),
    val issue: BookSearchIssue? = null,
    val listeningState: ListeningState = ListeningState.Unavailable,
    val baseFloor: Int = LibrarySettings.DEFAULT_BASE_FLOOR,
    val isServerConfigured: Boolean = true
) {
    /** 마이크 버튼을 보여줄지. 로봇이 아닌 기기에서는 숨긴다. */
    val canUseVoice: Boolean
        get() = listeningState != ListeningState.Unavailable

    val showEmptyResult: Boolean
        get() = hasSearched && !isSearching && issue == null && hits.isEmpty()
}

class BookSearchViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsRepository = SettingsRepository.getInstance(application)
    private val controller: TemiController = TemiControllerProvider.current
    private val api = BookSearchApi()

    private val internal = MutableStateFlow(BookSearchUiState())
    private var searchJob: Job? = null
    private var baseUrl: String = ""

    val uiState: StateFlow<BookSearchUiState> = combine(
        internal,
        controller.listeningState,
        settingsRepository.settings
    ) { state, listening, settings ->
        state.copy(
            listeningState = listening,
            baseFloor = settings.baseFloor,
            isServerConfigured = settings.bookSearchBaseUrl.isNotBlank()
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = BookSearchUiState()
    )

    init {
        // 서버 주소는 검색할 때마다 읽지 않고 캐시한다 — 관리자가 바꾸면 Flow 가 갱신한다.
        viewModelScope.launch {
            settingsRepository.settings.collect { baseUrl = it.bookSearchBaseUrl }
        }
        // 음성 인식 결과가 오면 입력창에 채우고 곧바로 검색한다. 아이가 말한 뒤 버튼을 또
        // 눌러야 하면 흐름이 끊긴다.
        viewModelScope.launch {
            controller.asrResults.collect { spoken ->
                internal.update { it.copy(keyword = spoken) }
                search(spoken)
            }
        }
    }

    fun onKeywordChange(value: String) {
        internal.update { it.copy(keyword = value) }
    }

    fun onSearchClick() {
        search(internal.value.keyword)
    }

    /** 인기 키워드 칩을 눌렀을 때. 입력창에도 반영해 무엇으로 찾았는지 보이게 한다. */
    fun onSuggestionClick(keyword: String) {
        internal.update { it.copy(keyword = keyword) }
        search(keyword)
    }

    /** 마이크 버튼. 로봇이 질문을 읽어 준 뒤 발화를 듣는다. */
    fun onVoiceClick() {
        if (uiState.value.listeningState.isActive) {
            controller.finishConversation()
            return
        }
        controller.askQuestion(VOICE_PROMPT)
    }

    /** 화면을 벗어날 때 대화 레이어를 반드시 닫는다. */
    fun onLeaveScreen() {
        controller.finishConversation()
    }

    private fun search(rawQuery: String) {
        val query = rawQuery.trim()
        if (query.isEmpty()) return

        searchJob?.cancel()
        internal.update {
            it.copy(
                submittedQuery = query,
                isSearching = true,
                hasSearched = true,
                issue = null
            )
        }
        searchJob = viewModelScope.launch {
            try {
                val result = api.search(baseUrl = baseUrl, query = query)
                internal.update {
                    it.copy(
                        isSearching = false,
                        hits = result.hits,
                        planKeywords = result.plan.keywords,
                        issue = null
                    )
                }
            } catch (e: BookSearchException) {
                internal.update {
                    it.copy(isSearching = false, hits = emptyList(), issue = e.issue)
                }
            }
        }
    }

    companion object {
        /** 마이크 버튼을 눌렀을 때 로봇이 읽어 주는 질문. */
        const val VOICE_PROMPT = "어떤 책을 찾으시나요? 말씀해 주세요."

        /**
         * 입력을 돕는 기본 키워드. 아이가 타이핑 없이도 쓸 수 있게 한다.
         * 실제 이용 통계가 쌓이면 관리자 설정으로 옮길 후보다.
         */
        val SUGGESTED_KEYWORDS = listOf("공룡", "공주", "똥", "우주", "동물", "위인전")
    }
}
