package com.rhythmday.app.di

import com.rhythmday.app.core.domain.repository.DayEntryRepository
import com.rhythmday.app.core.domain.repository.DayTagRepository
import com.rhythmday.app.core.domain.usecase.DayScoreCalculatorUseCase
import com.rhythmday.app.core.domain.usecase.ObserveDayEntryUseCase
import com.rhythmday.app.core.domain.usecase.SaveDayEntryUseCase
import com.rhythmday.app.data.local.RhythmDayDatabase
import com.rhythmday.app.data.repository.DemoDataSeeder
import com.rhythmday.app.data.repository.RoomDayEntryRepository
import com.rhythmday.app.data.repository.RoomDayTagRepository
import com.rhythmday.app.feature.analytics.presentation.AnalyticsViewModel
import com.rhythmday.app.feature.createentry.presentation.CreateEntryViewModel
import com.rhythmday.app.feature.entrydetails.presentation.EntryDetailsViewModel
import com.rhythmday.app.feature.history.presentation.HistoryViewModel
import com.rhythmday.app.feature.home.domain.usecase.ObserveHomeDashboardUseCase
import com.rhythmday.app.feature.home.presentation.HomeViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

fun appModule(
    database: RhythmDayDatabase,
    seedDemoData: Boolean = false
) = module {
    single { database }
    single { database.rhythmDayDao() }
    single { DayScoreCalculatorUseCase() }
    single { DemoDataSeeder(get(), get()) }
    single<DayEntryRepository> {
        RoomDayEntryRepository(
            dao = get(),
            demoDataSeeder = get<DemoDataSeeder>().takeIf { seedDemoData }
        )
    }
    single<DayTagRepository> { RoomDayTagRepository(get()) }
    single { SaveDayEntryUseCase(get(), get()) }
    single { ObserveHomeDashboardUseCase(get()) }
    single { ObserveDayEntryUseCase(get()) }
    viewModel { CreateEntryViewModel(get(), get(), get()) }
    viewModel { AnalyticsViewModel(get()) }
    viewModel { HistoryViewModel(get()) }
    viewModel { parameters ->
        EntryDetailsViewModel(
            entryId = parameters.get(),
            observeDayEntryUseCase = get()
        )
    }
    viewModel { HomeViewModel(get()) }
}
