package com.example.ibtech.ui.main

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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.ibtech.R
import com.example.ibtech.data.temi.BatteryStatus
import com.example.ibtech.data.temi.NavigationState
import com.example.ibtech.data.temi.TemiConnectionState
import com.example.ibtech.ui.theme.IBTECHTheme

/*
 * 화면 치수는 한곳에 모아 둔다.
 * 방문객이 로봇 앞에 서서 조작하므로, 터치 최소 크기(48dp)가 아니라
 * 한두 걸음 떨어져서도 읽고 누를 수 있는 크기를 기준으로 잡는다.
 */

/** 이동·발화 같은 주요 명령 버튼 높이. */
private val PrimaryButtonHeight = 88.dp

/** POI 이동·새로고침처럼 목록에 반복되는 버튼 높이. */
private val SecondaryButtonHeight = 72.dp

private val ButtonShape = RoundedCornerShape(20.dp)
private val CardShape = RoundedCornerShape(24.dp)
private val CardPadding = 24.dp
private val CardElevation = 2.dp
private val StatusDotSize = 14.dp
private val ProgressHeight = 8.dp
private val OutlineWidth = 2.dp

/** 이 값 이하로 떨어지면 배터리 수치를 경고색으로 보여준다. */
private const val LOW_BATTERY_PERCENT = 20

/**
 * 대시보드 화면.
 *
 * 상태는 [TemiUiState] 하나만 읽고, 버튼은 콜백만 올려보낸다.
 * 화면이 직접 로봇 상태를 확정하지 않는다.
 */
@Composable
fun MainDashboardScreen(
    uiState: TemiUiState,
    onGoTo: (String) -> Unit,
    onSpeakWelcome: () -> Unit,
    onStop: () -> Unit,
    onRefreshLocations: () -> Unit,
    onRequestPermissions: () -> Unit,
    modifier: Modifier = Modifier
) {
    val officeName = stringResource(R.string.location_office)
    val lobbyName = stringResource(R.string.location_lobby)

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text(
            text = stringResource(R.string.dashboard_title),
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold
        )

        RobotStatusCard(uiState = uiState)

        if (uiState.needsPermission) {
            PermissionCard(
                requesting = uiState.permissions.requestInFlight,
                onRequestPermissions = onRequestPermissions
            )
        }

        NavigationMessage(uiState = uiState)

        CommandButtons(
            uiState = uiState,
            officeName = officeName,
            lobbyName = lobbyName,
            onGoTo = onGoTo,
            onSpeakWelcome = onSpeakWelcome,
            onStop = onStop
        )

        PoiSection(
            uiState = uiState,
            onGoTo = onGoTo,
            onRefreshLocations = onRefreshLocations
        )
    }
}

/** 연결·배터리·목적지·현재 작업을 한눈에 보여주는 카드. */
@Composable
private fun RobotStatusCard(
    uiState: TemiUiState,
    modifier: Modifier = Modifier
) {
    val battery = uiState.battery

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = CardShape,
        elevation = CardDefaults.cardElevation(defaultElevation = CardElevation)
    ) {
        Column(
            modifier = Modifier.padding(CardPadding),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.dashboard_status_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                StatusBadge(activity = uiState.activity)
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            StatusRow(
                label = stringResource(R.string.label_connection),
                value = when (uiState.connection) {
                    TemiConnectionState.Ready -> stringResource(R.string.value_connected)
                    TemiConnectionState.Connecting -> stringResource(R.string.value_connecting)
                    TemiConnectionState.Unavailable -> stringResource(R.string.value_unavailable)
                }
            )

            StatusRow(
                label = stringResource(R.string.label_battery),
                value = battery?.let {
                    if (it.isCharging) {
                        stringResource(R.string.battery_charging_format, it.percentage)
                    } else {
                        stringResource(R.string.battery_format, it.percentage)
                    }
                } ?: stringResource(R.string.value_none),
                // 충전 중이 아닌 저전력만 경고색으로 띄운다. 충전 중이면 곧 회복된다.
                valueColor = if (battery != null &&
                    !battery.isCharging &&
                    battery.percentage <= LOW_BATTERY_PERCENT
                ) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )

            StatusRow(
                label = stringResource(R.string.label_destination),
                value = uiState.currentTarget ?: stringResource(R.string.value_none)
            )

            // 이동 중에는 진행 표시를 항상 띄운다.
            if (uiState.isMoving) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(ProgressHeight),
                    strokeCap = StrokeCap.Round
                )
            }
        }
    }
}

@Composable
private fun StatusRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = valueColor
        )
    }
}

/** 현재 작업을 색으로 구분하는 배지. 멀리서도 읽히도록 크게 잡는다. */
@Composable
private fun StatusBadge(
    activity: RobotActivity,
    modifier: Modifier = Modifier
) {
    val (labelRes, container) = when (activity) {
        RobotActivity.UNAVAILABLE ->
            R.string.status_unavailable to MaterialTheme.colorScheme.errorContainer

        RobotActivity.CONNECTING ->
            R.string.status_connecting to MaterialTheme.colorScheme.surfaceVariant

        RobotActivity.STANDBY ->
            R.string.status_standby to MaterialTheme.colorScheme.secondaryContainer

        RobotActivity.MOVING ->
            R.string.status_moving to MaterialTheme.colorScheme.primaryContainer

        RobotActivity.SPEAKING ->
            R.string.status_speaking to MaterialTheme.colorScheme.tertiaryContainer
    }
    val content = contentColorFor(container)

    Card(
        modifier = modifier,
        shape = CircleShape,
        colors = CardDefaults.cardColors(
            containerColor = container,
            contentColor = content
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 색만으로 구분하면 멀리서 흐릿하므로 점을 하나 더 얹어 형태로도 구분되게 한다.
            Box(
                modifier = Modifier
                    .size(StatusDotSize)
                    .background(color = content, shape = CircleShape)
            )
            Text(
                text = stringResource(labelRes),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/** 권한이 없을 때만 노출되는 안내 카드. */
@Composable
private fun PermissionCard(
    requesting: Boolean,
    onRequestPermissions: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = CardShape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(CardPadding),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(R.string.permission_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = stringResource(R.string.permission_description),
                style = MaterialTheme.typography.bodyLarge
            )
            PrimaryActionButton(
                text = if (requesting) {
                    stringResource(R.string.permission_requesting)
                } else {
                    stringResource(R.string.permission_action_allow)
                },
                onClick = onRequestPermissions,
                enabled = !requesting,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/** 이동/발화 결과 안내. 표시할 내용이 없으면 아무 것도 그리지 않는다. */
@Composable
private fun NavigationMessage(
    uiState: TemiUiState,
    modifier: Modifier = Modifier
) {
    val message = navigationMessage(uiState) ?: return

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = CardShape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(horizontal = CardPadding, vertical = 20.dp)
        )
    }
}

/**
 * 진행 중인 이동만 문구로 보여준다.
 *
 * 도착·중단·실패 같은 종료 결과는 [UiAlert] 스낵바가 담당한다.
 * ViewModel 이 결과를 안내한 뒤 상태를 즉시 Idle 로 되돌리므로, 여기서 그리면 한 프레임만
 * 깜빡이고 사라진다.
 */
@Composable
private fun navigationMessage(uiState: TemiUiState): String? =
    when (val navigation = uiState.navigation) {
        is NavigationState.Moving ->
            stringResource(R.string.nav_moving_format, navigation.target)

        is NavigationState.Requested ->
            stringResource(R.string.nav_moving_format, navigation.target)

        is NavigationState.Arrived,
        is NavigationState.Interrupted,
        is NavigationState.Failed,
        NavigationState.Idle -> null
    }

@Composable
private fun CommandButtons(
    uiState: TemiUiState,
    officeName: String,
    lobbyName: String,
    onGoTo: (String) -> Unit,
    onSpeakWelcome: () -> Unit,
    onStop: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            DestinationButton(
                text = stringResource(R.string.action_go_to_office),
                location = officeName,
                uiState = uiState,
                onGoTo = onGoTo,
                modifier = Modifier.weight(1f)
            )
            DestinationButton(
                text = stringResource(R.string.action_go_to_lobby),
                location = lobbyName,
                uiState = uiState,
                onGoTo = onGoTo,
                modifier = Modifier.weight(1f)
            )
        }

        PrimaryActionButton(
            text = stringResource(R.string.action_speak_welcome),
            onClick = onSpeakWelcome,
            enabled = uiState.isReady,
            modifier = Modifier.fillMaxWidth()
        )

        // 이동 중일 때만 중지 버튼을 연다.
        if (uiState.isMoving) {
            PrimaryActionButton(
                text = stringResource(R.string.action_stop),
                onClick = onStop,
                enabled = true,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/**
 * 화면의 주요 명령 버튼.
 * 크기·모서리·글자 굵기를 여기서만 정해 모든 명령 버튼이 같은 무게로 보이게 한다.
 */
@Composable
private fun PrimaryActionButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    colors: ButtonColors = ButtonDefaults.buttonColors()
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = ButtonShape,
        colors = colors,
        modifier = modifier.height(PrimaryButtonHeight)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
    }
}

/**
 * 목적지 이동 버튼.
 * 지도에 없는 POI 는 눌러도 실패하므로 미리 비활성화하고 이유를 보여준다.
 */
@Composable
private fun DestinationButton(
    text: String,
    location: String,
    uiState: TemiUiState,
    onGoTo: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val exists = uiState.hasLocation(location)

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        PrimaryActionButton(
            text = text,
            onClick = { onGoTo(location) },
            enabled = uiState.canNavigate && exists,
            modifier = Modifier.fillMaxWidth()
        )
        if (uiState.isReady && !exists) {
            Text(
                text = stringResource(R.string.poi_missing_format, location),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

/** temi 지도에서 읽어온 POI 목록. 탭하면 그 위치로 이동한다. */
@Composable
private fun PoiSection(
    uiState: TemiUiState,
    onGoTo: (String) -> Unit,
    onRefreshLocations: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = CardShape,
        elevation = CardDefaults.cardElevation(defaultElevation = CardElevation)
    ) {
        Column(
            modifier = Modifier.padding(CardPadding),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.poi_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = stringResource(R.string.poi_count_format, uiState.locations.size),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            if (uiState.locations.isEmpty()) {
                Text(
                    text = stringResource(R.string.poi_empty),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                // 목록이 길어질 수 있으나 바깥 Column 이 스크롤을 담당하므로
                // 중첩 스크롤을 만들지 않도록 LazyColumn 을 쓰지 않는다.
                uiState.locations.forEach { location ->
                    FilledTonalButton(
                        onClick = { onGoTo(location) },
                        enabled = uiState.canNavigate,
                        shape = ButtonShape,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(SecondaryButtonHeight)
                    ) {
                        Text(
                            text = location,
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                }
            }

            // 이동 버튼과 성격이 다르므로 목록과 같은 채움 버튼을 쓰지 않는다.
            OutlinedButton(
                onClick = onRefreshLocations,
                enabled = uiState.isReady,
                shape = ButtonShape,
                border = BorderStroke(
                    width = OutlineWidth,
                    color = if (uiState.isReady) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.outlineVariant
                    }
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(SecondaryButtonHeight)
            ) {
                Text(
                    text = stringResource(R.string.action_refresh_locations),
                    style = MaterialTheme.typography.titleLarge
                )
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 800, heightDp = 1200)
@Composable
private fun MainDashboardPreview() {
    IBTECHTheme {
        MainDashboardScreen(
            uiState = TemiUiState(
                connection = TemiConnectionState.Ready,
                navigation = NavigationState.Moving("로비", "going"),
                locations = listOf("로비", "사무실", "회의실"),
                battery = BatteryStatus(percentage = 62, isCharging = false)
            ),
            onGoTo = {},
            onSpeakWelcome = {},
            onStop = {},
            onRefreshLocations = {},
            onRequestPermissions = {}
        )
    }
}

/** 대기 상태 + 저전력 경고를 함께 확인하기 위한 프리뷰. */
@Preview(showBackground = true, widthDp = 800, heightDp = 1200)
@Composable
private fun MainDashboardStandbyPreview() {
    IBTECHTheme {
        MainDashboardScreen(
            uiState = TemiUiState(
                connection = TemiConnectionState.Ready,
                locations = listOf("로비", "사무실"),
                battery = BatteryStatus(percentage = 12, isCharging = false)
            ),
            onGoTo = {},
            onSpeakWelcome = {},
            onStop = {},
            onRefreshLocations = {},
            onRequestPermissions = {}
        )
    }
}
