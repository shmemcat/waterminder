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
    @Test fun `drinking dismisses notification and restarts interval without a log`() {
        SettingsStore(context).write(active)
        assertTrue(ReminderNotifications.show(context, active))
        val drank = ReminderScheduler.drink(context)
        assertTrue(drank.nextAt >= Instant.now().minusSeconds(2).plusSeconds(7200).toEpochMilli())
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
    @Test fun `restore preserves future reminder`() {
        val scheduled = ReminderScheduler.schedule(context, active)
        assertEquals(scheduled.nextAt, ReminderScheduler.restore(context).nextAt)
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
