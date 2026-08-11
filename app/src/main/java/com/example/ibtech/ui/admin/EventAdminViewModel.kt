package com.example.ibtech.ui.admin

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ibtech.R
import com.example.ibtech.data.repository.SettingsRepository
import com.example.ibtech.domain.model.LibrarySettings
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class EventAdminUiState(
    val isLoaded: Boolean = false,
    val noticeUrl: String = ""
)

/** 행사·공지 관리 화면 — 홈 "행사 안내" 버튼이 여는 웹페이지 주소만 관리한다. */
class EventAdminViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsRepository = SettingsRepository.getInstance(application)
    /** 홈 화면 "행사 안내" 버튼이 여는 URL — [LibrarySettings.eventNoticeUrl]의 편집 중 값. */
    private val noticeUrlText = MutableStateFlow<String?>(null)

    private val _events = MutableSharedFlow<Int>()
    val events: SharedFlow<Int> = _events.asSharedFlow()

    init {
        viewModelScope.launch {
            noticeUrlText.value = settingsRepository.settings.first().eventNoticeUrl
        }
    }

    val uiState: StateFlow<EventAdminUiState> = noticeUrlText.map { noticeUrl ->
        EventAdminUiState(isLoaded = noticeUrl != null, noticeUrl = noticeUrl.orEmpty())
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = EventAdminUiState()
    )

    fun onNoticeUrlChange(value: String) {
        noticeUrlText.value = value
    }

    fun onResetNoticeUrl() {
        noticeUrlText.value = LibrarySettings.DEFAULT_EVENT_NOTICE_URL
    }

    fun onSaveNoticeUrl() {
        val url = noticeUrlText.value.orEmpty().trim().ifBlank { LibrarySettings.DEFAULT_EVENT_NOTICE_URL }
        viewModelScope.launch {
            val current = settingsRepository.settings.first()
            settingsRepository.updateSettings(current.copy(eventNoticeUrl = url))
            noticeUrlText.value = url
            _events.emit(R.string.admin_save_success)
        }
    }
}
