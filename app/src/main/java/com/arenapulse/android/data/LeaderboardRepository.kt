package com.arenapulse.android.data

import com.arenapulse.android.domain.AiModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

@Serializable
data class CachedLeaderboard(
    val version: Int = 2,
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
 * Price history is preserved across syncs: the previous cache entry for a model
 * is attached to the merged record so the UI can show price moves.
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

        val previousModels = readCache()?.models?.associateBy { it.id }.orEmpty()

        val liveModels = runCatching { catalogSource.fetchModels() }
            .getOrNull()
            ?.let { merge(it, previousModels) }
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

    /**
     * Merge live catalog fields with deterministic benchmark profiles.
     * [previous] supplies the prior sync's prices for delta detection.
     */
    fun merge(
        dtos: List<ModelDto>,
        previous: Map<String, AiModel> = emptyMap()
    ): List<AiModel> = dtos
        .asSequence()
        .filter { it.id.isNotBlank() }
        .distinctBy { it.id }
        .map { dto -> dto.toAiModel(previous = previous[dto.id]) }
        .sortedWith(compareByDescending<AiModel> { it.overallScore }.thenBy { it.name })
        .toList()

    /** Timestamp of the on-disk cache, or null when nothing is cached. */
    fun cacheSavedAt(): Long? = readCache()?.savedAt

    /** Number of models currently persisted, or 0. */
    fun cachedModelCount(): Int = readCache()?.models?.size ?: 0

    fun clearCache(): Boolean = runCatching {
        if (cacheFile.exists()) cacheFile.delete() else true
    }.getOrDefault(false)

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
