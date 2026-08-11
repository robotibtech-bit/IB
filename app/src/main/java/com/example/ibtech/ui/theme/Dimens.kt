package com.example.ibtech.ui.theme

import androidx.compose.ui.unit.dp

/**
 * 화면 치수 공통값 (요구사항 3.8절: 13.3인치 키오스크, 최소 터치 64dp).
 * 방문객이 로봇 앞에 서서 조작하므로 일반 모바일 터치 최소 크기(48dp)보다 크게 잡는다.
 *
 * 12단계에서 참고 프로젝트(temi 실기에서 "몇 걸음 떨어져서도 읽힌다"를 기준으로 만든 다른
 * temi 도서관 앱 프로토타입)와 대조해 전반적으로 키웠다 — 우리 쪽 초기값은 요구사항의
 * "최소" 기준(64dp)에 맞춘 값이었고, 참고 프로젝트는 실사용 거리 기준으로 더 크게 잡았다.
 */
object LibraryDimens {
    val MinTouchTarget = 64.dp
    val PrimaryButtonHeight = 100.dp
    val SecondaryButtonHeight = 88.dp
    val ScreenPadding = 32.dp
    val CardPadding = 28.dp
    val CardSpacing = 28.dp
    /** 상단바 안쪽 콘텐츠(버튼 90dp/제목 headlineSmall 54sp)에 맞춘 높이 — 이전 220dp는 글자
     * 크기를 15% 낮추기 전 기준이라 위아래 여백이 과하게 남았다(사용자 피드백). */
    val TopBarHeight = 140.dp

    /**
     * 목적지/메뉴 카드의 원형 아이콘 배경 지름과 아이콘 크기.
     *
     * 사용자 요청으로 아이콘을 3배(48→144dp)로 키웠다. 배경 원은 원래 지름:아이콘 = 2:1
     * 비율(288dp)로 같이 키웠더니 2x2 격자(이용방법 등)에서 카드 한 칸 높이보다 원이 커져
     * 라벨 글자가 카드 밖으로 밀려나 잘렸다. 208dp(여백 32dp)로 줄여도 2행 격자에서는 여전히
     * 부족해, 실기에서 확인하며 여백 12dp 수준(168dp)까지 더 줄였다. 아이콘 자체 크기는
     * 3배를 그대로 지킨다.
     */
    val LargeIconCircle = 168.dp
    val LargeIconSize = 144.dp

    /** 상단바 뒤로가기/홈 버튼 (기존 MinTouchTarget 64dp 대비 약 40% 확대 — 사용자 요청).
     * 아이콘은 같은 줄의 글자 크기(68sp)와 맞춰 68dp로 키웠다. */
    val TopBarActionSize = 90.dp
    val TopBarActionIconSize = 68.dp

    /**
     * 전체 UI 디자인 패키지 기준 신규 치수(2단계). 기존 값은 하나도 바꾸지 않고 추가만 한다 —
     * 아래 값은 이번 단계에서는 어디에도 아직 쓰이지 않고, 카드 강조선 등은 3단계 이후 화면
     * 작업에서 참조한다.
     */
    val CardAccentWidth = 7.dp
    val ListIconCircle = 82.dp
    val ListIcon = 46.dp
    val ArrowCircle = 46.dp
}
