package com.rhythmday.app

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.rhythmday.app.data.local.buildRhythmDayDatabase
import com.rhythmday.app.data.local.createDatabaseBuilder

fun main() {
    val database = buildRhythmDayDatabase(createDatabaseBuilder())
    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "RhythmDay"
        ) {
            App(database)
        }
    }
}

