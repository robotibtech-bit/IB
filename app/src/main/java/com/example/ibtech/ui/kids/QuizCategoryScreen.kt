package com.example.ibtech.ui.kids

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.ibtech.R
import com.example.ibtech.ui.common.DecorativeBackground
import com.example.ibtech.ui.common.LibraryCard
import com.example.ibtech.ui.common.debounced
import com.example.ibtech.ui.theme.LibraryDimens

/** 퀴즈 주제 선택 화면 (요구사항 명세서 2.11절). */
@Composable
fun QuizCategoryScreen(
    uiState: QuizCategoryUiState,
    onSelectCategory: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        DecorativeBackground(modifier = Modifier.fillMaxSize())

        if (uiState.isLoaded) {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 220.dp),
                contentPadding = PaddingValues(LibraryDimens.ScreenPadding),
                verticalArrangement = Arrangement.spacedBy(LibraryDimens.CardSpacing),
                horizontalArrangement = Arrangement.spacedBy(LibraryDimens.CardSpacing),
                modifier = Modifier.fillMaxSize()
            ) {
                items(uiState.categories, key = { it.name }) { item ->
                    QuizCategoryCard(
                        item = item,
                        onClick = { onSelectCategory(item.name) }
                    )
                }
            }
        }
    }
}

@Composable
private fun QuizCategoryCard(item: QuizCategoryItem, onClick: () -> Unit) {
    val hasQuestions = item.questionCount > 0
    val contentColor = if (hasQuestions) {
        MaterialTheme.colorScheme.onSurface
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    LibraryCard(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (hasQuestions) {
                    Modifier.clickable(onClick = debounced(onClick))
                } else {
                    Modifier
                }
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(LibraryDimens.CardPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(
                        color = if (hasQuestions) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        },
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Quiz,
                    contentDescription = null,
                    tint = if (hasQuestions) MaterialTheme.colorScheme.primary else contentColor
                )
            }
            Text(
                text = item.name,
                style = MaterialTheme.typography.titleLarge,
                color = contentColor,
                textAlign = TextAlign.Center
            )
            if (!hasQuestions) {
                Text(
                    text = stringResource(R.string.quiz_category_empty_label),
                    style = MaterialTheme.typography.labelLarge,
                    color = contentColor,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
