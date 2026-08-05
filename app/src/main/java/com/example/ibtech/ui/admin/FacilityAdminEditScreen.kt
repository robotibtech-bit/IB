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
import com.example.ibtech.domain.model.GuideMode
import com.example.ibtech.ui.common.AdminSwitchRow
import com.example.ibtech.ui.common.AdminTextField
import com.example.ibtech.ui.common.DecorativeBackground
import com.example.ibtech.ui.common.EmptyState
import com.example.ibtech.ui.common.LibraryPrimaryButton
import com.example.ibtech.ui.theme.LibraryDimens

private val ICON_KEYS = listOf(null, "child_care", "computer", "library_books", "groups")

/** 시설 편집 화면 (로드맵 10단계): 표시명·층·설명·안내방식·아이콘·노출여부·순서. */
@Composable
fun FacilityAdminEditScreen(
    uiState: FacilityAdminEditUiState,
    onNameChange: (String) -> Unit,
    onFloorChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onGuideModeChange: (GuideMode) -> Unit,
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
                AdminTextField(
                    value = uiState.floorText,
                    onValueChange = onFloorChange,
                    label = stringResource(R.string.facility_admin_field_floor),
                    errorText = uiState.floorError,
                    keyboardType = KeyboardType.Number
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
private fun iconKeyLabel(key: String?): String = when (key) {
    "child_care" -> stringResource(R.string.facility_admin_icon_child)
    "computer" -> stringResource(R.string.facility_admin_icon_computer)
    "library_books" -> stringResource(R.string.facility_admin_icon_library)
    "groups" -> stringResource(R.string.facility_admin_icon_groups)
    else -> stringResource(R.string.facility_admin_icon_auto)
}
