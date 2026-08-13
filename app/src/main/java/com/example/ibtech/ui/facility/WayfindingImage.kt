package com.example.ibtech.ui.facility

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import com.example.ibtech.domain.model.StairsWayfindingOverride
import com.example.ibtech.domain.model.WayfindingCorridorOverride
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

// 연결통로 안내도 PNG 원본은 실제 평면도 주위에 장식용 하늘색 여백(카드처럼 보이게 하는
// 테두리)이 잡혀 있다 — 크롭하지 않으면 `ContentScale.Fit`이 이 여백까지 통째로 박스에 맞춰서
// 정작 평면도 그림은 작게 보인다. 여백 비율은 좌 약 3%, 위 약 4%, 우 약 5%, 아래 약 19%
// (측정치) — 이미지가 업데이트되면서 왼쪽 위에 "1F" 층수 표시가 여백 안쪽 가장자리에 가깝게
// 추가돼, 이전보다 좌·상단은 더 적게 잘라야 그 표시가 안 잘린다(사용자 피드백: "1F 글씨가
// 잘려서 안나오네"). 계단 안내도(StairsWayfindingOverride)는 원본 자체가 이미 여백 없이 꽉
// 차 있어 크롭이 필요 없다 — 같은 비율을 적용하면 오히려 그림 가장자리가 잘린다. 크롭은
// 시설이 아니라 이미지가 어느 asset 폴더에서 왔는지로 판단한다 — 계단 안내도 대상은 아래에서
// 보듯 두 폴더의 이미지를 섞어서 보여주기 때문이다.
private data class WayfindingCropInsets(val left: Float, val top: Float, val right: Float, val bottom: Float)

private val CORRIDOR_CROP = WayfindingCropInsets(left = 0.025f, top = 0.035f, right = 0.045f, bottom = 0.19f)
private val NO_CROP = WayfindingCropInsets(left = 0f, top = 0f, right = 0f, bottom = 0f)

private fun wayfindingCropInsetsForPath(assetPath: String): WayfindingCropInsets =
    if (assetPath.startsWith("wayfinding/")) CORRIDOR_CROP else NO_CROP

/** 안내도가 여러 장일 때(계단 안내도 대상) 몇 ms마다 다음 장으로 넘길지. */
private const val WAYFINDING_CYCLE_INTERVAL_MS = 6_000L

/**
 * [facilityId]가 안내도 대상이면 순서대로 보여줄 asset 경로 목록을, 대상이 아니면 빈 목록을
 * 돌려준다.
 *
 * 계단 안내도 대상([StairsWayfindingOverride])은 로봇이 실제로 데려다주는 지점(연결통로)과
 * 안내도가 그리는 시작점(여름강의실 옆 계단 위, 2층)이 서로 다르다 — 연결통로에 도착한 다음
 * 여름강의실까지 먼저 걸어가야 계단이 나오므로, 여름강의실 연결통로 안내도
 * ([WayfindingCorridorOverride])를 먼저 넣고 그 시설 전용 계단 안내도를 이어 붙인다(요청:
 * "여름강의실 안내도와 반복적으로 표시"). 그 외(연결통로 대상)는 기존처럼 한 장뿐이다.
 */
fun wayfindingImageAssetPaths(facilityId: String): List<String> {
    val stairsPath = StairsWayfindingOverride.wayfindingImageAssetPath(facilityId)
    if (stairsPath != null) {
        val summerLectureRoomPath = WayfindingCorridorOverride.wayfindingImageAssetPath("여름강의실")
        return listOfNotNull(summerLectureRoomPath, stairsPath)
    }
    return listOfNotNull(WayfindingCorridorOverride.wayfindingImageAssetPath(facilityId))
}

/**
 * [facilityId]가 안내도 대상이면 그중 첫 번째 asset 경로를 돌려준다 — "안내도가 있는 시설인지"만
 * 판단하면 되는 곳(예: [NavigationProgressScreen]의 레이아웃 분기)에서 쓴다.
 */
fun wayfindingImageAssetPath(facilityId: String): String? = wayfindingImageAssetPaths(facilityId).firstOrNull()

/**
 * [facilityId]의 안내도를 읽어 필요하면 잘라낸다. 안내도가 두 장 이상이면([wayfindingImageAssetPaths])
 * 일정 간격으로 번갈아 보여준다. 동행 도착 화면([NavigationProgressScreen])과 위치만 보기 화면
 * ([LocationMapScreen]) 양쪽에서 같은 이미지·같은 크롭을 쓰도록 여기 하나로 모은다 — 대상이
 * 아니면 null.
 */
@Composable
fun rememberWayfindingImageBitmap(facilityId: String): ImageBitmap? {
    val context = LocalContext.current
    val assetPaths = remember(facilityId) { wayfindingImageAssetPaths(facilityId) }
    var index by remember(facilityId) { mutableIntStateOf(0) }

    if (assetPaths.size > 1) {
        LaunchedEffect(facilityId) {
            while (true) {
                delay(WAYFINDING_CYCLE_INTERVAL_MS)
                index = (index + 1) % assetPaths.size
            }
        }
    }

    val state = produceState<ImageBitmap?>(initialValue = null, assetPaths, index) {
        value = assetPaths.getOrNull(index)?.let { path ->
            withContext(Dispatchers.IO) {
                runCatching {
                    val full = context.assets.open(path).use { BitmapFactory.decodeStream(it) }
                        ?: return@runCatching null
                    val insets = wayfindingCropInsetsForPath(path)
                    val left = (full.width * insets.left).toInt()
                    val top = (full.height * insets.top).toInt()
                    val right = full.width - (full.width * insets.right).toInt()
                    val bottom = full.height - (full.height * insets.bottom).toInt()
                    Bitmap.createBitmap(full, left, top, right - left, bottom - top).asImageBitmap()
                }.getOrNull()
            }
        }
    }
    return state.value
}
