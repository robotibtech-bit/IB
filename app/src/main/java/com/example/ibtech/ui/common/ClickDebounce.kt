package com.example.ibtech.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

private const val DEBOUNCE_MILLIS = 800L

/**
 * 연속 터치 방지 (요구사항 3.8절).
 *
 * 짧은 시간 안의 반복 탭에서 첫 번째만 실행한다. 모든 공통 버튼/상단바 액션이 이 함수를
 * 거치므로 화면마다 따로 막을 필요가 없다.
 */
@Composable
fun debounced(onClick: () -> Unit): () -> Unit {
    var lastClickAtMillis by remember { mutableLongStateOf(0L) }
    return remember(onClick) {
        {
            val now = System.currentTimeMillis()
            if (now - lastClickAtMillis >= DEBOUNCE_MILLIS) {
                lastClickAtMillis = now
                onClick()
            }
        }
    }
}
