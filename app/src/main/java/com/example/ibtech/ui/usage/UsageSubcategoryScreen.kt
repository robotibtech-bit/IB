package com.example.ibtech.ui.usage

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.ibtech.R
import com.example.ibtech.domain.model.UsageTopic
import com.example.ibtech.ui.common.DecorativeBackground
import com.example.ibtech.ui.common.EmptyState
import com.example.ibtech.ui.common.LibraryCard
import com.example.ibtech.ui.common.debounced
import com.example.ibtech.ui.theme.LibraryDimens

/** 이용방법 세부 항목 목록 화면 (요구사항 명세서 2.8절). */
@Composable
fun UsageSubcategoryScreen(
    uiState: UsageSubcategoryUiState,
    onSelectTopic: (UsageTopic) -> Unit,
    onGoHome: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        DecorativeBackground(modifier = Modifier.fillMaxSize())

        when {
            !uiState.isLoaded -> Unit

            uiState.category == null || uiState.subtopics.isEmpty() -> EmptyState(
                message = stringResource(R.string.usage_invalid_content),
                actionLabel = stringResource(R.string.top_bar_home),
                onAction = onGoHome,
                modifier = Modifier.fillMaxSize()
            )

            else -> LazyColumn(
                contentPadding = PaddingValues(LibraryDimens.ScreenPadding),
                verticalArrangement = Arrangement.spacedBy(LibraryDimens.CardSpacing),
                modifier = Modifier.fillMaxSize()
            ) {
                items(uiState.subtopics, key = { it.id }) { topic ->
                    UsageTopicRow(topic = topic, onClick = { onSelectTopic(topic) })
                }
            }
        }
    }
}

@Composable
private fun UsageTopicRow(topic: UsageTopic, onClick: () -> Unit) {
    LibraryCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = debounced(onClick))
    ) {
        Text(
            text = topic.title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(LibraryDimens.CardPadding)
        )
    }
}
