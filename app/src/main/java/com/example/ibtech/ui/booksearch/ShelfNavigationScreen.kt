package com.example.ibtech.ui.booksearch

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.ibtech.R
import com.example.ibtech.domain.model.BookDetail
import com.example.ibtech.robot.NavigationState
import com.example.ibtech.ui.common.LibraryCard
import com.example.ibtech.ui.common.LibraryOutlinedButton
import com.example.ibtech.ui.common.LibraryPrimaryButton
import com.example.ibtech.ui.common.LibraryScaffold
import com.example.ibtech.ui.common.RobotSpeechBubble
import com.example.ibtech.ui.theme.LibraryDimens

/**
 * 서가 안내 화면 (책 찾기 > 결과 선택).
 *
 * 로봇이 갈 수 있는 서가면 동행하고, 다른 층이면 엘리베이터까지만 데려다준 뒤 나머지는
 * 말로 안내한다. 판단은 [ShelfNavigationViewModel]이 하고 이 화면은 그리기만 한다.
 */
@Composable
fun ShelfNavigationScreen(
    onBack: () -> Unit,
    onHome: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ShelfNavigationViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LibraryScaffold(
        title = stringResource(R.string.shelf_navigation_title),
        onBack = onBack,
        onHome = onHome,
        modifier = modifier
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(LibraryDimens.ScreenPadding)
                // 디버그 대체 안내 배너 + 도착 후 버튼 두 개까지 합쳐지면 화면 높이를
                // 넘을 수 있다 — 스크롤이 없으면 그 아래 버튼이 아예 눌리지 않는다(실기 확인).
                .verticalScroll(rememberScrollState()),
            // 요청: "한화면에 버튼이 다 나오는지 보고 싶어" — 24dp는 도착 후 버튼 줄까지
            // 합치면 화면 아래로 넘친다. 16dp로 줄여서 최대한 스크롤 없이 다 보이게 한다.
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            BookSummaryCard(
                title = state.bookTitle,
                callNo = state.callNo,
                location = state.locationText(),
                detail = state.detail
            )

            val guideText = state.guideText()
            RobotSpeechBubble(text = guideText)
            // 말풍선(화면 표시)과 별개로 실제 음성도 나오게 한다(요청: "도서 선택할 때
            // 안내할 때 tts로 소리도 나오게") — 중복 재생 방지는 뷰모델의 hasSpokenGuide가 맡는다.
            //
            // isGuideResolved 를 기다리는 이유: 기준층 설정이 도착하기 전의 guideText 는
            // "서가 위치를 확인하지 못했어요"(LOCATION_ONLY)다. 화면은 곧 올바른 문구로
            // 바뀌지만 음성은 한 번만 나가므로, 기다리지 않으면 틀린 안내를 말하게 된다.
            if (state.isGuideResolved) {
                LaunchedEffect(guideText) { viewModel.speakGuide(guideText) }
            }
            // 도착 안내(제가 데려다 드릴게요 이후 "도착했어요" 멘트)는 ShelfNavigationViewModel이
            // navigationState를 직접 구독해서 말한다 — NavigationViewModel과 같은 방식.

            // 디버그 빌드에서 서가 POI 가 지도에 없어 다른 POI 로 대신 이동한 경우.
            // 실제 안내와 혼동하지 않도록 원래는 반드시 화면에 남긴다 — 사용자 요청으로
            // 지금 빌드만 화면 표시를 끈다(대체 이동 자체는 그대로 동작한다). 서가 POI가
            // 실제 지도에 등록되면 이 조건이 항상 false라 자연히 안 뜨게 되므로, 그때는
            // 이 주석과 아래 if(false)를 지우고 원래대로 되돌리면 된다.
            if (false) {
                state.substitutePoi?.let { poi ->
                    Text(
                        text = stringResource(
                            R.string.shelf_navigation_substitute_poi,
                            state.shelfLabel,
                            poi
                        ),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            when {
                !state.canEscort -> Unit

                state.hasStarted -> EscortProgress(
                    navigationState = state.navigationState,
                    onStop = viewModel::stopEscort,
                    onSearchAnotherBook = onBack,
                    onGoHome = onHome
                )

                // 요청: "버튼 가로크기를 절반으로 나누고 나머지 반 공간에 다른책찾기버튼
                // 추가" — 이동 버튼 옆에 다른 책을 바로 찾을 수 있는 버튼을 반씩 나눠 둔다.
                // "다른 책 찾기"는 새 화면을 만들지 않고 뒤로가기와 같다 — 이 화면을 부른
                // 검색 결과 화면이 그대로 남아 있어 바로 다시 고를 수 있다.
                else -> Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    LibraryPrimaryButton(
                        text = stringResource(
                            if (state.guideMode == ShelfGuideMode.ELEVATOR) {
                                R.string.shelf_navigation_go_elevator
                            } else {
                                R.string.shelf_navigation_go_shelf
                            }
                        ),
                        onClick = viewModel::startEscort,
                        icon = Icons.Filled.DirectionsWalk,
                        modifier = Modifier.weight(1f)
                    )
                    LibraryOutlinedButton(
                        text = stringResource(R.string.shelf_navigation_search_another_book),
                        onClick = onBack,
                        icon = Icons.Filled.Search,
                        // Outlined 버튼은 기본이 Primary보다 낮다(88dp vs 100dp) — 같은 줄에서
                        // 나란히 쓰니 높이를 맞춘다(요청: "2개버튼 크기가 달라보여 크기 통일").
                        height = LibraryDimens.PrimaryButtonHeight,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

/**
 * 책 정보 카드. 표지는 서버가 외부 서점에서 찾아 준 것이라 없을 수 있고,
 * 그때는 표지 자리를 아예 비운다 — 엉뚱한 그림을 넣느니 없는 편이 낫다.
 */
@Composable
private fun BookSummaryCard(
    title: String,
    callNo: String,
    location: String,
    detail: BookDetail?
) {
    LibraryCard(
        modifier = Modifier.fillMaxWidth(),
        accentColor = MaterialTheme.colorScheme.primary
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(LibraryDimens.CardPadding),
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            val cover = detail?.thumbnail.orEmpty()
            if (cover.isNotBlank()) {
                AsyncImage(
                    model = cover,
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .width(COVER_WIDTH)
                        .height(COVER_HEIGHT)
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = title, style = MaterialTheme.typography.headlineSmall)

                detail?.byline()?.let { byline ->
                    Text(
                        text = byline,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Text(
                    text = stringResource(R.string.shelf_navigation_call_no, callNo),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = location,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

/** "이주훈 글 · 삼성당 · 2006" 처럼 한 줄로. 값이 없는 항목은 빼고 잇는다. */
@Composable
private fun BookDetail.byline(): String? {
    val parts = buildList {
        if (authors.isNotEmpty()) add(authors.joinToString(", "))
        if (translators.isNotEmpty()) {
            add(stringResource(R.string.shelf_navigation_translator, translators.joinToString(", ")))
        }
        if (publisher.isNotBlank()) add(publisher)
        if (publishedYear.isNotBlank()) add(publishedYear)
    }
    return parts.takeIf { it.isNotEmpty() }?.joinToString(" · ")
}

@Composable
private fun EscortProgress(
    navigationState: NavigationState,
    onStop: () -> Unit,
    onSearchAnotherBook: () -> Unit,
    onGoHome: () -> Unit
) {
    val statusRes = when (navigationState) {
        is NavigationState.Requested -> R.string.shelf_navigation_status_requested
        is NavigationState.Moving -> R.string.shelf_navigation_status_moving
        is NavigationState.Arrived -> R.string.shelf_navigation_status_arrived
        is NavigationState.Interrupted -> R.string.shelf_navigation_status_interrupted
        is NavigationState.Failed -> R.string.shelf_navigation_status_failed
        NavigationState.Idle -> R.string.shelf_navigation_status_requested
    }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = stringResource(statusRes),
            style = MaterialTheme.typography.titleLarge
        )
        if (navigationState.isBusy) {
            LibraryOutlinedButton(
                text = stringResource(R.string.shelf_navigation_stop),
                onClick = onStop,
                icon = Icons.Filled.Stop
            )
        }
        // 요청: "서가 이동버튼 누르고 도착해서도 버튼으로 다른책찾기, 홈으로이동 버튼
        // 추가해줘" — 도착한 뒤에는 다음 행동(다른 책을 더 찾을지, 끝내고 홈으로 갈지)을
        // 바로 고를 수 있게 한다.
        if (navigationState is NavigationState.Arrived) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                LibraryOutlinedButton(
                    text = stringResource(R.string.shelf_navigation_search_another_book),
                    onClick = onSearchAnotherBook,
                    icon = Icons.Filled.Search,
                    height = LibraryDimens.PrimaryButtonHeight,
                    modifier = Modifier.weight(1f)
                )
                LibraryPrimaryButton(
                    text = stringResource(R.string.shelf_navigation_go_home),
                    onClick = onGoHome,
                    icon = Icons.Filled.Home,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

private val COVER_WIDTH = 210.dp
private val COVER_HEIGHT = 300.dp

/** "1층 어린이자료실 · 아동4~1" 처럼 한 줄로. 층을 모르면 층을 뺀다. */
@Composable
private fun ShelfNavigationUiState.locationText(): String {
    val floorText = shelfFloor?.let { stringResource(R.string.book_search_floor_format, it) }
    return listOfNotNull(
        floorText,
        room.takeIf { it.isNotBlank() },
        shelfLabel.takeIf { it.isNotBlank() }
    ).joinToString(" · ")
        .ifBlank { stringResource(R.string.book_search_shelf_unknown) }
}

/**
 * 로봇이 말할 안내 문구.
 *
 * 추정 배정된 서가([ShelfNavigationUiState.isEstimated])는 "근처"로 낮춰 말한다 — 매핑표에
 * 구간이 없어 앞 서가로 추정한 위치라 정확하지 않을 수 있다.
 */
@Composable
private fun ShelfNavigationUiState.guideText(): String = when (guideMode) {
    ShelfGuideMode.ESCORT -> if (isEstimated) {
        stringResource(R.string.shelf_navigation_guide_escort_estimated, shelfLabel)
    } else {
        stringResource(R.string.shelf_navigation_guide_escort, shelfLabel)
    }

    ShelfGuideMode.ELEVATOR -> stringResource(
        R.string.shelf_navigation_guide_elevator,
        shelfFloor ?: baseFloor,
        room.ifBlank { shelfLabel }
    )

    ShelfGuideMode.LOCATION_ONLY -> stringResource(R.string.shelf_navigation_guide_unknown)
}
