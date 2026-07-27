package com.rhythmday.app.navigation

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import androidx.savedstate.serialization.SavedStateConfiguration
import com.rhythmday.app.core.designsystem.RhythmDayIcons
import com.rhythmday.app.core.designsystem.RhythmDayPalette
import com.rhythmday.app.core.domain.repository.DayEntryRepository
import com.rhythmday.app.feature.analytics.presentation.AnalyticsRoute as AnalyticsScreenRoute
import com.rhythmday.app.feature.createentry.presentation.CreateEntryRoute as CreateEntryScreenRoute
import com.rhythmday.app.feature.entrydetails.presentation.EntryDetailsRoute as EntryDetailsScreenRoute
import com.rhythmday.app.feature.history.presentation.HistoryRoute as HistoryScreenRoute
import com.rhythmday.app.feature.home.presentation.HomeRoute
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import org.koin.compose.koinInject
import kotlin.time.Clock

private const val ScreenTransitionDurationMillis = 220
private const val ScreenTransitionSettlingDelayMillis = 32L

private val navigationStateConfiguration = SavedStateConfiguration {
    serializersModule = SerializersModule {
        polymorphic(NavKey::class) {
            subclass(HomeRouteKey::class, HomeRouteKey.serializer())
            subclass(HistoryRoute::class, HistoryRoute.serializer())
            subclass(CreateEntryRoute::class, CreateEntryRoute.serializer())
            subclass(EditEntryRoute::class, EditEntryRoute.serializer())
            subclass(AnalyticsRoute::class, AnalyticsRoute.serializer())
            subclass(AiRoute::class, AiRoute.serializer())
            subclass(BlitzRoute::class, BlitzRoute.serializer())
            subclass(SettingsRoute::class, SettingsRoute.serializer())
            subclass(EntryDetailsRoute::class, EntryDetailsRoute.serializer())
        }
    }
}

private val topLevelRoutes = listOf(
    TopLevelDestination(HomeRouteKey, "Главная", RhythmDayIcons.Home),
    TopLevelDestination(HistoryRoute, "История", RhythmDayIcons.Calendar),
    TopLevelDestination(CreateEntryRoute, "Запись дня", Icons.Filled.Add),
    TopLevelDestination(AnalyticsRoute, "Аналитика", RhythmDayIcons.Analytics),
    TopLevelDestination(AiRoute, "ИИ", RhythmDayIcons.Ai)
)

private data class TopLevelDestination(
    val route: AppRoute,
    val contentDescription: String,
    val icon: ImageVector
)

@Composable
fun RhythmDayNavigation() {
    val backStack = rememberNavBackStack(navigationStateConfiguration, HomeRouteKey)
    val coroutineScope = rememberCoroutineScope()
    val dayEntryRepository = koinInject<DayEntryRepository>()
    val entriesFlow = remember(dayEntryRepository) { dayEntryRepository.observeEntries() }
    val entries by entriesFlow.collectAsStateWithLifecycle(emptyList())
    var navigationDirection by remember { mutableStateOf(1) }
    var pendingTopLevelRoute by remember { mutableStateOf<AppRoute?>(null) }
    var topLevelNavigationJob by remember { mutableStateOf<Job?>(null) }

    fun updateNavigationDirection(targetRoute: AppRoute) {
        val currentRoute = backStack.lastOrNull() as? AppRoute
        navigationDirection = transitionDirection(currentRoute, targetRoute)
    }

    fun navigateToTopLevel(route: AppRoute) {
        updateNavigationDirection(route)
        while (backStack.size > 1) {
            backStack.removeLast()
        }
        if (route != HomeRouteKey) {
            backStack.add(route)
        }
    }

    fun openTopLevel(route: AppRoute) {
        if (topLevelNavigationJob?.isActive == true) {
            pendingTopLevelRoute = route
            return
        }
        if (backStack.lastOrNull() == route) return

        navigateToTopLevel(route)
        topLevelNavigationJob = coroutineScope.launch {
            delay(ScreenTransitionDurationMillis.toLong() + ScreenTransitionSettlingDelayMillis)
            topLevelNavigationJob = null

            val pendingRoute = pendingTopLevelRoute
            pendingTopLevelRoute = null
            if (pendingRoute != null && pendingRoute != backStack.lastOrNull()) {
                openTopLevel(pendingRoute)
            }
        }
    }

    fun open(route: AppRoute) {
        if (route.topLevelIndex() != null) {
            openTopLevel(route)
            return
        }
        if (backStack.lastOrNull() != route) {
            updateNavigationDirection(route)
            backStack.add(route)
        }
    }

    fun openCreateEntry() {
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        val todayEntryId = entries.firstOrNull { entry -> entry.date == today }?.id
        if (todayEntryId == null) {
            openTopLevel(CreateEntryRoute)
            return
        }

        val detailsRoute = EntryDetailsRoute(todayEntryId)
        if (backStack.lastOrNull() == detailsRoute) return

        navigateToTopLevel(CreateEntryRoute)
        updateNavigationDirection(detailsRoute)
        backStack.add(detailsRoute)
    }

    fun navigateBack() {
        if (backStack.size > 1) {
            val previousRoute = backStack[backStack.lastIndex - 1] as AppRoute
            if (
                backStack.size > 2 &&
                backStack.lastOrNull() is EntryDetailsRoute &&
                previousRoute == CreateEntryRoute
            ) {
                val targetRoute = backStack[backStack.lastIndex - 2] as AppRoute
                updateNavigationDirection(targetRoute)
                backStack.removeLast()
                backStack.removeLast()
                return
            }
            if (topLevelNavigationJob?.isActive == true && previousRoute.topLevelIndex() != null) {
                pendingTopLevelRoute = previousRoute
                return
            }
            updateNavigationDirection(previousRoute)
            backStack.removeLast()
        }
    }

    val currentRoute = backStack.lastOrNull() as? AppRoute
    val selectedTopLevelRoute = when (currentRoute) {
        is EntryDetailsRoute, is EditEntryRoute -> backStack
            .dropLast(1)
            .mapNotNull { route -> route as? AppRoute }
            .lastOrNull { route -> route.topLevelIndex() != null }

        else -> currentRoute?.takeIf { route -> route.topLevelIndex() != null }
    }

    Scaffold(
        containerColor = RhythmDayPalette.Background,
        bottomBar = {
            selectedTopLevelRoute?.let { selectedRoute ->
                BottomNavigation(
                    selectedRoute = selectedRoute,
                    onNavigate = { route ->
                        if (route == CreateEntryRoute) openCreateEntry() else openTopLevel(route)
                    }
                )
            }
        }
    ) { contentPadding ->
        NavDisplay(
            backStack = backStack,
            onBack = ::navigateBack,
            transitionSpec = {
                (slideInHorizontally(
                    animationSpec = tween(durationMillis = ScreenTransitionDurationMillis),
                    initialOffsetX = { width -> width * navigationDirection / 8 }
                ) + fadeIn(animationSpec = tween(durationMillis = 160))) togetherWith
                    (slideOutHorizontally(
                        animationSpec = tween(durationMillis = ScreenTransitionDurationMillis),
                        targetOffsetX = { width -> -width * navigationDirection / 12 }
                    ) + fadeOut(animationSpec = tween(durationMillis = 140)))
            },
            popTransitionSpec = {
                (slideInHorizontally(
                    animationSpec = tween(durationMillis = ScreenTransitionDurationMillis),
                    initialOffsetX = { width -> width * navigationDirection / 8 }
                ) + fadeIn(animationSpec = tween(durationMillis = 160))) togetherWith
                    (slideOutHorizontally(
                        animationSpec = tween(durationMillis = ScreenTransitionDurationMillis),
                        targetOffsetX = { width -> -width * navigationDirection / 12 }
                    ) + fadeOut(animationSpec = tween(durationMillis = 140)))
            },
            entryProvider = entryProvider {
                entry<HomeRouteKey> {
                    HomeRoute(
                        viewModel = koinViewModel(),
                        contentPadding = contentPadding,
                        onCreateEntry = ::openCreateEntry,
                        onEditEntry = { entryId -> open(EditEntryRoute(entryId)) },
                        onStartBlitz = { open(BlitzRoute) },
                        onOpenHistory = { openTopLevel(HistoryRoute) },
                        onOpenAnalytics = { openTopLevel(AnalyticsRoute) },
                        onOpenEntry = { entryId -> open(EntryDetailsRoute(entryId)) },
                        onOpenSettings = { open(SettingsRoute) }
                    )
                }
                entry<HistoryRoute> {
                    HistoryScreenRoute(
                        viewModel = koinViewModel(),
                        contentPadding = contentPadding,
                        onOpenEntry = { entryId -> open(EntryDetailsRoute(entryId)) }
                    )
                }
                entry<CreateEntryRoute> {
                    CreateEntryScreenRoute(
                        viewModel = koinViewModel(key = "entry-form-create"),
                        entryId = null,
                        showBackButton = false,
                        contentPadding = contentPadding,
                        onBack = ::navigateBack
                    )
                }
                entry<EditEntryRoute> { route ->
                    CreateEntryScreenRoute(
                        viewModel = koinViewModel(key = "entry-form-edit-${route.entryId}"),
                        entryId = route.entryId,
                        showBackButton = true,
                        contentPadding = contentPadding,
                        onBack = ::navigateBack
                    )
                }
                entry<AnalyticsRoute> {
                    AnalyticsScreenRoute(
                        viewModel = koinViewModel(),
                        contentPadding = contentPadding,
                        onOpenEntry = { entryId -> open(EntryDetailsRoute(entryId)) }
                    )
                }
                entry<AiRoute> {
                    PlaceholderContent("Функционал в разработке", contentPadding)
                }
                entry<BlitzRoute> {
                    NestedPlaceholder("Блиц-опрос", ::navigateBack)
                }
                entry<SettingsRoute> {
                    NestedPlaceholder("Настройки", ::navigateBack)
                }
                entry<EntryDetailsRoute> { route ->
                    EntryDetailsScreenRoute(
                        viewModel = koinViewModel(
                            key = "entry-details-${route.entryId}",
                            parameters = { parametersOf(route.entryId) }
                        ),
                        contentPadding = contentPadding,
                        onBack = ::navigateBack,
                        onEdit = { open(EditEntryRoute(route.entryId)) }
                    )
                }
            }
        )
    }
}

@Composable
private fun BottomNavigation(
    selectedRoute: AppRoute,
    onNavigate: (AppRoute) -> Unit
) {
    Column(
        modifier = Modifier
            .background(RhythmDayPalette.Panel)
            .navigationBarsPadding()
    ) {
        HorizontalDivider(color = RhythmDayPalette.Line)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(69.dp)
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            topLevelRoutes.forEach { destination ->
                val selected = selectedRoute == destination.route
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(horizontal = 1.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (selected) ColorSelectedNavigation
                            else RhythmDayPalette.Panel
                        )
                        .clickable(
                            role = Role.Tab,
                            onClick = { onNavigate(destination.route) }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = destination.icon,
                        contentDescription = destination.contentDescription,
                        tint = if (selected) {
                            RhythmDayPalette.TealDark
                        } else {
                            RhythmDayPalette.Muted
                        },
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}

private fun transitionDirection(
    currentRoute: AppRoute?,
    targetRoute: AppRoute
): Int {
    val currentIndex = currentRoute?.topLevelIndex()
    val targetIndex = targetRoute.topLevelIndex()

    return when {
        currentIndex != null && targetIndex != null -> {
            if (targetIndex > currentIndex) 1 else -1
        }

        currentIndex == null && targetIndex != null -> -1
        else -> 1
    }
}

private fun AppRoute.topLevelIndex(): Int? =
    topLevelRoutes.indexOfFirst { destination -> destination.route == this }
        .takeIf { index -> index >= 0 }

@Composable
private fun NestedPlaceholder(
    title: String,
    onBack: () -> Unit
) {
    Scaffold(
        containerColor = RhythmDayPalette.Background,
        topBar = {
            Box(
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(start = 10.dp, top = 8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(RhythmDayPalette.Panel)
                        .clickable(role = Role.Button, onClick = onBack),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = "Назад",
                        tint = RhythmDayPalette.Ink
                    )
                }
            }
        }
    ) { padding ->
        PlaceholderContent(title, padding)
    }
}

@Composable
private fun PlaceholderContent(
    title: String,
    contentPadding: PaddingValues
) {
    Box(
        modifier = Modifier.fillMaxSize().padding(contentPadding).padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = RhythmDayPalette.Ink
        )
    }
}

private val ColorSelectedNavigation = androidx.compose.ui.graphics.Color(0xFFE9F4F2)
