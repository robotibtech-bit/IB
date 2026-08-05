package com.example.ibtech.ui.admin

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ibtech.data.repository.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** 관리자 로그인 화면 상태 (로드맵 10단계 "비밀번호 보호"). */
data class AdminLoginUiState(
    val password: String = "",
    val error: Boolean = false,
    val loginSuccess: Boolean = false
)

class AdminLoginViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsRepository = SettingsRepository.getInstance(application)

    private val _uiState = MutableStateFlow(AdminLoginUiState())
    val uiState: StateFlow<AdminLoginUiState> = _uiState.asStateFlow()

    fun onPasswordChange(password: String) {
        _uiState.update { it.copy(password = password, error = false) }
    }

    fun onSubmit() {
        val password = _uiState.value.password
        viewModelScope.launch {
            val verified = settingsRepository.verifyAdminPassword(password)
            _uiState.update { it.copy(error = !verified, loginSuccess = verified) }
        }
    }

    /** NavHost가 로그인 성공을 소비한 뒤 호출한다 — 같은 화면으로 되돌아왔을 때 재내비게이션을 막는다. */
    fun onLoginHandled() {
        _uiState.update { it.copy(loginSuccess = false) }
    }
}
