package com.example.ibtech.ui.seatstatus

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.EventSeat
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ibtech.R
import com.example.ibtech.ui.common.DecorativeBackground
import com.example.ibtech.ui.common.FillSpaceGrid
import com.example.ibtech.ui.common.LibraryCard
import com.example.ibtech.ui.common.debounced
import com.example.ibtech.ui.theme.IBTECHTheme
import com.example.ibtech.ui.theme.LavenderAccent
import com.example.ibtech.ui.theme.LavenderAccentContainer
import com.example.ibtech.ui.theme.LibraryDimens
import com.example.ibtech.ui.theme.YellowAccent
import com.example.ibtech.ui.theme.YellowAccentContainer

private val SeatStatusCardIconCircle = 108.dp
private val SeatStatusCardIcon = 56.dp

/**
 * "실시간 좌석 · 예약현황" 메뉴 화면(홈 → 실시간 좌석 · 예약현황).
 *
 * 카드 2개만 있으므로 [FillSpaceGrid]를 1열로 써서 위아래로 큼직하게 채운다. 시각 스타일은
 * 이용방법 1차 메뉴([com.example.ibtech.ui.usage.UsageCategoryScreen])의 카드와 동일한 형태
 * (왼쪽 원형 아이콘 + 제목/부제, 우하단 화살표)를 그대로 재사용해 새 디자인 언어를 만들지
 * 않는다.
 *
 * 현재는 "현황 보기"(카드 전체 클릭 → 바로 웹뷰)만 제공한다. 추후 "예약하러 가기"가 추가되면
 * 카드를 [SeatStatusMenuItem] 단위로 유지한 채, 카드 하단에 보조 버튼 한 줄만 더 넣으면 되므로
 * 지금 구조를 바꿀 필요는 없다 — 실제 예약 URL이 없어 이번 단계에서는 만들지 않는다.
 */
@Composable
fun SeatStatusMenuScreen(
    onDigitalRoomStatus: () -> Unit,
    onReadingRoomSeatStatus: () -> Unit,
    modifier: Modifier = Modifier
) {
    val items = listOf(
        SeatStatusMenuItem(
            icon = Icons.Filled.Computer,
            title = stringResource(R.string.seat_status_digital_room_title),
            subtitle = stringResource(R.string.seat_status_digital_room_subtitle),
            accent = LavenderAccent,
            iconBackground = LavenderAccentContainer,
            onClick = onDigitalRoomStatus
        ),
        SeatStatusMenuItem(
            icon = Icons.Filled.EventSeat,
            title = stringResource(R.string.seat_status_reading_room_title),
            subtitle = stringResource(R.string.seat_status_reading_room_subtitle),
            accent = YellowAccent,
            iconBackground = YellowAccentContainer,
            onClick = onReadingRoomSeatStatus
        )
    )

    Box(modifier = modifier.fillMaxSize()) {
        DecorativeBackground(modifier = Modifier.fillMaxSize())

        FillSpaceGrid(
            items = items,
            columns = 1,
            modifier = Modifier
                .fillMaxSize()
                .padding(LibraryDimens.ScreenPadding)
        ) { item, cardModifier ->
            SeatStatusMenuCard(item = item, modifier = cardModifier)
        }
    }
}

private data class SeatStatusMenuItem(
    val icon: ImageVector,
    val title: String,
    val subtitle: String,
    val accent: Color,
    val iconBackground: Color,
    val onClick: () -> Unit
)

@Composable
private fun SeatStatusMenuCard(item: SeatStatusMenuItem, modifier: Modifier = Modifier) {
    LibraryCard(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = debounced(item.onClick)),
        accentColor = item.accent,
        fillHeight = true
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(LibraryDimens.CardPadding),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(SeatStatusCardIconCircle)
                        .background(color = item.iconBackground, shape = CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = null,
                        tint = item.accent,
                        modifier = Modifier.size(SeatStatusCardIcon)
                    )
                }
                Spacer(modifier = Modifier.width(20.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = item.subtitle,
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 26.sp, lineHeight = 32.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(14.dp)
                    .size(LibraryDimens.ArrowCircle)
                    .background(item.iconBackground, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = item.accent
                )
            }
        }
    }
}

@Preview(name = "실시간 좌석 · 예약현황 · 1280x720", widthDp = 1280, heightDp = 720, showBackground = true)
@Composable
private fun SeatStatusMenuScreenPreview() {
    IBTECHTheme {
        SeatStatusMenuScreen(onDigitalRoomStatus = {}, onReadingRoomSeatStatus = {})
    }
}
