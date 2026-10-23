package com.rhythmday.app.data.repository

import com.rhythmday.app.core.domain.model.DayEntry
import com.rhythmday.app.data.local.dao.RhythmDayDao
import com.rhythmday.app.data.local.entity.AiReportEntity
import com.rhythmday.app.data.remote.AiReportApi
import com.rhythmday.app.data.remote.AiReportResponseDto
import com.rhythmday.app.data.remote.toDomain
import com.rhythmday.app.feature.ai.domain.AiReport
import com.rhythmday.app.feature.ai.domain.AiReportRepository
import kotlinx.datetime.LocalDate
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class LocalAiReportRepository(
    private val dao: RhythmDayDao,
    private val api: AiReportApi,
    private val json: Json = Json { ignoreUnknownKeys = true }
) : AiReportRepository {
    override suspend fun cachedReport(
        from: LocalDate,
        to: LocalDate,
        entries: List<DayEntry>
    ): AiReport? {
        val entity = dao.aiReport(from.toEpochDays(), to.toEpochDays()) ?: return null
        if (entity.entriesFingerprint != entries.fingerprint()) return null
        return runCatching {
            json.decodeFromString<AiReportResponseDto>(entity.reportJson).toDomain()
        }.getOrNull()
    }

    override suspend fun generateReport(
        from: LocalDate,
        to: LocalDate,
        entries: List<DayEntry>
    ): AiReport {
        val report = api.generateReport(from, to, entries)
        val dto = report.toDto()
        dao.upsertAiReport(
            AiReportEntity(
                fromEpochDay = from.toEpochDays(),
                toEpochDay = to.toEpochDays(),
                entriesFingerprint = entries.fingerprint(),
                reportJson = json.encodeToString(dto),
                generatedAtEpochMilliseconds = report.generatedAt.toEpochMilliseconds()
            )
        )
        return report
    }
}

internal fun List<DayEntry>.fingerprint(): String = sortedBy(DayEntry::date)
    .joinToString(separator = "|") { entry ->
        listOf(
            entry.id,
            entry.date,
            entry.overallScore,
            entry.metrics.energy,
            entry.metrics.mood,
            entry.metrics.wellbeing,
            entry.metrics.mentalClarity,
            entry.metrics.sociality,
            entry.metrics.sleepQuality,
            entry.metrics.stress,
            entry.tags.sorted().joinToString(","),
            entry.title.orEmpty(),
            entry.note.orEmpty(),
            entry.savedAt
        ).joinToString(separator = ":")
    }.hashCode().toString()

private fun AiReport.toDto() = AiReportResponseDto(
    from = from.toString(),
    to = to.toString(),
    generatedAt = generatedAt.toString(),
    entryCount = entryCount,
    assessment = assessment.name.lowercase(),
    confidence = confidence.name.lowercase(),
    summary = summary,
    observations = observations.map { observation ->
        com.rhythmday.app.data.remote.AiObservationDto(observation.text, observation.evidence)
    },
    suggestions = suggestions,
    reflectionQuestions = reflectionQuestions,
    disclaimer = disclaimer
)
