package io.github.shmemcat.waterminder.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.app.AlarmManager
import java.time.Instant
import java.time.ZoneId

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != REMIND) return
        val settings = SettingsStore(context).read()
        if (!settings.enabled) { ReminderScheduler.cancel(context); return }
        val now = Instant.now()
        // Ignore a canceled/replaced alarm that was already being delivered.
        if (settings.nextAt <= 0L || now.toEpochMilli() < settings.nextAt) return
        if (!ReminderPolicy.isQuiet(now, settings, ZoneId.systemDefault())) {
            ReminderNotifications.show(context, settings)
        }
        // If Android delayed delivery into quiet hours, skip it and resume at quiet end.
        val next = if (ReminderPolicy.isQuiet(now, settings, ZoneId.systemDefault())) {
            ReminderPolicy.nextAllowed(now, settings, ZoneId.systemDefault())
        } else ReminderPolicy.afterInterval(now, settings, ZoneId.systemDefault())
        ReminderScheduler.schedule(context, settings, next)
    }
    companion object { const val REMIND = "io.github.shmemcat.waterminder.REMIND" }
}

class RestoreReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in ACTIONS) return
        ReminderNotifications.createChannel(context)
        ReminderScheduler.restore(context, resetInterval = intent.action == Intent.ACTION_TIME_CHANGED)
    }
    companion object {
        private val ACTIONS = setOf(Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_MY_PACKAGE_REPLACED, Intent.ACTION_TIME_CHANGED, Intent.ACTION_TIMEZONE_CHANGED, AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED)
    }
}
