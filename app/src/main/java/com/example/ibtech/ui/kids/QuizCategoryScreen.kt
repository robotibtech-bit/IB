package com.example.ibtech.ui.kids

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.unit.sp
import com.example.ibtech.R
import androidx.compose.ui.text.style.TextOverflow
import com.example.ibtech.ui.common.DecorativeBackground
import com.example.ibtech.ui.common.FillSpaceGrid
import com.example.ibtech.ui.common.LibraryCard
import com.example.ibtech.ui.common.debounced
import com.example.ibtech.ui.theme.LavenderAccent
import com.example.ibtech.ui.theme.LavenderAccentContainer
import com.example.ibtech.ui.theme.LibraryDimens

/**
 * 퀴즈 주제 선택 화면 (요구사항 명세서 2.11절).
 *
 * 12단계: 등록된 주제 개수만큼 화면을 여백 없이 채우는 큰 카드 격자로 배치한다. 주제 수는
 * 관리자가 등록한 퀴즈에 따라 달라질 수 있어(고정 3~4개가 아님), [FillSpaceGrid]의 열 수를
 * 개수에 맞춰 계산한다.
 */
@Composable
fun QuizCategoryScreen(
    uiState: QuizCategoryUiState,
    onSelectCategory: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        DecorativeBackground(modifier = Modifier.fillMaxSize())

        if (uiState.isLoaded) {
            val columns = if (uiState.categories.size <= 2) uiState.categories.size.coerceAtLeast(1) else 2
            FillSpaceGrid(
                items = uiState.categories,
                columns = columns,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(LibraryDimens.ScreenPadding)
            ) { item, cardModifier ->
                QuizCategoryCard(item = item, onClick = { onSelectCategory(item.name) }, modifier = cardModifier)
            }
        }
    }
}

@Composable
private fun QuizCategoryCard(item: QuizCategoryItem, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val hasQuestions = item.questionCount > 0
    val contentColor = if (hasQuestions) {
        MaterialTheme.colorScheme.onSurface
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    LibraryCard(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (hasQuestions) {
                    Modifier.clickable(onClick = debounced(onClick))
                } else {
                    Modifier
                }
            ),
        // 열람실·퀴즈 = Lavender(01_DESIGN_SYSTEM.md). 문제가 없는 주제는 클릭 불가 상태를
        // 그대로 유지해야 하므로 강조색을 넣지 않고 기존 회색 비활성 표시를 그대로 쓴다.
        accentColor = if (hasQuestions) LavenderAccent else null,
        fillHeight = true
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(LibraryDimens.CardPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(LibraryDimens.LargeIconCircle)
                    .background(
                        color = if (hasQuestions) {
                            LavenderAccentContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        },
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                val emoji = quizCategoryEmoji(item.name)
                if (emoji != null) {
                    Text(text = emoji, fontSize = 88.sp)
                } else {
                    Icon(
                        imageVector = Icons.Filled.Quiz,
                        contentDescription = null,
                        tint = if (hasQuestions) LavenderAccent else contentColor,
                        modifier = Modifier.size(LibraryDimens.LargeIconSize)
                    )
                }
            }
            Text(
                text = item.name,
                style = MaterialTheme.typography.titleLarge,
                color = contentColor,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 16.dp)
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

/**
 * 시드 퀴즈(quiz_seed_200.json) 4개 분야에만 매칭되는 이모지 아이콘. 관리자가 새 분야를
 * 추가하면 매칭되는 이모지가 없어 [Icons.Filled.Quiz] 기본 아이콘으로 자연스럽게 대체된다.
 */
private fun quizCategoryEmoji(name: String): String? = when (name) {
    "공룡" -> "🦕"
    "과학" -> "🔬"
    "동물" -> "🐾"
    "동화" -> "📖"
    else -> null
}
