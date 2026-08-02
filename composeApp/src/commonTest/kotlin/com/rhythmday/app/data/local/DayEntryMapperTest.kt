package com.rhythmday.app.data.local

import com.rhythmday.app.core.domain.model.DayEntry
import com.rhythmday.app.core.domain.model.DayMetrics
import com.rhythmday.app.data.local.entity.DayEntryWithTags
import com.rhythmday.app.data.local.entity.DayTagEntity
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

class DayEntryMapperTest {
    @Test
    fun mapsStoredEntryAndTagsBackToDomain() {
        val entry = DayEntry(
            id = 4,
            date = LocalDate(2026, 8, 2),
            metrics = DayMetrics(7.4, 6.2, 7.1, 6.8, 6.3, 8.5, 4.2),
            overallScore = 6.9,
            tags = listOf("Работа", "Прогулка"),
            title = "Спокойный день",
            note = "Удалось отдохнуть",
            savedAt = Instant.parse("2026-08-02T17:30:00Z")
        )
        val storedEntry = DayEntryWithTags(
            entry = entry.toEntity(),
            tags = listOf(
                DayTagEntity(name = "Прогулка", sortOrder = 1),
                DayTagEntity(name = "Работа", sortOrder = 0)
            )
        )

        assertEquals(entry, storedEntry.toDomain())
    }
}
