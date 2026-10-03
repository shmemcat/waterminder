package io.github.shmemcat.waterminder

import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.semantics.SemanticsActions
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], qualifiers = "w411dp-h891dp")
class WateringNavigationTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun `returning from settings does not replay a drink but another drink still waters`() {
        compose.mainClock.autoAdvance = false
        compose.onNodeWithContentDescription("A smiling little plant in a peach pot").assertExists()

        drinkAndCheckAnimation()
        advanceTime(5_000)
        compose.onNodeWithContentDescription("A smiling little plant in a peach pot").assertExists()

        repeat(2) { visit ->
            compose.onNodeWithContentDescription("Reminder settings").performSemanticsAction(SemanticsActions.OnClick) { it() }
            advanceTime(100)
            if (visit == 0) {
                compose.onNodeWithContentDescription("Back to plant").performSemanticsAction(SemanticsActions.OnClick) { it() }
            } else {
                compose.runOnUiThread { compose.activity.onBackPressedDispatcher.onBackPressed() }
            }
            advanceTime(400)
            compose.onNodeWithContentDescription("A smiling little plant in a peach pot").assertExists()
            compose.onNodeWithText("a little happier already").assertDoesNotExist()
        }

        drinkAndCheckAnimation()
    }

    @Test fun `leaving during watering does not restart it when returning`() {
        compose.mainClock.autoAdvance = false
        drinkAndCheckAnimation()
        compose.onNodeWithContentDescription("Reminder settings").performSemanticsAction(SemanticsActions.OnClick) { it() }
        advanceTime(100)
        compose.onNodeWithContentDescription("Back to plant").performSemanticsAction(SemanticsActions.OnClick) { it() }
        advanceTime(400)
        compose.onNodeWithContentDescription("A smiling little plant in a peach pot").assertExists()
        compose.onNodeWithText("a little happier already").assertDoesNotExist()
    }

    private fun advanceTime(milliseconds: Long) {
        compose.waitForIdle()
        compose.mainClock.advanceTimeBy(milliseconds)
        compose.waitForIdle()
    }

    private fun drinkAndCheckAnimation() {
        compose.onNodeWithText("I drank water").performScrollTo().performSemanticsAction(SemanticsActions.OnClick) { it() }
        advanceTime(400)
        compose.onNodeWithContentDescription("A happy little plant being watered").assertExists()
    }
}
