package com.rhythmday.app.core.domain.usecase

import com.rhythmday.app.core.domain.model.DayEntry
import com.rhythmday.app.core.domain.model.DayMetrics
import com.rhythmday.app.core.domain.model.DAY_TITLE_MAX_LENGTH
import com.rhythmday.app.core.domain.repository.DayEntryRepository
import kotlinx.coroutines.flow.first
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlin.time.Clock
import kotlin.time.Instant

class SaveDayEntryUseCase(
    private val repository: DayEntryRepository,
    private val scoreCalculator: DayScoreCalculatorUseCase,
    private val currentDate: () -> LocalDate = {
        Clock.System.todayIn(TimeZone.currentSystemDefault())
    },
    private val currentInstant: () -> Instant = Clock.System::now
) {
    suspend operator fun invoke(
        metrics: DayMetrics,
        title: String,
        tags: List<String>,
        note: String,
        entryId: Long? = null,
        entryDate: LocalDate? = null
    ): DayEntry {
        val today = currentDate()
        val requestedDate = entryDate ?: today
        require(entryId != null || requestedDate <= today) {
            "A day entry cannot be created for a future date"
        }
        val entries = repository.observeEntries().first()
        val existingEntry = if (entryId == null) {
            entries.firstOrNull { entry -> entry.date == requestedDate }
        } else {
            requireNotNull(entries.firstOrNull { entry -> entry.id == entryId }) {
                "Entry with id $entryId was not found"
            }
        }
        val nextId = (entries.maxOfOrNull(DayEntry::id) ?: 0L) + 1L
        val cleanTags = tags
            .map(String::trim)
            .filter(String::isNotEmpty)
            .distinct()

        val entry = DayEntry(
            id = existingEntry?.id ?: nextId,
            date = existingEntry?.date ?: requestedDate,
            metrics = metrics,
            overallScore = scoreCalculator(metrics),
            tags = cleanTags,
            title = title.trim().take(DAY_TITLE_MAX_LENGTH).takeIf(String::isNotEmpty),
            note = note.trim().takeIf(String::isNotEmpty),
            savedAt = currentInstant()
        )

        repository.upsertEntry(entry)
        return entry
    }
}
