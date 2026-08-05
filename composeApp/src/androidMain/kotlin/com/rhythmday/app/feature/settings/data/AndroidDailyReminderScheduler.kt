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
import android.graphics.Color
import android.graphics.drawable.Icon
import android.os.Build
import android.widget.RemoteViews
import com.rhythmday.app.MainActivity
import com.rhythmday.app.R
import com.rhythmday.app.feature.settings.domain.DailyReminderScheduler
import java.util.Calendar

const val OPEN_CREATE_ENTRY_ACTION = "com.rhythmday.app.action.OPEN_CREATE_ENTRY"
const val OPEN_BLITZ_ACTION = "com.rhythmday.app.action.OPEN_BLITZ"

class AndroidDailyReminderScheduler(context: Context) : DailyReminderScheduler {
    private val applicationContext = context.applicationContext
    private val alarmManager = applicationContext.getSystemService(AlarmManager::class.java)

    override fun schedule(hour: Int, minute: Int): Boolean {
        if (!alarmManager.canScheduleExactReminder()) return false

        val pendingIntent = applicationContext.reminderPendingIntent()
        return try {
            alarmManager.cancel(pendingIntent)
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                nextTriggerAtMillis(hour, minute),
                pendingIntent
            )
            true
        } catch (_: SecurityException) {
            false
        }
    }

    override fun cancel() {
        alarmManager.cancel(applicationContext.reminderPendingIntent())
    }
}

private fun AlarmManager.canScheduleExactReminder(): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.S || canScheduleExactAlarms()

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
    notificationManager.createReminderChannel()
    val appName = applicationInfo.loadLabel(packageManager).toString()

    val openCreateEntryPendingIntent = navigationPendingIntent(
        action = OPEN_CREATE_ENTRY_ACTION,
        requestCode = OPEN_CREATE_ENTRY_REQUEST_CODE
    )
    val openBlitzPendingIntent = navigationPendingIntent(
        action = OPEN_BLITZ_ACTION,
        requestCode = OPEN_BLITZ_REQUEST_CODE
    )
    val compactView = RemoteViews(packageName, R.layout.notification_daily_reminder_compact).apply {
        setTextViewText(R.id.notification_app_name, appName.uppercase())
        setOnClickPendingIntent(R.id.notification_content, openCreateEntryPendingIntent)
    }
    val expandedView = reminderChoiceView(
        layoutId = R.layout.notification_daily_reminder_expanded,
        appName = appName.uppercase(),
        openCreateEntryPendingIntent = openCreateEntryPendingIntent,
        openBlitzPendingIntent = openBlitzPendingIntent
    )
    val headsUpView = reminderChoiceView(
        layoutId = R.layout.notification_daily_reminder_heads_up,
        appName = "$appName · Время записать день",
        openCreateEntryPendingIntent = openCreateEntryPendingIntent,
        openBlitzPendingIntent = openBlitzPendingIntent
    )
    val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        Notification.Builder(this, REMINDER_CHANNEL_ID)
    } else {
        Notification.Builder(this)
    }
    builder
        .setSmallIcon(R.drawable.ic_notification)
        .setContentTitle(appName)
        .setContentText("Время записать день")
        .setContentIntent(openCreateEntryPendingIntent)
        .setAutoCancel(true)
        .setCategory(Notification.CATEGORY_REMINDER)
        .setColor(Color.rgb(21, 154, 140))
        .setPriority(Notification.PRIORITY_HIGH)
        .setVisibility(Notification.VISIBILITY_PUBLIC)
        .setShowWhen(true)
        .setWhen(System.currentTimeMillis())

    val notification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        builder
            .setCustomContentView(compactView)
            .setCustomBigContentView(expandedView)
            .setCustomHeadsUpContentView(headsUpView)
            .setStyle(Notification.DecoratedCustomViewStyle())
            .build()
    } else {
        val actionIcon = Icon.createWithResource(this, R.drawable.ic_notification)
        builder
            .addAction(
                Notification.Action.Builder(
                    actionIcon,
                    "Обычная запись",
                    openCreateEntryPendingIntent
                ).build()
            )
            .addAction(
                Notification.Action.Builder(
                    actionIcon,
                    "Блиц-опрос",
                    openBlitzPendingIntent
                ).build()
            )
            .build()
            .apply {
                contentView = compactView
                bigContentView = expandedView
                headsUpContentView = headsUpView
            }
    }

    notificationManager.notify(REMINDER_NOTIFICATION_ID, notification)
}

private fun NotificationManager.createReminderChannel() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

    createNotificationChannel(
        NotificationChannel(
            REMINDER_CHANNEL_ID,
            "Напоминания о записи дня",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Напоминание заполнить обычную запись или блиц-опрос"
            enableVibration(true)
            setShowBadge(true)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
    )
}

private fun Context.reminderChoiceView(
    layoutId: Int,
    appName: String,
    openCreateEntryPendingIntent: PendingIntent,
    openBlitzPendingIntent: PendingIntent
): RemoteViews = RemoteViews(packageName, layoutId).apply {
    setTextViewText(R.id.notification_app_name, appName)
    setOnClickPendingIntent(R.id.notification_content, openCreateEntryPendingIntent)
    setOnClickPendingIntent(R.id.notification_regular_button, openCreateEntryPendingIntent)
    setOnClickPendingIntent(R.id.notification_blitz_button, openBlitzPendingIntent)
}

private fun Context.navigationPendingIntent(action: String, requestCode: Int): PendingIntent {
    val intent = Intent(this, MainActivity::class.java).apply {
        this.action = action
        flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
    }
    return PendingIntent.getActivity(
        this,
        requestCode,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
}

internal fun Context.dismissDailyReminderNotification() {
    getSystemService(NotificationManager::class.java).cancel(REMINDER_NOTIFICATION_ID)
}

private const val REMINDER_ALARM_ACTION = "com.rhythmday.app.action.DAILY_REMINDER"
private const val REMINDER_CHANNEL_ID = "daily_entry_reminder_v2"
private const val REMINDER_ALARM_REQUEST_CODE = 4101
private const val OPEN_CREATE_ENTRY_REQUEST_CODE = 4102
private const val REMINDER_NOTIFICATION_ID = 4103
private const val OPEN_BLITZ_REQUEST_CODE = 4104
