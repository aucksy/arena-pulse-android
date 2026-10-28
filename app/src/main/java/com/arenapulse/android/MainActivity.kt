package com.arenapulse.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import com.arenapulse.android.ui.LeaderboardViewModel
import com.arenapulse.android.ui.screens.MainScreen
import com.arenapulse.android.ui.theme.ArenaPulseTheme

class MainActivity : ComponentActivity() {

    private val container: AppContainer by lazy { AppContainer(applicationContext) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val viewModel = ViewModelProvider(
            this,
            LeaderboardViewModel.factory(container.repository, container.preferences)
        )[LeaderboardViewModel::class.java]

        setContent {
            ArenaPulseTheme {
                MainScreen(viewModel = viewModel)
            }
        }
    }
}
