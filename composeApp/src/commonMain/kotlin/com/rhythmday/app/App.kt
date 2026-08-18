package com.rhythmday.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import com.rhythmday.app.core.designsystem.RhythmDayTheme
import com.rhythmday.app.core.presentation.UserMessageHost
import com.rhythmday.app.core.presentation.UserMessageManager
import com.rhythmday.app.data.local.RhythmDayDatabase
import com.rhythmday.app.data.remote.BackendDependencies
import com.rhythmday.app.data.sync.DiarySyncCoordinator
import com.rhythmday.app.di.appModule
import com.rhythmday.app.feature.auth.domain.AuthRepository
import com.rhythmday.app.feature.auth.domain.AuthSession
import com.rhythmday.app.feature.auth.domain.InMemoryAuthRepository
import com.rhythmday.app.feature.settings.domain.DailyReminderScheduler
import com.rhythmday.app.feature.settings.domain.InMemoryReminderSettingsRepository
import com.rhythmday.app.feature.settings.domain.NoOpDailyReminderScheduler
import com.rhythmday.app.feature.settings.domain.ReminderSettingsRepository
import com.rhythmday.app.navigation.RhythmDayNavigation
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.emptyFlow
import org.koin.compose.KoinApplication
import org.koin.compose.koinInject
import org.koin.dsl.koinConfiguration

@Composable
fun App(
    database: RhythmDayDatabase,
    seedDemoData: Boolean = false,
    reminderSettingsRepository: ReminderSettingsRepository = InMemoryReminderSettingsRepository,
    dailyReminderScheduler: DailyReminderScheduler = NoOpDailyReminderScheduler,
    backendDependencies: BackendDependencies? = null,
    authRepository: AuthRepository = backendDependencies?.authRepository ?: InMemoryAuthRepository(),
    openCreateEntryRequests: Flow<Unit> = emptyFlow(),
    openBlitzRequests: Flow<Unit> = emptyFlow()
) {
    val syncCoordinator = remember(database, backendDependencies) {
        backendDependencies?.let { backend ->
            DiarySyncCoordinator(
                api = backend.api,
                authorizedSession = backend.authorizedSession,
                dao = database.rhythmDayDao()
            )
        }
    }
    LaunchedEffect(authRepository, syncCoordinator) {
        val coordinator = syncCoordinator ?: return@LaunchedEffect
        authRepository.session.collectLatest { session ->
            if (session is AuthSession.Authenticated) {
                while (true) {
                    coordinator.refresh()
                    delay(SyncIntervalMilliseconds)
                }
            }
        }
    }

    KoinApplication(
        configuration = koinConfiguration {
            modules(
                appModule(
                    database = database,
                    seedDemoData = seedDemoData,
                    reminderSettingsRepository = reminderSettingsRepository,
                    dailyReminderScheduler = dailyReminderScheduler,
                    authRepository = authRepository,
                    syncCoordinator = syncCoordinator
                )
            )
        }
    ) {
        val userMessages = koinInject<UserMessageManager>()
        UserMessageHost(userMessages)
        RhythmDayTheme {
            RhythmDayNavigation(
                openCreateEntryRequests = openCreateEntryRequests,
                openBlitzRequests = openBlitzRequests
            )
        }
    }
}

private const val SyncIntervalMilliseconds = 60_000L
