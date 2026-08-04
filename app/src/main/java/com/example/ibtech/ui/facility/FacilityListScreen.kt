package com.example.ibtech.ui.facility

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.unit.dp
import com.example.ibtech.R
import com.example.ibtech.domain.model.Facility
import com.example.ibtech.ui.common.DecorativeBackground
import com.example.ibtech.ui.common.EmptyState
import com.example.ibtech.ui.common.LibraryCard
import com.example.ibtech.ui.common.LibraryOutlinedButton
import com.example.ibtech.ui.common.debounced
import com.example.ibtech.ui.theme.LibraryDimens

/**
 * 시설 목록 화면 (요구사항 명세서 2.3절).
 *
 * `isEnabled == true && floor != UNSET_FLOOR`인 시설만 [FacilityListViewModel]이 걸러서
 * 넘겨준다 — 이 화면은 필터링 로직을 갖지 않고 받은 목록을 그대로 그린다.
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

        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = LibraryDimens.ScreenPadding, vertical = 12.dp),
                horizontalArrangement = Arrangement.End
            ) {
                LibraryOutlinedButton(
                    text = stringResource(R.string.facility_list_search_toggle),
                    onClick = onToggleSearch,
                    modifier = Modifier.fillMaxWidth(0.4f)
                )
            }

            if (uiState.isSearchVisible) {
                OutlinedTextField(
                    value = uiState.query,
                    onValueChange = onQueryChange,
                    singleLine = true,
                    placeholder = { Text(stringResource(R.string.facility_list_search_hint)) },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = LibraryDimens.ScreenPadding)
                        .padding(bottom = 12.dp)
                )
            }

            when {
                !uiState.isLoaded -> Unit

                !uiState.hasAnyFacility -> EmptyState(
                    message = stringResource(R.string.facility_list_empty),
                    actionLabel = stringResource(R.string.top_bar_home),
                    onAction = onGoHome,
                    modifier = Modifier.fillMaxSize()
                )

                uiState.facilities.isEmpty() -> EmptyState(
                    message = stringResource(R.string.facility_list_empty_search),
                    modifier = Modifier.fillMaxSize()
                )

                else -> LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 220.dp),
                    contentPadding = PaddingValues(LibraryDimens.ScreenPadding),
                    verticalArrangement = Arrangement.spacedBy(LibraryDimens.CardSpacing),
                    horizontalArrangement = Arrangement.spacedBy(LibraryDimens.CardSpacing),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(uiState.facilities, key = { it.id }) { facility ->
                        FacilityCard(facility = facility, onClick = { onSelectFacility(facility) })
                    }
                }
            }
        }
    }
}

/** PDF 목업의 "아이콘 상단 + 라벨 하단" 카드를 재해석한 레이아웃. */
@Composable
private fun FacilityCard(facility: Facility, onClick: () -> Unit) {
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
                    imageVector = facility.resolveIcon(),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Text(
                text = facility.name,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            Text(
                text = stringResource(R.string.facility_card_floor_format, facility.floor),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
