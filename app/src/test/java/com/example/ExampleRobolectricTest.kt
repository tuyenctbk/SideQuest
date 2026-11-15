package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("SideQuest", appName)
  }

  @Test
  fun `verify 20 locales load formatted and non-formatted strings cleanly`() {
    val baseContext = ApplicationProvider.getApplicationContext<Context>()
    val localeTags = listOf(
      "en", "es", "de", "fr", "it", "pt", "nl", "pl", "sv", "tr",
      "ru", "ar", "hi", "id", "th", "vi", "ja", "ko", "zh-CN", "zh-TW"
    )
    for (tag in localeTags) {
      val locale = java.util.Locale.forLanguageTag(tag)
      val config = android.content.res.Configuration(baseContext.resources.configuration)
      config.setLocale(locale)
      val localizedContext = baseContext.createConfigurationContext(config)

      val nudge = localizedContext.getString(R.string.quota_soft_nudge_format, 80, 5, "4h 12m")
      val expectedPct = String.format(locale, "%1\$d%%", 80)
      org.junit.Assert.assertTrue("Expected $expectedPct in $tag: $nudge", nudge.contains(expectedPct))
      org.junit.Assert.assertTrue("Expected 4h 12m in $tag: $nudge", nudge.contains("4h 12m"))
      org.junit.Assert.assertFalse("Unexpected double %% in $tag: $nudge", nudge.contains("%%"))

      val adFree = localizedContext.getString(R.string.support_adfree_desc)
      org.junit.Assert.assertTrue("Expected 100% in $tag: $adFree", adFree.contains("100%"))
      org.junit.Assert.assertFalse("Unexpected 100%% in $tag: $adFree", adFree.contains("100%%"))

      val perk = localizedContext.getString(R.string.rank_pathfinder_perk)
      org.junit.Assert.assertTrue("Expected +10% in $tag: $perk", perk.contains("+10%"))
    }
  }
}
