package com.example.ibtech.ui.facility

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.ibtech.R
import com.example.ibtech.domain.model.Facility
import com.example.ibtech.ui.common.DecorativeBackground
import com.example.ibtech.ui.common.EmptyState
import com.example.ibtech.ui.common.FillSpaceGrid
import com.example.ibtech.ui.common.LibraryCard
import com.example.ibtech.ui.common.LibraryOutlinedButton
import com.example.ibtech.ui.common.debounced
import com.example.ibtech.ui.theme.LibraryDimens

/**
 * 시설 목록 화면 (요구사항 명세서 2.3절, 12단계 개편).
 *
 * 기본 화면은 관리자가 "대표 장소"로 표시한 시설만(관리자 설정 개수만큼, 2/4/8) 화면을 여백
 * 없이 채우는 큰 카드로 보여준다. "다른 장소 찾기"를 누르면 노출 가능한 시설 전체를 검색·탐색
 * 가능한 목록으로 전환한다 — 대표 장소로 지정되지 않은 곳도 여기서는 전부 보이고 누를 수 있다.
 */
@Composable
fun FacilityListScreen(
    uiState: FacilityListUiState,
    onQueryChange: (String) -> Unit,
    onToggleSearch: () -> Unit,
    onSelectFacility: (Facility) -> Unit,
    onGoHome: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        DecorativeBackground(modifier = Modifier.fillMaxSize())

        if (!uiState.isLoaded) return@Box

        if (!uiState.hasAnyFacility) {
            EmptyState(
                message = stringResource(R.string.facility_list_empty),
                actionLabel = stringResource(R.string.top_bar_home),
                onAction = onGoHome,
                modifier = Modifier.fillMaxSize()
            )
            return@Box
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(LibraryDimens.ScreenPadding)
        ) {
            if (uiState.isSearchVisible) {
                OutlinedTextField(
                    value = uiState.query,
                    onValueChange = onQueryChange,
                    singleLine = true,
                    placeholder = { Text(stringResource(R.string.facility_list_search_hint)) },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                )

                if (uiState.allFacilities.isEmpty()) {
                    EmptyState(
                        message = stringResource(R.string.facility_list_empty_search),
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 240.dp),
                        contentPadding = PaddingValues(vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(LibraryDimens.CardSpacing),
                        horizontalArrangement = Arrangement.spacedBy(LibraryDimens.CardSpacing),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(uiState.allFacilities, key = { it.id }) { facility ->
                            FacilityCard(facility = facility, onClick = { onSelectFacility(facility) })
                        }
                    }
                }

                LibraryOutlinedButton(
                    text = stringResource(R.string.facility_list_show_featured),
                    onClick = onToggleSearch,
                    modifier = Modifier.padding(top = 12.dp)
                )
            } else if (uiState.featuredFacilities.isNotEmpty()) {
                val columns = when {
                    uiState.featuredFacilities.size <= 2 -> uiState.featuredFacilities.size.coerceAtLeast(1)
                    uiState.featuredFacilities.size <= 4 -> 2
                    else -> 4
                }
                FillSpaceGrid(
                    items = uiState.featuredFacilities,
                    columns = columns,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) { facility, cardModifier ->
                    FacilityCard(facility = facility, onClick = { onSelectFacility(facility) }, modifier = cardModifier)
                }
                Spacer(modifier = Modifier.height(LibraryDimens.CardSpacing))
                LibraryOutlinedButton(
                    text = stringResource(R.string.facility_list_search_toggle),
                    onClick = onToggleSearch
                )
            } else {
                EmptyState(
                    message = stringResource(R.string.facility_list_no_featured),
                    actionLabel = stringResource(R.string.facility_list_search_toggle),
                    onAction = onToggleSearch,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

/** PDF 목업의 "아이콘 상단 + 라벨 하단" 카드를 재해석한 레이아웃. */
@Composable
private fun FacilityCard(facility: Facility, onClick: () -> Unit, modifier: Modifier = Modifier) {
    LibraryCard(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = debounced(onClick))
    ) {
        Column(
            // 이 카드는 이름+층 두 줄을 쓰는 유일한 큰 카드라, 2행 격자(대표 장소 4~8개)에서
            // 다른 카드보다 여유가 적다 — 안쪽 여백을 CardPadding(28dp)보다 좁게 잡는다.
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(LibraryDimens.LargeIconCircle)
                    .background(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = facility.resolveIcon(),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(LibraryDimens.LargeIconSize)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            // 이름+층 두 줄을 쓰는 유일한 큰 카드라 대표 장소 개수(2/4/8, 관리자 설정)에 따라
            // 칸이 좁아질 수 있다 — 한 줄로 고정하고 넘치면 말줄임표로 잘라 세로 높이가
            // 예측 불가능하게 늘어나 카드 밖으로 밀려나는 일을 막는다.
            Text(
                text = facility.name,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = stringResource(R.string.facility_card_floor_format, facility.floor),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
