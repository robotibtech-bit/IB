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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ibtech.R
import com.example.ibtech.domain.model.Facility
import com.example.ibtech.domain.usecase.ResolveNavigationTargetUseCase
import com.example.ibtech.ui.common.DecorativeBackground
import com.example.ibtech.ui.common.EmptyState
import com.example.ibtech.ui.common.FillSpaceGrid
import com.example.ibtech.ui.common.LibraryCard
import com.example.ibtech.ui.common.LibraryOutlinedButton
import com.example.ibtech.ui.common.debounced
import com.example.ibtech.ui.theme.CoralAccent
import com.example.ibtech.ui.theme.LibraryDimens
import com.example.ibtech.ui.theme.SkyAccent
import com.example.ibtech.ui.theme.SkyAccentContainer

/** 대표 장소(자주 쓰는 곳) 그리드 화면을 일단 막아뒀다(요구사항: "시설안내 버튼을 누르면
 * 바로 다른 장소 찾기 화면으로, 자주쓰는 버튼 기능은 일단 막아놔 안보이게") — 이 그리드로
 * 돌아가는 버튼을 숨긴다. [FacilityListViewModel]의 `searchVisibleState` 초기값과 함께
 * true로 되돌리면 원래 동작(대표 장소 먼저 보여주기)이 복구된다. */
private const val FEATURED_FACILITY_GRID_ENABLED = false

/**
 * 시설 목록 화면 (요구사항 명세서 2.3절, 12단계 개편).
 *
 * 기본 화면은 관리자가 "대표 장소"로 표시한 시설만(관리자 설정 개수만큼, 2/4/8) 화면을 여백
 * 없이 채우는 큰 카드로 보여준다. "다른 장소 찾기"를 누르면 노출 가능한 시설 전체를 검색·탐색
 * 가능한 목록으로 전환한다 — 대표 장소로 지정되지 않은 곳도 여기서는 전부 보이고 누를 수 있다.
 * ([FEATURED_FACILITY_GRID_ENABLED]가 false인 동안은 항상 검색 목록부터 보여준다.)
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
                            FacilityCard(
                                facility = facility,
                                knownLocations = uiState.knownLocations,
                                baseFloor = uiState.baseFloor,
                                onClick = { onSelectFacility(facility) }
                            )
                        }
                    }
                }

                if (FEATURED_FACILITY_GRID_ENABLED) {
                    LibraryOutlinedButton(
                        text = stringResource(R.string.facility_list_show_featured),
                        onClick = onToggleSearch,
                        modifier = Modifier.padding(top = 12.dp)
                    )
                }
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
                    FacilityCard(
                        facility = facility,
                        knownLocations = uiState.knownLocations,
                        baseFloor = uiState.baseFloor,
                        onClick = { onSelectFacility(facility) },
                        modifier = cardModifier
                    )
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

/**
 * PDF 목업의 "아이콘 상단 + 라벨 하단" 카드를 재해석한 레이아웃.
 *
 * 시설 대표색은 Sky로 통일한다(01_DESIGN_SYSTEM.md "시설 찾기·위치 안내 = Sky"). 실제로
 * `goTo()`할 대상([ResolveNavigationTargetUseCase])이 temi 지도에 없으면(관리자가 아직
 * 비활성화하지 않았다면 이용자 화면에도 그대로 노출될 수 있다 — FacilityRepository.visibleFacilities는
 * isEnabled·floor만 거르고 이동 가능 여부는 거르지 않는다), 아이콘 모서리에 작은 Coral 배지로
 * "위치 정보 없음" 상태를 표시한다 — 텍스트 줄을 추가하면 이 카드(대표 장소 2~8개 격자에서
 * 이미 세로 여유가 가장 빠듯한 카드)가 다시 잘리므로 배지 하나로만 표현한다.
 *
 * `facility.syncStatus`(시설 자체의 `sourcePoiName`이 temi에 있는지)가 아니라 실제 이동 대상
 * 기준으로 판단한다 — POI GOTO 경로 처리 요구사항 이후로는 연결통로·엘리베이터 대상 시설은
 * `sourcePoiName` 자체가 temi에 등록돼 있지 않아도 정상 동작하므로, syncStatus만 보면 정상
 * 작동하는 시설도 "위치 정보 없음"으로 잘못 표시된다.
 */
@Composable
private fun FacilityCard(
    facility: Facility,
    knownLocations: List<String>,
    baseFloor: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDisconnected = ResolveNavigationTargetUseCase.poiName(facility, baseFloor) !in knownLocations

    LibraryCard(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = debounced(onClick)),
        accentColor = SkyAccent
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                // 이 카드는 이름+층 두 줄을 쓰는 유일한 큰 카드라, 2행 격자(대표 장소 4~8개)에서
                // 다른 카드보다 여유가 적다 — 안쪽 여백을 CardPadding(28dp)보다 좁게 잡는다.
                // 세로 패딩만 키운 건 목록(전체 검색) 화면에서 카드를 더 높여서, 스크롤 가능함을
                // 보여주는 3번째 줄이 절반이 아니라 4분의 1 정도만 보이게 하려는 것이다(사용자 요청).
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp, vertical = 30.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box {
                    // 공용 LibraryDimens.LargeIconCircle/LargeIconSize(168dp/144dp)보다 이
                    // 카드에서만 작게 쓴다(사용자 요청으로 축소) — 다른 화면(퀴즈 주제, 로봇과
                    // 놀아요 등)의 아이콘 크기는 그대로 둔다.
                    Box(
                        modifier = Modifier
                            .size(108.dp)
                            .background(color = SkyAccentContainer, shape = CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = facility.resolveIcon(),
                            contentDescription = null,
                            tint = SkyAccent,
                            modifier = Modifier.size(88.dp)
                        )
                    }
                    if (isDisconnected) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .size(36.dp)
                                .background(CoralAccent, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.LocationOff,
                                contentDescription = stringResource(R.string.facility_card_poi_disconnected),
                                tint = MaterialTheme.colorScheme.surface,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                // 이름 글자 수(공백 제외)에 따라 글자 크기를 줄여서 항상 한 줄에 담는다(사용자
                // 요청) — 5자까지는 그대로, 6자부터는 한 글자씩 늘어날 때마다 더 작게 줄인다.
                val compactLength = facility.name.replace(" ", "").length
                val nameFontSize = when {
                    compactLength <= 5 -> 40.sp
                    compactLength == 6 -> 34.sp
                    compactLength == 7 -> 30.sp
                    else -> 26.sp
                }
                Text(
                    text = facility.name,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontSize = nameFontSize,
                        lineHeight = nameFontSize
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                // labelLarge(41sp)를 그대로 쓰면 이름 한 줄 밑에 이 층수 줄까지 들어갈 세로
                // 여유가 2행 격자(대표 장소 8개)에서 모자라 카드 아래쪽이 잘려 보였다(사용자
                // 피드백) — 부제 성격의 줄이라 훨씬 작은 크기로도 충분히 읽힌다.
                Text(
                    text = formatFloorBadge(LocalContext.current, facility.floor),
                    style = MaterialTheme.typography.labelLarge.copy(fontSize = 26.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(10.dp)
                    .size(LibraryDimens.ArrowCircle)
                    .background(SkyAccentContainer, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = SkyAccent
                )
            }
        }
    }
}
