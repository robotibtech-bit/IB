package com.example.ibtech.ui.facility

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.example.ibtech.data.repository.FacilityRepository
import com.example.ibtech.data.repository.SettingsRepository
import com.example.ibtech.domain.model.EscortBlockReason
import com.example.ibtech.domain.model.EscortGate
import com.example.ibtech.domain.model.Facility
import com.example.ibtech.domain.model.GuideOptionSet
import com.example.ibtech.domain.model.LibrarySettings
import com.example.ibtech.domain.usecase.CanStartEscortUseCase
import com.example.ibtech.domain.usecase.ResolveGuideOptionUseCase
import com.example.ibtech.domain.usecase.RobotSnapshot
import com.example.ibtech.robot.TemiControllerProvider
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * 시설 상세 화면 상태 (요구사항 명세서 2.4절).
 *
 * [facility]가 null이면(잘못된 facilityId, 비활성, 미설정) 화면은 [guideOptions]/[escortGate]를
 * 쓰지 않고 안내 실패 상태만 그린다.
 *
 * 목적지 확인 다이얼로그는 이 화면이 아니라 `facility_navigation`(`NavigationViewModel`)이
 * 갖는다(요구사항 2.6절 — "Requested/확인 전" 상태는 그 화면의 몫). 그래서 여기는 `동행 안내`가
 * 눌러도 되는지(escortGate)만 판단하고, 실제 눌렀을 때 이동은 NavHost가 라우트 전환으로 처리한다.
 */
data class FacilityDetailUiState(
    val isLoaded: Boolean = false,
    val facility: Facility? = null,
    val guideOptions: GuideOptionSet = GuideOptionSet.Hidden,
    val escortGate: EscortGate = EscortGate.Blocked(EscortBlockReason.SDK_NOT_READY),
    val permissionRequestInFlight: Boolean = false,
    val baseFloor: Int = LibrarySettings.DEFAULT_BASE_FLOOR
)

class FacilityDetailViewModel(
    application: Application,
    savedStateHandle: SavedStateHandle
) : AndroidViewModel(application) {

    private val facilityId: String = checkNotNull(savedStateHandle["facilityId"]) {
        "facility_detail 라우트에 facilityId 인자가 없습니다."
    }

    private val facilityRepository = FacilityRepository.getInstance(application)
    private val settingsRepository = SettingsRepository.getInstance(application)
    private val temiController = TemiControllerProvider.current

    // 목록 단계에서 걸러지지 않은 잘못된 경로(직접 딥링크, 관리자에서 방금 비활성화 등)를
    // 여기서도 한 번 더 막는다 — 상세 화면은 "노출 가능한" 시설에만 의미가 있다(2.4절 예외).
    private val validFacility = facilityRepository.allFacilities.map { list ->
        list.firstOrNull { it.id == facilityId }
            ?.takeIf { it.isEnabled && it.floor != Facility.UNSET_FLOOR }
    }

    private val robotSnapshot = combine(
        temiController.connectionState,
        temiController.permissionStatus,
        temiController.locations,
        temiController.navigationState
    ) { connection, permission, locations, navigation ->
        RobotSnapshot(connection, permission, locations, navigation.isBusy)
    }

    val uiState: StateFlow<FacilityDetailUiState> = combine(
        validFacility,
        settingsRepository.settings,
        robotSnapshot
    ) { facility, settings, snapshot ->
        if (facility == null) {
            FacilityDetailUiState(isLoaded = true, facility = null)
        } else {
            FacilityDetailUiState(
                isLoaded = true,
                facility = facility,
                guideOptions = ResolveGuideOptionUseCase(facility),
                escortGate = CanStartEscortUseCase(facility, snapshot, settings.baseFloor),
                permissionRequestInFlight = snapshot.permissionStatus.requestInFlight,
                baseFloor = settings.baseFloor
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = FacilityDetailUiState()
    )

    /** "권한 요청" 버튼. 결과는 [uiState]가 `TemiController.permissionStatus` 변화로 자동 반영한다. */
    fun onRequestPermission() {
        val sent = temiController.requestMissingPermissions()
        if (!sent) return

        // 승인/거부 결과 콜백이 유실되면 requestInFlight 가 계속 true 로 남아 버튼이 영구히
        // 잠긴다. reportPermissionRequestTimeout()은 그 시점에도 여전히 요청 중일 때만
        // 동작하므로, 정상 콜백이 먼저 도착한 경우에는 아무 효과가 없다.
        viewModelScope.launch {
            delay(PERMISSION_REQUEST_TIMEOUT_MILLIS)
            temiController.reportPermissionRequestTimeout()
        }
    }

    companion object {
        private const val PERMISSION_REQUEST_TIMEOUT_MILLIS = 10_000L
    }
}
