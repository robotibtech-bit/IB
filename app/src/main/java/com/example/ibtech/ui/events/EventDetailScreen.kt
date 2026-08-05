package com.example.ibtech.ui.events

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.ibtech.R
import com.example.ibtech.ui.common.DecorativeBackground
import com.example.ibtech.ui.common.EmptyState
import com.example.ibtech.ui.common.LibraryOutlinedButton
import com.example.ibtech.ui.theme.LibraryDimens

/** 행사 상세 화면 (요구사항 명세서 3.5절): 시간·장소·대상·설명과 신청 QR/장소 안내 진입점. */
@Composable
fun EventDetailScreen(
    uiState: EventDetailUiState,
    onRelatedFacilityClick: (String) -> Unit,
    onQrOpened: () -> Unit,
    onGoHome: () -> Unit,
    modifier: Modifier = Modifier
) {
    val event = uiState.event

    when {
        !uiState.isLoaded -> Unit

        event == null -> EmptyState(
            message = stringResource(R.string.events_detail_not_found),
            actionLabel = stringResource(R.string.top_bar_home),
            onAction = onGoHome,
            modifier = modifier.fillMaxSize()
        )

        else -> Box(modifier = modifier.fillMaxSize()) {
            DecorativeBackground(modifier = Modifier.fillMaxSize())

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(LibraryDimens.ScreenPadding),
                verticalArrangement = Arrangement.spacedBy(LibraryDimens.CardSpacing)
            ) {
                Text(
                    text = event.title,
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )

                val dateTimeText = listOfNotNull(event.startDate, event.timeText).joinToString(" ")
                DetailLine(icon = Icons.Filled.Schedule, text = dateTimeText)
                DetailLine(icon = Icons.Filled.Place, text = event.place)
                event.target?.takeIf { it.isNotBlank() }?.let { target ->
                    DetailLine(icon = Icons.Filled.Groups, text = target)
                }

                if (event.description.isNotBlank()) {
                    Text(
                        text = event.description,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (!event.qrUrl.isNullOrBlank()) {
                    QrButton(url = event.qrUrl, onOpened = onQrOpened)
                }

                val relatedFacility = uiState.relatedFacility
                if (relatedFacility != null) {
                    LibraryOutlinedButton(
                        text = stringResource(R.string.events_related_facility_action),
                        icon = Icons.Filled.Place,
                        onClick = { onRelatedFacilityClick(relatedFacility.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailLine(icon: ImageVector, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun QrButton(url: String, onOpened: () -> Unit) {
    val context = LocalContext.current
    val errorMessage = stringResource(R.string.usage_answer_qr_error)
    LibraryOutlinedButton(
        text = stringResource(R.string.events_qr_action),
        icon = Icons.Filled.QrCode,
        onClick = {
            runCatching {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            }.onSuccess {
                onOpened()
            }.onFailure {
                Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show()
            }
        }
    )
}
