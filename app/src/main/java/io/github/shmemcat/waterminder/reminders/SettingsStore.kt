package io.github.shmemcat.waterminder.reminders

import android.content.Context

/** Only reminder preferences and one pending reminder time; no drink log or history. */
class SettingsStore(context: Context) {
    private val prefs = context.getSharedPreferences("reminders", Context.MODE_PRIVATE)
    fun read(): ReminderSettings {
        val start = prefs.getInt("quietStart", 1320).coerceIn(0, 1439)
        val end = prefs.getInt("quietEnd", 420).coerceIn(0, 1439)
        return ReminderSettings(
            enabled = prefs.getBoolean("enabled", false),
            intervalMinutes = prefs.getInt("intervalMinutes", 120).coerceIn(30, 480),
            quietEnabled = prefs.getBoolean("quietEnabled", true) && start != end,
            quietStart = start,
            quietEnd = end,
            wakeScreen = prefs.getBoolean("wakeScreen", true),
            nextAt = prefs.getLong("nextAt", 0L),
        )
    }
    fun write(settings: ReminderSettings) {
        // Receivers may be terminated after returning; finish disk persistence first.
        check(prefs.edit()
            .putBoolean("enabled", settings.enabled)
            .putInt("intervalMinutes", settings.intervalMinutes)
            .putBoolean("quietEnabled", settings.quietEnabled)
            .putInt("quietStart", settings.quietStart)
            .putInt("quietEnd", settings.quietEnd)
            .putBoolean("wakeScreen", settings.wakeScreen)
            .putLong("nextAt", settings.nextAt)
            .commit())
    }
}
