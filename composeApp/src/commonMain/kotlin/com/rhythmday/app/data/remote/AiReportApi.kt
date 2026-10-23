package com.rhythmday.app.data.remote

import com.rhythmday.app.core.domain.model.DayEntry
import com.rhythmday.app.feature.ai.domain.AiObservation
import com.rhythmday.app.feature.ai.domain.AiPeriodAssessment
import com.rhythmday.app.feature.ai.domain.AiReport
import com.rhythmday.app.feature.ai.domain.AiReportConfidence
import com.rhythmday.app.feature.ai.domain.AiReportException
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.isSuccess
import io.ktor.http.contentType
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable
import kotlin.time.Instant

class AiReportApi(
    private val client: HttpClient,
    baseUrl: String
) {
    private val baseUrl = baseUrl.trimEnd('/')

    suspend fun generateReport(
        from: LocalDate,
        to: LocalDate,
        entries: List<DayEntry>
    ): AiReport {
        if (baseUrl.isBlank()) {
            throw AiReportException("AI_SERVER_NOT_CONFIGURED", "Адрес сервера анализа не настроен")
        }
        val response = try {
            client.post("$baseUrl/api/v1/ai/reports") {
                contentType(ContentType.Application.Json)
                setBody(
                    AiReportRequestDto(
                        from = from.toString(),
                        to = to.toString(),
                        entries = entries.sortedBy(DayEntry::date).map(DayEntry::toDto)
                    )
                )
            }
        } catch (error: AiReportException) {
            throw error
        } catch (error: Exception) {
            throw AiReportException(
                "AI_CONNECTION_FAILED",
                "Не удалось подключиться к серверу анализа"
            )
        }

        if (!response.status.isSuccess()) {
            val error = runCatching { response.body<ApiErrorDto>() }.getOrNull()
            throw AiReportException(
                code = error?.code ?: "AI_REQUEST_FAILED",
                message = error.toUserMessage()
            )
        }
        return response.body<AiReportResponseDto>().toDomain()
    }
}

@Serializable
data class AiReportRequestDto(
    val from: String,
    val to: String,
    val entries: List<AiDiaryEntryDto>
)

@Serializable
data class AiDiaryEntryDto(
    val date: String,
    val metrics: AiDayMetricsDto,
    val overallScore: Double,
    val tags: List<String>,
    val title: String?,
    val note: String?
)

@Serializable
data class AiDayMetricsDto(
    val energy: Double,
    val mood: Double,
    val wellbeing: Double,
    val mentalClarity: Double,
    val sociality: Double,
    val sleepQuality: Double,
    val stress: Double
)

@Serializable
data class AiReportResponseDto(
    val from: String,
    val to: String,
    val generatedAt: String,
    val entryCount: Int,
    val assessment: String,
    val confidence: String,
    val summary: String,
    val observations: List<AiObservationDto>,
    val suggestions: List<String>,
    val reflectionQuestions: List<String>,
    val disclaimer: String
)

@Serializable
data class AiObservationDto(
    val text: String,
    val evidence: List<String>
)

@Serializable
private data class ApiErrorDto(
    val code: String,
    val message: String
)

private fun DayEntry.toDto() = AiDiaryEntryDto(
    date = date.toString(),
    metrics = AiDayMetricsDto(
        energy = metrics.energy,
        mood = metrics.mood,
        wellbeing = metrics.wellbeing,
        mentalClarity = metrics.mentalClarity,
        sociality = metrics.sociality,
        sleepQuality = metrics.sleepQuality,
        stress = metrics.stress
    ),
    overallScore = overallScore,
    tags = tags,
    title = title,
    note = note
)

fun AiReportResponseDto.toDomain() = AiReport(
    from = LocalDate.parse(from),
    to = LocalDate.parse(to),
    generatedAt = Instant.parse(generatedAt),
    entryCount = entryCount,
    assessment = assessment.toAssessment(),
    confidence = confidence.toConfidence(),
    summary = summary,
    observations = observations.map { AiObservation(it.text, it.evidence) },
    suggestions = suggestions,
    reflectionQuestions = reflectionQuestions,
    disclaimer = disclaimer
)

private fun String.toAssessment(): AiPeriodAssessment = when (lowercase()) {
    "improving" -> AiPeriodAssessment.IMPROVING
    "stable" -> AiPeriodAssessment.STABLE
    "declining" -> AiPeriodAssessment.DECLINING
    "mixed" -> AiPeriodAssessment.MIXED
    else -> AiPeriodAssessment.INSUFFICIENT_DATA
}

private fun String.toConfidence(): AiReportConfidence = when (lowercase()) {
    "high" -> AiReportConfidence.HIGH
    "medium" -> AiReportConfidence.MEDIUM
    else -> AiReportConfidence.LOW
}

private fun ApiErrorDto?.toUserMessage(): String = when (this?.code) {
    "AI_NOT_CONFIGURED" -> "На сервере не настроен ключ GigaChat"
    "AI_PROVIDER_AUTH_FAILED",
    "AI_PROVIDER_UNAVAILABLE" -> "Сервис анализа временно недоступен"
    "AI_PROVIDER_INVALID_RESPONSE" -> "GigaChat вернул некорректный ответ"
    "AI_PERIOD_INVALID",
    "AI_PERIOD_TOO_LONG",
    "AI_TOO_MANY_ENTRIES",
    "AI_DUPLICATE_ENTRY_DATE",
    "AI_ENTRY_OUTSIDE_PERIOD",
    "AI_SCORE_INVALID",
    "AI_METRIC_INVALID" -> "Проверь выбранный период и данные записей"
    else -> "Не удалось сформировать AI-отчёт"
}
