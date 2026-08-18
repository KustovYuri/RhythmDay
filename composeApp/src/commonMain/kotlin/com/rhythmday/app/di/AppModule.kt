package com.rhythmday.app.di

import com.rhythmday.app.core.domain.repository.DayEntryRepository
import com.rhythmday.app.core.domain.repository.DayTagRepository
import com.rhythmday.app.core.domain.usecase.DayScoreCalculatorUseCase
import com.rhythmday.app.core.domain.usecase.ObserveDayEntryUseCase
import com.rhythmday.app.core.domain.usecase.SaveDayEntryUseCase
import com.rhythmday.app.core.presentation.DiaryRefreshViewModel
import com.rhythmday.app.core.presentation.UserMessageManager
import com.rhythmday.app.data.local.RhythmDayDatabase
import com.rhythmday.app.data.repository.DemoDataSeeder
import com.rhythmday.app.data.repository.RoomDayEntryRepository
import com.rhythmday.app.data.repository.RoomDayTagRepository
import com.rhythmday.app.data.sync.DiarySyncCoordinator
import com.rhythmday.app.feature.blitz.presentation.BlitzViewModel
import com.rhythmday.app.feature.analytics.presentation.AnalyticsViewModel
import com.rhythmday.app.feature.createentry.presentation.CreateEntryViewModel
import com.rhythmday.app.feature.entrydetails.presentation.EntryDetailsViewModel
import com.rhythmday.app.feature.history.presentation.HistoryViewModel
import com.rhythmday.app.feature.home.domain.usecase.ObserveHomeDashboardUseCase
import com.rhythmday.app.feature.home.presentation.HomeViewModel
import com.rhythmday.app.feature.settings.domain.DailyReminderScheduler
import com.rhythmday.app.feature.settings.domain.NoOpDailyReminderScheduler
import com.rhythmday.app.feature.settings.domain.InMemoryReminderSettingsRepository
import com.rhythmday.app.feature.settings.domain.ReminderSettingsRepository
import com.rhythmday.app.feature.settings.presentation.SettingsViewModel
import com.rhythmday.app.feature.auth.domain.AuthRepository
import com.rhythmday.app.feature.auth.domain.InMemoryAuthRepository
import com.rhythmday.app.feature.auth.presentation.AuthViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

fun appModule(
    database: RhythmDayDatabase,
    seedDemoData: Boolean = false,
    reminderSettingsRepository: ReminderSettingsRepository = InMemoryReminderSettingsRepository,
    dailyReminderScheduler: DailyReminderScheduler = NoOpDailyReminderScheduler,
    authRepository: AuthRepository = InMemoryAuthRepository(),
    syncCoordinator: DiarySyncCoordinator? = null
) = module {
    single { database }
    single { database.rhythmDayDao() }
    single { DayScoreCalculatorUseCase() }
    single { DemoDataSeeder(get(), get()) }
    single<CoroutineScope> { CoroutineScope(SupervisorJob() + Dispatchers.Default) }
    single<DayEntryRepository> {
        RoomDayEntryRepository(
            dao = get(),
            demoDataSeeder = get<DemoDataSeeder>().takeIf { seedDemoData },
            syncCoordinator = syncCoordinator,
            applicationScope = get()
        )
    }
    single<DayTagRepository> {
        RoomDayTagRepository(
            dao = get(),
            syncCoordinator = syncCoordinator,
            applicationScope = get()
        )
    }
    single { reminderSettingsRepository }
    single { dailyReminderScheduler }
    single { authRepository }
    single { UserMessageManager() }
    single { SaveDayEntryUseCase(get(), get()) }
    single { ObserveHomeDashboardUseCase(get()) }
    single { ObserveDayEntryUseCase(get()) }
    viewModel { DiaryRefreshViewModel(syncCoordinator, get()) }
    viewModel { BlitzViewModel(get(), get(), get()) }
    viewModel { CreateEntryViewModel(get(), get(), get(), get()) }
    viewModel { AnalyticsViewModel(get(), messages = get()) }
    viewModel { HistoryViewModel(get(), messages = get()) }
    viewModel { parameters ->
        EntryDetailsViewModel(
            entryId = parameters.get(),
            observeDayEntryUseCase = get(),
            messages = get()
        )
    }
    viewModel { HomeViewModel(get(), get()) }
    viewModel { SettingsViewModel(get(), get()) }
    viewModel { AuthViewModel(get(), get()) }
}
