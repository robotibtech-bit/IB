package com.example.ibtech.ui.kids

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.ibtech.R
import com.example.ibtech.ui.common.DecorativeBackground
import com.example.ibtech.ui.common.EmptyState
import com.example.ibtech.ui.common.debounced
import com.example.ibtech.ui.theme.CoralAccent
import com.example.ibtech.ui.theme.LavenderAccent
import com.example.ibtech.ui.theme.LavenderAccentContainer
import com.example.ibtech.ui.theme.LibraryDimens
import com.example.ibtech.ui.theme.SuccessAccent

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

            // 문항·선택지가 길어지면(관리자가 입력한 텍스트라 길이 제한이 없다) 고정 화면에
            // 다 안 들어올 수 있어 세로 스크롤을 추가했다 — 정오답 판정 로직과는 무관한 순수
            // 레이아웃 보강이다.
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(LibraryDimens.ScreenPadding),
                verticalArrangement = Arrangement.spacedBy(LibraryDimens.CardSpacing)
            ) {
                QuizProgressBadge(current = uiState.currentIndex + 1, total = uiState.totalCount)

                Text(
                    text = question.question,
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )

                question.choices.forEachIndexed { index, choice ->
                    QuizChoiceButton(
                        label = ('A' + index).toString(),
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
                            tint = if (isCorrect) SuccessAccent else CoralAccent
                        )
                        Text(
                            text = stringResource(
                                if (isCorrect) R.string.quiz_correct_feedback else R.string.quiz_incorrect_feedback
                            ),
                            style = MaterialTheme.typography.titleMedium,
                            color = if (isCorrect) SuccessAccent else CoralAccent
                        )
                    }
                }
            }
        }
    }
}

/** 진행률 pill + progress bar. currentIndex/totalCount는 실제 퀴즈 진행 데이터 그대로다. */
@Composable
private fun QuizProgressBadge(current: Int, total: Int) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(
            modifier = Modifier
                .background(LavenderAccentContainer, RoundedCornerShape(50))
                .padding(horizontal = 20.dp, vertical = 10.dp)
        ) {
            Text(
                text = stringResource(R.string.quiz_progress_format, current, total),
                style = MaterialTheme.typography.labelLarge,
                color = LavenderAccent
            )
        }
        if (total > 0) {
            LinearProgressIndicator(
                progress = { current.toFloat() / total.toFloat() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = LavenderAccent,
                trackColor = LavenderAccentContainer
            )
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

/**
 * 정오답을 색만으로 구분하지 않는다 — 아이콘 유무로도 함께 구분한다(요구사항 3.8절).
 * 정답=Success, 오답(선택함)=Coral로 통일한다(01_DESIGN_SYSTEM.md). [label]은 A/B/C/D 원형 번호.
 */
@Composable
private fun QuizChoiceButton(label: String, text: String, state: ChoiceVisualState, onClick: () -> Unit) {
    val containerColor: Color
    val contentColor: Color
    val badgeBackground: Color
    val icon: ImageVector?

    when (state) {
        ChoiceVisualState.NEUTRAL -> {
            containerColor = MaterialTheme.colorScheme.surface
            contentColor = MaterialTheme.colorScheme.onSurface
            badgeBackground = MaterialTheme.colorScheme.primary
            icon = null
        }

        ChoiceVisualState.CORRECT -> {
            containerColor = SuccessAccent
            contentColor = MaterialTheme.colorScheme.surface
            badgeBackground = MaterialTheme.colorScheme.surface
            icon = Icons.Filled.Check
        }

        ChoiceVisualState.WRONG_SELECTED -> {
            containerColor = CoralAccent
            contentColor = MaterialTheme.colorScheme.surface
            badgeBackground = MaterialTheme.colorScheme.surface
            icon = Icons.Filled.Close
        }

        ChoiceVisualState.DISABLED -> {
            containerColor = MaterialTheme.colorScheme.surfaceVariant
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
            badgeBackground = MaterialTheme.colorScheme.onSurfaceVariant
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
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(badgeBackground.copy(alpha = 0.18f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(text = label, style = MaterialTheme.typography.labelLarge, color = contentColor)
            }
            if (icon != null) {
                Icon(imageVector = icon, contentDescription = null)
            }
            Text(text = text, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        }
    }
}
