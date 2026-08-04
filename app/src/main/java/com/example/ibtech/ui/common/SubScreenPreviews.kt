package com.example.ibtech.ui.common

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.ibtech.ui.theme.IBTECHTheme

/**
 * 하위 화면 공통 뼈대(상단바 + Scaffold)가 13.3인치 가로 화면과 소형 화면 모두에서
 * 잘림 없이 동작하는지 확인하는 미리보기 (요구사항 로드맵 2단계 5·6번 작업).
 *
 * 시설 안내/이용방법/어린이 콘텐츠 각 화면의 실제 내용은 이후 단계(4/7/8단계)에서 채워지므로
 * 지금은 [PlaceholderScreen]으로 상단바·레이아웃만 검증한다.
 */
@Composable
private fun SubScreenSample() {
    LibraryScaffold(
        title = "시설 안내",
        onBack = {},
        onHome = {}
    ) { padding ->
        PlaceholderScreen(
            description = "시설 안내 화면은 4단계에서 구성됩니다.",
            modifier = Modifier.padding(padding)
        )
    }
}

@Preview(name = "하위 화면 · 13.3인치 가로", widthDp = 1280, heightDp = 800, showBackground = true)
@Composable
private fun SubScreenLargePreview() {
    IBTECHTheme { SubScreenSample() }
}

@Preview(name = "하위 화면 · 소형 화면 폴백", widthDp = 360, heightDp = 800, showBackground = true)
@Composable
private fun SubScreenSmallPreview() {
    IBTECHTheme { SubScreenSample() }
}
