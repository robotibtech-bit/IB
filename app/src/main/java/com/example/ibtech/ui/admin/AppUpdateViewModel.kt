package com.example.ibtech.ui.admin

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ibtech.BuildConfig
import com.example.ibtech.data.repository.AppUpdateRepository
import com.example.ibtech.data.repository.toUpdateErrorMessage
import com.example.ibtech.domain.model.AppUpdateCheckState
import com.example.ibtech.domain.model.AppUpdateUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * [KSH/updater] 프로젝트의 `UpdaterViewModel`을 이식했다. 원본은 별도 앱이 대상 앱을 갱신했지만,
 * 이 화면은 이 앱 자신을 갱신한다.
 */
class AppUpdateViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = AppUpdateRepository.getInstance(application)
    private val _uiState = MutableStateFlow(
        AppUpdateUiState(
            currentVersionName = BuildConfig.VERSION_NAME,
            currentVersionCode = BuildConfig.VERSION_CODE.toLong(),
        )
    )
    val uiState: StateFlow<AppUpdateUiState> = _uiState.asStateFlow()

    init {
        refreshInstallPermission()
        checkForUpdate()
    }

    fun refreshInstallPermission() {
        _uiState.update { it.copy(canInstallPackages = getApplication<Application>().packageManager.canRequestPackageInstalls()) }
    }

    fun checkForUpdate() {
        if (_uiState.value.isDownloading || _uiState.value.checkState == AppUpdateCheckState.CHECKING) return
        viewModelScope.launch {
            _uiState.update { it.copy(checkState = AppUpdateCheckState.CHECKING, statusMessage = "최신 버전을 확인하고 있습니다.") }
            runCatching {
                withContext(Dispatchers.IO) { repository.fetchLatestRelease() }
            }.onSuccess { release ->
                val currentCode = _uiState.value.currentVersionCode
                val state = if (release.versionCode > currentCode) {
                    AppUpdateCheckState.UPDATE_AVAILABLE
                } else {
                    AppUpdateCheckState.UP_TO_DATE
                }
                val message = if (state == AppUpdateCheckState.UPDATE_AVAILABLE) {
                    "새로운 업데이트가 있습니다."
                } else {
                    "최신 버전을 사용하고 있습니다."
                }
                _uiState.update { it.copy(checkState = state, release = release, statusMessage = message) }
            }.onFailure { error ->
                _uiState.update { it.copy(checkState = AppUpdateCheckState.ERROR, statusMessage = error.toUpdateErrorMessage("버전 확인")) }
            }
        }
    }

    fun downloadAndInstall(onInstallReady: (Intent) -> Unit) {
        val release = _uiState.value.release ?: return
        if (!_uiState.value.canDownload) return
        if (!_uiState.value.canInstallPackages) {
            _uiState.update { it.copy(statusMessage = "업데이트를 설치하려면 '알 수 없는 앱 설치' 권한이 필요합니다.") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isDownloading = true, downloadPercent = 0, statusMessage = "업데이트 파일 다운로드 중...") }
            runCatching {
                withContext(Dispatchers.IO) {
                    repository.downloadApk(release) { percent ->
                        _uiState.update { it.copy(downloadPercent = percent) }
                    }
                }
            }.onSuccess { apk ->
                val context = getApplication<Application>()
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", apk)
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, "application/vnd.android.package-archive")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                _uiState.update { it.copy(isDownloading = false, statusMessage = "다운로드가 완료되었습니다. 설치 화면을 엽니다.") }
                onInstallReady(intent)
            }.onFailure { error ->
                _uiState.update { it.copy(isDownloading = false, statusMessage = error.toUpdateErrorMessage("APK 다운로드")) }
            }
        }
    }

    fun installPermissionIntent(): Intent = Intent(
        Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
        Uri.parse("package:${getApplication<Application>().packageName}"),
    )
}
