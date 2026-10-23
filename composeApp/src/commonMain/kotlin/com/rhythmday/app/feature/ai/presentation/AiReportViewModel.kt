package com.rhythmday.app.feature.ai.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rhythmday.app.core.domain.model.DayEntry
import com.rhythmday.app.core.domain.repository.DayEntryRepository
import com.rhythmday.app.feature.ai.domain.AiReport
import com.rhythmday.app.feature.ai.domain.AiReportRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate

data class AiReportUiState(
    val from: LocalDate,
    val to: LocalDate,
    val isPreparing: Boolean = true,
    val isGenerating: Boolean = false,
    val entryCount: Int = 0,
    val report: AiReport? = null,
    val errorMessage: String? = null
)

class AiReportViewModel(
    private val from: LocalDate,
    private val to: LocalDate,
    dayEntryRepository: DayEntryRepository,
    private val aiReportRepository: AiReportRepository
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(AiReportUiState(from = from, to = to))
    val uiState: StateFlow<AiReportUiState> = mutableUiState.asStateFlow()
    private var periodEntries: List<DayEntry> = emptyList()

    init {
        viewModelScope.launch {
            dayEntryRepository.observeEntries().collectLatest { entries ->
                periodEntries = entries.filter { entry -> entry.date in from..to }
                    .sortedBy(DayEntry::date)
                val cached = aiReportRepository.cachedReport(from, to, periodEntries)
                mutableUiState.update { state ->
                    state.copy(
                        isPreparing = false,
                        entryCount = periodEntries.size,
                        report = cached,
                        errorMessage = null
                    )
                }
            }
        }
    }

    fun generateReport() {
        if (mutableUiState.value.isGenerating || periodEntries.isEmpty()) return
        val entries = periodEntries
        viewModelScope.launch {
            mutableUiState.update { state ->
                state.copy(isGenerating = true, errorMessage = null)
            }
            runCatching {
                aiReportRepository.generateReport(from, to, entries)
            }.onSuccess { report ->
                mutableUiState.update { state ->
                    state.copy(isGenerating = false, report = report)
                }
            }.onFailure { error ->
                mutableUiState.update { state ->
                    state.copy(
                        isGenerating = false,
                        errorMessage = error.message ?: "Не удалось сформировать AI-отчёт"
                    )
                }
            }
        }
    }
}
