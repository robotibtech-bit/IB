package com.example.ibtech.ui.kids

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.ibtech.R
import com.example.ibtech.ui.common.DecorativeBackground
import com.example.ibtech.ui.common.EmptyState
import com.example.ibtech.ui.common.debounced
import com.example.ibtech.ui.theme.LibraryDimens

/** 퀴즈 진행 화면 (요구사항 명세서 2.12절). */
@Composable
fun QuizPlayScreen(
    uiState: QuizUiState,
    onSelectChoice: (Int) -> Unit,
    onGoHome: () -> Unit,
    modifier: Modifier = Modifier
) {
    val question = uiState.currentQuestion

    when {
        !uiState.isLoaded -> Unit

        question == null -> EmptyState(
            message = stringResource(R.string.usage_invalid_content),
            actionLabel = stringResource(R.string.top_bar_home),
            onAction = onGoHome,
            modifier = modifier.fillMaxSize()
        )

        else -> Box(modifier = modifier.fillMaxSize()) {
            DecorativeBackground(modifier = Modifier.fillMaxSize())

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(LibraryDimens.ScreenPadding),
                verticalArrangement = Arrangement.spacedBy(LibraryDimens.CardSpacing)
            ) {
                Text(
                    text = stringResource(
                        R.string.quiz_progress_format,
                        uiState.currentIndex + 1,
                        uiState.totalCount
                    ),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = question.question,
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )

                question.choices.forEachIndexed { index, choice ->
                    QuizChoiceButton(
                        text = choice,
                        state = choiceVisualState(index, uiState.selectedChoiceIndex, question.correctIndex),
                        onClick = { onSelectChoice(index) }
                    )
                }

                if (uiState.selectedChoiceIndex != null) {
                    val isCorrect = uiState.selectedChoiceIndex == question.correctIndex
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = if (isCorrect) Icons.Filled.Check else Icons.Filled.Close,
                            contentDescription = null,
                            tint = if (isCorrect) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = stringResource(
                                if (isCorrect) R.string.quiz_correct_feedback else R.string.quiz_incorrect_feedback
                            ),
                            style = MaterialTheme.typography.titleMedium,
                            color = if (isCorrect) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}

private enum class ChoiceVisualState { NEUTRAL, CORRECT, WRONG_SELECTED, DISABLED }

private fun choiceVisualState(index: Int, selected: Int?, correctIndex: Int): ChoiceVisualState = when {
    selected == null -> ChoiceVisualState.NEUTRAL
    index == correctIndex -> ChoiceVisualState.CORRECT
    index == selected -> ChoiceVisualState.WRONG_SELECTED
    else -> ChoiceVisualState.DISABLED
}

/** 정오답을 색만으로 구분하지 않는다 — 아이콘 유무로도 함께 구분한다(요구사항 3.8절). */
@Composable
private fun QuizChoiceButton(text: String, state: ChoiceVisualState, onClick: () -> Unit) {
    val containerColor: Color
    val contentColor: Color
    val icon: ImageVector?

    when (state) {
        ChoiceVisualState.NEUTRAL -> {
            containerColor = MaterialTheme.colorScheme.surface
            contentColor = MaterialTheme.colorScheme.onSurface
            icon = null
        }

        ChoiceVisualState.CORRECT -> {
            containerColor = MaterialTheme.colorScheme.primary
            contentColor = MaterialTheme.colorScheme.onPrimary
            icon = Icons.Filled.Check
        }

        ChoiceVisualState.WRONG_SELECTED -> {
            containerColor = MaterialTheme.colorScheme.error
            contentColor = MaterialTheme.colorScheme.onError
            icon = Icons.Filled.Close
        }

        ChoiceVisualState.DISABLED -> {
            containerColor = MaterialTheme.colorScheme.surfaceVariant
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
            icon = null
        }
    }

    // 채움색이 배경과 거의 같은 NEUTRAL/DISABLED 상태는 테두리가 없으면 버튼처럼 보이지
    // 않는다(흰 배경 위의 흰 버튼) — LibraryOutlinedButton과 같은 테두리 규칙을 맞춘다.
    val border = when (state) {
        ChoiceVisualState.NEUTRAL -> BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        ChoiceVisualState.DISABLED -> BorderStroke(2.dp, MaterialTheme.colorScheme.outlineVariant)
        ChoiceVisualState.CORRECT, ChoiceVisualState.WRONG_SELECTED -> null
    }

    Button(
        onClick = debounced(onClick),
        enabled = state == ChoiceVisualState.NEUTRAL,
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = containerColor,
            disabledContentColor = contentColor
        ),
        border = border,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier
            .fillMaxWidth()
            .height(LibraryDimens.SecondaryButtonHeight)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (icon != null) {
                Icon(imageVector = icon, contentDescription = null)
            }
            Text(text = text, style = MaterialTheme.typography.titleMedium)
        }
    }
}

