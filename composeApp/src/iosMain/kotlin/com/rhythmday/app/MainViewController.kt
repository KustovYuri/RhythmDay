package com.rhythmday.app

import androidx.compose.ui.window.ComposeUIViewController
import com.rhythmday.app.data.local.buildRhythmDayDatabase
import com.rhythmday.app.data.local.createDatabaseBuilder
import com.rhythmday.app.feature.auth.data.createPlatformBackendDependencies

fun MainViewController() = buildRhythmDayDatabase(createDatabaseBuilder()).let { database ->
    val backendDependencies = createPlatformBackendDependencies()
    ComposeUIViewController { App(database = database, backendDependencies = backendDependencies) }
}

