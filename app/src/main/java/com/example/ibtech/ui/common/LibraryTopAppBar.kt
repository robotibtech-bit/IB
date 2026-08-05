package com.example.ibtech.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ibtech.R
import com.example.ibtech.ui.theme.LibraryDimens

/**
 * 모든 하위 화면 공통 상단바 (요구사항 3.6절).
 *
 * 왼쪽 뒤로가기 / 가운데 화면 제목 / 오른쪽 홈. 메인 화면([com.example.ibtech.ui.home.HomeScreen])
 * 에는 쓰지 않는다 — 뒤로가기·홈 자체가 필요 없다.
 * 아이콘만으로 구분하지 않고 텍스트를 함께 표시해 고령층도 의미를 바로 알 수 있게 한다(3.8절).
 */
@Composable
fun LibraryTopAppBar(
    title: String,
    onBack: () -> Unit,
    onHome: () -> Unit,
    modifier: Modifier = Modifier
) {
    // 화면 배경이 방문객 화면에서는 민트 틴트(VisitorBackground)라 상단바를 명시적으로 흰색
    // surface로 칠한다 — 안 그러면 뒤에 비치는 배경색 때문에 "상단바는 흰색 유지" 규칙이 깨진다.
    Column(modifier = modifier.background(MaterialTheme.colorScheme.surface)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(LibraryDimens.TopBarHeight)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // 좌우 버튼을 자연 크기(SpaceBetween)로 두면 "뒤로"(2글자)와 "홈"(1글자)의 폭이
            // 달라 제목이 실제 화면 중앙에서 벗어난다 — 양쪽을 동일한 weight(1f) 영역으로 감싸
            // 버튼은 각 영역의 바깥쪽 끝에 붙이고, 제목이 남는 공간의 정중앙에 오게 한다.
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                TopBarAction(
                    icon = Icons.AutoMirrored.Filled.ArrowBack,
                    label = stringResource(R.string.top_bar_back),
                    onClick = onBack
                )
            }

            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(3f)
                    .padding(horizontal = 8.dp)
            )

            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
                TopBarAction(
                    icon = Icons.Filled.Home,
                    label = stringResource(R.string.top_bar_home),
                    onClick = onHome
                )
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}

@Composable
private fun TopBarAction(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = debounced(onClick)
            )
            .heightIn(min = LibraryDimens.TopBarActionSize)
            .widthIn(min = LibraryDimens.TopBarActionSize)
            .clip(RoundedCornerShape(16.dp))
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(LibraryDimens.TopBarActionIconSize)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge.copy(fontSize = 58.sp),
            color = MaterialTheme.colorScheme.primary
        )
    }
}
