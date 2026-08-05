package com.example.ibtech.ui.events

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ibtech.data.repository.EventRepository
import com.example.ibtech.domain.model.LibraryEvent
import com.example.ibtech.domain.model.LibraryNotice
import com.example.ibtech.domain.usecase.ResolveEventListUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** 오늘의 행사 목록 화면 상태 (요구사항 명세서 3.5절, 로드맵 9단계). */
data class EventsUiState(
    val isLoaded: Boolean = false,
    val notices: List<LibraryNotice> = emptyList(),
    val todayEvents: List<LibraryEvent> = emptyList(),
    val upcomingEvents: List<LibraryEvent> = emptyList()
) {
    val hasAnyEvent: Boolean get() = todayEvents.isNotEmpty() || upcomingEvents.isNotEmpty()
}

class EventsViewModel(application: Application) : AndroidViewModel(application) {

    private val eventRepository = EventRepository.getInstance(application)

    val uiState: StateFlow<EventsUiState> = combine(
        eventRepository.events,
        eventRepository.notices
    ) { events, notices ->
        val result = ResolveEventListUseCase(events, today = todayDateString())
        EventsUiState(
            isLoaded = true,
            notices = notices.filter { it.isEnabled }.sortedBy { it.sortOrder },
            todayEvents = result.todayEvents,
            upcomingEvents = result.upcomingEvents
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = EventsUiState()
    )

    private fun todayDateString(): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.KOREA).format(Date())
}
