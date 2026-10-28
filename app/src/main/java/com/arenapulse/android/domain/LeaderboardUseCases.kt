package com.arenapulse.android.domain

import kotlin.math.max
import kotlin.math.roundToInt
import java.util.Locale

enum class RankingCategory(val label: String) {
    ALL("All"),
    CODING("Coding"),
    REASONING("Reasoning"),
    AGENTIC("Agentic"),
    OPEN_WEIGHT("Open-Weight"),
    ULTRA_CHEAP("Ultra-Cheap")
}

enum class SortMode(val label: String) {
    OVERALL("Overall"),
    VALUE("Best Value"),
    SPEED("Speed"),
    CONTEXT("Context"),
    PRICE("Lowest Price"),
    NEWEST("Newest")
}

/**
 * Pure business logic, deliberately Android-free so it is fully unit-testable
 * on the JVM in CI.
 */
object LeaderboardUseCases {

    const val DAYS_PER_MONTH = 30.0
    const val FRONTIER_CATEGORY_THRESHOLD = 85.0
    const val NEW_ARRIVAL_WINDOW_DAYS = 60

    fun filter(models: List<AiModel>, category: RankingCategory): List<AiModel> = when (category) {
        RankingCategory.ALL -> models
        RankingCategory.CODING -> models.filter { it.codingScore >= FRONTIER_CATEGORY_THRESHOLD }
        RankingCategory.REASONING -> models.filter { it.reasoningScore >= FRONTIER_CATEGORY_THRESHOLD }
        RankingCategory.AGENTIC -> models.filter { it.agenticScore >= FRONTIER_CATEGORY_THRESHOLD }
        RankingCategory.OPEN_WEIGHT -> models.filter { it.isOpenWeight }
        RankingCategory.ULTRA_CHEAP -> models.filter { it.isUltraCheap }
    }

    /**
     * Multi-term, case-insensitive search across display name, provider and slug.
     * Every whitespace-separated term must match, so "claude 5" narrows correctly.
     */
    fun search(models: List<AiModel>, query: String): List<AiModel> {
        val normalized = query.trim().lowercase(Locale.US)
        if (normalized.isEmpty()) return models
        val terms = normalized.split(Regex("\\s+")).filter { it.isNotEmpty() }
        if (terms.isEmpty()) return models
        return models.filter { model ->
            val haystack = "${model.name} ${model.id} ${model.provider}".lowercase(Locale.US)
            terms.all { haystack.contains(it) }
        }
    }

    /** Providers ordered by how many models they contribute, then alphabetically. */
    fun providers(models: List<AiModel>): List<String> =
        models.groupingBy { it.provider }
            .eachCount()
            .entries
            .sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key })
            .map { it.key }

    /** True when the model first appeared within [windowDays] of [nowSeconds]. */
    fun isNewArrival(
        model: AiModel,
        nowSeconds: Long,
        windowDays: Int = NEW_ARRIVAL_WINDOW_DAYS
    ): Boolean {
        if (model.created <= 0L) return false
        val ageSeconds = nowSeconds - model.created
        return ageSeconds in 0..(windowDays.toLong() * 86_400L)
    }

    fun sort(models: List<AiModel>, mode: SortMode): List<AiModel> = when (mode) {
        SortMode.OVERALL -> models.sortedWith(
            compareByDescending<AiModel> { it.overallScore }.thenBy { it.name }
        )
        SortMode.VALUE -> models.sortedWith(
            compareByDescending<AiModel> { it.valueScore }
                .thenByDescending { it.overallScore }
                .thenBy { it.name }
        )
        SortMode.SPEED -> models.sortedWith(
            compareByDescending<AiModel> { it.speedTokensPerSec }.thenByDescending { it.overallScore }
        )
        SortMode.CONTEXT -> models.sortedWith(
            compareByDescending<AiModel> { it.contextLength }.thenByDescending { it.overallScore }
        )
        SortMode.PRICE -> models.sortedWith(
            compareBy<AiModel> { it.blendedPricePerMillion }.thenByDescending { it.overallScore }
        )
        SortMode.NEWEST -> models.sortedWith(
            compareByDescending<AiModel> { it.created }.thenByDescending { it.overallScore }
        )
    }

    fun apply(
        models: List<AiModel>,
        category: RankingCategory,
        mode: SortMode
    ): List<AiModel> = sort(filter(models, category), mode)

    /**
     * One call for the rankings screen: search, provider and watchlist narrowing,
     * then category filtering and sorting.
     */
    fun refine(
        models: List<AiModel>,
        query: String,
        category: RankingCategory,
        mode: SortMode,
        provider: String? = null,
        watchedIds: Set<String> = emptySet(),
        watchlistOnly: Boolean = false
    ): List<AiModel> {
        var result = search(models, query)
        if (provider != null) result = result.filter { it.provider == provider }
        if (watchlistOnly) result = result.filter { it.id in watchedIds }
        return apply(result, category, mode)
    }

    /** Cost of one day of usage at the given token volumes. */
    fun dailyCost(model: AiModel, dailyInputTokens: Long, dailyOutputTokens: Long): Double {
        val input = dailyInputTokens.coerceAtLeast(0L) / 1_000_000.0 * model.inputPricePerMillion
        val output = dailyOutputTokens.coerceAtLeast(0L) / 1_000_000.0 * model.outputPricePerMillion
        return input + output
    }

    /** Cost of a 30-day month at the given daily token volumes. */
    fun monthlyCost(model: AiModel, dailyInputTokens: Long, dailyOutputTokens: Long): Double =
        dailyCost(model, dailyInputTokens, dailyOutputTokens) * DAYS_PER_MONTH

    /**
     * Pareto frontier: no other model is both cheaper (blended $/1M) and at least
     * as intelligent. Returned cheapest-first with strictly increasing scores.
     */
    fun paretoFrontier(models: List<AiModel>): List<AiModel> {
        val sorted = models.sortedWith(
            compareBy<AiModel> { it.blendedPricePerMillion }
                .thenByDescending { it.overallScore }
        )
        val frontier = mutableListOf<AiModel>()
        var bestScore = Double.NEGATIVE_INFINITY
        for (model in sorted) {
            if (model.overallScore > bestScore + 0.000001) {
                frontier += model
                bestScore = model.overallScore
            }
        }
        return frontier
    }

    fun isParetoLeader(model: AiModel, models: List<AiModel>): Boolean =
        paretoFrontier(models).any { it.id == model.id }

    /** Higher is better; used by Versus cost-efficiency bars. */
    fun costEfficiency(model: AiModel): Double =
        1.0 / max(model.blendedPricePerMillion, AiModel.MIN_PRICE_FLOOR)

    fun bestValue(models: List<AiModel>): AiModel? = models.maxByOrNull { it.valueScore }

    fun fastest(models: List<AiModel>): AiModel? = models.maxByOrNull { it.speedTokensPerSec }

    fun cheapest(models: List<AiModel>): AiModel? = models.minByOrNull { it.blendedPricePerMillion }

    fun averageOverall(models: List<AiModel>): Double =
        if (models.isEmpty()) 0.0
        else (models.sumOf { it.overallScore } / models.size * 10.0).roundToInt() / 10.0

    /** Models with a recorded price drop, steepest first. */
    fun biggestPriceDrops(models: List<AiModel>, limit: Int = 3): List<AiModel> = models
        .filter { model -> (model.priceChangePercent ?: 0.0) < 0.0 }
        .sortedBy { it.priceChangePercent }
        .take(limit)

    fun formatPrice(price: Double): String = when {
        price <= 0.0 -> "Free"
        price < 0.01 -> "<$0.01"
        price < 10.0 -> "$" + String.format(Locale.US, "%.2f", price)
        else -> "$" + String.format(Locale.US, "%.1f", price)
    }

    fun formatCompactNumber(value: Double): String = when {
        value >= 1_000_000_000 -> String.format(Locale.US, "%.1fB", value / 1_000_000_000)
        value >= 1_000_000 -> String.format(Locale.US, "%.1fM", value / 1_000_000)
        value >= 1_000 -> String.format(Locale.US, "%.0fK", value / 1_000)
        else -> value.roundToInt().toString()
    }

    fun formatCost(value: Double): String = when {
        value >= 100_000 -> "$" + String.format(Locale.US, "%,.0f", value)
        value >= 100 -> "$" + String.format(Locale.US, "%,.2f", value)
        else -> "$" + String.format(Locale.US, "%.2f", value)
    }

    /** "just now", "12m ago", "3h ago", "5d ago" for cache freshness. */
    fun relativeTime(fromMillis: Long, nowMillis: Long = System.currentTimeMillis()): String {
        if (fromMillis <= 0L) return "never"
        val delta = nowMillis - fromMillis
        if (delta < 0L) return "just now"
        val minutes = delta / 60_000L
        return when {
            minutes < 1L -> "just now"
            minutes < 60L -> "${minutes}m ago"
            minutes < 1_440L -> "${minutes / 60L}h ago"
            else -> "${minutes / 1_440L}d ago"
        }
    }

    fun formatPriceChange(percent: Double): String {
        val arrow = if (percent < 0) "\u25BC" else "\u25B2"
        return "$arrow " + String.format(Locale.US, "%.0f%%", kotlin.math.abs(percent))
    }
}
