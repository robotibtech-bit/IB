package com.example.ibtech.ui.usage

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.ibtech.domain.usecase.UsageCategoryItem
import com.example.ibtech.ui.common.DecorativeBackground
import com.example.ibtech.ui.common.LibraryCard
import com.example.ibtech.ui.common.debounced
import com.example.ibtech.ui.theme.LibraryDimens

/**
 * 이용방법 1차 메뉴 화면 (요구사항 명세서 2.7절).
 *
 * 하위 항목이 1개뿐인 카테고리인지는 [UsageCategoryItem.singleAnswerTopicId] 유무로 이미
 * 정해져 있다 — 이 화면은 그 값을 보고 어디로 이동할지만 콜백에 위임한다.
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
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 220.dp),
                contentPadding = PaddingValues(LibraryDimens.ScreenPadding),
                verticalArrangement = Arrangement.spacedBy(LibraryDimens.CardSpacing),
                horizontalArrangement = Arrangement.spacedBy(LibraryDimens.CardSpacing),
                modifier = Modifier.fillMaxSize()
            ) {
                items(uiState.categories, key = { it.category.id }) { item ->
                    UsageCategoryCard(item = item, onClick = { onSelectCategory(item) })
                }
            }
        }
    }
}

@Composable
private fun UsageCategoryCard(item: UsageCategoryItem, onClick: () -> Unit) {
    LibraryCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = debounced(onClick))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(LibraryDimens.CardPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = item.category.resolveCategoryIcon(),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Text(
                text = item.category.title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
        }
    }
}
