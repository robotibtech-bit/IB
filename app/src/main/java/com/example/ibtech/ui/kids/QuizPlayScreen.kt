package com.example.ibtech.ui.kids

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
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
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    QuizQuestionImage(imageKey = question.imageKey, modifier = Modifier.size(380.dp))
                }

                question.choices.forEachIndexed { index, choice ->
                    QuizChoiceButton(
                        label = ('A' + index).toString(),
                        text = choice,
                        // TODO(이미지 준비되면): 문제별 보기 이미지 매핑이 생기면 여기로 넘긴다.
                        // 지금은 자리만 마련해 둔 상태라 항상 null이다.
                        imageRes = null,
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

/**
 * 문제 삽화. [imageKey]("dinosaur_01" 등, 확장자 제외 `res/drawable` 리소스 이름)에 해당하는
 * PNG가 아직 없으면(200문제 중 88개는 의도적으로 비어 있음, `docs/CLAUDE_QUIZ_IMAGE_HANDOFF_
 * OPTIMIZED_112_OF_200` 인계본 기준) [resources.getIdentifier]가 0을 반환하므로 공통 기본
 * 이미지(아이콘)로 대체한다 — 나중에 매핑표와 같은 이름의 PNG를 `res/drawable`에 넣기만 하면
 * 코드 수정 없이 자동으로 반영된다.
 */
@Composable
private fun QuizQuestionImage(imageKey: String?, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val resId = remember(imageKey) {
        imageKey
            ?.let { context.resources.getIdentifier(it, "drawable", context.packageName) }
            ?.takeIf { it != 0 }
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(LavenderAccentContainer),
        contentAlignment = Alignment.Center
    ) {
        if (resId != null) {
            // 원본 이미지가 정사각형(720x720)이라 Crop을 쓰면 좌우 폭이 훨씬 넓은 이 박스에서
            // 이미지 대부분이 잘려 나간다 — Fit으로 전체 그림이 잘리지 않고 다 보이게 한다.
            Image(
                painter = painterResource(resId),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Icon(
                imageVector = Icons.Filled.Image,
                contentDescription = null,
                tint = LavenderAccent,
                modifier = Modifier.size(56.dp)
            )
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
private fun QuizChoiceButton(
    label: String,
    text: String,
    imageRes: Int?,
    state: ChoiceVisualState,
    onClick: () -> Unit
) {
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
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = LibraryDimens.SecondaryButtonHeight)
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
            QuizOptionImageSlot(imageRes = imageRes, tint = contentColor)
            Text(text = text, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            if (icon != null) {
                Icon(imageVector = icon, contentDescription = null)
            }
        }
    }
}

/**
 * 보기 이미지 자리표시. 도서관 예절 게임의 `OptionImageSlot`과 같은 역할이다 — 실제 이미지가
 * 준비되면 `Icon` 대신 `Image(painterResource(imageRes), ...)`로 바꿔 끼운다.
 */
@Composable
private fun QuizOptionImageSlot(imageRes: Int?, tint: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(56.dp)
            .clip(CircleShape)
            .background(tint.copy(alpha = 0.12f)),
        contentAlignment = Alignment.Center
    ) {
        if (imageRes != null) {
            Image(
                painter = painterResource(imageRes),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
            )
        } else {
            Icon(
                imageVector = Icons.Filled.Image,
                contentDescription = null,
                tint = tint.copy(alpha = 0.6f),
                modifier = Modifier.size(28.dp)
            )
        }
    }
}
