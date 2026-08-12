package com.example.ibtech.ui.facility

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import com.example.ibtech.domain.model.WayfindingCorridorOverride
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// 안내도 PNG 원본은 실제 평면도 주위에 장식용 하늘색 여백(카드처럼 보이게 하는 테두리)이 크게
// 잡혀 있다 — 캔버스 기준 좌우 약 5%, 위 약 11%, 아래 약 19%(측정치). 크롭하지 않으면
// `ContentScale.Fit`이 이 여백까지 통째로 박스에 맞춰서 정작 평면도 그림은 작게 보인다.
private const val WAYFINDING_CROP_LEFT_FRACTION = 0.045f
private const val WAYFINDING_CROP_TOP_FRACTION = 0.11f
private const val WAYFINDING_CROP_RIGHT_FRACTION = 0.045f
private const val WAYFINDING_CROP_BOTTOM_FRACTION = 0.19f

/**
 * [facilityId]가 연결통로 안내도 대상([WayfindingCorridorOverride])이면 `assets/wayfinding/`의
 * 안내도를 잘라서 읽는다. 동행 도착 화면([NavigationProgressScreen])과 위치만 보기 화면
 * ([LocationMapScreen]) 양쪽에서 같은 이미지·같은 크롭을 쓰도록 여기 하나로 모은다 — 대상이
 * 아니면 null.
 */
@Composable
fun rememberWayfindingImageBitmap(facilityId: String): ImageBitmap? {
    val context = LocalContext.current
    val assetPath = WayfindingCorridorOverride.wayfindingImageAssetPath(facilityId)
    val state = produceState<ImageBitmap?>(initialValue = null, assetPath) {
        value = assetPath?.let { path ->
            withContext(Dispatchers.IO) {
                runCatching {
                    val full = context.assets.open(path).use { BitmapFactory.decodeStream(it) }
                        ?: return@runCatching null
                    val left = (full.width * WAYFINDING_CROP_LEFT_FRACTION).toInt()
                    val top = (full.height * WAYFINDING_CROP_TOP_FRACTION).toInt()
                    val right = full.width - (full.width * WAYFINDING_CROP_RIGHT_FRACTION).toInt()
                    val bottom = full.height - (full.height * WAYFINDING_CROP_BOTTOM_FRACTION).toInt()
                    Bitmap.createBitmap(full, left, top, right - left, bottom - top).asImageBitmap()
                }.getOrNull()
            }
        }
    }
    return state.value
}
