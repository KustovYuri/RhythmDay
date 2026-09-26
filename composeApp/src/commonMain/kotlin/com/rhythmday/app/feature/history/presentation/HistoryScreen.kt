package com.rhythmday.app.feature.history.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rhythmday.app.core.designsystem.RhythmDayPalette
import com.rhythmday.app.core.designsystem.scoreColors
import com.rhythmday.app.core.domain.model.DayEntry
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlin.math.roundToInt

private val CardShape = RoundedCornerShape(8.dp)
private val EmptyCellBackground = Color(0xFFFBFCFD)
private val TagBackground = Color(0xFFE9F4F2)
private val NavigationButtonBackground = Color(0xFFEDF3F5)
private val DisabledNavigationButtonBackground = Color(0xFFF3F6F7)

@Composable
fun HistoryRoute(
    viewModel: HistoryViewModel,
    contentPadding: PaddingValues,
    onOpenEntry: (Long) -> Unit,
    onCreateEntry: (LocalDate) -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    HistoryScreen(
        state = state,
        contentPadding = contentPadding,
        onPreviousMonth = viewModel::showPreviousMonth,
        onNextMonth = viewModel::showNextMonth,
        onOpenEntry = onOpenEntry,
        onCreateEntry = onCreateEntry
    )
}

@Composable
private fun HistoryScreen(
    state: HistoryUiState,
    contentPadding: PaddingValues,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onOpenEntry: (Long) -> Unit,
    onCreateEntry: (LocalDate) -> Unit
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
            Text(
                text = "История",
                fontSize = 27.sp,
                lineHeight = 30.sp,
                fontWeight = FontWeight.Black,
                color = RhythmDayPalette.Ink,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }

        when {
            state.isLoading -> item {
                Box(
                    modifier = Modifier.fillMaxWidth().heightIn(min = 240.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = RhythmDayPalette.Teal)
                }
            }

            state.errorMessage != null -> item {
                HistoryCard {
                    Text(
                        text = state.errorMessage,
                        modifier = Modifier.fillMaxWidth(),
                        fontSize = 13.sp,
                        lineHeight = 19.sp,
                        color = RhythmDayPalette.Muted,
                        textAlign = TextAlign.Center
                    )
                }
            }

            else -> {
                item {
                    CalendarCard(
                        state = state,
                        onPreviousMonth = onPreviousMonth,
                        onNextMonth = onNextMonth,
                        onOpenEntry = onOpenEntry,
                        onCreateEntry = onCreateEntry
                    )
                }
                item {
                    EntryListCard(
                        entries = state.entries,
                        onOpenEntry = onOpenEntry
                    )
                }
            }
        }
    }
}

@Composable
private fun CalendarCard(
    state: HistoryUiState,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onOpenEntry: (Long) -> Unit,
    onCreateEntry: (LocalDate) -> Unit
) {
    HistoryCard {
        MonthSwitcher(
            month = state.selectedMonth,
            previousEnabled = state.canShowPreviousMonth,
            nextEnabled = state.canShowNextMonth,
            onPreviousMonth = onPreviousMonth,
            onNextMonth = onNextMonth
        )
        CalendarGrid(
            month = state.selectedMonth,
            currentDate = state.currentDate,
            entries = state.entries,
            onOpenEntry = onOpenEntry,
            onCreateEntry = onCreateEntry,
            modifier = Modifier.padding(top = 12.dp)
        )
    }
}

@Composable
private fun MonthSwitcher(
    month: LocalDate,
    previousEnabled: Boolean,
    nextEnabled: Boolean,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        MonthNavigationButton(
            icon = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
            contentDescription = "Предыдущий месяц",
            enabled = previousEnabled,
            onClick = onPreviousMonth
        )
        Text(
            text = month.asMonthTitle(),
            modifier = Modifier.weight(1f),
            fontSize = 19.sp,
            lineHeight = 23.sp,
            fontWeight = FontWeight.Bold,
            color = RhythmDayPalette.Ink,
            textAlign = TextAlign.Center
        )
        MonthNavigationButton(
            icon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = "Следующий месяц",
            enabled = nextEnabled,
            onClick = onNextMonth
        )
    }
}

@Composable
private fun MonthNavigationButton(
    icon: ImageVector,
    contentDescription: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val background = if (enabled) {
        NavigationButtonBackground
    } else {
        DisabledNavigationButtonBackground
    }
    val iconTint = if (enabled) {
        RhythmDayPalette.TealDark
    } else {
        RhythmDayPalette.TealDark.copy(alpha = 0.38f)
    }

    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(CardShape)
            .background(background)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = iconTint,
            modifier = Modifier.size(22.dp)
        )
    }
}

@Composable
private fun CalendarGrid(
    month: LocalDate,
    currentDate: LocalDate,
    entries: List<DayEntry>,
    onOpenEntry: (Long) -> Unit,
    onCreateEntry: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            calendarWeekdays.forEach { weekday ->
                Box(
                    modifier = Modifier.weight(1f).height(26.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = weekday,
                        fontSize = 13.sp,
                        lineHeight = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = RhythmDayPalette.Muted
                    )
                }
            }
        }

        calendarCells(month, entries).chunked(7).forEach { week ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                week.forEach { day ->
                    CalendarDayCell(
                        day = day,
                        currentDate = currentDate,
                        onOpenEntry = onOpenEntry,
                        onCreateEntry = onCreateEntry,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun CalendarDayCell(
    day: CalendarDay?,
    currentDate: LocalDate,
    onOpenEntry: (Long) -> Unit,
    onCreateEntry: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    if (day == null) {
        Box(modifier = modifier.height(42.dp))
        return
    }

    val entry = day.entry
    val colors = entry?.overallScore?.let(::scoreColors)
    val shape = CardShape
    val clickModifier = when {
        entry != null -> Modifier.clickable(role = Role.Button) { onOpenEntry(entry.id) }
        day.date < currentDate -> Modifier.clickable(role = Role.Button) {
            onCreateEntry(day.date)
        }
        else -> Modifier
    }

    Box(
        modifier = modifier
            .height(42.dp)
            .clip(shape)
            .background(colors?.container ?: EmptyCellBackground)
            .border(1.dp, colors?.border ?: RhythmDayPalette.Line, shape)
            .then(clickModifier),
        contentAlignment = Alignment.Center
    ) {
        if (entry == null) {
            Text(
                text = day.number.toString(),
                fontSize = 13.sp,
                lineHeight = 15.sp,
                fontWeight = FontWeight.Bold,
                color = RhythmDayPalette.Ink
            )
        } else {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = day.number.toString(),
                    fontSize = 13.sp,
                    lineHeight = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors!!.content.copy(alpha = 0.84f)
                )
                Box(
                    modifier = Modifier
                        .size(width = 26.dp, height = 14.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(colors.accent),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = entry.overallScore.asDisplayScore(),
                        fontSize = 9.sp,
                        lineHeight = 10.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun EntryListCard(
    entries: List<DayEntry>,
    onOpenEntry: (Long) -> Unit
) {
    HistoryCard {
        Text(
            text = "Записи месяца",
            fontSize = 18.sp,
            lineHeight = 22.sp,
            fontWeight = FontWeight.Bold,
            color = RhythmDayPalette.Ink
        )

        if (entries.isEmpty()) {
            Text(
                text = "В этом месяце пока нет записей",
                modifier = Modifier.fillMaxWidth().padding(top = 18.dp, bottom = 8.dp),
                fontSize = 13.sp,
                lineHeight = 19.sp,
                color = RhythmDayPalette.Muted,
                textAlign = TextAlign.Center
            )
        } else {
            Column(
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                entries.forEach { entry ->
                    EntryRow(
                        entry = entry,
                        onClick = { onOpenEntry(entry.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun EntryRow(
    entry: DayEntry,
    onClick: () -> Unit
) {
    val colors = scoreColors(entry.overallScore)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(EmptyCellBackground)
            .border(1.dp, RhythmDayPalette.Line, CardShape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(10.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            entry.title?.let { title ->
                Text(
                    text = title,
                    fontSize = 14.sp,
                    lineHeight = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = RhythmDayPalette.TealDark
                )
            }
            Text(
                text = entry.date.asEntryDate(),
                fontSize = if (entry.title == null) 14.sp else 12.sp,
                lineHeight = if (entry.title == null) 17.sp else 15.sp,
                fontWeight = FontWeight.Bold,
                color = if (entry.title == null) RhythmDayPalette.Ink else RhythmDayPalette.Muted,
                modifier = Modifier.padding(top = if (entry.title == null) 0.dp else 2.dp)
            )
            if (entry.tags.isNotEmpty()) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
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
        }
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CardShape)
                .background(colors.accent),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = entry.overallScore.asDisplayScore(),
                fontSize = 20.sp,
                lineHeight = 22.sp,
                fontWeight = FontWeight.Black,
                color = Color.White
            )
        }
    }
}

@Composable
private fun HistoryCard(content: @Composable () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = CardShape,
        color = RhythmDayPalette.Panel
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            content()
        }
    }
}

private data class CalendarDay(
    val date: LocalDate,
    val number: Int,
    val entry: DayEntry?
)

private fun calendarCells(month: LocalDate, entries: List<DayEntry>): List<CalendarDay?> {
    val entriesByDay = entries.associateBy { entry -> entry.date.day }
    val firstWeekdayOffset = month.dayOfWeek.isoDayNumber - 1
    val lastDay = month.plus(1, DateTimeUnit.MONTH).minus(1, DateTimeUnit.DAY).day
    val cells = MutableList<CalendarDay?>(firstWeekdayOffset) { null }

    (1..lastDay).forEach { day ->
        cells += CalendarDay(
            date = LocalDate(month.year, month.month, day),
            number = day,
            entry = entriesByDay[day]
        )
    }
    while (cells.size % 7 != 0) cells += null

    return cells
}

private fun Double.asDisplayScore(): String {
    val tenths = (this * 10).roundToInt()
    return if (tenths % 10 == 0) {
        (tenths / 10).toString()
    } else {
        "${tenths / 10}.${tenths % 10}"
    }
}

private fun LocalDate.asMonthTitle(): String = "${monthNames[month.ordinal]} $year"

private fun LocalDate.asEntryDate(): String =
    "${dayOfWeek.asName()}, $day ${monthNamesGenitive[month.ordinal]}"

private fun DayOfWeek.asName(): String = weekdayNames[isoDayNumber - 1]

private val calendarWeekdays = listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс")

private val weekdayNames = listOf(
    "Понедельник",
    "Вторник",
    "Среда",
    "Четверг",
    "Пятница",
    "Суббота",
    "Воскресенье"
)

private val monthNames = listOf(
    "Январь",
    "Февраль",
    "Март",
    "Апрель",
    "Май",
    "Июнь",
    "Июль",
    "Август",
    "Сентябрь",
    "Октябрь",
    "Ноябрь",
    "Декабрь"
)

private val monthNamesGenitive = listOf(
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
