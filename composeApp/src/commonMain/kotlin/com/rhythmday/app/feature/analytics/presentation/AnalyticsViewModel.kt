package com.rhythmday.app.feature.analytics.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rhythmday.app.core.domain.model.DayEntry
import com.rhythmday.app.core.domain.repository.DayEntryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.todayIn
import kotlin.time.Clock

enum class AnalyticsRange {
    WEEK,
    MONTH,
    PERIOD
}

enum class AnalyticsMode {
    CHART,
    OVERVIEW
}

enum class AnalyticsMetric {
    SCORE,
    SLEEP,
    MENTAL_CLARITY,
    ENERGY,
    MOOD,
    WELLBEING,
    SOCIALITY,
    STRESS;

    fun value(entry: DayEntry): Double = when (this) {
        SCORE -> entry.overallScore
        SLEEP -> entry.metrics.sleepQuality.toDouble()
        MENTAL_CLARITY -> entry.metrics.mentalClarity.toDouble()
        ENERGY -> entry.metrics.energy.toDouble()
        MOOD -> entry.metrics.mood.toDouble()
        WELLBEING -> entry.metrics.wellbeing.toDouble()
        SOCIALITY -> entry.metrics.sociality.toDouble()
        STRESS -> entry.metrics.stress.toDouble()
    }
}

data class AnalyticsUiState(
    val isLoading: Boolean = true,
    val range: AnalyticsRange = AnalyticsRange.WEEK,
    val mode: AnalyticsMode = AnalyticsMode.CHART,
    val selectedMetrics: Set<AnalyticsMetric> = DefaultAnalyticsMetrics,
    val periodStart: LocalDate,
    val periodEnd: LocalDate,
    val rangeStart: LocalDate = periodStart,
    val rangeEnd: LocalDate = periodEnd,
    val entries: List<DayEntry> = emptyList(),
    val errorMessage: String? = null
)

private data class AnalyticsControls(
    val range: AnalyticsRange = AnalyticsRange.WEEK,
    val mode: AnalyticsMode = AnalyticsMode.CHART,
    val selectedMetrics: Set<AnalyticsMetric> = DefaultAnalyticsMetrics,
    val periodStart: LocalDate? = null,
    val periodEnd: LocalDate? = null
)

class AnalyticsViewModel(
    repository: DayEntryRepository,
    currentDate: () -> LocalDate = {
        Clock.System.todayIn(TimeZone.currentSystemDefault())
    }
) : ViewModel() {
    private val today = currentDate()
    private val controls = MutableStateFlow(AnalyticsControls())

    val uiState: StateFlow<AnalyticsUiState> = repository.observeEntries()
        .combine(controls) { repositoryEntries, controls ->
            val allEntries = repositoryEntries
                .filter { entry -> entry.date <= today }
                .sortedBy(DayEntry::date)
            val periodStart = controls.periodStart ?: allEntries.firstOrNull()?.date ?: today
            val periodEnd = controls.periodEnd ?: today
            val (rangeStart, rangeEnd) = controls.range.dateRange(
                today = today,
                periodStart = periodStart,
                periodEnd = periodEnd
            )

            AnalyticsUiState(
                isLoading = false,
                range = controls.range,
                mode = controls.mode,
                selectedMetrics = controls.selectedMetrics,
                periodStart = periodStart,
                periodEnd = periodEnd,
                rangeStart = rangeStart,
                rangeEnd = rangeEnd,
                entries = allEntries.filter { entry -> entry.date in rangeStart..rangeEnd }
            )
        }
        .catch {
            val controls = controls.value
            emit(
                AnalyticsUiState(
                    isLoading = false,
                    range = controls.range,
                    mode = controls.mode,
                    selectedMetrics = controls.selectedMetrics,
                    periodStart = controls.periodStart ?: today,
                    periodEnd = controls.periodEnd ?: today,
                    errorMessage = "Не удалось загрузить аналитику"
                )
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = AnalyticsUiState(periodStart = today, periodEnd = today)
        )

    fun selectRange(range: AnalyticsRange) {
        controls.update { state -> state.copy(range = range) }
    }

    fun selectMode(mode: AnalyticsMode) {
        controls.update { state -> state.copy(mode = mode) }
    }

    fun toggleMetric(metric: AnalyticsMetric) {
        controls.update { state ->
            val selectedMetrics = if (metric in state.selectedMetrics) {
                state.selectedMetrics - metric
            } else {
                state.selectedMetrics + metric
            }
            state.copy(selectedMetrics = selectedMetrics.inMetricOrder())
        }
    }

    fun showMetric(metric: AnalyticsMetric) {
        controls.update { state ->
            state.copy(
                mode = AnalyticsMode.CHART,
                selectedMetrics = setOf(metric)
            )
        }
    }

    fun setPeriodStart(date: LocalDate) {
        controls.update { state ->
            state.copy(
                periodStart = date,
                periodEnd = state.periodEnd?.coerceAtLeast(date)
            )
        }
    }

    fun setPeriodEnd(date: LocalDate) {
        controls.update { state ->
            state.copy(
                periodStart = state.periodStart?.coerceAtMost(date),
                periodEnd = date
            )
        }
    }
}

private fun AnalyticsRange.dateRange(
    today: LocalDate,
    periodStart: LocalDate,
    periodEnd: LocalDate
): Pair<LocalDate, LocalDate> {
    val start = when (this) {
        AnalyticsRange.WEEK -> today.minus(today.dayOfWeek.isoDayNumber - 1, DateTimeUnit.DAY)
        AnalyticsRange.MONTH -> LocalDate(today.year, today.month, 1)
        AnalyticsRange.PERIOD -> periodStart
    }
    val end = if (this == AnalyticsRange.PERIOD) periodEnd else today
    return start to end
}

private fun Set<AnalyticsMetric>.inMetricOrder(): Set<AnalyticsMetric> =
    AnalyticsMetric.entries.filterTo(linkedSetOf()) { metric -> metric in this }

private val DefaultAnalyticsMetrics = linkedSetOf(
    AnalyticsMetric.SCORE
)
