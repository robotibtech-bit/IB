package com.example.ibtech.ui.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.ibtech.R
import com.example.ibtech.ui.common.DecorativeBackground
import com.example.ibtech.ui.common.LibraryPrimaryButton
import com.example.ibtech.ui.theme.LibraryDimens

/** 관리자 대시보드 (로드맵 5장 Admin 하위 라우트 진입점, 10단계). */
@Composable
fun AdminHomeScreen(
    onFacilityAdmin: () -> Unit,
    onUsageInfoAdmin: () -> Unit,
    onKidsContentAdmin: () -> Unit,
    onEventAdmin: () -> Unit,
    onSettingsAdmin: () -> Unit,
    onStatistics: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        DecorativeBackground(modifier = Modifier.fillMaxSize())

        Column(
            modifier = Modifier
                .fillMaxSize()
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
        }
    }
}
