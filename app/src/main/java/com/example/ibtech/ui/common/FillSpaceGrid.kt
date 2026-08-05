package com.example.ibtech.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.ibtech.ui.theme.LibraryDimens

/**
 * 고정/소수 개수 카드를 화면(또는 주어진 영역)을 여백 없이 채우는 격자로 배치한다(12단계).
 *
 * `LazyVerticalGrid(Adaptive)`는 카드를 최소 크기로만 채우고 남는 공간을 비워 두지만, 이
 * 컴포저블은 각 행/카드에 `weight`를 줘 항상 주어진 영역 전체를 채운다. 시설 안내의 대표 장소,
 * 이용방법 1차 메뉴, 로봇과 놀아요 메뉴처럼 "몇 개 안 되는 목적지를 크게 보여준다"에 맞는
 * 화면에서만 쓴다 — 개수가 많아 스크롤이 필요한 목록(전체 시설 검색 등)에는 맞지 않는다.
 */
@Composable
fun <T> FillSpaceGrid(
    items: List<T>,
    columns: Int,
    modifier: Modifier = Modifier,
    itemContent: @Composable (T, Modifier) -> Unit
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(LibraryDimens.CardSpacing)
    ) {
        items.chunked(columns).forEach { rowItems ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(LibraryDimens.CardSpacing)
            ) {
                rowItems.forEach { item ->
                    itemContent(
                        item,
                        Modifier
                            .weight(1f)
                            .fillMaxSize()
                    )
                }
                repeat(columns - rowItems.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}
