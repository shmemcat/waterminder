package io.github.shmemcat.waterminder.reminders

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Looper
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowAlarmManager
import java.time.Instant
import java.time.Duration
import java.time.LocalTime

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class ReminderDeliveryTest {
    private lateinit var context: Context
    private val active = ReminderSettings(enabled = true, quietEnabled = false, wakeScreen = false)
    @Before fun setup() {
        context = RuntimeEnvironment.getApplication()
        context.getSharedPreferences("reminders", Context.MODE_PRIVATE).edit().clear().commit()
        ReminderNotifications.createChannel(context)
    }
    @Test fun `one pending reminder is replaced when interval changes`() {
        ReminderScheduler.schedule(context, active)
        val changed = ReminderScheduler.schedule(context, active.copy(intervalMinutes = 60))
        val alarms = shadowOf(context.getSystemService(AlarmManager::class.java)).scheduledAlarms
        assertEquals(1, alarms.size)
        assertEquals(changed.nextAt, alarms.single().triggerAtTime)
        assertEquals(changed, SettingsStore(context).read())
    }
    @Test fun `pausing cancels pending alarm`() {
        ReminderScheduler.schedule(context, active)
        val paused = ReminderScheduler.schedule(context, active.copy(enabled = false))
        assertTrue(shadowOf(context.getSystemService(AlarmManager::class.java)).scheduledAlarms.isEmpty())
        assertEquals(0L, paused.nextAt)
    }
    @Test fun `blocked notification channel prevents scheduling`() {
        val manager = context.getSystemService(NotificationManager::class.java)
        shadowOf(manager).setNotificationsEnabled(false)
        val settings = ReminderScheduler.schedule(context, active)
        assertEquals(0L, settings.nextAt)
        assertTrue(shadowOf(context.getSystemService(AlarmManager::class.java)).scheduledAlarms.isEmpty())
    }
    @Test fun `drinking dismisses notification without postponing the next reminder or keeping a log`() {
        val original = ReminderScheduler.schedule(context, active, Instant.now().plusSeconds(900))
        assertTrue(ReminderNotifications.show(context, active))
        val drank = ReminderScheduler.drink(context)
        assertEquals(original.nextAt, drank.nextAt)
        assertEquals(original.nextAt, shadowOf(context.getSystemService(AlarmManager::class.java)).scheduledAlarms.single().triggerAtTime)
        assertEquals(0, shadowOf(context.getSystemService(NotificationManager::class.java)).size())
        assertFalse(context.getSharedPreferences("reminders", Context.MODE_PRIVATE).all.keys.any { "drink" in it || "count" in it })
    }
    @Test fun `delivery ignores an old alarm after rescheduling`() {
        val scheduled = ReminderScheduler.schedule(context, active)
        ReminderReceiver().onReceive(context, Intent().setAction(ReminderReceiver.REMIND))
        assertEquals(0, shadowOf(context.getSystemService(NotificationManager::class.java)).size())
        assertEquals(scheduled.nextAt, SettingsStore(context).read().nextAt)
    }
    @Test fun `due delivery posts notification and schedules only next reminder`() {
        SettingsStore(context).write(active.copy(nextAt = Instant.now().minusSeconds(60).toEpochMilli()))
        ReminderReceiver().onReceive(context, Intent().setAction(ReminderReceiver.REMIND))
        assertEquals(1, shadowOf(context.getSystemService(NotificationManager::class.java)).size())
        assertEquals(1, shadowOf(context.getSystemService(AlarmManager::class.java)).scheduledAlarms.size)
        assertTrue(SettingsStore(context).read().nextAt > Instant.now().toEpochMilli())
    }
    @Test fun `dismissing or ignoring a notification leaves the next hourly reminder scheduled`() {
        val hourly = active.copy(intervalMinutes = 60, nextAt = Instant.now().minusSeconds(1).toEpochMilli())
        SettingsStore(context).write(hourly)
        val before = Instant.now()
        ReminderReceiver().onReceive(context, Intent().setAction(ReminderReceiver.REMIND))
        val next = SettingsStore(context).read().nextAt
        assertTrue(next >= before.plusSeconds(3600).toEpochMilli())
        assertTrue(next <= Instant.now().plusSeconds(3600).toEpochMilli())
        context.getSystemService(NotificationManager::class.java).cancelAll()
        assertEquals(next, SettingsStore(context).read().nextAt)
        assertEquals(next, shadowOf(context.getSystemService(AlarmManager::class.java)).scheduledAlarms.single().triggerAtTime)
    }
    @Test fun `restore preserves future reminder`() {
        val scheduled = ReminderScheduler.schedule(context, active)
        assertEquals(scheduled.nextAt, ReminderScheduler.restore(context).nextAt)
    }
    @Test @Config(sdk = [31]) fun `allowed precise reminders use an idle broadcast alarm without an activity`() {
        ShadowAlarmManager.setCanScheduleExactAlarms(true)
        val scheduled = ReminderScheduler.schedule(context, active)
        val alarm = shadowOf(context.getSystemService(AlarmManager::class.java)).scheduledAlarms.single()
        assertEquals(0L, alarm.windowLengthMs)
        assertTrue(alarm.allowWhileIdle)
        assertEquals(scheduled.nextAt, alarm.triggerAtTime)
        assertEquals(ReminderReceiver.REMIND, shadowOf(alarm.operation).savedIntent.action)
        assertEquals(ReminderReceiver::class.java.name, shadowOf(alarm.operation).savedIntent.component?.className)
    }
    @Test @Config(sdk = [31]) fun `missing precise access keeps an approximate idle reminder`() {
        ShadowAlarmManager.setCanScheduleExactAlarms(false)
        val scheduled = ReminderScheduler.schedule(context, active)
        val alarm = shadowOf(context.getSystemService(AlarmManager::class.java)).scheduledAlarms.single()
        assertFalse(ReminderScheduler.canSchedulePrecisely(context))
        assertEquals(ShadowAlarmManager.WINDOW_HEURISTIC, alarm.windowLengthMs)
        assertTrue(alarm.allowWhileIdle)
        assertEquals(scheduled.nextAt, alarm.triggerAtTime)
    }
    @Test @Config(sdk = [31]) fun `granting precise access upgrades the existing reminder without restarting it`() {
        ShadowAlarmManager.setCanScheduleExactAlarms(false)
        val original = ReminderScheduler.schedule(context, active)
        ShadowAlarmManager.setCanScheduleExactAlarms(true)
        RestoreReceiver().onReceive(context, Intent(AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED))
        val alarm = shadowOf(context.getSystemService(AlarmManager::class.java)).scheduledAlarms.single()
        assertEquals(original, SettingsStore(context).read())
        assertEquals(original.nextAt, alarm.triggerAtTime)
        assertEquals(0L, alarm.windowLengthMs)
    }
    @Test @Config(sdk = [31]) fun `restoring after precise access is revoked uses the fallback`() {
        ShadowAlarmManager.setCanScheduleExactAlarms(true)
        val original = ReminderScheduler.schedule(context, active)
        ShadowAlarmManager.setCanScheduleExactAlarms(false)
        assertEquals(original, ReminderScheduler.restore(context))
        val alarm = shadowOf(context.getSystemService(AlarmManager::class.java)).scheduledAlarms.single()
        assertEquals(ShadowAlarmManager.WINDOW_HEURISTIC, alarm.windowLengthMs)
    }
    @Test fun `equal quiet hours cannot leak from stored preferences`() {
        SettingsStore(context).write(active.copy(quietEnabled = true, quietStart = 420, quietEnd = 420))
        assertFalse(SettingsStore(context).read().quietEnabled)
    }
    @Test fun `delivery delayed into quiet hours waits until quiet end`() {
        val minutes = LocalTime.now().hour * 60 + LocalTime.now().minute
        val quiet = active.copy(quietEnabled = true, quietStart = (minutes + 1439) % 1440, quietEnd = (minutes + 2) % 1440, nextAt = Instant.now().minusSeconds(1).toEpochMilli())
        SettingsStore(context).write(quiet)
        ReminderReceiver().onReceive(context, Intent().setAction(ReminderReceiver.REMIND))
        assertEquals(0, shadowOf(context.getSystemService(NotificationManager::class.java)).size())
        assertEquals(ReminderPolicy.nextAllowed(Instant.now(), quiet, java.time.ZoneId.systemDefault()).toEpochMilli(), SettingsStore(context).read().nextAt)
    }
    @Test fun `test nudge waits ten seconds so the screen can be locked`() {
        SettingsStore(context).write(active)
        assertTrue(ReminderNotifications.queueTest(context))
        assertEquals(0, shadowOf(context.getSystemService(NotificationManager::class.java)).size())
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(10))
        assertEquals(1, shadowOf(context.getSystemService(NotificationManager::class.java)).size())
    }
    @Test @Config(sdk = [33]) fun `missing Android notification permission blocks reminders`() {
        shadowOf(RuntimeEnvironment.getApplication()).denyPermissions(Manifest.permission.POST_NOTIFICATIONS)
        assertFalse(ReminderNotifications.canNotify(context))
        assertEquals(0L, ReminderScheduler.schedule(context, active).nextAt)
    }
}
