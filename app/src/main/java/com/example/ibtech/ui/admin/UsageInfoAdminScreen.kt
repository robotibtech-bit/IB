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
import com.example.ibtech.domain.model.UsageTopic
import com.example.ibtech.ui.common.AdminFormDialog
import com.example.ibtech.ui.common.AdminListRow
import com.example.ibtech.ui.common.AdminSwitchRow
import com.example.ibtech.ui.common.AdminTextField
import com.example.ibtech.ui.common.ConfirmDialog
import com.example.ibtech.ui.common.DecorativeBackground
import com.example.ibtech.ui.common.LibraryOutlinedButton
import com.example.ibtech.ui.theme.LibraryDimens

/** 이용정보 관리 화면 (로드맵 10단계): 4개 고정 카테고리 아래 세부 항목 CRUD. */
@Composable
fun UsageInfoAdminScreen(
    uiState: UsageInfoAdminUiState,
    onAddTopic: (String) -> Unit,
    onEditTopic: (UsageTopic) -> Unit,
    onDeleteTopic: (String) -> Unit,
    onDismissDialog: () -> Unit,
    onDraftTitleChange: (String) -> Unit,
    onDraftShortAnswerChange: (String) -> Unit,
    onDraftQrUrlChange: (String) -> Unit,
    onDraftFacilityChange: (String?) -> Unit,
    onDraftEnabledChange: (Boolean) -> Unit,
    onDraftSortOrderChange: (String) -> Unit,
    onSaveDraft: () -> Unit,
    modifier: Modifier = Modifier
) {
    var pendingDelete by remember { mutableStateOf<UsageTopic?>(null) }

    Box(modifier = modifier.fillMaxSize()) {
        DecorativeBackground(modifier = Modifier.fillMaxSize())

        if (!uiState.isLoaded) return@Box

        LazyColumn(
            contentPadding = PaddingValues(LibraryDimens.ScreenPadding),
            verticalArrangement = Arrangement.spacedBy(LibraryDimens.CardSpacing),
            modifier = Modifier.fillMaxSize()
        ) {
            uiState.categories.forEach { category ->
                item(key = "category_${category.id}") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = category.title, style = MaterialTheme.typography.titleLarge)
                        LibraryOutlinedButton(
                            text = stringResource(R.string.usage_admin_add_action),
                            onClick = { onAddTopic(category.id) },
                            modifier = Modifier.fillMaxWidth(0.3f)
                        )
                    }
                }

                val topics = uiState.topicsByCategory[category.id].orEmpty()
                items(topics, key = { it.id }) { topic ->
                    AdminListRow(
                        title = topic.title,
                        subtitle = topic.shortAnswer,
                        onEdit = { onEditTopic(topic) },
                        onDelete = { pendingDelete = topic }
                    )
                }
            }
        }

        val draft = uiState.editingDraft
        if (draft != null) {
            AdminFormDialog(
                title = if (draft.id == null) {
                    stringResource(R.string.admin_dialog_title_add)
                } else {
                    stringResource(R.string.admin_dialog_title_edit)
                },
                onDismiss = onDismissDialog,
                onSave = onSaveDraft,
                saveLabel = stringResource(R.string.admin_dialog_save),
                cancelLabel = stringResource(R.string.admin_dialog_cancel)
            ) {
                AdminTextField(
                    value = draft.title,
                    onValueChange = onDraftTitleChange,
                    label = stringResource(R.string.usage_admin_field_title),
                    errorText = draft.titleError
                )
                AdminTextField(
                    value = draft.shortAnswer,
                    onValueChange = onDraftShortAnswerChange,
                    label = stringResource(R.string.usage_admin_field_answer),
                    singleLine = false,
                    minLines = 2
                )
                AdminTextField(
                    value = draft.qrUrl,
                    onValueChange = onDraftQrUrlChange,
                    label = stringResource(R.string.usage_admin_field_qr)
                )

                Text(
                    text = stringResource(R.string.usage_admin_field_facility),
                    style = MaterialTheme.typography.titleMedium
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = draft.relatedFacilityId == null,
                        onClick = { onDraftFacilityChange(null) },
                        label = { Text(stringResource(R.string.usage_admin_facility_none)) }
                    )
                    uiState.facilities.forEach { facility ->
                        FilterChip(
                            selected = draft.relatedFacilityId == facility.id,
                            onClick = { onDraftFacilityChange(facility.id) },
                            label = { Text(facility.name) }
                        )
                    }
                }

                AdminSwitchRow(
                    label = stringResource(R.string.usage_admin_field_enabled),
                    checked = draft.isEnabled,
                    onCheckedChange = onDraftEnabledChange
                )
                AdminTextField(
                    value = draft.sortOrderText,
                    onValueChange = onDraftSortOrderChange,
                    label = stringResource(R.string.facility_admin_field_sort_order),
                    keyboardType = KeyboardType.Number
                )
            }
        }

        val target = pendingDelete
        if (target != null) {
            ConfirmDialog(
                title = stringResource(R.string.usage_admin_delete_confirm_title),
                body = stringResource(R.string.usage_admin_delete_confirm_body, target.title),
                confirmLabel = stringResource(R.string.facility_admin_delete_confirm_action),
                dismissLabel = stringResource(R.string.facility_detail_escort_confirm_cancel),
                onConfirm = {
                    onDeleteTopic(target.id)
                    pendingDelete = null
                },
                onDismiss = { pendingDelete = null }
            )
        }
    }
}
