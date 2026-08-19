package com.example.ibtech.ui.admin

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.ibtech.R
import com.example.ibtech.domain.model.FacilityDirection
import com.example.ibtech.domain.model.GuideMode
import com.example.ibtech.ui.common.AdminDropdownField
import com.example.ibtech.ui.common.AdminSwitchRow
import com.example.ibtech.ui.common.AdminTextField
import com.example.ibtech.ui.common.DecorativeBackground
import com.example.ibtech.ui.common.EmptyState
import com.example.ibtech.ui.common.LibraryPrimaryButton
import com.example.ibtech.ui.facility.formatFloorBadge
import com.example.ibtech.ui.theme.LibraryDimens

private val ICON_KEYS = listOf(null, "child_care", "computer", "library_books", "groups")
private val DIRECTION_KEYS = listOf(null, FacilityDirection.RIGHT, FacilityDirection.FRONT, FacilityDirection.LEFT)

// 자유 입력을 드롭다운으로 바꿨다(요청: "층설정을 드롭다운방식으로 바꾸면 편한거 같은데") —
// 건물에 실제로 있는 층만 골라 쓰게 해 오타를 막는다. null은 "미설정"(신규 POI 기본값)이다.
// 새 층이 생기면 이 목록에 추가해야 한다.
private val FLOOR_OPTIONS: List<Int?> = listOf(null, -1, 1, 2, 3, 4)

/** 시설 편집 화면 (로드맵 10단계): 표시명·층·설명·안내방식·아이콘·노출여부·순서. */
@Composable
fun FacilityAdminEditScreen(
    uiState: FacilityAdminEditUiState,
    onNameChange: (String) -> Unit,
    onFloorChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onGuideModeChange: (GuideMode) -> Unit,
    onDirectionChange: (FacilityDirection?) -> Unit,
    onNavigationTargetOverrideChange: (String?) -> Unit,
    onIconKeyChange: (String?) -> Unit,
    onEnabledChange: (Boolean) -> Unit,
    onFeaturedChange: (Boolean) -> Unit,
    onSortOrderChange: (String) -> Unit,
    onSave: () -> Unit,
    onSaved: () -> Unit,
    onGoHome: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val savedMessage = stringResource(R.string.admin_save_success)

    LaunchedEffect(uiState.saveCompleted) {
        if (uiState.saveCompleted) {
            Toast.makeText(context, savedMessage, Toast.LENGTH_SHORT).show()
            onSaved()
        }
    }

    when {
        !uiState.isLoaded -> Unit

        !uiState.found -> EmptyState(
            message = stringResource(R.string.facility_admin_not_found),
            actionLabel = stringResource(R.string.top_bar_home),
            onAction = onGoHome,
            modifier = modifier.fillMaxSize()
        )

        else -> Box(modifier = modifier.fillMaxSize()) {
            DecorativeBackground(modifier = Modifier.fillMaxSize(), showBookAndPlantDecoration = false)

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(LibraryDimens.ScreenPadding),
                verticalArrangement = Arrangement.spacedBy(LibraryDimens.CardSpacing)
            ) {
                AdminTextField(
                    value = uiState.name,
                    onValueChange = onNameChange,
                    label = stringResource(R.string.facility_admin_field_name),
                    errorText = uiState.nameError
                )
                AdminDropdownField(
                    label = stringResource(R.string.facility_admin_field_floor),
                    options = FLOOR_OPTIONS,
                    selected = uiState.floorText.trim().toIntOrNull(),
                    optionLabel = { floorOptionLabel(it) },
                    onSelect = { floor -> onFloorChange(floor?.toString().orEmpty()) },
                    errorText = uiState.floorError
                )
                AdminTextField(
                    value = uiState.description,
                    onValueChange = onDescriptionChange,
                    label = stringResource(R.string.facility_admin_field_description),
                    singleLine = false,
                    minLines = 2
                )

                Text(
                    text = stringResource(R.string.facility_admin_field_guide_mode),
                    style = MaterialTheme.typography.titleMedium
                )
                GuideModeOption(
                    label = stringResource(R.string.facility_detail_escort_action),
                    selected = uiState.guideMode == GuideMode.ESCORT,
                    onClick = { onGuideModeChange(GuideMode.ESCORT) }
                )
                GuideModeOption(
                    label = stringResource(R.string.facility_detail_location_action),
                    selected = uiState.guideMode == GuideMode.LOCATION_ONLY,
                    onClick = { onGuideModeChange(GuideMode.LOCATION_ONLY) }
                )
                GuideModeOption(
                    label = stringResource(R.string.facility_admin_guide_both),
                    selected = uiState.guideMode == GuideMode.BOTH,
                    onClick = { onGuideModeChange(GuideMode.BOTH) }
                )

                // 기준층(baseFloor)과 다른 층일 때 의미가 있어 그때 보여준다 — 연결통로 안내도
                // 대상 시설(allowDirectionOnBaseFloor)은 기준층이어도 예외로 보여준다(요구사항:
                // "기준층이 아닌 목적지와 동일하게 방향을 선택").
                if (uiState.floorText.trim().toIntOrNull() != uiState.baseFloor || uiState.allowDirectionOnBaseFloor) {
                    Text(
                        text = stringResource(R.string.facility_admin_field_direction),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        DIRECTION_KEYS.forEach { key ->
                            FilterChip(
                                selected = uiState.direction == key,
                                onClick = { onDirectionChange(key) },
                                label = { Text(directionLabel(key)) }
                            )
                        }
                    }
                }

                // 실제 goTo() 목적지를 이 시설(sourcePoiName)과 다르게 지정하고 싶을 때 쓴다
                // (요청: "위치이름과 가야할곳이 디폴트로는 같은곳이 되고, 사용자가 추가로
                // 가야할곳을 다른곳으로 지정할수있게"). 목록은 오타 방지를 위해 현재 temi
                // 지도에서 확인된 POI로만 제한한다(층 드롭다운과 같은 방식).
                AdminDropdownField(
                    label = stringResource(R.string.facility_admin_field_navigation_target),
                    options = listOf(null) + uiState.knownLocations,
                    selected = uiState.navigationTargetOverride,
                    optionLabel = { navigationTargetOptionLabel(it) },
                    onSelect = onNavigationTargetOverrideChange
                )

                Text(
                    text = stringResource(R.string.facility_admin_field_icon),
                    style = MaterialTheme.typography.titleMedium
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    ICON_KEYS.forEach { key ->
                        FilterChip(
                            selected = uiState.iconKey == key,
                            onClick = { onIconKeyChange(key) },
                            label = { Text(iconKeyLabel(key)) }
                        )
                    }
                }

                AdminSwitchRow(
                    label = stringResource(R.string.facility_admin_field_enabled),
                    checked = uiState.isEnabled,
                    onCheckedChange = onEnabledChange
                )
                AdminSwitchRow(
                    label = stringResource(R.string.facility_admin_field_featured),
                    checked = uiState.isFeatured,
                    onCheckedChange = onFeaturedChange
                )

                AdminTextField(
                    value = uiState.sortOrderText,
                    onValueChange = onSortOrderChange,
                    label = stringResource(R.string.facility_admin_field_sort_order),
                    keyboardType = KeyboardType.Number
                )

                LibraryPrimaryButton(
                    text = stringResource(R.string.facility_admin_save),
                    onClick = onSave
                )
            }
        }
    }
}

@Composable
private fun GuideModeOption(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Text(text = label, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun navigationTargetOptionLabel(target: String?): String =
    target ?: stringResource(R.string.facility_admin_navigation_target_auto)

@Composable
private fun floorOptionLabel(floor: Int?): String =
    if (floor == null) stringResource(R.string.facility_admin_unset_floor) else formatFloorBadge(LocalContext.current, floor)

@Composable
private fun directionLabel(key: FacilityDirection?): String = when (key) {
    FacilityDirection.RIGHT -> stringResource(R.string.facility_direction_right)
    FacilityDirection.FRONT -> stringResource(R.string.facility_direction_front)
    FacilityDirection.LEFT -> stringResource(R.string.facility_direction_left)
    null -> stringResource(R.string.facility_direction_none)
}

@Composable
private fun iconKeyLabel(key: String?): String = when (key) {
    "child_care" -> stringResource(R.string.facility_admin_icon_child)
    "computer" -> stringResource(R.string.facility_admin_icon_computer)
    "library_books" -> stringResource(R.string.facility_admin_icon_library)
    "groups" -> stringResource(R.string.facility_admin_icon_groups)
    else -> stringResource(R.string.facility_admin_icon_auto)
}
