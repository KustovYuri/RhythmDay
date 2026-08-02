package com.rhythmday.app.data.local

import com.rhythmday.app.core.domain.model.DayEntry
import com.rhythmday.app.core.domain.model.DayMetrics
import com.rhythmday.app.data.local.entity.DayEntryEntity
import com.rhythmday.app.data.local.entity.DayEntryWithTags
import kotlinx.datetime.LocalDate
import kotlin.time.Instant

internal fun DayEntry.toEntity(): DayEntryEntity = DayEntryEntity(
    id = id,
    dateEpochDay = date.toEpochDays(),
    energy = metrics.energy,
    mood = metrics.mood,
    wellbeing = metrics.wellbeing,
    mentalClarity = metrics.mentalClarity,
    sociality = metrics.sociality,
    sleepQuality = metrics.sleepQuality,
    stress = metrics.stress,
    overallScore = overallScore,
    title = title,
    note = note,
    savedAtEpochMilliseconds = savedAt?.toEpochMilliseconds()
)

internal fun DayEntryWithTags.toDomain(): DayEntry = DayEntry(
    id = entry.id,
    date = LocalDate.fromEpochDays(entry.dateEpochDay),
    metrics = DayMetrics(
        energy = entry.energy,
        mood = entry.mood,
        wellbeing = entry.wellbeing,
        mentalClarity = entry.mentalClarity,
        sociality = entry.sociality,
        sleepQuality = entry.sleepQuality,
        stress = entry.stress
    ),
    overallScore = entry.overallScore,
    tags = tags.sortedBy { tag -> tag.sortOrder }.map { tag -> tag.name },
    title = entry.title,
    note = entry.note,
    savedAt = entry.savedAtEpochMilliseconds?.let(Instant::fromEpochMilliseconds)
)
