package com.example.ibtech.ui.kids

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.ibtech.R
import com.example.ibtech.ui.common.DecorativeBackground
import com.example.ibtech.ui.common.EmptyState
import com.example.ibtech.ui.common.LibraryCard
import com.example.ibtech.ui.common.LibraryOutlinedButton
import com.example.ibtech.ui.common.LibraryPrimaryButton
import com.example.ibtech.ui.common.debounced
import com.example.ibtech.ui.theme.LibraryDimens
import com.example.ibtech.ui.theme.SkyAccent
import com.example.ibtech.ui.theme.SkyAccentContainer

/**
 * 추천도서 화면 (요구사항 명세서 2.14절)을 "몇 가지 질문 → 어울리는 책 추천" 게임으로 바꿨다.
 *
 * [RecommendedBook]에는 연령대([RecommendedBook.ageGroup])·주제([RecommendedBook.topic]) 두
 * 항목만 있어(요구사항 명세서 2.14절), 스무고개처럼 20문항을 묻는 게 아니라 이 두 가지만 차례로
 * 물어보고 바로 결과를 보여준다. 필터 자체(연령/주제 선택, [onSelectAgeGroup]/[onSelectTopic])는
 * 기존 [BookRecommendationViewModel] 로직을 그대로 재사용하고, 화면 쪽만 "질문 → 결과" 흐름으로
 * 감싼다 — 원래 있던 필터-목록 탐색 화면은 [BookGameStep.BROWSE_ALL]로 남겨 "내가 직접
 * 골라볼래요"를 누르면 볼 수 있다.
 */
@Composable
fun BookRecommendationScreen(
    uiState: BookRecommendationUiState,
    onSelectAgeGroup: (String?) -> Unit,
    onSelectTopic: (String?) -> Unit,
    onResetFilters: () -> Unit,
    onGoToChildrenFacility: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        DecorativeBackground(modifier = Modifier.fillMaxSize())

        if (!uiState.isLoaded) return@Box

        // isLoaded는 최초 한 번만 false→true로 바뀌므로, 이 remember는 데이터가 처음 들어왔을 때
        // 딱 한 번만 시작 단계를 계산한다 — 이후 필터 목록이 다시 방출돼도 진행 중인 게임
        // 단계(step)는 유지된다.
        var step by remember(uiState.isLoaded) {
            mutableStateOf(initialBookGameStep(uiState.ageGroupOptions, uiState.topicOptions))
        }

        when (step) {
            BookGameStep.AGE_QUESTION -> BookGameQuestion(
                title = stringResource(R.string.kids_book_game_age_question),
                options = uiState.ageGroupOptions,
                onSelect = { option ->
                    onSelectAgeGroup(option)
                    step = if (uiState.topicOptions.isNotEmpty()) BookGameStep.TOPIC_QUESTION else BookGameStep.REVEAL
                }
            )

            BookGameStep.TOPIC_QUESTION -> BookGameQuestion(
                title = stringResource(R.string.kids_book_game_topic_question),
                options = uiState.topicOptions,
                onSelect = { option ->
                    onSelectTopic(option)
                    step = BookGameStep.REVEAL
                }
            )

            BookGameStep.REVEAL -> BookGameReveal(
                uiState = uiState,
                onRestart = {
                    onResetFilters()
                    step = initialBookGameStep(uiState.ageGroupOptions, uiState.topicOptions)
                },
                onBrowseAll = { step = BookGameStep.BROWSE_ALL },
                onGoToChildrenFacility = onGoToChildrenFacility
            )

            BookGameStep.BROWSE_ALL -> BookBrowseAllContent(
                uiState = uiState,
                onSelectAgeGroup = onSelectAgeGroup,
                onSelectTopic = onSelectTopic,
                onResetFilters = onResetFilters,
                onGoToChildrenFacility = onGoToChildrenFacility,
                onBackToGame = {
                    onResetFilters()
                    step = initialBookGameStep(uiState.ageGroupOptions, uiState.topicOptions)
                }
            )
        }
    }
}

private enum class BookGameStep { AGE_QUESTION, TOPIC_QUESTION, REVEAL, BROWSE_ALL }

/** 등록된 연령/주제 값이 아예 없으면 그 질문 단계는 건너뛴다(관리자가 데이터를 안 채웠을 때). */
private fun initialBookGameStep(ageGroupOptions: List<String>, topicOptions: List<String>): BookGameStep = when {
    ageGroupOptions.isNotEmpty() -> BookGameStep.AGE_QUESTION
    topicOptions.isNotEmpty() -> BookGameStep.TOPIC_QUESTION
    else -> BookGameStep.REVEAL
}

// FillSpaceGrid(고정 카드 격자)는 각 행에 화면 높이를 나눠 주는 방식이라, 이 화면처럼 질문
// 문구가 위에 따로 있고 옵션 개수가 가변적(등록된 연령/주제 수에 따라 2~n개)이면 행이 늘어날수록
// 카드가 짧아져 큰 아이콘+글자가 잘린다(실기 확인). 그래서 옵션은 세로로 쌓이는 스크롤 목록으로
// 바꿔, 개수가 몇 개든 항상 카드 높이가 일정하고 잘리지 않게 한다.
@Composable
private fun BookGameQuestion(
    title: String,
    options: List<String>,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(LibraryDimens.ScreenPadding),
        verticalArrangement = Arrangement.spacedBy(LibraryDimens.CardSpacing)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground
        )
        options.forEach { option ->
            BookGameOptionCard(label = option, onClick = { onSelect(option) })
        }
    }
}

@Composable
private fun BookGameOptionCard(label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    LibraryCard(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = debounced(onClick)),
        accentColor = SkyAccent
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(LibraryDimens.CardPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(SkyAccentContainer, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.MenuBook,
                    contentDescription = null,
                    tint = SkyAccent
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun BookGameReveal(
    uiState: BookRecommendationUiState,
    onRestart: () -> Unit,
    onBrowseAll: () -> Unit,
    onGoToChildrenFacility: (String) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(LibraryDimens.ScreenPadding),
        verticalArrangement = Arrangement.spacedBy(LibraryDimens.CardSpacing),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Text(
                text = if (uiState.books.isNotEmpty()) {
                    stringResource(R.string.kids_book_game_reveal_title)
                } else {
                    stringResource(R.string.kids_book_game_reveal_fallback)
                },
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        if (uiState.books.isNotEmpty()) {
            items(uiState.books, key = { it.id }) { book -> RecommendedBookCard(book = book) }
        } else {
            item {
                LibraryOutlinedButton(
                    text = stringResource(R.string.kids_book_game_browse_all_action),
                    onClick = onBrowseAll
                )
            }
        }

        item {
            val facility = uiState.childrenFacility
            if (facility != null) {
                LibraryPrimaryButton(
                    text = stringResource(R.string.kids_go_facility_action),
                    onClick = { onGoToChildrenFacility(facility.id) }
                )
            }
        }

        item {
            LibraryOutlinedButton(
                text = stringResource(R.string.kids_book_game_restart_action),
                onClick = onRestart
            )
        }
    }
}

/** 게임 흐름 이전의 필터-목록 탐색 화면. "내가 직접 골라볼래요"를 누르면 이 화면으로 온다. */
@Composable
private fun BookBrowseAllContent(
    uiState: BookRecommendationUiState,
    onSelectAgeGroup: (String?) -> Unit,
    onSelectTopic: (String?) -> Unit,
    onResetFilters: () -> Unit,
    onGoToChildrenFacility: (String) -> Unit,
    onBackToGame: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = LibraryDimens.ScreenPadding, vertical = 8.dp),
            horizontalArrangement = Arrangement.End
        ) {
            LibraryOutlinedButton(
                text = stringResource(R.string.kids_book_game_back_to_game_action),
                onClick = onBackToGame,
                modifier = Modifier.width(240.dp)
            )
        }

        if (uiState.ageGroupOptions.isNotEmpty()) {
            FilterRow(
                allLabel = stringResource(R.string.kids_book_filter_age_all),
                options = uiState.ageGroupOptions,
                selected = uiState.selectedAgeGroup,
                onSelect = onSelectAgeGroup
            )
        }
        if (uiState.topicOptions.isNotEmpty()) {
            FilterRow(
                allLabel = stringResource(R.string.kids_book_filter_topic_all),
                options = uiState.topicOptions,
                selected = uiState.selectedTopic,
                onSelect = onSelectTopic
            )
        }

        if (uiState.books.isEmpty()) {
            EmptyState(
                message = stringResource(R.string.kids_book_empty),
                actionLabel = stringResource(R.string.kids_book_filter_reset),
                onAction = onResetFilters,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(LibraryDimens.ScreenPadding),
                verticalArrangement = Arrangement.spacedBy(LibraryDimens.CardSpacing),
                modifier = Modifier.fillMaxSize()
            ) {
                items(uiState.books, key = { it.id }) { book ->
                    RecommendedBookCard(book = book)
                }
                val facility = uiState.childrenFacility
                if (facility != null) {
                    item {
                        LibraryPrimaryButton(
                            text = stringResource(R.string.kids_go_facility_action),
                            onClick = { onGoToChildrenFacility(facility.id) }
                        )
                    }
                }
            }
        }
    }
}

// FilterChip 기본 모서리도 이미 pill에 가깝지만, 선택 상태 색만 어린이 콘텐츠 팔레트(Sky)로
// 맞춰 큰 pill처럼 도드라지게 한다 — 필터 로직(onSelect)은 그대로다.
@Composable
private fun pillChipColors() = FilterChipDefaults.filterChipColors(
    selectedContainerColor = SkyAccent,
    selectedLabelColor = Color.White
)

@Composable
private fun FilterRow(
    allLabel: String,
    options: List<String>,
    selected: String?,
    onSelect: (String?) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = LibraryDimens.ScreenPadding, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(
            selected = selected == null,
            onClick = { onSelect(null) },
            label = { Text(allLabel) },
            colors = pillChipColors()
        )
        options.forEach { option ->
            FilterChip(
                selected = selected == option,
                onClick = { onSelect(option) },
                label = { Text(option) },
                colors = pillChipColors()
            )
        }
    }
}
