package com.example.ibtech.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Park
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.ibtech.R
import com.example.ibtech.ui.common.DecorativeBackground
import com.example.ibtech.ui.common.LibraryOutlinedButton
import com.example.ibtech.ui.common.LibraryPrimaryButton
import com.example.ibtech.ui.theme.IBTECHTheme
import com.example.ibtech.ui.theme.LibraryDimens

/**
 * 메인 화면 (요구사항 명세서 2.2절, 2026-08-04 정정: 도서관명 "신트리도서관" 고정).
 *
 * 상단바가 없다 — 뒤로가기/홈 자체가 필요 없는 유일한 화면이다.
 * [welcomeMessage]는 관리자 설정(DataStore, [HomeViewModel])에서 읽은 값을 그대로 받는다.
 * 도서관명은 더 이상 설정값이 아니므로 `R.string.library_name`을 직접 참조한다.
 */
@Composable
fun HomeScreen(
    welcomeMessage: String,
    onFindFacility: () -> Unit,
    onUsageGuide: () -> Unit,
    onKidsContent: () -> Unit,
    onTodayEvents: () -> Unit,
    onAdminClick: () -> Unit,
    modifier: Modifier = Modifier,
    onDevMenuClick: (() -> Unit)? = null
) {
    Box(modifier = modifier.fillMaxSize()) {
        DecorativeBackground(modifier = Modifier.fillMaxSize())

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(LibraryDimens.ScreenPadding),
            verticalArrangement = Arrangement.spacedBy(LibraryDimens.CardSpacing)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Park,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = stringResource(R.string.library_name),
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            Text(
                text = welcomeMessage,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            LibraryPrimaryButton(
                text = stringResource(R.string.home_action_find_facility),
                icon = Icons.Filled.Search,
                onClick = onFindFacility
            )
            LibraryPrimaryButton(
                text = stringResource(R.string.home_action_usage_guide),
                icon = Icons.AutoMirrored.Filled.MenuBook,
                onClick = onUsageGuide
            )
            LibraryPrimaryButton(
                text = stringResource(R.string.home_action_kids_content),
                icon = Icons.Filled.SmartToy,
                onClick = onKidsContent
            )
            // PDF 목업에서 "오늘의 행사"는 3단계 핵심 흐름(시설/이용방법/어린이 콘텐츠)과 달리
            // 보조 진입점으로 구분되어 있어(2쪽 UI FLOW) 테두리 버튼으로 구분한다.
            LibraryOutlinedButton(
                text = stringResource(R.string.home_action_today_events),
                icon = Icons.Filled.Event,
                onClick = onTodayEvents
            )

            // 관리자 진입점(왼쪽, release 포함)과 개발자 메뉴 진입점(오른쪽, BuildConfig.DEBUG
            // 전용 — 로드맵 5단계 "개발자 메뉴에서 재현 가능")을 한 줄에 둔다. 눈에 띄지 않게
            // 작은 텍스트 버튼으로 두되, 비밀번호로 보호되므로(10단계) 이용자가 눌러도 안전하다.
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onAdminClick) {
                    Icon(
                        imageVector = Icons.Filled.Lock,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = stringResource(R.string.home_action_admin))
                }
                if (onDevMenuClick != null) {
                    TextButton(onClick = onDevMenuClick) {
                        Text(text = stringResource(R.string.dev_menu_entry))
                    }
                }
            }
        }
    }
}

@Preview(name = "13.3인치 가로", widthDp = 1280, heightDp = 800, showBackground = true)
@Composable
private fun HomeScreenLargePreview() {
    IBTECHTheme {
        HomeScreen(
            welcomeMessage = "안녕하세요\n무엇을 도와드릴까요?",
            onFindFacility = {},
            onUsageGuide = {},
            onKidsContent = {},
            onTodayEvents = {},
            onAdminClick = {}
        )
    }
}

@Preview(name = "소형 화면 폴백", widthDp = 360, heightDp = 800, showBackground = true)
@Composable
private fun HomeScreenSmallPreview() {
    IBTECHTheme {
        HomeScreen(
            welcomeMessage = "안녕하세요\n무엇을 도와드릴까요?",
            onFindFacility = {},
            onUsageGuide = {},
            onKidsContent = {},
            onTodayEvents = {},
            onAdminClick = {}
        )
    }
}
