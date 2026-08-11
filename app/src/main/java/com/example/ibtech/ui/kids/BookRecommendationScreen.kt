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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.example.ibtech.domain.model.BookTasteOption
import com.example.ibtech.domain.model.BookTasteQuestion
import com.example.ibtech.domain.model.RecommendedBook
import com.example.ibtech.ui.common.DecorativeBackground
import com.example.ibtech.ui.common.EmptyState
import com.example.ibtech.ui.common.LibraryCard
import com.example.ibtech.ui.common.LibraryOutlinedButton
import com.example.ibtech.ui.common.LibraryPrimaryButton
import com.example.ibtech.ui.common.debounced
import com.example.ibtech.ui.theme.LavenderAccent
import com.example.ibtech.ui.theme.LibraryDimens
import com.example.ibtech.ui.theme.SkyAccent
import com.example.ibtech.ui.theme.SkyAccentContainer

/**
 * 추천도서 화면 (요구사항 명세서 2.14절) — "몇 가지 질문 → 어울리는 책 추천" 게임.
 *
 * 기획 문서 "2. 나에게 맞는 책" 절에 따라 고정 6문항(연령대/주제 2문항짜리 예전 버전 대신
 * [BookTasteQuestion] 취향 퀴즈)에 답하면 [com.example.ibtech.domain.usecase.BookTasteEngine]이
 * 점수를 매겨 3권을 추천한다. 원래 있던 연령/주제 필터-목록 탐색 화면은 그대로 남겨
 * [BookGameStep.BROWSE_ALL]로 "내가 직접 골라볼래요"를 누르면 볼 수 있다 — 관리자가 태그 없이
 * 추가한 책도 그 화면에서는 기존과 동일하게 보인다.
 */
@Composable
fun BookRecommendationScreen(
    uiState: BookRecommendationUiState,
    onAnswerTasteQuestion: (BookTasteOption) -> Unit,
    onShowMoreBooks: () -> Unit,
    onRestartTaste: () -> Unit,
    onSelectAgeGroup: (String?) -> Unit,
    onSelectTopic: (String?) -> Unit,
    onResetFilters: () -> Unit,
    onGoToChildrenFacility: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        DecorativeBackground(modifier = Modifier.fillMaxSize())

        if (!uiState.isLoaded) return@Box

        var step by remember { mutableStateOf(BookGameStep.TASTE_QUESTION) }

        // 마지막 문항에 답해 ViewModel이 결과를 계산해 내놓으면(tasteQuestionIndex가 전체 문항
        // 수에 도달) 결과 화면으로 자동 전환한다.
        LaunchedEffect(uiState.tasteQuestionIndex, uiState.tasteQuestions.size) {
            if (step == BookGameStep.TASTE_QUESTION &&
                uiState.tasteQuestions.isNotEmpty() &&
                uiState.tasteQuestionIndex >= uiState.tasteQuestions.size
            ) {
                step = BookGameStep.REVEAL
            }
        }

        when (step) {
            BookGameStep.TASTE_QUESTION -> {
                val question = uiState.tasteQuestions.getOrNull(uiState.tasteQuestionIndex)
                if (question != null) {
                    BookTasteQuestionContent(
                        question = question,
                        current = uiState.tasteQuestionIndex + 1,
                        total = uiState.tasteQuestions.size,
                        onSelect = onAnswerTasteQuestion
                    )
                }
            }

            BookGameStep.REVEAL -> BookGameReveal(
                uiState = uiState,
                onShowMoreBooks = onShowMoreBooks,
                onRestart = {
                    onRestartTaste()
                    step = BookGameStep.TASTE_QUESTION
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
                    onRestartTaste()
                    step = BookGameStep.TASTE_QUESTION
                }
            )
        }
    }
}

private enum class BookGameStep { TASTE_QUESTION, REVEAL, BROWSE_ALL }

@Composable
private fun BookTasteQuestionContent(
    question: BookTasteQuestion,
    current: Int,
    total: Int,
    onSelect: (BookTasteOption) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(LibraryDimens.ScreenPadding),
        verticalArrangement = Arrangement.spacedBy(LibraryDimens.CardSpacing)
    ) {
        Box(
            modifier = Modifier
                .background(SkyAccentContainer, RoundedCornerShape(50))
                .padding(horizontal = 20.dp, vertical = 10.dp)
        ) {
            Text(
                text = stringResource(R.string.quiz_progress_format, current, total),
                style = MaterialTheme.typography.labelLarge,
                color = SkyAccent
            )
        }
        Text(
            text = question.question,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground
        )
        question.options.forEach { option ->
            BookGameOptionCard(label = option.label, onClick = { onSelect(option) })
        }
    }
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
    onShowMoreBooks: () -> Unit,
    onRestart: () -> Unit,
    onBrowseAll: () -> Unit,
    onGoToChildrenFacility: (String) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(LibraryDimens.ScreenPadding),
        verticalArrangement = Arrangement.spacedBy(LibraryDimens.CardSpacing),
        modifier = Modifier.fillMaxSize()
    ) {
        val characterKey = uiState.resultCharacterKey
        if (characterKey != null && uiState.recommendedBooks.isNotEmpty()) {
            item { BookResultCharacterBanner(characterKey = characterKey) }
        }

        item {
            Text(
                text = if (uiState.recommendedBooks.isNotEmpty()) {
                    stringResource(R.string.kids_book_taste_result_heading)
                } else {
                    stringResource(R.string.kids_book_game_reveal_fallback)
                },
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        if (uiState.recommendedBooks.isNotEmpty()) {
            items(uiState.recommendedBooks, key = { it.id }) { book -> BookTasteResultCard(book = book) }
            item {
                LibraryOutlinedButton(
                    text = stringResource(R.string.kids_book_taste_show_more_action),
                    onClick = onShowMoreBooks
                )
            }
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
                text = stringResource(R.string.kids_book_taste_restart_action),
                onClick = onRestart
            )
        }
    }
}

@Composable
private fun BookResultCharacterBanner(characterKey: String) {
    val (titleRes, taglineRes) = characterResources(characterKey)
    LibraryCard(modifier = Modifier.fillMaxWidth(), accentColor = LavenderAccent) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(LibraryDimens.CardPadding),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(text = stringResource(titleRes), style = MaterialTheme.typography.headlineSmall)
            Text(
                text = stringResource(taglineRes),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun characterResources(key: String): Pair<Int, Int> = when (key) {
    "wizard" -> R.string.kids_book_character_wizard to R.string.kids_book_character_wizard_tagline
    "explorer" -> R.string.kids_book_character_explorer to R.string.kids_book_character_explorer_tagline
    "comedian" -> R.string.kids_book_character_comedian to R.string.kids_book_character_comedian_tagline
    "animal_friend" -> R.string.kids_book_character_animal_friend to R.string.kids_book_character_animal_friend_tagline
    "scientist" -> R.string.kids_book_character_scientist to R.string.kids_book_character_scientist_tagline
    "time_traveler" -> R.string.kids_book_character_time_traveler to R.string.kids_book_character_time_traveler_tagline
    "adventurer" -> R.string.kids_book_character_adventurer to R.string.kids_book_character_adventurer_tagline
    else -> R.string.kids_book_character_warm_collector to R.string.kids_book_character_warm_collector_tagline
}

/** 추천 이유 한 줄 + 책 카드 + 재미/호기심/감동 별점 + 난이도 (기획 문서 "4. 책 추천 결과" 절). */
@Composable
private fun BookTasteResultCard(book: RecommendedBook) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(reasonRes(book)),
            style = MaterialTheme.typography.bodyMedium,
            color = SkyAccent
        )
        RecommendedBookCard(book = book)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            StarStat(label = stringResource(R.string.kids_book_star_fun), filled = starCount(book, MOOD_FUN))
            StarStat(label = stringResource(R.string.kids_book_star_curious), filled = starCount(book, MOOD_CURIOUS))
            StarStat(label = stringResource(R.string.kids_book_star_touching), filled = starCount(book, MOOD_TOUCHING))
            Text(
                text = difficultyLabel(book),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private val MOOD_FUN = setOf("mood:exciting", "mood:funny")
private val MOOD_CURIOUS = setOf("mood:curious")
private val MOOD_TOUCHING = setOf("mood:touching", "mood:warm")

private fun starCount(book: RecommendedBook, matchTags: Set<String>): Int =
    if (book.tags.any { it in matchTags }) 3 else 1

@Composable
private fun StarStat(label: String, filled: Int, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Row {
            repeat(3) { index ->
                Icon(
                    imageVector = if (index < filled) Icons.Filled.Star else Icons.Filled.StarBorder,
                    contentDescription = null,
                    tint = LavenderAccent,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun reasonRes(book: RecommendedBook): Int {
    val genre = book.tags.firstOrNull { it.startsWith("genre:") }?.removePrefix("genre:")
    return when (genre) {
        "fantasy" -> R.string.kids_book_reason_fantasy
        "mystery" -> R.string.kids_book_reason_mystery
        "humor" -> R.string.kids_book_reason_humor
        "animal" -> R.string.kids_book_reason_animal
        "science" -> R.string.kids_book_reason_science
        "history" -> R.string.kids_book_reason_history
        "friendship" -> R.string.kids_book_reason_friendship
        "emotion" -> R.string.kids_book_reason_emotion
        "adventure" -> R.string.kids_book_reason_adventure
        "daily" -> R.string.kids_book_reason_daily
        else -> R.string.kids_book_reason_default
    }
}

@Composable
private fun difficultyLabel(book: RecommendedBook): String {
    val ageTag = book.tags.firstOrNull { it.startsWith("age:") }?.removePrefix("age:")
    return when (ageTag) {
        "lower" -> stringResource(R.string.kids_book_level_lower)
        "upper" -> stringResource(R.string.kids_book_level_upper)
        else -> stringResource(R.string.kids_book_level_middle)
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
