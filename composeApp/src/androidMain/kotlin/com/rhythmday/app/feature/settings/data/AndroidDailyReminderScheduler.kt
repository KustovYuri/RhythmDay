package com.rhythmday.app.feature.settings.data

import android.Manifest
import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import com.rhythmday.app.MainActivity
import com.rhythmday.app.R
import com.rhythmday.app.feature.settings.domain.DailyReminderScheduler
import java.util.Calendar

const val OPEN_CREATE_ENTRY_ACTION = "com.rhythmday.app.action.OPEN_CREATE_ENTRY"

class AndroidDailyReminderScheduler(context: Context) : DailyReminderScheduler {
    private val applicationContext = context.applicationContext
    private val alarmManager = applicationContext.getSystemService(AlarmManager::class.java)

    override fun schedule(hour: Int, minute: Int) {
        alarmManager.setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            nextTriggerAtMillis(hour, minute),
            applicationContext.reminderPendingIntent()
        )
    }

    override fun cancel() {
        alarmManager.cancel(applicationContext.reminderPendingIntent())
    }
}

class DailyReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val settings = context.readReminderSettings()
        val scheduler = AndroidDailyReminderScheduler(context)

        if (!settings.enabled) {
            scheduler.cancel()
            return
        }

        if (intent.action == REMINDER_ALARM_ACTION) {
            context.showReminderNotification()
        }
        scheduler.schedule(settings.hour, settings.minute)
    }
}

private fun nextTriggerAtMillis(hour: Int, minute: Int): Long {
    val now = Calendar.getInstance()
    return Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, hour)
        set(Calendar.MINUTE, minute)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
        if (!after(now)) add(Calendar.DAY_OF_YEAR, 1)
    }.timeInMillis
}

private fun Context.reminderPendingIntent(): PendingIntent {
    val intent = Intent(this, DailyReminderReceiver::class.java).apply {
        action = REMINDER_ALARM_ACTION
    }
    return PendingIntent.getBroadcast(
        this,
        REMINDER_ALARM_REQUEST_CODE,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
}

private fun Context.showReminderNotification() {
    if (
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
        checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
    ) {
        return
    }

    val notificationManager = getSystemService(NotificationManager::class.java)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        notificationManager.createNotificationChannel(
            NotificationChannel(
                REMINDER_CHANNEL_ID,
                "Ежедневные напоминания",
                NotificationManager.IMPORTANCE_DEFAULT
            )
        )
    }

    val openAppIntent = Intent(this, MainActivity::class.java).apply {
        action = OPEN_CREATE_ENTRY_ACTION
        flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
    }
    val openAppPendingIntent = PendingIntent.getActivity(
        this,
        OPEN_APP_REQUEST_CODE,
        openAppIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        Notification.Builder(this, REMINDER_CHANNEL_ID)
    } else {
        Notification.Builder(this)
    }

    notificationManager.notify(
        REMINDER_NOTIFICATION_ID,
        builder
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Время записать день")
            .setContentText("Как прошёл ваш день?")
            .setContentIntent(openAppPendingIntent)
            .setAutoCancel(true)
            .setCategory(Notification.CATEGORY_REMINDER)
            .build()
    )
}

private const val REMINDER_ALARM_ACTION = "com.rhythmday.app.action.DAILY_REMINDER"
private const val REMINDER_CHANNEL_ID = "daily_entry_reminder"
private const val REMINDER_ALARM_REQUEST_CODE = 4101
private const val OPEN_APP_REQUEST_CODE = 4102
private const val REMINDER_NOTIFICATION_ID = 4103
