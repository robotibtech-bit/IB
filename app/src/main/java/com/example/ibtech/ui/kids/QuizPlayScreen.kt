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
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
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
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                val selectedIndex = uiState.selectedChoiceIndex
                QuizProgressBadge(
                    current = uiState.currentIndex + 1,
                    total = uiState.totalCount,
                    isCorrect = selectedIndex?.let { it == question.correctIndex }
                )

                Text(
                    text = question.question,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    QuizQuestionImage(imageKey = question.imageKey, modifier = Modifier.size(380.dp))
                }

                // 보기를 세로 한 줄씩 쌓지 않고 2열 격자로 배치한다 — 4보기 문제도 문제 문구·
                // 문제 그림·보기 전부가 스크롤 없이 한 화면에 들어오게 하기 위해서다(사용자
                // 요청). 2보기 문제는 자연히 한 줄(2칸)만 채운다.
                question.choices.chunked(2).forEachIndexed { rowIndex, rowChoices ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        rowChoices.forEachIndexed { columnIndex, choice ->
                            val index = rowIndex * 2 + columnIndex
                            QuizChoiceButton(
                                label = ('A' + index).toString(),
                                text = choice,
                                // TODO(이미지 준비되면): 문제별 보기 이미지 매핑이 생기면 여기로 넘긴다.
                                // 지금은 자리만 마련해 둔 상태라 항상 null이다.
                                imageRes = null,
                                state = choiceVisualState(index, uiState.selectedChoiceIndex, question.correctIndex),
                                onClick = { onSelectChoice(index) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 문제 삽화. [imageKey]("dinosaur_01" 등, 확장자 제외 `res/drawable` 리소스 이름)에 해당하는
 * PNG가 없으면(관리자가 이미지 없이 새 문제를 추가한 경우) [resources.getIdentifier]가 0을
 * 반환하므로 공통 기본 이미지(아이콘)로 대체한다 — 나중에 같은 이름의 PNG를 `res/drawable`에
 * 넣기만 하면 코드 수정 없이 자동으로 반영된다.
 *
 * 문제은행이 200장(720x720 PNG)이라 전부 `painterResource`로 미리 디코딩해 두면 메모리 부담이
 * 크다 — Coil의 [AsyncImage]로 지금 보이는 문제 이미지 하나만 지연 로딩하고, Coil의 자체
 * 메모리 캐시(고정 상한)가 화면을 벗어난 이미지를 알아서 회수한다.
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
            AsyncImage(
                model = resId,
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

/**
 * 진행률 pill + progress bar. pill 오른쪽의 빈 공간에 정오답 결과를 바로 보여준다([isCorrect]가
 * null이 아니면) — 예전에는 이 결과 문구를 보기 4개 아래에 따로 그려서 한 화면에 다 안 들어올
 * 때가 있었는데(사용자 피드백), 이 자리는 원래 비어 있던 공간이라 화면 높이를 더 쓰지 않고도
 * 보여줄 수 있다.
 */
@Composable
private fun QuizProgressBadge(current: Int, total: Int, isCorrect: Boolean?) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
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
            if (isCorrect != null) {
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
    onClick: () -> Unit,
    modifier: Modifier = Modifier
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
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = LibraryDimens.SecondaryButtonHeight)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // labelLarge(41sp, 키오스크용 확대 타이포)를 이 작은 원에 그대로 쓰면 글자가 원
            // 밖으로 넘쳐 옆 요소(보기 텍스트 등)에 그려져 잘린 것처럼 보였다(사용자 피드백) —
            // 원 크기에 맞는 폰트 크기로 줄이고 clip으로 넘치는 부분을 막는다.
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(badgeBackground.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge.copy(fontSize = 20.sp),
                    color = contentColor
                )
            }
            // 보기별 이미지가 아직 없는 동안(항상 null, 위 TODO 참고) 빈 원형 자리표시까지
            // 그리면 2열 격자에서 좁아진 버튼 폭을 불필요하게 더 잡아먹는다 — 실제 이미지가
            // 생겼을 때만 자리를 차지하게 한다.
            if (imageRes != null) {
                QuizOptionImageSlot(imageRes = imageRes, tint = contentColor)
            }
            // titleSmall은 이 앱 타이포그래피(Type.kt)에 별도로 정의돼 있지 않아 Material3
            // 기본값(14sp)으로 떨어져 글자가 작고 흐릿해 보였다(사용자 피드백) — titleMedium
            // 굵기를 유지한 채 2열 격자 폭에 맞는 크기로 직접 지정한다.
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium.copy(fontSize = 26.sp),
                modifier = Modifier.weight(1f)
            )
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
