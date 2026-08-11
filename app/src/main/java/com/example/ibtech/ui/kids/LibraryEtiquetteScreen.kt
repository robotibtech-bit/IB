package com.example.ibtech.ui.kids

import androidx.compose.foundation.Image
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.ui.unit.sp
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
 * 예절" 절: "한 판에 랜덤 5~7라운드만 진행한다"). 라운드마다 [EtiquetteGameType] 3종 중 하나를
 * 무작위로 고른다 — 상황극 선택만 관리자가 등록한 [LibraryEtiquetteTip] 문구를 쓰고, 나머지
 * 2종(좋아요?안돼요!/예절왕 찾기)은 이 화면 안의 고정 이미지 예시 목록만으로 진행되어 문구
 * 개수와 무관하게 항상 라운드를 채울 수 있다. ("책을 구해줘!"는 사용자 요청으로 제거했다. 카드
 * 뒤집기·드래그 커튼·카드 짝 맞추기도 이전 사용자 요청으로 제외했다.)
 *
 * [LibraryEtiquetteTip]에는 자유 문구 하나뿐이라 "정답/오답" 구분이 없다. 상황극 선택에서 쓰는
 * 오답 보기는 실제 관리자 데이터가 아니라 이 화면 안에서만 쓰는 예시 오답 목록
 * ([ETIQUETTE_DECOYS])이다 — 관리자가 등록한 예절 문구를 왜곡하지 않는다.
 *
 * 이미지([etiquetteTipImageRes], [ETIQUETTE_DECOYS]의 imageRes)는 seed 문구 4개·오답 18개에만
 * 매칭되는 res/drawable 그림(`docs/library_etiquette_assets` 30장 세트)이다 — 관리자가 새 예절
 * 문구를 추가하면 매칭되는 그림이 없어 자리표시 아이콘으로 자연스럽게 대체된다(그래픽 자산 없이도
 * 깨지지 않는다).
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
                EtiquetteGameType.SITUATION_CHOICE -> SituationChoiceGame(
                    tip = round.tip!!,
                    decoys = round.situationDecoys!!,
                    onNext = onNext
                )
                EtiquetteGameType.LIKE_OR_NOT -> LikeOrNotGame(target = round.likeOrNotTarget!!, onNext = onNext)
                EtiquetteGameType.FIND_THE_ODD_ONE -> FindTheOddOneGame(options = round.oddOneOptions!!, onNext = onNext)
            }
        }
    }
}

internal enum class EtiquetteGameType { SITUATION_CHOICE, LIKE_OR_NOT, FIND_THE_ODD_ONE }

private val TIP_BASED_GAME_TYPES = setOf(EtiquetteGameType.SITUATION_CHOICE)

/**
 * 라운드별로 어떤 미니게임을 낼지뿐 아니라 그 라운드에서 쓸 그림까지 미리 정해 둔다 — 그래야
 * 한 판(5~7라운드) 전체에서 같은 그림이 겹치지 않게 미리 조율할 수 있다(사용자 피드백: "같은
 * 이미지가 중복으로 나온다"). 각 게임 종류는 자기 필드만 채워져 있다.
 */
internal data class EtiquetteRound(
    val type: EtiquetteGameType,
    val tip: LibraryEtiquetteTip? = null,
    val situationDecoys: List<EtiquetteDecoy>? = null,
    val likeOrNotTarget: EtiquetteImage? = null,
    val oddOneOptions: List<EtiquetteImage>? = null
)

/**
 * 5~7라운드를 무작위로 구성한다(기획 문서 "한 판에 랜덤 5~7라운드만 진행한다").
 *
 * 상황극 선택은 관리자 문구 수([tips.size])만큼만 뽑는다 — 그 이상 뽑으면 문구가 바닥나
 * 반드시 같은 문구(와 같은 정답 그림)를 반복하게 된다(사용자 피드백: "같은 이미지가
 * 중복으로 나온다" — 실기 테스트 중이 아니라 [LibraryEtiquetteScreenTest]가 먼저 잡아낸
 * 회귀다). 상황극 선택을 다 쓰고 나면 나머지 라운드는 그림 풀이 훨씬 큰(30장) 다른 두
 * 게임 종류로만 채운다.
 *
 * 이번 판에서 쓸 상황극 문구(최대 [tips.size]개)를 라운드를 배정하기 전에 먼저 뽑아 두고,
 * 그 문구들의 정답 그림을 [usedImageRes]에 즉시 예약한다 — 안 그러면 좋아요?안돼요!·예절왕
 * 찾기가 먼저 같은 그림을 쓴 뒤, 나중에 상황극 선택이 (정답 그림은 문구에 고정돼 있어 다른
 * 그림으로 바꿔 뽑을 수 없으므로) 그 그림을 또 보여주는 순서 의존적인 중복이 생겼다(역시
 * [LibraryEtiquetteScreenTest]가 먼저 잡아낸 회귀다).
 *
 * [usedImageRes]에 이 판에서 이미 뽑은 그림을 전부 기록해 두고, 후보 그림을 고를 때마다 아직
 * 안 쓴 것만 우선한다(전부 썼으면 그제서야 다시 허용).
 *
 * `internal`로 열어 둔 이유: 이 함수의 중복 방지·id 매칭 로직이 이번 세션에 실기에서만 발견된
 * 버그(이미지 매칭 깨짐, 중복 노출)의 근원이었다 — [LibraryEtiquetteScreenTest]에서 JUnit으로
 * 직접 검증해 같은 종류의 회귀를 빌드 단계에서 잡는다.
 */
internal fun buildEtiquetteRounds(tips: List<LibraryEtiquetteTip>): List<EtiquetteRound> {
    val roundCount = (5..7).random()
    val situationChoiceLimit = minOf(tips.size, roundCount)
    val situationChoiceTips = ArrayDeque(tips.shuffled().take(situationChoiceLimit))
    val usedImageRes = mutableSetOf<Int>()
    situationChoiceTips.forEach { tip -> etiquetteTipImageRes(tip.id)?.let { usedImageRes += it } }

    fun <T> pickUnused(pool: List<T>, imageResOf: (T) -> Int): T {
        val candidates = pool.filter { imageResOf(it) !in usedImageRes }.ifEmpty { pool }
        val chosen = candidates.random()
        usedImageRes += imageResOf(chosen)
        return chosen
    }

    return (0 until roundCount).map {
        val availableTypes = if (situationChoiceTips.isEmpty()) {
            EtiquetteGameType.entries - EtiquetteGameType.SITUATION_CHOICE
        } else {
            EtiquetteGameType.entries
        }
        when (val type = availableTypes.random()) {
            EtiquetteGameType.SITUATION_CHOICE -> {
                val tip = situationChoiceTips.removeFirst()
                // 정답 그림은 함수 시작부에서 이미 usedImageRes에 예약해 뒀다.
                val decoyPool = ETIQUETTE_DECOYS.filter { it.text != tip.text }
                val firstDecoy = pickUnused(decoyPool) { it.imageRes }
                val secondDecoy = pickUnused(decoyPool.filterNot { it == firstDecoy }) { it.imageRes }
                EtiquetteRound(type = type, tip = tip, situationDecoys = listOf(firstDecoy, secondDecoy))
            }

            EtiquetteGameType.LIKE_OR_NOT -> {
                EtiquetteRound(type = type, likeOrNotTarget = pickUnused(ETIQUETTE_ALL_IMAGES) { it.imageRes })
            }

            EtiquetteGameType.FIND_THE_ODD_ONE -> {
                val target = pickUnused(ETIQUETTE_GOOD_IMAGES) { it.imageRes }
                val firstWrong = pickUnused(ETIQUETTE_BAD_IMAGES) { it.imageRes }
                val secondWrong = pickUnused(ETIQUETTE_BAD_IMAGES.filterNot { it == firstWrong }) { it.imageRes }
                EtiquetteRound(type = type, oddOneOptions = listOf(firstWrong, secondWrong, target).shuffled())
            }
        }
    }
}

/**
 * seed 예절 문구 4개([DefaultKidsContent.buildEtiquetteTips]의 고정 id)에만 매칭되는 실제
 * 삽화. 그 외(관리자가 새로 추가한 문구)는 null → 각 게임이 자리표시 아이콘으로 대신한다.
 *
 * 문구 텍스트가 아니라 id로 매칭한다 — 예전에는 문구 "텍스트"가 strings.xml 값과 정확히
 * 같을 때만 매칭했는데, 문구를 다듬어 텍스트가 바뀌면(이미 시드된 기기의 관리자 데이터는
 * 옛 텍스트 그대로 남아 있으므로) 매칭이 깨져 그림이 안 나왔다(사용자 피드백: "이미지
 * 매칭 안 된 것도 있다"). id는 문구를 고쳐도 바뀌지 않아 이 문제가 생기지 않는다.
 */
internal fun etiquetteTipImageRes(tipId: String): Int? = when (tipId) {
    "etiquette_1" -> R.drawable.etiquette_book_cart
    "etiquette_2" -> R.drawable.etiquette_quiet_voice
    "etiquette_3" -> R.drawable.etiquette_clean_hands
    "etiquette_4" -> R.drawable.etiquette_wait_turn
    else -> null
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
internal data class EtiquetteDecoy(val text: String, val imageRes: Int)

// 카드 캡션은 세 칸 격자 폭에 맞춰 짧게 다듬는다 — 원래 문구가 길어 "목소리로"/"켜두기" 같은
// 단어 중간에서 줄바꿈되어 어색해 보였다(사용자 피드백). 뜻은 그대로 두고 글자 수만 줄였다.
internal val ETIQUETTE_DECOYS = listOf(
    EtiquetteDecoy("책상에 발 올리기", R.drawable.decoy_feet_on_desk),
    EtiquetteDecoy("책 아무렇게나 던지기", R.drawable.decoy_throw_book),
    EtiquetteDecoy("복도에서 뛰어다니기", R.drawable.decoy_run_hallway),
    EtiquetteDecoy("큰 소리로 이야기하기", R.drawable.decoy_loud_talk),
    EtiquetteDecoy("다른 사람 자리 앉기", R.drawable.decoy_sit_others_seat),
    EtiquetteDecoy("책에 낙서하기", R.drawable.decoy_scribble_book),
    EtiquetteDecoy("벨소리 크게 켜두기", R.drawable.decoy_loud_phone_ring),
    EtiquetteDecoy("쓰레기 아무 데나 버리기", R.drawable.decoy_litter),
    EtiquetteDecoy("시끄럽게 전화하기", R.drawable.decoy_loud_call),
    EtiquetteDecoy("책 아무 곳에나 두기", R.drawable.decoy_leave_book_anywhere),
    // `docs/library_etiquette_assets` 30장 세트의 신규 오답 8개(사용자 요청으로 추가).
    EtiquetteDecoy("책 위에서 음식 먹기", R.drawable.decoy_new_01_eat_over_book),
    EtiquetteDecoy("책 모서리 접기", R.drawable.decoy_new_02_fold_page_corner),
    EtiquetteDecoy("책 페이지 찢기", R.drawable.decoy_new_03_tear_page),
    EtiquetteDecoy("가방으로 통로 막기", R.drawable.decoy_new_04_block_aisle),
    EtiquetteDecoy("책 서가 뒤에 숨기기", R.drawable.decoy_new_05_hide_book),
    EtiquetteDecoy("책장 밟고 올라가기", R.drawable.decoy_new_06_climb_shelf),
    EtiquetteDecoy("젖은 우산 가져오기", R.drawable.decoy_new_07_wet_umbrella),
    EtiquetteDecoy("책 옷 속에 숨기기", R.drawable.decoy_new_08_unborrowed_book)
)

private data class SituationOption(val text: String, val imageRes: Int?, val isCorrect: Boolean)

private enum class ChoiceState { NEUTRAL, CORRECT, WRONG_SELECTED, DISABLED }

@Composable
private fun SituationChoiceGame(tip: LibraryEtiquetteTip, decoys: List<EtiquetteDecoy>, onNext: () -> Unit) {
    val correctImageRes = etiquetteTipImageRes(tip.id)
    val options = remember(tip.id, decoys) {
        (decoys.map { SituationOption(it.text, it.imageRes, isCorrect = false) } +
            SituationOption(tip.text, correctImageRes, isCorrect = true)).shuffled()
    }
    var selected by remember(tip.id, decoys) { mutableStateOf<SituationOption?>(null) }

    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            text = stringResource(R.string.kids_etiquette_situation_question),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        // height(IntrinsicSize.Max) + 각 카드 fillMaxHeight()로 카드 3장의 높이를 항상
        // 맞춘다 — weight(1f)는 폭만 맞추고 높이는 각자 콘텐츠에 맡기므로, 캡션 줄 수가
        // 다르면 카드 키가 제각각이었다(사용자 피드백).
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Max),
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
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
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
        // weight(1f)로 남는 세로 공간을 이 캡션 영역이 흡수하게 한다 — 카드 3장의 문구
        // 길이가 서로 달라 줄 수가 다르면(한 줄/두 줄/세 줄) 카드 키가 제각각이었다(사용자
        // 피드백). 이미지 높이는 고정, 캡션 영역만 늘어나 항상 카드 3장 높이가 같아진다.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .heightIn(min = 76.dp)
                .background(Color.Transparent)
                .padding(12.dp),
            contentAlignment = Alignment.Center
        ) {
            // titleMedium은 48sp/lineHeight 58sp라 3칸 카드 폭에서 단어 중간에 줄바꿈되고
            // (사용자 피드백) fontSize만 줄이면 48sp용 줄 간격이 그대로 남아 두 줄일 때 위아래
            // 글자가 붕 떠 보였다 — 크기와 줄 간격을 함께 줄이고 살짝 키워 또렷하게 보이게 한다.
            Text(
                text = option.text,
                style = MaterialTheme.typography.titleMedium.copy(fontSize = 28.sp, lineHeight = 34.sp),
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

// ── 2) 좋아요? 안 돼요! / 3) 예절왕·범인을 찾아라 공용 이미지 목록 ────────────────────

/** [ETIQUETTE_DECOYS]는 "오답 보기 문구"만 있던 목록이라, 올바른 행동 쪽도 같은 모양
 * ([EtiquetteImage])으로 맞춰 둔 목록. seed 예절 문구 4개와 같은 그림([etiquetteTipImageRes]
 * 매칭 대상)이지만, 이 화면 예시 전용 문구라 관리자 문구와는 별개다([ETIQUETTE_DECOYS]와 같은
 * 이유). */
internal data class EtiquetteImage(val text: String, val imageRes: Int, val isGood: Boolean)

internal val ETIQUETTE_GOOD_IMAGES = listOf(
    EtiquetteImage("읽은 책 수레에 놓기", R.drawable.etiquette_book_cart, isGood = true),
    EtiquetteImage("작은 목소리로 말해요", R.drawable.etiquette_quiet_voice, isGood = true),
    // 캡션은 `docs/LIBRARY_ETIQUETTE_IMAGE_SITUATIONS.md`(실제 그림 내용 기준 설명)에 맞춘다.
    EtiquetteImage("깨끗한 손으로 책 읽기", R.drawable.etiquette_clean_hands, isGood = true),
    EtiquetteImage("차례를 기다려요", R.drawable.etiquette_wait_turn, isGood = true),
    // `docs/library_etiquette_assets` 30장 세트의 신규 올바른 행동 8개(사용자 요청으로 추가).
    EtiquetteImage("책갈피 사용하기", R.drawable.etiquette_new_01_bookmark, isGood = true),
    EtiquetteImage("책장 조심히 넘기기", R.drawable.etiquette_new_02_turn_page_gently, isGood = true),
    EtiquetteImage("책 두 팔로 들고 걷기", R.drawable.etiquette_new_03_carry_books_safely, isGood = true),
    EtiquetteImage("가방 의자 아래 정리", R.drawable.etiquette_new_04_store_backpack, isGood = true),
    EtiquetteImage("의자 밀어 넣기", R.drawable.etiquette_new_05_push_chair_in, isGood = true),
    EtiquetteImage("휴대전화 진동으로 하기", R.drawable.etiquette_new_06_vibrate_phone, isGood = true),
    EtiquetteImage("손상된 책 알려주기", R.drawable.etiquette_new_07_report_damaged_book, isGood = true),
    EtiquetteImage("음식은 가방에 넣기", R.drawable.etiquette_new_08_keep_food_away, isGood = true)
)

internal val ETIQUETTE_BAD_IMAGES = ETIQUETTE_DECOYS.map { EtiquetteImage(it.text, it.imageRes, isGood = false) }
internal val ETIQUETTE_ALL_IMAGES = ETIQUETTE_GOOD_IMAGES + ETIQUETTE_BAD_IMAGES

// ── 2) 좋아요? 안 돼요! ──────────────────────────────────────────────────────

@Composable
private fun LikeOrNotGame(target: EtiquetteImage, onNext: () -> Unit) {
    // 사용자가 누른 값(true=좋아요). 아직 안 눌렀으면 null.
    var picked by remember(target) { mutableStateOf<Boolean?>(null) }

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
        // 이미지+버튼을 조금 줄여 "정답이에요! 다음" 줄까지 스크롤 없이 한 화면에 들어오게
        // 한다(사용자 요청).
        EtiquetteIllustration(
            imageRes = target.imageRes,
            modifier = Modifier
                .width(520.dp)
                .height(350.dp)
        )
        // 세 장 카드 게임들은 그림 밑에 행동을 설명하는 캡션이 있는데 이 게임만 그림만 덩그러니
        // 있었다(사용자 피드백) — 같은 문구([EtiquetteImage.text])를 캡션으로 보여준다. 정답/
        // 오답 여부는 캡션이 아니라 선택 후 색으로만 드러나므로 미리 답을 알려주지 않는다.
        Text(
            text = target.text,
            style = MaterialTheme.typography.titleMedium.copy(fontSize = 28.sp, lineHeight = 34.sp),
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
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
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text = text, style = MaterialTheme.typography.titleMedium, color = Color.White)
    }
}

// ── 3) 예절왕·범인을 찾아라 ───────────────────────────────────────────────────

@Composable
private fun FindTheOddOneGame(options: List<EtiquetteImage>, onNext: () -> Unit) {
    // "예절왕(좋은 행동 1개)을 찾기"만 진행한다 — "범인(나쁜 행동 1개)을 찾기" 문구는 사용자
    // 요청으로 제외했다.
    var selected by remember(options) { mutableStateOf<EtiquetteImage?>(null) }

    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            text = stringResource(R.string.kids_etiquette_odd_one_good_prompt),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Max),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
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
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                )
            }
        }
        val current = selected
        if (current != null) {
            FeedbackWithNextRow(isCorrect = current.isGood, onNext = onNext)
        }
    }
}

