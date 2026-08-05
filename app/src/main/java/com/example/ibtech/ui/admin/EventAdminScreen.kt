package com.example.ibtech.ui.admin

import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.ibtech.R
import com.example.ibtech.domain.model.Facility
import com.example.ibtech.domain.model.LibraryEvent
import com.example.ibtech.domain.model.LibraryNotice
import com.example.ibtech.ui.common.AdminFormDialog
import com.example.ibtech.ui.common.AdminListRow
import com.example.ibtech.ui.common.AdminSwitchRow
import com.example.ibtech.ui.common.AdminTextField
import com.example.ibtech.ui.common.ConfirmDialog
import com.example.ibtech.ui.common.DecorativeBackground
import com.example.ibtech.ui.common.LibraryOutlinedButton
import com.example.ibtech.ui.theme.LibraryDimens

private enum class EventAdminTab { EVENTS, NOTICES }

/** 행사/공지 관리 화면 (로드맵 10단계). */
@Composable
fun EventAdminScreen(
    uiState: EventAdminUiState,
    onAddEvent: () -> Unit,
    onEditEvent: (LibraryEvent) -> Unit,
    onDeleteEvent: (String) -> Unit,
    onAddNotice: () -> Unit,
    onEditNotice: (LibraryNotice) -> Unit,
    onDeleteNotice: (String) -> Unit,
    onDismissDialog: () -> Unit,
    onEventTitleChange: (String) -> Unit,
    onEventStartDateChange: (String) -> Unit,
    onEventEndDateChange: (String) -> Unit,
    onEventTimeTextChange: (String) -> Unit,
    onEventPlaceChange: (String) -> Unit,
    onEventTargetChange: (String) -> Unit,
    onEventDescriptionChange: (String) -> Unit,
    onEventQrUrlChange: (String) -> Unit,
    onEventFacilityChange: (String?) -> Unit,
    onEventEnabledChange: (Boolean) -> Unit,
    onEventSortOrderChange: (String) -> Unit,
    onSaveEventDraft: () -> Unit,
    onNoticeTextChange: (String) -> Unit,
    onNoticeEnabledChange: (Boolean) -> Unit,
    onNoticeSortOrderChange: (String) -> Unit,
    onSaveNoticeDraft: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(EventAdminTab.EVENTS) }
    var pendingDeleteEvent by remember { mutableStateOf<LibraryEvent?>(null) }
    var pendingDeleteNotice by remember { mutableStateOf<LibraryNotice?>(null) }

    Box(modifier = modifier.fillMaxSize()) {
        DecorativeBackground(modifier = Modifier.fillMaxSize())

        if (!uiState.isLoaded) return@Box

        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = LibraryDimens.ScreenPadding, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedTab == EventAdminTab.EVENTS,
                    onClick = { selectedTab = EventAdminTab.EVENTS },
                    label = { Text(stringResource(R.string.event_admin_tab_events)) }
                )
                FilterChip(
                    selected = selectedTab == EventAdminTab.NOTICES,
                    onClick = { selectedTab = EventAdminTab.NOTICES },
                    label = { Text(stringResource(R.string.event_admin_tab_notices)) }
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = LibraryDimens.ScreenPadding),
                horizontalArrangement = Arrangement.End
            ) {
                LibraryOutlinedButton(
                    text = stringResource(R.string.usage_admin_add_action),
                    onClick = if (selectedTab == EventAdminTab.EVENTS) onAddEvent else onAddNotice,
                    modifier = Modifier.fillMaxWidth(0.4f)
                )
            }

            LazyColumn(
                contentPadding = PaddingValues(LibraryDimens.ScreenPadding),
                verticalArrangement = Arrangement.spacedBy(LibraryDimens.CardSpacing),
                modifier = Modifier.fillMaxSize()
            ) {
                if (selectedTab == EventAdminTab.EVENTS) {
                    items(uiState.events, key = { it.id }) { event ->
                        AdminListRow(
                            title = event.title,
                            subtitle = event.startDate,
                            onEdit = { onEditEvent(event) },
                            onDelete = { pendingDeleteEvent = event }
                        )
                    }
                } else {
                    items(uiState.notices, key = { it.id }) { notice ->
                        AdminListRow(
                            title = notice.text,
                            onEdit = { onEditNotice(notice) },
                            onDelete = { pendingDeleteNotice = notice }
                        )
                    }
                }
            }
        }

        when (val draft = uiState.editingDraft) {
            is EventAdminDraft.Event -> EventDraftDialog(
                draft = draft.draft,
                facilities = uiState.facilities,
                onDismiss = onDismissDialog,
                onSave = onSaveEventDraft,
                onTitleChange = onEventTitleChange,
                onStartDateChange = onEventStartDateChange,
                onEndDateChange = onEventEndDateChange,
                onTimeTextChange = onEventTimeTextChange,
                onPlaceChange = onEventPlaceChange,
                onTargetChange = onEventTargetChange,
                onDescriptionChange = onEventDescriptionChange,
                onQrUrlChange = onEventQrUrlChange,
                onFacilityChange = onEventFacilityChange,
                onEnabledChange = onEventEnabledChange,
                onSortOrderChange = onEventSortOrderChange
            )

            is EventAdminDraft.Notice -> NoticeDraftDialog(
                draft = draft.draft,
                onDismiss = onDismissDialog,
                onSave = onSaveNoticeDraft,
                onTextChange = onNoticeTextChange,
                onEnabledChange = onNoticeEnabledChange,
                onSortOrderChange = onNoticeSortOrderChange
            )

            null -> Unit
        }

        pendingDeleteEvent?.let { target ->
            ConfirmDialog(
                title = stringResource(R.string.usage_admin_delete_confirm_title),
                body = stringResource(R.string.usage_admin_delete_confirm_body, target.title),
                confirmLabel = stringResource(R.string.facility_admin_delete_confirm_action),
                dismissLabel = stringResource(R.string.facility_detail_escort_confirm_cancel),
                onConfirm = { onDeleteEvent(target.id); pendingDeleteEvent = null },
                onDismiss = { pendingDeleteEvent = null }
            )
        }
        pendingDeleteNotice?.let { target ->
            ConfirmDialog(
                title = stringResource(R.string.usage_admin_delete_confirm_title),
                body = stringResource(R.string.usage_admin_delete_confirm_body, target.text),
                confirmLabel = stringResource(R.string.facility_admin_delete_confirm_action),
                dismissLabel = stringResource(R.string.facility_detail_escort_confirm_cancel),
                onConfirm = { onDeleteNotice(target.id); pendingDeleteNotice = null },
                onDismiss = { pendingDeleteNotice = null }
            )
        }
    }
}

@Composable
private fun EventDraftDialog(
    draft: EventDraft,
    facilities: List<Facility>,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
    onTitleChange: (String) -> Unit,
    onStartDateChange: (String) -> Unit,
    onEndDateChange: (String) -> Unit,
    onTimeTextChange: (String) -> Unit,
    onPlaceChange: (String) -> Unit,
    onTargetChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onQrUrlChange: (String) -> Unit,
    onFacilityChange: (String?) -> Unit,
    onEnabledChange: (Boolean) -> Unit,
    onSortOrderChange: (String) -> Unit
) {
    AdminFormDialog(
        title = if (draft.id == null) stringResource(R.string.admin_dialog_title_add) else stringResource(R.string.admin_dialog_title_edit),
        onDismiss = onDismiss,
        onSave = onSave,
        saveLabel = stringResource(R.string.admin_dialog_save),
        cancelLabel = stringResource(R.string.admin_dialog_cancel)
    ) {
        AdminTextField(value = draft.title, onValueChange = onTitleChange, label = stringResource(R.string.usage_admin_field_title), errorText = draft.titleError)
        AdminTextField(
            value = draft.startDate,
            onValueChange = onStartDateChange,
            label = stringResource(R.string.event_admin_field_start_date),
            errorText = draft.startDateError
        )
        AdminTextField(value = draft.endDate, onValueChange = onEndDateChange, label = stringResource(R.string.event_admin_field_end_date))
        AdminTextField(value = draft.timeText, onValueChange = onTimeTextChange, label = stringResource(R.string.event_admin_field_time))
        AdminTextField(value = draft.place, onValueChange = onPlaceChange, label = stringResource(R.string.event_admin_field_place))
        AdminTextField(value = draft.target, onValueChange = onTargetChange, label = stringResource(R.string.event_admin_field_target))
        AdminTextField(
            value = draft.description,
            onValueChange = onDescriptionChange,
            label = stringResource(R.string.event_admin_field_description),
            singleLine = false,
            minLines = 2
        )
        AdminTextField(value = draft.qrUrl, onValueChange = onQrUrlChange, label = stringResource(R.string.usage_admin_field_qr))

        Text(text = stringResource(R.string.usage_admin_field_facility), style = MaterialTheme.typography.titleMedium)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = draft.relatedFacilityId == null,
                onClick = { onFacilityChange(null) },
                label = { Text(stringResource(R.string.usage_admin_facility_none)) }
            )
            facilities.forEach { facility ->
                FilterChip(
                    selected = draft.relatedFacilityId == facility.id,
                    onClick = { onFacilityChange(facility.id) },
                    label = { Text(facility.name) }
                )
            }
        }

        AdminSwitchRow(
            label = stringResource(R.string.usage_admin_field_enabled),
            checked = draft.isEnabled,
            onCheckedChange = onEnabledChange
        )
        AdminTextField(
            value = draft.sortOrderText,
            onValueChange = onSortOrderChange,
            label = stringResource(R.string.facility_admin_field_sort_order),
            keyboardType = KeyboardType.Number
        )
    }
}

@Composable
private fun NoticeDraftDialog(
    draft: NoticeDraft,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
    onTextChange: (String) -> Unit,
    onEnabledChange: (Boolean) -> Unit,
    onSortOrderChange: (String) -> Unit
) {
    AdminFormDialog(
        title = if (draft.id == null) stringResource(R.string.admin_dialog_title_add) else stringResource(R.string.admin_dialog_title_edit),
        onDismiss = onDismiss,
        onSave = onSave,
        saveLabel = stringResource(R.string.admin_dialog_save),
        cancelLabel = stringResource(R.string.admin_dialog_cancel)
    ) {
        AdminTextField(
            value = draft.text,
            onValueChange = onTextChange,
            label = stringResource(R.string.event_admin_field_notice_text),
            errorText = draft.textError,
            singleLine = false,
            minLines = 2
        )
        AdminSwitchRow(
            label = stringResource(R.string.usage_admin_field_enabled),
            checked = draft.isEnabled,
            onCheckedChange = onEnabledChange
        )
        AdminTextField(
            value = draft.sortOrderText,
            onValueChange = onSortOrderChange,
            label = stringResource(R.string.facility_admin_field_sort_order),
            keyboardType = KeyboardType.Number
        )
    }
}
