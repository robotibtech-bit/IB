package com.example.ibtech.ui.common

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import com.example.ibtech.ui.theme.NavyTitle

/**
 * PDF 표지(`docs/temi.pdf` 1쪽)의 민트 곱선 장식을 재해석한 배경 레이어.
 *
 * 이미지 파일 없이 원호와 도형만으로 같은 인상을 낸다. 반드시 화면 콘텐츠보다 먼저(아래)
 * 그려야 한다 — 호출부는 `Box`의 첫 번째 자식으로 이 컴포저블을 두고 그 위에 실제 콘텐츠를
 * 겹쳐 쌓는다. 불투명도를 낮게 유지해 텍스트 대비 기준(요구사항 3.8절)에 영향을 주지 않는다.
 *
 * 전체 UI 디자인 패키지(2단계)에서 요청한 "책·식물 느낌의 낮은 채도 장식"을 이미지 없이
 * Canvas 도형(책 등처럼 보이는 둥근 세로 막대 + 잎처럼 보이는 타원)으로 추가했다. 글자·버튼과
 * 겹치지 않도록 화면 네 모서리 영역에만 그린다.
 *
 * 최종 단계("공통 배경 보완")에서 실기 확인 결과 장식이 거의 안 보인다는 피드백에 따라 책/잎
 * 도형 크기를 약 1.8배, 불투명도를 0.08~0.14 범위로 올렸다. 카드는 흰색 표면 위에 그려지므로
 * 이 정도 불투명도에서도 카드 안쪽 글자 대비에는 영향이 없다 — 카드가 없는 배경 빈 공간과
 * 화면 가장자리에서만 도드라진다.
 *
 * [showBookAndPlantDecoration] 기본값 true는 기존 방문객 호출부를 전부 그대로 둔다. 최종
 * 단계 D("관리자 화면에 방문객용 책·식물 장식을 적용하지 마")에 따라 관리자·개발자 화면만
 * false를 넘겨 책/잎 도형을 끈다 — 두 원호는 이번 세션 이전부터 있던 장식이라 관리자 화면에도
 * 그대로 둔다(민원 대상은 새로 추가한 책·식물 도형뿐이다).
 */
@Composable
fun DecorativeBackground(modifier: Modifier = Modifier, showBookAndPlantDecoration: Boolean = true) {
    val mint = MaterialTheme.colorScheme.primary
    val navy = NavyTitle

    Canvas(modifier = modifier.fillMaxSize()) {
        val strokeWidth = size.minDimension * 0.16f

        // 좌하단 큰 원호 — PDF 표지 좌측 하단의 굵은 민트 반원.
        drawArc(
            color = mint.copy(alpha = 0.12f),
            startAngle = 90f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = Offset(-size.width * 0.32f, size.height * 0.58f),
            size = Size(size.width * 0.72f, size.width * 0.72f),
            style = Stroke(width = strokeWidth)
        )

        // 우상단 작은 원호 — PDF 표지 우상단의 포인트 곡선.
        drawArc(
            color = mint.copy(alpha = 0.13f),
            startAngle = 270f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = Offset(size.width * 0.78f, -size.height * 0.18f),
            size = Size(size.width * 0.42f, size.width * 0.42f),
            style = Stroke(width = strokeWidth * 0.55f)
        )

        if (showBookAndPlantDecoration) {
            // 좌하단 "책" 더미 — 폭이 다른 둥근 세로 막대 3개, 화면 왼쪽 끝 안쪽에만(기존 대비 약 1.8배).
            val bookColors = listOf(mint.copy(alpha = 0.13f), navy.copy(alpha = 0.09f), mint.copy(alpha = 0.11f))
            val bookWidth = size.width * 0.029f
            val bookGap = size.width * 0.014f
            val bookBaseY = size.height * 0.995f
            val bookHeights = listOf(size.height * 0.29f, size.height * 0.38f, size.height * 0.23f)
            var bookX = size.width * 0.012f
            bookHeights.forEachIndexed { index, bookHeight ->
                drawRoundRect(
                    color = bookColors[index % bookColors.size],
                    topLeft = Offset(bookX, bookBaseY - bookHeight),
                    size = Size(bookWidth, bookHeight),
                    cornerRadius = CornerRadius(bookWidth * 0.3f)
                )
                bookX += bookWidth + bookGap
            }

            // 우상단 "잎" 두 장 — 화면 오른쪽 끝 안쪽, 상단바와 겹치지 않게(기존 대비 약 1.8배).
            val leafColor = mint.copy(alpha = 0.11f)
            rotate(degrees = 30f, pivot = Offset(size.width * 0.965f, size.height * 0.05f)) {
                drawOval(
                    color = leafColor,
                    topLeft = Offset(size.width * 0.905f, size.height * -0.03f),
                    size = Size(size.width * 0.09f, size.height * 0.16f)
                )
            }
            rotate(degrees = -20f, pivot = Offset(size.width * 0.99f, size.height * 0.09f)) {
                drawOval(
                    color = leafColor,
                    topLeft = Offset(size.width * 0.945f, size.height * 0.02f),
                    size = Size(size.width * 0.08f, size.height * 0.145f)
                )
            }
        }
    }
}
