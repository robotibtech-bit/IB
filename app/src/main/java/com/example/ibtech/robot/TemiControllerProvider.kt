package com.example.ibtech.robot

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * [TemiController] 구현체 선택 지점 (로드맵 5/6단계 "Fake/Real 구현 전환").
 *
 * ViewModel은 `TemiRepository.getInstance()`를 직접 부르지 않고 이 객체의 [current]만
 * 참조한다. [useFake]는 개발자 메뉴(`ui/dev/DevMenuScreen`, `BuildConfig.DEBUG` 전용)에서만
 * 바꿀 수 있고, 영구 저장하지 않는다 — 앱을 재시작하면 항상 실제 로봇(`TemiRepository`)으로
 * 돌아온다.
 *
 * `mutableStateOf`로 두는 이유: [LibraryNavHost][com.example.ibtech.navigation.LibraryNavHost]의
 * 무입력 복귀 관찰자처럼 앱 시작 시 한 번만 composable로 읽는 곳도 있다 — 일반 `var`면 개발자
 * 메뉴에서 토글해도 그 화면이 재구성되지 않아 옛 컨트롤러를 계속 관찰하게 된다.
 */
object TemiControllerProvider {

    var useFake: Boolean by mutableStateOf(false)

    /** 켜져 있는 동안 상태를 유지해야 하므로 하나만 만들어 재사용한다. */
    val fake: FakeTemiController by lazy { FakeTemiController() }

    val current: TemiController
        get() = if (useFake) fake else TemiRepository.getInstance()
}
