package com.rhythmday.app.di

import com.rhythmday.app.core.domain.repository.DayEntryRepository
import com.rhythmday.app.core.domain.usecase.DayScoreCalculatorUseCase
import com.rhythmday.app.core.domain.usecase.ObserveDayEntryUseCase
import com.rhythmday.app.core.domain.usecase.SaveDayEntryUseCase
import com.rhythmday.app.data.repository.MockDayEntryRepository
import com.rhythmday.app.feature.createentry.presentation.CreateEntryViewModel
import com.rhythmday.app.feature.entrydetails.presentation.EntryDetailsViewModel
import com.rhythmday.app.feature.home.domain.usecase.ObserveHomeDashboardUseCase
import com.rhythmday.app.feature.home.presentation.HomeViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    single { DayScoreCalculatorUseCase() }
    single<DayEntryRepository> { MockDayEntryRepository(get()) }
    single { SaveDayEntryUseCase(get(), get()) }
    single { ObserveHomeDashboardUseCase(get()) }
    single { ObserveDayEntryUseCase(get()) }
    viewModel { CreateEntryViewModel(get(), get()) }
    viewModel { parameters ->
        EntryDetailsViewModel(
            entryId = parameters.get(),
            observeDayEntryUseCase = get()
        )
    }
    viewModel { HomeViewModel(get()) }
}
