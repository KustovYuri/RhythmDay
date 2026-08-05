package com.rhythmday.app.feature.blitz.presentation

import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rhythmday.app.core.designsystem.RhythmDayPalette
import com.rhythmday.app.core.designsystem.scoreAccentColor
import com.rhythmday.app.core.designsystem.scoreColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

private val CardShape = RoundedCornerShape(8.dp)
private val ControlBackground = Color(0xFFE9EEF2)
private const val AutoAdvanceDelayMillis = 260L

@Composable
fun BlitzRoute(
    viewModel: BlitzViewModel,
    contentPadding: PaddingValues,
    onBack: () -> Unit,
    onFinished: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var pendingAdvance by remember { mutableStateOf<BlitzMetric?>(null) }
    val pagerState = rememberPagerState(pageCount = { state.unlockedPageCount })

    LaunchedEffect(pendingAdvance, state.currentPage, state.currentScore) {
        val pendingMetric = pendingAdvance ?: return@LaunchedEffect
        if (pendingMetric != state.currentMetric || state.currentScore == null) {
            pendingAdvance = null
            return@LaunchedEffect
        }

        delay(AutoAdvanceDelayMillis)
        viewModel.advance()
        pendingAdvance = null
    }

    LaunchedEffect(state.currentPage) {
        if (pagerState.currentPage != state.currentPage) {
            pagerState.animateScrollToPage(state.currentPage)
        }
    }

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }
            .distinctUntilChanged()
            .collect(viewModel::showPage)
    }

    LaunchedEffect(state.isFinished) {
        if (state.isFinished) {
            viewModel.consumeFinishedEvent()
            onFinished()
        }
    }

    BlitzScreen(
        state = state,
        pagerState = pagerState,
        contentPadding = contentPadding,
        scoreSelectionEnabled = pendingAdvance == null && !state.isSaving,
        onBack = onBack,
        onScoreSelected = { score ->
            viewModel.selectScore(score)
            pendingAdvance = state.currentMetric
        },
        onPageSelected = { page ->
            pendingAdvance = null
            viewModel.showPage(page)
        },
        onEditAnswers = {
            pendingAdvance = null
            viewModel.editAnswers()
        },
        onFinish = viewModel::finish
    )
}

@Composable
private fun BlitzScreen(
    state: BlitzUiState,
    pagerState: PagerState,
    contentPadding: PaddingValues,
    scoreSelectionEnabled: Boolean,
    onBack: () -> Unit,
    onScoreSelected: (Int) -> Unit,
    onPageSelected: (Int) -> Unit,
    onEditAnswers: () -> Unit,
    onFinish: () -> Unit
) {
    val currentAccent = if (state.isSummaryPage) {
        RhythmDayPalette.Teal
    } else {
        state.currentMetric.content.accent
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            top = contentPadding.calculateTopPadding() + 2.dp,
            bottom = contentPadding.calculateBottomPadding() + 18.dp
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Box(modifier = Modifier.padding(horizontal = 18.dp)) {
                BlitzHeader(onBack)
            }
        }
        item {
            Box(modifier = Modifier.padding(horizontal = 18.dp)) {
                BlitzProgress(
                    state = state,
                    currentAccent = currentAccent
                )
            }
        }
        item {
            AnswersNavigation(
                state = state,
                onPageSelected = onPageSelected
            )
        }
        item {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 18.dp),
                pageSpacing = 10.dp,
                beyondViewportPageCount = 1,
                userScrollEnabled = !state.isSaving,
                key = { page -> page }
            ) { page ->
                CarouselPage(
                    pagerState = pagerState,
                    page = page
                ) {
                    if (page == BlitzMetric.entries.size) {
                        CompleteCard(
                            state = state,
                            onPageSelected = onPageSelected,
                            onEditAnswers = onEditAnswers,
                            onFinish = onFinish
                        )
                    } else {
                        val step = page
                        val metric = BlitzMetric.entries[step]
                        QuestionCard(
                            step = step,
                            metric = metric,
                            content = metric.content,
                            selectedScore = state.answers[metric],
                            selectionEnabled = scoreSelectionEnabled,
                            onScoreSelected = onScoreSelected
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CarouselPage(
    pagerState: PagerState,
    page: Int,
    content: @Composable () -> Unit
) {
    val pageOffset = (
        pagerState.currentPage - page + pagerState.currentPageOffsetFraction
    ).coerceIn(-1f, 1f)
    val distanceFromCenter = pageOffset.absoluteValue
    val scale = 1f - distanceFromCenter * 0.06f

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                translationY = distanceFromCenter * 12.dp.toPx()
                rotationY = -pageOffset * 7f
                rotationZ = pageOffset * 2.2f
                alpha = 1f - distanceFromCenter * 0.08f
                cameraDistance = 18f * density
                transformOrigin = TransformOrigin.Center
            }
    ) {
        content()
    }
}

@Composable
private fun BlitzHeader(onBack: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().height(46.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onBack,
            contentDescription = "Закрыть блиц-опрос"
        )
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Блиц-опрос",
                fontSize = 20.sp,
                lineHeight = 21.sp,
                fontWeight = FontWeight.Black,
                color = RhythmDayPalette.Ink
            )
            Text(
                text = "Только главное · около 30 секунд",
                fontSize = 11.sp,
                lineHeight = 14.sp,
                fontWeight = FontWeight.Bold,
                color = RhythmDayPalette.Muted,
                modifier = Modifier.padding(top = 3.dp)
            )
        }
        Spacer(modifier = Modifier.size(42.dp))
    }
}

@Composable
private fun IconButton(
    onClick: () -> Unit,
    contentDescription: String
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
            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
            contentDescription = contentDescription,
            tint = RhythmDayPalette.Ink,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun BlitzProgress(
    state: BlitzUiState,
    currentAccent: Color
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = if (state.isSummaryPage) {
                    "${BlitzMetric.entries.size} из ${BlitzMetric.entries.size}"
                } else {
                    "${state.currentPage + 1} из ${BlitzMetric.entries.size}"
                },
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = RhythmDayPalette.Muted
            )
            Text(
                text = when {
                    state.isSummaryPage -> "Готово"
                    state.isComplete -> "Все заполнено"
                    state.currentPage == BlitzMetric.entries.lastIndex -> "Последний вопрос"
                    else -> "Осталось ${BlitzMetric.entries.lastIndex - state.currentPage}"
                },
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = RhythmDayPalette.Muted
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp).height(8.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BlitzMetric.entries.forEachIndexed { index, metric ->
                val color = when {
                    !state.isSummaryPage && index == state.currentPage -> currentAccent
                    state.answers[metric] != null -> RhythmDayPalette.Teal
                    else -> Color(0xFFDCE4E9)
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(if (!state.isSummaryPage && index == state.currentPage) 8.dp else 6.dp)
                        .clip(CardShape)
                        .background(color)
                )
            }
        }
    }
}

@Composable
private fun AnswersNavigation(
    state: BlitzUiState,
    onPageSelected: (Int) -> Unit
) {
    val listState = rememberLazyListState()

    LaunchedEffect(state.currentPage, state.unlockedPageCount) {
        val targetPage = state.currentPage.coerceIn(0, state.unlockedPageCount - 1)
        listState.animateScrollToItem(targetPage)
    }

    LazyRow(
        modifier = Modifier.fillMaxWidth().height(40.dp),
        state = listState,
        horizontalArrangement = Arrangement.spacedBy(7.dp),
        contentPadding = PaddingValues(horizontal = 18.dp)
    ) {
        items(
            count = state.unlockedPageCount,
            key = { page -> page }
        ) { page ->
            val selected = state.currentPage == page
            val itemModifier = Modifier
                .animateItem(
                    fadeInSpec = tween(durationMillis = 320),
                    placementSpec = null,
                    fadeOutSpec = null
                )
            if (page == BlitzMetric.entries.size) {
                SummaryNavigationItem(
                    score = state.overallScore,
                    selected = selected,
                    modifier = itemModifier,
                    onClick = { onPageSelected(page) }
                )
            } else {
                val metric = BlitzMetric.entries[page]
                RecapItem(
                    label = metric.content.shortName,
                    score = state.answers[metric],
                    inverted = metric == BlitzMetric.STRESS,
                    selected = selected,
                    selectedBackground = metric.content.surface,
                    selectedContent = metric.content.ink,
                    modifier = itemModifier,
                    onClick = { onPageSelected(page) }
                )
            }
        }
    }
}

@Composable
private fun RecapItem(
    label: String,
    score: Int?,
    inverted: Boolean,
    selected: Boolean,
    selectedBackground: Color,
    selectedContent: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val scoreContent = score?.let { value ->
        scoreColors(value.toDouble(), inverted).content
    } ?: selectedContent
    Row(
        modifier = modifier
            .height(40.dp)
            .clip(CardShape)
            .background(if (selected) selectedBackground else ControlBackground)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(start = 10.dp, end = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            lineHeight = 14.sp,
            fontWeight = FontWeight.Bold,
            color = if (selected) selectedContent else RhythmDayPalette.Muted,
            maxLines = 1
        )
        Box(
            modifier = Modifier
                .size(width = 27.dp, height = 26.dp)
                .clip(CardShape)
                .background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = score?.toString() ?: "—",
                fontSize = 12.sp,
                lineHeight = 14.sp,
                fontWeight = FontWeight.Black,
                color = scoreContent
            )
        }
    }
}

@Composable
private fun SummaryNavigationItem(
    score: Double?,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Row(
        modifier = modifier
            .height(40.dp)
            .clip(CardShape)
            .background(if (selected) Color(0xFFE5F3F1) else ControlBackground)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(start = 10.dp, end = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        Text(
            text = "Итог",
            fontSize = 12.sp,
            lineHeight = 14.sp,
            fontWeight = FontWeight.Bold,
            color = if (selected) RhythmDayPalette.TealDark else RhythmDayPalette.Muted
        )
        Box(
            modifier = Modifier
                .size(width = 34.dp, height = 26.dp)
                .clip(CardShape)
                .background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = score?.asScore() ?: "—",
                fontSize = 12.sp,
                lineHeight = 14.sp,
                fontWeight = FontWeight.Black,
                color = RhythmDayPalette.TealDark
            )
        }
    }
}

@Composable
private fun QuestionCard(
    step: Int,
    metric: BlitzMetric,
    content: BlitzMetricContent,
    selectedScore: Int?,
    selectionEnabled: Boolean,
    onScoreSelected: (Int) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(424.dp)
            .clip(CardShape)
            .background(content.surface)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(5.dp)
                .background(content.accent)
        )
        Column(
            modifier = Modifier.fillMaxSize().padding(18.dp)
        ) {
            MetricHeader(
                step = step,
                title = content.title,
                selectedScore = selectedScore,
                content = content,
                inverted = metric == BlitzMetric.STRESS
            )
            Column(modifier = Modifier.padding(top = 34.dp)) {
                Text(
                    text = content.question,
                    fontSize = 27.sp,
                    lineHeight = 30.sp,
                    fontWeight = FontWeight.Black,
                    color = RhythmDayPalette.Ink
                )
                Text(
                    text = content.hint,
                    style = MaterialTheme.typography.bodyMedium,
                    color = RhythmDayPalette.Ink.copy(alpha = 0.72f),
                    modifier = Modifier.padding(top = 9.dp)
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = content.lowLabel,
                    fontSize = 11.sp,
                    lineHeight = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = content.ink.copy(alpha = 0.72f)
                )
                Text(
                    text = content.highLabel,
                    fontSize = 11.sp,
                    lineHeight = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = content.ink.copy(alpha = 0.72f),
                    textAlign = TextAlign.End
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                (1..10).forEach { score ->
                    ScoreButton(
                        score = score,
                        selected = selectedScore == score,
                        inverted = metric == BlitzMetric.STRESS,
                        enabled = selectionEnabled,
                        onClick = { onScoreSelected(score) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun MetricHeader(
    step: Int,
    title: String,
    selectedScore: Int?,
    content: BlitzMetricContent,
    inverted: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CardShape)
                .background(Color.White.copy(alpha = 0.72f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = (step + 1).toString().padStart(2, '0'),
                fontSize = 13.sp,
                lineHeight = 16.sp,
                fontWeight = FontWeight.Black,
                color = content.ink
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Параметр дня",
                fontSize = 11.sp,
                lineHeight = 13.sp,
                fontWeight = FontWeight.Bold,
                color = content.ink.copy(alpha = 0.72f)
            )
            Text(
                text = title,
                fontSize = 18.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.Black,
                color = content.ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Box(
            modifier = Modifier
                .size(width = 58.dp, height = 48.dp)
                .clip(CardShape)
                .background(Color.White.copy(alpha = 0.78f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = selectedScore?.toString() ?: "—",
                fontSize = 23.sp,
                lineHeight = 25.sp,
                fontWeight = FontWeight.Black,
                color = selectedScore?.let { score ->
                    scoreAccentColor(score.toDouble(), inverted)
                } ?: content.ink
            )
        }
    }
}

@Composable
private fun ScoreButton(
    score: Int,
    selected: Boolean,
    inverted: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedColor = scoreAccentColor(score.toDouble(), inverted)
    Box(
        modifier = modifier
            .height(46.dp)
            .clip(RoundedCornerShape(7.dp))
            .background(if (selected) selectedColor else Color.White.copy(alpha = 0.8f))
            .selectable(
                selected = selected,
                enabled = enabled,
                role = Role.RadioButton,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = score.toString(),
            fontSize = 13.sp,
            lineHeight = 15.sp,
            fontWeight = FontWeight.Black,
            color = if (selected) Color.White else RhythmDayPalette.Ink
        )
    }
}

@Composable
private fun CompleteCard(
    state: BlitzUiState,
    onPageSelected: (Int) -> Unit,
    onEditAnswers: () -> Unit,
    onFinish: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(Color(0xFFE5F3F1))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(11.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CardShape)
                    .background(RhythmDayPalette.Teal),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Главное отмечено",
                    fontSize = 20.sp,
                    lineHeight = 22.sp,
                    fontWeight = FontWeight.Black,
                    color = RhythmDayPalette.Ink
                )
                Text(
                    text = "Проверь ответы или сразу сохрани блиц.",
                    style = MaterialTheme.typography.bodySmall,
                    color = RhythmDayPalette.Muted,
                    modifier = Modifier.padding(top = 3.dp)
                )
            }
            Box(
                modifier = Modifier
                    .size(width = 64.dp, height = 48.dp)
                    .clip(CardShape)
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = state.overallScore?.asScore() ?: "—",
                    fontSize = 22.sp,
                    lineHeight = 24.sp,
                    fontWeight = FontWeight.Black,
                    color = RhythmDayPalette.TealDark
                )
            }
        }
        Column(
            modifier = Modifier.padding(top = 18.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            BlitzMetric.entries.chunked(2).forEach { rowMetrics ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    rowMetrics.forEach { metric ->
                        ResultItem(
                            metric = metric,
                            score = state.answers.getValue(metric),
                            onClick = { onPageSelected(metric.ordinal) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (rowMetrics.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
        PrimaryButton(
            text = if (state.isSaving) "Сохранение..." else "Сохранить",
            enabled = state.isComplete && !state.isSaving,
            onClick = onFinish,
            modifier = Modifier.padding(top = 16.dp)
        )
        if (state.errorMessage != null) {
            Text(
                text = state.errorMessage,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            )
        }
        SecondaryButton(
            text = "Изменить ответы",
            enabled = !state.isSaving,
            onClick = onEditAnswers,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

@Composable
private fun ResultItem(
    metric: BlitzMetric,
    score: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = scoreColors(score.toDouble(), metric == BlitzMetric.STRESS)
    Row(
        modifier = modifier
            .height(48.dp)
            .clip(CardShape)
            .background(Color.White.copy(alpha = 0.72f))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(start = 10.dp, end = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        Text(
            text = metric.content.title,
            fontSize = 12.sp,
            lineHeight = 14.sp,
            fontWeight = FontWeight.Bold,
            color = RhythmDayPalette.Ink,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Box(
            modifier = Modifier
                .size(width = 27.dp, height = 26.dp)
                .clip(CardShape)
                .background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = score.toString(),
                fontSize = 12.sp,
                lineHeight = 14.sp,
                fontWeight = FontWeight.Black,
                color = colors.content
            )
        }
    }
}

@Composable
private fun PrimaryButton(
    text: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(46.dp)
            .clip(CardShape)
            .background(if (enabled) RhythmDayPalette.Teal else ControlBackground)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text,
            fontSize = 14.sp,
            lineHeight = 18.sp,
            fontWeight = FontWeight.Bold,
            color = if (enabled) Color.White else RhythmDayPalette.Muted
        )
    }
}

@Composable
private fun SecondaryButton(
    text: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(46.dp)
            .clip(CardShape)
            .background(Color.White.copy(alpha = if (enabled) 0.62f else 0.36f))
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 14.sp,
            lineHeight = 18.sp,
            fontWeight = FontWeight.Bold,
            color = if (enabled) RhythmDayPalette.TealDark else RhythmDayPalette.Muted
        )
    }
}

private data class BlitzMetricContent(
    val title: String,
    val shortName: String,
    val question: String,
    val hint: String,
    val lowLabel: String,
    val highLabel: String,
    val accent: Color,
    val surface: Color,
    val ink: Color
)

private val BlitzMetric.content: BlitzMetricContent
    get() = when (this) {
        BlitzMetric.ENERGY -> BlitzMetricContent(
            title = "Энергия",
            shortName = "Энергия",
            question = "Сколько сил было сегодня?",
            hint = "Общий запас энергии в течение дня.",
            lowLabel = "Совсем нет",
            highLabel = "Очень много",
            accent = Color(0xFFE5A52D),
            surface = Color(0xFFFFF3D6),
            ink = Color(0xFF76510A)
        )

        BlitzMetric.MOOD -> BlitzMetricContent(
            title = "Настроение",
            shortName = "Настроение",
            question = "Каким было настроение?",
            hint = "Общая эмоциональная окраска дня.",
            lowLabel = "Тяжёлое",
            highLabel = "Отличное",
            accent = Color(0xFF319DB8),
            surface = Color(0xFFDFF3F7),
            ink = Color(0xFF25697A)
        )

        BlitzMetric.WELLBEING -> BlitzMetricContent(
            title = "Самочувствие",
            shortName = "Самочувствие",
            question = "Как чувствовало себя тело?",
            hint = "Тонус, усталость и физический комфорт.",
            lowLabel = "Плохо",
            highLabel = "Отлично",
            accent = Color(0xFF4AA276),
            surface = Color(0xFFE5F3EA),
            ink = Color(0xFF2F704F)
        )

        BlitzMetric.SLEEP_QUALITY -> BlitzMetricContent(
            title = "Качество сна",
            shortName = "Сон",
            question = "Насколько хорошо ты спал?",
            hint = "Оцени восстановление после сна.",
            lowLabel = "Очень плохо",
            highLabel = "Отлично",
            accent = Color(0xFF588BC2),
            surface = Color(0xFFE8F0F8),
            ink = Color(0xFF365F8D)
        )

        BlitzMetric.MENTAL_CLARITY -> BlitzMetricContent(
            title = "Ясность ума",
            shortName = "Ясность",
            question = "Насколько ясной была голова?",
            hint = "Фокус, память и лёгкость мышления.",
            lowLabel = "Туман",
            highLabel = "Полная ясность",
            accent = Color(0xFF299B91),
            surface = Color(0xFFE2F2F0),
            ink = Color(0xFF176D66)
        )

        BlitzMetric.SOCIALITY -> BlitzMetricContent(
            title = "Социальность",
            shortName = "Общение",
            question = "Хватало ли ресурса на людей?",
            hint = "Желание и силы общаться с другими.",
            lowLabel = "Не хватало",
            highLabel = "Было много",
            accent = Color(0xFF8873BD),
            surface = Color(0xFFEEEBF7),
            ink = Color(0xFF5E4B8C)
        )

        BlitzMetric.STRESS -> BlitzMetricContent(
            title = "Стресс",
            shortName = "Стресс",
            question = "Насколько напряжённым был день?",
            hint = "Здесь высокая оценка означает сильный стресс.",
            lowLabel = "Спокойно",
            highLabel = "Очень сильно",
            accent = Color(0xFFD26D50),
            surface = Color(0xFFF8E8E3),
            ink = Color(0xFF8B4938)
        )
    }

private fun Double.asScore(): String {
    val tenths = (this * 10).roundToInt()
    return "${tenths / 10}.${tenths % 10}"
}
