package com.example.ibtech.ui.events

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.example.ibtech.data.repository.EventRepository
import com.example.ibtech.data.repository.FacilityRepository
import com.example.ibtech.data.repository.StatsRepository
import com.example.ibtech.domain.model.Facility
import com.example.ibtech.domain.model.LibraryEvent
import com.example.ibtech.domain.model.StatEventType
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * 행사 상세 화면 상태 (요구사항 명세서 3.5절).
 *
 * [relatedFacility]는 [UsageAnswerViewModel]과 같은 이유로 이용자 화면에 노출 가능한
 * (`isEnabled && floor != UNSET_FLOOR`) 시설일 때만 채운다.
 */
data class EventDetailUiState(
    val isLoaded: Boolean = false,
    val event: LibraryEvent? = null,
    val relatedFacility: Facility? = null
)

class EventDetailViewModel(
    application: Application,
    savedStateHandle: SavedStateHandle
) : AndroidViewModel(application) {

    private val eventId: String = checkNotNull(savedStateHandle["eventId"]) {
        "event_detail 라우트에 eventId 인자가 없습니다."
    }

    private val eventRepository = EventRepository.getInstance(application)
    private val facilityRepository = FacilityRepository.getInstance(application)
    private val statsRepository = StatsRepository.getInstance(application)

    init {
        // 행사 조회수 (요구사항 4.3절, 로드맵 11단계). [uiState]의 combine은 시설 목록 변화로도
        // 재계산되므로, 화면 진입 시 한 번만 기록하려면 별도 일회성 조회가 필요하다.
        viewModelScope.launch {
            val event = eventRepository.events.first().firstOrNull { it.id == eventId && it.isEnabled }
            if (event != null) {
                statsRepository.logEvent(StatEventType.EVENT_VIEW, event.title)
            }
        }
    }

    fun onQrOpened() {
        val title = uiState.value.event?.title ?: return
        viewModelScope.launch { statsRepository.logEvent(StatEventType.EVENT_QR_OPEN, title) }
    }

    val uiState: StateFlow<EventDetailUiState> = combine(
        eventRepository.events,
        facilityRepository.allFacilities
    ) { events, facilities ->
        val event = events.firstOrNull { it.id == eventId && it.isEnabled }
        val relatedFacility = event?.relatedFacilityId?.let { facilityId ->
            facilities.firstOrNull {
                it.id == facilityId && it.isEnabled && it.floor != Facility.UNSET_FLOOR
            }
        }
        EventDetailUiState(isLoaded = true, event = event, relatedFacility = relatedFacility)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = EventDetailUiState()
    )
}
