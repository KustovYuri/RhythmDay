package com.rhythmday.app.feature.createentry.presentation

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rhythmday.app.core.designsystem.RhythmDayPalette
import com.rhythmday.app.core.domain.model.DAY_METRIC_MAX_VALUE
import com.rhythmday.app.core.domain.model.DAY_METRIC_MIN_VALUE
import com.rhythmday.app.core.domain.model.DAY_TITLE_MAX_LENGTH
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.todayIn
import kotlin.math.roundToInt
import kotlin.time.Clock

private val CardShape = RoundedCornerShape(8.dp)
private val ControlBackground = Color(0xFFEDF3F5)
private val FieldBackground = Color(0xFFF0F4F7)
private val SelectedTagBackground = Color(0xFFCBE8E3)
private val InactiveSliderThumb = Color(0xFF5FAFA6)
private val LowPillBackground = Color(0xFFF7EBE8)
private val LowPillText = Color(0xFF8C4D45)
private val MediumPillBackground = Color(0xFFFFF4D8)
private val MediumPillText = Color(0xFF896617)
private val HighPillBackground = Color(0xFFE4F3EA)
private val HighPillText = Color(0xFF2D7841)
private const val VoiceWaveformBarCount = 28
private const val VoiceWaveformSampleIntervalMillis = 160L

@Composable
fun CreateEntryRoute(
    viewModel: CreateEntryViewModel,
    entryId: Long?,
    showBackButton: Boolean,
    contentPadding: PaddingValues,
    onBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val speechTextRequester = rememberSpeechTextRequester(
        onTextRecognized = viewModel::onSpeechTextRecognized
    )

    LaunchedEffect(viewModel, entryId) {
        viewModel.prepareEntry(entryId)
    }

    LaunchedEffect(state.isSaved) {
        if (state.isSaved) {
            viewModel.consumeSavedEvent()
            onBack()
        }
    }

    CreateEntryScreen(
        state = state,
        today = Clock.System.todayIn(TimeZone.currentSystemDefault()),
        showBackButton = showBackButton,
        contentPadding = contentPadding,
        onBack = onBack,
        onMetricChanged = viewModel::onMetricChanged,
        onTagClick = viewModel::onTagClick,
        onTagsExpandedChanged = viewModel::onTagsExpandedChanged,
        onCustomTagAdded = viewModel::onCustomTagAdded,
        onTitleChanged = viewModel::onTitleChanged,
        onNoteChanged = viewModel::onNoteChanged,
        speechInputActive = speechTextRequester.isListening,
        speechAudioLevel = speechTextRequester.audioLevel,
        onSpeechInputToggle = speechTextRequester.toggle,
        onSave = viewModel::save
    )
}

@Composable
private fun CreateEntryScreen(
    state: CreateEntryUiState,
    today: LocalDate,
    showBackButton: Boolean,
    contentPadding: PaddingValues,
    onBack: () -> Unit,
    onMetricChanged: (CreateEntryMetric, Double) -> Unit,
    onTagClick: (String) -> Unit,
    onTagsExpandedChanged: (Boolean) -> Unit,
    onCustomTagAdded: (String) -> Unit,
    onTitleChanged: (String) -> Unit,
    onNoteChanged: (String) -> Unit,
    speechInputActive: Boolean,
    speechAudioLevel: Float,
    onSpeechInputToggle: () -> Unit,
    onSave: () -> Unit
) {
    var showCustomTagDialog by remember { mutableStateOf(false) }
    var showSpeechInputDialog by remember { mutableStateOf(false) }

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
            CreateEntryHeader(
                date = state.entryDate ?: today,
                today = today,
                showBackButton = showBackButton,
                onBack = onBack
            )
        }
        if (state.isLoading) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().heightIn(min = 220.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = RhythmDayPalette.Teal)
                }
            }
        } else {
            item {
                MetricsCard(
                    state = state,
                    onMetricChanged = onMetricChanged
                )
            }
            item {
                TagsCard(
                    state = state,
                    onTagClick = onTagClick,
                    onTagsExpandedChanged = onTagsExpandedChanged,
                    onAddTagClick = { showCustomTagDialog = true }
                )
            }
            item {
                DayTitleCard(
                    title = state.title,
                    onTitleChanged = onTitleChanged
                )
            }
            item {
                DescriptionCard(
                    note = state.note,
                    onNoteChanged = onNoteChanged,
                    onSpeechInputClick = { showSpeechInputDialog = true }
                )
            }
            item {
                SaveSection(
                    enabled = state.canSave,
                    isSaving = state.isSaving,
                    errorMessage = state.errorMessage,
                    onSave = onSave,
                    modifier = Modifier.padding(top = 12.dp)
                )
            }
        }
    }

    if (showCustomTagDialog) {
        CustomTagDialog(
            onDismiss = { showCustomTagDialog = false },
            onConfirm = { tag ->
                onCustomTagAdded(tag)
                showCustomTagDialog = false
            }
        )
    }

    if (showSpeechInputDialog) {
        val dismissSpeechInput = {
            if (speechInputActive) onSpeechInputToggle()
            showSpeechInputDialog = false
        }

        VoiceInputDialog(
            isListening = speechInputActive,
            audioLevel = speechAudioLevel,
            onStart = onSpeechInputToggle,
            onStop = dismissSpeechInput,
            onDismiss = dismissSpeechInput
        )
    }
}

@Composable
private fun CreateEntryHeader(
    date: LocalDate,
    today: LocalDate,
    showBackButton: Boolean,
    onBack: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = 42.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (showBackButton) {
            CompactIconButton(
                icon = Icons.AutoMirrored.Outlined.ArrowBack,
                contentDescription = "Назад",
                onClick = onBack,
                size = 42.dp,
                iconSize = 20.dp,
                tint = RhythmDayPalette.Ink,
                background = RhythmDayPalette.Panel
            )
        } else {
            Spacer(modifier = Modifier.size(42.dp))
        }
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Запись дня",
                fontSize = 20.sp,
                lineHeight = 21.sp,
                fontWeight = FontWeight.Bold,
                color = RhythmDayPalette.Ink,
                textAlign = TextAlign.Center
            )
            Text(
                text = date.asRelativeDate(today),
                fontSize = 11.sp,
                lineHeight = 14.sp,
                fontWeight = FontWeight.Bold,
                color = RhythmDayPalette.Muted,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
        Spacer(modifier = Modifier.size(42.dp))
    }
}

@Composable
private fun MetricsCard(
    state: CreateEntryUiState,
    onMetricChanged: (CreateEntryMetric, Double) -> Unit
) {
    PrototypeCard {
        Column(
            modifier = Modifier.padding(horizontal = 15.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            metricItems(state).forEach { metric ->
                MetricSliderBlock(
                    item = metric,
                    onValueChange = { value -> onMetricChanged(metric.metric, value) }
                )
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun MetricSliderBlock(
    item: MetricItem,
    onValueChange: (Double) -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }

    Column {
        Text(
            text = item.label,
            fontSize = 13.sp,
            lineHeight = 16.sp,
            fontWeight = FontWeight.Bold,
            color = RhythmDayPalette.Muted
        )
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Slider(
                value = item.value?.toFloat() ?: DAY_METRIC_MIN_VALUE.toFloat(),
                onValueChange = { value -> onValueChange(value.asMetricValue()) },
                valueRange = DAY_METRIC_MIN_VALUE.toFloat()..DAY_METRIC_MAX_VALUE.toFloat(),
                steps = 98,
                interactionSource = interactionSource,
                thumb = { CompactSliderThumb(visible = item.value != null) },
                track = { sliderState ->
                    CompactSliderTrack(
                        progress = if (item.value == null) {
                            0f
                        } else {
                            sliderState.coercedValueAsFraction
                        }
                    )
                },
                modifier = Modifier.weight(1f).height(30.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            ScorePill(
                value = item.value,
                inverted = item.inverted
            )
        }
    }
}

@Composable
private fun CompactSliderThumb(visible: Boolean) {
    Box(
        modifier = Modifier
            .size(18.dp)
            .clip(CircleShape)
            .background(
                if (visible) RhythmDayPalette.Teal else InactiveSliderThumb
            )
    )
}

@Composable
private fun CompactSliderTrack(progress: Float) {
    val shape = RoundedCornerShape(percent = 50)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(shape)
            .background(RhythmDayPalette.Line)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .fillMaxHeight()
                .background(RhythmDayPalette.Teal)
        )
    }
}

@Composable
private fun ScorePill(
    value: Double?,
    inverted: Boolean
) {
    val colors = if (value == null) {
        PillColors(ControlBackground, RhythmDayPalette.TealDark)
    } else {
        pillColors(value = value, inverted = inverted)
    }

    Box(
        modifier = Modifier
            .width(38.dp)
            .height(30.dp)
            .clip(CardShape)
            .background(colors.background),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = value?.asMetricLabel() ?: "—",
            fontSize = 16.sp,
            lineHeight = 18.sp,
            fontWeight = FontWeight.Bold,
            color = colors.text
        )
    }
}

@Composable
private fun TagsCard(
    state: CreateEntryUiState,
    onTagClick: (String) -> Unit,
    onTagsExpandedChanged: (Boolean) -> Unit,
    onAddTagClick: () -> Unit
) {
    PrototypeCard {
        Column(modifier = Modifier.padding(14.dp)) {
            CardHeader(
                title = "Теги дня",
                action = {
                    CompactIconButton(
                        icon = Icons.Filled.Add,
                        contentDescription = "Добавить свой тег",
                        onClick = onAddTagClick
                    )
                }
            )
            FlowRow(
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                state.visibleTags().forEach { tag ->
                    TagChip(
                        text = tag,
                        selected = tag in state.selectedTags,
                        onClick = { onTagClick(tag) }
                    )
                }
                ExpandTagsChip(
                    expanded = state.tagsExpanded,
                    onClick = { onTagsExpandedChanged(!state.tagsExpanded) }
                )
            }
        }
    }
}

@Composable
private fun TagChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .height(34.dp)
            .clip(CardShape)
            .background(if (selected) SelectedTagBackground else FieldBackground)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 13.sp,
            lineHeight = 16.sp,
            fontWeight = FontWeight.Bold,
            color = if (selected) RhythmDayPalette.TealDark else RhythmDayPalette.Ink,
            maxLines = 1
        )
    }
}

@Composable
private fun ExpandTagsChip(
    expanded: Boolean,
    onClick: () -> Unit
) {
    CompactIconButton(
        icon = if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
        contentDescription = if (expanded) "Свернуть теги" else "Показать больше тегов",
        onClick = onClick,
        size = 34.dp,
        iconSize = 20.dp
    )
}

@Composable
private fun DayTitleCard(
    title: String,
    onTitleChanged: (String) -> Unit
) {
    PrototypeCard {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Название дня",
                    style = MaterialTheme.typography.titleLarge,
                    color = RhythmDayPalette.Ink
                )
                Text(
                    text = "${title.length}/$DAY_TITLE_MAX_LENGTH",
                    fontSize = 11.sp,
                    lineHeight = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = RhythmDayPalette.Muted
                )
            }
            SingleLineInput(
                value = title,
                onValueChange = onTitleChanged,
                placeholder = "Например, день новых идей",
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
            )
        }
    }
}

@Composable
private fun DescriptionCard(
    note: String,
    onNoteChanged: (String) -> Unit,
    onSpeechInputClick: () -> Unit
) {
    PrototypeCard {
        Column(modifier = Modifier.padding(14.dp)) {
            CardHeader(
                title = "Описание дня",
                action = {
                    CompactIconButton(
                        icon = Icons.Outlined.Mic,
                        contentDescription = "Открыть голосовой ввод",
                        onClick = onSpeechInputClick
                    )
                }
            )
            MultilineInput(
                value = note,
                onValueChange = onNoteChanged,
                placeholder = "Что запомнилось сегодня?",
                modifier = Modifier.padding(top = 12.dp)
            )
        }
    }
}

@Composable
private fun VoiceInputDialog(
    isListening: Boolean,
    audioLevel: Float,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.9f).widthIn(max = 318.dp),
            shape = CardShape,
            color = RhythmDayPalette.Panel
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Голосовой ввод",
                    style = MaterialTheme.typography.titleLarge,
                    color = RhythmDayPalette.Ink,
                    textAlign = TextAlign.Center
                )
                Row(
                    modifier = Modifier.padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(
                                if (isListening) RhythmDayPalette.HardDay else RhythmDayPalette.Line
                            )
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isListening) "Идёт запись" else "Готово к записи",
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isListening) {
                            RhythmDayPalette.HardDay
                        } else {
                            RhythmDayPalette.Muted
                        }
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 18.dp)
                        .height(78.dp)
                        .clip(CardShape)
                        .background(FieldBackground)
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    VoiceWaveform(
                        audioLevel = audioLevel,
                        isListening = isListening,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Spacer(modifier = Modifier.height(18.dp))
                CompactIconButton(
                    icon = if (isListening) Icons.Filled.Stop else Icons.Filled.PlayArrow,
                    contentDescription = if (isListening) {
                        "Завершить запись"
                    } else {
                        "Начать запись"
                    },
                    onClick = if (isListening) onStop else onStart,
                    size = 54.dp,
                    iconSize = 26.dp,
                    tint = RhythmDayPalette.TealDark,
                    background = ControlBackground
                )
            }
        }
    }
}

@Composable
private fun VoiceWaveform(
    audioLevel: Float,
    isListening: Boolean,
    modifier: Modifier = Modifier
) {
    val samples = remember { mutableStateListOf<Float>() }
    val latestAudioLevel by rememberUpdatedState(audioLevel)
    val scrollProgress = remember { Animatable(0f) }
    var incomingLevel by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(isListening) {
        if (isListening) {
            while (true) {
                val sampledLevel = latestAudioLevel.coerceIn(0f, 1f)
                incomingLevel = sampledLevel
                scrollProgress.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(
                        durationMillis = VoiceWaveformSampleIntervalMillis.toInt(),
                        easing = LinearEasing
                    )
                )
                samples += sampledLevel
                if (samples.size > VoiceWaveformBarCount) samples.removeAt(0)
                scrollProgress.snapTo(0f)
            }
        } else {
            samples.clear()
            incomingLevel = 0f
            scrollProgress.snapTo(0f)
        }
    }

    Canvas(modifier = modifier) {
        val step = size.width / VoiceWaveformBarCount
        val strokeWidth = 3.dp.toPx()
        val minimumHeight = 5.dp.toPx()
        val availableHeight = (size.height - minimumHeight).coerceAtLeast(0f)
        val emptyBars = VoiceWaveformBarCount - samples.size
        val color = if (isListening) RhythmDayPalette.Teal else RhythmDayPalette.Line

        repeat(VoiceWaveformBarCount) { index ->
            val sampleIndex = index - emptyBars
            val sample = samples.getOrNull(sampleIndex) ?: 0f
            val barHeight = minimumHeight + availableHeight * sample
            val x = step * (index + 0.5f - scrollProgress.value)

            drawLine(
                color = color,
                start = Offset(x, (size.height - barHeight) / 2f),
                end = Offset(x, (size.height + barHeight) / 2f),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )
        }

        if (isListening) {
            val incomingHeight = minimumHeight +
                availableHeight * incomingLevel
            val incomingX = step * (VoiceWaveformBarCount + 0.5f - scrollProgress.value)

            drawLine(
                color = color,
                start = Offset(incomingX, (size.height - incomingHeight) / 2f),
                end = Offset(incomingX, (size.height + incomingHeight) / 2f),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )
        }
    }
}

@Composable
private fun MultilineInput(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 122.dp)
            .clip(CardShape)
            .background(FieldBackground)
            .padding(12.dp)
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            textStyle = TextStyle(
                fontSize = 14.sp,
                lineHeight = 20.sp,
                color = RhythmDayPalette.Ink
            ),
            modifier = Modifier.fillMaxWidth().heightIn(min = 98.dp),
            decorationBox = { innerTextField ->
                Box(modifier = Modifier.fillMaxWidth()) {
                    if (value.isEmpty()) {
                        Text(
                            text = placeholder,
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                            color = RhythmDayPalette.Muted
                        )
                    }
                    innerTextField()
                }
            }
        )
    }
}

@Composable
private fun SaveSection(
    enabled: Boolean,
    isSaving: Boolean,
    errorMessage: String?,
    onSave: () -> Unit,
    modifier: Modifier = Modifier
) {
    val background = if (enabled) RhythmDayPalette.Teal else ControlBackground
    val contentColor = if (enabled) Color.White else RhythmDayPalette.Muted

    Column(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp)
                .clip(CardShape)
                .background(background)
                .clickable(
                    enabled = enabled,
                    role = Role.Button,
                    onClick = onSave
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (isSaving) "Сохраняем" else "Сохранить",
                fontSize = 14.sp,
                lineHeight = 18.sp,
                fontWeight = FontWeight.Bold,
                color = contentColor
            )
        }
        if (errorMessage != null) {
            Text(
                text = errorMessage,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@Composable
private fun CustomTagDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var tag by remember { mutableStateOf("") }
    val confirm = {
        val cleanTag = tag.trim()
        if (cleanTag.isNotEmpty()) {
            onConfirm(cleanTag)
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.9f).widthIn(max = 318.dp),
            shape = CardShape,
            color = RhythmDayPalette.Panel
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Свой тег",
                    style = MaterialTheme.typography.titleLarge,
                    color = RhythmDayPalette.Ink
                )
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SingleLineInput(
                        value = tag,
                        onValueChange = { value -> tag = value },
                        placeholder = "Например, медитация",
                        modifier = Modifier.weight(1f)
                    )
                    ConfirmTagButton(
                        enabled = tag.isNotBlank(),
                        onClick = confirm
                    )
                }
            }
        }
    }
}

@Composable
private fun SingleLineInput(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(38.dp)
            .clip(CardShape)
            .background(FieldBackground)
            .padding(horizontal = 10.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            textStyle = TextStyle(
                fontSize = 14.sp,
                lineHeight = 18.sp,
                color = RhythmDayPalette.Ink
            ),
            modifier = Modifier.fillMaxWidth(),
            decorationBox = { innerTextField ->
                Box(modifier = Modifier.fillMaxWidth()) {
                    if (value.isEmpty()) {
                        Text(
                            text = placeholder,
                            fontSize = 14.sp,
                            lineHeight = 18.sp,
                            color = RhythmDayPalette.Muted,
                            maxLines = 1
                        )
                    }
                    innerTextField()
                }
            }
        )
    }
}

@Composable
private fun ConfirmTagButton(
    enabled: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .width(42.dp)
            .height(38.dp)
            .clip(CardShape)
            .background(ControlBackground)
            .clickable(
                enabled = enabled,
                role = Role.Button,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Filled.Check,
            contentDescription = "Добавить тег",
            tint = if (enabled) RhythmDayPalette.TealDark else RhythmDayPalette.Muted,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun CardHeader(
    title: String,
    action: @Composable () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = RhythmDayPalette.Ink
        )
        action()
    }
}

@Composable
private fun PrototypeCard(content: @Composable () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = CardShape,
        color = RhythmDayPalette.Panel,
        content = content
    )
}

@Composable
private fun CompactIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    size: Dp = 34.dp,
    iconSize: Dp = 18.dp,
    tint: Color = RhythmDayPalette.TealDark,
    background: Color = ControlBackground
) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CardShape)
            .background(background)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(iconSize)
        )
    }
}

private data class MetricItem(
    val metric: CreateEntryMetric,
    val label: String,
    val value: Double?,
    val inverted: Boolean = false
)

private data class PillColors(
    val background: Color,
    val text: Color
)

private fun metricItems(state: CreateEntryUiState): List<MetricItem> = listOf(
    MetricItem(CreateEntryMetric.ENERGY, "Энергия", state.energy),
    MetricItem(CreateEntryMetric.MOOD, "Настроение", state.mood),
    MetricItem(CreateEntryMetric.WELLBEING, "Самочувствие", state.wellbeing),
    MetricItem(CreateEntryMetric.SLEEP_QUALITY, "Качество сна", state.sleepQuality),
    MetricItem(CreateEntryMetric.MENTAL_CLARITY, "Ясность ума", state.mentalClarity),
    MetricItem(CreateEntryMetric.SOCIALITY, "Социальность", state.sociality),
    MetricItem(CreateEntryMetric.STRESS, "Стресс", state.stress, inverted = true)
)

private fun CreateEntryUiState.visibleTags(): List<String> {
    val allTags = (DefaultDayTags + customTags).distinct()
    return if (tagsExpanded) {
        allTags
    } else {
        (PrimaryDayTags + allTags.filter { tag -> tag in selectedTags && tag !in PrimaryDayTags })
            .distinct()
    }
}

private fun pillColors(
    value: Double,
    inverted: Boolean
): PillColors {
    val levelValue = if (inverted) {
        DAY_METRIC_MIN_VALUE + DAY_METRIC_MAX_VALUE - value
    } else {
        value
    }
    return when {
        levelValue >= 8.0 -> PillColors(HighPillBackground, HighPillText)
        levelValue >= 5.0 -> PillColors(MediumPillBackground, MediumPillText)
        else -> PillColors(LowPillBackground, LowPillText)
    }
}

private fun Float.asMetricValue(): Double =
    ((this * 10f).roundToInt() / 10.0)
        .coerceIn(DAY_METRIC_MIN_VALUE, DAY_METRIC_MAX_VALUE)

private fun Double.asMetricLabel(): String {
    val tenths = (this * 10.0).roundToInt()
    return if (tenths % 10 == 0) {
        (tenths / 10).toString()
    } else {
        "${tenths / 10}.${tenths % 10}"
    }
}

private fun LocalDate.asRelativeDate(today: LocalDate): String {
    val prefix = when (today.toEpochDays() - toEpochDays()) {
        0L -> "Сегодня"
        1L -> "Вчера"
        else -> dayOfWeek.asName()
    }
    return "$prefix, $day ${monthNames[month.ordinal]}"
}

private fun DayOfWeek.asName(): String = weekdayNames[isoDayNumber - 1]

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
