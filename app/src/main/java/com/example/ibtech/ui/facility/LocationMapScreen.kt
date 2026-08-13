package com.example.ibtech.ui.facility

import android.graphics.BitmapFactory
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.ibtech.R
import com.example.ibtech.domain.model.Facility
import com.example.ibtech.ui.common.DecorativeBackground
import com.example.ibtech.ui.common.EmptyState
import com.example.ibtech.ui.common.LibraryCard
import com.example.ibtech.ui.common.RobotSpeechBubble
import com.example.ibtech.ui.theme.LibraryDimens
import com.example.ibtech.ui.theme.SkyAccent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 위치만 보기 화면 (요구사항 명세서 2.5절).
 *
 * [Facility.mapImagePath]가 없으면 텍스트 설명만 보여준다(레이아웃이 깨지지 않게). 이미지가
 * 있으면 로컬 파일에서 읽어 보여준다 — 관리자 이미지 업로드(10단계)가 아직 없어 실제로는
 * 항상 텍스트만 표시되지만, 필드와 렌더링 경로는 미리 준비해 둔다.
 */
@Composable
fun LocationMapScreen(
    uiState: LocationMapUiState,
    onGoHome: () -> Unit,
    modifier: Modifier = Modifier
) {
    val facility = uiState.facility

    when {
        !uiState.isLoaded -> Unit

        facility == null -> EmptyState(
            message = stringResource(R.string.facility_detail_invalid),
            actionLabel = stringResource(R.string.top_bar_home),
            onAction = onGoHome,
            modifier = modifier.fillMaxSize()
        )

        else -> {
            // 연결통로 안내도 대상 시설(WayfindingCorridorOverride)은 동행 도착 화면과 똑같이
            // 여기서도 안내도를 보여준다(요청: "위치만 보기를 눌러 들어간 화면에도 안내도를
            // 표시해줘") — 관리자 업로드 이미지([Facility.mapImagePath], 아직 실제로 쓰이는
            // 곳이 없다)보다 우선한다.
            val bitmap = rememberWayfindingImageBitmap(facility.id) ?: rememberMapBitmap(facility.mapImagePath)

            Box(modifier = modifier.fillMaxSize()) {
                DecorativeBackground(modifier = Modifier.fillMaxSize())

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(LibraryDimens.ScreenPadding),
                    verticalArrangement = Arrangement.spacedBy(LibraryDimens.CardSpacing)
                ) {
                    // 위치 요약은 작은 흰 정보 카드로만 두고, 지도 이미지가 있을 때는 남는 세로
                    // 공간을 전부 그 이미지에 준다(요구사항: "지도 영역은 최대한 크게 유지").
                    LibraryCard(modifier = Modifier.fillMaxWidth(), accentColor = SkyAccent) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(LibraryDimens.CardPadding),
                            verticalArrangement = Arrangement.spacedBy(LibraryDimens.CardSpacing)
                        ) {
                            Text(
                                text = facility.name,
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            RobotSpeechBubble(
                                text = buildFloorDirectionGuideText(LocalContext.current, facility, uiState.baseFloor)
                            )
                        }
                    }
                    // 안내도가 여러 장을 번갈아 보여주는 시설(계단 안내도 대상)은 그냥 이미지를
                    // 바꿔치면 언제 바뀌었는지 놓치기 쉽다 — Crossfade로 부드럽게 겹쳐 넘겨서
                    // 전환을 눈에 띄게 한다(요청: "안내도가 전환된다는걸 사용자가 알수있게").
                    Crossfade(
                        targetState = bitmap,
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        animationSpec = tween(durationMillis = 700),
                        label = "wayfindingImage"
                    ) { currentBitmap ->
                        if (currentBitmap != null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(24.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    bitmap = currentBitmap,
                                    contentDescription = null,
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun rememberMapBitmap(path: String?): ImageBitmap? {
    val state = produceState<ImageBitmap?>(initialValue = null, path) {
        value = path?.let { imagePath ->
            withContext(Dispatchers.IO) {
                runCatching { BitmapFactory.decodeFile(imagePath)?.asImageBitmap() }.getOrNull()
            }
        }
    }
    return state.value
}
