package com.example.ibtech.ui.usage

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.example.ibtech.data.repository.FacilityRepository
import com.example.ibtech.data.repository.UsageRepository
import com.example.ibtech.domain.model.Facility
import com.example.ibtech.domain.model.UsageTopic
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/**
 * 이용방법 답변 화면 상태 (요구사항 명세서 2.9절).
 *
 * [relatedFacility]는 이용자 화면에 노출 가능한(`isEnabled && floor != UNSET_FLOOR`) 시설일
 * 때만 채운다 — 관리자가 감춘 시설을 연결해 뒀다면 "관련 시설 없음"과 동일하게 버튼을 숨긴다.
 */
data class UsageAnswerUiState(
    val isLoaded: Boolean = false,
    val topic: UsageTopic? = null,
    val relatedFacility: Facility? = null,
    val showStaffHelp: Boolean = false
)

class UsageAnswerViewModel(
    application: Application,
    savedStateHandle: SavedStateHandle
) : AndroidViewModel(application) {

    private val topicId: String = checkNotNull(savedStateHandle["topicId"]) {
        "usage_answer 라우트에 topicId 인자가 없습니다."
    }

    private val usageRepository = UsageRepository.getInstance(application)
    private val facilityRepository = FacilityRepository.getInstance(application)

    private val showStaffHelp = MutableStateFlow(false)

    val uiState: StateFlow<UsageAnswerUiState> = combine(
        usageRepository.topics,
        facilityRepository.allFacilities,
        showStaffHelp
    ) { topics, facilities, staffHelpVisible ->
        val topic = topics.firstOrNull { it.id == topicId && it.parentId != null }
        val relatedFacility = topic?.relatedFacilityId?.let { facilityId ->
            facilities.firstOrNull {
                it.id == facilityId && it.isEnabled && it.floor != Facility.UNSET_FLOOR
            }
        }
        UsageAnswerUiState(
            isLoaded = true,
            topic = topic,
            relatedFacility = relatedFacility,
            showStaffHelp = staffHelpVisible
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = UsageAnswerUiState()
    )

    fun onStaffHelpClick() {
        showStaffHelp.value = true
    }

    fun onDismissStaffHelp() {
        showStaffHelp.value = false
    }
}
