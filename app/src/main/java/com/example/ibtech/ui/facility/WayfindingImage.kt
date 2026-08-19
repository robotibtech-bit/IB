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
import com.example.ibtech.domain.model.BasementStairsWayfindingOverride
import com.example.ibtech.domain.model.ElevatorWayfindingOverride
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
//
// STAIRS_LANDING_IMAGE_PATH("11_여자화장실옆계단.png")는 같은 wayfinding/ 폴더에 있지만 다른
// 10장과 여백 비율이 다르다(실측: 위 약 16%, 아래 약 5%, 좌 약 5%, 우 약 1% — "1F" 글씨가 박스
// 위쪽 멀리 떨어져 있어 위쪽 여백이 훨씬 크다) — CORRIDOR_CROP을 그대로 적용하면 아래쪽을
// 19%나 잘라내 실제 평면도 내용이 잘린다. 그래서 경로를 정확히 매칭해 전용 크롭을 따로 둔다.
private data class WayfindingCropInsets(val left: Float, val top: Float, val right: Float, val bottom: Float)

private val CORRIDOR_CROP = WayfindingCropInsets(left = 0.025f, top = 0.035f, right = 0.045f, bottom = 0.19f)
private val STAIRS_LANDING_CROP = WayfindingCropInsets(left = 0.02f, top = 0.02f, right = 0.005f, bottom = 0.03f)
private val NO_CROP = WayfindingCropInsets(left = 0f, top = 0f, right = 0f, bottom = 0f)

/** 계단 이용 안내(위/아래 공통) 첫 장면 — 연결통로 도착 지점에서 계단 앞(여자화장실 옆)까지 가는
 * 길을 보여준다. 예전에는 "여름강의실" 시설의 연결통로 안내도([WayfindingCorridorOverride])를
 * 재사용했지만, 목적지 표시가 여름강의실이라 계단으로 가라는 의도가 명확하지 않았다(요청: "그거
 * 대신 …11_여자화장실옆계단 이미지를 이용해서 안내해줘") — 계단 시설과 무관한 전용 이미지라
 * [WayfindingCorridorOverride]의 시설별 맵이 아니라 여기 상수로 직접 관리한다. */
private const val STAIRS_LANDING_IMAGE_PATH = "wayfinding/11_여자화장실옆계단.png"

private fun wayfindingCropInsetsForPath(assetPath: String): WayfindingCropInsets = when {
    assetPath == STAIRS_LANDING_IMAGE_PATH -> STAIRS_LANDING_CROP
    assetPath.startsWith("wayfinding/") -> CORRIDOR_CROP
    else -> NO_CROP
}

/** 안내도가 여러 장일 때(계단 안내도 대상) 몇 ms마다 다음 장으로 넘길지. */
private const val WAYFINDING_CYCLE_INTERVAL_MS = 6_000L

/**
 * [facilityId]가 안내도 대상이면 순서대로 보여줄 asset 경로 목록을, 대상이 아니면 빈 목록을
 * 돌려준다.
 *
 * 계단 안내도 대상([StairsWayfindingOverride], [BasementStairsWayfindingOverride])은 로봇이
 * 실제로 데려다주는 지점(연결통로)과 안내도가 그리는 시작점(계단 앞)이 서로 다르다 — 연결통로에
 * 도착한 다음 계단까지 먼저 걸어가야 하므로, 계단 앞까지 가는 길을 보여주는
 * [STAIRS_LANDING_IMAGE_PATH]를 먼저 넣고 그 시설 전용 계단 안내도를 이어 붙인다(요청: "안내도와
 * 반복적으로 표시" — 지하 계단도 "1열람실을 안내할 때와 똑같은 루틴, 층만 지하로 바뀐 것"이라
 * 같은 방식을 그대로 쓴다). 엘리베이터 안내도 대상([ElevatorWayfindingOverride])은 엘리베이터가
 * 목적지 층까지 바로 데려다주므로 다른 이미지와 이어 붙일 필요 없이 그 시설 전용 안내도 한 장만
 * 보여준다. 그 외(연결통로 대상)도 기존처럼 한 장뿐이다.
 */
fun wayfindingImageAssetPaths(facilityId: String): List<String> {
    val stairsPath = StairsWayfindingOverride.wayfindingImageAssetPath(facilityId)
        ?: BasementStairsWayfindingOverride.wayfindingImageAssetPath(facilityId)
    if (stairsPath != null) {
        return listOf(STAIRS_LANDING_IMAGE_PATH, stairsPath)
    }
    val elevatorPath = ElevatorWayfindingOverride.wayfindingImageAssetPath(facilityId)
    if (elevatorPath != null) {
        return listOf(elevatorPath)
    }
    return listOfNotNull(WayfindingCorridorOverride.wayfindingImageAssetPath(facilityId))
}

/**
 * [facilityId]가 안내도 대상이면 그중 첫 번째 asset 경로를 돌려준다 — "안내도가 있는 시설인지"만
 * 판단하면 되는 곳(예: [NavigationProgressScreen]의 레이아웃 분기)에서 쓴다.
 */
fun wayfindingImageAssetPath(facilityId: String): String? = wayfindingImageAssetPaths(facilityId).firstOrNull()

/** [wayfindingBitmapCache]에 동시에 들고 있을 최대 이미지 수. 안내도 원본은 대략 1880×834~
 * 2155×730 해상도라 디코딩된 비트맵 하나가 수 MB에 달한다 — 전체 안내도(수십 장)를 무기한 다
 * 캐시하면 메모리 최적화 취지에 어긋난다. 실제 사용 패턴은 한 번에 한 시설(계단 대상이면 2장)만
 * 반복해서 순환 표시하는 것이므로, 최근 방문한 몇 시설 분량만 남기면 "같은 화면에 머무는 동안
 * 반복 디코딩"은 완전히 없어지면서 메모리 상한도 지킨다. */
private const val WAYFINDING_BITMAP_CACHE_CAPACITY = 8

/** asset 경로별 디코딩·크롭 결과의 LRU 캐시 — 안내도가 여러 장인 시설은 [WAYFINDING_CYCLE_INTERVAL_MS]
 * 마다 같은 파일을 다시 읽게 되는데, 캐시가 없으면 화면에 머무는 내내 몇 초마다 asset 디코딩(디스크
 * I/O + 비트맵 할당)이 반복된다(최적화: "실행 속도/반응성, 메모리/배터리 사용"). 번들 asset은 앱
 * 설치 중 바뀌지 않으므로 값 자체는 무기한 캐시해도 안전하지만, 개수는 [WAYFINDING_BITMAP_CACHE_CAPACITY]로
 * 제한한다. `LinkedHashMap`은 스레드 안전하지 않아 모든 접근을 `synchronized`로 감싼다. */
private val wayfindingBitmapCache = object : LinkedHashMap<String, ImageBitmap?>(
    WAYFINDING_BITMAP_CACHE_CAPACITY, 0.75f, true
) {
    override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, ImageBitmap?>): Boolean =
        size > WAYFINDING_BITMAP_CACHE_CAPACITY
}

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
            val cached = synchronized(wayfindingBitmapCache) { wayfindingBitmapCache[path] }
            cached ?: withContext(Dispatchers.IO) {
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
            }.also { decoded -> synchronized(wayfindingBitmapCache) { wayfindingBitmapCache[path] = decoded } }
        }
    }
    return state.value
}
