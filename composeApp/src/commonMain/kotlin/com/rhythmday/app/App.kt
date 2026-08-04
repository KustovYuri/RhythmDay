package com.rhythmday.app

import androidx.compose.runtime.Composable
import com.rhythmday.app.core.designsystem.RhythmDayTheme
import com.rhythmday.app.data.local.RhythmDayDatabase
import com.rhythmday.app.di.appModule
import com.rhythmday.app.navigation.RhythmDayNavigation
import org.koin.compose.KoinApplication
import org.koin.dsl.koinConfiguration

@Composable
fun App(
    database: RhythmDayDatabase,
    seedDemoData: Boolean = false
) {
    KoinApplication(
        configuration = koinConfiguration {
            modules(appModule(database, seedDemoData))
        }
    ) {
        RhythmDayTheme {
            RhythmDayNavigation()
        }
    }
}
