package com.arenapulse.android.ui.screens

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.ScatterPlot
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.arenapulse.android.ui.LeaderboardViewModel
import com.arenapulse.android.ui.theme.ArenaBackground
import com.arenapulse.android.ui.theme.ArenaCyan
import com.arenapulse.android.ui.theme.ArenaSurface
import com.arenapulse.android.ui.theme.ArenaTextMuted

private enum class ArenaTab(val label: String, val icon: ImageVector) {
    RANKINGS("Rankings", Icons.Filled.Leaderboard),
    VERSUS("Versus", Icons.Filled.CompareArrows),
    MATRIX("Matrix", Icons.Filled.ScatterPlot),
    SIGNALS("Signals", Icons.Filled.Bolt)
}

@Composable
fun MainScreen(viewModel: LeaderboardViewModel) {
    val state by viewModel.uiState.collectAsState()

    var selectedTab by rememberSaveable { mutableStateOf(ArenaTab.RANKINGS) }
    var selectedModelId by rememberSaveable { mutableStateOf<String?>(null) }

    val selectedModel = remember(state.models, selectedModelId) {
        state.models.firstOrNull { it.id == selectedModelId }
    }
    val selectedRank = remember(state.models, selectedModelId) {
        if (selectedModelId == null) {
            0
        } else {
            state.models
                .sortedByDescending { it.overallScore }
                .indexOfFirst { it.id == selectedModelId } + 1
        }
    }

    Scaffold(
        containerColor = ArenaBackground,
        bottomBar = {
            NavigationBar(containerColor = ArenaSurface, tonalElevation = 0.dp) {
                ArenaTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        icon = {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = tab.label
                            )
                        },
                        label = {
                            Text(
                                text = tab.label,
                                style = MaterialTheme.typography.labelSmall
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = ArenaCyan,
                            selectedTextColor = ArenaCyan,
                            indicatorColor = ArenaCyan.copy(alpha = 0.12f),
                            unselectedIconColor = ArenaTextMuted,
                            unselectedTextColor = ArenaTextMuted
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        Crossfade(
            targetState = selectedTab,
            animationSpec = tween(durationMillis = 220),
            label = "tab-crossfade",
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) { tab ->
            when (tab) {
                ArenaTab.RANKINGS -> RankingsScreen(
                    models = state.models,
                    isLoading = state.isLoading,
                    isRefreshing = state.isRefreshing,
                    lastUpdated = state.lastUpdated,
                    sourceLabel = state.sourceLabel,
                    errorMessage = state.errorMessage,
                    onRefresh = { viewModel.refresh() },
                    onModelClick = { selectedModelId = it.id }
                )

                ArenaTab.VERSUS -> VersusScreen(
                    models = state.models,
                    onInspect = { selectedModelId = it.id }
                )

                ArenaTab.MATRIX -> FrontierChartScreen(
                    models = state.models,
                    onInspect = { selectedModelId = it.id }
                )

                ArenaTab.SIGNALS -> InsightsScreen(
                    models = state.models,
                    onInspect = { selectedModelId = it.id }
                )
            }
        }
    }

    if (selectedModel != null) {
        ModelDetailSheet(
            model = selectedModel,
            rank = selectedRank,
            onDismiss = { selectedModelId = null }
        )
    }
}
