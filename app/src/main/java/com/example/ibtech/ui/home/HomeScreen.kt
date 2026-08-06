package com.example.ibtech.ui.home

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.filled.Lock
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
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
 * 14단계(Shintree_Android_Image_Assets 적용)에서 Hero 배경과 로고·카드·행사 바 아이콘을
 * `drawable-nodpi`의 실제 PNG 그래픽으로 교체했다 — 이전엔 Material 아이콘/Canvas 도형으로
 * 근사했다. 콜백 5개와 welcomeMessage 파라미터, [BoxWithConstraints] 기반 반응형 크기 조절은
 * 그대로다. PNG는 다색 일러스트라 tint를 걸지 않는다(README "PNG 자체에 색상 tint를 적용하지
 * 않는다") — 카드 원형 배경·강조선·화살표 색은 이전처럼 Compose에서 계속 관리한다.
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
            iconRes = R.drawable.ic_home_facility_search,
            label = stringResource(R.string.home_action_find_facility),
            subtitle = stringResource(R.string.home_action_find_facility_subtitle),
            accent = SkyAccent,
            iconBackground = SkyAccentContainer,
            onClick = onFindFacility
        ),
        HomeMenu(
            iconRes = R.drawable.ic_home_usage_book,
            label = stringResource(R.string.home_action_usage_guide),
            subtitle = stringResource(R.string.home_action_usage_guide_subtitle),
            // "이용방법" 대표색은 패키지 문서상 Teal이지만, 이 앱의 기존 대표색(MintPrimary)이
            // 이미 같은 계열의 teal이라 새 강조색을 따로 만들지 않고 그대로 재사용한다.
            accent = MintPrimary,
            iconBackground = MintContainer,
            onClick = onUsageGuide
        ),
        HomeMenu(
            iconRes = R.drawable.ic_home_robot_play,
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
        // 목표 시안(target_home_mockup.png)의 큰 원형 아이콘 배경(120~145dp)에 맞춰 확대했다.
        // 카드 아이콘을 더 키워달라는 요청에 따라 한 단계 더 키움 — CardPadding·제목·구분선·
        // 부제가 남는 세로 공간에서 잘리지 않는지 실기로 재확인 필요.
        val cardIconCircle = if (compact) 128.dp else 156.dp
        val cardIconSize = if (compact) 92.dp else 112.dp
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
                // 인사말이 2줄(관리자가 자유롭게 설정 가능)일 때 고정 height라면 아래쪽 그래픽
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
    @DrawableRes val iconRes: Int,
    val label: String,
    val subtitle: String,
    val accent: Color,
    val iconBackground: Color,
    val onClick: () -> Unit
)

/**
 * 연한 민트 Hero 영역: 도서관명 + 관리자 잠금 버튼 + 환영 문구.
 * 배경은 `bg_home_library_decor`(좌우 책장·나무·화분 일러스트, 가운데는 비어 있게 제작됨) —
 * Hero 뒤 레이어에 놓아 중앙 제목·문구를 가리지 않는다.
 */
@Composable
private fun HomeHero(
    welcomeMessage: String,
    onAdminClick: () -> Unit,
    compact: Boolean,
    modifier: Modifier = Modifier
) {
    val logoSize = if (compact) 84.dp else 100.dp

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(bottomStart = 34.dp, bottomEnd = 34.dp))
            .background(MaterialTheme.colorScheme.primaryContainer)
    ) {
        Image(
            painter = painterResource(R.drawable.bg_home_library_decor),
            contentDescription = null,
            contentScale = ContentScale.FillBounds,
            modifier = Modifier.matchParentSize()
        )

        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth(0.86f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // ic_library_tree_logo에는 원형 배경이 이미 포함돼 있어 별도 Box/배경 없이 그대로 쓴다.
                Image(
                    painter = painterResource(R.drawable.ic_library_tree_logo),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.size(logoSize)
                )
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
                // 카드 원형 배경은 Compose로 유지하고(README), 그 위에 tint 없는 PNG 아이콘을 올린다.
                Box(
                    modifier = Modifier
                        .size(iconCircleSize)
                        .background(menu.iconBackground, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(menu.iconRes),
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
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
                Image(
                    painter = painterResource(R.drawable.ic_today_calendar),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.size(44.dp)
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
