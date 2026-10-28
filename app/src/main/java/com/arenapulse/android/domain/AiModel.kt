package com.arenapulse.android.domain

import kotlinx.serialization.Serializable
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max

/**
 * A normalized leaderboard entry. Live catalog data (pricing, context, modality)
 * is merged with deterministic benchmark scores before it reaches the UI.
 *
 * [previousInputPricePerMillion] / [previousOutputPricePerMillion] are populated
 * from the prior cache entry so the UI can surface price moves between syncs.
 */
@Serializable
data class AiModel(
    val id: String,
    val name: String,
    val provider: String,
    val created: Long = 0L,
    val contextLength: Int = 0,
    val inputPricePerMillion: Double = 0.0,
    val outputPricePerMillion: Double = 0.0,
    val modality: String = "text->text",
    val supportsReasoning: Boolean = false,
    val supportsTools: Boolean = false,
    val supportsStructuredOutputs: Boolean = false,
    val overallScore: Double = 0.0,
    val codingScore: Double = 0.0,
    val reasoningScore: Double = 0.0,
    val agenticScore: Double = 0.0,
    val speedTokensPerSec: Double = 0.0,
    val isOpenWeight: Boolean = false,
    val previousInputPricePerMillion: Double? = null,
    val previousOutputPricePerMillion: Double? = null
) {
    /** 3:1 input-to-output blend used for value and Pareto comparisons. */
    val blendedPricePerMillion: Double
        get() = inputPricePerMillion * INPUT_WEIGHT + outputPricePerMillion * OUTPUT_WEIGHT

    /** Intelligence points purchased per blended dollar. Higher is better. */
    val valueScore: Double
        get() = overallScore / max(blendedPricePerMillion, MIN_PRICE_FLOOR)

    val isUltraCheap: Boolean
        get() = blendedPricePerMillion < ULTRA_CHEAP_THRESHOLD

    /** Zero-cost promotional or local models. */
    val isFree: Boolean
        get() = blendedPricePerMillion <= 0.0001

    val supportsVision: Boolean
        get() = modality.contains("image", ignoreCase = true)

    /**
     * Percentage change in blended price versus the previous sync.
     * Null when there is no baseline or the move is under 0.5%.
     */
    val priceChangePercent: Double?
        get() {
            val previousInput = previousInputPricePerMillion ?: return null
            val previousOutput = previousOutputPricePerMillion ?: return null
            val previousBlended = previousInput * INPUT_WEIGHT + previousOutput * OUTPUT_WEIGHT
            if (previousBlended <= 0.0001) return null
            val percent = (blendedPricePerMillion - previousBlended) / previousBlended * 100.0
            return if (abs(percent) < 0.5) null else percent
        }

    val hasPriceChange: Boolean
        get() = priceChangePercent != null

    val tier: ScoreTier
        get() = when {
            overallScore >= 94.0 -> ScoreTier.ELITE
            overallScore >= 88.0 -> ScoreTier.FRONTIER
            overallScore >= 80.0 -> ScoreTier.STRONG
            overallScore >= 70.0 -> ScoreTier.CAPABLE
            else -> ScoreTier.EMERGING
        }

    val contextLabel: String
        get() = when {
            contextLength >= 1_000_000 ->
                String.format(Locale.US, "%.1fM", contextLength / 1_000_000.0)
            contextLength >= 1_000 -> "${contextLength / 1_000}K"
            else -> contextLength.toString()
        }

    companion object {
        const val INPUT_WEIGHT = 0.75
        const val OUTPUT_WEIGHT = 0.25
        const val MIN_PRICE_FLOOR = 0.05
        const val ULTRA_CHEAP_THRESHOLD = 1.0
    }
}

enum class ScoreTier {
    ELITE,
    FRONTIER,
    STRONG,
    CAPABLE,
    EMERGING
}
