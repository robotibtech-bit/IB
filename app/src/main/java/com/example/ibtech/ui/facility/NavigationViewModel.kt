package com.example.ibtech.ui.facility

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.example.ibtech.R
import com.example.ibtech.data.repository.FacilityRepository
import com.example.ibtech.data.repository.SettingsRepository
import com.example.ibtech.data.repository.StatsRepository
import com.example.ibtech.domain.model.Facility
import com.example.ibtech.domain.model.LibrarySettings
import com.example.ibtech.domain.model.StatEventType
import com.example.ibtech.domain.usecase.ResolveNavigationTargetUseCase
import com.example.ibtech.robot.NavigationState
import com.example.ibtech.robot.TemiController
import com.example.ibtech.robot.TemiControllerProvider
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * 동행 이동 진행 화면 상태 (요구사항 명세서 2.6절, 로드맵 8장 상태 머신).
 *
 * [hasStarted]가 false면 아직 `goTo`를 호출하지 않은 "확인 전" 단계다 — 목적지 확인
 * 다이얼로그만 보여준다. 승인 즉시 true가 되고 그 뒤로는 [navigationState]가 화면을 그대로
 * 결정한다.
 */
data class NavigationUiState(
    val isLoaded: Boolean = false,
    val facility: Facility? = null,
    val hasStarted: Boolean = false,
    val navigationState: NavigationState = NavigationState.Idle,
    val baseFloor: Int = LibrarySettings.DEFAULT_BASE_FLOOR
)

class NavigationViewModel(
    application: Application,
    savedStateHandle: SavedStateHandle
) : AndroidViewModel(application) {

    private val facilityId: String = checkNotNull(savedStateHandle["facilityId"]) {
        "facility_navigation 라우트에 facilityId 인자가 없습니다."
    }

    private val facilityRepository = FacilityRepository.getInstance(application)
    private val settingsRepository = SettingsRepository.getInstance(application)
    private val statsRepository = StatsRepository.getInstance(application)
    private val controller: TemiController = TemiControllerProvider.current

    private val hasStarted = MutableStateFlow(false)
    private var hasSpokenArrival = false

    val uiState: StateFlow<NavigationUiState> = combine(
        facilityRepository.allFacilities.map { list -> list.firstOrNull { it.id == facilityId } },
        hasStarted,
        controller.navigationState,
        settingsRepository.settings
    ) { facility, started, navState, settings ->
        NavigationUiState(
            isLoaded = true,
            facility = facility,
            hasStarted = started,
            navigationState = navState,
            baseFloor = settings.baseFloor
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = NavigationUiState()
    )

    init {
        // 도착 음성 안내 + 이동 이벤트 통계 기록 (요구사항 4.3절 "이동 성공/중지/실패", 로드맵 11단계).
        viewModelScope.launch {
            controller.navigationState.collect { navState ->
                when (navState) {
                    is NavigationState.Arrived -> {
                        if (!hasSpokenArrival) {
                            hasSpokenArrival = true
                            val facility = uiState.value.facility
                            if (facility != null) {
                                val text = buildNavigationArrivedText(
                                    context = getApplication(),
                                    facility = facility,
                                    baseFloor = uiState.value.baseFloor,
                                    sameFloorTextRes = R.string.navigation_arrived_speech
                                )
                                controller.speak(text)
                            }
                        }
                        statsRepository.logEvent(StatEventType.NAV_SUCCESS, navigationStatEventTarget(navState.target))
                    }

                    is NavigationState.Interrupted -> {
                        statsRepository.logEvent(StatEventType.NAV_CANCELLED, navigationStatEventTarget(navState.target))
                    }

                    is NavigationState.Failed -> {
                        statsRepository.logEvent(StatEventType.NAV_FAILED, navigationStatEventTarget(navState.target))
                    }

                    else -> Unit
                }
            }
        }

        // goTo 응답 콜백이 오지 않는 실기 상황을 대비한 타임아웃 폴백(TemiRepository와 동일한
        // 이유). Fake 구현은 항상 콜백을 스스로 발행하므로 사실상 트리거되지 않는다.
        // collectLatest: 새 Requested 가 emit 되면 이전 요청을 위해 대기 중이던 delay 를 즉시
        // 취소하고 새 target 기준으로 10초를 다시 잰다 (collect 는 이전 delay 가 끝날 때까지
        // 다음 emit 처리를 미뤄 새 요청의 timeout 감시 시작이 늦어지는 문제가 있었다).
        viewModelScope.launch {
            controller.navigationState.collectLatest { navState ->
                if (navState is NavigationState.Requested) {
                    delay(NAVIGATION_TIMEOUT_MILLIS)
                    controller.reportNavigationTimeout(navState.target)
                }
            }
        }
    }

    /** 통계에는 실제 goTo 대상("엘리베이터"/"연결통로")이 아니라 사용자가 선택한 시설명을
     * 남긴다 — target은 중간 경유지일 뿐이라 그대로 쓰면 여러 다른 시설이 전부 같은 이름으로
     * 뭉뚱그려 기록된다. facility가 비어 있는 예외적인 경우에만 target으로 대체한다. */
    private fun navigationStatEventTarget(target: String): String =
        uiState.value.facility?.name ?: target

    /** ViewModel 이 사라질 때(홈 이동/재시도로 새 인스턴스 전환) 남아있는 terminal state를
     * 소비해 Idle 로 되돌린다. 그대로 두면 다음 이동의 새 ViewModel 이 이전 이동의
     * Arrived/Interrupted/Failed 를 즉시 재수신해 도착 TTS·통계가 중복 처리될 수 있다.
     * 아직 진행 중(Requested/Moving)이면 [TemiController.consumeNavigationResult] 자체가
     * 아무 것도 하지 않으므로 실제 주행에는 영향이 없다. */
    override fun onCleared() {
        super.onCleared()
        controller.consumeNavigationResult()
    }

    fun onConfirmStart() {
        // 확인 버튼 연속 탭 재진입 방지 — 이미 시작된 뒤에는 무시한다.
        if (hasStarted.value) return
        val facility = uiState.value.facility ?: return
        hasSpokenArrival = false

        // 사용자가 선택한 목적지(facility)와 실제 goTo() 대상은 분리된다(POI GOTO 경로 처리
        // 요구사항) — 연결통로·기존 계단/지하계단 대상은 "연결통로" 공용 POI로, 그 외 타 층
        // 시설은 "엘리베이터" 공용 POI로, 그 외 기준층 일반 시설만 facility.sourcePoiName으로
        // 직접 이동한다(ResolveNavigationTargetUseCase). 도착 후 안내 문구·층·방향·이미지는 이
        // target과 무관하게 항상 facility 기준으로만 결정된다(buildNavigationArrivedText 등 참고).
        val targetPoi = ResolveNavigationTargetUseCase.poiName(facility, uiState.value.baseFloor)

        // 엘리베이터/연결통로 POI가 temi 지도에 아직 없으면 goTo 자체를 호출하지 않는다 —
        // CanStartEscortUseCase가 이미 같은 기준으로 버튼을 막아 두므로 정상 경로로는 여기까지
        // 오지 않지만, 크래시하거나 엉뚱한 곳으로 이동하지 않도록 한 번 더 확인한다.
        if (targetPoi !in controller.locations.value) return

        // goTo()가 실제로 Temi 에 전달되지 못했으면(연결 끊김 등) 이동 시작 상태로 확정하지
        // 않는다. hasStarted 가 false 로 남으므로 확인 다이얼로그가 계속 보이고, 사용자는
        // 빈 이동 화면 대신 재시도하거나 취소할 수 있다.
        val accepted = controller.goTo(targetPoi)
        if (!accepted) return

        hasStarted.value = true
        // ESCORT_START는 확인 팝업에서 취소할 수도 있으므로 여기, 즉 goTo()가 실제로 수락된
        // 시점에 1회만 기록한다(요구사항 4.3절). FACILITY_REQUEST는 시설 상세 화면에서
        // "동행 안내" 클릭 시점에 이미 기록됐다.
        viewModelScope.launch { statsRepository.logEvent(StatEventType.ESCORT_START, facility.name) }
        controller.speak(buildNavigationStartText(getApplication(), facility, uiState.value.baseFloor))
    }

    fun onStopRequested() {
        val accepted = controller.stopMovement()
        if (!accepted) return

        // stopMovement 이후 최종 상태 콜백(Interrupted 등)이 유실되면 UI가 영구히
        // Requested/Moving 에 남는다. reportStopTimeout()은 그 시점에도 여전히 busy 일 때만
        // 동작하므로, 정상 콜백이 먼저 도착한 경우에는 아무 효과가 없다.
        viewModelScope.launch {
            delay(STOP_TIMEOUT_MILLIS)
            controller.reportStopTimeout()
        }
    }

    companion object {
        private const val NAVIGATION_TIMEOUT_MILLIS = 10_000L
        private const val STOP_TIMEOUT_MILLIS = 10_000L
    }
}
