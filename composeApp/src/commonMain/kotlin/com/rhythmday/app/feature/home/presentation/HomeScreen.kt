package com.rhythmday.app.feature.home.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rhythmday.app.core.designsystem.RhythmDayIcons
import com.rhythmday.app.core.designsystem.RhythmDayPalette
import com.rhythmday.app.core.designsystem.scoreAccentColor
import com.rhythmday.app.core.domain.model.DayEntry
import com.rhythmday.app.feature.home.domain.HomeDashboard
import com.rhythmday.app.feature.home.domain.WeekSummary
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.toLocalDateTime
import kotlin.math.roundToInt
import kotlin.time.Instant

private val CardShape = RoundedCornerShape(8.dp)
private val ControlBackground = Color(0xFFEDF3F5)
private val TagBackground = Color(0xFFE9F4F2)
private val ProgressTrack = Color(0xFFDFEBE9)

@Composable
fun HomeRoute(
    viewModel: HomeViewModel,
    contentPadding: PaddingValues,
    onCreateEntry: () -> Unit,
    onStartBlitz: () -> Unit,
    onEditEntry: (Long) -> Unit,
    onOpenHistory: () -> Unit,
    onOpenAnalytics: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenEntry: (Long) -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val dashboard = state.dashboard

    when {
        state.isLoading -> Box(
            modifier = Modifier.fillMaxSize().padding(contentPadding),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = RhythmDayPalette.Teal)
        }

        dashboard != null -> HomeScreen(
            dashboard = dashboard,
            contentPadding = contentPadding,
            onCreateEntry = onCreateEntry,
            onEditEntry = onEditEntry,
            onStartBlitz = onStartBlitz,
            onOpenHistory = onOpenHistory,
            onOpenAnalytics = onOpenAnalytics,
            onOpenSettings = onOpenSettings,
            onOpenEntry = onOpenEntry
        )

        else -> Box(
            modifier = Modifier.fillMaxSize().padding(contentPadding).padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = state.errorMessage.orEmpty(),
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun HomeScreen(
    dashboard: HomeDashboard,
    contentPadding: PaddingValues,
    onCreateEntry: () -> Unit,
    onEditEntry: (Long) -> Unit,
    onStartBlitz: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenAnalytics: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenEntry: (Long) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 18.dp,
            top = contentPadding.calculateTopPadding() + 2.dp,
            end = 18.dp,
            bottom = contentPadding.calculateBottomPadding() + 16.dp
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            HomeHeader(dashboard.currentDate, onOpenSettings)
        }
        item {
            TodayCard(
                entry = dashboard.todayEntry,
                onCreateEntry = onCreateEntry,
                onEditEntry = onEditEntry,
                onStartBlitz = onStartBlitz,
                onOpenHistory = onOpenHistory,
                onOpenEntry = onOpenEntry
            )
        }
        item {
            WeekCard(dashboard.weekSummary, onOpenAnalytics)
        }
        dashboard.previousEntry?.let { entry ->
            item {
                PreviousEntryCard(
                    entry = entry,
                    currentDate = dashboard.currentDate,
                    onClick = { onOpenEntry(entry.id) }
                )
            }
        }
    }
}

@Composable
private fun HomeHeader(
    date: LocalDate,
    onOpenSettings: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = 46.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = date.asFullDate(),
            style = MaterialTheme.typography.headlineSmall.copy(
                fontSize = 23.sp,
                lineHeight = 26.sp
            ),
            color = RhythmDayPalette.Ink,
            modifier = Modifier.weight(1f)
        )
        Box {
            PrototypeIconButton(
                icon = RhythmDayIcons.Profile,
                contentDescription = "Открыть меню профиля",
                onClick = { menuExpanded = true },
                size = 42.dp,
                iconSize = 21.dp
            )
            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false },
                containerColor = RhythmDayPalette.Panel,
                shape = CardShape
            ) {
                DropdownMenuItem(
                    text = { Text("Профиль") },
                    onClick = {},
                    enabled = false
                )
                DropdownMenuItem(
                    text = { Text("Настройки") },
                    onClick = {
                        menuExpanded = false
                        onOpenSettings()
                    }
                )
            }
        }
    }
}

@Composable
private fun TodayCard(
    entry: DayEntry?,
    onCreateEntry: () -> Unit,
    onEditEntry: (Long) -> Unit,
    onStartBlitz: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenEntry: (Long) -> Unit
) {
    val clickModifier = if (entry == null) {
        Modifier
    } else {
        Modifier.clickable(role = Role.Button) { onOpenEntry(entry.id) }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(
                Brush.linearGradient(
                    colors = listOf(Color(0xF54F7379), Color(0xF526978C))
                )
            )
            .then(clickModifier)
            .padding(16.dp)
    ) {
        if (entry == null) {
            EmptyTodayContent(onCreateEntry, onStartBlitz)
        } else {
            FilledTodayContent(
                entry = entry,
                onEdit = { onEditEntry(entry.id) },
                onOpenHistory = onOpenHistory
            )
        }
    }
}

@Composable
private fun EmptyTodayContent(
    onCreateEntry: () -> Unit,
    onStartBlitz: () -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            StatusPill("День еще не записан")
            StatusPill("≈1 мин")
        }
        Text(
            text = "Как прошел день?",
            fontSize = 25.sp,
            lineHeight = 28.sp,
            fontWeight = FontWeight.Black,
            color = Color.White,
            modifier = Modifier.padding(top = 18.dp)
        )
        Text(
            text = "Можно сохранить только оценки. Текст, теги и голосовое описание добавишь, если будет ресурс.",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.82f),
            modifier = Modifier
                .padding(top = 8.dp)
                .widthIn(max = 286.dp)
        )
        Column(
            modifier = Modifier.padding(top = 18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            PrototypeButton(
                text = "Заполнить день",
                onClick = onCreateEntry,
                showAddIcon = true
            )
            PrototypeButton(
                text = "Блиц-опрос",
                onClick = onStartBlitz
            )
        }
    }
}

@Composable
private fun FilledTodayContent(
    entry: DayEntry,
    onEdit: () -> Unit,
    onOpenHistory: () -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            StatusPill("День записан")
            StatusPill(entry.savedAt.asLocalTime())
        }
        Text(
            text = "Опрос сохранен",
            fontSize = 25.sp,
            lineHeight = 28.sp,
            fontWeight = FontWeight.Black,
            color = Color.White,
            modifier = Modifier.padding(top = 18.dp)
        )
        Text(
            text = "Основные оценки сохранены. Можно вернуться и дописать текст или теги позже.",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.82f),
            modifier = Modifier.padding(top = 8.dp)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp)
                .clip(CardShape)
                .background(Color.White.copy(alpha = 0.24f))
                .padding(12.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "ОБЩАЯ ОЦЕНКА",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White.copy(alpha = 0.72f)
            )
            Text(
                text = entry.overallScore.asScore(),
                fontSize = 42.sp,
                lineHeight = 42.sp,
                fontWeight = FontWeight.Black,
                color = Color.White
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            FilledMetric("энергия", entry.metrics.energy, Modifier.weight(1f))
            FilledMetric("настроение", entry.metrics.mood, Modifier.weight(1f))
            FilledMetric("стресс", entry.metrics.stress, Modifier.weight(1f))
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 14.dp),
            horizontalArrangement = Arrangement.End
        ) {
            PrototypeIconButton(
                icon = Icons.Outlined.Edit,
                contentDescription = "Редактировать запись",
                onClick = onEdit,
                filled = true
            )
            Spacer(modifier = Modifier.size(8.dp))
            PrototypeIconButton(
                icon = RhythmDayIcons.Calendar,
                contentDescription = "Открыть историю",
                onClick = onOpenHistory,
                filled = true
            )
        }
    }
}

@Composable
private fun FilledMetric(label: String, value: Double, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .defaultMinSize(minHeight = 48.dp)
            .clip(CardShape)
            .background(Color.White.copy(alpha = 0.20f))
            .padding(8.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = value.asMetricValue(),
            fontSize = 16.sp,
            lineHeight = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Text(
            text = label,
            fontSize = 10.sp,
            lineHeight = 12.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White.copy(alpha = 0.72f)
        )
    }
}

@Composable
private fun StatusPill(text: String) {
    Box(
        modifier = Modifier
            .heightIn(min = 30.dp)
            .clip(CardShape)
            .background(Color.White.copy(alpha = 0.22f))
            .padding(horizontal = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 12.sp,
            lineHeight = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White.copy(alpha = 0.9f),
            maxLines = 1
        )
    }
}

@Composable
private fun PrototypeButton(
    text: String,
    onClick: () -> Unit,
    showAddIcon: Boolean = false
) {
    val background = Color.White.copy(alpha = 0.18f)
    val contentColor = Color.White

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(46.dp)
            .clip(CardShape)
            .background(background)
            .clickable(role = Role.Button, onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (showAddIcon) {
            Icon(
                Icons.Filled.Add,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.size(4.dp))
        }
        Text(
            text = text,
            fontSize = 14.sp,
            lineHeight = 18.sp,
            fontWeight = FontWeight.Bold,
            color = contentColor,
            maxLines = 1
        )
    }
}

@Composable
private fun WeekCard(
    summary: WeekSummary,
    onOpenAnalytics: () -> Unit
) {
    PrototypeCard {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, top = 5.dp, end = 5.dp, bottom = 5.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Неделя коротко",
                    style = MaterialTheme.typography.titleLarge,
                    color = RhythmDayPalette.Ink
                )
                PrototypeIconButton(
                    icon = RhythmDayIcons.Analytics,
                    contentDescription = "Открыть аналитику",
                    onClick = onOpenAnalytics,
                    size = 34.dp,
                    iconSize = 18.dp,
                    filled = true
                )
            }
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                color = RhythmDayPalette.Line,
                thickness = 1.dp
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                WeekStat(
                    top = "заполнено",
                    value = "${summary.completedDays}/7",
                    bottom = "дней",
                    progress = summary.completedDays.coerceIn(0, 7) / 7f,
                    modifier = Modifier.weight(1f)
                )
                WeekStat(
                    top = "средняя",
                    value = summary.averageScore?.asScore() ?: "—",
                    bottom = "оценка",
                    modifier = Modifier.weight(1f)
                )
                WeekStat(
                    top = "лучший день",
                    value = summary.bestEntry?.overallScore?.asScore() ?: "—",
                    bottom = summary.bestEntry?.date?.dayOfWeek?.asName() ?: "нет данных",
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun WeekStat(
    top: String,
    value: String,
    bottom: String,
    progress: Float? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .padding(horizontal = 2.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = top,
            fontSize = 11.sp,
            lineHeight = 14.sp,
            fontWeight = FontWeight.Bold,
            color = RhythmDayPalette.Muted,
            maxLines = 1,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = value,
            fontSize = 21.sp,
            lineHeight = 24.sp,
            fontWeight = FontWeight.Black,
            color = Color(0xFF174E51),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = bottom,
            fontSize = 11.sp,
            lineHeight = 14.sp,
            fontWeight = FontWeight.Bold,
            color = RhythmDayPalette.Muted,
            maxLines = 1,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        if (progress != null) {
            Box(
                modifier = Modifier
                    .padding(top = 3.dp)
                    .width(58.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(ProgressTrack)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress.coerceIn(0f, 1f))
                        .fillMaxHeight()
                        .background(RhythmDayPalette.Teal)
                )
            }
        }
    }
}

@Composable
private fun PreviousEntryCard(
    entry: DayEntry,
    currentDate: LocalDate,
    onClick: () -> Unit
) {
    PrototypeCard(onClick = onClick) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 11.dp)
            ) {
                Text(
                    text = "Предыдущая запись",
                    style = MaterialTheme.typography.titleLarge,
                    color = RhythmDayPalette.Ink
                )
            }
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 14.dp),
                color = RhythmDayPalette.Line,
                thickness = 1.dp
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 14.dp, top = 14.dp, end = 14.dp, bottom = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    entry.title?.let { title ->
                        Text(
                            text = title,
                            fontSize = 15.sp,
                            lineHeight = 19.sp,
                            fontWeight = FontWeight.Bold,
                            color = RhythmDayPalette.TealDark
                        )
                    }
                    Text(
                        text = entry.date.asRelativeDate(currentDate),
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = RhythmDayPalette.Muted,
                        modifier = Modifier.padding(top = if (entry.title == null) 0.dp else 2.dp)
                    )
                    Text(
                        text = "Энергия ${entry.metrics.energy.asMetricValue()}, " +
                            "настроение ${entry.metrics.mood.asMetricValue()}, " +
                            "стресс ${entry.metrics.stress.asMetricValue()}.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = RhythmDayPalette.Muted,
                        modifier = Modifier.padding(top = if (entry.title == null) 2.dp else 4.dp)
                    )
                    FlowRow(
                        modifier = Modifier.padding(top = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        verticalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        entry.tags.forEach { tag ->
                            Box(
                                modifier = Modifier
                                    .heightIn(min = 22.dp)
                                    .clip(CardShape)
                                    .background(TagBackground)
                                    .padding(horizontal = 7.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = tag,
                                    fontSize = 11.sp,
                                    lineHeight = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = RhythmDayPalette.TealDark
                                )
                            }
                        }
                    }
                }
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CardShape)
                        .background(scoreAccentColor(entry.overallScore)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = entry.overallScore.asScore(),
                        fontSize = 22.sp,
                        lineHeight = 24.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun PrototypeCard(
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val clickModifier = if (onClick == null) {
        Modifier
    } else {
        Modifier.clickable(role = Role.Button, onClick = onClick)
    }

    Surface(
        modifier = Modifier.fillMaxWidth().then(clickModifier),
        shape = CardShape,
        color = RhythmDayPalette.Panel,
        content = content
    )
}

@Composable
private fun PrototypeIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    size: androidx.compose.ui.unit.Dp = 42.dp,
    iconSize: androidx.compose.ui.unit.Dp = 20.dp,
    tint: Color = RhythmDayPalette.Ink,
    background: Color = ControlBackground,
    filled: Boolean = false,
    enabled: Boolean = true
) {
    val clickModifier = if (enabled) {
        Modifier.clickable(role = Role.Button, onClick = onClick)
    } else {
        Modifier
    }
    Box(
        modifier = Modifier
            .size(size)
            .clip(CardShape)
            .background(if (filled) Color.White.copy(alpha = 0.94f) else background)
            .then(clickModifier),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (filled) RhythmDayPalette.TealDark else tint,
            modifier = Modifier.size(iconSize)
        )
    }
}

private fun Double.asScore(): String {
    val tenths = (this * 10).roundToInt()
    return "${tenths / 10}.${tenths % 10}"
}

private fun Double.asMetricValue(): String {
    val tenths = (this * 10).roundToInt()
    return if (tenths % 10 == 0) {
        (tenths / 10).toString()
    } else {
        "${tenths / 10}.${tenths % 10}"
    }
}

private fun LocalDate.asFullDate(): String =
    "${dayOfWeek.asName()}, $day ${monthNames[month.ordinal]}".replaceFirstChar(Char::uppercase)

private fun LocalDate.asRelativeDate(today: LocalDate): String {
    val prefix = if (today.toEpochDays() - toEpochDays() == 1L) {
        "Вчера"
    } else {
        dayOfWeek.asName().replaceFirstChar(Char::uppercase)
    }
    return "$prefix, $day ${monthNames[month.ordinal]}"
}

private fun Instant?.asLocalTime(): String {
    if (this == null) return "—"

    val time = toLocalDateTime(TimeZone.currentSystemDefault()).time
    return "${time.hour.toString().padStart(2, '0')}:${time.minute.toString().padStart(2, '0')}"
}

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
