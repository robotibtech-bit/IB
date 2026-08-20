package com.example.ibtech.ui.booksearch

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ibtech.R
import com.example.ibtech.data.booksearch.BookSearchIssue
import com.example.ibtech.domain.model.BookHit
import com.example.ibtech.robot.ListeningState
import com.example.ibtech.ui.common.EmptyState
import com.example.ibtech.ui.common.LibraryCard
import com.example.ibtech.ui.common.LibraryPrimaryButton
import com.example.ibtech.ui.common.LibraryScaffold
import com.example.ibtech.ui.common.debounced
import com.example.ibtech.ui.theme.LibraryDimens

/**
 * 책 찾기 화면 (홈 > 책 찾기).
 *
 * 입력은 화면 키보드와 음성 두 가지다. 음성은 로봇에서만 동작하므로
 * [BookSearchUiState.canUseVoice]가 false면 마이크 버튼 자체를 그리지 않는다.
 *
 * 결과를 눌러 서가로 이동하는 판단(동행 / 위치 안내)은 이 화면이 하지 않는다 —
 * 다음 화면([ShelfNavigationScreen])이 기준층과 비교해 정한다.
 */
@Composable
fun BookSearchScreen(
    onBack: () -> Unit,
    onHome: () -> Unit,
    onSelectBook: (BookHit) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BookSearchViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    // 화면을 벗어날 때 대화 레이어를 닫지 않으면 temi 가 계속 듣고 있는 상태로 남는다.
    DisposableEffect(Unit) {
        onDispose { viewModel.onLeaveScreen() }
    }

    LibraryScaffold(
        title = stringResource(R.string.book_search_title),
        onBack = onBack,
        onHome = onHome,
        modifier = modifier
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(LibraryDimens.ScreenPadding),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            SearchInputRow(
                keyword = state.keyword,
                listeningState = state.listeningState,
                canUseVoice = state.canUseVoice,
                enabled = state.isServerConfigured,
                onKeywordChange = viewModel::onKeywordChange,
                onSearch = viewModel::onSearchClick,
                onVoiceClick = viewModel::onVoiceClick
            )

            if (state.listeningState.isActive) {
                ListeningBanner(state.listeningState)
            }

            SuggestionRow(
                enabled = state.isServerConfigured && !state.isSearching,
                onClick = viewModel::onSuggestionClick
            )

            Box(modifier = Modifier.fillMaxSize()) {
                when {
                    !state.isServerConfigured -> EmptyState(
                        message = stringResource(R.string.book_search_not_configured)
                    )

                    state.isSearching -> Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) { CircularProgressIndicator() }

                    state.issue != null -> EmptyState(
                        message = stringResource(state.issue!!.messageRes()),
                        actionLabel = stringResource(R.string.book_search_retry),
                        onAction = viewModel::onSearchClick
                    )

                    state.showEmptyResult -> EmptyState(
                        message = stringResource(
                            R.string.book_search_empty_result,
                            state.submittedQuery
                        )
                    )

                    state.hits.isNotEmpty() -> ResultList(
                        hits = state.hits,
                        planKeywords = state.planKeywords,
                        onSelectBook = onSelectBook
                    )

                    else -> EmptyState(message = stringResource(R.string.book_search_prompt))
                }
            }
        }
    }
}

@Composable
private fun SearchInputRow(
    keyword: String,
    listeningState: ListeningState,
    canUseVoice: Boolean,
    enabled: Boolean,
    onKeywordChange: (String) -> Unit,
    onSearch: () -> Unit,
    onVoiceClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = keyword,
            onValueChange = onKeywordChange,
            enabled = enabled,
            singleLine = true,
            textStyle = MaterialTheme.typography.titleLarge,
            label = { Text(stringResource(R.string.book_search_input_label)) },
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                imeAction = ImeAction.Search
            ),
            keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                onSearch = { onSearch() }
            ),
            modifier = Modifier.weight(1f)
        )

        if (canUseVoice) {
            VoiceButton(
                listeningState = listeningState,
                enabled = enabled,
                onClick = onVoiceClick
            )
        }

        // LibraryPrimaryButton 은 내부에서 fillMaxWidth() 를 걸기 때문에 Row 안에서 폭을
        // 직접 못 준다. Box 로 감싸 폭만 고정한다.
        Box(modifier = Modifier.width(SEARCH_BUTTON_WIDTH)) {
            LibraryPrimaryButton(
                text = stringResource(R.string.book_search_action),
                onClick = onSearch,
                enabled = enabled && keyword.isNotBlank(),
                icon = Icons.Filled.Search
            )
        }
    }
}

/**
 * 마이크 버튼. 듣는 중에는 아이콘이 바뀌어 다시 누르면 취소된다.
 * 실기 없이 개발할 때는 [BookSearchUiState.canUseVoice] 가 false 라 아예 그려지지 않는다.
 */
@Composable
private fun VoiceButton(
    listeningState: ListeningState,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val active = listeningState.isActive
    FilledTonalIconButton(
        onClick = debounced(onClick),
        enabled = enabled,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.size(LibraryDimens.PrimaryButtonHeight)
    ) {
        Icon(
            imageVector = if (active) Icons.Filled.MicOff else Icons.Filled.Mic,
            contentDescription = stringResource(
                if (active) R.string.book_search_voice_stop else R.string.book_search_voice_start
            ),
            modifier = Modifier.size(VOICE_ICON_SIZE)
        )
    }
}

@Composable
private fun ListeningBanner(listeningState: ListeningState) {
    val messageRes = when (listeningState) {
        ListeningState.Listening -> R.string.book_search_listening
        ListeningState.Thinking -> R.string.book_search_thinking
        else -> R.string.book_search_voice_speaking
    }
    LibraryCard(
        modifier = Modifier.fillMaxWidth(),
        accentColor = MaterialTheme.colorScheme.primary
    ) {
        Text(
            text = stringResource(messageRes),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(LibraryDimens.CardPadding)
        )
    }
}

@Composable
private fun SuggestionRow(enabled: Boolean, onClick: (String) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        BookSearchViewModel.SUGGESTED_KEYWORDS.forEach { keyword ->
            AssistChip(
                onClick = { onClick(keyword) },
                enabled = enabled,
                label = {
                    Text(text = keyword, style = MaterialTheme.typography.titleMedium)
                },
                modifier = Modifier.height(CHIP_HEIGHT)
            )
        }
    }
}

@Composable
private fun ResultList(
    hits: List<BookHit>,
    planKeywords: List<String>,
    onSelectBook: (BookHit) -> Unit
) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        if (planKeywords.isNotEmpty()) {
            item {
                Text(
                    text = stringResource(
                        R.string.book_search_understood_as,
                        planKeywords.joinToString(", ")
                    ),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        items(items = hits, key = { it.bookId }) { hit ->
            BookHitCard(hit = hit, onClick = { onSelectBook(hit) })
        }
    }
}

private val SEARCH_BUTTON_WIDTH = 220.dp
private val VOICE_ICON_SIZE = 56.dp
private val CHIP_HEIGHT = 64.dp
