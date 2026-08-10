package com.example.ibtech.ui.kids

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.ibtech.R
import com.example.ibtech.domain.model.LibraryEtiquetteTip
import com.example.ibtech.ui.common.DecorativeBackground
import com.example.ibtech.ui.common.EmptyState
import com.example.ibtech.ui.common.LibraryOutlinedButton
import com.example.ibtech.ui.common.LibraryPrimaryButton
import com.example.ibtech.ui.common.debounced
import com.example.ibtech.ui.theme.CoralAccent
import com.example.ibtech.ui.theme.CoralAccentContainer
import com.example.ibtech.ui.theme.LibraryDimens
import com.example.ibtech.ui.theme.SuccessAccent
import com.example.ibtech.ui.theme.SuccessAccentContainer
import com.example.ibtech.ui.theme.YellowAccent
import com.example.ibtech.ui.theme.YellowAccentContainer
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

/**
 * 도서관 예절 화면을 게임으로 바꿨다. 카드 뒤집기/상황극 선택(정답 맞히기)/드래그(커튼 열기) 중
 * 하나를 항목마다 무작위로 골라 보여준다 — 같은 항목이라도 다시 플레이하면 다른 방식이 나올 수
 * 있다.
 *
 * [LibraryEtiquetteTip]에는 자유 문구 하나뿐이라 "정답/오답" 구분이 없다. 상황극 선택 게임에서
 * 쓰는 오답 보기는 실제 관리자 데이터가 아니라 이 화면 안에서만 쓰는 예시 오답 목록
 * ([ETIQUETTE_DECOYS])이다 — 관리자가 등록한 예절 문구를 왜곡하지 않는다.
 *
 * 이미지([etiquetteTipImageRes], [ETIQUETTE_DECOYS]의 imageRes)는 seed 문구 4개·오답 10개에만
 * 매칭되는 res/drawable 그림이다 — 관리자가 새 예절 문구를 추가하면 매칭되는 그림이 없어
 * 자리표시 아이콘으로 자연스럽게 대체된다(그래픽 자산 없이도 깨지지 않는다).
 */
@Composable
fun LibraryEtiquetteScreen(
    uiState: LibraryEtiquetteUiState,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        DecorativeBackground(modifier = Modifier.fillMaxSize())

        if (!uiState.isLoaded) return@Box

        if (uiState.tips.isEmpty()) {
            EmptyState(
                message = stringResource(R.string.kids_etiquette_empty),
                modifier = Modifier.fillMaxSize()
            )
            return@Box
        }

        // shuffleSeed가 바뀌면 orderedTips가 새 목록으로 바뀌고, 그 키를 물고 있는 currentIndex도
        // 자동으로 0부터 다시 시작한다 — "다시 하기"에서 순서를 다시 섞어 매번 다른 순서로 논다.
        var shuffleSeed by remember { mutableIntStateOf(0) }
        val orderedTips = remember(uiState.tips, shuffleSeed) { uiState.tips.shuffled() }
        var currentIndex by remember(orderedTips) { mutableIntStateOf(0) }

        if (currentIndex >= orderedTips.size) {
            EtiquetteCompletionScreen(onRestart = { shuffleSeed++ })
            return@Box
        }

        val tip = orderedTips[currentIndex]
        // tip.id로 키를 잡아, 같은 항목을 다시 보여줄 때(카드 뒤집기 후 뒤로 왔다가 다시 등)마다
        // 게임 방식이 바뀌지 않고 한 라운드 동안은 유지되게 한다.
        val gameType = remember(tip.id) { EtiquetteGameType.entries.random() }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(LibraryDimens.ScreenPadding),
            verticalArrangement = Arrangement.spacedBy(LibraryDimens.CardSpacing)
        ) {
            EtiquetteProgressBadge(current = currentIndex + 1, total = orderedTips.size)

            when (gameType) {
                EtiquetteGameType.CARD_FLIP -> CardFlipGame(tip = tip, onNext = { currentIndex++ })
                EtiquetteGameType.SITUATION_CHOICE -> SituationChoiceGame(tip = tip, onNext = { currentIndex++ })
                EtiquetteGameType.DRAG_REVEAL -> DragRevealGame(tip = tip, onNext = { currentIndex++ })
            }
        }
    }
}

private enum class EtiquetteGameType { CARD_FLIP, SITUATION_CHOICE, DRAG_REVEAL }

/**
 * seed 예절 문구 4개(strings.xml)에만 매칭되는 실제 삽화. 문구가 정확히 일치할 때만 그림이
 * 나오고, 그 외(관리자가 새로 추가한 문구)는 null → 각 게임이 자리표시 아이콘으로 대신한다.
 */
@Composable
private fun etiquetteTipImageRes(tipText: String): Int? {
    val tip1 = stringResource(R.string.kids_etiquette_tip_1)
    val tip2 = stringResource(R.string.kids_etiquette_tip_2)
    val tip3 = stringResource(R.string.kids_etiquette_tip_3)
    val tip4 = stringResource(R.string.kids_etiquette_tip_4)
    return when (tipText) {
        tip1 -> R.drawable.etiquette_book_cart
        tip2 -> R.drawable.etiquette_quiet_voice
        tip3 -> R.drawable.etiquette_clean_hands
        tip4 -> R.drawable.etiquette_wait_turn
        else -> null
    }
}

@Composable
private fun EtiquetteProgressBadge(current: Int, total: Int) {
    Box(
        modifier = Modifier
            .background(YellowAccentContainer, RoundedCornerShape(50))
            .padding(horizontal = 20.dp, vertical = 10.dp)
    ) {
        Text(
            text = stringResource(R.string.quiz_progress_format, current, total),
            style = MaterialTheme.typography.labelLarge,
            color = YellowAccent
        )
    }
}

@Composable
private fun EtiquetteCompletionScreen(onRestart: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(LibraryDimens.ScreenPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterVertically)
    ) {
        Box(
            modifier = Modifier
                .size(LibraryDimens.ListIconCircle)
                .background(YellowAccentContainer, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.EmojiEvents,
                contentDescription = null,
                tint = YellowAccent,
                modifier = Modifier.size(LibraryDimens.ListIcon)
            )
        }
        Text(
            text = stringResource(R.string.kids_etiquette_complete_title),
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center
        )
        Text(
            text = stringResource(R.string.kids_etiquette_complete_subtitle),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        LibraryOutlinedButton(
            text = stringResource(R.string.kids_etiquette_restart_action),
            onClick = onRestart,
            modifier = Modifier.width(240.dp)
        )
    }
}

// ── 1) 카드 뒤집기 ──────────────────────────────────────────────────────────

@Composable
private fun CardFlipGame(tip: LibraryEtiquetteTip, onNext: () -> Unit) {
    var flipped by remember(tip.id) { mutableStateOf(false) }
    val rotation by animateFloatAsState(
        targetValue = if (flipped) 180f else 0f,
        animationSpec = tween(500),
        label = "etiquette_card_flip"
    )
    val showingBack = rotation > 90f
    val imageRes = etiquetteTipImageRes(tip.text)

    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Text(
            text = stringResource(R.string.kids_etiquette_card_flip_hint),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(400.dp)
                .graphicsLayer {
                    rotationY = rotation
                    cameraDistance = 12f * density
                }
                .clip(RoundedCornerShape(28.dp))
                .background(if (showingBack) YellowAccentContainer else YellowAccent)
                .clickable(enabled = !flipped) { flipped = true },
            contentAlignment = Alignment.Center
        ) {
            if (!showingBack) {
                Icon(
                    imageVector = Icons.Filled.TouchApp,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(72.dp)
                )
            } else {
                // 뒷면 내용은 카드 전체와 함께 180도 돌아가 있어, 한 번 더 되돌려 글자가
                // 거울에 비친 것처럼 보이지 않게 한다. 이미지 영역을 카드 전체 너비로 두면
                // (가로가 세로보다 훨씬 넓어져) 원본 삽화가 극단적으로 확대돼 잘린다(실기 확인)
                // — 실제 삽화 비율(약 3:2)에 가까운 고정 크기로 가운데 배치한다.
                Column(
                    modifier = Modifier
                        .graphicsLayer { rotationY = 180f }
                        .fillMaxSize()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    EtiquetteIllustration(
                        imageRes = imageRes,
                        modifier = Modifier
                            .width(260.dp)
                            .height(180.dp)
                    )
                    Text(
                        text = stringResource(R.string.kids_etiquette_card_flip_reveal_label),
                        style = MaterialTheme.typography.labelLarge,
                        color = YellowAccent
                    )
                    Text(
                        text = tip.text,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                        maxLines = 2
                    )
                }
            }
        }
        if (flipped) {
            LibraryPrimaryButton(text = stringResource(R.string.kids_etiquette_next_action), onClick = onNext)
        }
    }
}

/** 예절/오답 삽화 공용 프레임. 실제 이미지가 없으면(관리자가 새로 추가한 문구) 아이콘으로 대신한다. */
@Composable
private fun EtiquetteIllustration(imageRes: Int?, modifier: Modifier = Modifier, cornerRadius: Int = 20) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius.dp))
            .background(Color.White.copy(alpha = 0.5f)),
        contentAlignment = Alignment.Center
    ) {
        if (imageRes != null) {
            Image(
                painter = painterResource(imageRes),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Icon(
                imageVector = Icons.Filled.Image,
                contentDescription = null,
                tint = YellowAccent.copy(alpha = 0.6f),
                modifier = Modifier.size(40.dp)
            )
        }
    }
}

// ── 2) 상황극 선택 ──────────────────────────────────────────────────────────

/**
 * 상황극 선택 게임의 오답 보기 예시 — 실제 도서관 규정이 아니라 "정답을 골라내는 재미"를 위해
 * 이 화면에서만 쓰는 대비용 문구 + 그림이다.
 */
private data class EtiquetteDecoy(val text: String, val imageRes: Int)

private val ETIQUETTE_DECOYS = listOf(
    EtiquetteDecoy("책상에 발을 올려놓기", R.drawable.decoy_feet_on_desk),
    EtiquetteDecoy("책을 아무렇게나 던지기", R.drawable.decoy_throw_book),
    EtiquetteDecoy("복도에서 큰 소리로 뛰어다니기", R.drawable.decoy_run_hallway),
    EtiquetteDecoy("친구와 큰 소리로 이야기하기", R.drawable.decoy_loud_talk),
    EtiquetteDecoy("다른 사람 자리에 함부로 앉기", R.drawable.decoy_sit_others_seat),
    EtiquetteDecoy("책에 낙서하기", R.drawable.decoy_scribble_book),
    EtiquetteDecoy("휴대폰 벨소리를 크게 켜두기", R.drawable.decoy_loud_phone_ring),
    EtiquetteDecoy("쓰레기를 아무 데나 버리기", R.drawable.decoy_litter),
    EtiquetteDecoy("열람실에서 시끄럽게 전화하기", R.drawable.decoy_loud_call),
    EtiquetteDecoy("빌린 책을 아무 곳에나 두고 가기", R.drawable.decoy_leave_book_anywhere)
)

private data class SituationOption(val text: String, val imageRes: Int?, val isCorrect: Boolean)

private enum class ChoiceState { NEUTRAL, CORRECT, WRONG_SELECTED, DISABLED }

@Composable
private fun SituationChoiceGame(tip: LibraryEtiquetteTip, onNext: () -> Unit) {
    val correctImageRes = etiquetteTipImageRes(tip.text)
    val options = remember(tip.id) {
        val decoys = ETIQUETTE_DECOYS.filter { it.text != tip.text }.shuffled().take(2)
        (decoys.map { SituationOption(it.text, it.imageRes, isCorrect = false) } +
            SituationOption(tip.text, correctImageRes, isCorrect = true)).shuffled()
    }
    var selected by remember(tip.id) { mutableStateOf<SituationOption?>(null) }

    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Text(
            text = stringResource(R.string.kids_etiquette_situation_question),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            options.forEach { option ->
                val state = when {
                    selected == null -> ChoiceState.NEUTRAL
                    option.isCorrect -> ChoiceState.CORRECT
                    option == selected -> ChoiceState.WRONG_SELECTED
                    else -> ChoiceState.DISABLED
                }
                SituationChoiceCard(
                    option = option,
                    state = state,
                    onClick = { if (selected == null) selected = option },
                    modifier = Modifier.weight(1f)
                )
            }
        }
        if (selected != null) {
            val isCorrect = selected?.isCorrect == true
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
            LibraryPrimaryButton(text = stringResource(R.string.kids_etiquette_next_action), onClick = onNext)
        }
    }
}

@Composable
private fun SituationChoiceCard(
    option: SituationOption,
    state: ChoiceState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor: Color
    val backgroundColor: Color
    when (state) {
        ChoiceState.NEUTRAL -> {
            borderColor = MaterialTheme.colorScheme.outlineVariant
            backgroundColor = MaterialTheme.colorScheme.surface
        }

        ChoiceState.CORRECT -> {
            borderColor = SuccessAccent
            backgroundColor = SuccessAccentContainer
        }

        ChoiceState.WRONG_SELECTED -> {
            borderColor = CoralAccent
            backgroundColor = CoralAccentContainer
        }

        ChoiceState.DISABLED -> {
            borderColor = MaterialTheme.colorScheme.outlineVariant
            backgroundColor = MaterialTheme.colorScheme.surfaceVariant
        }
    }

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(backgroundColor)
            .border(width = 2.dp, color = borderColor, shape = RoundedCornerShape(24.dp))
            .clickable(enabled = state == ChoiceState.NEUTRAL, onClick = debounced(onClick)),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(modifier = Modifier.fillMaxWidth().height(150.dp)) {
            EtiquetteIllustration(
                imageRes = option.imageRes,
                cornerRadius = 0,
                modifier = Modifier.fillMaxSize()
            )
            if (state == ChoiceState.CORRECT || state == ChoiceState.WRONG_SELECTED) {
                Box(
                    modifier = Modifier
                        .padding(8.dp)
                        .align(Alignment.TopEnd)
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(if (state == ChoiceState.CORRECT) SuccessAccent else CoralAccent),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (state == ChoiceState.CORRECT) Icons.Filled.Check else Icons.Filled.Close,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 72.dp)
                .background(Color.Transparent)
                .padding(12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = option.text,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
        }
    }
}

// ── 3) 드래그로 커튼 열기 ────────────────────────────────────────────────────

@Composable
private fun DragRevealGame(tip: LibraryEtiquetteTip, onNext: () -> Unit) {
    // 커튼 박스 자체가 부모와 같은 너비라서, 드래그 가능 거리를 고정 dp로 잡으면 다 밀어도
    // 커튼의 일부만 옆으로 비켜나 뒤 내용을 절반도 못 가린다(실기 확인) — 실제 측정된 박스
    // 너비만큼 밀어야 커튼이 화면 밖으로 완전히 빠진다.
    var containerWidthPx by remember(tip.id) { mutableFloatStateOf(0f) }
    val offsetX = remember(tip.id) { Animatable(0f) }
    var revealed by remember(tip.id) { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val imageRes = etiquetteTipImageRes(tip.text)

    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Text(
            text = stringResource(R.string.kids_etiquette_drag_hint),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(YellowAccentContainer)
                .onSizeChanged { containerWidthPx = it.width.toFloat() }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                EtiquetteIllustration(
                    imageRes = imageRes,
                    modifier = Modifier
                        .width(220.dp)
                        .fillMaxHeight()
                )
                Text(
                    text = tip.text,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
            }

            // 커튼: 오른쪽으로 드래그해서 밀면 뒤에 있는 예절 문구가 드러난다. 다 열리기 전에
            // 손을 떼면 절반 기준으로 완전히 열리거나(revealed) 원위치로 되돌아간다.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                    .background(YellowAccent, RoundedCornerShape(28.dp))
                    .draggable(
                        orientation = Orientation.Horizontal,
                        enabled = !revealed,
                        state = rememberDraggableState { delta ->
                            scope.launch {
                                offsetX.snapTo((offsetX.value + delta).coerceIn(0f, containerWidthPx))
                            }
                        },
                        onDragStopped = {
                            scope.launch {
                                if (offsetX.value > containerWidthPx * 0.5f) {
                                    offsetX.animateTo(containerWidthPx, tween(220))
                                    revealed = true
                                } else {
                                    offsetX.animateTo(0f, tween(220))
                                }
                            }
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = Color.White)
                    Text(
                        text = stringResource(R.string.kids_etiquette_drag_handle_label),
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }
        if (revealed) {
            LibraryPrimaryButton(text = stringResource(R.string.kids_etiquette_next_action), onClick = onNext)
        }
    }
}
