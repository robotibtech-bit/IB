package com.example.ibtech.ui.admin

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.ibtech.R
import com.example.ibtech.domain.model.LibraryEtiquetteTip
import com.example.ibtech.domain.model.QuizQuestion
import com.example.ibtech.domain.model.RecommendedBook
import com.example.ibtech.ui.common.AdminFormDialog
import com.example.ibtech.ui.common.AdminListRow
import com.example.ibtech.ui.common.AdminSwitchRow
import com.example.ibtech.ui.common.AdminTextField
import com.example.ibtech.ui.common.ConfirmDialog
import com.example.ibtech.ui.common.DecorativeBackground
import com.example.ibtech.ui.common.LibraryOutlinedButton
import com.example.ibtech.ui.theme.LibraryDimens

private enum class KidsContentTab { QUIZ, BOOKS, ETIQUETTE }

/** 어린이 콘텐츠(퀴즈/추천도서/예절) 관리 화면 (로드맵 10단계). */
@Composable
fun KidsContentAdminScreen(
    uiState: KidsContentAdminUiState,
    onAddQuiz: () -> Unit,
    onEditQuiz: (QuizQuestion) -> Unit,
    onDeleteQuiz: (String) -> Unit,
    onAddBook: () -> Unit,
    onEditBook: (RecommendedBook) -> Unit,
    onDeleteBook: (String) -> Unit,
    onAddEtiquette: () -> Unit,
    onEditEtiquette: (LibraryEtiquetteTip) -> Unit,
    onDeleteEtiquette: (String) -> Unit,
    onDismissDialog: () -> Unit,
    onQuizCategoryChange: (String) -> Unit,
    onQuizQuestionChange: (String) -> Unit,
    onQuizChoiceChange: (Int, String) -> Unit,
    onQuizChoiceCountChange: (Int) -> Unit,
    onQuizCorrectIndexChange: (Int) -> Unit,
    onQuizExplanationChange: (String) -> Unit,
    onQuizToggleRecommendedBook: (String) -> Unit,
    onQuizEnabledChange: (Boolean) -> Unit,
    onQuizSortOrderChange: (String) -> Unit,
    onSaveQuizDraft: () -> Unit,
    onBookTitleChange: (String) -> Unit,
    onBookAuthorChange: (String) -> Unit,
    onBookAgeGroupChange: (String) -> Unit,
    onBookTopicChange: (String) -> Unit,
    onBookDescriptionChange: (String) -> Unit,
    onBookLocationChange: (String) -> Unit,
    onBookEnabledChange: (Boolean) -> Unit,
    onBookSortOrderChange: (String) -> Unit,
    onSaveBookDraft: () -> Unit,
    onEtiquetteTextChange: (String) -> Unit,
    onEtiquetteEnabledChange: (Boolean) -> Unit,
    onEtiquetteSortOrderChange: (String) -> Unit,
    onSaveEtiquetteDraft: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(KidsContentTab.QUIZ) }
    var pendingDeleteQuiz by remember { mutableStateOf<QuizQuestion?>(null) }
    var pendingDeleteBook by remember { mutableStateOf<RecommendedBook?>(null) }
    var pendingDeleteEtiquette by remember { mutableStateOf<LibraryEtiquetteTip?>(null) }

    Box(modifier = modifier.fillMaxSize()) {
        DecorativeBackground(modifier = Modifier.fillMaxSize(), showBookAndPlantDecoration = false)

        if (!uiState.isLoaded) return@Box

        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = LibraryDimens.ScreenPadding, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedTab == KidsContentTab.QUIZ,
                    onClick = { selectedTab = KidsContentTab.QUIZ },
                    label = { Text(stringResource(R.string.kids_admin_tab_quiz)) }
                )
                FilterChip(
                    selected = selectedTab == KidsContentTab.BOOKS,
                    onClick = { selectedTab = KidsContentTab.BOOKS },
                    label = { Text(stringResource(R.string.kids_admin_tab_books)) }
                )
                FilterChip(
                    selected = selectedTab == KidsContentTab.ETIQUETTE,
                    onClick = { selectedTab = KidsContentTab.ETIQUETTE },
                    label = { Text(stringResource(R.string.kids_admin_tab_etiquette)) }
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = LibraryDimens.ScreenPadding),
                horizontalArrangement = Arrangement.End
            ) {
                LibraryOutlinedButton(
                    text = stringResource(R.string.usage_admin_add_action),
                    onClick = when (selectedTab) {
                        KidsContentTab.QUIZ -> onAddQuiz
                        KidsContentTab.BOOKS -> onAddBook
                        KidsContentTab.ETIQUETTE -> onAddEtiquette
                    },
                    modifier = Modifier.fillMaxWidth(0.4f)
                )
            }

            LazyColumn(
                contentPadding = PaddingValues(LibraryDimens.ScreenPadding),
                verticalArrangement = Arrangement.spacedBy(LibraryDimens.CardSpacing),
                modifier = Modifier.fillMaxSize()
            ) {
                when (selectedTab) {
                    KidsContentTab.QUIZ -> items(uiState.quizQuestions, key = { it.id }) { question ->
                        AdminListRow(
                            title = question.question,
                            subtitle = question.category,
                            onEdit = { onEditQuiz(question) },
                            onDelete = { pendingDeleteQuiz = question }
                        )
                    }

                    KidsContentTab.BOOKS -> items(uiState.books, key = { it.id }) { book ->
                        AdminListRow(
                            title = book.title,
                            subtitle = book.author,
                            onEdit = { onEditBook(book) },
                            onDelete = { pendingDeleteBook = book }
                        )
                    }

                    KidsContentTab.ETIQUETTE -> items(uiState.etiquetteTips, key = { it.id }) { tip ->
                        AdminListRow(
                            title = tip.text,
                            onEdit = { onEditEtiquette(tip) },
                            onDelete = { pendingDeleteEtiquette = tip }
                        )
                    }
                }
            }
        }

        when (val draft = uiState.editingDraft) {
            is KidsContentDraft.Quiz -> QuizDraftDialog(
                draft = draft.draft,
                books = uiState.books,
                onDismiss = onDismissDialog,
                onSave = onSaveQuizDraft,
                onCategoryChange = onQuizCategoryChange,
                onQuestionChange = onQuizQuestionChange,
                onChoiceChange = onQuizChoiceChange,
                onChoiceCountChange = onQuizChoiceCountChange,
                onCorrectIndexChange = onQuizCorrectIndexChange,
                onExplanationChange = onQuizExplanationChange,
                onToggleRecommendedBook = onQuizToggleRecommendedBook,
                onEnabledChange = onQuizEnabledChange,
                onSortOrderChange = onQuizSortOrderChange
            )

            is KidsContentDraft.Book -> BookDraftDialog(
                draft = draft.draft,
                onDismiss = onDismissDialog,
                onSave = onSaveBookDraft,
                onTitleChange = onBookTitleChange,
                onAuthorChange = onBookAuthorChange,
                onAgeGroupChange = onBookAgeGroupChange,
                onTopicChange = onBookTopicChange,
                onDescriptionChange = onBookDescriptionChange,
                onLocationChange = onBookLocationChange,
                onEnabledChange = onBookEnabledChange,
                onSortOrderChange = onBookSortOrderChange
            )

            is KidsContentDraft.Etiquette -> EtiquetteDraftDialog(
                draft = draft.draft,
                onDismiss = onDismissDialog,
                onSave = onSaveEtiquetteDraft,
                onTextChange = onEtiquetteTextChange,
                onEnabledChange = onEtiquetteEnabledChange,
                onSortOrderChange = onEtiquetteSortOrderChange
            )

            null -> Unit
        }

        pendingDeleteQuiz?.let { target ->
            ConfirmDialog(
                title = stringResource(R.string.usage_admin_delete_confirm_title),
                body = stringResource(R.string.usage_admin_delete_confirm_body, target.question),
                confirmLabel = stringResource(R.string.facility_admin_delete_confirm_action),
                dismissLabel = stringResource(R.string.facility_detail_escort_confirm_cancel),
                onConfirm = { onDeleteQuiz(target.id); pendingDeleteQuiz = null },
                onDismiss = { pendingDeleteQuiz = null }
            )
        }
        pendingDeleteBook?.let { target ->
            ConfirmDialog(
                title = stringResource(R.string.usage_admin_delete_confirm_title),
                body = stringResource(R.string.usage_admin_delete_confirm_body, target.title),
                confirmLabel = stringResource(R.string.facility_admin_delete_confirm_action),
                dismissLabel = stringResource(R.string.facility_detail_escort_confirm_cancel),
                onConfirm = { onDeleteBook(target.id); pendingDeleteBook = null },
                onDismiss = { pendingDeleteBook = null }
            )
        }
        pendingDeleteEtiquette?.let { target ->
            ConfirmDialog(
                title = stringResource(R.string.usage_admin_delete_confirm_title),
                body = stringResource(R.string.usage_admin_delete_confirm_body, target.text),
                confirmLabel = stringResource(R.string.facility_admin_delete_confirm_action),
                dismissLabel = stringResource(R.string.facility_detail_escort_confirm_cancel),
                onConfirm = { onDeleteEtiquette(target.id); pendingDeleteEtiquette = null },
                onDismiss = { pendingDeleteEtiquette = null }
            )
        }
    }
}

@Composable
private fun QuizDraftDialog(
    draft: QuizDraft,
    books: List<RecommendedBook>,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
    onCategoryChange: (String) -> Unit,
    onQuestionChange: (String) -> Unit,
    onChoiceChange: (Int, String) -> Unit,
    onChoiceCountChange: (Int) -> Unit,
    onCorrectIndexChange: (Int) -> Unit,
    onExplanationChange: (String) -> Unit,
    onToggleRecommendedBook: (String) -> Unit,
    onEnabledChange: (Boolean) -> Unit,
    onSortOrderChange: (String) -> Unit
) {
    AdminFormDialog(
        title = if (draft.id == null) stringResource(R.string.admin_dialog_title_add) else stringResource(R.string.admin_dialog_title_edit),
        onDismiss = onDismiss,
        onSave = onSave,
        saveLabel = stringResource(R.string.admin_dialog_save),
        cancelLabel = stringResource(R.string.admin_dialog_cancel)
    ) {
        AdminTextField(value = draft.category, onValueChange = onCategoryChange, label = stringResource(R.string.kids_admin_field_category))
        AdminTextField(
            value = draft.question,
            onValueChange = onQuestionChange,
            label = stringResource(R.string.kids_admin_field_question),
            errorText = draft.questionError,
            singleLine = false,
            minLines = 2
        )
        Text(text = stringResource(R.string.kids_admin_field_choice_count), style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(2, 4).forEach { count ->
                FilterChip(
                    selected = draft.choices.size == count,
                    onClick = { onChoiceCountChange(count) },
                    label = { Text(count.toString()) }
                )
            }
        }

        draft.choices.forEachIndexed { index, choice ->
            AdminTextField(
                value = choice,
                onValueChange = { onChoiceChange(index, it) },
                label = stringResource(R.string.kids_admin_field_choice, index + 1)
            )
        }

        Text(text = stringResource(R.string.kids_admin_field_correct_choice), style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            draft.choices.indices.forEach { index ->
                FilterChip(
                    selected = draft.correctIndex == index,
                    onClick = { onCorrectIndexChange(index) },
                    label = { Text(stringResource(R.string.kids_admin_field_choice, index + 1)) }
                )
            }
        }

        AdminTextField(
            value = draft.explanation,
            onValueChange = onExplanationChange,
            label = stringResource(R.string.kids_admin_field_explanation),
            singleLine = false,
            minLines = 2
        )

        Text(text = stringResource(R.string.kids_admin_field_recommended_books), style = MaterialTheme.typography.titleMedium)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            books.forEach { book ->
                FilterChip(
                    selected = book.id in draft.recommendedBookIds,
                    onClick = { onToggleRecommendedBook(book.id) },
                    label = { Text(book.title) }
                )
            }
        }

        AdminSwitchRow(
            label = stringResource(R.string.usage_admin_field_enabled),
            checked = draft.isEnabled,
            onCheckedChange = onEnabledChange
        )
        AdminTextField(
            value = draft.sortOrderText,
            onValueChange = onSortOrderChange,
            label = stringResource(R.string.facility_admin_field_sort_order),
            keyboardType = KeyboardType.Number
        )
    }
}

@Composable
private fun BookDraftDialog(
    draft: BookDraft,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
    onTitleChange: (String) -> Unit,
    onAuthorChange: (String) -> Unit,
    onAgeGroupChange: (String) -> Unit,
    onTopicChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onLocationChange: (String) -> Unit,
    onEnabledChange: (Boolean) -> Unit,
    onSortOrderChange: (String) -> Unit
) {
    AdminFormDialog(
        title = if (draft.id == null) stringResource(R.string.admin_dialog_title_add) else stringResource(R.string.admin_dialog_title_edit),
        onDismiss = onDismiss,
        onSave = onSave,
        saveLabel = stringResource(R.string.admin_dialog_save),
        cancelLabel = stringResource(R.string.admin_dialog_cancel)
    ) {
        AdminTextField(value = draft.title, onValueChange = onTitleChange, label = stringResource(R.string.kids_admin_field_book_title), errorText = draft.titleError)
        AdminTextField(value = draft.author, onValueChange = onAuthorChange, label = stringResource(R.string.kids_admin_field_book_author))
        AdminTextField(value = draft.ageGroup, onValueChange = onAgeGroupChange, label = stringResource(R.string.kids_admin_field_book_age_group))
        AdminTextField(value = draft.topic, onValueChange = onTopicChange, label = stringResource(R.string.kids_admin_field_book_topic))
        AdminTextField(
            value = draft.description,
            onValueChange = onDescriptionChange,
            label = stringResource(R.string.kids_admin_field_book_description),
            singleLine = false,
            minLines = 2
        )
        AdminTextField(value = draft.locationText, onValueChange = onLocationChange, label = stringResource(R.string.kids_admin_field_book_location))
        AdminSwitchRow(
            label = stringResource(R.string.usage_admin_field_enabled),
            checked = draft.isEnabled,
            onCheckedChange = onEnabledChange
        )
        AdminTextField(
            value = draft.sortOrderText,
            onValueChange = onSortOrderChange,
            label = stringResource(R.string.facility_admin_field_sort_order),
            keyboardType = KeyboardType.Number
        )
    }
}

@Composable
private fun EtiquetteDraftDialog(
    draft: EtiquetteDraft,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
    onTextChange: (String) -> Unit,
    onEnabledChange: (Boolean) -> Unit,
    onSortOrderChange: (String) -> Unit
) {
    AdminFormDialog(
        title = if (draft.id == null) stringResource(R.string.admin_dialog_title_add) else stringResource(R.string.admin_dialog_title_edit),
        onDismiss = onDismiss,
        onSave = onSave,
        saveLabel = stringResource(R.string.admin_dialog_save),
        cancelLabel = stringResource(R.string.admin_dialog_cancel)
    ) {
        AdminTextField(
            value = draft.text,
            onValueChange = onTextChange,
            label = stringResource(R.string.kids_admin_field_etiquette_text),
            errorText = draft.textError,
            singleLine = false,
            minLines = 2
        )
        AdminSwitchRow(
            label = stringResource(R.string.usage_admin_field_enabled),
            checked = draft.isEnabled,
            onCheckedChange = onEnabledChange
        )
        AdminTextField(
            value = draft.sortOrderText,
            onValueChange = onSortOrderChange,
            label = stringResource(R.string.facility_admin_field_sort_order),
            keyboardType = KeyboardType.Number
        )
    }
}
