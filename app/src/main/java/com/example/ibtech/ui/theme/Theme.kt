package com.example.ibtech.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

/*
 * 브랜드 색(민트·청록 + 짙은 남색)을 고정 적용한다.
 * 기기별 Dynamic Color(Android 12+)를 쓰면 도서관마다, 단말기마다 색이 달라져
 * PDF 디자인 원칙(2.3절)이 깨지므로 사용하지 않는다.
 */
private val LibraryLightColorScheme = lightColorScheme(
    primary = MintPrimary,
    onPrimary = SurfaceWhite,
    primaryContainer = MintContainer,
    onPrimaryContainer = NavyTitle,
    secondary = NavyTitle,
    onSecondary = SurfaceWhite,
    secondaryContainer = NavyTitleContainer,
    onSecondaryContainer = NavyTitle,
    tertiary = MintPrimaryDark,
    onTertiary = SurfaceWhite,
    background = SurfaceWhite,
    onBackground = NavyTitle,
    surface = SurfaceWhite,
    onSurface = NavyTitle,
    surfaceVariant = SurfaceMintTint,
    onSurfaceVariant = TextBody,
    outline = OutlineMint,
    outlineVariant = OutlineNeutral,
    error = ErrorRed,
    errorContainer = ErrorContainer,
    onError = SurfaceWhite,
    onErrorContainer = ErrorRed
)

// 키오스크 환경은 상시 밝은 화면을 전제로 하지만, 야간 모드 기기 대비 최소한의 폴백은 둔다.
private val LibraryDarkColorScheme = darkColorScheme(
    primary = MintPrimary,
    secondary = MintPrimaryDark,
    tertiary = NavyTitleContainer
)

@Composable
fun IBTECHTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) LibraryDarkColorScheme else LibraryLightColorScheme,
        typography = LibraryTypography,
        shapes = LibraryShapes,
        content = content
    )
}
