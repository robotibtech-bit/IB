package com.example.ibtech.ui.facility

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.stringResource
import com.example.ibtech.R
import com.example.ibtech.domain.model.Facility
import com.example.ibtech.ui.common.DecorativeBackground
import com.example.ibtech.ui.common.EmptyState
import com.example.ibtech.ui.common.RobotSpeechBubble
import com.example.ibtech.ui.theme.LibraryDimens
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
            val bitmap = rememberMapBitmap(facility.mapImagePath)

            Box(modifier = modifier.fillMaxSize()) {
                DecorativeBackground(modifier = Modifier.fillMaxSize())

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(LibraryDimens.ScreenPadding),
                    verticalArrangement = Arrangement.spacedBy(LibraryDimens.CardSpacing)
                ) {
                    Text(
                        text = facility.name,
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    RobotSpeechBubble(
                        text = facility.directionText?.takeIf { it.isNotBlank() }
                            ?: stringResource(R.string.facility_map_default_direction, facility.floor)
                    )
                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap,
                            contentDescription = null,
                            modifier = Modifier.fillMaxWidth()
                        )
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
