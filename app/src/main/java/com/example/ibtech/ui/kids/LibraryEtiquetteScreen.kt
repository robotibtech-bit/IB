package com.example.ibtech.ui.kids

import androidx.compose.foundation.Image
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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

/**
 * 도서관 예절 화면을 여러 미니게임을 섞은 "도서관 미션 게임"으로 구성했다(기획 문서 "5. 도서관
 * 예절" 절: "한 판에 랜덤 5~7라운드만 진행한다"). 라운드마다 [EtiquetteGameType] 4종 중 하나를
 * 무작위로 고른다 — 상황극 선택만 관리자가 등록한 [LibraryEtiquetteTip] 문구를 쓰고, 나머지
 * 3종(좋아요?안돼요!/예절왕 찾기/책을 구해줘!)은 이 화면 안의 고정 이미지 예시 목록만으로
 * 진행되어 문구 개수와 무관하게 항상 라운드를 채울 수 있다. (카드 뒤집기·드래그 커튼·카드 짝
 * 맞추기는 사용자 요청으로 제외했다.)
 *
 * [LibraryEtiquetteTip]에는 자유 문구 하나뿐이라 "정답/오답" 구분이 없다. 상황극 선택에서 쓰는
 * 오답 보기는 실제 관리자 데이터가 아니라 이 화면 안에서만 쓰는 예시 오답 목록
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

        // shuffleSeed가 바뀌면 rounds가 새로 구성되고, 그 키를 물고 있는 currentIndex도 자동으로
        // 0부터 다시 시작한다 — "다시 하기"에서 순서를 다시 섞어 매번 다른 라운드 구성으로 논다.
        var shuffleSeed by remember { mutableIntStateOf(0) }
        val rounds = remember(uiState.tips, shuffleSeed) { buildEtiquetteRounds(uiState.tips) }
        var currentIndex by remember(rounds) { mutableIntStateOf(0) }

        if (currentIndex >= rounds.size) {
            EtiquetteCompletionScreen(onRestart = { shuffleSeed++ })
            return@Box
        }

        val round = rounds[currentIndex]

        // rememberScrollState()를 currentIndex로 키를 걸지 않으면 이전 라운드에서 스크롤한
        // 위치가 다음 라운드까지 그대로 남아 새 라운드의 카드 윗부분이 화면 밖으로 잘려 보인다
        // (실기 확인) — 라운드가 바뀔 때마다 스크롤 위치를 0으로 되돌린다.
        val scrollState = remember(currentIndex) { ScrollState(0) }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(LibraryDimens.ScreenPadding),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            EtiquetteProgressBadge(current = currentIndex + 1, total = rounds.size)

            val onNext: () -> Unit = { currentIndex++ }
            when (round.type) {
                EtiquetteGameType.SITUATION_CHOICE -> SituationChoiceGame(tip = round.tip!!, onNext = onNext)
                EtiquetteGameType.LIKE_OR_NOT -> LikeOrNotGame(seed = currentIndex, onNext = onNext)
                EtiquetteGameType.FIND_THE_ODD_ONE -> FindTheOddOneGame(seed = currentIndex, onNext = onNext)
                EtiquetteGameType.RESCUE_THE_BOOK -> RescueBookGame(seed = currentIndex, onNext = onNext)
            }
        }
    }
}

private enum class EtiquetteGameType { SITUATION_CHOICE, LIKE_OR_NOT, FIND_THE_ODD_ONE, RESCUE_THE_BOOK }

private val TIP_BASED_GAME_TYPES = setOf(EtiquetteGameType.SITUATION_CHOICE)

private data class EtiquetteRound(val type: EtiquetteGameType, val tip: LibraryEtiquetteTip?)

/**
 * 5~7라운드를 무작위로 구성한다(기획 문서 "한 판에 랜덤 5~7라운드만 진행한다"). 상황극 선택이
 * 뽑히면 문구를 순서대로 소비하되, 문구 개수(seed 기준 4개)보다 라운드가 많을 수 있으므로 다
 * 쓰면 다시 섞어 반복한다 — 같은 문구를 두 번 보는 것보다 라운드 부족으로 게임이 조기 종료되는
 * 쪽이 아이 입장에서 더 어색하다.
 */
private fun buildEtiquetteRounds(tips: List<LibraryEtiquetteTip>): List<EtiquetteRound> {
    val roundCount = (5..7).random()
    val tipCycle = generateSequence { tips.shuffled() }.flatten().iterator()
    return (0 until roundCount).map {
        val type = EtiquetteGameType.entries.random()
        val tip = if (type in TIP_BASED_GAME_TYPES) tipCycle.next() else null
        EtiquetteRound(type, tip)
    }
}

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
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        Text(
            text = stringResource(R.string.quiz_progress_format, current, total),
            style = MaterialTheme.typography.labelMedium,
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
            // Crop을 쓰면 상자 가로세로 비율이 원본 그림과 다를 때 그림 일부가 잘려 나간다
            // (실기 확인) — Fit으로 그림 전체가 항상 다 보이게 한다.
            Image(
                painter = painterResource(imageRes),
                contentDescription = null,
                contentScale = ContentScale.Fit,
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

// ── 1) 상황극 선택 (기존) ────────────────────────────────────────────────────

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

    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            text = stringResource(R.string.kids_etiquette_situation_question),
            style = MaterialTheme.typography.titleMedium,
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
            FeedbackWithNextRow(isCorrect = selected?.isCorrect == true, onNext = onNext)
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
        // 카드 바깥쪽 RoundedCornerShape(24.dp) 클립이 이미지 상단 모서리를 바짝 파고들어
        // "위가 잘린" 것처럼 보였다(사용자 피드백) — 이미지를 살짝 아래로 내려 여유를 준다.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 14.dp)
                .height(256.dp)
        ) {
            EtiquetteIllustration(
                imageRes = option.imageRes,
                cornerRadius = 12,
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
                .heightIn(min = 76.dp)
                .background(Color.Transparent)
                .padding(12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = option.text,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
        }
    }
}

// ── 정오답 공용 표시 ─────────────────────────────────────────────────────────

/** 정답/오답 아이콘+문구. 여러 게임(상황극 선택 포함)이 같은 정답/오답 문구
 * ([R.string.quiz_correct_feedback]/[R.string.quiz_incorrect_feedback])를 공유한다 — 게임마다
 * 다른 문구가 필요하면 [correctTextRes]/[incorrectTextRes]로 바꿔 쓴다. */
@Composable
private fun FeedbackRow(
    isCorrect: Boolean,
    correctTextRes: Int = R.string.quiz_correct_feedback,
    incorrectTextRes: Int = R.string.quiz_incorrect_feedback,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = if (isCorrect) Icons.Filled.Check else Icons.Filled.Close,
            contentDescription = null,
            tint = if (isCorrect) SuccessAccent else CoralAccent
        )
        Text(
            text = stringResource(if (isCorrect) correctTextRes else incorrectTextRes),
            style = MaterialTheme.typography.titleMedium,
            color = if (isCorrect) SuccessAccent else CoralAccent
        )
    }
}

/** 정오답 문구와 "다음" 버튼을 한 줄에 나란히 배치해(오른쪽 공간에 버튼) 세로 공간을 아껴
 * 라운드 내용이 스크롤 없이 한 화면에 들어오게 한다. */
@Composable
private fun FeedbackWithNextRow(
    isCorrect: Boolean,
    onNext: () -> Unit,
    correctTextRes: Int = R.string.quiz_correct_feedback,
    incorrectTextRes: Int = R.string.quiz_incorrect_feedback
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        FeedbackRow(
            isCorrect = isCorrect,
            correctTextRes = correctTextRes,
            incorrectTextRes = incorrectTextRes,
            modifier = Modifier.weight(1f)
        )
        Box(modifier = Modifier.width(160.dp)) {
            LibraryPrimaryButton(text = stringResource(R.string.kids_etiquette_next_action), onClick = onNext)
        }
    }
}

// ── 2) 좋아요? 안 돼요! / 3) 예절왕·범인을 찾아라 / 4) 책을 구해줘! 공용 이미지 목록 ─────────

/** [ETIQUETTE_DECOYS]는 "오답 보기 문구"만 있던 목록이라, 올바른 행동 쪽도 같은 모양
 * ([EtiquetteImage])으로 맞춰 둔 목록. seed 예절 문구 4개와 같은 그림([etiquetteTipImageRes]
 * 매칭 대상)이지만, 이 화면 예시 전용 문구라 관리자 문구와는 별개다([ETIQUETTE_DECOYS]와 같은
 * 이유). */
private data class EtiquetteImage(val text: String, val imageRes: Int, val isGood: Boolean)

private val ETIQUETTE_GOOD_IMAGES = listOf(
    EtiquetteImage("책을 다 읽은 뒤에는 책 수레에 놓아요", R.drawable.etiquette_book_cart, isGood = true),
    EtiquetteImage("도서관에서는 작은 목소리로 말해요", R.drawable.etiquette_quiet_voice, isGood = true),
    EtiquetteImage("손을 깨끗이 씻고 책장을 조심히 넘겨요", R.drawable.etiquette_clean_hands, isGood = true),
    EtiquetteImage("친구가 읽고 있는 책은 차례를 기다려요", R.drawable.etiquette_wait_turn, isGood = true)
)

private val ETIQUETTE_BAD_IMAGES = ETIQUETTE_DECOYS.map { EtiquetteImage(it.text, it.imageRes, isGood = false) }
private val ETIQUETTE_ALL_IMAGES = ETIQUETTE_GOOD_IMAGES + ETIQUETTE_BAD_IMAGES

/** "책을 구해줘!" 전용 — 책을 직접 다루는 행동만 골라 둔다(기획 문서 게임 C 예시와 동일). */
private val BOOK_CARE_GOOD = listOf(ETIQUETTE_GOOD_IMAGES[0], ETIQUETTE_GOOD_IMAGES[2])
private val BOOK_CARE_BAD_RES = setOf(R.drawable.decoy_scribble_book, R.drawable.decoy_throw_book, R.drawable.decoy_leave_book_anywhere)
private val BOOK_CARE_BAD = ETIQUETTE_BAD_IMAGES.filter { it.imageRes in BOOK_CARE_BAD_RES }

// ── 2) 좋아요? 안 돼요! ──────────────────────────────────────────────────────

@Composable
private fun LikeOrNotGame(seed: Int, onNext: () -> Unit) {
    val target = remember(seed) { ETIQUETTE_ALL_IMAGES.random() }
    // 사용자가 누른 값(true=좋아요). 아직 안 눌렀으면 null.
    var picked by remember(seed) { mutableStateOf<Boolean?>(null) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = stringResource(R.string.kids_etiquette_like_or_not_question),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.fillMaxWidth()
        )
        EtiquetteIllustration(
            imageRes = target.imageRes,
            modifier = Modifier
                .width(600.dp)
                .height(410.dp)
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            LikeOrNotButton(
                text = stringResource(R.string.kids_etiquette_like_action),
                accent = SuccessAccent,
                enabled = picked == null,
                onClick = { picked = true },
                modifier = Modifier.weight(1f)
            )
            LikeOrNotButton(
                text = stringResource(R.string.kids_etiquette_dislike_action),
                accent = CoralAccent,
                enabled = picked == null,
                onClick = { picked = false },
                modifier = Modifier.weight(1f)
            )
        }
        val answer = picked
        if (answer != null) {
            FeedbackWithNextRow(isCorrect = answer == target.isGood, onNext = onNext)
        }
    }
}

@Composable
private fun LikeOrNotButton(
    text: String,
    accent: Color,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (enabled) accent else accent.copy(alpha = 0.4f))
            .clickable(enabled = enabled, onClick = debounced(onClick))
            .padding(vertical = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text = text, style = MaterialTheme.typography.titleLarge, color = Color.White)
    }
}

// ── 3) 예절왕·범인을 찾아라 ───────────────────────────────────────────────────

@Composable
private fun FindTheOddOneGame(seed: Int, onNext: () -> Unit) {
    // "예절왕(좋은 행동 1개)을 찾기"만 진행한다 — "범인(나쁜 행동 1개)을 찾기" 문구는 사용자
    // 요청으로 제외했다.
    val options = remember(seed) {
        (ETIQUETTE_BAD_IMAGES.shuffled().take(2) + ETIQUETTE_GOOD_IMAGES.random()).shuffled()
    }
    var selected by remember(seed) { mutableStateOf<EtiquetteImage?>(null) }

    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            text = stringResource(R.string.kids_etiquette_odd_one_good_prompt),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            options.forEach { option ->
                val isTarget = option.isGood
                val state = when {
                    selected == null -> ChoiceState.NEUTRAL
                    isTarget -> ChoiceState.CORRECT
                    option == selected -> ChoiceState.WRONG_SELECTED
                    else -> ChoiceState.DISABLED
                }
                SituationChoiceCard(
                    option = SituationOption(option.text, option.imageRes, isCorrect = isTarget),
                    state = state,
                    onClick = { if (selected == null) selected = option },
                    modifier = Modifier.weight(1f)
                )
            }
        }
        val current = selected
        if (current != null) {
            FeedbackWithNextRow(isCorrect = current.isGood, onNext = onNext)
        }
    }
}

// ── 4) 책을 구해줘! ──────────────────────────────────────────────────────────

@Composable
private fun RescueBookGame(seed: Int, onNext: () -> Unit) {
    val correct = remember(seed) { BOOK_CARE_GOOD.random() }
    val options = remember(seed) { (BOOK_CARE_BAD.shuffled().take(2) + correct).shuffled() }
    var solved by remember(seed) { mutableStateOf(false) }
    // 오답을 고르면 즉시 끝내지 않고 그 카드만 잠가서(기획: "한 번 더 선택할 수 있게 한다") 다시
    // 고를 수 있게 한다 — 시도할수록 정답 쪽으로 좁혀진다.
    var triedWrong by remember(seed) { mutableStateOf(setOf<EtiquetteImage>()) }

    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            text = stringResource(R.string.kids_etiquette_rescue_book_prompt),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            options.forEach { option ->
                val state = when {
                    solved && option == correct -> ChoiceState.CORRECT
                    option in triedWrong -> ChoiceState.WRONG_SELECTED
                    else -> ChoiceState.NEUTRAL
                }
                SituationChoiceCard(
                    option = SituationOption(option.text, option.imageRes, isCorrect = option == correct),
                    state = state,
                    onClick = {
                        if (!solved && option !in triedWrong) {
                            if (option == correct) solved = true else triedWrong = triedWrong + option
                        }
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }
        if (solved) {
            FeedbackWithNextRow(
                isCorrect = true,
                onNext = onNext,
                correctTextRes = R.string.kids_etiquette_rescue_book_success
            )
        } else if (triedWrong.isNotEmpty()) {
            FeedbackRow(isCorrect = false, incorrectTextRes = R.string.kids_etiquette_rescue_book_retry)
        }
    }
}
