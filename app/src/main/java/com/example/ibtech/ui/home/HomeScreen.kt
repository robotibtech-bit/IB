package com.example.ibtech.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Park
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ibtech.R
import com.example.ibtech.ui.common.DecorativeBackground
import com.example.ibtech.ui.common.LibraryCard
import com.example.ibtech.ui.common.debounced
import com.example.ibtech.ui.theme.IBTECHTheme
import com.example.ibtech.ui.theme.LibraryDimens
import com.example.ibtech.ui.theme.MintContainer
import com.example.ibtech.ui.theme.MintPrimary
import com.example.ibtech.ui.theme.SkyAccent
import com.example.ibtech.ui.theme.SkyAccentContainer
import com.example.ibtech.ui.theme.YellowAccent
import com.example.ibtech.ui.theme.YellowAccentContainer

/**
 * 메인 화면 (요구사항 명세서 2.2절, 2026-08-04 정정: 도서관명 "신트리도서관" 고정).
 *
 * 상단바가 없다 — 뒤로가기/홈 자체가 필요 없는 유일한 화면이다.
 * [welcomeMessage]는 관리자 설정(DataStore, [HomeViewModel])에서 읽은 값을 그대로 받는다.
 * 도서관명은 더 이상 설정값이 아니므로 `R.string.library_name`을 직접 참조한다.
 *
 * 13단계(전체 UI 디자인 패키지 3단계)에서 참고 시안(mockup_home.png) 기준으로 Hero 영역과
 * 카드별 강조색·부제·화살표를 추가했다. 콜백 5개와 welcomeMessage 파라미터는 그대로다 —
 * 시각 구조만 바뀌었다. [BoxWithConstraints]로 1024×600처럼 낮은 화면에서는 Hero 높이와
 * 카드 아이콘을 줄여 스크롤 없이 한 화면에 들어오게 한다(글자 크기는 줄이지 않는다).
 */
@Composable
fun HomeScreen(
    welcomeMessage: String,
    onFindFacility: () -> Unit,
    onUsageGuide: () -> Unit,
    onKidsContent: () -> Unit,
    onTodayEvents: () -> Unit,
    onAdminClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val menus = listOf(
        HomeMenu(
            icon = Icons.Filled.Search,
            label = stringResource(R.string.home_action_find_facility),
            subtitle = stringResource(R.string.home_action_find_facility_subtitle),
            accent = SkyAccent,
            iconBackground = SkyAccentContainer,
            onClick = onFindFacility
        ),
        HomeMenu(
            icon = Icons.AutoMirrored.Filled.MenuBook,
            label = stringResource(R.string.home_action_usage_guide),
            subtitle = stringResource(R.string.home_action_usage_guide_subtitle),
            // "이용방법" 대표색은 패키지 문서상 Teal이지만, 이 앱의 기존 대표색(MintPrimary)이
            // 이미 같은 계열의 teal이라 새 강조색을 따로 만들지 않고 그대로 재사용한다.
            accent = MintPrimary,
            iconBackground = MintContainer,
            onClick = onUsageGuide
        ),
        HomeMenu(
            icon = Icons.Filled.SmartToy,
            label = stringResource(R.string.home_action_kids_content),
            subtitle = stringResource(R.string.home_action_kids_content_subtitle),
            accent = YellowAccent,
            iconBackground = YellowAccentContainer,
            onClick = onKidsContent
        )
    )

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val compact = maxHeight < 650.dp
        val heroHeight = if (compact) 168.dp else 208.dp
        val cardIconCircle = if (compact) 72.dp else 92.dp
        val cardIconSize = if (compact) 36.dp else 46.dp
        val heroToCardsSpacing = if (compact) 12.dp else 16.dp
        val cardsToEventSpacing = if (compact) 12.dp else LibraryDimens.CardSpacing
        val eventBarHeight = if (compact) 76.dp else 92.dp

        DecorativeBackground(modifier = Modifier.fillMaxSize())

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(LibraryDimens.ScreenPadding)
        ) {
            HomeHero(
                welcomeMessage = welcomeMessage,
                onAdminClick = onAdminClick,
                compact = compact,
                // 인사말이 2줄(관리자가 자유롭게 설정 가능)일 때 고정 height라면 아래쪽 잎 장식
                // clip() 경계에 잘리는 문제가 있었다 — heroHeight를 최소값으로만 쓰고, 실제
                // 내용이 더 필요하면 Hero가 늘어나게 한다(남는 세로 공간은 카드 Row가 weight로
                // 흡수하므로 전체 레이아웃은 계속 1280×720 안에 들어온다).
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = heroHeight)
            )

            Spacer(modifier = Modifier.height(heroToCardsSpacing))

            // 3대 방문 목적: 남는 세로 공간을 전부 채우는 큰 카드 3개를 가로로 나란히 둔다.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(LibraryDimens.CardSpacing)
            ) {
                menus.forEach { menu ->
                    HomeMenuCard(
                        menu = menu,
                        iconCircleSize = cardIconCircle,
                        iconSize = cardIconSize,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize()
                    )
                }
            }

            Spacer(modifier = Modifier.height(cardsToEventSpacing))

            TodayEventBar(
                text = stringResource(R.string.home_action_today_events),
                onClick = onTodayEvents,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(eventBarHeight)
            )
        }
    }
}

private data class HomeMenu(
    val icon: ImageVector,
    val label: String,
    val subtitle: String,
    val accent: Color,
    val iconBackground: Color,
    val onClick: () -> Unit
)

/**
 * 연한 민트 Hero 영역: 도서관명 + 관리자 잠금 버튼 + 환영 문구.
 * 좌우 모서리에 책·식물 느낌의 낮은 투명도 아이콘 장식을 둔다(이미지 아님, 기존 Material 아이콘).
 */
@Composable
private fun HomeHero(
    welcomeMessage: String,
    onAdminClick: () -> Unit,
    compact: Boolean,
    modifier: Modifier = Modifier
) {
    val logoCircleSize = if (compact) 60.dp else 72.dp
    val logoIconSize = if (compact) 30.dp else 36.dp

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(bottomStart = 34.dp, bottomEnd = 34.dp))
            .background(MaterialTheme.colorScheme.primaryContainer)
    ) {
        // 장식용 아이콘 — 스크린리더에는 노출하지 않고, 텍스트/버튼과 겹치지 않는 좌우 바깥쪽에만.
        Icon(
            imageVector = Icons.AutoMirrored.Filled.MenuBook,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 4.dp)
                .size(if (compact) 64.dp else 84.dp)
                .alpha(0.08f)
        )
        Icon(
            imageVector = Icons.Filled.Park,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 8.dp, bottom = 4.dp)
                .size(if (compact) 56.dp else 76.dp)
                .alpha(0.08f)
        )

        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth(0.86f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(logoCircleSize)
                        .background(MaterialTheme.colorScheme.surface, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Park,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(logoIconSize)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = stringResource(R.string.library_name),
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.height(if (compact) 4.dp else 8.dp))
            Text(
                text = welcomeMessage,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }

        // 관리자 진입점. 비밀번호로 보호되므로(10단계) 이용자가 눌러도 안전하다 —
        // 눈에 띄지 않는 작은 아이콘 버튼으로 우측 상단에 둔다.
        IconButton(
            onClick = onAdminClick,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp)
                .size(LibraryDimens.MinTouchTarget)
        ) {
            Icon(
                imageVector = Icons.Filled.Lock,
                contentDescription = stringResource(R.string.home_action_admin),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** 방문 목적 카드. 왼쪽 강조선(2단계 [LibraryCard.accentColor]) + 원형 아이콘 + 제목 + 부제 + 화살표. */
@Composable
private fun HomeMenuCard(
    menu: HomeMenu,
    iconCircleSize: Dp,
    iconSize: Dp,
    modifier: Modifier = Modifier
) {
    LibraryCard(
        modifier = modifier.clickable(onClick = debounced(menu.onClick)),
        accentColor = menu.accent
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(LibraryDimens.CardPadding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(iconCircleSize)
                        .background(menu.iconBackground, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = menu.icon,
                        contentDescription = null,
                        tint = menu.accent,
                        modifier = Modifier.size(iconSize)
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = menu.label,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(
                    modifier = Modifier.fillMaxWidth(0.55f),
                    color = menu.accent.copy(alpha = 0.28f)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = menu.subtitle,
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 26.sp, lineHeight = 32.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(14.dp)
                    .size(LibraryDimens.ArrowCircle)
                    .background(menu.iconBackground, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = menu.accent
                )
            }
        }
    }
}

// mockup_home.png의 "흰색에서 연한 크림색으로 이어지는 배경"을 근사한 값. Color.kt(2단계)에는
// 아직 이 톤에 맞는 토큰이 없어 이 화면 전용으로만 로컬 정의한다.
private val EventBarCream = Color(0xFFFFFBF1)

/** 오늘의 행사 진입 바. Yellow 계열로 3대 메뉴와 구분한다 — 기존 onClick/debounced 유지. */
@Composable
private fun TodayEventBar(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.clickable(onClick = debounced(onClick)),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = EventBarCream),
        border = BorderStroke(1.5.dp, YellowAccent),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight(0.72f)
                    .aspectRatio(1f)
                    .background(YellowAccentContainer, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Event,
                    contentDescription = null,
                    tint = YellowAccent
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Box(
                modifier = Modifier
                    .fillMaxHeight(0.72f)
                    .aspectRatio(1f)
                    .background(YellowAccent, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.surface
                )
            }
        }
    }
}

@Preview(name = "1280x800", widthDp = 1280, heightDp = 800, showBackground = true)
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

@Preview(name = "1280x720", widthDp = 1280, heightDp = 720, showBackground = true)
@Composable
private fun HomeScreen1280x720Preview() {
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

@Preview(name = "1024x600", widthDp = 1024, heightDp = 600, showBackground = true)
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
