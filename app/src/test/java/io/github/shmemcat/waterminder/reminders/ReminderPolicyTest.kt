package io.github.shmemcat.waterminder.reminders

import org.junit.Assert.*
import org.junit.Test
import java.time.Instant
import java.time.ZoneId

class ReminderPolicyTest {
    private val utc = ZoneId.of("UTC")
    private val overnight = ReminderSettings()
    private fun at(value: String) = Instant.parse(value)

    @Test fun `overnight quiet start is inclusive and end is exclusive`() {
        assertFalse(ReminderPolicy.isQuiet(at("2026-10-02T21:59:59Z"), overnight, utc))
        assertTrue(ReminderPolicy.isQuiet(at("2026-10-02T22:00:00Z"), overnight, utc))
        assertTrue(ReminderPolicy.isQuiet(at("2026-10-03T06:59:59Z"), overnight, utc))
        assertFalse(ReminderPolicy.isQuiet(at("2026-10-03T07:00:00Z"), overnight, utc))
    }
    @Test fun `evening reminder moves to next morning`() {
        assertEquals(at("2026-10-03T07:00:00Z"), ReminderPolicy.afterInterval(at("2026-10-02T21:00:00Z"), overnight, utc))
    }
    @Test fun `early morning reminder moves to same morning`() {
        assertEquals(at("2026-10-03T07:00:00Z"), ReminderPolicy.nextAllowed(at("2026-10-03T02:00:00Z"), overnight, utc))
    }
    @Test fun `daytime quiet interval keeps other hours active`() {
        val daytime = overnight.copy(quietStart = 12 * 60, quietEnd = 14 * 60)
        assertEquals(at("2026-10-02T14:00:00Z"), ReminderPolicy.nextAllowed(at("2026-10-02T12:30:00Z"), daytime, utc))
        assertEquals(at("2026-10-02T19:00:00Z"), ReminderPolicy.nextAllowed(at("2026-10-02T19:00:00Z"), daytime, utc))
    }
    @Test fun `disabled quiet hours leaves reminder alone`() {
        val candidate = at("2026-10-02T23:00:00Z")
        assertEquals(candidate, ReminderPolicy.nextAllowed(candidate, overnight.copy(quietEnabled = false), utc))
    }
    @Test fun `interval can contain minutes`() {
        assertEquals(at("2026-10-02T11:30:00Z"), ReminderPolicy.afterInterval(at("2026-10-02T10:00:00Z"), overnight.copy(intervalMinutes = 90), utc))
    }
    @Test fun `quiet end in skipped DST hour resolves forward`() {
        val ny = ZoneId.of("America/New_York")
        val settings = overnight.copy(quietEnd = 150)
        assertEquals(at("2026-03-08T07:30:00Z"), ReminderPolicy.nextAllowed(at("2026-03-08T06:15:00Z"), settings, ny))
    }
    @Test fun `quiet end in repeated DST hour uses later occurrence`() {
        val ny = ZoneId.of("America/New_York")
        val settings = overnight.copy(quietEnd = 90)
        assertEquals(at("2026-11-01T06:30:00Z"), ReminderPolicy.nextAllowed(at("2026-11-01T06:15:00Z"), settings, ny))
    }
    @Test fun `quiet hours follow local timezone`() {
        val phoenix = ZoneId.of("America/Phoenix")
        assertEquals(at("2026-10-03T14:00:00Z"), ReminderPolicy.nextAllowed(at("2026-10-03T05:00:00Z"), overnight, phoenix))
    }
    @Test(expected = IllegalArgumentException::class) fun `equal quiet start and end is invalid`() {
        ReminderPolicy.isQuiet(at("2026-10-02T10:00:00Z"), overnight.copy(quietStart = 420), utc)
    }
}

