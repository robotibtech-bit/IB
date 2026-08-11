package com.example.ibtech.ui.admin

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ibtech.R
import com.example.ibtech.data.repository.KidsContentRepository
import com.example.ibtech.domain.model.LibraryEtiquetteTip
import com.example.ibtech.domain.model.QuizQuestion
import com.example.ibtech.domain.model.RecommendedBook
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * [choices]는 항상 2개 또는 4개다(기획 문서 "스마트 보기 개수" 원칙: 판단형 문제는 2보기,
 * 선택형 문제는 4보기). [onQuizChoiceCountChange]로만 개수를 바꾸며, 그 외에는 항상 이
 * 길이를 유지한 채 값만 바꾼다.
 */
data class QuizDraft(
    val id: String? = null,
    val category: String = "",
    val question: String = "",
    val choices: List<String> = List(4) { "" },
    val correctIndex: Int = 0,
    val explanation: String = "",
    val recommendedBookIds: Set<String> = emptySet(),
    val isEnabled: Boolean = true,
    val sortOrderText: String = "0",
    val questionError: String? = null
)

data class BookDraft(
    val id: String? = null,
    val title: String = "",
    val author: String = "",
    val ageGroup: String = "",
    val topic: String = "",
    val description: String = "",
    val locationText: String = "",
    val isEnabled: Boolean = true,
    val sortOrderText: String = "0",
    val titleError: String? = null
)

data class EtiquetteDraft(
    val id: String? = null,
    val text: String = "",
    val isEnabled: Boolean = true,
    val sortOrderText: String = "0",
    val textError: String? = null
)

sealed interface KidsContentDraft {
    data class Quiz(val draft: QuizDraft) : KidsContentDraft
    data class Book(val draft: BookDraft) : KidsContentDraft
    data class Etiquette(val draft: EtiquetteDraft) : KidsContentDraft
}

data class KidsContentAdminUiState(
    val isLoaded: Boolean = false,
    val quizQuestions: List<QuizQuestion> = emptyList(),
    val books: List<RecommendedBook> = emptyList(),
    val etiquetteTips: List<LibraryEtiquetteTip> = emptyList(),
    val editingDraft: KidsContentDraft? = null
)

/** 어린이 콘텐츠(퀴즈/추천도서/예절) 관리 화면 (로드맵 10단계). */
class KidsContentAdminViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = KidsContentRepository.getInstance(application)
    private val editingDraft = MutableStateFlow<KidsContentDraft?>(null)

    val uiState: StateFlow<KidsContentAdminUiState> = combine(
        repository.quizQuestions,
        repository.books,
        repository.etiquetteTips,
        editingDraft
    ) { quiz, books, etiquette, draft ->
        KidsContentAdminUiState(
            isLoaded = true,
            quizQuestions = quiz.sortedBy { it.sortOrder },
            books = books.sortedBy { it.sortOrder },
            etiquetteTips = etiquette.sortedBy { it.sortOrder },
            editingDraft = draft
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = KidsContentAdminUiState()
    )

    fun onDismissDialog() {
        editingDraft.value = null
    }

    // ---- 퀴즈 ----

    private fun updateQuizDraft(transform: (QuizDraft) -> QuizDraft) {
        editingDraft.update { current -> (current as? KidsContentDraft.Quiz)?.copy(draft = transform(current.draft)) }
    }

    fun onAddQuiz() {
        editingDraft.value = KidsContentDraft.Quiz(QuizDraft())
    }

    fun onEditQuiz(question: QuizQuestion) {
        // 정확히 2보기인 문제만 2칸으로 열고, 그 외(4보기, 혹은 이 폼이 3칸 고정이던 시절 저장된
        // 3보기 등)는 4칸으로 열어 기존 값을 그대로 보여준다 — 관리자가 필요하면 보기 개수
        // 토글로 2보기로 줄일 수 있다.
        val choices = if (question.choices.size == 2) {
            question.choices
        } else {
            List(4) { question.choices.getOrElse(it) { "" } }
        }
        editingDraft.value = KidsContentDraft.Quiz(
            QuizDraft(
                id = question.id,
                category = question.category,
                question = question.question,
                choices = choices,
                correctIndex = question.correctIndex.coerceIn(0, choices.lastIndex),
                explanation = question.explanation,
                recommendedBookIds = question.recommendedBookIds.toSet(),
                isEnabled = question.isEnabled,
                sortOrderText = question.sortOrder.toString()
            )
        )
    }

    fun onQuizCategoryChange(value: String) = updateQuizDraft { it.copy(category = value) }
    fun onQuizQuestionChange(value: String) = updateQuizDraft { it.copy(question = value, questionError = null) }

    fun onQuizChoiceChange(index: Int, value: String) = updateQuizDraft { draft ->
        draft.copy(choices = draft.choices.toMutableList().also { it[index] = value })
    }

    /** 보기 개수를 2 또는 4로 바꾼다. 4→2는 앞 두 칸만 남기고, 2→4는 빈 칸 두 개를 덧붙인다 —
     * 어느 쪽이든 이미 입력한 값은 잃지 않는다. 정답 인덱스가 줄어든 범위를 벗어나면 0으로
     * 되돌린다. */
    fun onQuizChoiceCountChange(count: Int) = updateQuizDraft { draft ->
        val resized = when {
            count == draft.choices.size -> draft.choices
            count < draft.choices.size -> draft.choices.take(count)
            else -> draft.choices + List(count - draft.choices.size) { "" }
        }
        draft.copy(
            choices = resized,
            correctIndex = if (draft.correctIndex in resized.indices) draft.correctIndex else 0
        )
    }

    fun onQuizCorrectIndexChange(index: Int) = updateQuizDraft { it.copy(correctIndex = index) }
    fun onQuizExplanationChange(value: String) = updateQuizDraft { it.copy(explanation = value) }

    fun onQuizToggleRecommendedBook(bookId: String) = updateQuizDraft {
        val ids = it.recommendedBookIds
        it.copy(recommendedBookIds = if (bookId in ids) ids - bookId else ids + bookId)
    }

    fun onQuizEnabledChange(value: Boolean) = updateQuizDraft { it.copy(isEnabled = value) }
    fun onQuizSortOrderChange(value: String) = updateQuizDraft { it.copy(sortOrderText = value) }

    fun onSaveQuizDraft() {
        val draft = (editingDraft.value as? KidsContentDraft.Quiz)?.draft ?: return
        if (draft.question.isBlank()) {
            updateQuizDraft { it.copy(questionError = blankErrorMessage()) }
            return
        }
        val sortOrder = draft.sortOrderText.trim().toIntOrNull() ?: 0
        val question = QuizQuestion(
            id = draft.id ?: "quiz_${System.currentTimeMillis()}",
            category = draft.category.trim().ifBlank { getApplication<Application>().getString(R.string.quiz_category_empty_label) },
            question = draft.question.trim(),
            choices = draft.choices.map { it.trim() },
            correctIndex = draft.correctIndex.coerceIn(0, draft.choices.lastIndex),
            explanation = draft.explanation.trim(),
            recommendedBookIds = draft.recommendedBookIds.toList(),
            isEnabled = draft.isEnabled,
            sortOrder = sortOrder
        )
        viewModelScope.launch {
            repository.upsertQuiz(question)
            editingDraft.value = null
        }
    }

    fun onDeleteQuiz(id: String) {
        viewModelScope.launch { repository.deleteQuiz(id) }
    }

    // ---- 추천도서 ----

    private fun updateBookDraft(transform: (BookDraft) -> BookDraft) {
        editingDraft.update { current -> (current as? KidsContentDraft.Book)?.copy(draft = transform(current.draft)) }
    }

    fun onAddBook() {
        editingDraft.value = KidsContentDraft.Book(BookDraft())
    }

    fun onEditBook(book: RecommendedBook) {
        editingDraft.value = KidsContentDraft.Book(
            BookDraft(
                id = book.id,
                title = book.title,
                author = book.author,
                ageGroup = book.ageGroup.orEmpty(),
                topic = book.topic.orEmpty(),
                description = book.description,
                locationText = book.locationText.orEmpty(),
                isEnabled = book.isEnabled,
                sortOrderText = book.sortOrder.toString()
            )
        )
    }

    fun onBookTitleChange(value: String) = updateBookDraft { it.copy(title = value, titleError = null) }
    fun onBookAuthorChange(value: String) = updateBookDraft { it.copy(author = value) }
    fun onBookAgeGroupChange(value: String) = updateBookDraft { it.copy(ageGroup = value) }
    fun onBookTopicChange(value: String) = updateBookDraft { it.copy(topic = value) }
    fun onBookDescriptionChange(value: String) = updateBookDraft { it.copy(description = value) }
    fun onBookLocationChange(value: String) = updateBookDraft { it.copy(locationText = value) }
    fun onBookEnabledChange(value: Boolean) = updateBookDraft { it.copy(isEnabled = value) }
    fun onBookSortOrderChange(value: String) = updateBookDraft { it.copy(sortOrderText = value) }

    fun onSaveBookDraft() {
        val draft = (editingDraft.value as? KidsContentDraft.Book)?.draft ?: return
        if (draft.title.isBlank()) {
            updateBookDraft { it.copy(titleError = blankErrorMessage()) }
            return
        }
        val sortOrder = draft.sortOrderText.trim().toIntOrNull() ?: 0
        val book = RecommendedBook(
            id = draft.id ?: "book_${System.currentTimeMillis()}",
            title = draft.title.trim(),
            author = draft.author.trim(),
            ageGroup = draft.ageGroup.trim().ifBlank { null },
            topic = draft.topic.trim().ifBlank { null },
            description = draft.description.trim(),
            locationText = draft.locationText.trim().ifBlank { null },
            isEnabled = draft.isEnabled,
            sortOrder = sortOrder
        )
        viewModelScope.launch {
            repository.upsertBook(book)
            editingDraft.value = null
        }
    }

    fun onDeleteBook(id: String) {
        viewModelScope.launch { repository.deleteBook(id) }
    }

    // ---- 도서관 예절 ----

    private fun updateEtiquetteDraft(transform: (EtiquetteDraft) -> EtiquetteDraft) {
        editingDraft.update { current -> (current as? KidsContentDraft.Etiquette)?.copy(draft = transform(current.draft)) }
    }

    fun onAddEtiquette() {
        editingDraft.value = KidsContentDraft.Etiquette(EtiquetteDraft())
    }

    fun onEditEtiquette(tip: LibraryEtiquetteTip) {
        editingDraft.value = KidsContentDraft.Etiquette(
            EtiquetteDraft(
                id = tip.id,
                text = tip.text,
                isEnabled = tip.isEnabled,
                sortOrderText = tip.sortOrder.toString()
            )
        )
    }

    fun onEtiquetteTextChange(value: String) = updateEtiquetteDraft { it.copy(text = value, textError = null) }
    fun onEtiquetteEnabledChange(value: Boolean) = updateEtiquetteDraft { it.copy(isEnabled = value) }
    fun onEtiquetteSortOrderChange(value: String) = updateEtiquetteDraft { it.copy(sortOrderText = value) }

    fun onSaveEtiquetteDraft() {
        val draft = (editingDraft.value as? KidsContentDraft.Etiquette)?.draft ?: return
        if (draft.text.isBlank()) {
            updateEtiquetteDraft { it.copy(textError = blankErrorMessage()) }
            return
        }
        val sortOrder = draft.sortOrderText.trim().toIntOrNull() ?: 0
        val tip = LibraryEtiquetteTip(
            id = draft.id ?: "etiquette_${System.currentTimeMillis()}",
            text = draft.text.trim(),
            isEnabled = draft.isEnabled,
            sortOrder = sortOrder
        )
        viewModelScope.launch {
            repository.upsertEtiquette(tip)
            editingDraft.value = null
        }
    }

    fun onDeleteEtiquette(id: String) {
        viewModelScope.launch { repository.deleteEtiquette(id) }
    }

    private fun blankErrorMessage(): String = getApplication<Application>().getString(R.string.admin_error_blank_title)
}
