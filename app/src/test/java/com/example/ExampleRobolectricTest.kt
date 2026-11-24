package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.touchflow.models.ActionItem
import com.example.touchflow.models.AppShortcut
import com.example.touchflow.models.MenuLayoutType
import com.example.touchflow.models.OverlayTheme
import com.example.touchflow.preferences.TouchFlowPreferences
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun readAppNameFromContext() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("TouchFlow", appName)
    }

    @Test
    fun preferencesPersistConfigurationAndShortcuts() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = TouchFlowPreferences(context)
        prefs.setButtonSizeDp(64)
        prefs.setOpacityPercent(80)
        prefs.setOverlayTheme(OverlayTheme.AMOLED_BLACK)
        prefs.setMenuLayout(MenuLayoutType.CIRCULAR)
        prefs.setDoubleTapAction(ActionItem.SCREENSHOT)
        prefs.saveShortcuts(listOf(AppShortcut("com.android.settings", "Settings")))

        val config = prefs.loadConfig()
        assertEquals(64, config.buttonSizeDp)
        assertEquals(80, config.opacityPercent)
        assertEquals(OverlayTheme.AMOLED_BLACK, config.overlayTheme)
        assertEquals(MenuLayoutType.CIRCULAR, config.menuLayout)
        assertEquals(ActionItem.SCREENSHOT, config.doubleTapAction)
        assertEquals(1, prefs.getShortcuts().size)
    }
}
