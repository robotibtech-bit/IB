package com.example.ibtech.domain.model

/** [KSH/updater] 프로젝트의 `ReleaseInfo`를 이식 — GitHub의 latest.json 한 건을 표현한다. */
data class AppReleaseInfo(
    val packageName: String,
    val versionCode: Long,
    val versionName: String,
    val apkUrl: String,
    val releaseNote: String,
)

enum class AppUpdateCheckState { IDLE, CHECKING, UPDATE_AVAILABLE, UP_TO_DATE, ERROR }

data class AppUpdateUiState(
    val checkState: AppUpdateCheckState = AppUpdateCheckState.IDLE,
    val currentVersionName: String = "",
    val currentVersionCode: Long = 0L,
    val release: AppReleaseInfo? = null,
    val statusMessage: String = "",
    val isDownloading: Boolean = false,
    val downloadPercent: Int = 0,
    val canInstallPackages: Boolean = false,
) {
    val canDownload: Boolean
        get() = checkState == AppUpdateCheckState.UPDATE_AVAILABLE && !isDownloading
}
