package com.example.ibtech.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.ibtech.ui.theme.LibraryDimens

/**
 * 아직 구현되지 않은 하위 화면의 자리표시자.
 *
 * 로드맵의 각 단계(4/7/8/9/10단계)가 실제 화면으로 이 자리를 대체할 때까지, 라우트가
 * 존재하고 공통 상단바(뒤로가기/홈)가 정상 동작하는지 지금 단계에서 검증하기 위해 둔다.
 */
@Composable
fun PlaceholderScreen(
    description: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(LibraryDimens.ScreenPadding),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = description,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
