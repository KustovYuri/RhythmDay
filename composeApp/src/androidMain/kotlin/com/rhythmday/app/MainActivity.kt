package com.rhythmday.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import android.content.Intent
import com.rhythmday.app.feature.settings.data.AndroidDailyReminderScheduler
import com.rhythmday.app.feature.settings.data.AndroidReminderSettingsRepository
import com.rhythmday.app.feature.settings.data.OPEN_BLITZ_ACTION
import com.rhythmday.app.feature.settings.data.OPEN_CREATE_ENTRY_ACTION
import com.rhythmday.app.feature.settings.data.dismissDailyReminderNotification
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
    private val openBlitzRequests = Channel<Unit>(capacity = Channel.BUFFERED)
    private val openBlitzRequestsFlow = openBlitzRequests.receiveAsFlow()

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
                openCreateEntryRequests = openCreateEntryRequestsFlow,
                openBlitzRequests = openBlitzRequestsFlow
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleNavigationIntent(intent)
    }

    private fun handleNavigationIntent(intent: Intent?) {
        when (intent?.action) {
            OPEN_CREATE_ENTRY_ACTION -> openCreateEntryRequests.trySend(Unit)
            OPEN_BLITZ_ACTION -> openBlitzRequests.trySend(Unit)
            else -> return
        }
        applicationContext.dismissDailyReminderNotification()
        intent.action = null
    }
}

