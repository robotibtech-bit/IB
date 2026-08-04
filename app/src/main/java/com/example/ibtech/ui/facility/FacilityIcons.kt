package com.example.ibtech.ui.facility

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.filled.ChildCare
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Place
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.ibtech.domain.model.Facility

/**
 * 시설 카드에 쓸 아이콘을 결정한다.
 *
 * [Facility.iconKey]가 설정돼 있으면 그대로 쓴다(10단계 관리자 아이콘 설정 대비 — 지금은
 * 관리자 화면이 없어 항상 null이라 이 분기는 실질적으로 미사용). 없으면 시설명에 포함된
 * 키워드로 최선 추정한다. 실제 Temi POI 이름이 무엇이든, 매핑에 실패하면 기본 위치 아이콘으로
 * 안전하게 폴백하므로 화면이 깨지지 않는다(시설명을 하드코딩하지 않는다는 원칙과 양립).
 */
fun Facility.resolveIcon(): ImageVector = when (iconKey) {
    "child_care" -> Icons.Filled.ChildCare
    "computer" -> Icons.Filled.Computer
    "library_books" -> Icons.AutoMirrored.Filled.LibraryBooks
    "groups" -> Icons.Filled.Groups
    else -> when {
        name.contains("어린이") -> Icons.Filled.ChildCare
        name.contains("디지털") || name.contains("컴퓨터") -> Icons.Filled.Computer
        name.contains("종합") || name.contains("자료실") -> Icons.AutoMirrored.Filled.LibraryBooks
        name.contains("열람") -> Icons.Filled.Groups
        else -> Icons.Filled.Place
    }
}
