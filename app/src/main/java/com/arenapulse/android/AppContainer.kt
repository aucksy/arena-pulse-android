package com.arenapulse.android

import android.content.Context
import com.arenapulse.android.data.ArenaJson
import com.arenapulse.android.data.LeaderboardRepository
import com.arenapulse.android.data.ModelCatalogSource
import com.arenapulse.android.data.OpenRouterApiClient
import java.io.File

/**
 * Tiny manual DI container. Deliberately no Hilt, no Dagger, no annotation
 * processors: the dependency graph is small enough to wire by hand.
 */
class AppContainer(context: Context) {

    val json = ArenaJson

    val catalogSource: ModelCatalogSource = OpenRouterApiClient(json = json)

    val repository: LeaderboardRepository = LeaderboardRepository(
        cacheFile = File(context.filesDir, "leaderboard_cache.json"),
        catalogSource = catalogSource,
        json = json
    )
}
