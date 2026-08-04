package com.rhythmday.app.feature.settings.data

import android.content.Context
import com.rhythmday.app.feature.settings.domain.DEFAULT_REMINDER_HOUR
import com.rhythmday.app.feature.settings.domain.DEFAULT_REMINDER_MINUTE
import com.rhythmday.app.feature.settings.domain.ReminderSettings
import com.rhythmday.app.feature.settings.domain.ReminderSettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class AndroidReminderSettingsRepository(context: Context) : ReminderSettingsRepository {
    private val preferences = context.applicationContext.getSharedPreferences(
        REMINDER_PREFERENCES_NAME,
        Context.MODE_PRIVATE
    )
    private val mutableSettings = MutableStateFlow(preferences.readReminderSettings())

    override val settings: StateFlow<ReminderSettings> = mutableSettings

    override suspend fun save(settings: ReminderSettings) {
        preferences.edit()
            .putBoolean(REMINDER_ENABLED_KEY, settings.enabled)
            .putInt(REMINDER_HOUR_KEY, settings.hour)
            .putInt(REMINDER_MINUTE_KEY, settings.minute)
            .apply()
        mutableSettings.value = settings
    }
}

internal fun Context.readReminderSettings(): ReminderSettings =
    getSharedPreferences(REMINDER_PREFERENCES_NAME, Context.MODE_PRIVATE).readReminderSettings()

private fun android.content.SharedPreferences.readReminderSettings(): ReminderSettings =
    ReminderSettings(
        enabled = getBoolean(REMINDER_ENABLED_KEY, false),
        hour = getInt(REMINDER_HOUR_KEY, DEFAULT_REMINDER_HOUR),
        minute = getInt(REMINDER_MINUTE_KEY, DEFAULT_REMINDER_MINUTE)
    )

private const val REMINDER_PREFERENCES_NAME = "daily_reminder_settings"
private const val REMINDER_ENABLED_KEY = "enabled"
private const val REMINDER_HOUR_KEY = "hour"
private const val REMINDER_MINUTE_KEY = "minute"
