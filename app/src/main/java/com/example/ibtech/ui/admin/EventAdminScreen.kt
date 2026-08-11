package com.example.ibtech.ui.admin

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.ibtech.R
import com.example.ibtech.ui.common.AdminTextField
import com.example.ibtech.ui.common.DecorativeBackground
import com.example.ibtech.ui.common.debounced
import com.example.ibtech.ui.theme.LibraryDimens
import kotlinx.coroutines.flow.SharedFlow

/** 행사·공지 관리 화면 — 홈 "행사 안내" 버튼이 여는 웹페이지 주소만 관리한다. */
@Composable
fun EventAdminScreen(
    uiState: EventAdminUiState,
    events: SharedFlow<Int>,
    onNoticeUrlChange: (String) -> Unit,
    onSaveNoticeUrl: () -> Unit,
    onResetNoticeUrl: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    LaunchedEffect(events) {
        events.collect { resId ->
            Toast.makeText(context, context.getString(resId), Toast.LENGTH_SHORT).show()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        DecorativeBackground(modifier = Modifier.fillMaxSize(), showBookAndPlantDecoration = false)

        if (!uiState.isLoaded) return@Box

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(LibraryDimens.ScreenPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AdminTextField(
                value = uiState.noticeUrl,
                onValueChange = onNoticeUrlChange,
                label = stringResource(R.string.event_admin_field_notice_url),
                keyboardType = KeyboardType.Uri,
                textStyle = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f)
            )
            InlineOutlinedButton(
                text = stringResource(R.string.event_admin_notice_url_reset_action),
                onClick = onResetNoticeUrl
            )
            InlineOutlinedButton(
                text = stringResource(R.string.event_admin_notice_url_save_action),
                onClick = onSaveNoticeUrl
            )
        }
    }
}

/** [com.example.ibtech.ui.common.LibraryOutlinedButton]은 항상 fillMaxWidth라 텍스트 필드 옆에
 * 나란히 못 둔다 — 내용 크기만 차지하는 버튼이 필요한 자리(저장/초기화)에 쓰는 축소판. */
@Composable
private fun InlineOutlinedButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    OutlinedButton(
        onClick = debounced(onClick),
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary),
        modifier = modifier.height(LibraryDimens.SecondaryButtonHeight)
    ) {
        Text(text = text, style = MaterialTheme.typography.titleMedium)
    }
}
