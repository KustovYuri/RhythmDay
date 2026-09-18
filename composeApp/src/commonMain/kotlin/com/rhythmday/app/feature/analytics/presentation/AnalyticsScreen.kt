package com.rhythmday.app.feature.analytics.presentation

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rhythmday.app.core.designsystem.RhythmDayPalette
import com.rhythmday.app.core.designsystem.scoreAccentColor
import com.rhythmday.app.core.domain.model.DayEntry
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.pow
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
private val ChartAxisWidth = 28.dp
private val ChartTopPadding = 16.dp
private val ChartBottomPadding = 32.dp
private val ChartHorizontalPadding = 16.dp
private val ChartLineWidth = 3.dp
private val ChartPointOuterRadius = 5.dp
private val ChartPointInnerRadius = 3.5.dp
private val MinimumChartTickSpacing = 22.dp
private const val ChartPointSpacing = 52f
private const val MinimumChartZoom = 1f
private const val MaximumChartZoom = 4f

private data class ChartVerticalScale(
    val visibleRange: Double
)

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

    AnalyticsCard(
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 14.dp)
    ) {
        Text(
            text = if (selectedMetrics.size == 1) primaryMetric.label else "Выбранные показатели",
            fontSize = 18.sp,
            lineHeight = 22.sp,
            fontWeight = FontWeight.Bold,
            color = RhythmDayPalette.Ink,
            modifier = Modifier.padding(horizontal = 6.dp)
        )
        Text(
            text = state.rangeSubtitle(),
            fontSize = 12.sp,
            lineHeight = 16.sp,
            fontWeight = FontWeight.Bold,
            color = RhythmDayPalette.Muted,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
        )

        LineChart(
            entries = state.entries,
            rangeStart = state.rangeStart,
            rangeEnd = state.rangeEnd,
            selectedMetrics = selectedMetrics,
            onOpenEntry = onOpenEntry,
            modifier = Modifier.padding(top = 10.dp)
        )
        MetricLegend(
            metrics = selectedMetrics,
            modifier = Modifier.padding(start = 6.dp, top = 8.dp, end = 6.dp)
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
    rangeStart: LocalDate,
    rangeEnd: LocalDate,
    selectedMetrics: List<AnalyticsMetric>,
    onOpenEntry: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val chartStateKey = buildString {
        append(rangeStart)
        append(':')
        append(rangeEnd)
        append(':')
        append(entries.firstOrNull()?.id)
        append(':')
        append(entries.lastOrNull()?.id)
        append(':')
        append(entries.size)
        append(':')
        selectedMetrics.forEach { metric -> append(metric.name) }
    }
    val initialVerticalCenter = entries
        .flatMap { entry -> selectedMetrics.map { metric -> metric.value(entry) } }
        .average()
        .coerceIn(0.0, 10.0)
    BoxWithConstraints(modifier = modifier.fillMaxWidth().height(ChartHeight)) {
        val viewportWidth = (maxWidth - ChartAxisWidth).coerceAtLeast(1.dp)
        val baseChartWidth = max(viewportWidth.value, entries.size * ChartPointSpacing)
        val minimumChartZoom = viewportWidth.value / baseChartWidth
        var zoom by remember(chartStateKey, viewportWidth.value) {
            mutableFloatStateOf(minimumChartZoom)
        }
        var verticalCenter by remember(chartStateKey) {
            mutableDoubleStateOf(initialVerticalCenter)
        }
        val verticalScale = chartVerticalScale(zoom)
        val displayedCenter = constrainedVerticalCenter(verticalCenter, verticalScale.visibleRange)
        val minimumVisibleValue = displayedCenter - verticalScale.visibleRange / 2.0
        val maximumVisibleValue = displayedCenter + verticalScale.visibleRange / 2.0

        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.Top
        ) {
            ChartYAxis(
                minimumValue = minimumVisibleValue,
                maximumValue = maximumVisibleValue,
                modifier = Modifier.width(ChartAxisWidth).height(ChartHeight)
            )
            Box(modifier = Modifier.weight(1f).height(ChartHeight)) {
                val scrollState = remember(chartStateKey, viewportWidth.value) {
                    ScrollState(initial = 0)
                }
                val chartPlotHeightPx = with(density) {
                    (ChartHeight - ChartTopPadding - ChartBottomPadding).toPx()
                }
                val chartTopPaddingPx = with(density) { ChartTopPadding.toPx() }
                val chartHorizontalPaddingPx = with(density) { ChartHorizontalPadding.toPx() }
                val viewportWidthPx = with(density) { viewportWidth.toPx() }
                val baseChartWidthPx = with(density) { baseChartWidth.dp.toPx() }
                val chartWidth = (baseChartWidth * zoom).dp
                var pendingHorizontalOffset by remember(chartStateKey, viewportWidth.value) {
                    mutableFloatStateOf(Float.NaN)
                }
                val transformableState = rememberTransformableState { centroid, zoomChange, panChange, _ ->
                    val previousZoom = zoom
                    val previousScale = chartVerticalScale(previousZoom)
                    val nextZoom = (previousZoom * zoomChange)
                        .coerceIn(minimumChartZoom, MaximumChartZoom)
                    val nextScale = chartVerticalScale(nextZoom)
                    val previousChartWidthPx = baseChartWidthPx * previousZoom
                    val nextChartWidthPx = baseChartWidthPx * nextZoom
                    val currentHorizontalOffset = pendingHorizontalOffset
                        .takeUnless(Float::isNaN)
                        ?: scrollState.value.toFloat()
                    var nextHorizontalOffset = currentHorizontalOffset
                    if (abs(nextZoom - previousZoom) > 0.0001f) {
                        nextHorizontalOffset = anchoredHorizontalOffset(
                            currentOffset = currentHorizontalOffset,
                            centroidX = centroid.x,
                            previousContentWidth = previousChartWidthPx,
                            nextContentWidth = nextChartWidthPx,
                            horizontalPadding = chartHorizontalPaddingPx
                        )
                    }
                    nextHorizontalOffset = (nextHorizontalOffset - panChange.x).coerceIn(
                        minimumValue = 0f,
                        maximumValue = (nextChartWidthPx - viewportWidthPx).coerceAtLeast(0f)
                    )
                    pendingHorizontalOffset = nextHorizontalOffset
                    scrollState.dispatchRawDelta(nextHorizontalOffset - scrollState.value)

                    val previousCenter = constrainedVerticalCenter(
                        center = verticalCenter,
                        visibleRange = previousScale.visibleRange
                    )
                    val verticalAnchorFraction = (
                        (centroid.y - chartTopPaddingPx) / chartPlotHeightPx
                        ).coerceIn(0f, 1f)
                    val anchorValue = previousCenter + previousScale.visibleRange / 2.0 -
                        verticalAnchorFraction * previousScale.visibleRange
                    val anchoredCenter = anchorValue +
                        (verticalAnchorFraction - 0.5f) * nextScale.visibleRange
                    verticalCenter = constrainedVerticalCenter(
                        center = anchoredCenter +
                            panChange.y / chartPlotHeightPx * nextScale.visibleRange,
                        visibleRange = nextScale.visibleRange
                    )
                    zoom = nextZoom
                }

                LaunchedEffect(chartWidth, pendingHorizontalOffset) {
                    val targetOffset = pendingHorizontalOffset
                    if (!targetOffset.isNaN()) {
                        withFrameNanos { }
                        scrollState.dispatchRawDelta(
                            targetOffset.coerceIn(0f, scrollState.maxValue.toFloat()) -
                                scrollState.value
                        )
                        if (pendingHorizontalOffset == targetOffset) {
                            pendingHorizontalOffset = Float.NaN
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .transformable(
                            state = transformableState,
                            canPan = { pan ->
                                zoom > minimumChartZoom ||
                                    scrollState.maxValue > 0 && abs(pan.x) > abs(pan.y)
                            }
                        )
                        .horizontalScroll(scrollState, enabled = false)
                ) {
                    MetricChartCanvas(
                        entries = entries,
                        selectedMetrics = selectedMetrics,
                        minimumValue = minimumVisibleValue,
                        maximumValue = maximumVisibleValue,
                        onOpenEntry = onOpenEntry,
                        modifier = Modifier.width(chartWidth).height(ChartHeight)
                    )
                }
            }
        }
    }
}

@Composable
private fun ChartYAxis(
    minimumValue: Double,
    maximumValue: Double,
    modifier: Modifier = Modifier
) {
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
        val pixelsPerValue = plotHeight / (maximumValue - minimumValue).toFloat()

        chartTicks(
            minimumValue = minimumValue,
            maximumValue = maximumValue,
            pixelsPerValue = pixelsPerValue,
            minimumSpacing = MinimumChartTickSpacing.toPx()
        ).forEach { value ->
            val fraction = (
                (maximumValue - value) / (maximumValue - minimumValue)
                ).toFloat()
            val y = top + fraction * plotHeight
            val label = value.asAxisLabel()
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
    minimumValue: Double,
    maximumValue: Double,
    onOpenEntry: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()
    val axisStyle = TextStyle(
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        color = RhythmDayPalette.Muted
    )
    Canvas(
        modifier = modifier.pointerInput(
            entries,
            selectedMetrics,
            minimumValue,
            maximumValue
        ) {
            detectChartTaps { tap ->
                val left = ChartHorizontalPadding.toPx()
                val right = ChartHorizontalPadding.toPx()
                val top = ChartTopPadding.toPx()
                val bottom = ChartBottomPadding.toPx()
                val plotWidth = (size.width - left - right).coerceAtLeast(0f)
                val plotHeight = (size.height - top - bottom).coerceAtLeast(0f)
                val nearestPoint = entries.indices.flatMap { index ->
                    selectedMetrics.mapNotNull { metric ->
                        val point = chartPoint(
                            index = index,
                            count = entries.size,
                            value = metric.value(entries[index]),
                            left = left,
                            top = top,
                            plotWidth = plotWidth,
                            plotHeight = plotHeight,
                            minimumValue = minimumValue,
                            maximumValue = maximumValue
                        )
                        if (point.y in top..(top + plotHeight)) index to point else null
                    }
                }.minByOrNull { (_, point) ->
                    (point.x - tap.x) * (point.x - tap.x) +
                        (point.y - tap.y) * (point.y - tap.y)
                }
                if (nearestPoint != null) {
                    val (nearestIndex, point) = nearestPoint
                    val radius = 20.dp.toPx()
                    val distanceSquared = (point.x - tap.x) * (point.x - tap.x) +
                        (point.y - tap.y) * (point.y - tap.y)
                    if (distanceSquared <= radius * radius) {
                        onOpenEntry(entries[nearestIndex].id)
                    }
                }
            }
        }
    ) {
        val left = ChartHorizontalPadding.toPx()
        val right = ChartHorizontalPadding.toPx()
        val top = ChartTopPadding.toPx()
        val bottom = ChartBottomPadding.toPx()
        val plotWidth = size.width - left - right
        val plotHeight = size.height - top - bottom
        val pointSpacing = plotWidth / (entries.size - 1).coerceAtLeast(1)
        val pixelsPerValue = plotHeight / (maximumValue - minimumValue).toFloat()

        chartTicks(
            minimumValue = minimumValue,
            maximumValue = maximumValue,
            pixelsPerValue = pixelsPerValue,
            minimumSpacing = MinimumChartTickSpacing.toPx()
        ).forEach { value ->
            val y = top + (
                (maximumValue - value) / (maximumValue - minimumValue)
                ).toFloat() * plotHeight
            drawLine(
                color = GridLine,
                start = Offset(left, y),
                end = Offset(size.width - right, y),
                strokeWidth = 1.dp.toPx()
            )
        }

        val metricPoints = selectedMetrics.associateWith { metric ->
            entries.mapIndexed { index, entry ->
                chartPoint(
                    index = index,
                    count = entries.size,
                    value = metric.value(entry),
                    left = left,
                    top = top,
                    plotWidth = plotWidth,
                    plotHeight = plotHeight,
                    minimumValue = minimumValue,
                    maximumValue = maximumValue
                )
            }
        }
        val halfLineWidth = ChartLineWidth.toPx() / 2f
        clipRect(
            left = 0f,
            top = top - halfLineWidth,
            right = size.width,
            bottom = top + plotHeight + halfLineWidth
        ) {
            selectedMetrics.forEach { metric ->
                val points = metricPoints.getValue(metric)
                drawPath(
                    path = smoothPath(points),
                    color = metric.color,
                    style = Stroke(
                        width = ChartLineWidth.toPx(),
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )
            }
        }

        selectedMetrics.forEach { metric ->
            val points = metricPoints.getValue(metric)
            entries.forEachIndexed { index, entry ->
                if (pointSpacing < 12.dp.toPx()) return@forEachIndexed
                val point = points[index]
                if (point.y in top..(top + plotHeight)) {
                    drawCircle(
                        color = Color.White,
                        radius = ChartPointOuterRadius.toPx(),
                        center = point
                    )
                    drawCircle(
                        color = metric.pointColor(metric.value(entry)),
                        radius = ChartPointInnerRadius.toPx(),
                        center = point
                    )
                }
            }
        }

        val labelStep = ceil(48.dp.toPx() / pointSpacing.coerceAtLeast(1f))
            .toInt()
            .coerceAtLeast(1)
        entries.forEachIndexed { index, entry ->
            if (index != 0 && index != entries.lastIndex &&
                (index % labelStep != 0 || entries.lastIndex - index < labelStep)
            ) return@forEachIndexed
            val point = chartPoint(
                index = index,
                count = entries.size,
                value = minimumValue,
                left = left,
                top = top,
                plotWidth = plotWidth,
                plotHeight = plotHeight,
                minimumValue = minimumValue,
                maximumValue = maximumValue
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

private suspend fun PointerInputScope.detectChartTaps(onTap: (Offset) -> Unit) {
    awaitEachGesture {
        val down = awaitFirstDown(
            requireUnconsumed = false,
            pass = PointerEventPass.Final
        )
        val pointerId = down.id
        val startPosition = down.position
        var isTap = true

        while (true) {
            val event = awaitPointerEvent(PointerEventPass.Final)
            if (event.changes.count { change -> change.pressed } > 1) {
                isTap = false
            }
            val change = event.changes.firstOrNull { candidate -> candidate.id == pointerId }
                ?: break
            if (change.isConsumed ||
                (change.position - startPosition).getDistance() > viewConfiguration.touchSlop
            ) {
                isTap = false
            }
            if (!change.pressed) {
                if (isTap) onTap(change.position)
                break
            }
        }
    }
}

private fun chartVerticalScale(zoom: Float): ChartVerticalScale {
    val progress = (
        (zoom - MinimumChartZoom) / (MaximumChartZoom - MinimumChartZoom)
        ).coerceIn(0f, 1f)
    return ChartVerticalScale(
        visibleRange = 10.0.pow(1.0 - progress)
    )
}

private fun constrainedVerticalCenter(center: Double, visibleRange: Double): Double =
    center.coerceIn(visibleRange / 2.0, 10.0 - visibleRange / 2.0)

private fun anchoredHorizontalOffset(
    currentOffset: Float,
    centroidX: Float,
    previousContentWidth: Float,
    nextContentWidth: Float,
    horizontalPadding: Float
): Float {
    val previousPlotWidth = (previousContentWidth - 2f * horizontalPadding).coerceAtLeast(1f)
    val nextPlotWidth = (nextContentWidth - 2f * horizontalPadding).coerceAtLeast(1f)
    val positionInPlot = (
        currentOffset + centroidX - horizontalPadding
        ) / previousPlotWidth
    val nextContentX = horizontalPadding + positionInPlot * nextPlotWidth
    return nextContentX - centroidX
}

private fun chartTicks(
    minimumValue: Double,
    maximumValue: Double,
    pixelsPerValue: Float,
    minimumSpacing: Float
): List<Double> {
    val step = listOf(0.1, 0.2, 0.5, 1.0).firstOrNull { candidate ->
        pixelsPerValue * candidate >= minimumSpacing
    } ?: 1.0
    val firstIndex = ceil((minimumValue - 0.0001) / step).toInt()
    val lastIndex = floor((maximumValue + 0.0001) / step).toInt()
    return (firstIndex..lastIndex).map { index -> index * step }
}

private fun smoothPath(points: List<Offset>): Path = Path().apply {
    if (points.isEmpty()) return@apply
    moveTo(points.first().x, points.first().y)
    if (points.size == 1) return@apply

    for (index in 0 until points.lastIndex) {
        val previous = points.getOrElse(index - 1) { points[index] }
        val current = points[index]
        val next = points[index + 1]
        val following = points.getOrElse(index + 2) { next }
        cubicTo(
            x1 = current.x + (next.x - previous.x) / 6f,
            y1 = current.y + (next.y - previous.y) / 6f,
            x2 = next.x - (following.x - current.x) / 6f,
            y2 = next.y - (following.y - current.y) / 6f,
            x3 = next.x,
            y3 = next.y
        )
    }
}

private fun chartPoint(
    index: Int,
    count: Int,
    value: Double,
    left: Float,
    top: Float,
    plotWidth: Float,
    plotHeight: Float,
    minimumValue: Double,
    maximumValue: Double
): Offset {
    val x = if (count <= 1) {
        left + plotWidth / 2f
    } else {
        left + plotWidth * index / (count - 1)
    }
    val cleanValue = value.coerceIn(0.0, 10.0)
    val valueRange = (maximumValue - minimumValue).coerceAtLeast(0.01)
    val y = top + ((maximumValue - cleanValue) / valueRange).toFloat() * plotHeight
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
        val points = values.mapIndexed { index, value ->
            val x = if (values.size <= 1) size.width / 2f else size.width * index / (values.size - 1)
            val y = ((10.0 - value.coerceIn(0.0, 10.0)) / 10.0).toFloat() * size.height
            Offset(x, y)
        }
        drawPath(
            path = smoothPath(points),
            color = color,
            style = Stroke(
                width = 2.5.dp.toPx(),
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )
    }
}

@Composable
private fun AnalyticsCard(
    contentPadding: PaddingValues = PaddingValues(14.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = CardShape,
        color = RhythmDayPalette.Panel
    ) {
        Column(modifier = Modifier.padding(contentPadding), content = content)
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
        AnalyticsRange.WEEK -> "7 дней"
        AnalyticsRange.MONTH -> "30 дней"
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

private fun AnalyticsMetric.pointColor(value: Double): Color {
    return scoreAccentColor(value, inverted = this == AnalyticsMetric.STRESS)
}

private fun AnalyticsUiState.rangeSubtitle(): String = formatDateRange(rangeStart, rangeEnd)

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

private fun Double.asAxisLabel(): String {
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
