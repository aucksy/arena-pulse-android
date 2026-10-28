package com.arenapulse.android.data

import android.content.Context

/**
 * Lightweight preference store for the watchlist. SharedPreferences is enough
 * here and keeps the app free of Room/KSP/KAPT.
 */
class UserPreferences(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun watchedIds(): Set<String> =
        prefs.getStringSet(KEY_WATCHLIST, emptySet())?.toSet() ?: emptySet()

    /** Adds or removes [modelId] and returns the resulting watchlist. */
    fun toggleWatch(modelId: String): Set<String> {
        val updated = watchedIds().toMutableSet()
        if (!updated.add(modelId)) {
            updated.remove(modelId)
        }
        prefs.edit().putStringSet(KEY_WATCHLIST, updated).apply()
        return updated
    }

    fun clearWatchlist(): Set<String> {
        prefs.edit().remove(KEY_WATCHLIST).apply()
        return emptySet()
    }

    companion object {
        private const val PREFS_NAME = "arena_pulse_prefs"
        private const val KEY_WATCHLIST = "watchlist"
    }
}
