package com.arenapulse.android.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.arenapulse.android.data.LeaderboardRepository
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
    val errorMessage: String? = null
)

class LeaderboardViewModel(
    private val repository: LeaderboardRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LeaderboardUiState())
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
            refresh(showSpinner = false)
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
                        "Offline mode - serving ${snapshot.sourceLabel} leaderboard data"
                    }
                )
            }
        }
    }

    companion object {
        fun factory(repository: LeaderboardRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    require(modelClass.isAssignableFrom(LeaderboardViewModel::class.java)) {
                        "Unsupported ViewModel class: ${modelClass.name}"
                    }
                    return LeaderboardViewModel(repository) as T
                }
            }
    }
}
