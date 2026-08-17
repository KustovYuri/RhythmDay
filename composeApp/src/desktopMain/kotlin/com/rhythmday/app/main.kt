package com.rhythmday.app

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.rhythmday.app.data.local.buildRhythmDayDatabase
import com.rhythmday.app.data.local.createDatabaseBuilder
import com.rhythmday.app.feature.auth.data.createAuthRepository

fun main() {
    val database = buildRhythmDayDatabase(createDatabaseBuilder())
    val authRepository = createAuthRepository()
    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "RhythmDay"
        ) {
            App(database = database, authRepository = authRepository)
        }
    }
}

