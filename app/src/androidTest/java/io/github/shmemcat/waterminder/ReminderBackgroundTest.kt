package io.github.shmemcat.waterminder

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.shmemcat.waterminder.reminders.ReminderNotifications
import io.github.shmemcat.waterminder.reminders.ReminderPolicy
import io.github.shmemcat.waterminder.reminders.ReminderScheduler
import io.github.shmemcat.waterminder.reminders.SettingsStore
import org.junit.Assert.assertEquals
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import java.time.ZoneId

/** Device smoke test: schedule a nudge, finish instrumentation, then verify delivery after
 * closing the background process. The receiver uses the user's normal interval afterward.
 * This deliberately sends one real notification without changing reminder preferences.
 */
@RunWith(AndroidJUnit4::class)
class ReminderBackgroundTest {
    @Test fun scheduleNudgeForBackgroundDelivery() {
        val context: Context = InstrumentationRegistry.getInstrumentation().targetContext
        val original = SettingsStore(context).read()
        assumeTrue(original.enabled)
        assumeTrue(ReminderScheduler.canSchedulePrecisely(context))
        assumeTrue(ReminderNotifications.canNotify(context))
        assumeTrue(!ReminderPolicy.isQuiet(Instant.now(), original, ZoneId.systemDefault()))
        val target = Instant.now().plusSeconds(20)
        assumeTrue(!ReminderPolicy.isQuiet(target, original, ZoneId.systemDefault()))
        val scheduled = ReminderScheduler.schedule(context, original, target)
        assertEquals(target.toEpochMilli(), scheduled.nextAt)
        assertEquals(original.copy(nextAt = scheduled.nextAt), scheduled)
    }
}
