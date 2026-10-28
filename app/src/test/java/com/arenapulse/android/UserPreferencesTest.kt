package com.arenapulse.android

import androidx.test.core.app.ApplicationProvider
import com.arenapulse.android.data.UserPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Robolectric exercises the real SharedPreferences implementation, not a stub. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class UserPreferencesTest {

    private fun prefs(): UserPreferences =
        UserPreferences(ApplicationProvider.getApplicationContext())

    @Test
    fun watchlist_startsEmpty() {
        assertTrue(prefs().watchedIds().isEmpty())
    }

    @Test
    fun toggleWatch_addsThenRemoves() {
        val preferences = prefs()

        assertEquals(setOf("openai/gpt-6.1"), preferences.toggleWatch("openai/gpt-6.1"))
        assertEquals(setOf("openai/gpt-6.1", "qwen/qwen4"), preferences.toggleWatch("qwen/qwen4"))
        assertEquals(setOf("qwen/qwen4"), preferences.toggleWatch("openai/gpt-6.1"))
        assertEquals(setOf("qwen/qwen4"), preferences.watchedIds())
    }

    @Test
    fun watchlist_survivesANewPreferencesInstance() {
        prefs().toggleWatch("deepseek/v4.1")

        // A fresh instance reads from the same on-disk preferences file.
        assertEquals(setOf("deepseek/v4.1"), prefs().watchedIds())
    }

    @Test
    fun clearWatchlist_removesEverything() {
        val preferences = prefs()
        preferences.toggleWatch("a/one")
        preferences.toggleWatch("b/two")

        assertTrue(preferences.clearWatchlist().isEmpty())
        assertTrue(prefs().watchedIds().isEmpty())
    }
}
