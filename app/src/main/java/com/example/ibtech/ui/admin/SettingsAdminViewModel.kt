package com.example.ibtech.ui.admin

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ibtech.R
import com.example.ibtech.data.repository.BackupRepository
import com.example.ibtech.data.repository.SettingsRepository
import com.example.ibtech.domain.model.LibrarySettings
import com.example.ibtech.domain.usecase.PasswordChangeValidation
import com.example.ibtech.domain.usecase.ValidatePasswordChangeUseCase
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** 설정(환영문구/무입력시간/기준층/음량/비밀번호/백업) 화면 상태 (로드맵 10단계). */
data class SettingsAdminUiState(
    val isLoaded: Boolean = false,
    val welcomeMessage: String = "",
    val idleTimeoutText: String = "",
    val baseFloorText: String = "",
    val volume: Int = LibrarySettings.DEFAULT_VOLUME,
    val volumeLocked: Boolean = LibrarySettings.DEFAULT_VOLUME_LOCKED,
    val featuredFacilityCount: Int = LibrarySettings.DEFAULT_FEATURED_FACILITY_COUNT,
    /** 도서검색 서버 주소. 비어 있으면 홈 > 책 찾기가 "미설정" 안내를 띄운다. */
    val bookSearchBaseUrl: String = "",
    val bookSearchBaseUrlError: String? = null,
    val idleTimeoutError: String? = null,
    val baseFloorError: String? = null,
    val currentPassword: String = "",
    val newPassword: String = "",
    val confirmPassword: String = "",
    val currentPasswordError: String? = null,
    val newPasswordError: String? = null,
    val lastBackupAt: Long? = null
)

/**
 * 저장/비밀번호변경/백업·복구는 모두 "한 번만 알리면 되는 결과"라 [events]
 * (`SharedFlow<Int>`, 문자열 리소스 id)로 흘려보낸다 — [ui.main.TemiViewModel.alerts]와 같은
 * 상태·사건 분리 원칙을 따른다. 입력값 자체가 잘못된 경우(빈 제목, 형식 오류)는 이 화면에
 * 머무르며 바로 옆에 보여줘야 하므로 [uiState]의 `*Error` 필드로 다룬다.
 */
class SettingsAdminViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsRepository = SettingsRepository.getInstance(application)
    private val backupRepository = BackupRepository.getInstance(application)

    private val _uiState = MutableStateFlow(SettingsAdminUiState())
    val uiState: StateFlow<SettingsAdminUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<Int>()
    val events: SharedFlow<Int> = _events.asSharedFlow()

    init {
        viewModelScope.launch {
            val settings = settingsRepository.settings.first()
            _uiState.update {
                it.copy(
                    isLoaded = true,
                    welcomeMessage = settings.welcomeMessage,
                    idleTimeoutText = settings.idleTimeoutSeconds.toString(),
                    baseFloorText = settings.baseFloor.toString(),
                    volume = settings.volume,
                    volumeLocked = settings.volumeLocked,
                    featuredFacilityCount = settings.featuredFacilityCount,
                    bookSearchBaseUrl = settings.bookSearchBaseUrl,
                    lastBackupAt = backupRepository.lastBackupAt()
                )
            }
        }
    }

    fun onWelcomeMessageChange(value: String) {
        _uiState.update { it.copy(welcomeMessage = value) }
    }

    fun onIdleTimeoutChange(value: String) {
        _uiState.update { it.copy(idleTimeoutText = value, idleTimeoutError = null) }
    }

    fun onBaseFloorChange(value: String) {
        _uiState.update { it.copy(baseFloorText = value, baseFloorError = null) }
    }

    fun onVolumeChange(value: Int) {
        _uiState.update { it.copy(volume = value) }
    }

    fun onVolumeLockedChange(value: Boolean) {
        _uiState.update { it.copy(volumeLocked = value) }
    }

    fun onBookSearchBaseUrlChange(value: String) {
        _uiState.update { it.copy(bookSearchBaseUrl = value, bookSearchBaseUrlError = null) }
    }

    fun onFeaturedFacilityCountChange(value: Int) {
        _uiState.update { it.copy(featuredFacilityCount = value) }
    }

    fun onSaveSettings() {
        val state = _uiState.value
        val app = getApplication<Application>()
        val idleTimeout = state.idleTimeoutText.trim().toIntOrNull()
        val baseFloor = state.baseFloorText.trim().toIntOrNull()

        if (idleTimeout == null || idleTimeout <= 0) {
            _uiState.update { it.copy(idleTimeoutError = app.getString(R.string.admin_error_invalid_number)) }
            return
        }
        if (baseFloor == null) {
            _uiState.update { it.copy(baseFloorError = app.getString(R.string.admin_error_invalid_number)) }
            return
        }
        // 비워 두는 것(기능 끄기)은 허용하되, 넣었다면 http(s) 형식은 맞아야 한다 — 오타를
        // 저장하면 실기에서 "연결 실패"로만 보여 원인을 찾기 어렵다.
        val bookSearchBaseUrl = state.bookSearchBaseUrl.trim().trimEnd('/')
        if (bookSearchBaseUrl.isNotEmpty() &&
            !bookSearchBaseUrl.startsWith("http://") &&
            !bookSearchBaseUrl.startsWith("https://")
        ) {
            _uiState.update {
                it.copy(bookSearchBaseUrlError = app.getString(R.string.settings_admin_error_book_search_url))
            }
            return
        }

        viewModelScope.launch {
            val current = settingsRepository.settings.first()
            settingsRepository.updateSettings(
                current.copy(
                    welcomeMessage = state.welcomeMessage.ifBlank { LibrarySettings.DEFAULT_WELCOME_MESSAGE },
                    bookSearchBaseUrl = bookSearchBaseUrl,
                    idleTimeoutSeconds = idleTimeout,
                    baseFloor = baseFloor,
                    volume = state.volume,
                    volumeLocked = state.volumeLocked,
                    featuredFacilityCount = state.featuredFacilityCount
                )
            )
            _events.emit(R.string.admin_save_success)
        }
    }

    fun onCurrentPasswordChange(value: String) {
        _uiState.update { it.copy(currentPassword = value, currentPasswordError = null) }
    }

    fun onNewPasswordChange(value: String) {
        _uiState.update { it.copy(newPassword = value, newPasswordError = null) }
    }

    fun onConfirmPasswordChange(value: String) {
        _uiState.update { it.copy(confirmPassword = value, newPasswordError = null) }
    }

    fun onChangePassword() {
        val state = _uiState.value
        val app = getApplication<Application>()
        viewModelScope.launch {
            if (!settingsRepository.verifyAdminPassword(state.currentPassword)) {
                _uiState.update { it.copy(currentPasswordError = app.getString(R.string.admin_login_error)) }
                return@launch
            }
            when (ValidatePasswordChangeUseCase(state.newPassword, state.confirmPassword)) {
                PasswordChangeValidation.TooShort -> _uiState.update {
                    it.copy(newPasswordError = app.getString(R.string.settings_admin_password_too_short))
                }

                PasswordChangeValidation.Mismatch -> _uiState.update {
                    it.copy(newPasswordError = app.getString(R.string.settings_admin_password_mismatch))
                }

                PasswordChangeValidation.Valid -> {
                    settingsRepository.changeAdminPassword(state.newPassword)
                    _uiState.update {
                        it.copy(currentPassword = "", newPassword = "", confirmPassword = "")
                    }
                    _events.emit(R.string.settings_admin_password_changed)
                }
            }
        }
    }

    fun onExportBackup() {
        viewModelScope.launch {
            val success = backupRepository.export()
            _uiState.update { it.copy(lastBackupAt = backupRepository.lastBackupAt()) }
            _events.emit(
                if (success) R.string.settings_admin_backup_export_success else R.string.settings_admin_backup_export_failure
            )
        }
    }

    fun onImportBackup() {
        viewModelScope.launch {
            val success = backupRepository.import()
            if (success) {
                val settings = settingsRepository.settings.first()
                _uiState.update {
                    it.copy(
                        welcomeMessage = settings.welcomeMessage,
                        idleTimeoutText = settings.idleTimeoutSeconds.toString(),
                        baseFloorText = settings.baseFloor.toString(),
                        volume = settings.volume,
                        volumeLocked = settings.volumeLocked,
                        featuredFacilityCount = settings.featuredFacilityCount
                    )
                }
            }
            _events.emit(
                if (success) R.string.settings_admin_backup_import_success else R.string.settings_admin_backup_import_failure
            )
        }
    }
}
