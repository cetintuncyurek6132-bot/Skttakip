package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("SKT Takip V2", appName)
  }

  @Test
  fun `launch MainActivity successfully`() {
    val controller = Robolectric.buildActivity(MainActivity::class.java).setup()
    val activity = controller.get()
    assertNotNull(activity)
  }

  @get:org.junit.Rule
  val composeTestRule = androidx.compose.ui.test.junit4.createAndroidComposeRule<MainActivity>()

  @Test
  fun `navigate from dashboard to other screens`() {
    composeTestRule.waitForIdle()
    // Click on stat_card_total (Takipte -> Products)
    composeTestRule.onNodeWithTag("stat_card_total").performClick()
    composeTestRule.waitForIdle()

    // Now on products screen, click on BottomNav ANA SAYFA
    composeTestRule.onNodeWithText("ANA SAYFA").performClick()
    composeTestRule.waitForIdle()

    // Click on Adetsel quick action
    composeTestRule.onNodeWithTag("quick_action_adetsel").performClick()
    composeTestRule.waitForIdle()

    // Back to ANA SAYFA
    composeTestRule.onNodeWithText("ANA SAYFA").performClick()
    composeTestRule.waitForIdle()

    // Click on Takip quick action
    composeTestRule.onNodeWithTag("quick_action_takip").performClick()
    composeTestRule.waitForIdle()
  }
}

