package com.example.ibtech.ui.facility

import android.content.Context
import com.example.ibtech.R
import com.example.ibtech.domain.model.BasementStairsWayfindingOverride
import com.example.ibtech.domain.model.Facility
import com.example.ibtech.domain.model.FacilityDirection
import com.example.ibtech.domain.model.StairsWayfindingOverride
import com.example.ibtech.domain.model.WayfindingCorridorOverride

/**
 * 층·방향 안내 문구. 화면 표시(시설 상세·위치만 보기)와 로봇 음성(동행 도착)이 완전히 같은
 * 문장을 쓰도록 이 함수 하나로 통일한다 — Context 기반 순수 함수라 Composable(`LocalContext.current`)
 * 과 ViewModel(TTS) 양쪽에서 그대로 쓸 수 있다.
 *
 * 기준층과 같으면 엘리베이터를 언급할 이유가 없으므로 층수만 안내한다. 기준층이 아니면
 * [Facility.direction](관리자가 고른 우측/정면/좌측, 기준층 시설은 저장 시점에 항상 null로
 * 비워진다)이 있는지에 따라 "엘리베이터를 이용해 N층으로 올라가서/내려가서 [방향]으로
 * 이동하시면 있습니다" 형태의 문장을 만든다.
 */
fun buildFloorDirectionGuideText(context: Context, facility: Facility, baseFloor: Int): String {
    if (facility.floor == baseFloor) {
        // "연결통로를 통해 …" 문구는 연결통로 안내도 대상 시설([WayfindingCorridorOverride])에만
        // 맞는 말이다 — 신규 POI는 기준층이어도 방향이 기본값(정면)으로 채워져 들어오므로
        // (SyncPoiUseCase), direction != null만으로 판단하면 그냥 로봇이 문 앞까지 바로 데려다
        // 주는 일반 기준층 시설에도 잘못된 "연결통로를 통해" 문구가 나간다.
        val direction = facility.direction
        return if (direction != null && WayfindingCorridorOverride.appliesTo(facility.id)) {
            context.getString(R.string.location_guide_same_floor_with_direction, context.getString(direction.labelRes()))
        } else {
            context.getString(R.string.facility_detail_floor_line, facility.floor)
        }
    }

    // 여름강의실 옆 계단으로만 갈 수 있는 시설(StairsWayfindingOverride)은 일반 "엘리베이터를
    // 이용해 …" 안내 대신 그 계단 위치를 짚어 안내한다 — 그 외에는 location_guide_up_*과 같은
    // 짜임으로 층·방향을 그대로 안내한다.
    if (StairsWayfindingOverride.appliesTo(facility.id)) {
        val direction = facility.direction
        return if (direction != null) {
            context.getString(R.string.stairs_wayfinding_guide_body, facility.floor, context.getString(direction.labelRes()))
        } else {
            context.getString(R.string.stairs_wayfinding_guide_body_no_direction, facility.floor)
        }
    }

    // 지하 1층 계단으로만 갈 수 있는 시설(BasementStairsWayfindingOverride)도 위와 같은 방식이되
    // 내려가는 문구를 쓴다 — "엘리베이터를 이용해 …" 문구는 지상 층 기준이라 지하에는 안 맞는다.
    if (BasementStairsWayfindingOverride.appliesTo(facility.id)) {
        val direction = facility.direction
        return if (direction != null) {
            context.getString(R.string.stairs_wayfinding_guide_body_down, context.getString(direction.labelRes()))
        } else {
            context.getString(R.string.stairs_wayfinding_guide_body_down_no_direction)
        }
    }

    val direction = facility.direction
    val goingUp = facility.floor > baseFloor
    return when {
        direction != null && goingUp -> context.getString(
            R.string.location_guide_up_with_direction, facility.floor, context.getString(direction.labelRes())
        )
        direction != null && !goingUp -> context.getString(
            R.string.location_guide_down_with_direction, facility.floor, context.getString(direction.labelRes())
        )
        goingUp -> context.getString(R.string.location_guide_up_no_direction, facility.floor)
        else -> context.getString(R.string.location_guide_down_no_direction, facility.floor)
    }
}

/**
 * 시설 카드·관리자 목록에 보이는 층수 배지 문구. 지하 시설(관리자가 [Facility.floor]를 음수로
 * 등록, 예: 지하 1층 = -1)은 "N층"이 아니라 "지하 N층"으로 보여준다
 * ([BasementStairsWayfindingOverride]).
 */
fun formatFloorBadge(context: Context, floor: Int): String =
    if (floor < 0) {
        context.getString(R.string.facility_card_floor_format_basement, -floor)
    } else {
        context.getString(R.string.facility_card_floor_format, floor)
    }

internal fun FacilityDirection.labelRes(): Int = when (this) {
    FacilityDirection.RIGHT -> R.string.facility_direction_right
    FacilityDirection.FRONT -> R.string.facility_direction_front
    FacilityDirection.LEFT -> R.string.facility_direction_left
}
