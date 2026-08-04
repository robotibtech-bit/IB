package com.example.ibtech.ui.common

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable

/**
 * 확인 버튼 하나뿐인 공용 안내창 ([ConfirmDialog]의 축소판). "직원 도움"처럼 취소할 대상 행동이
 * 없는 순수 정보 안내에 쓴다(요구사항 명세서 1절 "직원 도움은 다이얼로그로 처리").
 */
@Composable
fun InfoDialog(
    title: String,
    body: String,
    dismissLabel: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(body) },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(dismissLabel) }
        }
    )
}
