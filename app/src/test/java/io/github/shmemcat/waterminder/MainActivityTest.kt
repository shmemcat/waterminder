package io.github.shmemcat.waterminder

import android.content.Intent
import androidx.lifecycle.ViewModelProvider
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class MainActivityTest {
    @Test fun `app opens without enabling reminders automatically`() {
        Robolectric.buildActivity(MainActivity::class.java).setup().use { controller ->
            val model = ViewModelProvider(controller.get())[WaterminderViewModel::class.java]
            assertFalse(model.settings.enabled)
            assertEquals(0L, model.settings.nextAt)
            assertEquals(0, model.wateringEvent)
        }
    }
    @Test fun `notification drink action opens app and waters plant once`() {
        val intent = Intent(RuntimeEnvironment.getApplication(), MainActivity::class.java).setAction(MainActivity.DRANK_WATER)
        Robolectric.buildActivity(MainActivity::class.java, intent).setup().use { controller ->
            val model = ViewModelProvider(controller.get())[WaterminderViewModel::class.java]
            assertEquals(1, model.wateringEvent)
            controller.pause().resume()
            assertEquals(1, model.wateringEvent)
        }
    }
}
