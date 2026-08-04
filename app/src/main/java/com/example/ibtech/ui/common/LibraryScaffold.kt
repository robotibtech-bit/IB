package com.example.ibtech.ui.common

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * 화면 공통 뼈대.
 *
 * 메인 화면은 [title]을 null로 두어 상단바 자체를 그리지 않는다(3.6절 "메인에는 홈 버튼이
 * 불필요"). 하위 화면은 [title]/[onBack]/[onHome]을 모두 채워 [LibraryTopAppBar]를 노출한다.
 */
@Composable
fun LibraryScaffold(
    title: String?,
    onBack: () -> Unit,
    onHome: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (PaddingValues) -> Unit
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            if (title != null) {
                LibraryTopAppBar(title = title, onBack = onBack, onHome = onHome)
            }
        },
        content = content
    )
}
