package com.example.ibtech.ui.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.example.ibtech.ui.theme.LibraryDimens

/**
 * PDF 2.3절 버튼 규칙: 선택(주요) 버튼은 민트색 채움, 일반 버튼은 흰 바탕 + 민트/연회색 테두리.
 * 모든 방문 목적·메뉴 버튼은 이 두 컴포넌트만 사용해 시각적 무게를 통일한다.
 *
 * [icon]을 지정하면 PDF 목업처럼 아이콘 + 좌측 정렬 텍스트로, 지정하지 않으면 기존처럼
 * 텍스트만 가운데 정렬로 그린다 — 기존 호출부는 수정 없이 그대로 동작한다.
 */
@Composable
fun LibraryPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null
) {
    Button(
        onClick = debounced(onClick),
        enabled = enabled,
        shape = MaterialTheme.shapes.medium,
        modifier = modifier
            .fillMaxWidth()
            .height(LibraryDimens.PrimaryButtonHeight)
    ) {
        ButtonContent(text = text, icon = icon, textStyle = MaterialTheme.typography.titleLarge)
    }
}

@Composable
fun LibraryOutlinedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null
) {
    OutlinedButton(
        onClick = debounced(onClick),
        enabled = enabled,
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(
            width = 2.dp,
            color = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
        ),
        modifier = modifier
            .fillMaxWidth()
            .height(LibraryDimens.SecondaryButtonHeight)
    ) {
        ButtonContent(text = text, icon = icon, textStyle = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun ButtonContent(
    text: String,
    icon: ImageVector?,
    textStyle: TextStyle
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = if (icon != null) Arrangement.Start else Arrangement.Center
    ) {
        if (icon != null) {
            Icon(imageVector = icon, contentDescription = null)
            Spacer(modifier = Modifier.width(16.dp))
        }
        Text(text = text, style = textStyle)
    }
}

/**
 * PDF 목업의 로봇 말풍선을 재해석한 안내 카드(시설 상세/위치 안내 화면 공용).
 * 민트 틴트 배경 위에 로봇 아이콘과 안내 문구를 나란히 둔다.
 */
@Composable
fun RobotSpeechBubble(
    text: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(LibraryDimens.CardPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(MaterialTheme.colorScheme.surface, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.SmartToy,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

/** 둥근 모서리 흰 카드. 시설/이용방법/어린이 콘텐츠 등 모든 목록 카드가 공유한다. */
@Composable
fun LibraryCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        content()
    }
}
