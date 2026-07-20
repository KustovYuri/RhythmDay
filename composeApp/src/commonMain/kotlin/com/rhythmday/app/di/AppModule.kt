package com.rhythmday.app.di

import com.rhythmday.app.core.domain.repository.DayEntryRepository
import com.rhythmday.app.core.domain.usecase.DayScoreCalculatorUseCase
import com.rhythmday.app.core.domain.usecase.SaveDayEntryUseCase
import com.rhythmday.app.data.repository.MockDayEntryRepository
import com.rhythmday.app.feature.createentry.presentation.CreateEntryViewModel
import com.rhythmday.app.feature.home.domain.usecase.ObserveHomeDashboardUseCase
import com.rhythmday.app.feature.home.presentation.HomeViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    single { DayScoreCalculatorUseCase() }
    single<DayEntryRepository> { MockDayEntryRepository(get()) }
    single { SaveDayEntryUseCase(get(), get()) }
    single { ObserveHomeDashboardUseCase(get()) }
    viewModel { CreateEntryViewModel(get()) }
    viewModel { HomeViewModel(get()) }
}
