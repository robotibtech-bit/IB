package com.example.ibtech.ui.common

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke

/**
 * PDF 표지(`docs/temi.pdf` 1쪽)의 민트 곱선 장식을 재해석한 배경 레이어.
 *
 * 이미지 파일 없이 두 개의 굵은 원호만으로 같은 인상을 낸다. 반드시 화면 콘텐츠보다 먼저(아래)
 * 그려야 한다 — 호출부는 `Box`의 첫 번째 자식으로 이 컴포저블을 두고 그 위에 실제 콘텐츠를
 * 겹쳐 쌓는다. 불투명도를 낮게 유지해 텍스트 대비 기준(요구사항 3.8절)에 영향을 주지 않는다.
 */
@Composable
fun DecorativeBackground(modifier: Modifier = Modifier) {
    val mint = MaterialTheme.colorScheme.primary

    Canvas(modifier = modifier.fillMaxSize()) {
        val strokeWidth = size.minDimension * 0.16f

        // 좌하단 큰 원호 — PDF 표지 좌측 하단의 굵은 민트 반원.
        drawArc(
            color = mint.copy(alpha = 0.08f),
            startAngle = 90f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = Offset(-size.width * 0.32f, size.height * 0.58f),
            size = Size(size.width * 0.72f, size.width * 0.72f),
            style = Stroke(width = strokeWidth)
        )

        // 우상단 작은 원호 — PDF 표지 우상단의 포인트 곡선.
        drawArc(
            color = mint.copy(alpha = 0.10f),
            startAngle = 270f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = Offset(size.width * 0.78f, -size.height * 0.18f),
            size = Size(size.width * 0.42f, size.width * 0.42f),
            style = Stroke(width = strokeWidth * 0.55f)
        )
    }
}
