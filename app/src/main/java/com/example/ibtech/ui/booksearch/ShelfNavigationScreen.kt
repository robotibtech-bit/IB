package com.example.ibtech.ui.booksearch

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsWalk
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
                .padding(LibraryDimens.ScreenPadding),
            verticalArrangement = Arrangement.spacedBy(24.dp)
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

            // 디버그 빌드에서 서가 POI 가 지도에 없어 다른 POI 로 대신 이동한 경우.
            // 실제 안내와 혼동하지 않도록 반드시 화면에 남긴다.
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

            when {
                !state.canEscort -> Unit

                state.hasStarted -> EscortProgress(
                    navigationState = state.navigationState,
                    onStop = viewModel::stopEscort
                )

                else -> LibraryPrimaryButton(
                    text = stringResource(
                        if (state.guideMode == ShelfGuideMode.ELEVATOR) {
                            R.string.shelf_navigation_go_elevator
                        } else {
                            R.string.shelf_navigation_go_shelf
                        }
                    ),
                    onClick = viewModel::startEscort,
                    icon = Icons.Filled.DirectionsWalk
                )
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
                if (detail != null && detail.bookId.isNotBlank()) {
                    Text(
                        text = stringResource(R.string.shelf_navigation_reg_no, detail.bookId),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = location,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                if (detail != null && detail.holding.isNotBlank()) {
                    Text(
                        text = stringResource(R.string.shelf_navigation_holding, detail.holding),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
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
private fun EscortProgress(navigationState: NavigationState, onStop: () -> Unit) {
    val statusRes = when (navigationState) {
        is NavigationState.Requested -> R.string.shelf_navigation_status_requested
        is NavigationState.Moving -> R.string.shelf_navigation_status_moving
        is NavigationState.Arrived -> R.string.shelf_navigation_status_arrived
        is NavigationState.Interrupted -> R.string.shelf_navigation_status_interrupted
        is NavigationState.Failed -> R.string.shelf_navigation_status_failed
        NavigationState.Idle -> R.string.shelf_navigation_status_requested
    }
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
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
    }
}

private val COVER_WIDTH = 180.dp
private val COVER_HEIGHT = 260.dp

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
