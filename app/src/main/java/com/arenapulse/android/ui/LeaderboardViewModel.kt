package com.arenapulse.android.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.arenapulse.android.data.LeaderboardRepository
import com.arenapulse.android.data.UserPreferences
import com.arenapulse.android.domain.AiModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LeaderboardUiState(
    val models: List<AiModel> = emptyList(),
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val lastUpdated: Long = 0L,
    val sourceLabel: String = "seed",
    val errorMessage: String? = null,
    val watchedIds: Set<String> = emptySet()
) {
    /** True when the served snapshot is older than a day. */
    val isStale: Boolean
        get() = lastUpdated > 0L &&
            System.currentTimeMillis() - lastUpdated > STALE_AFTER_MILLIS

    companion object {
        const val STALE_AFTER_MILLIS = 24L * 60L * 60L * 1_000L
    }
}

class LeaderboardViewModel(
    private val repository: LeaderboardRepository,
    private val preferences: UserPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        LeaderboardUiState(watchedIds = preferences.watchedIds())
    )
    val uiState: StateFlow<LeaderboardUiState> = _uiState.asStateFlow()

    private var refreshJob: Job? = null

    init {
        viewModelScope.launch {
            val cached = repository.load(forceRefresh = false)
            _uiState.update {
                it.copy(
                    models = cached.models,
                    isLoading = false,
                    lastUpdated = cached.lastUpdated,
                    sourceLabel = cached.sourceLabel
                )
            }
            // Only reach for the network when the snapshot is old or we fell back to seeds.
            val isFresh = cached.sourceLabel != "seed" &&
                System.currentTimeMillis() - cached.lastUpdated < AUTO_REFRESH_WINDOW_MILLIS
            if (!isFresh) {
                refresh(showSpinner = false)
            }
        }
    }

    fun refresh(showSpinner: Boolean = true) {
        if (refreshJob?.isActive == true) return
        refreshJob = viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = showSpinner, errorMessage = null) }
            val snapshot = repository.load(forceRefresh = true)
            _uiState.update {
                it.copy(
                    models = snapshot.models,
                    isLoading = false,
                    isRefreshing = false,
                    lastUpdated = snapshot.lastUpdated,
                    sourceLabel = snapshot.sourceLabel,
                    errorMessage = if (snapshot.fromNetwork) {
                        null
                    } else {
                        "Offline - showing the ${snapshot.sourceLabel} snapshot"
                    }
                )
            }
        }
    }

    fun toggleWatch(modelId: String) {
        val updated = preferences.toggleWatch(modelId)
        _uiState.update { it.copy(watchedIds = updated) }
    }

    fun clearWatchlist() {
        val updated = preferences.clearWatchlist()
        _uiState.update { it.copy(watchedIds = updated) }
    }

    /** Drops the JSON cache and immediately re-syncs from OpenRouter. */
    fun clearCacheAndResync() {
        repository.clearCache()
        _uiState.update { it.copy(lastUpdated = 0L) }
        refresh(showSpinner = true)
    }

    fun cacheSummary(): Pair<Int, Long?> = repository.cachedModelCount() to repository.cacheSavedAt()

    companion object {
        private const val AUTO_REFRESH_WINDOW_MILLIS = 10L * 60L * 1_000L

        fun factory(
            repository: LeaderboardRepository,
            preferences: UserPreferences
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    require(modelClass.isAssignableFrom(LeaderboardViewModel::class.java)) {
                        "Unsupported ViewModel class: ${modelClass.name}"
                    }
                    return LeaderboardViewModel(repository, preferences) as T
                }
            }
    }
}
