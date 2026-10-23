package com.rhythmday.app.di

import com.rhythmday.app.core.domain.repository.DayEntryRepository
import com.rhythmday.app.core.domain.repository.DayPhotoRepository
import com.rhythmday.app.core.domain.repository.DayTagRepository
import com.rhythmday.app.core.domain.usecase.DayScoreCalculatorUseCase
import com.rhythmday.app.core.domain.usecase.ObserveDayEntryUseCase
import com.rhythmday.app.core.domain.usecase.SaveDayEntryUseCase
import com.rhythmday.app.data.local.RhythmDayDatabase
import com.rhythmday.app.data.remote.AiReportApi
import com.rhythmday.app.data.remote.createAiHttpClient
import com.rhythmday.app.data.remote.platformAiBaseUrl
import com.rhythmday.app.data.repository.DemoDataSeeder
import com.rhythmday.app.data.repository.LocalAiReportRepository
import com.rhythmday.app.data.repository.RoomDayEntryRepository
import com.rhythmday.app.data.repository.RoomDayPhotoRepository
import com.rhythmday.app.data.repository.RoomDayTagRepository
import com.rhythmday.app.feature.blitz.presentation.BlitzViewModel
import com.rhythmday.app.feature.analytics.presentation.AnalyticsViewModel
import com.rhythmday.app.feature.ai.domain.AiReportRepository
import com.rhythmday.app.feature.ai.presentation.AiReportViewModel
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
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

fun appModule(
    database: RhythmDayDatabase,
    seedDemoData: Boolean = false,
    reminderSettingsRepository: ReminderSettingsRepository = InMemoryReminderSettingsRepository,
    dailyReminderScheduler: DailyReminderScheduler = NoOpDailyReminderScheduler
) = module {
    single { database }
    single { database.rhythmDayDao() }
    single { createAiHttpClient() }
    single { AiReportApi(get(), platformAiBaseUrl()) }
    single { DayScoreCalculatorUseCase() }
    single { DemoDataSeeder(get(), get()) }
    single<DayEntryRepository> {
        RoomDayEntryRepository(
            dao = get(),
            demoDataSeeder = get<DemoDataSeeder>().takeIf { seedDemoData }
        )
    }
    single<DayTagRepository> { RoomDayTagRepository(get()) }
    single<DayPhotoRepository> { RoomDayPhotoRepository(get()) }
    single<AiReportRepository> { LocalAiReportRepository(get(), get()) }
    single { reminderSettingsRepository }
    single { dailyReminderScheduler }
    single { SaveDayEntryUseCase(get(), get()) }
    single { ObserveHomeDashboardUseCase(get()) }
    single { ObserveDayEntryUseCase(get()) }
    viewModel { BlitzViewModel(get(), get()) }
    viewModel { CreateEntryViewModel(get(), get(), get(), get()) }
    viewModel { AnalyticsViewModel(get()) }
    viewModel { parameters ->
        AiReportViewModel(
            from = parameters.get(),
            to = parameters.get(),
            dayEntryRepository = get(),
            aiReportRepository = get()
        )
    }
    viewModel { HistoryViewModel(get()) }
    viewModel { parameters ->
        EntryDetailsViewModel(
            entryId = parameters.get(),
            observeDayEntryUseCase = get(),
            dayPhotoRepository = get()
        )
    }
    viewModel { HomeViewModel(get()) }
    viewModel { SettingsViewModel(get(), get()) }
}
