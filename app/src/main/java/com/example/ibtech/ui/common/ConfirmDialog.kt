package com.example.ibtech.ui.common

import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties

/**
 * 공용 확인창 (요구사항 명세서 3.6절 "이동 중지 확인", 2.4절 "동행 안내 시작 확인" 등에서 재사용).
 *
 * 2단계에서 전체 UI 디자인 패키지 기준 모서리(24dp)·버튼 우선순위(주요 행동 강조)만 정돈했다 —
 * 파라미터는 그대로다.
 *
 * [LibraryTypography]가 이 화면들처럼 몇 걸음 떨어져서 보는 키오스크용으로 아주 크다(제목
 * headlineSmall 54sp) — Material3 AlertDialog 기본 너비(휴대폰 기준)로는 제목 한 줄이 두 줄로
 * 쪼개져 잘려 보인다(사용자 피드백: "가로 세로를 늘려서 한줄로"). `usePlatformDefaultWidth =
 * false` + 고정 너비로 이 큰 글자에 맞춰 폭을 넉넉히 잡는다.
 *
 * [emphasize]가 [body] 안에 있으면(주로 목적지 시설명) 그 구간만 굵게 강조한다(사용자 요청:
 * "목적지 명만 두껍께 표시"). 못 찾으면(null이거나 매칭 실패) 그냥 일반 굵기로 보여준다.
 */
@Composable
fun ConfirmDialog(
    title: String,
    body: String,
    confirmLabel: String,
    dismissLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    emphasize: String? = null
) {
    val annotatedBody = remember(body, emphasize) {
        val start = emphasize?.let { body.indexOf(it) } ?: -1
        if (emphasize == null || start < 0) {
            AnnotatedString(body)
        } else {
            buildAnnotatedString {
                append(body)
                addStyle(SpanStyle(fontWeight = FontWeight.Bold), start, start + emphasize.length)
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier.width(1100.dp),
        shape = RoundedCornerShape(24.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text(title, color = MaterialTheme.colorScheme.onSurface) },
        text = { Text(annotatedBody, color = MaterialTheme.colorScheme.onSurfaceVariant) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(confirmLabel, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(dismissLabel, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    )
}
