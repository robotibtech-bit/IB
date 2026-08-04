package com.example.ibtech.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavHostController
import com.example.ibtech.navigation.LibraryRoutes
import kotlinx.coroutines.delay

/**
 * 무입력 자동 홈 복귀 (요구사항 4.2절).
 *
 * 홈 화면 자체에는 적용하지 않는다(이미 홈이므로). 로봇 이동 중이거나 충전 중이면 복귀하지
 * 않는다 — 두 값이 바뀌면 이 effect가 다시 시작되며 타이머가 그 시점부터 새로 시작된다.
 * [interactionTick]은 사용자가 화면을 터치할 때마다 증가하는 값으로, 매 터치마다 effect를
 * 취소·재시작시켜 카운트다운을 초기화하는 용도로만 쓴다.
 */
@Composable
fun IdleTimeoutObserver(
    navController: NavHostController,
    currentRoute: String?,
    idleTimeoutSeconds: Int,
    isRobotBusy: Boolean,
    isCharging: Boolean,
    interactionTick: Int
) {
    LaunchedEffect(currentRoute, isRobotBusy, isCharging, interactionTick, idleTimeoutSeconds) {
        if (currentRoute == null || currentRoute == LibraryRoutes.HOME) return@LaunchedEffect
        if (isRobotBusy || isCharging) return@LaunchedEffect

        delay(idleTimeoutSeconds * 1000L)

        navController.navigate(LibraryRoutes.HOME) {
            popUpTo(LibraryRoutes.HOME) { inclusive = true }
            launchSingleTop = true
        }
    }
}
