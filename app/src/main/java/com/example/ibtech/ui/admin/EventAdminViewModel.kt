package com.example.ibtech.ui.admin

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ibtech.R
import com.example.ibtech.data.repository.EventRepository
import com.example.ibtech.data.repository.FacilityRepository
import com.example.ibtech.domain.model.Facility
import com.example.ibtech.domain.model.LibraryEvent
import com.example.ibtech.domain.model.LibraryNotice
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private val DATE_FORMAT_REGEX = Regex("""\d{4}-\d{2}-\d{2}""")

data class EventDraft(
    val id: String? = null,
    val title: String = "",
    val startDate: String = "",
    val endDate: String = "",
    val timeText: String = "",
    val place: String = "",
    val target: String = "",
    val description: String = "",
    val qrUrl: String = "",
    val relatedFacilityId: String? = null,
    val isEnabled: Boolean = true,
    val sortOrderText: String = "0",
    val titleError: String? = null,
    val startDateError: String? = null
)

data class NoticeDraft(
    val id: String? = null,
    val text: String = "",
    val isEnabled: Boolean = true,
    val sortOrderText: String = "0",
    val textError: String? = null
)

sealed interface EventAdminDraft {
    data class Event(val draft: EventDraft) : EventAdminDraft
    data class Notice(val draft: NoticeDraft) : EventAdminDraft
}

data class EventAdminUiState(
    val isLoaded: Boolean = false,
    val events: List<LibraryEvent> = emptyList(),
    val notices: List<LibraryNotice> = emptyList(),
    val facilities: List<Facility> = emptyList(),
    val editingDraft: EventAdminDraft? = null
)

/** 행사/공지 관리 화면 (로드맵 10단계). */
class EventAdminViewModel(application: Application) : AndroidViewModel(application) {

    private val eventRepository = EventRepository.getInstance(application)
    private val facilityRepository = FacilityRepository.getInstance(application)
    private val editingDraft = MutableStateFlow<EventAdminDraft?>(null)

    val uiState: StateFlow<EventAdminUiState> = combine(
        eventRepository.events,
        eventRepository.notices,
        facilityRepository.visibleFacilities,
        editingDraft
    ) { events, notices, facilities, draft ->
        EventAdminUiState(
            isLoaded = true,
            events = events.sortedBy { it.sortOrder },
            notices = notices.sortedBy { it.sortOrder },
            facilities = facilities,
            editingDraft = draft
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = EventAdminUiState()
    )

    fun onDismissDialog() {
        editingDraft.value = null
    }

    // ---- 행사 ----

    private fun updateEventDraft(transform: (EventDraft) -> EventDraft) {
        editingDraft.update { current -> (current as? EventAdminDraft.Event)?.copy(draft = transform(current.draft)) }
    }

    fun onAddEvent() {
        editingDraft.value = EventAdminDraft.Event(EventDraft())
    }

    fun onEditEvent(event: LibraryEvent) {
        editingDraft.value = EventAdminDraft.Event(
            EventDraft(
                id = event.id,
                title = event.title,
                startDate = event.startDate,
                endDate = event.endDate.orEmpty(),
                timeText = event.timeText.orEmpty(),
                place = event.place,
                target = event.target.orEmpty(),
                description = event.description,
                qrUrl = event.qrUrl.orEmpty(),
                relatedFacilityId = event.relatedFacilityId,
                isEnabled = event.isEnabled,
                sortOrderText = event.sortOrder.toString()
            )
        )
    }

    fun onEventTitleChange(value: String) = updateEventDraft { it.copy(title = value, titleError = null) }
    fun onEventStartDateChange(value: String) = updateEventDraft { it.copy(startDate = value, startDateError = null) }
    fun onEventEndDateChange(value: String) = updateEventDraft { it.copy(endDate = value) }
    fun onEventTimeTextChange(value: String) = updateEventDraft { it.copy(timeText = value) }
    fun onEventPlaceChange(value: String) = updateEventDraft { it.copy(place = value) }
    fun onEventTargetChange(value: String) = updateEventDraft { it.copy(target = value) }
    fun onEventDescriptionChange(value: String) = updateEventDraft { it.copy(description = value) }
    fun onEventQrUrlChange(value: String) = updateEventDraft { it.copy(qrUrl = value) }
    fun onEventFacilityChange(facilityId: String?) = updateEventDraft { it.copy(relatedFacilityId = facilityId) }
    fun onEventEnabledChange(value: Boolean) = updateEventDraft { it.copy(isEnabled = value) }
    fun onEventSortOrderChange(value: String) = updateEventDraft { it.copy(sortOrderText = value) }

    fun onSaveEventDraft() {
        val draft = (editingDraft.value as? EventAdminDraft.Event)?.draft ?: return
        val app = getApplication<Application>()
        if (draft.title.isBlank()) {
            updateEventDraft { it.copy(titleError = app.getString(R.string.admin_error_blank_title)) }
            return
        }
        if (!DATE_FORMAT_REGEX.matches(draft.startDate.trim())) {
            updateEventDraft { it.copy(startDateError = app.getString(R.string.admin_error_invalid_date)) }
            return
        }
        val sortOrder = draft.sortOrderText.trim().toIntOrNull() ?: 0
        val event = LibraryEvent(
            id = draft.id ?: "event_${System.currentTimeMillis()}",
            title = draft.title.trim(),
            startDate = draft.startDate.trim(),
            endDate = draft.endDate.trim().ifBlank { null },
            timeText = draft.timeText.trim().ifBlank { null },
            place = draft.place.trim(),
            target = draft.target.trim().ifBlank { null },
            description = draft.description.trim(),
            qrUrl = draft.qrUrl.trim().ifBlank { null },
            relatedFacilityId = draft.relatedFacilityId,
            isEnabled = draft.isEnabled,
            sortOrder = sortOrder
        )
        viewModelScope.launch {
            eventRepository.upsertEvent(event)
            editingDraft.value = null
        }
    }

    fun onDeleteEvent(id: String) {
        viewModelScope.launch { eventRepository.deleteEvent(id) }
    }

    // ---- 공지 ----

    private fun updateNoticeDraft(transform: (NoticeDraft) -> NoticeDraft) {
        editingDraft.update { current -> (current as? EventAdminDraft.Notice)?.copy(draft = transform(current.draft)) }
    }

    fun onAddNotice() {
        editingDraft.value = EventAdminDraft.Notice(NoticeDraft())
    }

    fun onEditNotice(notice: LibraryNotice) {
        editingDraft.value = EventAdminDraft.Notice(
            NoticeDraft(
                id = notice.id,
                text = notice.text,
                isEnabled = notice.isEnabled,
                sortOrderText = notice.sortOrder.toString()
            )
        )
    }

    fun onNoticeTextChange(value: String) = updateNoticeDraft { it.copy(text = value, textError = null) }
    fun onNoticeEnabledChange(value: Boolean) = updateNoticeDraft { it.copy(isEnabled = value) }
    fun onNoticeSortOrderChange(value: String) = updateNoticeDraft { it.copy(sortOrderText = value) }

    fun onSaveNoticeDraft() {
        val draft = (editingDraft.value as? EventAdminDraft.Notice)?.draft ?: return
        if (draft.text.isBlank()) {
            updateNoticeDraft { it.copy(textError = getApplication<Application>().getString(R.string.admin_error_blank_title)) }
            return
        }
        val sortOrder = draft.sortOrderText.trim().toIntOrNull() ?: 0
        val notice = LibraryNotice(
            id = draft.id ?: "notice_${System.currentTimeMillis()}",
            text = draft.text.trim(),
            isEnabled = draft.isEnabled,
            sortOrder = sortOrder
        )
        viewModelScope.launch {
            eventRepository.upsertNotice(notice)
            editingDraft.value = null
        }
    }

    fun onDeleteNotice(id: String) {
        viewModelScope.launch { eventRepository.deleteNotice(id) }
    }
}
