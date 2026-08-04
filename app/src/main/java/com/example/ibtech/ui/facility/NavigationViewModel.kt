package com.example.ibtech.ui.facility

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.example.ibtech.R
import com.example.ibtech.data.repository.FacilityRepository
import com.example.ibtech.domain.model.Facility
import com.example.ibtech.robot.NavigationState
import com.example.ibtech.robot.TemiController
import com.example.ibtech.robot.TemiControllerProvider
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
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
    val navigationState: NavigationState = NavigationState.Idle
)

class NavigationViewModel(
    application: Application,
    savedStateHandle: SavedStateHandle
) : AndroidViewModel(application) {

    private val facilityId: String = checkNotNull(savedStateHandle["facilityId"]) {
        "facility_navigation 라우트에 facilityId 인자가 없습니다."
    }

    private val facilityRepository = FacilityRepository.getInstance(application)
    private val controller: TemiController = TemiControllerProvider.current

    private val hasStarted = MutableStateFlow(false)
    private var hasSpokenArrival = false

    val uiState: StateFlow<NavigationUiState> = combine(
        facilityRepository.allFacilities.map { list -> list.firstOrNull { it.id == facilityId } },
        hasStarted,
        controller.navigationState
    ) { facility, started, navState ->
        NavigationUiState(
            isLoaded = true,
            facility = facility,
            hasStarted = started,
            navigationState = navState
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = NavigationUiState()
    )

    init {
        // 도착 음성 안내 + 이동 이벤트 로그(11단계 StatisticsRepository가 이 지점에 훅을 붙인다).
        viewModelScope.launch {
            controller.navigationState.collect { navState ->
                when (navState) {
                    is NavigationState.Arrived -> {
                        if (!hasSpokenArrival) {
                            hasSpokenArrival = true
                            speak(R.string.navigation_arrived_speech)
                        }
                        Log.i(TAG, "이동 성공 target=${navState.target}")
                    }

                    is NavigationState.Interrupted -> {
                        Log.i(TAG, "이동 중지 target=${navState.target} issue=${navState.issue}")
                    }

                    is NavigationState.Failed -> {
                        Log.i(TAG, "이동 실패 target=${navState.target} issue=${navState.issue}")
                    }

                    else -> Unit
                }
            }
        }

        // goTo 응답 콜백이 오지 않는 실기 상황을 대비한 타임아웃 폴백(TemiRepository와 동일한
        // 이유). Fake 구현은 항상 콜백을 스스로 발행하므로 사실상 트리거되지 않는다.
        viewModelScope.launch {
            controller.navigationState.collect { navState ->
                if (navState is NavigationState.Requested) {
                    delay(NAVIGATION_TIMEOUT_MILLIS)
                    controller.reportNavigationTimeout(navState.target)
                }
            }
        }
    }

    fun onConfirmStart() {
        val facility = uiState.value.facility ?: return
        hasSpokenArrival = false
        hasStarted.value = true
        controller.goTo(facility.sourcePoiName)
        speak(R.string.navigation_start_speech)
    }

    fun onStopRequested() {
        controller.stopMovement()
    }

    private fun speak(resId: Int, vararg args: Any) {
        val text = getApplication<Application>().getString(resId, *args)
        controller.speak(text)
    }

    companion object {
        private const val TAG = "NavigationViewModel"
        private const val NAVIGATION_TIMEOUT_MILLIS = 10_000L
    }
}
