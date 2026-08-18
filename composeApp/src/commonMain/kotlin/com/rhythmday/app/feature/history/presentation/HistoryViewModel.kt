package com.rhythmday.app.feature.history.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rhythmday.app.core.domain.model.DayEntry
import com.rhythmday.app.core.domain.repository.DayEntryRepository
import com.rhythmday.app.core.presentation.UserMessageManager
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
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn
import kotlin.time.Clock

data class HistoryUiState(
    val isLoading: Boolean = true,
    val selectedMonth: LocalDate,
    val entries: List<DayEntry> = emptyList(),
    val canShowPreviousMonth: Boolean = false,
    val canShowNextMonth: Boolean = false,
    val errorMessage: String? = null
)

class HistoryViewModel(
    repository: DayEntryRepository,
    currentDate: () -> LocalDate = {
        Clock.System.todayIn(TimeZone.currentSystemDefault())
    },
    messages: UserMessageManager = UserMessageManager()
) : ViewModel() {
    private val currentMonth = currentDate().firstDayOfMonth()
    private val selectedMonth = MutableStateFlow(currentMonth)

    val uiState: StateFlow<HistoryUiState> = repository.observeEntries()
        .combine(selectedMonth) { entries, month ->
            val sortedEntries = entries.sortedBy(DayEntry::date)
            val earliestMonth = sortedEntries.firstOrNull()?.date?.firstDayOfMonth() ?: currentMonth

            HistoryUiState(
                isLoading = false,
                selectedMonth = month,
                entries = sortedEntries
                    .filter { entry -> entry.date.isInMonth(month) }
                    .sortedByDescending(DayEntry::date),
                canShowPreviousMonth = month > earliestMonth,
                canShowNextMonth = month < currentMonth
            )
        }
        .catch { error ->
            messages.showError(error, "Не удалось загрузить историю")
            emit(
                HistoryUiState(
                    isLoading = false,
                    selectedMonth = selectedMonth.value,
                    errorMessage = "Не удалось загрузить историю"
                )
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = HistoryUiState(selectedMonth = currentMonth)
        )

    fun showPreviousMonth() {
        if (!uiState.value.canShowPreviousMonth) return
        selectedMonth.update { month -> month.minus(1, DateTimeUnit.MONTH) }
    }

    fun showNextMonth() {
        if (!uiState.value.canShowNextMonth) return
        selectedMonth.update { month -> month.plus(1, DateTimeUnit.MONTH) }
    }
}

private fun LocalDate.firstDayOfMonth(): LocalDate = LocalDate(year, month, 1)

private fun LocalDate.isInMonth(month: LocalDate): Boolean =
    year == month.year && this.month == month.month
