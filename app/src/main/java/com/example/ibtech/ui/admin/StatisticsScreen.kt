package com.example.ibtech.ui.admin

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.ibtech.R
import com.example.ibtech.domain.usecase.StatsPeriod
import com.example.ibtech.ui.common.DecorativeBackground
import com.example.ibtech.ui.common.LibraryCard
import com.example.ibtech.ui.common.LibraryOutlinedButton
import com.example.ibtech.ui.theme.LibraryDimens
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.flow.SharedFlow

/** 통계 화면 (요구사항 명세서 4.3절, 로드맵 11단계). */
@Composable
fun StatisticsScreen(
    uiState: StatisticsUiState,
    events: SharedFlow<Int>,
    onSelectPeriod: (StatsPeriod) -> Unit,
    onExportCsv: () -> Unit,
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

        val summary = uiState.summary
        val emptyMessage = stringResource(R.string.stats_empty)
        val labelEscort = stringResource(R.string.stats_label_escort)
        val labelLocationOnly = stringResource(R.string.stats_label_location_only)
        val labelNavSuccess = stringResource(R.string.stats_label_nav_success)
        val labelNavCancelled = stringResource(R.string.stats_label_nav_cancelled)
        val labelNavFailed = stringResource(R.string.stats_label_nav_failed)
        val labelStaffHelp = stringResource(R.string.stats_label_staff_help)
        val labelQuizStart = stringResource(R.string.stats_label_quiz_start)
        val labelQuizComplete = stringResource(R.string.stats_label_quiz_complete)
        val labelQuizAccuracy = stringResource(R.string.stats_label_quiz_accuracy)
        val labelEventQrOpen = stringResource(R.string.stats_label_event_qr_open)

        LazyColumn(
            contentPadding = PaddingValues(LibraryDimens.ScreenPadding),
            verticalArrangement = Arrangement.spacedBy(LibraryDimens.CardSpacing),
            modifier = Modifier.fillMaxSize()
        ) {
            item(key = "period_selector") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = uiState.period == StatsPeriod.TODAY,
                        onClick = { onSelectPeriod(StatsPeriod.TODAY) },
                        label = { Text(stringResource(R.string.stats_period_today)) }
                    )
                    FilterChip(
                        selected = uiState.period == StatsPeriod.WEEK,
                        onClick = { onSelectPeriod(StatsPeriod.WEEK) },
                        label = { Text(stringResource(R.string.stats_period_week)) }
                    )
                    FilterChip(
                        selected = uiState.period == StatsPeriod.MONTH,
                        onClick = { onSelectPeriod(StatsPeriod.MONTH) },
                        label = { Text(stringResource(R.string.stats_period_month)) }
                    )
                }
            }

            item(key = "menu") {
                StatCard(
                    title = stringResource(R.string.stats_section_menu),
                    rows = summary.menuSelectCounts.map { it.label to it.count },
                    emptyMessage = emptyMessage
                )
            }
            item(key = "facility") {
                StatCard(
                    title = stringResource(R.string.stats_section_facility),
                    rows = summary.facilityRequestCounts.map { it.label to it.count },
                    emptyMessage = emptyMessage
                )
            }
            item(key = "guide_mode") {
                StatCard(
                    title = stringResource(R.string.stats_section_guide_mode),
                    rows = listOf(labelEscort to summary.escortCount, labelLocationOnly to summary.locationOnlyCount),
                    emptyMessage = emptyMessage
                )
            }
            item(key = "navigation") {
                StatCard(
                    title = stringResource(R.string.stats_section_navigation),
                    rows = listOf(
                        labelNavSuccess to summary.navSuccessCount,
                        labelNavCancelled to summary.navCancelledCount,
                        labelNavFailed to summary.navFailedCount
                    ),
                    emptyMessage = emptyMessage
                )
            }
            item(key = "usage_topic") {
                StatCard(
                    title = stringResource(R.string.stats_section_usage_topic),
                    rows = summary.usageTopicCounts.map { it.label to it.count },
                    emptyMessage = emptyMessage
                )
            }
            item(key = "staff_help") {
                StatCard(
                    title = stringResource(R.string.usage_answer_staff_help_action),
                    rows = listOf(labelStaffHelp to summary.staffHelpCount),
                    emptyMessage = emptyMessage
                )
            }
            item(key = "quiz") {
                StatCard(
                    title = stringResource(R.string.stats_section_quiz),
                    rows = listOf(
                        labelQuizStart to summary.quizStartCount,
                        labelQuizComplete to summary.quizCompleteCount,
                        labelQuizAccuracy to summary.quizAccuracyPercent
                    ),
                    emptyMessage = emptyMessage
                )
            }
            item(key = "book") {
                StatCard(
                    title = stringResource(R.string.stats_section_book),
                    rows = summary.bookViewCounts.map { it.label to it.count },
                    emptyMessage = emptyMessage
                )
            }
            item(key = "event") {
                StatCard(
                    title = stringResource(R.string.stats_section_event),
                    rows = summary.eventViewCounts.map { it.label to it.count } +
                        (labelEventQrOpen to summary.eventQrOpenCount),
                    emptyMessage = emptyMessage
                )
            }

            item(key = "export") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = uiState.lastExportAt?.let { formatExportTime(it) }
                            ?: stringResource(R.string.stats_last_export_none),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    LibraryOutlinedButton(
                        text = stringResource(R.string.stats_export_action),
                        onClick = onExportCsv
                    )
                }
            }
        }
    }
}

@Composable
private fun StatCard(title: String, rows: List<Pair<String, Int>>, emptyMessage: String) {
    // 관리자 화면 카드 모서리는 16~20dp로 통일한다(최종 단계 D).
    LibraryCard(modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(LibraryDimens.CardPadding),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(text = title, style = MaterialTheme.typography.titleLarge)
            if (rows.isEmpty()) {
                Text(text = emptyMessage, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                rows.forEach { (label, count) -> StatRow(label, count) }
            }
        }
    }
}

@Composable
private fun StatRow(label: String, count: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label.ifBlank { "-" },
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun formatExportTime(epochMillis: Long): String {
    val formatted = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.KOREA).format(Date(epochMillis))
    return stringResource(R.string.stats_last_export_format, formatted)
}
