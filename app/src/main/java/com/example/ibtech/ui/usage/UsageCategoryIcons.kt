package com.example.ibtech.ui.usage

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.ibtech.domain.model.UsageTopic

/**
 * 이용방법 1차 메뉴 카드 아이콘. 카테고리 id는 [com.example.ibtech.data.repository.DefaultUsageContent]
 * 가 고정으로 부여하므로(관리자 화면이 생기기 전까지는 카테고리 자체를 새로 만들 수 없음)
 * [com.example.ibtech.ui.facility.FacilityIcons]의 키워드 추정과 달리 id로 직접 매핑한다.
 */
fun UsageTopic.resolveCategoryIcon(): ImageVector = when (id) {
    "category_loan" -> Icons.AutoMirrored.Filled.MenuBook
    "category_membership" -> Icons.Filled.PersonAdd
    "category_reading_room" -> Icons.Filled.Groups
    "category_hours" -> Icons.Filled.Schedule
    else -> Icons.Filled.Info
}
