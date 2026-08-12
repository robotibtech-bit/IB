package com.example.ibtech.ui.facility

import android.content.Context
import com.example.ibtech.R
import com.example.ibtech.domain.model.Facility
import com.example.ibtech.domain.model.WayfindingCorridorOverride

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
 * 화면 표시용(`navigation_arrived_body`)이 문구는 같지만 리소스가 분리되어 있어 호출부에서 고른다.
 * 기준층이 아니면 [buildFloorDirectionGuideText]와 완전히 같은 문장을 쓴다 — "위치만 보기"
 * 화면에 표시되는 안내와 로봇이 도착해서 말하는 안내가 서로 달라 보이지 않도록 하기 위함이다. */
fun buildNavigationArrivedText(
    context: Context,
    facility: Facility,
    baseFloor: Int,
    sameFloorTextRes: Int
): String {
    // 연결통로까지만 동행하는 시설(WayfindingCorridorOverride)은 실제 문 앞이 아니라서 기본
    // 층 안내 문구는 맞지 않는다 — 방향이 설정돼 있으면(요구사항: "기준층이 아닌 목적지와
    // 동일하게, 엘리베이터 대신 연결통로로 안내") [buildFloorDirectionGuideText]와 완전히 같은
    // 문장("연결통로를 통해 …")을 쓰고, 아직 방향이 없으면 안내도를 보라는 문구로 대신한다.
    if (WayfindingCorridorOverride.appliesTo(facility.id)) {
        return if (facility.direction != null) {
            buildFloorDirectionGuideText(context, facility, baseFloor)
        } else {
            context.getString(R.string.navigation_arrived_wayfinding_body)
        }
    }
    if (facility.floor == baseFloor) return context.getString(sameFloorTextRes)
    return buildFloorDirectionGuideText(context, facility, baseFloor)
}
