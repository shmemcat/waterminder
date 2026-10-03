package io.github.shmemcat.waterminder

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import io.github.shmemcat.waterminder.reminders.ReminderNotifications
import io.github.shmemcat.waterminder.reminders.ReminderScheduler
import io.github.shmemcat.waterminder.reminders.ReminderSettings
import io.github.shmemcat.waterminder.reminders.SettingsStore
import java.time.Instant

class WaterminderViewModel(application: Application) : AndroidViewModel(application) {
    private val context = application
    var settings by mutableStateOf(SettingsStore(context).read())
        private set
    var notificationsAllowed by mutableStateOf(false)
        private set
    var preciseRemindersAllowed by mutableStateOf(false)
        private set
    var wateringEvent by mutableIntStateOf(0)
        private set
    private var handledWateringEvent = 0

    init { ReminderNotifications.createChannel(context); refresh() }

    fun refresh() {
        notificationsAllowed = ReminderNotifications.canNotify(context)
        preciseRemindersAllowed = ReminderScheduler.canSchedulePrecisely(context)
        settings = ReminderScheduler.restore(context)
    }
    fun update(value: ReminderSettings) {
        ReminderNotifications.dismiss(context)
        val onlyWakeChanged = value.copy(wakeScreen = settings.wakeScreen) == settings
        val existing = if (onlyWakeChanged && settings.nextAt > 0) Instant.ofEpochMilli(settings.nextAt) else null
        settings = ReminderScheduler.schedule(context, value, existing)
    }
    fun drink() {
        settings = ReminderScheduler.drink(context)
        wateringEvent++
    }
    // A drink is handled once, even if the home screen is recreated after settings.
    fun consumeWateringEvent(): Boolean {
        if (wateringEvent == handledWateringEvent) return false
        handledWateringEvent = wateringEvent
        return true
    }
    fun testReminder(): Boolean = ReminderNotifications.queueTest(context)
}
