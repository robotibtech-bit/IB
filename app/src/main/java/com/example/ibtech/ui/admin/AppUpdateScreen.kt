package com.example.ibtech.ui.admin

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.ibtech.domain.model.AppUpdateCheckState
import com.example.ibtech.domain.model.AppUpdateUiState
import com.example.ibtech.ui.common.DecorativeBackground
import com.example.ibtech.ui.common.LibraryOutlinedButton
import com.example.ibtech.ui.common.LibraryPrimaryButton
import com.example.ibtech.ui.theme.LibraryDimens

/** 앱 업데이트 화면 (원본 [KSH/updater] 프로젝트의 `UpdaterScreen` 이식). */
@Composable
fun AppUpdateScreen(
    uiState: AppUpdateUiState,
    onCheckForUpdate: () -> Unit,
    onOpenInstallPermission: () -> Unit,
    onDownloadAndInstall: (onInstallReady: (Intent) -> Unit) -> Unit,
    onInstallApk: (Intent) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        DecorativeBackground(modifier = Modifier.fillMaxSize(), showBookAndPlantDecoration = false)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(LibraryDimens.ScreenPadding),
            verticalArrangement = Arrangement.spacedBy(LibraryDimens.CardSpacing)
        ) {
            InfoRow("현재 버전", "${uiState.currentVersionName} (${uiState.currentVersionCode})")
            InfoRow(
                "최신 버전",
                uiState.release?.let { "${it.versionName} (${it.versionCode})" } ?: "-"
            )

            if (uiState.release != null) {
                Text(text = "업데이트 내용", style = MaterialTheme.typography.titleMedium)
                Text(text = uiState.release.releaseNote, style = MaterialTheme.typography.bodyMedium)
            }

            if (uiState.statusMessage.isNotBlank()) {
                Text(text = uiState.statusMessage, style = MaterialTheme.typography.bodyMedium)
            }

            if (uiState.checkState == AppUpdateCheckState.CHECKING) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    CircularProgressIndicator()
                    Text(text = "확인 중...", style = MaterialTheme.typography.bodyMedium)
                }
            }

            if (uiState.isDownloading) {
                Text(
                    text = "업데이트 파일 다운로드 중... ${uiState.downloadPercent}%",
                    style = MaterialTheme.typography.bodyMedium
                )
                LinearProgressIndicator(
                    progress = { uiState.downloadPercent / 100f },
                    modifier = Modifier.fillMaxWidth().height(8.dp)
                )
            }

            if (!uiState.canInstallPackages && uiState.checkState == AppUpdateCheckState.UPDATE_AVAILABLE) {
                Text(
                    text = "업데이트를 설치하려면 '알 수 없는 앱 설치' 권한이 필요합니다.",
                    style = MaterialTheme.typography.bodyMedium
                )
                LibraryOutlinedButton(text = "설치 권한 설정", onClick = onOpenInstallPermission)
            }

            LibraryOutlinedButton(
                text = "업데이트 확인",
                onClick = onCheckForUpdate,
                enabled = !uiState.isDownloading && uiState.checkState != AppUpdateCheckState.CHECKING
            )

            LibraryPrimaryButton(
                text = "업데이트 다운로드",
                onClick = { onDownloadAndInstall(onInstallApk) },
                enabled = uiState.canDownload && uiState.canInstallPackages
            )
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(text = label, style = MaterialTheme.typography.titleMedium)
        Text(text = value, style = MaterialTheme.typography.bodyLarge)
    }
}
