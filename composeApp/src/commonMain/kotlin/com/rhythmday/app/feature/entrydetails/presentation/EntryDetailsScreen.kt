package com.rhythmday.app.feature.entrydetails.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rhythmday.app.core.designsystem.RhythmDayPalette
import com.rhythmday.app.core.domain.model.DayEntry
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.isoDayNumber
import kotlin.math.roundToInt

private val CardShape = RoundedCornerShape(8.dp)
private val DetailFieldBackground = Color(0xFFF0F4F7)

@Composable
fun EntryDetailsRoute(
    viewModel: EntryDetailsViewModel,
    contentPadding: PaddingValues,
    onBack: () -> Unit,
    onEdit: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    EntryDetailsScreen(
        state = state,
        contentPadding = contentPadding,
        onBack = onBack,
        onEdit = onEdit
    )
}

@Composable
private fun EntryDetailsScreen(
    state: EntryDetailsUiState,
    contentPadding: PaddingValues,
    onBack: () -> Unit,
    onEdit: () -> Unit
) {
    val entry = state.entry

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
            EntryDetailsHeader(onBack = onBack, onEdit = onEdit)
        }

        when {
            state.isLoading -> item {
                Box(
                    modifier = Modifier.fillMaxWidth().heightIn(min = 220.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = RhythmDayPalette.Teal)
                }
            }

            entry != null -> {
                item { DayHero(entry = entry) }
                item { MetricsCard(entry = entry) }
                item { CommentCard(note = entry.note) }
            }

            else -> item {
                UnavailableEntryCard(
                    message = state.errorMessage ?: "Запись не найдена"
                )
            }
        }
    }
}

@Composable
private fun EntryDetailsHeader(
    onBack: () -> Unit,
    onEdit: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = 42.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        HeaderIconButton(
            icon = Icons.AutoMirrored.Outlined.ArrowBack,
            contentDescription = "Назад",
            onClick = onBack
        )
        Text(
            text = "Запись дня",
            modifier = Modifier.weight(1f),
            fontSize = 20.sp,
            lineHeight = 22.sp,
            fontWeight = FontWeight.Bold,
            color = RhythmDayPalette.Ink,
            textAlign = TextAlign.Center
        )
        HeaderIconButton(
            icon = Icons.Outlined.Edit,
            contentDescription = "Редактировать запись",
            onClick = onEdit
        )
    }
}

@Composable
private fun HeaderIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(42.dp)
            .clip(CardShape)
            .background(RhythmDayPalette.Panel)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = RhythmDayPalette.Ink,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun DayHero(entry: DayEntry) {
    val style = scoreStyle(entry.overallScore)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = CardShape,
        color = Color.Transparent,
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .background(Brush.linearGradient(listOf(style.startColor, style.endColor)))
                .padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                entry.title?.let { title ->
                    Text(
                        text = title,
                        fontSize = 18.sp,
                        lineHeight = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                Text(
                    text = entry.date.asFullDate(),
                    fontSize = if (entry.title == null) 18.sp else 13.sp,
                    lineHeight = if (entry.title == null) 22.sp else 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (entry.title == null) Color.White else Color.White.copy(alpha = 0.78f),
                    modifier = Modifier.padding(top = if (entry.title == null) 0.dp else 2.dp)
                )
                Box(
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .heightIn(min = 26.dp)
                        .clip(CardShape)
                        .background(Color.White.copy(alpha = 0.20f))
                        .border(1.dp, Color.White.copy(alpha = 0.22f), CardShape)
                        .padding(horizontal = 9.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = style.dayLabel,
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                Text(
                    text = entry.tags.takeIf(List<String>::isNotEmpty)?.joinToString() ?: "Теги не добавлены",
                    modifier = Modifier.padding(top = 7.dp),
                    fontSize = 13.sp,
                    lineHeight = 19.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White.copy(alpha = 0.74f)
                )
            }
            Box(
                modifier = Modifier
                    .size(76.dp)
                    .clip(CardShape)
                    .background(style.scoreColor)
                    .border(1.dp, Color.White.copy(alpha = 0.18f), CardShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = entry.overallScore.asDisplayScore(),
                    fontSize = 34.sp,
                    lineHeight = 34.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
private fun MetricsCard(entry: DayEntry) {
    DetailCard {
        Text(
            text = "Оценки",
            fontSize = 18.sp,
            lineHeight = 22.sp,
            fontWeight = FontWeight.Bold,
            color = RhythmDayPalette.Ink
        )
        Column(
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            metricItems(entry).chunked(3).forEach { rowItems ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    rowItems.forEach { item ->
                        MetricTile(item = item, modifier = Modifier.weight(1f))
                    }
                    repeat(3 - rowItems.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricTile(
    item: MetricItem,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .heightIn(min = 62.dp)
            .clip(CardShape)
            .background(DetailFieldBackground)
            .padding(9.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = item.value.asDisplayScore(),
            fontSize = 19.sp,
            lineHeight = 20.sp,
            fontWeight = FontWeight.Bold,
            color = RhythmDayPalette.Ink
        )
        Text(
            text = item.label,
            fontSize = 11.sp,
            lineHeight = 13.sp,
            fontWeight = FontWeight.Bold,
            color = RhythmDayPalette.Muted
        )
    }
}

@Composable
private fun CommentCard(note: String?) {
    DetailCard {
        Text(
            text = "Комментарий",
            fontSize = 18.sp,
            lineHeight = 22.sp,
            fontWeight = FontWeight.Bold,
            color = RhythmDayPalette.Ink
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp)
                .clip(CardShape)
                .background(DetailFieldBackground)
                .padding(12.dp)
        ) {
            Text(
                text = note ?: "Комментарий не добавлен.",
                fontSize = 13.sp,
                lineHeight = 19.sp,
                color = if (note == null) RhythmDayPalette.Muted else RhythmDayPalette.Ink
            )
        }
    }
}

@Composable
private fun UnavailableEntryCard(message: String) {
    DetailCard {
        Text(
            text = message,
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.bodyMedium,
            color = RhythmDayPalette.Muted,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun DetailCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = CardShape,
        color = RhythmDayPalette.Panel
    ) {
        Column(modifier = Modifier.padding(14.dp), content = content)
    }
}

private data class MetricItem(
    val label: String,
    val value: Double
)

private data class ScoreStyle(
    val startColor: Color,
    val endColor: Color,
    val scoreColor: Color,
    val dayLabel: String
)

private fun metricItems(entry: DayEntry): List<MetricItem> = listOf(
    MetricItem("энергия", entry.metrics.energy),
    MetricItem("настроение", entry.metrics.mood),
    MetricItem("самочувствие", entry.metrics.wellbeing),
    MetricItem("качество сна", entry.metrics.sleepQuality),
    MetricItem("Ясность ума", entry.metrics.mentalClarity),
    MetricItem("стресс", entry.metrics.stress),
    MetricItem("социальность", entry.metrics.sociality)
)

private fun scoreStyle(score: Double): ScoreStyle = when {
    score >= 8.0 -> ScoreStyle(
        startColor = Color(0xFF54A266),
        endColor = Color(0xFF2F7D49),
        scoreColor = RhythmDayPalette.GoodDay,
        dayLabel = "Хороший день"
    )

    score >= 5.0 -> ScoreStyle(
        startColor = Color(0xFFC99A35),
        endColor = Color(0xFFA87818),
        scoreColor = RhythmDayPalette.MediumDay,
        dayLabel = "Средний день"
    )

    else -> ScoreStyle(
        startColor = Color(0xFFC9837B),
        endColor = Color(0xFFA75E56),
        scoreColor = RhythmDayPalette.HardDay,
        dayLabel = "Тяжелый день"
    )
}

private fun Double.asDisplayScore(): String {
    val tenths = (this * 10).roundToInt()
    return if (tenths % 10 == 0) {
        (tenths / 10).toString()
    } else {
        "${tenths / 10}.${tenths % 10}"
    }
}

private fun LocalDate.asFullDate(): String =
    "${dayOfWeek.asName()}, $day ${monthNames[month.ordinal]}".replaceFirstChar(Char::uppercase)

private fun DayOfWeek.asName(): String = weekDayNames[isoDayNumber - 1]

private val weekDayNames = listOf(
    "понедельник",
    "вторник",
    "среда",
    "четверг",
    "пятница",
    "суббота",
    "воскресенье"
)

private val monthNames = listOf(
    "января",
    "февраля",
    "марта",
    "апреля",
    "мая",
    "июня",
    "июля",
    "августа",
    "сентября",
    "октября",
    "ноября",
    "декабря"
)
