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
