package com.example.ibtech.ui.theme

import androidx.compose.ui.graphics.Color

/*
 * PDF("테미 도서관") 디자인 원칙 기반 팔레트.
 * 흰색 배경 + 민트·청록 포인트 + 짙은 남색 제목.
 * PDF 이미지에서 픽셀 단위로 추출한 값이 아니라 설명(2.3절)을 따른 근사치이므로,
 * 실제 브랜드 헥스 코드가 확정되면 이 파일만 바꾸면 된다.
 */

// 민트 · 청록 (선택/강조 버튼, 포인트)
val MintPrimary = Color(0xFF14B8A6)
val MintPrimaryDark = Color(0xFF0F9488)
val MintContainer = Color(0xFFCFF3EE)

// 짙은 남색 (제목, 강조 텍스트)
val NavyTitle = Color(0xFF0B2E4E)
val NavyTitleContainer = Color(0xFFE1E9F0)

// 중립 (배경, 카드, 테두리)
val SurfaceWhite = Color(0xFFFFFFFF)
val SurfaceMintTint = Color(0xFFF1F8F7)
val OutlineMint = Color(0xFFB7E4DE)
val OutlineNeutral = Color(0xFFDDE3E2)
val TextBody = Color(0xFF33424F)

// 상태
val ErrorRed = Color(0xFFB3261E)
val ErrorContainer = Color(0xFFFBE9E7)

/*
 * 전체 UI 디자인 패키지(Shintree_Library_Full_UI_Package, 01_DESIGN_SYSTEM.md) 기준 추가 토큰.
 * 위 기존 팔레트(Mint/Navy 등)는 그대로 두고, 화면 영역별 강조색(시설=Sky, 이용방법=Teal는
 * 기존 MintPrimary 재사용, 열람실/퀴즈=Lavender, 로봇놀이/행사/운영시간=Yellow, 휴관/주의=Coral,
 * 완료/정답=Success)만 신규로 얹는다. 2단계(테마·공통 컴포넌트)에서는 토큰만 추가하고 아직 화면에
 * 적용하지 않는다 — 실제 적용은 3단계 이후 화면별 작업에서 이 토큰을 참조한다.
 */
val SkyAccent = Color(0xFF3E9AD8)
val SkyAccentContainer = Color(0xFFE2F2FC)
val LavenderAccent = Color(0xFF8269D8)
val LavenderAccentContainer = Color(0xFFEEE9FC)
val YellowAccent = Color(0xFFF5AA18)
val YellowAccentContainer = Color(0xFFFFF3D2)
val CoralAccent = Color(0xFFF27D72)
val CoralAccentContainer = Color(0xFFFFF0EE)
val SuccessAccent = Color(0xFF32A873)
val SuccessAccentContainer = Color(0xFFE1F5EB)

/**
 * 방문객 화면 전용 배경(01_DESIGN_SYSTEM.md Mint050, 최종 단계 "공통 배경 보완"). [Theme.kt]의
 * 전역 `background`에 적용하고, 관리자 화면은 [com.example.ibtech.navigation.LibraryNavHost]의
 * `AdminTypography` 래퍼에서 다시 [SurfaceWhite]로 되돌려 장식이 강제 적용되지 않게 한다.
 * 카드([LibraryCard])와 상단바([LibraryTopAppBar])는 `colorScheme.surface`(항상 흰색)를 쓰므로
 * 이 배경색과 무관하게 흰색으로 유지된다.
 */
val VisitorBackground = Color(0xFFF4FAF9)
