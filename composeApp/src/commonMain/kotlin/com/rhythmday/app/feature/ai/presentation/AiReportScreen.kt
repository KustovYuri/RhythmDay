package com.rhythmday.app.feature.ai.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rhythmday.app.core.designsystem.RhythmDayIcons
import com.rhythmday.app.core.designsystem.RhythmDayPalette
import com.rhythmday.app.feature.ai.domain.AiPeriodAssessment
import com.rhythmday.app.feature.ai.domain.AiReport
import com.rhythmday.app.feature.ai.domain.AiReportConfidence
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

private val ReportCardShape = RoundedCornerShape(12.dp)
private val ReportFieldBackground = Color(0xFFF0F4F7)

@Composable
fun AiReportRoute(
    viewModel: AiReportViewModel,
    contentPadding: PaddingValues,
    onBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    AiReportScreen(
        state = state,
        contentPadding = contentPadding,
        onBack = onBack,
        onGenerate = viewModel::generateReport
    )
}

@Composable
private fun AiReportScreen(
    state: AiReportUiState,
    contentPadding: PaddingValues,
    onBack: () -> Unit,
    onGenerate: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 18.dp,
            top = contentPadding.calculateTopPadding() + 2.dp,
            end = 18.dp,
            bottom = contentPadding.calculateBottomPadding() + 18.dp
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Назад")
                }
                Column(modifier = Modifier.padding(start = 2.dp)) {
                    Text(
                        text = "AI-анализ",
                        fontSize = 27.sp,
                        lineHeight = 30.sp,
                        fontWeight = FontWeight.Black,
                        color = RhythmDayPalette.Ink
                    )
                    Text(
                        text = state.from.asNumericDate() + " — " + state.to.asNumericDate(),
                        fontSize = 13.sp,
                        color = RhythmDayPalette.Muted
                    )
                }
            }
        }

        when {
            state.isPreparing -> item { LoadingPanel("Загружаем записи…") }
            state.entryCount == 0 -> item {
                MessagePanel("В выбранном периоде пока нет записей для анализа.")
            }
            state.report != null -> {
                item { ReportHeader(state.report, state.isGenerating, onGenerate) }
                item { SummaryCard(state.report) }
                if (state.report.observations.isNotEmpty()) {
                    item {
                        ReportSection("Что заметно") {
                            state.report.observations.forEach { observation ->
                                ReportBullet(observation.text)
                                observation.evidence.forEach { evidence ->
                                    Text(
                                        text = evidence,
                                        fontSize = 12.sp,
                                        lineHeight = 17.sp,
                                        color = RhythmDayPalette.Muted,
                                        modifier = Modifier.padding(start = 18.dp, top = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
                if (state.report.suggestions.isNotEmpty()) {
                    item {
                        ReportSection("Что можно попробовать") {
                            state.report.suggestions.forEach { ReportBullet(it) }
                        }
                    }
                }
                if (state.report.reflectionQuestions.isNotEmpty()) {
                    item {
                        ReportSection("Вопросы для себя") {
                            state.report.reflectionQuestions.forEach { ReportBullet(it) }
                        }
                    }
                }
                item {
                    Text(
                        text = state.report.disclaimer,
                        fontSize = 11.sp,
                        lineHeight = 16.sp,
                        color = RhythmDayPalette.Muted,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }
            }
            state.isGenerating -> item { LoadingPanel("Анализируем записи…") }
            else -> item {
                StartAnalysisCard(
                    entryCount = state.entryCount,
                    errorMessage = state.errorMessage,
                    onGenerate = onGenerate
                )
            }
        }

        if (state.report != null && state.errorMessage != null) {
            item { MessagePanel(state.errorMessage) }
        }
    }
}

@Composable
private fun StartAnalysisCard(
    entryCount: Int,
    errorMessage: String?,
    onGenerate: () -> Unit
) {
    ReportSection("Готово к анализу") {
        Text(
            text = "Будут отправлены показатели, теги и текст $entryCount записей. Фотографии не отправляются.",
            fontSize = 14.sp,
            lineHeight = 20.sp,
            color = RhythmDayPalette.Muted
        )
        if (entryCount < 3) {
            Text(
                text = "Для поиска закономерностей лучше иметь не менее трёх записей.",
                fontSize = 13.sp,
                lineHeight = 18.sp,
                color = RhythmDayPalette.Amber,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
        if (errorMessage != null) {
            Text(
                text = errorMessage,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
        GenerateButton("Сформировать отчёт", onGenerate, Modifier.padding(top = 14.dp))
    }
}

@Composable
private fun ReportHeader(report: AiReport, isGenerating: Boolean, onGenerate: () -> Unit) {
    ReportSection("${report.assessment.label} · уверенность ${report.confidence.label}") {
        Text(
            text = "Записей: ${report.entryCount} · обновлено ${report.generatedAt.toLocalDateTime(TimeZone.currentSystemDefault()).date.asNumericDate()}",
            fontSize = 12.sp,
            color = RhythmDayPalette.Muted
        )
        GenerateButton(
            text = if (isGenerating) "Обновляем…" else "Обновить анализ",
            onClick = onGenerate,
            modifier = Modifier.padding(top = 12.dp),
            enabled = !isGenerating
        )
    }
}

@Composable
private fun SummaryCard(report: AiReport) {
    Surface(
        shape = ReportCardShape,
        color = Color(0xFFE2F2EF),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = RhythmDayIcons.Ai,
                    contentDescription = null,
                    tint = RhythmDayPalette.Teal
                )
                Text(
                    text = "Кратко",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = RhythmDayPalette.Ink,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
            Text(
                text = report.summary,
                fontSize = 15.sp,
                lineHeight = 22.sp,
                color = RhythmDayPalette.Ink,
                modifier = Modifier.padding(top = 10.dp)
            )
        }
    }
}

@Composable
private fun ReportSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Surface(shape = ReportCardShape, color = RhythmDayPalette.Panel, modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                fontSize = 17.sp,
                lineHeight = 21.sp,
                fontWeight = FontWeight.Bold,
                color = RhythmDayPalette.Ink
            )
            content()
        }
    }
}

@Composable
private fun ReportBullet(text: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text("•", color = RhythmDayPalette.Teal, fontWeight = FontWeight.Black)
        Text(
            text = text,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            color = RhythmDayPalette.Ink,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}

@Composable
private fun GenerateButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(containerColor = RhythmDayPalette.Teal),
        modifier = modifier.fillMaxWidth().heightIn(min = 48.dp)
    ) {
        Text(text, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun LoadingPanel(message: String) {
    Box(
        modifier = Modifier.fillMaxWidth().heightIn(min = 220.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = RhythmDayPalette.Teal)
            Text(message, color = RhythmDayPalette.Muted, modifier = Modifier.padding(top = 12.dp))
        }
    }
}

@Composable
private fun MessagePanel(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 120.dp)
            .background(ReportFieldBackground, ReportCardShape)
            .padding(18.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(message, color = RhythmDayPalette.Muted, textAlign = TextAlign.Center)
    }
}

private val AiPeriodAssessment.label: String
    get() = when (this) {
        AiPeriodAssessment.IMPROVING -> "Положительная динамика"
        AiPeriodAssessment.STABLE -> "Стабильный период"
        AiPeriodAssessment.DECLINING -> "Есть снижение"
        AiPeriodAssessment.MIXED -> "Смешанная динамика"
        AiPeriodAssessment.INSUFFICIENT_DATA -> "Недостаточно данных"
    }

private val AiReportConfidence.label: String
    get() = when (this) {
        AiReportConfidence.LOW -> "низкая"
        AiReportConfidence.MEDIUM -> "средняя"
        AiReportConfidence.HIGH -> "высокая"
    }

private fun LocalDate.asNumericDate(): String =
    "${day.toString().padStart(2, '0')}.${month.ordinal.plus(1).toString().padStart(2, '0')}.$year"
