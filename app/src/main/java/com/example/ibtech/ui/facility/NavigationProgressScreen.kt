package com.example.ibtech.ui.facility

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.ibtech.R
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
    onConfirmStart: () -> Unit,
    onCancelStart: () -> Unit,
    onStop: () -> Unit,
    onRetry: () -> Unit,
    onLocationOnlyClick: () -> Unit,
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

                        is NavigationState.Arrived -> ArrivedContent(onGoHome = onGoHome)

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
private fun ArrivedContent(onGoHome: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(LibraryDimens.CardSpacing)) {
        NavigationStatusCard(
            icon = Icons.Filled.CheckCircle,
            accent = SuccessAccent,
            container = SuccessAccentContainer,
            text = stringResource(R.string.navigation_arrived_body)
        )
        LibraryPrimaryButton(text = stringResource(R.string.top_bar_home), onClick = onGoHome)
    }

    // 도착 후 일정 시간이 지나면 자동으로 홈으로 돌아간다(명세서 2.6절 "자동 3~5초 후 홈 복귀").
    // 지연 시간·트리거 조건은 그대로다 — 디자인 변경과 무관한 로직이라 손대지 않았다.
    LaunchedEffect(Unit) {
        delay(ARRIVED_AUTO_HOME_DELAY_MILLIS)
        onGoHome()
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

private const val ARRIVED_AUTO_HOME_DELAY_MILLIS = 4_000L
