package com.rhythmday.app.feature.analytics.presentation

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rhythmday.app.core.designsystem.RhythmDayPalette
import com.rhythmday.app.core.domain.model.DayEntry
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.time.Instant

private val CardShape = RoundedCornerShape(8.dp)
private val SegmentBackground = Color(0xFFE8EEF3)
private val FieldBackground = Color(0xFFF0F4F7)
private val SelectedChipBackground = Color(0xFFCBE8E3)
private val GridLine = Color(0xFFE2E9EE)
private val SleepColor = Color(0xFF3F63D9)
private val ClarityColor = Color(0xFF2F8CA8)
private val WellbeingColor = Color(0xFF7D6AC9)
private val SocialityColor = Color(0xFFD1783F)
private val StressColor = Color(0xFFD96A58)
private val ChartHeight = 292.dp
private val ChartAxisWidth = 30.dp
private val ChartTopPadding = 16.dp
private val ChartBottomPadding = 32.dp
private const val ChartPointSpacing = 52f
private const val MinimumChartZoom = 1f
private const val MaximumChartZoom = 4f

@Composable
fun AnalyticsRoute(
    viewModel: AnalyticsViewModel,
    contentPadding: PaddingValues,
    onOpenEntry: (Long) -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    AnalyticsScreen(
        state = state,
        contentPadding = contentPadding,
        onRangeSelected = viewModel::selectRange,
        onModeSelected = viewModel::selectMode,
        onMetricToggled = viewModel::toggleMetric,
        onMetricOpened = viewModel::showMetric,
        onPeriodStartChanged = viewModel::setPeriodStart,
        onPeriodEndChanged = viewModel::setPeriodEnd,
        onOpenEntry = onOpenEntry
    )
}

@Composable
private fun AnalyticsScreen(
    state: AnalyticsUiState,
    contentPadding: PaddingValues,
    onRangeSelected: (AnalyticsRange) -> Unit,
    onModeSelected: (AnalyticsMode) -> Unit,
    onMetricToggled: (AnalyticsMetric) -> Unit,
    onMetricOpened: (AnalyticsMetric) -> Unit,
    onPeriodStartChanged: (LocalDate) -> Unit,
    onPeriodEndChanged: (LocalDate) -> Unit,
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
            Text(
                text = "Аналитика",
                fontSize = 27.sp,
                lineHeight = 30.sp,
                fontWeight = FontWeight.Black,
                color = RhythmDayPalette.Ink,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }
        item {
            RangeSwitch(
                selectedRange = state.range,
                onRangeSelected = onRangeSelected
            )
        }
        if (state.range == AnalyticsRange.PERIOD) {
            item {
                PeriodFields(
                    startDate = state.periodStart,
                    endDate = state.periodEnd,
                    onStartDateChanged = onPeriodStartChanged,
                    onEndDateChanged = onPeriodEndChanged
                )
            }
        }
        item {
            ModeSwitch(
                selectedMode = state.mode,
                onModeSelected = onModeSelected
            )
        }
        if (state.mode == AnalyticsMode.CHART) {
            item {
                MetricPicker(
                    selectedMetrics = state.selectedMetrics,
                    onMetricToggled = onMetricToggled
                )
            }
        }

        when {
            state.isLoading -> item {
                Box(
                    modifier = Modifier.fillMaxWidth().heightIn(min = 260.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = RhythmDayPalette.Teal)
                }
            }

            state.errorMessage != null -> item {
                AnalyticsCard {
                    EmptyAnalyticsMessage(state.errorMessage)
                }
            }

            state.mode == AnalyticsMode.CHART -> item {
                ChartPanel(state = state, onOpenEntry = onOpenEntry)
            }

            else -> item {
                OverviewPanel(
                    state = state,
                    onMetricOpened = onMetricOpened
                )
            }
        }
    }
}

@Composable
private fun RangeSwitch(
    selectedRange: AnalyticsRange,
    onRangeSelected: (AnalyticsRange) -> Unit
) {
    SegmentedControl {
        AnalyticsRange.entries.forEach { range ->
            SegmentButton(
                text = range.label,
                selected = range == selectedRange,
                onClick = { onRangeSelected(range) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun ModeSwitch(
    selectedMode: AnalyticsMode,
    onModeSelected: (AnalyticsMode) -> Unit
) {
    SegmentedControl {
        AnalyticsMode.entries.forEach { mode ->
            SegmentButton(
                text = mode.label,
                selected = mode == selectedMode,
                onClick = { onModeSelected(mode) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun SegmentedControl(content: @Composable RowScope.() -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(SegmentBackground)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        content = content
    )
}

@Composable
private fun SegmentButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(34.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(if (selected) RhythmDayPalette.Panel else Color.Transparent)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 13.sp,
            lineHeight = 16.sp,
            fontWeight = FontWeight.Bold,
            color = if (selected) RhythmDayPalette.Ink else RhythmDayPalette.Muted,
            maxLines = 1
        )
    }
}

@Composable
private fun PeriodFields(
    startDate: LocalDate,
    endDate: LocalDate,
    onStartDateChanged: (LocalDate) -> Unit,
    onEndDateChanged: (LocalDate) -> Unit
) {
    var dateTarget by remember { mutableStateOf<DateTarget?>(null) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        PeriodDateField(
            label = "С",
            date = startDate,
            onClick = { dateTarget = DateTarget.START },
            modifier = Modifier.weight(1f)
        )
        PeriodDateField(
            label = "По",
            date = endDate,
            onClick = { dateTarget = DateTarget.END },
            modifier = Modifier.weight(1f)
        )
    }

    dateTarget?.let { target ->
        AnalyticsDatePickerDialog(
            initialDate = if (target == DateTarget.START) startDate else endDate,
            onDismiss = { dateTarget = null },
            onDateSelected = { date ->
                if (target == DateTarget.START) {
                    onStartDateChanged(date)
                } else {
                    onEndDateChanged(date)
                }
                dateTarget = null
            }
        )
    }
}

@Composable
private fun PeriodDateField(
    label: String,
    date: LocalDate,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .height(42.dp)
            .clip(CardShape)
            .background(FieldBackground)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            lineHeight = 14.sp,
            fontWeight = FontWeight.Bold,
            color = RhythmDayPalette.Muted
        )
        Text(
            text = date.asNumericDate(),
            modifier = Modifier.weight(1f),
            fontSize = 13.sp,
            lineHeight = 16.sp,
            fontWeight = FontWeight.Bold,
            color = RhythmDayPalette.Ink
        )
        Icon(
            imageVector = Icons.Outlined.CalendarMonth,
            contentDescription = null,
            tint = RhythmDayPalette.TealDark,
            modifier = Modifier.size(17.dp)
        )
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun AnalyticsDatePickerDialog(
    initialDate: LocalDate,
    onDismiss: () -> Unit,
    onDateSelected: (LocalDate) -> Unit
) {
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialDate
            .atStartOfDayIn(TimeZone.UTC)
            .toEpochMilliseconds()
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    datePickerState.selectedDateMillis
                        ?.let(Instant::fromEpochMilliseconds)
                        ?.toLocalDateTime(TimeZone.UTC)
                        ?.date
                        ?.let(onDateSelected)
                }
            ) {
                Text("Готово")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    ) {
        DatePicker(state = datePickerState)
    }
}

@Composable
private fun MetricPicker(
    selectedMetrics: Set<AnalyticsMetric>,
    onMetricToggled: (AnalyticsMetric) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        AnalyticsMetric.entries.forEach { metric ->
            val selected = metric in selectedMetrics
            Box(
                modifier = Modifier
                    .height(34.dp)
                    .clip(CardShape)
                    .background(if (selected) SelectedChipBackground else FieldBackground)
                    .clickable(role = Role.Checkbox) { onMetricToggled(metric) }
                    .padding(horizontal = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = metric.label,
                    fontSize = 13.sp,
                    lineHeight = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (selected) RhythmDayPalette.TealDark else RhythmDayPalette.Ink,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun ChartPanel(
    state: AnalyticsUiState,
    onOpenEntry: (Long) -> Unit
) {
    val selectedMetrics = AnalyticsMetric.entries.filter { metric ->
        metric in state.selectedMetrics
    }
    if (selectedMetrics.isEmpty() || state.entries.isEmpty()) {
        RangeSummaryCard(state.rangeSubtitle())
        return
    }
    val primaryMetric = selectedMetrics.first()

    AnalyticsCard {
        Text(
            text = if (selectedMetrics.size == 1) primaryMetric.label else "Выбранные показатели",
            fontSize = 18.sp,
            lineHeight = 22.sp,
            fontWeight = FontWeight.Bold,
            color = RhythmDayPalette.Ink
        )
        Text(
            text = state.rangeSubtitle(),
            fontSize = 12.sp,
            lineHeight = 16.sp,
            fontWeight = FontWeight.Bold,
            color = RhythmDayPalette.Muted,
            modifier = Modifier.padding(top = 3.dp)
        )

        LineChart(
            entries = state.entries,
            selectedMetrics = selectedMetrics,
            onOpenEntry = onOpenEntry,
            modifier = Modifier.padding(top = 10.dp)
        )
        MetricLegend(
            metrics = selectedMetrics,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

@Composable
private fun RangeSummaryCard(range: String) {
    AnalyticsCard {
        Text(
            text = "Выбранный диапазон",
            fontSize = 12.sp,
            lineHeight = 16.sp,
            fontWeight = FontWeight.Bold,
            color = RhythmDayPalette.Muted
        )
        Text(
            text = range,
            fontSize = 18.sp,
            lineHeight = 22.sp,
            fontWeight = FontWeight.Bold,
            color = RhythmDayPalette.Ink,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
private fun LineChart(
    entries: List<DayEntry>,
    selectedMetrics: List<AnalyticsMetric>,
    onOpenEntry: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth().height(ChartHeight),
        verticalAlignment = Alignment.Top
    ) {
        ChartYAxis(
            modifier = Modifier.width(ChartAxisWidth).height(ChartHeight)
        )
        BoxWithConstraints(
            modifier = Modifier.weight(1f).height(ChartHeight)
        ) {
            val scrollState = rememberScrollState()
            var zoom by remember(
                entries.firstOrNull()?.id,
                entries.lastOrNull()?.id,
                entries.size
            ) {
                mutableFloatStateOf(MinimumChartZoom)
            }
            val transformableState = rememberTransformableState { _, zoomChange, _, _ ->
                zoom = (zoom * zoomChange).coerceIn(MinimumChartZoom, MaximumChartZoom)
            }
            val chartWidth = (
                max(maxWidth.value, entries.size * ChartPointSpacing) * zoom
                ).dp

            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .transformable(transformableState)
                    .horizontalScroll(scrollState)
            ) {
                MetricChartCanvas(
                    entries = entries,
                    selectedMetrics = selectedMetrics,
                    onOpenEntry = onOpenEntry,
                    modifier = Modifier.width(chartWidth).height(ChartHeight)
                )
            }
        }
    }
}

@Composable
private fun ChartYAxis(modifier: Modifier = Modifier) {
    val textMeasurer = rememberTextMeasurer()
    val axisStyle = TextStyle(
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        color = RhythmDayPalette.Muted
    )

    Canvas(modifier = modifier) {
        val top = ChartTopPadding.toPx()
        val bottom = ChartBottomPadding.toPx()
        val plotHeight = size.height - top - bottom

        (0..10).forEach { value ->
            val y = top + ((10f - value) / 10f) * plotHeight
            val label = value.toString()
            val measured = textMeasurer.measure(label, axisStyle)
            drawText(
                textMeasurer = textMeasurer,
                text = label,
                topLeft = Offset(
                    x = size.width - measured.size.width - 5.dp.toPx(),
                    y = (y - measured.size.height / 2f)
                        .coerceIn(0f, size.height - measured.size.height)
                ),
                style = axisStyle
            )
        }
    }
}

@Composable
private fun MetricChartCanvas(
    entries: List<DayEntry>,
    selectedMetrics: List<AnalyticsMetric>,
    onOpenEntry: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()
    val axisStyle = TextStyle(
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        color = RhythmDayPalette.Muted
    )
    val primaryMetric = selectedMetrics.first()

    Canvas(
        modifier = modifier.pointerInput(entries, primaryMetric) {
            detectTapGestures { tap ->
                val left = 8.dp.toPx()
                val right = 14.dp.toPx()
                val top = ChartTopPadding.toPx()
                val bottom = ChartBottomPadding.toPx()
                val plotWidth = (size.width - left - right).coerceAtLeast(0f)
                val plotHeight = (size.height - top - bottom).coerceAtLeast(0f)
                val nearestIndex = entries.indices.minByOrNull { index ->
                    val point = chartPoint(
                        index = index,
                        count = entries.size,
                        value = primaryMetric.value(entries[index]),
                        left = left,
                        top = top,
                        plotWidth = plotWidth,
                        plotHeight = plotHeight
                    )
                    (point.x - tap.x) * (point.x - tap.x) +
                        (point.y - tap.y) * (point.y - tap.y)
                }
                if (nearestIndex != null) {
                    val point = chartPoint(
                        index = nearestIndex,
                        count = entries.size,
                        value = primaryMetric.value(entries[nearestIndex]),
                        left = left,
                        top = top,
                        plotWidth = plotWidth,
                        plotHeight = plotHeight
                    )
                    val radius = 18.dp.toPx()
                    val distanceSquared = (point.x - tap.x) * (point.x - tap.x) +
                        (point.y - tap.y) * (point.y - tap.y)
                    if (distanceSquared <= radius * radius) {
                        onOpenEntry(entries[nearestIndex].id)
                    }
                }
            }
        }
    ) {
        val left = 8.dp.toPx()
        val right = 14.dp.toPx()
        val top = ChartTopPadding.toPx()
        val bottom = ChartBottomPadding.toPx()
        val plotWidth = size.width - left - right
        val plotHeight = size.height - top - bottom

        (0..10).forEach { value ->
            val y = top + ((10f - value) / 10f) * plotHeight
            drawLine(
                color = GridLine,
                start = Offset(left, y),
                end = Offset(size.width - right, y),
                strokeWidth = 1.dp.toPx()
            )
        }

        selectedMetrics.forEach { metric ->
            val path = Path()
            entries.forEachIndexed { index, entry ->
                val point = chartPoint(
                    index = index,
                    count = entries.size,
                    value = metric.value(entry),
                    left = left,
                    top = top,
                    plotWidth = plotWidth,
                    plotHeight = plotHeight
                )
                if (index == 0) path.moveTo(point.x, point.y) else path.lineTo(point.x, point.y)
            }
            drawPath(
                path = path,
                color = metric.color,
                style = Stroke(width = 3.dp.toPx())
            )
            entries.forEachIndexed { index, entry ->
                val point = chartPoint(
                    index = index,
                    count = entries.size,
                    value = metric.value(entry),
                    left = left,
                    top = top,
                    plotWidth = plotWidth,
                    plotHeight = plotHeight
                )
                drawCircle(Color.White, radius = 5.dp.toPx(), center = point)
                drawCircle(metric.color, radius = 3.5.dp.toPx(), center = point)
            }
        }

        entries.forEachIndexed { index, entry ->
            val point = chartPoint(
                index = index,
                count = entries.size,
                value = 0.0,
                left = left,
                top = top,
                plotWidth = plotWidth,
                plotHeight = plotHeight
            )
            val label = entry.date.asChartDate()
            val measured = textMeasurer.measure(label, axisStyle)
            drawText(
                textMeasurer = textMeasurer,
                text = label,
                topLeft = Offset(
                    x = (point.x - measured.size.width / 2f)
                        .coerceIn(0f, size.width - measured.size.width),
                    y = size.height - 20.dp.toPx()
                ),
                style = axisStyle
            )
        }
    }
}

private fun chartPoint(
    index: Int,
    count: Int,
    value: Double,
    left: Float,
    top: Float,
    plotWidth: Float,
    plotHeight: Float
): Offset {
    val x = if (count <= 1) {
        left + plotWidth / 2f
    } else {
        left + plotWidth * index / (count - 1)
    }
    val cleanValue = value.coerceIn(0.0, 10.0)
    val y = top + ((10.0 - cleanValue) / 10.0).toFloat() * plotHeight
    return Offset(x, y)
}

@Composable
private fun MetricLegend(
    metrics: List<AnalyticsMetric>,
    modifier: Modifier = Modifier
) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        metrics.forEach { metric ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(RoundedCornerShape(percent = 50))
                        .background(metric.color)
                )
                Text(
                    text = metric.label,
                    fontSize = 11.sp,
                    lineHeight = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = RhythmDayPalette.Muted
                )
            }
        }
    }
}

@Composable
private fun OverviewPanel(
    state: AnalyticsUiState,
    onMetricOpened: (AnalyticsMetric) -> Unit
) {
    AnalyticsCard {
        Text(
            text = "Все показатели",
            fontSize = 18.sp,
            lineHeight = 22.sp,
            fontWeight = FontWeight.Bold,
            color = RhythmDayPalette.Ink
        )
        Text(
            text = state.rangeSubtitle(),
            fontSize = 12.sp,
            lineHeight = 16.sp,
            fontWeight = FontWeight.Bold,
            color = RhythmDayPalette.Muted,
            modifier = Modifier.padding(top = 3.dp)
        )

        if (state.entries.isEmpty()) {
            EmptyAnalyticsMessage(
                message = "В выбранном диапазоне пока нет записей",
                modifier = Modifier.padding(top = 12.dp)
            )
        } else {
            Column(
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AnalyticsMetric.entries.toList().chunked(2).forEach { rowMetrics ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowMetrics.forEach { metric ->
                            MiniMetricCard(
                                metric = metric,
                                entries = state.entries,
                                onClick = { onMetricOpened(metric) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        if (rowMetrics.size == 1) Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun MiniMetricCard(
    metric: AnalyticsMetric,
    entries: List<DayEntry>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val values = entries.map(metric::value)
    Column(
        modifier = modifier
            .height(116.dp)
            .clip(CardShape)
            .background(FieldBackground)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(10.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(
                text = metric.label,
                fontSize = 13.sp,
                lineHeight = 16.sp,
                fontWeight = FontWeight.Bold,
                color = RhythmDayPalette.Ink
            )
            Text(
                text = values.average().asDisplayValue(),
                fontSize = 20.sp,
                lineHeight = 21.sp,
                fontWeight = FontWeight.Black,
                color = metric.color,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
        MiniLineChart(
            values = values,
            color = metric.color,
            modifier = Modifier.fillMaxWidth().height(42.dp)
        )
    }
}

@Composable
private fun MiniLineChart(
    values: List<Double>,
    color: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        if (values.isEmpty()) return@Canvas
        val path = Path()
        values.forEachIndexed { index, value ->
            val x = if (values.size <= 1) size.width / 2f else size.width * index / (values.size - 1)
            val y = ((10.0 - value.coerceIn(1.0, 10.0)) / 9.0).toFloat() * size.height
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path = path, color = color, style = Stroke(width = 2.5.dp.toPx()))
    }
}

@Composable
private fun AnalyticsCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = CardShape,
        color = RhythmDayPalette.Panel
    ) {
        Column(modifier = Modifier.padding(14.dp), content = content)
    }
}

@Composable
private fun EmptyAnalyticsMessage(
    message: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 80.dp)
            .clip(CardShape)
            .background(FieldBackground)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            fontSize = 13.sp,
            lineHeight = 19.sp,
            color = RhythmDayPalette.Muted,
            textAlign = TextAlign.Center
        )
    }
}

private val AnalyticsRange.label: String
    get() = when (this) {
        AnalyticsRange.WEEK -> "Неделя"
        AnalyticsRange.MONTH -> "Месяц"
        AnalyticsRange.PERIOD -> "Период"
    }

private val AnalyticsMode.label: String
    get() = when (this) {
        AnalyticsMode.CHART -> "График"
        AnalyticsMode.OVERVIEW -> "Все показатели"
    }

private val AnalyticsMetric.label: String
    get() = when (this) {
        AnalyticsMetric.SCORE -> "Оценка"
        AnalyticsMetric.SLEEP -> "Сон"
        AnalyticsMetric.MENTAL_CLARITY -> "Ясность"
        AnalyticsMetric.ENERGY -> "Энергия"
        AnalyticsMetric.MOOD -> "Настроение"
        AnalyticsMetric.WELLBEING -> "Самочувствие"
        AnalyticsMetric.SOCIALITY -> "Социальность"
        AnalyticsMetric.STRESS -> "Стресс"
    }

private val AnalyticsMetric.color: Color
    get() = when (this) {
        AnalyticsMetric.SCORE -> RhythmDayPalette.Teal
        AnalyticsMetric.SLEEP -> SleepColor
        AnalyticsMetric.MENTAL_CLARITY -> ClarityColor
        AnalyticsMetric.ENERGY -> RhythmDayPalette.Amber
        AnalyticsMetric.MOOD -> RhythmDayPalette.Green
        AnalyticsMetric.WELLBEING -> WellbeingColor
        AnalyticsMetric.SOCIALITY -> SocialityColor
        AnalyticsMetric.STRESS -> StressColor
    }

private fun AnalyticsUiState.rangeSubtitle(): String = when (range) {
    AnalyticsRange.MONTH -> "${monthNamesNominative[rangeStart.month.ordinal]} ${rangeStart.year}"
    AnalyticsRange.WEEK,
    AnalyticsRange.PERIOD -> formatDateRange(rangeStart, rangeEnd)
}

private fun formatDateRange(start: LocalDate, end: LocalDate): String = when {
    start == end -> "${start.day} ${monthNamesGenitive[start.month.ordinal]}"
    start.year == end.year && start.month == end.month ->
        "${start.day} - ${end.day} ${monthNamesGenitive[end.month.ordinal]}"

    else ->
        "${start.day} ${monthNamesGenitive[start.month.ordinal]} - " +
            "${end.day} ${monthNamesGenitive[end.month.ordinal]}"
}

private fun LocalDate.asNumericDate(): String =
    "${day.toString().padStart(2, '0')}." +
        "${month.ordinal.plus(1).toString().padStart(2, '0')}.$year"

private fun LocalDate.asChartDate(): String =
    "${day.toString().padStart(2, '0')}.${month.ordinal.plus(1).toString().padStart(2, '0')}"

private fun Double.asDisplayValue(): String {
    val tenths = (this * 10).roundToInt()
    return if (tenths % 10 == 0) {
        (tenths / 10).toString()
    } else {
        "${tenths / 10}.${tenths % 10}"
    }
}

private enum class DateTarget {
    START,
    END
}

private val monthNamesNominative = listOf(
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
