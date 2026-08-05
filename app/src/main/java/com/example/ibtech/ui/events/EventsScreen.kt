package com.example.ibtech.ui.events

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.ibtech.R
import com.example.ibtech.domain.model.LibraryEvent
import com.example.ibtech.domain.model.LibraryNotice
import com.example.ibtech.ui.common.DecorativeBackground
import com.example.ibtech.ui.common.EmptyState
import com.example.ibtech.ui.common.LibraryCard
import com.example.ibtech.ui.common.debounced
import com.example.ibtech.ui.theme.LibraryDimens

/** 오늘의 행사·공지 화면 (요구사항 명세서 3.5절, 로드맵 9단계). */
@Composable
fun EventsScreen(
    uiState: EventsUiState,
    onSelectEvent: (String) -> Unit,
    onGoHome: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        DecorativeBackground(modifier = Modifier.fillMaxSize())

        if (!uiState.isLoaded) return@Box

        if (!uiState.hasAnyEvent) {
            EmptyState(
                message = stringResource(R.string.events_empty_all),
                actionLabel = stringResource(R.string.top_bar_home),
                onAction = onGoHome,
                modifier = Modifier.fillMaxSize()
            )
            return@Box
        }

        LazyColumn(
            contentPadding = PaddingValues(LibraryDimens.ScreenPadding),
            verticalArrangement = Arrangement.spacedBy(LibraryDimens.CardSpacing),
            modifier = Modifier.fillMaxSize()
        ) {
            if (uiState.notices.isNotEmpty()) {
                item(key = "notice_title") {
                    SectionTitle(stringResource(R.string.events_notice_section_title))
                }
                items(uiState.notices, key = { "notice_${it.id}" }) { notice ->
                    NoticeCard(notice)
                }
            }

            item(key = "today_title") {
                SectionTitle(stringResource(R.string.events_today_section_title))
            }
            if (uiState.todayEvents.isEmpty()) {
                item(key = "today_empty") {
                    Text(
                        text = stringResource(R.string.events_empty_today),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(uiState.todayEvents, key = { "today_${it.id}" }) { event ->
                    EventCard(event = event, onClick = { onSelectEvent(event.id) })
                }
            }

            if (uiState.upcomingEvents.isNotEmpty()) {
                item(key = "upcoming_title") {
                    SectionTitle(stringResource(R.string.events_upcoming_section_title))
                }
                items(uiState.upcomingEvents, key = { "upcoming_${it.id}" }) { event ->
                    EventCard(event = event, onClick = { onSelectEvent(event.id) })
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.onBackground
    )
}

@Composable
private fun NoticeCard(notice: LibraryNotice) {
    LibraryCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(LibraryDimens.CardPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Campaign,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = notice.text,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun EventCard(event: LibraryEvent, onClick: () -> Unit) {
    LibraryCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = debounced(onClick))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(LibraryDimens.CardPadding),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = event.title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )

            val dateTimeText = listOfNotNull(event.startDate, event.timeText).joinToString(" ")
            IconLine(icon = Icons.Filled.Schedule, text = dateTimeText)
            IconLine(icon = Icons.Filled.Place, text = event.place)
            event.target?.takeIf { it.isNotBlank() }?.let { target ->
                IconLine(icon = Icons.Filled.Groups, text = target)
            }

            if (!event.qrUrl.isNullOrBlank()) {
                Text(
                    text = stringResource(R.string.events_apply_available_label),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun IconLine(icon: ImageVector, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
