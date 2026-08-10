package com.example.ibtech.ui.facility

import android.content.Context
import com.example.ibtech.R
import com.example.ibtech.domain.model.Facility

/**
 * 동행 이동 시작/도착 음성·화면 문구. 기준층 시설은 기존 고정 문구를 그대로 쓰고, 기준층이
 * 아닌 시설은 "로봇은 엘리베이터 POI까지만 동행한다"는 전제로 문구를 바꾼다(요구사항: 타 층
 * 안내 고도화 2차). ViewModel(TTS)과 Composable(화면 표시) 양쪽에서 같은 결과를 쓰도록
 * Context 기반 순수 함수로 둔다.
 */
fun buildNavigationStartText(context: Context, facility: Facility, baseFloor: Int): String =
    if (facility.floor == baseFloor) {
        context.getString(R.string.navigation_start_speech)
    } else {
        context.getString(R.string.navigation_start_speech_other_floor)
    }

/** [sameFloorTextRes]는 기준층일 때 쓸 문구 리소스 — 음성용(`navigation_arrived_speech`)과
 * 화면 표시용(`navigation_arrived_body`)이 문구는 같지만 리소스가 분리되어 있어 호출부에서 고른다. */
fun buildNavigationArrivedText(
    context: Context,
    facility: Facility,
    baseFloor: Int,
    sameFloorTextRes: Int
): String {
    if (facility.floor == baseFloor) return context.getString(sameFloorTextRes)

    val direction = facility.direction
    val goingUp = facility.floor > baseFloor
    return when {
        direction != null && goingUp -> context.getString(
            R.string.navigation_arrived_speech_up_with_direction, facility.floor, context.getString(direction.labelRes())
        )
        direction != null && !goingUp -> context.getString(
            R.string.navigation_arrived_speech_down_with_direction, facility.floor, context.getString(direction.labelRes())
        )
        goingUp -> context.getString(R.string.navigation_arrived_speech_up_no_direction, facility.floor)
        else -> context.getString(R.string.navigation_arrived_speech_down_no_direction, facility.floor)
    }
}
