package com.rhythmday.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import android.content.Intent
import com.rhythmday.app.feature.settings.data.AndroidDailyReminderScheduler
import com.rhythmday.app.feature.settings.data.AndroidReminderSettingsRepository
import com.rhythmday.app.feature.settings.data.OPEN_CREATE_ENTRY_ACTION
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow

class MainActivity : ComponentActivity() {
    private val reminderSettingsRepository by lazy {
        AndroidReminderSettingsRepository(applicationContext)
    }
    private val dailyReminderScheduler by lazy {
        AndroidDailyReminderScheduler(applicationContext)
    }
    private val openCreateEntryRequests = Channel<Unit>(capacity = Channel.BUFFERED)
    private val openCreateEntryRequestsFlow = openCreateEntryRequests.receiveAsFlow()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleNavigationIntent(intent)
        val database = (application as RhythmDayApplication).database
        setContent {
            App(
                database = database,
                seedDemoData = BuildConfig.SEED_DEMO_DATA,
                reminderSettingsRepository = reminderSettingsRepository,
                dailyReminderScheduler = dailyReminderScheduler,
                openCreateEntryRequests = openCreateEntryRequestsFlow
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleNavigationIntent(intent)
    }

    private fun handleNavigationIntent(intent: Intent?) {
        if (intent?.action == OPEN_CREATE_ENTRY_ACTION) {
            openCreateEntryRequests.trySend(Unit)
            intent.action = null
        }
    }
}

