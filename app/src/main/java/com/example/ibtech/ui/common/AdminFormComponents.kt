package com.example.ibtech.ui.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.ibtech.R
import com.example.ibtech.ui.theme.CoralAccent
import com.example.ibtech.ui.theme.LibraryDimens

/**
 * 관리자 CRUD 화면 공용 컴포넌트 (로드맵 10단계). 시설/이용정보/어린이콘텐츠/행사·공지 관리
 * 화면이 모두 "목록 + 추가/수정 폼"이라 폼과 목록 행을 여기서 공용화한다.
 */
@Composable
fun AdminFormDialog(
    title: String,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
    saveLabel: String,
    cancelLabel: String,
    modifier: Modifier = Modifier,
    saveEnabled: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = modifier.fillMaxWidth(),
            // 관리자 화면 모서리는 16~20dp로 통일한다(최종 단계 D) — 방문객 카드의 24dp보다 좁다.
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(LibraryDimens.CardPadding)
                    .heightIn(max = 560.dp)
            ) {
                Text(text = title, style = MaterialTheme.typography.titleLarge)

                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState())
                        .padding(vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    content()
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    // 저장=Teal(주요 색 강조), 취소=보조(중립색 텍스트)로 구분한다.
                    // 저장은 System.currentTimeMillis() 기반 ID 생성 + 비동기 저장이라, 다른 공용
                    // 버튼처럼 debounced()로 짧은 시간 안의 중복 탭을 막는다 — 안 그러면 두 번째
                    // 탭이 다이얼로그가 닫히기 전에 새 ID로 같은 내용을 한 번 더 저장할 수 있다.
                    TextButton(onClick = debounced(onDismiss)) {
                        Text(cancelLabel, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    TextButton(onClick = debounced(onSave), enabled = saveEnabled) {
                        Text(saveLabel, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun AdminTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    errorText: String? = null,
    singleLine: Boolean = true,
    minLines: Int = 1,
    keyboardType: KeyboardType = KeyboardType.Text,
    /** 이 필드만 글자를 키우고 싶을 때 쓴다(예: 행사 안내 URL) — 지정 안 하면 기존 크기 그대로다. */
    textStyle: TextStyle = LocalTextStyle.current,
    labelStyle: TextStyle = textStyle
) {
    Column(modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label, style = labelStyle) },
            textStyle = textStyle,
            singleLine = singleLine,
            minLines = minLines,
            isError = errorText != null,
            keyboardOptions = KeyboardOptions(
                keyboardType = keyboardType,
                capitalization = KeyboardCapitalization.None
            ),
            modifier = Modifier.fillMaxWidth()
        )
        if (errorText != null) {
            Text(
                text = errorText,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(start = 16.dp, top = 4.dp)
            )
        }
    }
}

/**
 * 관리자 폼의 드롭다운 선택 필드([AdminTextField]와 같은 시각 스타일) — 고정된 선택지 중에서만
 * 고르게 해 자유 입력 오타를 막는다(예: 시설 층 설정, 요청: "드롭다운방식으로 바꾸면 편한거
 * 같은데"). [options]는 실제 값 목록, [optionLabel]은 그 값을 화면에 보여줄 문구로 바꾼다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> AdminDropdownField(
    label: String,
    options: List<T>,
    selected: T,
    optionLabel: @Composable (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    errorText: String? = null
) {
    var expanded by remember { mutableStateOf(false) }
    Column(modifier = modifier.fillMaxWidth()) {
        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
            OutlinedTextField(
                value = optionLabel(selected),
                onValueChange = {},
                readOnly = true,
                label = { Text(label) },
                isError = errorText != null,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    .fillMaxWidth()
            )
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(optionLabel(option)) },
                        onClick = {
                            onSelect(option)
                            expanded = false
                        }
                    )
                }
            }
        }
        if (errorText != null) {
            Text(
                text = errorText,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(start = 16.dp, top = 4.dp)
            )
        }
    }
}

@Composable
fun AdminSwitchRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyLarge)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

/** 관리자 목록 화면의 공용 행: 제목/부제/배지 + 수정·삭제 아이콘 버튼.
 * [subtitle]은 [AnnotatedString]이라 호출부에서 일부 구간(예: "미설정")만 다른 색으로
 * 강조할 수 있다 — 그냥 문자열만 필요하면 `AnnotatedString(text)`로 감싸서 넘기면 된다. */
@Composable
fun AdminListRow(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: AnnotatedString? = null,
    trailingBadge: String? = null,
    onEdit: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null
) {
    // 편집 아이콘이 작아서 누르기 어렵다는 피드백에 따라 행 전체를 눌러도 편집이 열리게 한다 —
    // 삭제는 되돌리기 어려운 동작이라 아이콘을 직접 눌러야만 실행되도록 그대로 둔다(중첩된
    // IconButton의 클릭이 이 행의 클릭보다 우선 처리된다).
    val rowModifier = if (onEdit != null) {
        modifier.fillMaxWidth().clickable(onClick = debounced(onEdit))
    } else {
        modifier.fillMaxWidth()
    }
    LibraryCard(modifier = rowModifier, shape = MaterialTheme.shapes.medium) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(LibraryDimens.CardPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (trailingBadge != null) {
                        Text(
                            text = trailingBadge,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (onEdit != null) {
                IconButton(
                    onClick = debounced(onEdit),
                    modifier = Modifier.size(LibraryDimens.MinTouchTarget)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Edit,
                        contentDescription = stringResource(R.string.admin_list_edit_action, title)
                    )
                }
            }
            if (onDelete != null) {
                IconButton(
                    onClick = debounced(onDelete),
                    modifier = Modifier.size(LibraryDimens.MinTouchTarget)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = stringResource(R.string.admin_list_delete_action, title),
                        // 삭제=Error/Coral로 구분한다(최종 단계 D).
                        tint = CoralAccent
                    )
                }
            }
        }
    }
}
