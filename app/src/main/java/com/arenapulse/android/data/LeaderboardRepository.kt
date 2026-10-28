package com.arenapulse.android.data

import com.arenapulse.android.domain.AiModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

@Serializable
data class CachedLeaderboard(
    val version: Int = 1,
    val savedAt: Long = 0L,
    val models: List<AiModel> = emptyList()
)

data class LeaderboardSnapshot(
    val models: List<AiModel>,
    val fromNetwork: Boolean,
    val lastUpdated: Long,
    val sourceLabel: String
)

/**
 * Offline-first leaderboard store.
 *
 * Order of preference:
 * 1. Fresh OpenRouter catalog merged with the benchmark registry.
 * 2. Last successful JSON cache in `filesDir/leaderboard_cache.json`.
 * 3. The built-in registry seed (24 frontier models).
 *
 * No Room, no KSP, no KAPT: a single JSON document is all the persistence we need.
 */
class LeaderboardRepository(
    private val cacheFile: File,
    private val catalogSource: ModelCatalogSource,
    private val json: Json = ArenaJson
) {

    suspend fun load(forceRefresh: Boolean = false): LeaderboardSnapshot = withContext(Dispatchers.IO) {
        if (!forceRefresh) {
            readCache()?.takeIf { it.models.isNotEmpty() }?.let { cached ->
                return@withContext LeaderboardSnapshot(
                    models = cached.models,
                    fromNetwork = false,
                    lastUpdated = cached.savedAt,
                    sourceLabel = "cache"
                )
            }
        }

        val liveModels = runCatching { catalogSource.fetchModels() }
            .getOrNull()
            ?.let { merge(it) }
            .orEmpty()

        if (liveModels.isNotEmpty()) {
            writeCache(liveModels)
            return@withContext LeaderboardSnapshot(
                models = liveModels,
                fromNetwork = true,
                lastUpdated = System.currentTimeMillis(),
                sourceLabel = "live"
            )
        }

        readCache()?.takeIf { it.models.isNotEmpty() }?.let { cached ->
            return@withContext LeaderboardSnapshot(
                models = cached.models,
                fromNetwork = false,
                lastUpdated = cached.savedAt,
                sourceLabel = "cache"
            )
        }

        LeaderboardSnapshot(
            models = FrontierScoresRegistry.seedModels.sortedByDescending { it.overallScore },
            fromNetwork = false,
            lastUpdated = System.currentTimeMillis(),
            sourceLabel = "seed"
        )
    }

    /** Merge live catalog fields with deterministic benchmark profiles. */
    fun merge(dtos: List<ModelDto>): List<AiModel> = dtos
        .asSequence()
        .filter { it.id.isNotBlank() }
        .distinctBy { it.id }
        .map { it.toAiModel() }
        .sortedWith(compareByDescending<AiModel> { it.overallScore }.thenBy { it.name })
        .toList()

    private fun readCache(): CachedLeaderboard? {
        if (!cacheFile.exists()) return null
        return runCatching {
            json.decodeFromString<CachedLeaderboard>(cacheFile.readText())
        }.getOrNull()
    }

    private fun writeCache(models: List<AiModel>) {
        runCatching {
            cacheFile.parentFile?.mkdirs()
            val payload = CachedLeaderboard(
                savedAt = System.currentTimeMillis(),
                models = models
            )
            cacheFile.writeText(json.encodeToString(CachedLeaderboard.serializer(), payload))
        }
    }
}
