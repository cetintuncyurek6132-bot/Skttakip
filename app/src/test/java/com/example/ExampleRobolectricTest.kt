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
    composeTestRule.mainClock.advanceTimeBy(2500)
    composeTestRule.waitForIdle()

    // Navigate to Ürünler
    composeTestRule.onNodeWithText("Ürünler").performClick()
    composeTestRule.waitForIdle()

    // Back to Ana Sayfa
    composeTestRule.onNodeWithText("Ana Sayfa").performClick()
    composeTestRule.waitForIdle()

    // Navigate to İade Takip
    composeTestRule.onNodeWithText("İade Takip").performClick()
    composeTestRule.waitForIdle()

    // Back to Ana Sayfa
    composeTestRule.onNodeWithText("Ana Sayfa").performClick()
    composeTestRule.waitForIdle()
  }

  @Test
  fun `verify clean WhatsApp share bitmaps generation`() {
    val sampleProducts = (1..30).map { i ->
      com.example.data.Product(
        id = i,
        barkod = "8690000000$i",
        urunKodu = "KOD$i",
        urunAdi = "Ürün Adı $i",
        kategori = "Gıda",
        sktTarihi = System.currentTimeMillis() + (i * 86400000L),
        stokAdedi = i,
        isImportant = i % 2 == 0
      )
    }

    val bitmaps = com.example.util.ProductImageGenerator.createCleanWhatsAppShareBitmaps(sampleProducts)
    // 30 products should be chunked into 2 pages (20 + 10)
    assertEquals(2, bitmaps.size)
    assertNotNull(bitmaps[0])
    assertNotNull(bitmaps[1])
    assertEquals(1080, bitmaps[0].width)
    assertEquals(1080, bitmaps[1].width)

    // Formula: (pageProducts.size * 150) + (24 * 2)
    val expectedHeightPage1 = (20 * 150) + (24 * 2) // 3048
    val expectedHeightPage2 = (10 * 150) + (24 * 2) // 1548
    assertEquals(expectedHeightPage1, bitmaps[0].height)
    assertEquals(expectedHeightPage2, bitmaps[1].height)
  }
}

