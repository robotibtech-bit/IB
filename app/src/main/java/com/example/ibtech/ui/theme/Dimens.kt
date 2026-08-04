package com.example.ibtech.ui.theme

import androidx.compose.ui.unit.dp

/**
 * 화면 치수 공통값 (요구사항 3.8절: 13.3인치 키오스크, 최소 터치 64dp).
 * 방문객이 로봇 앞에 서서 조작하므로 일반 모바일 터치 최소 크기(48dp)보다 크게 잡는다.
 */
object LibraryDimens {
    val MinTouchTarget = 64.dp
    val PrimaryButtonHeight = 88.dp
    val SecondaryButtonHeight = 72.dp
    val ScreenPadding = 24.dp
    val CardPadding = 24.dp
    val CardSpacing = 20.dp
    val TopBarHeight = 72.dp
}
