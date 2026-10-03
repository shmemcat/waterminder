package io.github.shmemcat.waterminder.reminders

import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId

data class ReminderSettings(
    val enabled: Boolean = false,
    val intervalMinutes: Int = 120,
    val quietEnabled: Boolean = true,
    val quietStart: Int = 22 * 60,
    val quietEnd: Int = 7 * 60,
    val wakeScreen: Boolean = true,
    val nextAt: Long = 0L,
)

/** Quiet hours are local wall time, inclusive at the start and exclusive at the end. */
object ReminderPolicy {
    fun isQuiet(at: Instant, settings: ReminderSettings, zone: ZoneId): Boolean {
        if (!settings.quietEnabled) return false
        require(settings.quietStart != settings.quietEnd)
        val time = at.atZone(zone).toLocalTime()
        val start = LocalTime.ofSecondOfDay(settings.quietStart * 60L)
        val end = LocalTime.ofSecondOfDay(settings.quietEnd * 60L)
        return if (start < end) time >= start && time < end else time >= start || time < end
    }

    fun nextAllowed(candidate: Instant, settings: ReminderSettings, zone: ZoneId): Instant {
        if (!isQuiet(candidate, settings, zone)) return candidate
        val local = candidate.atZone(zone)
        val start = LocalTime.ofSecondOfDay(settings.quietStart * 60L)
        val end = LocalTime.ofSecondOfDay(settings.quietEnd * 60L)
        val endDate = if (start > end && local.toLocalTime() >= start) {
            local.toLocalDate().plusDays(1)
        } else local.toLocalDate()
        // If the clock repeats an hour, keep quiet until the second occurrence of the end.
        return endDate.atTime(end).atZone(zone).withLaterOffsetAtOverlap().toInstant()
    }

    fun afterInterval(now: Instant, settings: ReminderSettings, zone: ZoneId): Instant =
        nextAllowed(now.plusSeconds(settings.intervalMinutes * 60L), settings, zone)
}

