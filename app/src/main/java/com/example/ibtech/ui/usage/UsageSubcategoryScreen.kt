package com.example.ibtech.ui.usage

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.AssignmentReturn
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.EventSeat
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Stairs
import androidx.compose.material.icons.filled.Tablet
import androidx.compose.material.icons.filled.WatchLater
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
import com.example.ibtech.domain.model.UsageTopic
import com.example.ibtech.ui.common.DecorativeBackground
import com.example.ibtech.ui.common.EmptyState
import com.example.ibtech.ui.common.LibraryCard
import com.example.ibtech.ui.common.debounced
import com.example.ibtech.ui.theme.CoralAccent
import com.example.ibtech.ui.theme.CoralAccentContainer
import com.example.ibtech.ui.theme.LavenderAccent
import com.example.ibtech.ui.theme.LavenderAccentContainer
import com.example.ibtech.ui.theme.IBTECHTheme
import com.example.ibtech.ui.theme.LibraryDimens
import com.example.ibtech.ui.theme.MintContainer
import com.example.ibtech.ui.theme.MintPrimary
import com.example.ibtech.ui.theme.SkyAccent
import com.example.ibtech.ui.theme.SkyAccentContainer
import com.example.ibtech.ui.theme.YellowAccent
import com.example.ibtech.ui.theme.YellowAccentContainer

/**
 * 이용방법 세부 항목 목록 화면 (요구사항 명세서 2.8절, 13단계에서 VisitorListCard 스타일 적용).
 *
 * 항목 4개보다 많거나 텍스트가 길어도 [LazyColumn]이 스크롤을 처리하므로 그대로 유지한다.
 */
@Composable
fun UsageSubcategoryScreen(
    uiState: UsageSubcategoryUiState,
    onSelectTopic: (UsageTopic) -> Unit,
    onGoHome: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        DecorativeBackground(modifier = Modifier.fillMaxSize())

        when {
            !uiState.isLoaded -> Unit

            uiState.category == null || uiState.subtopics.isEmpty() -> EmptyState(
                message = stringResource(R.string.usage_invalid_content),
                actionLabel = stringResource(R.string.top_bar_home),
                onAction = onGoHome,
                modifier = Modifier.fillMaxSize()
            )

            else -> LazyColumn(
                contentPadding = PaddingValues(LibraryDimens.ScreenPadding),
                verticalArrangement = Arrangement.spacedBy(LibraryDimens.CardSpacing),
                modifier = Modifier.fillMaxSize()
            ) {
                items(uiState.subtopics, key = { it.id }) { topic ->
                    UsageTopicRow(topic = topic, onClick = { onSelectTopic(topic) })
                }
            }
        }
    }
}

/** 화면 전용 시각 정보. [UsageTopic]에는 저장하지 않는다 — 관리자가 답변을 바꿔도 무관하다. */
private data class TopicVisual(
    val icon: ImageVector,
    val subtitle: String,
    val accent: Color,
    val iconBackground: Color
)

/**
 * 제목 키워드로 아이콘·부제·강조색을 고른다(도메인 모델에는 저장하지 않는 화면 전용 매핑).
 * 회원가입 4항목은 참고 시안(mockup_membership_submenu.png)과 동일하게 매핑했다.
 * 매핑되지 않는 제목(관리자가 새로 추가한 항목 등)은 공통 아이콘 + 공통 부제로 대체된다 —
 * 카테고리 수나 항목 구성이 달라져도 깨지지 않는다.
 */
private fun resolveTopicVisual(title: String): TopicVisual = when {
    "대상" in title -> TopicVisual(Icons.Filled.Badge, "회원으로 가입할 수 있는 대상을 확인해요", MintPrimary, MintContainer)
    "서류" in title -> TopicVisual(Icons.AutoMirrored.Filled.Article, "연령에 따라 필요한 서류를 안내해요", SkyAccent, SkyAccentContainer)
    "모바일" in title || "재발급" in title ->
        TopicVisual(Icons.Filled.PhoneAndroid, "모바일 이용과 재발급 방법을 확인해요", LavenderAccent, LavenderAccentContainer)
    "절차" in title -> TopicVisual(Icons.Filled.Stairs, "회원증 발급 과정을 순서대로 확인해요", YellowAccent, YellowAccentContainer)
    "전자도서관" in title -> TopicVisual(Icons.Filled.Tablet, "전자책·오디오북 대출 규정을 확인해요", LavenderAccent, LavenderAccentContainer)
    "예약" in title -> TopicVisual(Icons.Filled.WatchLater, "예약 가능 권수와 방법을 확인해요", YellowAccent, YellowAccentContainer)
    "반납" in title || "연체" in title ->
        TopicVisual(Icons.AutoMirrored.Filled.AssignmentReturn, "반납 방법과 연체 규정을 확인해요", CoralAccent, CoralAccentContainer)
    "대출" in title -> TopicVisual(Icons.AutoMirrored.Filled.MenuBook, "대출 권수와 기간을 확인해요", SkyAccent, SkyAccentContainer)
    "좌석" in title -> TopicVisual(Icons.Filled.EventSeat, "좌석 발급과 반납 방법을 확인해요", MintPrimary, MintContainer)
    "수칙" in title || "대기" in title ->
        TopicVisual(Icons.Filled.HourglassTop, "이용 수칙과 대기 안내를 확인해요", CoralAccent, CoralAccentContainer)
    "디지털자료실" in title -> TopicVisual(Icons.Filled.Computer, "PC·DVD 등 디지털 시설을 확인해요", LavenderAccent, LavenderAccentContainer)
    "자료실" in title -> TopicVisual(Icons.AutoMirrored.Filled.LibraryBooks, "자료실별 소장 자료를 확인해요", MintPrimary, MintContainer)
    "운영시간" in title -> TopicVisual(Icons.Filled.Schedule, "요일별 운영시간을 확인해요", YellowAccent, YellowAccentContainer)
    "휴관" in title -> TopicVisual(Icons.Filled.EventBusy, "정기·임시 휴관일을 확인해요", CoralAccent, CoralAccentContainer)
    else -> TopicVisual(Icons.Filled.Info, "자세한 이용방법을 확인해요", MintPrimary, MintContainer)
}

@Composable
private fun UsageTopicRow(topic: UsageTopic, onClick: () -> Unit) {
    val visual = resolveTopicVisual(topic.title)

    LibraryCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = debounced(onClick)),
        accentColor = visual.accent
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(LibraryDimens.CardPadding),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(LibraryDimens.ListIconCircle)
                    .background(visual.iconBackground, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = visual.icon,
                    contentDescription = null,
                    tint = visual.accent,
                    modifier = Modifier.size(LibraryDimens.ListIcon)
                )
            }
            Spacer(modifier = Modifier.width(20.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = topic.title,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = visual.subtitle,
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 24.sp, lineHeight = 30.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Box(
                modifier = Modifier
                    .size(LibraryDimens.ArrowCircle)
                    .background(visual.iconBackground, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = visual.accent
                )
            }
        }
    }
}

private fun previewSubcategoryUiState() = UsageSubcategoryUiState(
    isLoaded = true,
    category = UsageTopic(id = "category_membership", parentId = null, title = "회원가입"),
    subtopics = listOf(
        UsageTopic(id = "membership_target", parentId = "category_membership", title = "가입 대상", sortOrder = 0),
        UsageTopic(id = "membership_documents", parentId = "category_membership", title = "가입 연령별 필요 서류", sortOrder = 1),
        UsageTopic(id = "membership_procedure", parentId = "category_membership", title = "발급 절차", sortOrder = 2),
        UsageTopic(id = "membership_card", parentId = "category_membership", title = "모바일 회원증 및 재발급", sortOrder = 3)
    )
)

@Preview(name = "이용방법 세부 목록(회원가입) · 1280x720", widthDp = 1280, heightDp = 720, showBackground = true)
@Composable
private fun UsageSubcategoryScreenPreview() {
    IBTECHTheme {
        UsageSubcategoryScreen(uiState = previewSubcategoryUiState(), onSelectTopic = {}, onGoHome = {})
    }
}
