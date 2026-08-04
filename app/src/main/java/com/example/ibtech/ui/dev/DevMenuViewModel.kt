package com.example.ibtech.ui.dev

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ibtech.data.repository.FacilityRepository
import com.example.ibtech.domain.model.GuideMode
import com.example.ibtech.robot.FakeOutcome
import com.example.ibtech.robot.TemiControllerProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DevMenuUiState(
    val useFake: Boolean = TemiControllerProvider.useFake,
    val nextOutcome: FakeOutcome = TemiControllerProvider.fake.nextOutcome
)

/**
 * 개발자 메뉴 ViewModel (`BuildConfig.DEBUG` 전용, 로드맵 5단계 "개발자 메뉴에서 재현 가능").
 *
 * [TemiControllerProvider]/`FakeTemiController`는 영구 저장하지 않는 순수 인메모리 토글이라
 * DataStore를 쓰지 않는다 — 앱을 재시작하면 항상 실제 로봇으로 돌아온다. 시설 목록 쪽은
 * [FacilityRepository]에 실제로 저장해야 화면에 뜨므로 Fake를 켤 때 샘플 POI를 바로 노출
 * 가능한 상태로 만든다([FacilityRepository.configureForDevMode]).
 */
class DevMenuViewModel(application: Application) : AndroidViewModel(application) {

    private val facilityRepository = FacilityRepository.getInstance(application)

    private val _uiState = MutableStateFlow(DevMenuUiState())
    val uiState: StateFlow<DevMenuUiState> = _uiState.asStateFlow()

    fun onToggleFake(useFake: Boolean) {
        TemiControllerProvider.useFake = useFake
        if (useFake) {
            TemiControllerProvider.fake.setLocations(SAMPLE_LOCATIONS.map { it.first })
            viewModelScope.launch {
                SAMPLE_LOCATIONS.forEach { (poiName, floor, guideMode) ->
                    facilityRepository.configureForDevMode(poiName, floor, guideMode)
                }
            }
        }
        _uiState.update { it.copy(useFake = useFake) }
    }

    fun onSelectOutcome(outcome: FakeOutcome) {
        TemiControllerProvider.fake.nextOutcome = outcome
        _uiState.update { it.copy(nextOutcome = outcome) }
    }

    companion object {
        // 4단계 실기 테스트에서 확인한 것과 동일하게 1층(기준층)/타 층 POI를 하나씩 둔다.
        private val SAMPLE_LOCATIONS = listOf(
            Triple("입구", 1, GuideMode.ESCORT),
            Triple("1403", 2, GuideMode.LOCATION_ONLY)
        )
    }
}
