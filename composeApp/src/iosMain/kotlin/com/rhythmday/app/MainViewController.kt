package com.rhythmday.app

import androidx.compose.ui.window.ComposeUIViewController
import com.rhythmday.app.data.local.buildRhythmDayDatabase
import com.rhythmday.app.data.local.createDatabaseBuilder

fun MainViewController() = buildRhythmDayDatabase(createDatabaseBuilder()).let { database ->
    ComposeUIViewController { App(database) }
}

