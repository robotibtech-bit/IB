package com.example.ibtech.ui.usage

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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ibtech.domain.model.UsageTopic
import com.example.ibtech.domain.usecase.UsageCategoryItem
import com.example.ibtech.ui.common.DecorativeBackground
import com.example.ibtech.ui.common.FillSpaceGrid
import com.example.ibtech.ui.common.LibraryCard
import com.example.ibtech.ui.common.debounced
import com.example.ibtech.ui.theme.IBTECHTheme
import com.example.ibtech.ui.theme.LibraryDimens

private val CategoryCardIconCircle = 108.dp
private val CategoryCardIcon = 56.dp

/**
 * 이용방법 1차 메뉴 화면 (요구사항 명세서 2.7절).
 *
 * 하위 항목이 1개뿐인 카테고리인지는 [UsageCategoryItem.singleAnswerTopicId] 유무로 이미
 * 정해져 있다 — 이 화면은 그 값을 보고 어디로 이동할지만 콜백에 위임한다.
 *
 * 12단계: 고정 4개(관리자가 일부를 숨기면 더 적을 수 있음) 카테고리를 여백 없이 화면을 채우는
 * 큰 카드 격자로 배치한다.
 */
@Composable
fun UsageCategoryScreen(
    uiState: UsageCategoryUiState,
    onSelectCategory: (UsageCategoryItem) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        DecorativeBackground(modifier = Modifier.fillMaxSize())

        if (uiState.isLoaded) {
            // 카테고리가 5개(PDF 5개 장 반영)로 늘면서 2열 고정이면 3행이 되어 카드 높이가
            // 줄어들고 제목 글자가 카드 밖으로 밀려 잘렸다 — 4개까지는 2열(2행), 그 이상은
            // 3열로 늘려 행 높이를 4개일 때와 비슷하게 유지한다.
            val count = uiState.categories.size
            val columns = when {
                count <= 2 -> count.coerceAtLeast(1)
                count <= 4 -> 2
                else -> 3
            }
            FillSpaceGrid(
                items = uiState.categories,
                columns = columns,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(LibraryDimens.ScreenPadding)
            ) { item, cardModifier ->
                UsageCategoryCard(item = item, onClick = { onSelectCategory(item) }, modifier = cardModifier)
            }
        }
    }
}

/**
 * 참고 시안(mockup_usage_category.png) 스타일: 왼쪽 원형 아이콘 + 오른쪽 제목/부제,
 * 우하단에 작은 화살표. 강조색·부제는 [UsageCategoryIcons.kt]의 id 기준 매핑 함수를 쓴다 —
 * 매핑되지 않은 카테고리(향후 추가분)는 기본 Teal + 공통 부제로 자연스럽게 대체된다.
 */
@Composable
private fun UsageCategoryCard(item: UsageCategoryItem, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val accent = item.category.resolveCategoryAccent()
    val iconBackground = item.category.resolveCategoryIconBackground()

    LibraryCard(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = debounced(onClick)),
        accentColor = accent,
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
                        .size(CategoryCardIconCircle)
                        .background(color = iconBackground, shape = CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = item.category.resolveCategoryIcon(),
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(CategoryCardIcon)
                    )
                }
                Spacer(modifier = Modifier.width(20.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.category.title,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = item.category.resolveCategorySubtitle(),
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 26.sp, lineHeight = 32.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(14.dp)
                    .size(LibraryDimens.ArrowCircle)
                    .background(iconBackground, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = accent
                )
            }
        }
    }
}

private fun previewCategoryUiState() = UsageCategoryUiState(
    isLoaded = true,
    categories = listOf(
        UsageCategoryItem(UsageTopic(id = "category_loan", parentId = null, title = "책 대출·반납"), singleAnswerTopicId = null),
        UsageCategoryItem(UsageTopic(id = "category_membership", parentId = null, title = "회원가입"), singleAnswerTopicId = null),
        UsageCategoryItem(UsageTopic(id = "category_reading_materials", parentId = null, title = "열람실·자료실 이용"), singleAnswerTopicId = null),
        UsageCategoryItem(UsageTopic(id = "category_hours", parentId = null, title = "운영시간·휴관일"), singleAnswerTopicId = null)
    )
)

@Preview(name = "이용방법 1차 메뉴 · 1280x720", widthDp = 1280, heightDp = 720, showBackground = true)
@Composable
private fun UsageCategoryScreenPreview() {
    IBTECHTheme {
        UsageCategoryScreen(uiState = previewCategoryUiState(), onSelectCategory = {})
    }
}
