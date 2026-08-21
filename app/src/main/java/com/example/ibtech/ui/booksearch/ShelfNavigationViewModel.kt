package com.example.ibtech.ui.booksearch

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.example.ibtech.BuildConfig
import com.example.ibtech.R
import com.example.ibtech.data.booksearch.BookSearchApi
import com.example.ibtech.data.booksearch.BookSearchException
import com.example.ibtech.data.repository.SettingsRepository
import com.example.ibtech.domain.model.BookDetail
import com.example.ibtech.domain.model.Facility
import com.example.ibtech.domain.model.LibrarySettings
import com.example.ibtech.domain.model.NavigationRouteType
import com.example.ibtech.domain.usecase.ResolveNavigationTargetUseCase
import com.example.ibtech.robot.NavigationState
import com.example.ibtech.robot.TemiController
import com.example.ibtech.robot.TemiControllerProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * 서가로 데려다줄 수 있는지, 아니면 위치만 알려줄지.
 *
 * 로봇은 자기 층 지도만 갖고 있으므로 층이 곧 "갈 수 있는가"를 가른다. 이 판단은 시설
 * 안내와 완전히 같은 규칙이라 [ResolveNavigationTargetUseCase]를 그대로 쓴다.
 */
enum class ShelfGuideMode {
    /** 같은 층. 서가 POI 로 바로 동행한다. */
    ESCORT,

    /** 다른 층. 엘리베이터까지 데려다주고 나머지는 말로 안내한다. */
    ELEVATOR,

    /** 서버가 층이나 서가를 모른다. 동행하지 않고 안내 문구만 보여 준다. */
    LOCATION_ONLY
}

data class ShelfNavigationUiState(
    val bookTitle: String = "",
    val callNo: String = "",
    val shelfLabel: String = "",
    val room: String = "",
    val shelfFloor: Int? = null,
    val isEstimated: Boolean = false,
    val baseFloor: Int = LibrarySettings.DEFAULT_BASE_FLOOR,
    val guideMode: ShelfGuideMode = ShelfGuideMode.LOCATION_ONLY,
    /** 아직 확인 다이얼로그 단계인지. false 면 goTo 를 부르기 전이다. */
    val hasStarted: Boolean = false,
    val navigationState: NavigationState = NavigationState.Idle,
    /**
     * 디버그 빌드에서 서가 POI가 지도에 없어 다른 POI로 대신 이동한 경우 그 이름.
     * release 에서는 항상 null 이며, 화면에 경고를 띄워 실제 안내와 혼동하지 않게 한다.
     */
    val substitutePoi: String? = null,
    /** 서버에서 받아온 표지·저자·출판사. 아직 못 받았거나 매칭 실패면 null. */
    val detail: BookDetail? = null,
    val isLoadingDetail: Boolean = false,
    /**
     * 관리자 설정(기준층)이 실제로 반영된 상태인지.
     *
     * [guideMode]는 기준층을 알아야 정해진다. 설정은 DataStore 에서 비동기로 오므로 화면이
     * 처음 그려지는 순간에는 아직 없고, 그때의 [guideMode]는 기본값 LOCATION_ONLY 다.
     * 말풍선은 곧 올바른 문구로 바뀌지만 음성은 한 번만 나가므로, 이 값이 true 가 되기
     * 전에는 말하지 않는다.
     */
    val isGuideResolved: Boolean = false
) {
    val canEscort: Boolean
        get() = guideMode != ShelfGuideMode.LOCATION_ONLY
}

/**
 * 검색 결과에서 고른 책의 서가로 안내하는 화면의 상태.
 *
 * 책 정보는 라우트 인자로 받는다 — 이 화면만 다시 열려도(예: 프로세스 종료 후 복원) 서버를
 * 다시 부르지 않고 그릴 수 있어야 하기 때문이다.
 */
class ShelfNavigationViewModel(
    application: Application,
    savedStateHandle: SavedStateHandle
) : AndroidViewModel(application) {

    private val bookTitle: String = savedStateHandle["title"] ?: ""
    private val callNo: String = savedStateHandle["callNo"] ?: ""
    private val locationName: String = savedStateHandle["location"] ?: ""
    private val shelfLabel: String = savedStateHandle["shelfLabel"] ?: locationName
    private val room: String = savedStateHandle["room"] ?: ""
    private val shelfFloor: Int? = savedStateHandle.get<String>("floor")?.toIntOrNull()
    private val isEstimated: Boolean = savedStateHandle.get<String>("estimated") == "1"

    private val settingsRepository = SettingsRepository.getInstance(application)
    private val controller: TemiController = TemiControllerProvider.current

    private val bookId: String = savedStateHandle["bookId"] ?: ""

    private val api = BookSearchApi()
    private val hasStarted = MutableStateFlow(false)
    private val substitutePoi = MutableStateFlow<String?>(null)
    private val detail = MutableStateFlow<BookDetail?>(null)
    private val isLoadingDetail = MutableStateFlow(false)

    /** 안내 문구를 한 번만 말하게 막는다 — 화면이 여러 번 recompose 돼도 [speakGuide]가
     * 여러 번 불릴 수 있어서(요청: "도서 선택할 때 안내할 때 tts로 소리도 나오게"), 실제
     * [TemiController.speak] 호출은 첫 번째 것만 통과시킨다. */
    private var hasSpokenGuide = false

    /** [speakArrival]이 여러 번 불려도 한 번만 말하게 막는다 — navigationState가 Arrived로
     * 고정된 채로도 다른 필드(예: detail 로딩) 변화로 recompose 될 수 있다. */
    private var hasSpokenArrival = false

    val uiState: StateFlow<ShelfNavigationUiState> = combine(
        hasStarted,
        controller.navigationState,
        settingsRepository.settings,
        substitutePoi,
        combine(detail, isLoadingDetail) { d, loading -> d to loading }
    ) { started, navState, settings, substitute, detailPair ->
        ShelfNavigationUiState(
            bookTitle = bookTitle,
            callNo = callNo,
            shelfLabel = shelfLabel,
            room = room,
            shelfFloor = shelfFloor,
            isEstimated = isEstimated,
            baseFloor = settings.baseFloor,
            guideMode = resolveGuideMode(settings.baseFloor),
            hasStarted = started,
            navigationState = navState,
            substitutePoi = substitute,
            detail = detailPair.first,
            isLoadingDetail = detailPair.second,
            // 여기까지 왔다는 것은 settings 가 도착했다는 뜻이다.
            isGuideResolved = true
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ShelfNavigationUiState(
            bookTitle = bookTitle,
            callNo = callNo,
            shelfLabel = shelfLabel,
            room = room,
            shelfFloor = shelfFloor,
            isEstimated = isEstimated
        )
    )

    init {
        // 지도에 어떤 POI 가 있는지 알아야 대체 여부를 판단할 수 있다.
        controller.refreshLocations()
        loadDetail()

        // 도착 음성 안내. NavigationViewModel과 같은 방식으로 ViewModel이 직접
        // navigationState를 구독한다 — Compose LaunchedEffect에 맡기면 화면이 잠깐이라도
        // 컴포즈되지 않는 동안(예: 로봇 도킹 UI가 잠깐 전면에 뜨는 경우) 도착을 놓칠 수 있다.
        //
        // hasStarted를 반드시 같이 확인해야 한다 — controller는 화면마다 새로 만들어지지
        // 않는 공유 객체라, 직전에 다른 책으로 동행했다가 Arrived로 끝난 상태가 그대로
        // 남아 있을 수 있다. 그 상태에서 새 책 화면이 열리자마자(this init이 uiState보다
        // 먼저 실행되던 예전 순서에서는 uiState가 아직 null이라 NullPointerException까지
        // 났었다) 가드 없이 반응하면 로봇이 움직이지도 않았는데 "도착했어요"를 말해 버린다.
        viewModelScope.launch {
            controller.navigationState.collect { navState ->
                if (hasStarted.value && navState is NavigationState.Arrived) speakArrival()
            }
        }
    }

    /**
     * 표지·저자·출판사를 서버에서 받아온다.
     *
     * 실패해도 화면은 그대로 동작한다 — 서가 안내에 필요한 정보는 이미 라우트 인자로
     * 갖고 있고, 보강 정보는 있으면 좋은 부가 정보다.
     */
    private fun loadDetail() {
        if (bookId.isBlank()) return
        isLoadingDetail.value = true
        viewModelScope.launch {
            val baseUrl = settingsRepository.settings.first().bookSearchBaseUrl
            detail.value = try {
                api.detail(baseUrl = baseUrl, bookId = bookId)
            } catch (e: BookSearchException) {
                null
            }
            isLoadingDetail.value = false
        }
    }

    /** 사용자가 "데려다 주세요"를 눌렀을 때. "제가 데려다 드릴게요"는 화면이 뜨자마자가
     * 아니라 실제로 출발하는 이 시점에 말해야 안내 문구와 어긋나지 않는다. */
    fun startEscort() {
        val target = navigationPoi() ?: return
        val actual = substituteIfMissing(target)
        hasStarted.value = true
        controller.speak(getApplication<Application>().getString(R.string.shelf_navigation_speak_start))
        controller.goTo(actual)
    }

    /** [NavigationState.Arrived]가 되면 부른다. 서가로 바로 동행한 경우와 엘리베이터까지만
     * 데려다준 경우는 도착 후 할 말이 다르다 — 후자는 몇 층으로 올라가야 하는지까지
     * 마저 안내한다. */
    private fun speakArrival() {
        if (hasSpokenArrival) return
        hasSpokenArrival = true
        val app = getApplication<Application>()
        val text = if (uiState.value.guideMode == ShelfGuideMode.ELEVATOR) {
            app.getString(R.string.shelf_navigation_speak_arrived_elevator, shelfFloor ?: uiState.value.baseFloor)
        } else {
            app.getString(R.string.shelf_navigation_status_arrived)
        }
        controller.speak(text)
    }

    /**
     * 지도에 없는 POI 를 디버그 빌드에서만 실제 존재하는 POI 로 바꿔 준다.
     *
     * 서가 POI(아동4~1 등)를 아직 지도에 등록하지 않은 단계에서도 동행 화면의 상태 전환을
     * 눈으로 확인할 수 있게 하려는 장치다. release 에서는 아무 일도 하지 않으며, 대체가
     * 일어나면 화면 상단에 경고를 띄워 진짜 안내와 헷갈리지 않게 한다.
     */
    private fun substituteIfMissing(target: String): String {
        if (!BuildConfig.DEBUG) return target
        val known = controller.locations.value
        if (known.isEmpty() || known.contains(target)) {
            substitutePoi.value = null
            return target
        }
        // "home base"는 충전 도킹 전용 지점이라 goTo로 보내면 temi SDK가 근처에서 스스로
        // 중단해 버려(실측: goTo status=abort desc=Abort by user) Arrived까지 가지 않는다.
        // 동행 화면의 상태 전환(도착 포함)을 눈으로 확인하려는 장치이므로 실제로 도착
        // 가능한 POI를 골라야 한다 — 시연 단계라 어린이자료실로 고정하고(있으면), 없으면
        // home base를 뺀 첫 항목으로 대신한다.
        val replacement = known.firstOrNull { it == DEMO_FALLBACK_POI_NAME }
            ?: known.firstOrNull { it != HOME_BASE_POI_NAME }
            ?: known.first()
        substitutePoi.value = replacement
        return replacement
    }

    fun stopEscort() {
        controller.stopMovement()
    }

    /** 화면에 뜬 안내 문구([ShelfNavigationScreen]의 `guideText()`)를 그대로 음성으로도
     * 들려준다 — 말풍선은 이미 있었지만 실제 TTS 재생이 빠져 있었다. */
    fun speakGuide(text: String) {
        if (hasSpokenGuide || text.isBlank()) return
        hasSpokenGuide = true
        controller.speak(text)
    }

    private fun resolveGuideMode(baseFloor: Int): ShelfGuideMode {
        if (locationName.isBlank() || shelfFloor == null) return ShelfGuideMode.LOCATION_ONLY
        return when (asFacility(shelfFloor).routeType(baseFloor)) {
            NavigationRouteType.ELEVATOR -> ShelfGuideMode.ELEVATOR
            else -> ShelfGuideMode.ESCORT
        }
    }

    private fun navigationPoi(): String? {
        val floor = shelfFloor ?: return null
        if (locationName.isBlank()) return null
        val baseFloor = uiState.value.baseFloor
        return ResolveNavigationTargetUseCase.poiName(asFacility(floor), baseFloor)
    }

    /**
     * 서가를 [Facility]로 감싼다.
     *
     * 서가 안내와 시설 안내는 "기준층이면 직접, 아니면 엘리베이터"라는 같은 규칙을 쓴다.
     * 규칙을 복제하는 대신 기존 유스케이스를 그대로 태우려고 어댑터를 둔다 — id 가 시설
     * id 와 겹치지 않으므로 계단·연결통로 예외 목록에는 걸리지 않고, 층 비교 규칙만 탄다.
     */
    private fun asFacility(floor: Int): Facility = Facility(
        id = SHELF_FACILITY_ID_PREFIX + locationName,
        sourcePoiName = locationName,
        name = shelfLabel,
        floor = floor
    )

    private fun Facility.routeType(baseFloor: Int): NavigationRouteType =
        ResolveNavigationTargetUseCase.routeType(this, baseFloor)

    companion object {
        /** 시설 id 와 절대 겹치지 않게 하는 접두어. */
        private const val SHELF_FACILITY_ID_PREFIX = "shelf:"

        /** temi SDK가 자동으로 붙이는 충전 도킹 POI 이름. [substituteIfMissing] 대체 후보에서 뺀다. */
        private const val HOME_BASE_POI_NAME = "home base"

        /** [substituteIfMissing]이 가능하면 우선 고르는 대체 POI. 시연 단계라 대부분의
         * 서가 POI가 속한 자료실로 고정한다(요청: "설정 어린이자료실로 고정해줘 시연용단계니깐"). */
        private const val DEMO_FALLBACK_POI_NAME = "어린이자료실"
    }
}
