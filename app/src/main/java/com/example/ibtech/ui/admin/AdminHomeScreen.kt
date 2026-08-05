package com.example.ibtech.ui.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.ibtech.R
import com.example.ibtech.ui.common.DecorativeBackground
import com.example.ibtech.ui.common.LibraryOutlinedButton
import com.example.ibtech.ui.common.LibraryPrimaryButton
import com.example.ibtech.ui.theme.LibraryDimens

/**
 * 관리자 대시보드 (로드맵 5장 Admin 하위 라우트 진입점, 10단계).
 *
 * 글자·버튼이 커지면서(12단계) 메뉴 6~7개가 한 화면 높이를 넘어설 수 있어 세로 스크롤을 둔다.
 * 개발자 메뉴는 원래 홈 화면 하단에 있었지만, 홈 화면 공간을 3대 메뉴 카드에 더 쓰기 위해
 * 이 화면으로 옮겼다(`BuildConfig.DEBUG` 전용은 그대로 유지).
 */
@Composable
fun AdminHomeScreen(
    onFacilityAdmin: () -> Unit,
    onUsageInfoAdmin: () -> Unit,
    onKidsContentAdmin: () -> Unit,
    onEventAdmin: () -> Unit,
    onSettingsAdmin: () -> Unit,
    onStatistics: () -> Unit,
    modifier: Modifier = Modifier,
    onDevMenuClick: (() -> Unit)? = null
) {
    Box(modifier = modifier.fillMaxSize()) {
        DecorativeBackground(modifier = Modifier.fillMaxSize())

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(LibraryDimens.ScreenPadding),
            verticalArrangement = Arrangement.spacedBy(LibraryDimens.CardSpacing)
        ) {
            LibraryPrimaryButton(
                text = stringResource(R.string.admin_menu_facility),
                icon = Icons.Filled.Place,
                onClick = onFacilityAdmin
            )
            LibraryPrimaryButton(
                text = stringResource(R.string.admin_menu_usage_info),
                icon = Icons.AutoMirrored.Filled.MenuBook,
                onClick = onUsageInfoAdmin
            )
            LibraryPrimaryButton(
                text = stringResource(R.string.admin_menu_kids_content),
                icon = Icons.Filled.SmartToy,
                onClick = onKidsContentAdmin
            )
            LibraryPrimaryButton(
                text = stringResource(R.string.admin_menu_event),
                icon = Icons.Filled.Event,
                onClick = onEventAdmin
            )
            LibraryPrimaryButton(
                text = stringResource(R.string.admin_menu_settings),
                icon = Icons.Filled.Settings,
                onClick = onSettingsAdmin
            )
            LibraryPrimaryButton(
                text = stringResource(R.string.admin_menu_statistics),
                icon = Icons.Filled.BarChart,
                onClick = onStatistics
            )
            if (onDevMenuClick != null) {
                LibraryOutlinedButton(
                    text = stringResource(R.string.dev_menu_entry),
                    icon = Icons.Filled.BugReport,
                    onClick = onDevMenuClick
                )
            }
        }
    }
}
