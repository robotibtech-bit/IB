package com.example.ibtech.ui.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
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
    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(LibraryDimens.TopBarHeight)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            TopBarAction(
                icon = Icons.AutoMirrored.Filled.ArrowBack,
                label = stringResource(R.string.top_bar_back),
                onClick = onBack
            )

            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp)
            )

            TopBarAction(
                icon = Icons.Filled.Home,
                label = stringResource(R.string.top_bar_home),
                onClick = onHome
            )
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
            .heightIn(min = LibraryDimens.MinTouchTarget)
            .widthIn(min = LibraryDimens.MinTouchTarget)
            .clip(RoundedCornerShape(16.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary
        )
    }
}
