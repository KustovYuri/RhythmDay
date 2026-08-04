package com.rhythmday.app

import androidx.compose.runtime.Composable
import com.rhythmday.app.core.designsystem.RhythmDayTheme
import com.rhythmday.app.data.local.RhythmDayDatabase
import com.rhythmday.app.di.appModule
import com.rhythmday.app.feature.settings.domain.DailyReminderScheduler
import com.rhythmday.app.feature.settings.domain.InMemoryReminderSettingsRepository
import com.rhythmday.app.feature.settings.domain.NoOpDailyReminderScheduler
import com.rhythmday.app.feature.settings.domain.ReminderSettingsRepository
import com.rhythmday.app.navigation.RhythmDayNavigation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import org.koin.compose.KoinApplication
import org.koin.dsl.koinConfiguration

@Composable
fun App(
    database: RhythmDayDatabase,
    seedDemoData: Boolean = false,
    reminderSettingsRepository: ReminderSettingsRepository = InMemoryReminderSettingsRepository,
    dailyReminderScheduler: DailyReminderScheduler = NoOpDailyReminderScheduler,
    openCreateEntryRequests: Flow<Unit> = emptyFlow()
) {
    KoinApplication(
        configuration = koinConfiguration {
            modules(
                appModule(
                    database = database,
                    seedDemoData = seedDemoData,
                    reminderSettingsRepository = reminderSettingsRepository,
                    dailyReminderScheduler = dailyReminderScheduler
                )
            )
        }
    ) {
        RhythmDayTheme {
            RhythmDayNavigation(openCreateEntryRequests)
        }
    }
}
