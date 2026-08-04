package com.example.ibtech.ui.facility

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.ibtech.R
import com.example.ibtech.domain.model.EscortBlockReason
import com.example.ibtech.domain.model.EscortGate
import com.example.ibtech.domain.model.GuideOptionSet
import com.example.ibtech.ui.common.DecorativeBackground
import com.example.ibtech.ui.common.EmptyState
import com.example.ibtech.ui.common.LibraryOutlinedButton
import com.example.ibtech.ui.common.LibraryPrimaryButton
import com.example.ibtech.ui.common.RobotSpeechBubble
import com.example.ibtech.ui.theme.LibraryDimens

/**
 * 시설 상세/안내 방식 화면 (요구사항 명세서 2.4절).
 *
 * 버튼 구성은 [FacilityDetailUiState.guideOptions]가 전부 결정한다 — 이 화면은 층 비교나
 * guideMode 분기를 직접 하지 않는다([com.example.ibtech.domain.usecase.ResolveGuideOptionUseCase]
 * 참고).
 */
@Composable
fun FacilityDetailScreen(
    uiState: FacilityDetailUiState,
    onEscortClick: () -> Unit,
    onLocationOnlyClick: () -> Unit,
    onGoHome: () -> Unit,
    onRequestPermission: () -> Unit,
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

        else -> {
            Box(modifier = modifier.fillMaxSize()) {
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
                    if (facility.shortDescription.isNotBlank()) {
                        Text(
                            text = facility.shortDescription,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    RobotSpeechBubble(
                        text = stringResource(R.string.facility_detail_floor_line, facility.floor)
                    )

                    when (uiState.guideOptions) {
                        GuideOptionSet.EscortAndLocationOnly -> {
                            val gate = uiState.escortGate
                            LibraryPrimaryButton(
                                text = stringResource(R.string.facility_detail_escort_action),
                                icon = Icons.AutoMirrored.Filled.DirectionsWalk,
                                onClick = onEscortClick,
                                enabled = gate is EscortGate.Allowed
                            )
                            if (gate is EscortGate.Blocked) {
                                Text(
                                    text = stringResource(gate.reason.toMessageRes()),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.error
                                )
                                // 권한 부족은 이 화면에서 바로 재시도할 수 있는 유일한 사유라
                                // 별도 버튼을 둔다(나머지 사유는 SDK/이동 상태가 스스로 바뀌어야 함).
                                if (gate.reason == EscortBlockReason.PERMISSION) {
                                    LibraryOutlinedButton(
                                        text = stringResource(R.string.facility_detail_request_permission_action),
                                        onClick = onRequestPermission,
                                        enabled = !uiState.permissionRequestInFlight
                                    )
                                }
                            }
                            LibraryOutlinedButton(
                                text = stringResource(R.string.facility_detail_location_action),
                                icon = Icons.Filled.Place,
                                onClick = onLocationOnlyClick
                            )
                        }

                        GuideOptionSet.LocationOnlyWithDirections -> {
                            LibraryOutlinedButton(
                                text = stringResource(R.string.facility_detail_location_action),
                                icon = Icons.Filled.Place,
                                onClick = onLocationOnlyClick,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        // 목록 단계에서 걸러지므로 이 화면에는 정상적으로 도달하지 않는 방어용 분기.
                        GuideOptionSet.Hidden, GuideOptionSet.TemiEscortOnlyUnconfigured -> Unit
                    }
                }
            }
        }
    }
}

private fun EscortBlockReason.toMessageRes(): Int = when (this) {
    EscortBlockReason.SDK_NOT_READY -> R.string.facility_detail_escort_reason_sdk_not_ready
    EscortBlockReason.PERMISSION -> R.string.facility_detail_escort_reason_permission
    EscortBlockReason.POI_MISSING -> R.string.facility_detail_escort_reason_poi_missing
    EscortBlockReason.ALREADY_MOVING -> R.string.facility_detail_escort_reason_already_moving
    EscortBlockReason.DIFFERENT_FLOOR -> R.string.facility_detail_escort_reason_different_floor
}
