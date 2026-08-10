package com.example.ibtech.ui.facility

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.example.ibtech.R
import com.example.ibtech.domain.model.Facility
import com.example.ibtech.domain.model.FacilityDirection

/**
 * 층·방향 안내 문구. [Facility.direction](관리자가 고른 우측/정면/좌측)이 있으면 우선 쓰고,
 * 레거시 [Facility.directionText](관리자 UI 없이 백업 JSON으로만 들어올 수 있는 자유 텍스트)로
 * 폴백한 뒤, 그마저 없으면 층수만 안내한다.
 */
@Composable
fun facilityLocationGuideText(facility: Facility): String {
    val direction = facility.direction
    val legacyText = facility.directionText
    return when {
        direction != null -> stringResource(
            R.string.facility_location_guide_with_direction,
            facility.floor,
            stringResource(direction.labelRes())
        )
        !legacyText.isNullOrBlank() -> legacyText
        else -> stringResource(R.string.facility_location_guide_no_direction, facility.floor)
    }
}

internal fun FacilityDirection.labelRes(): Int = when (this) {
    FacilityDirection.RIGHT -> R.string.facility_direction_right
    FacilityDirection.FRONT -> R.string.facility_direction_front
    FacilityDirection.LEFT -> R.string.facility_direction_left
}
