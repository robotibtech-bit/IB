package com.example.ibtech.ui.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.unit.dp
import com.example.ibtech.R
import com.example.ibtech.domain.model.Facility
import com.example.ibtech.domain.model.FacilitySyncStatus
import com.example.ibtech.domain.model.GuideMode
import com.example.ibtech.ui.common.AdminListRow
import com.example.ibtech.ui.common.ConfirmDialog
import com.example.ibtech.ui.common.DecorativeBackground
import com.example.ibtech.ui.common.EmptyState
import com.example.ibtech.ui.common.LibraryPrimaryButton
import com.example.ibtech.ui.theme.ErrorRed
import com.example.ibtech.ui.theme.LibraryDimens

/** 시설 관리 목록 화면 (로드맵 10단계): POI 새로고침, 동기화 상태 배지, 편집/삭제 진입점. */
@Composable
fun FacilityAdminScreen(
    uiState: FacilityAdminUiState,
    onRefresh: () -> Unit,
    onSelectFacility: (Facility) -> Unit,
    onDeleteFacility: (Facility) -> Unit,
    modifier: Modifier = Modifier
) {
    var pendingDelete by remember { mutableStateOf<Facility?>(null) }

    Box(modifier = modifier.fillMaxSize()) {
        DecorativeBackground(modifier = Modifier.fillMaxSize(), showBookAndPlantDecoration = false)

        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = LibraryDimens.ScreenPadding, vertical = 12.dp),
                horizontalArrangement = Arrangement.End
            ) {
                // 관리자가 자주 눌러야 하는 핵심 동작이라 눈에 띄게 아웃라인 대신 채워진
                // 버튼으로 바꿨다(사용자 요청) — 새 색을 추가하지 않고 기존 강조색(민트,
                // LibraryPrimaryButton)을 그대로 재사용한다.
                LibraryPrimaryButton(
                    text = stringResource(R.string.facility_admin_refresh),
                    icon = Icons.Filled.Refresh,
                    onClick = onRefresh,
                    modifier = Modifier.fillMaxWidth(0.4f)
                )
            }

            when {
                !uiState.isLoaded -> Unit

                uiState.facilities.isEmpty() -> EmptyState(
                    message = stringResource(R.string.facility_admin_empty),
                    modifier = Modifier.fillMaxSize()
                )

                else -> LazyColumn(
                    contentPadding = PaddingValues(LibraryDimens.ScreenPadding),
                    verticalArrangement = Arrangement.spacedBy(LibraryDimens.CardSpacing),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(uiState.facilities, key = { it.id }) { facility ->
                        AdminListRow(
                            title = facility.name,
                            subtitle = facilitySubtitle(facility),
                            trailingBadge = facilitySyncBadge(facility.syncStatus),
                            onEdit = { onSelectFacility(facility) },
                            onDelete = if (facility.syncStatus == FacilitySyncStatus.NOT_FOUND_ON_TEMI) {
                                { pendingDelete = facility }
                            } else {
                                null
                            }
                        )
                    }
                }
            }
        }

        val target = pendingDelete
        if (target != null) {
            ConfirmDialog(
                title = stringResource(R.string.facility_admin_delete_confirm_title),
                body = stringResource(R.string.facility_admin_delete_confirm_body, target.name),
                confirmLabel = stringResource(R.string.facility_admin_delete_confirm_action),
                dismissLabel = stringResource(R.string.facility_detail_escort_confirm_cancel),
                onConfirm = {
                    onDeleteFacility(target)
                    pendingDelete = null
                },
                onDismiss = { pendingDelete = null }
            )
        }
    }
}

@Composable
private fun facilitySubtitle(facility: Facility): AnnotatedString {
    val isUnset = facility.floor == Facility.UNSET_FLOOR
    val floorText = if (isUnset) {
        stringResource(R.string.facility_admin_unset_floor)
    } else {
        stringResource(R.string.facility_card_floor_format, facility.floor)
    }
    val guideModeText = when (facility.guideMode) {
        GuideMode.ESCORT -> stringResource(R.string.facility_detail_escort_action)
        GuideMode.LOCATION_ONLY -> stringResource(R.string.facility_detail_location_action)
        GuideMode.BOTH -> stringResource(R.string.facility_admin_guide_both)
    }
    val fullText = stringResource(R.string.facility_admin_row_subtitle, floorText, guideModeText)
    if (!isUnset) return AnnotatedString(fullText)

    // "미설정"만 빨간색으로 강조한다(사용자 요청) — 부제 전체가 "%1$s · %2$s" 형식 리소스로
    // 조립되므로, floorText가 그 안에서 시작하는 위치를 찾아 그 구간에만 색을 입힌다(구분자를
    // 코드에 하드코딩하지 않기 위해).
    return buildAnnotatedString {
        append(fullText)
        val start = fullText.indexOf(floorText)
        if (start >= 0) {
            addStyle(SpanStyle(color = ErrorRed), start, start + floorText.length)
        }
    }
}

@Composable
private fun facilitySyncBadge(status: FacilitySyncStatus): String? = when (status) {
    FacilitySyncStatus.NEW -> stringResource(R.string.facility_admin_badge_new)
    FacilitySyncStatus.NOT_FOUND_ON_TEMI -> stringResource(R.string.facility_admin_badge_not_found)
    FacilitySyncStatus.SYNCED -> null
}
