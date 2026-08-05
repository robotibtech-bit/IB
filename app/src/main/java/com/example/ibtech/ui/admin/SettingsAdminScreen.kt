package com.example.ibtech.ui.admin

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.ibtech.R
import com.example.ibtech.domain.model.LibrarySettings
import com.example.ibtech.ui.common.AdminTextField
import com.example.ibtech.ui.common.DecorativeBackground
import com.example.ibtech.ui.common.LibraryOutlinedButton
import com.example.ibtech.ui.common.LibraryPrimaryButton
import com.example.ibtech.ui.theme.LibraryDimens
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.flow.SharedFlow

/** 설정 화면 (로드맵 10단계): 환영문구·무입력시간·기준층·음량, 비밀번호 변경, JSON 백업/복구. */
@Composable
fun SettingsAdminScreen(
    uiState: SettingsAdminUiState,
    events: SharedFlow<Int>,
    onWelcomeMessageChange: (String) -> Unit,
    onIdleTimeoutChange: (String) -> Unit,
    onBaseFloorChange: (String) -> Unit,
    onVolumeChange: (Int) -> Unit,
    onFeaturedFacilityCountChange: (Int) -> Unit,
    onSaveSettings: () -> Unit,
    onCurrentPasswordChange: (String) -> Unit,
    onNewPasswordChange: (String) -> Unit,
    onConfirmPasswordChange: (String) -> Unit,
    onChangePassword: () -> Unit,
    onExportBackup: () -> Unit,
    onImportBackup: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    LaunchedEffect(events) {
        events.collect { resId ->
            Toast.makeText(context, context.getString(resId), Toast.LENGTH_SHORT).show()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        DecorativeBackground(modifier = Modifier.fillMaxSize())

        if (!uiState.isLoaded) return@Box

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(LibraryDimens.ScreenPadding),
            verticalArrangement = Arrangement.spacedBy(LibraryDimens.CardSpacing)
        ) {
            AdminTextField(
                value = uiState.welcomeMessage,
                onValueChange = onWelcomeMessageChange,
                label = stringResource(R.string.settings_admin_field_welcome_message),
                singleLine = false,
                minLines = 2
            )
            AdminTextField(
                value = uiState.idleTimeoutText,
                onValueChange = onIdleTimeoutChange,
                label = stringResource(R.string.settings_admin_field_idle_timeout),
                errorText = uiState.idleTimeoutError,
                keyboardType = KeyboardType.Number
            )
            AdminTextField(
                value = uiState.baseFloorText,
                onValueChange = onBaseFloorChange,
                label = stringResource(R.string.settings_admin_field_base_floor),
                errorText = uiState.baseFloorError,
                keyboardType = KeyboardType.Number
            )

            Text(
                text = stringResource(R.string.settings_admin_field_volume, uiState.volume),
                style = MaterialTheme.typography.titleMedium
            )
            Slider(
                value = uiState.volume.toFloat(),
                onValueChange = { onVolumeChange(it.toInt()) },
                valueRange = 0f..100f
            )

            Text(
                text = stringResource(R.string.settings_admin_field_featured_count),
                style = MaterialTheme.typography.titleMedium
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LibrarySettings.FEATURED_FACILITY_COUNT_OPTIONS.forEach { count ->
                    FilterChip(
                        selected = uiState.featuredFacilityCount == count,
                        onClick = { onFeaturedFacilityCountChange(count) },
                        label = { Text(stringResource(R.string.settings_admin_featured_count_option, count)) }
                    )
                }
            }

            LibraryPrimaryButton(
                text = stringResource(R.string.settings_admin_save_action),
                onClick = onSaveSettings
            )

            HorizontalDivider()

            Text(text = stringResource(R.string.settings_admin_password_section_title), style = MaterialTheme.typography.titleLarge)
            AdminTextField(
                value = uiState.currentPassword,
                onValueChange = onCurrentPasswordChange,
                label = stringResource(R.string.settings_admin_field_current_password),
                errorText = uiState.currentPasswordError,
                keyboardType = KeyboardType.NumberPassword
            )
            AdminTextField(
                value = uiState.newPassword,
                onValueChange = onNewPasswordChange,
                label = stringResource(R.string.settings_admin_field_new_password),
                errorText = uiState.newPasswordError,
                keyboardType = KeyboardType.NumberPassword
            )
            AdminTextField(
                value = uiState.confirmPassword,
                onValueChange = onConfirmPasswordChange,
                label = stringResource(R.string.settings_admin_field_confirm_password),
                keyboardType = KeyboardType.NumberPassword
            )
            LibraryOutlinedButton(
                text = stringResource(R.string.settings_admin_change_password_action),
                onClick = onChangePassword
            )

            HorizontalDivider()

            Text(text = stringResource(R.string.settings_admin_backup_section_title), style = MaterialTheme.typography.titleLarge)
            Text(
                text = uiState.lastBackupAt?.let { formatBackupTime(it) }
                    ?: stringResource(R.string.settings_admin_last_backup_none),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            LibraryOutlinedButton(
                text = stringResource(R.string.settings_admin_backup_export_action),
                onClick = onExportBackup
            )
            LibraryOutlinedButton(
                text = stringResource(R.string.settings_admin_backup_import_action),
                onClick = onImportBackup
            )
        }
    }
}

@Composable
private fun formatBackupTime(epochMillis: Long): String {
    val formatted = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.KOREA).format(Date(epochMillis))
    return stringResource(R.string.settings_admin_last_backup_format, formatted)
}
