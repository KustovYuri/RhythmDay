package com.rhythmday.app.feature.ai.domain

import com.rhythmday.app.core.domain.model.DayEntry
import kotlinx.datetime.LocalDate
import kotlin.time.Instant

data class AiReport(
    val from: LocalDate,
    val to: LocalDate,
    val generatedAt: Instant,
    val entryCount: Int,
    val assessment: AiPeriodAssessment,
    val confidence: AiReportConfidence,
    val summary: String,
    val observations: List<AiObservation>,
    val suggestions: List<String>,
    val reflectionQuestions: List<String>,
    val disclaimer: String
)

data class AiObservation(
    val text: String,
    val evidence: List<String>
)

enum class AiPeriodAssessment {
    IMPROVING,
    STABLE,
    DECLINING,
    MIXED,
    INSUFFICIENT_DATA
}

enum class AiReportConfidence {
    LOW,
    MEDIUM,
    HIGH
}

interface AiReportRepository {
    suspend fun cachedReport(
        from: LocalDate,
        to: LocalDate,
        entries: List<DayEntry>
    ): AiReport?

    suspend fun generateReport(
        from: LocalDate,
        to: LocalDate,
        entries: List<DayEntry>
    ): AiReport
}

class AiReportException(
    val code: String,
    override val message: String
) : Exception(message)
