package io.github.shmemcat.waterminder.reminders

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import java.time.Instant
import java.time.ZoneId

object ReminderScheduler {
    fun canSchedulePrecisely(context: Context): Boolean =
        Build.VERSION.SDK_INT < 31 || context.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()

    private fun pendingIntent(context: Context): PendingIntent = PendingIntent.getBroadcast(
        context, 1, Intent(context, ReminderReceiver::class.java).setAction(ReminderReceiver.REMIND),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    fun cancel(context: Context) {
        context.getSystemService(AlarmManager::class.java).cancel(pendingIntent(context))
    }

    fun schedule(context: Context, settings: ReminderSettings, candidate: Instant? = null): ReminderSettings {
        cancel(context)
        if (!settings.enabled || !ReminderNotifications.canNotify(context)) {
            return settings.copy(nextAt = 0).also { SettingsStore(context).write(it) }
        }
        val now = Instant.now()
        val target = candidate ?: ReminderPolicy.afterInterval(now, settings, ZoneId.systemDefault())
        val future = maxOf(target, now.plusSeconds(1))
        val next = ReminderPolicy.nextAllowed(future, settings, ZoneId.systemDefault())
        val scheduled = settings.copy(nextAt = next.toEpochMilli())
        SettingsStore(context).write(scheduled)
        val manager = context.getSystemService(AlarmManager::class.java)
        val alarm = pendingIntent(context)
        if (canSchedulePrecisely(context)) {
            try {
                manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, scheduled.nextAt, alarm)
                return scheduled
            } catch (_: SecurityException) {
                // Access can be revoked between checking and scheduling; keep an approximate reminder.
            }
        }
        manager.setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP, scheduled.nextAt, alarm,
        )
        return scheduled
    }

    fun restore(context: Context, resetInterval: Boolean = false): ReminderSettings {
        val settings = SettingsStore(context).read()
        val candidate = if (settings.nextAt > 0 && !resetInterval) Instant.ofEpochMilli(settings.nextAt) else null
        return schedule(context, settings, candidate)
    }

    fun drink(context: Context): ReminderSettings {
        ReminderNotifications.dismiss(context)
        // Drinking is an acknowledgment, not a snooze; keep the next automatic reminder.
        return restore(context)
    }
}
