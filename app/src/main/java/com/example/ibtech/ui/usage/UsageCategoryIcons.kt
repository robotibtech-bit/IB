package com.example.ibtech.ui.usage

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.example.ibtech.R
import com.example.ibtech.domain.model.UsageTopic
import com.example.ibtech.ui.theme.LavenderAccent
import com.example.ibtech.ui.theme.LavenderAccentContainer
import com.example.ibtech.ui.theme.MintContainer
import com.example.ibtech.ui.theme.MintPrimary
import com.example.ibtech.ui.theme.SkyAccent
import com.example.ibtech.ui.theme.SkyAccentContainer
import com.example.ibtech.ui.theme.YellowAccent
import com.example.ibtech.ui.theme.YellowAccentContainer

/**
 * 이용방법 1차 메뉴 카드 아이콘. 카테고리 id는 [com.example.ibtech.data.repository.DefaultUsageContent]
 * 가 고정으로 부여하므로(관리자 화면이 생기기 전까지는 카테고리 자체를 새로 만들 수 없음)
 * [com.example.ibtech.ui.facility.FacilityIcons]의 키워드 추정과 달리 id로 직접 매핑한다.
 */
fun UsageTopic.resolveCategoryIcon(): ImageVector = when (id) {
    "category_loan" -> Icons.AutoMirrored.Filled.MenuBook
    "category_membership" -> Icons.Filled.PersonAdd
    "category_reading_materials" -> Icons.AutoMirrored.Filled.LibraryBooks
    "category_hours" -> Icons.Filled.Schedule
    else -> Icons.Filled.Info
}

/**
 * 이용방법 1차 메뉴 카드 강조색 (13단계, 전체 UI 디자인 패키지 3단계).
 *
 * [resolveCategoryIcon]과 같은 이유로 id 기준 매핑이다 — 관리자가 카테고리 자체를 새로
 * 만들 수는 없으므로 4개 고정 id만 다루면 되고, 매핑되지 않은 경우(향후 새 카테고리가 코드로
 * 추가되는 경우 대비)에는 기본 Teal을 쓴다.
 */
fun UsageTopic.resolveCategoryAccent(): Color = when (id) {
    "category_loan" -> SkyAccent
    "category_membership" -> MintPrimary
    "category_reading_materials" -> LavenderAccent
    "category_hours" -> YellowAccent
    else -> MintPrimary
}

/** [resolveCategoryAccent]와 짝을 이루는 옅은 배경색(아이콘 원 등). */
fun UsageTopic.resolveCategoryIconBackground(): Color = when (id) {
    "category_loan" -> SkyAccentContainer
    "category_membership" -> MintContainer
    "category_reading_materials" -> LavenderAccentContainer
    "category_hours" -> YellowAccentContainer
    else -> MintContainer
}

/**
 * 이용방법 1차 메뉴 카드 부제 (13단계). 화면 표시 전용 문구이며 [UsageTopic]에는 저장하지 않는다
 * — 관리자가 실제 답변([UsageTopic.shortAnswer])을 아무리 바꿔도 이 카드 부제는 바뀌지 않는다.
 * id로 매핑되지 않는 카테고리(향후 코드 추가분)는 공통 안내 문구로 대체한다.
 */
@Composable
fun UsageTopic.resolveCategorySubtitle(): String = when (id) {
    "category_loan" -> stringResource(R.string.usage_category_loan_subtitle)
    "category_membership" -> stringResource(R.string.usage_category_membership_subtitle)
    "category_reading_materials" -> stringResource(R.string.usage_category_reading_materials_subtitle)
    "category_hours" -> stringResource(R.string.usage_category_hours_subtitle)
    else -> stringResource(R.string.usage_category_default_subtitle)
}
