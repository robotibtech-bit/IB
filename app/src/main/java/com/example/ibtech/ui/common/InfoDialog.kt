package com.example.ibtech.ui.common

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

/**
 * 확인 버튼 하나뿐인 공용 안내창 ([ConfirmDialog]의 축소판). "직원 도움"처럼 취소할 대상 행동이
 * 없는 순수 정보 안내에 쓴다(요구사항 명세서 1절 "직원 도움은 다이얼로그로 처리").
 *
 * 2단계에서 전체 UI 디자인 패키지 기준 모서리(24dp)·색상만 정돈했다 — 파라미터는 그대로다.
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
        shape = RoundedCornerShape(24.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text(title, color = MaterialTheme.colorScheme.onSurface) },
        text = { Text(body, color = MaterialTheme.colorScheme.onSurfaceVariant) },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(dismissLabel, color = MaterialTheme.colorScheme.primary)
            }
        }
    )
}
