package com.example.ibtech.ui.admin

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ibtech.R
import com.example.ibtech.data.repository.StatsRepository
import com.example.ibtech.domain.usecase.AggregateStatsUseCase
import com.example.ibtech.domain.usecase.ResolvePeriodStartUseCase
import com.example.ibtech.domain.usecase.StatsPeriod
import com.example.ibtech.domain.usecase.StatsSummary
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class StatisticsUiState(
    val isLoaded: Boolean = false,
    val period: StatsPeriod = StatsPeriod.TODAY,
    val summary: StatsSummary = StatsSummary(),
    val lastExportAt: Long? = null
)

/** 통계 화면 (요구사항 명세서 4.3절, 로드맵 11단계). */
@OptIn(ExperimentalCoroutinesApi::class)
class StatisticsViewModel(application: Application) : AndroidViewModel(application) {

    private val statsRepository = StatsRepository.getInstance(application)
    private val period = MutableStateFlow(StatsPeriod.TODAY)

    // 내보내기 직후 lastExportAt을 화면에 반영하기 위한 재계산 트리거일 뿐, 값 자체는 안 쓴다.
    private val exportTick = MutableStateFlow(0)

    private val _events = MutableSharedFlow<Int>()
    val events: SharedFlow<Int> = _events.asSharedFlow()

    val uiState: StateFlow<StatisticsUiState> = combine(period, exportTick) { p, _ -> p }
        .flatMapLatest { selectedPeriod ->
            val since = ResolvePeriodStartUseCase(selectedPeriod, System.currentTimeMillis())
            statsRepository.observeEventsSince(since).map { events ->
                StatisticsUiState(
                    isLoaded = true,
                    period = selectedPeriod,
                    summary = AggregateStatsUseCase(events),
                    lastExportAt = statsRepository.lastExportAt()
                )
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = StatisticsUiState()
        )

    fun onSelectPeriod(newPeriod: StatsPeriod) {
        period.value = newPeriod
    }

    fun onExportCsv() {
        viewModelScope.launch {
            val success = statsRepository.exportCsv()
            exportTick.update { it + 1 }
            _events.emit(if (success) R.string.stats_export_success else R.string.stats_export_failure)
        }
    }
}
