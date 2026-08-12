package com.example.ibtech.ui.facility

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.ibtech.R
import com.example.ibtech.domain.model.Facility
import com.example.ibtech.domain.model.WayfindingCorridorOverride
import com.example.ibtech.robot.NavigationIssue
import com.example.ibtech.robot.NavigationState
import com.example.ibtech.ui.common.ConfirmDialog
import com.example.ibtech.ui.common.DecorativeBackground
import com.example.ibtech.ui.common.EmptyState
import com.example.ibtech.ui.common.LibraryOutlinedButton
import com.example.ibtech.ui.common.LibraryPrimaryButton
import com.example.ibtech.ui.theme.CoralAccent
import com.example.ibtech.ui.theme.CoralAccentContainer
import com.example.ibtech.ui.theme.LibraryDimens
import com.example.ibtech.ui.theme.SkyAccent
import com.example.ibtech.ui.theme.SkyAccentContainer
import com.example.ibtech.ui.theme.SuccessAccent
import com.example.ibtech.ui.theme.SuccessAccentContainer
import com.example.ibtech.ui.theme.YellowAccent
import com.example.ibtech.ui.theme.YellowAccentContainer
import kotlinx.coroutines.delay

/**
 * 동행 이동 진행 화면 (요구사항 명세서 2.6절, 로드맵 8장 상태 머신).
 *
 * `NavigationState`를 그대로 받아 상태별로 다르게 그린다. 이동 중 뒤로/홈 확인창은 이 화면이
 * 아니라 [com.example.ibtech.navigation.LibraryNavHost]가 처리한다 — 상단바 버튼은 이 화면
 * 바깥(`LibraryScaffold`)에 있기 때문이다.
 */
@Composable
fun NavigationProgressScreen(
    uiState: NavigationUiState,
    idleTimeoutSeconds: Int,
    interactionTick: Int,
    onConfirmStart: () -> Unit,
    onCancelStart: () -> Unit,
    onStop: () -> Unit,
    onRetry: () -> Unit,
    onLocationOnlyClick: () -> Unit,
    onFindAnotherFacility: () -> Unit,
    onGoHome: () -> Unit,
    modifier: Modifier = Modifier
) {
    val facility = uiState.facility

    when {
        !uiState.isLoaded -> Unit

        facility == null -> EmptyState(
            message = stringResource(R.string.facility_detail_invalid),
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
                    text = facility.name,
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )

                if (uiState.hasStarted) {
                    when (val navState = uiState.navigationState) {
                        is NavigationState.Requested, is NavigationState.Moving ->
                            MovingContent(onStop = onStop)

                        is NavigationState.Interrupted ->
                            if (navState.issue == NavigationIssue.USER_STOPPED) {
                                StoppedByUserContent(onGoHome = onGoHome, onRetry = onRetry)
                            } else {
                                FailureContent(
                                    message = stringResource(navState.issue.toMessageRes()),
                                    onRetry = onRetry,
                                    onLocationOnlyClick = onLocationOnlyClick,
                                    onGoHome = onGoHome
                                )
                            }

                        is NavigationState.Failed -> FailureContent(
                            message = stringResource(navState.issue.toMessageRes()),
                            onRetry = onRetry,
                            onLocationOnlyClick = onLocationOnlyClick,
                            onGoHome = onGoHome
                        )

                        is NavigationState.Arrived -> ArrivedContent(
                            facility = facility,
                            baseFloor = uiState.baseFloor,
                            idleTimeoutSeconds = idleTimeoutSeconds,
                            interactionTick = interactionTick,
                            onFindAnotherFacility = onFindAnotherFacility,
                            onGoHome = onGoHome,
                            // 안내도 이미지가 있는 시설만 남는 세로 공간을 이미지에 준다 — 나머지는
                            // 기존처럼 내용 크기만큼만 차지한다.
                            modifier = if (WayfindingCorridorOverride.wayfindingImageAssetPath(facility.id) != null) {
                                Modifier.weight(1f)
                            } else {
                                Modifier
                            }
                        )

                        NavigationState.Idle -> Unit
                    }
                }
            }

            if (!uiState.hasStarted) {
                ConfirmDialog(
                    title = stringResource(R.string.navigation_confirm_title),
                    body = stringResource(R.string.navigation_confirm_body, facility.name),
                    confirmLabel = stringResource(R.string.navigation_confirm_action),
                    dismissLabel = stringResource(R.string.facility_detail_escort_confirm_cancel),
                    onConfirm = onConfirmStart,
                    onDismiss = onCancelStart
                )
            }
        }
    }
}

// 이동 중=Sky, 완료=Success, 오류=Coral, 중단=Yellow(01_DESIGN_SYSTEM.md 3절 "이동 완료·정답"
// 색과는 별개로, 최종 단계 지시에 따라 "중단"은 Yellow를 쓴다). 상태 머신(when 분기, LaunchedEffect
// 지연, toMessageRes 매핑)은 그대로 두고 이 4개 내용 컴포저블의 색·표면만 바꾼다.

@Composable
private fun MovingContent(onStop: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(LibraryDimens.CardSpacing)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(SkyAccentContainer, RoundedCornerShape(20.dp))
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 실제 진행률 데이터가 없으므로 임의의 퍼센트를 만들지 않고 불확정(indeterminate)
            // 스피너를 그대로 쓴다 — 색만 Sky로 맞춘다.
            CircularProgressIndicator(modifier = Modifier.size(36.dp), color = SkyAccent)
            Text(
                text = stringResource(R.string.navigation_moving_label),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        LibraryOutlinedButton(
            text = stringResource(R.string.navigation_stop_action),
            onClick = onStop
        )
    }
}

@Composable
private fun StoppedByUserContent(onGoHome: () -> Unit, onRetry: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(LibraryDimens.CardSpacing)) {
        NavigationStatusCard(
            icon = Icons.Filled.PauseCircle,
            accent = YellowAccent,
            container = YellowAccentContainer,
            text = stringResource(R.string.navigation_stopped_by_user)
        )
        LibraryPrimaryButton(text = stringResource(R.string.navigation_retry_action), onClick = onRetry)
        LibraryOutlinedButton(text = stringResource(R.string.top_bar_home), onClick = onGoHome)
    }
}

@Composable
private fun FailureContent(
    message: String,
    onRetry: () -> Unit,
    onLocationOnlyClick: () -> Unit,
    onGoHome: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(LibraryDimens.CardSpacing)) {
        NavigationStatusCard(
            icon = Icons.Filled.ErrorOutline,
            accent = CoralAccent,
            container = CoralAccentContainer,
            text = message
        )
        LibraryPrimaryButton(text = stringResource(R.string.navigation_retry_action), onClick = onRetry)
        LibraryOutlinedButton(
            text = stringResource(R.string.facility_detail_location_action),
            onClick = onLocationOnlyClick
        )
        LibraryOutlinedButton(text = stringResource(R.string.top_bar_home), onClick = onGoHome)
    }
}

@Composable
private fun ArrivedContent(
    facility: Facility,
    baseFloor: Int,
    idleTimeoutSeconds: Int,
    interactionTick: Int,
    onFindAnotherFacility: () -> Unit,
    onGoHome: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var secondsLeft by remember(idleTimeoutSeconds) { mutableIntStateOf(idleTimeoutSeconds) }
    val wayfindingImageAsset = WayfindingCorridorOverride.wayfindingImageAssetPath(facility.id)

    if (wayfindingImageAsset != null) {
        // 안내도 이미지가 화면의 대부분을 차지해야 하므로(요청: "안내도가 가장 크게 보일수
        // 있게"), 일반 도착 화면보다 간격을 좁히고 "홈" 버튼은 뺀다 — 60초 뒤 자동 홈 복귀가
        // 이미 있어서 버튼이 없어도 빠져나갈 방법은 남아있다. 상태 카드는 일반 도착 화면과
        // 같은 [NavigationStatusCard]를 그대로 쓴다 — 축약 헤더보다 이 카드 스타일이 낫다는
        // 피드백에 따라 되돌렸다.
        Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            NavigationStatusCard(
                icon = Icons.Filled.CheckCircle,
                accent = SuccessAccent,
                container = SuccessAccentContainer,
                text = buildNavigationArrivedText(context, facility, baseFloor, R.string.navigation_arrived_body)
            )
            val bitmap = rememberWayfindingImageBitmap(facility.id)
            if (bitmap != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(24.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        bitmap = bitmap,
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = stringResource(R.string.navigation_arrived_countdown, secondsLeft),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                LibraryPrimaryButton(
                    text = stringResource(R.string.navigation_arrived_continue_action),
                    onClick = onFindAnotherFacility,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    } else {
        Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(LibraryDimens.CardSpacing)) {
            NavigationStatusCard(
                icon = Icons.Filled.CheckCircle,
                accent = SuccessAccent,
                container = SuccessAccentContainer,
                text = buildNavigationArrivedText(context, facility, baseFloor, R.string.navigation_arrived_body)
            )
            Text(
                text = stringResource(R.string.navigation_arrived_countdown, secondsLeft),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            LibraryPrimaryButton(
                text = stringResource(R.string.navigation_arrived_continue_action),
                onClick = onFindAnotherFacility
            )
            LibraryOutlinedButton(text = stringResource(R.string.top_bar_home), onClick = onGoHome)
        }
    }

    // 실제 홈 복귀(화면 전환 + "홈" POI로 로봇 이동)는 LibraryNavHost의 IdleTimeoutObserver가
    // 관리자 설정 무입력시간을 기준으로 단독 수행한다 — 여기서는 같은 값(idleTimeoutSeconds)과
    // 같은 리셋 신호(interactionTick, 화면 아무 곳이나 터치하면 증가)로 카운트다운 "표시"만
    // 맞춰서 보여준다. 그래야 화면에 보이는 초와 실제 복귀 시점이 어긋나지 않는다.
    LaunchedEffect(idleTimeoutSeconds, interactionTick) {
        for (remaining in idleTimeoutSeconds downTo 0) {
            secondsLeft = remaining
            delay(1_000L)
        }
    }
}

@Composable
private fun NavigationStatusCard(icon: ImageVector, accent: Color, container: Color, text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(container, RoundedCornerShape(20.dp))
            .padding(20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .background(accent, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.surface, modifier = Modifier.size(30.dp))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
    }
}

// internal: 유닛 테스트(NavigationIssueMappingTest)가 문자열 리소스 매핑만 따로 검증한다.
internal fun NavigationIssue.toMessageRes(): Int = when (this) {
    NavigationIssue.USER_STOPPED -> R.string.navigation_stopped_by_user
    NavigationIssue.EXTERNAL_INTERRUPTION -> R.string.navigation_issue_external
    NavigationIssue.TIMEOUT -> R.string.navigation_issue_timeout
    NavigationIssue.SDK_ERROR -> R.string.navigation_issue_sdk_error
}
