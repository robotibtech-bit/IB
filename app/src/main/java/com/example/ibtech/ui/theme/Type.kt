package com.example.ibtech.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * 요구사항 3.8절 기준(본문 22sp 이상, 주요 버튼 26sp 이상)을 크게 넘어서는 확대 타이포그래피.
 * 어린이·고령층이 한두 걸음 떨어져서도 읽을 수 있어야 한다.
 *
 * 12단계에서 참고 프로젝트(실사용 거리 기준으로 만든 다른 temi 도서관 앱 프로토타입,
 * headlineLarge 48sp/titleLarge 34sp/bodyLarge 26sp)와 대조해 한 차례 키웠고, 이후 사용자
 * 요청으로 2배로 키웠다가, 카드 레이아웃에서 잘리는 문제가 있어 다시 15% 낮췄다 — 역할별
 * 상대 크기 차이는 유지한 채 전체를 위아래로 옮기는 방식은 그대로다.
 */
val LibraryTypography = Typography(
    headlineLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 75.sp,
        lineHeight = 88.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 65.sp,
        lineHeight = 78.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 54.sp,
        lineHeight = 68.sp
    ),
    titleLarge = TextStyle(
        // 주요 버튼 라벨.
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 54.sp,
        lineHeight = 68.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 48.sp,
        lineHeight = 58.sp
    ),
    bodyLarge = TextStyle(
        // 본문 기본값.
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 44.sp,
        lineHeight = 58.sp,
        letterSpacing = 0.3.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 41.sp,
        lineHeight = 54.sp
    ),
    labelLarge = TextStyle(
        // 뒤로가기/홈 등 상단바 텍스트.
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 41.sp,
        lineHeight = 48.sp
    )
)

/**
 * 관리자 화면 전용 타이포그래피 (12단계, 사용자 요청).
 *
 * 관리자 화면은 이용자가 몇 걸음 떨어져서 보는 키오스크 화면이 아니라 직원이 가까이서
 * 조작하는 화면이라 [LibraryTypography]만큼 키울 필요가 없다 — 요구사항 3.8절 최소 기준
 * (본문 22sp 이상, 주요 버튼 26sp 이상)에 맞춘 원래 크기로 되돌린다. 상단바(뒤로/홈/제목)는
 * `LibraryScaffold`가 이 테마 바깥에서 그리므로 영향을 받지 않고 그대로 크게 유지된다.
 */
val AdminTypography = Typography(
    headlineLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 34.sp,
        lineHeight = 42.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 30.sp,
        lineHeight = 38.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 26.sp,
        lineHeight = 34.sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 26.sp,
        lineHeight = 32.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 30.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 22.sp,
        lineHeight = 30.sp,
        letterSpacing = 0.3.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 20.sp,
        lineHeight = 28.sp
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 24.sp
    )
)
