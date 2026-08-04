package com.example.ibtech.ui.kids

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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.ibtech.R
import com.example.ibtech.ui.common.DecorativeBackground
import com.example.ibtech.ui.common.EmptyState
import com.example.ibtech.ui.common.LibraryPrimaryButton
import com.example.ibtech.ui.theme.LibraryDimens

/** 추천도서 화면 (요구사항 명세서 2.14절). */
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

        Column(modifier = Modifier.fillMaxSize()) {
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
}

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
            label = { Text(allLabel) }
        )
        options.forEach { option ->
            FilterChip(
                selected = selected == option,
                onClick = { onSelect(option) },
                label = { Text(option) }
            )
        }
    }
}
