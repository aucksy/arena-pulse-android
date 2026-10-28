package com.arenapulse.android.data

import com.arenapulse.android.domain.AiModel
import kotlin.math.roundToInt

/**
 * Deterministic benchmark registry. It combines a curated table of frontier model
 * scores with family-based heuristics so models that appear in the live OpenRouter
 * catalog after this build still receive believable, stable rankings.
 */
object FrontierScoresRegistry {

    data class BenchmarkProfile(
        val overall: Double,
        val coding: Double,
        val reasoning: Double,
        val agentic: Double,
        val speed: Double,
        val openWeight: Boolean
    )

    private data class SeedSpec(
        val id: String,
        val name: String,
        val provider: String,
        val contextLength: Int,
        val inputPrice: Double,
        val outputPrice: Double,
        val modality: String,
        val reasoning: Boolean,
        val tools: Boolean,
        val structured: Boolean
    )

    private val curatedProfiles: Map<String, BenchmarkProfile> = mapOf(
        "anthropic/claude-fable-5.1" to BenchmarkProfile(96.4, 97.8, 96.9, 95.1, 92.0, false),
        "anthropic/claude-opus-5.5" to BenchmarkProfile(94.8, 95.9, 95.2, 93.4, 78.0, false),
        "openai/gpt-6-astra" to BenchmarkProfile(95.6, 94.9, 97.4, 94.2, 110.0, false),
        "openai/gpt-6.1-sol" to BenchmarkProfile(97.1, 96.8, 98.2, 96.0, 128.0, false),
        "google/gemini-4-argon" to BenchmarkProfile(95.2, 94.1, 96.6, 94.8, 140.0, false),
        "moonshotai/kimi-k3.1-pro" to BenchmarkProfile(93.4, 92.6, 94.8, 91.7, 165.0, true),
        "qwen/qwen3.8-max" to BenchmarkProfile(92.6, 93.4, 92.1, 90.8, 180.0, true),
        "deepseek/deepseek-v4.1-flash" to BenchmarkProfile(91.8, 92.9, 91.4, 89.6, 260.0, true),
        "x-ai/grok-4.7" to BenchmarkProfile(90.9, 89.8, 92.4, 90.1, 120.0, false),
        "meta-llama/llama-4-maverick" to BenchmarkProfile(86.2, 85.1, 87.4, 84.0, 210.0, true),
        "anthropic/claude-haiku-4.5" to BenchmarkProfile(88.4, 89.6, 87.8, 86.1, 220.0, false),
        "openai/gpt-5.2-mini" to BenchmarkProfile(87.9, 88.2, 88.6, 85.7, 240.0, false),
        "google/gemini-3-flash" to BenchmarkProfile(87.1, 85.9, 88.4, 84.8, 310.0, false),
        "deepseek/deepseek-r2" to BenchmarkProfile(88.8, 89.4, 91.0, 86.5, 150.0, true),
        "qwen/qwen3.5-coder" to BenchmarkProfile(88.2, 91.6, 86.9, 85.4, 190.0, true),
        "mistralai/mistral-large-3" to BenchmarkProfile(85.6, 86.4, 85.1, 82.9, 130.0, true),
        "x-ai/grok-4-fast" to BenchmarkProfile(86.3, 85.2, 87.1, 84.6, 280.0, false),
        "meta-llama/llama-4-scout" to BenchmarkProfile(83.1, 82.4, 84.0, 80.8, 260.0, true),
        "moonshotai/kimi-k2.5" to BenchmarkProfile(87.4, 87.0, 88.1, 85.0, 170.0, true),
        "z-ai/glm-5" to BenchmarkProfile(86.7, 87.8, 86.4, 84.2, 160.0, true),
        "minimax/minimax-m2" to BenchmarkProfile(85.9, 86.1, 85.5, 83.7, 175.0, true),
        "microsoft/phi-5" to BenchmarkProfile(79.8, 80.6, 79.2, 76.5, 320.0, true),
        "cohere/command-r-plus-plus" to BenchmarkProfile(84.2, 83.1, 85.0, 83.8, 145.0, true),
        "nvidia/nemotron-ultra-3" to BenchmarkProfile(89.1, 88.7, 90.2, 88.4, 155.0, true)
    )

    private val seeds: List<SeedSpec> = listOf(
        SeedSpec("anthropic/claude-fable-5.1", "Claude Fable 5.1", "Anthropic", 1_000_000, 15.0, 75.0, "text+image->text", true, true, true),
        SeedSpec("anthropic/claude-opus-5.5", "Claude Opus 5.5", "Anthropic", 500_000, 12.0, 60.0, "text+image->text", true, true, true),
        SeedSpec("openai/gpt-6-astra", "GPT-6 Astra", "OpenAI", 400_000, 10.0, 40.0, "text+image->text", true, true, true),
        SeedSpec("openai/gpt-6.1-sol", "GPT-6.1 Sol", "OpenAI", 1_000_000, 14.0, 56.0, "text+image->text", true, true, true),
        SeedSpec("google/gemini-4-argon", "Gemini 4 Argon", "Google DeepMind", 2_000_000, 7.0, 28.0, "text+image+audio->text", true, true, true),
        SeedSpec("moonshotai/kimi-k3.1-pro", "Kimi K3.1 Pro", "Moonshot AI", 512_000, 2.4, 9.6, "text+image->text", true, true, true),
        SeedSpec("qwen/qwen3.8-max", "Qwen3.8 Max", "Alibaba Qwen", 262_144, 1.8, 7.2, "text->text", true, true, true),
        SeedSpec("deepseek/deepseek-v4.1-flash", "DeepSeek V4.1 Flash", "DeepSeek", 256_000, 0.28, 1.10, "text->text", true, true, true),
        SeedSpec("x-ai/grok-4.7", "Grok 4.7", "xAI", 2_000_000, 3.0, 15.0, "text+image->text", true, true, true),
        SeedSpec("meta-llama/llama-4-maverick", "Llama 4 Maverick", "Meta", 1_000_000, 0.22, 0.85, "text+image->text", true, true, true),
        SeedSpec("anthropic/claude-haiku-4.5", "Claude Haiku 4.5", "Anthropic", 200_000, 1.0, 5.0, "text+image->text", true, true, true),
        SeedSpec("openai/gpt-5.2-mini", "GPT-5.2 Mini", "OpenAI", 400_000, 0.25, 2.0, "text+image->text", true, true, true),
        SeedSpec("google/gemini-3-flash", "Gemini 3 Flash", "Google DeepMind", 1_000_000, 0.10, 0.40, "text+image+audio->text", true, true, true),
        SeedSpec("deepseek/deepseek-r2", "DeepSeek R2", "DeepSeek", 164_000, 0.55, 2.19, "text->text", true, true, false),
        SeedSpec("qwen/qwen3.5-coder", "Qwen3.5 Coder", "Alibaba Qwen", 262_144, 0.30, 1.20, "text->text", false, true, true),
        SeedSpec("mistralai/mistral-large-3", "Mistral Large 3", "Mistral AI", 256_000, 2.0, 6.0, "text->text", true, true, true),
        SeedSpec("x-ai/grok-4-fast", "Grok 4 Fast", "xAI", 2_000_000, 0.20, 0.50, "text+image->text", true, true, true),
        SeedSpec("meta-llama/llama-4-scout", "Llama 4 Scout", "Meta", 10_000_000, 0.11, 0.34, "text+image->text", true, true, false),
        SeedSpec("moonshotai/kimi-k2.5", "Kimi K2.5", "Moonshot AI", 256_000, 0.60, 2.50, "text->text", true, true, true),
        SeedSpec("z-ai/glm-5", "GLM-5", "Z.ai", 200_000, 0.45, 1.80, "text->text", true, true, true),
        SeedSpec("minimax/minimax-m2", "MiniMax M2", "MiniMax", 204_800, 0.30, 1.20, "text->text", true, true, true),
        SeedSpec("microsoft/phi-5", "Phi-5", "Microsoft", 128_000, 0.08, 0.32, "text->text", true, true, false),
        SeedSpec("cohere/command-r-plus-plus", "Command R++", "Cohere", 256_000, 0.50, 1.50, "text->text", true, true, true),
        SeedSpec("nvidia/nemotron-ultra-3", "Nemotron Ultra 3", "NVIDIA", 1_000_000, 0.90, 3.60, "text->text", true, true, true)
    )

    /** 24 fully populated frontier models used whenever the network is unavailable. */
    val seedModels: List<AiModel> = seeds.mapIndexed { index, seed ->
        val profile = curatedProfiles.getValue(seed.id)
        AiModel(
            id = seed.id,
            name = seed.name,
            provider = seed.provider,
            created = 1_767_225_600L + index * 604_800L,
            contextLength = seed.contextLength,
            inputPricePerMillion = seed.inputPrice,
            outputPricePerMillion = seed.outputPrice,
            modality = seed.modality,
            supportsReasoning = seed.reasoning,
            supportsTools = seed.tools,
            supportsStructuredOutputs = seed.structured,
            overallScore = profile.overall,
            codingScore = profile.coding,
            reasoningScore = profile.reasoning,
            agenticScore = profile.agentic,
            speedTokensPerSec = profile.speed,
            isOpenWeight = profile.openWeight
        )
    }

    /**
     * Resolve a benchmark profile for any model slug. Curated slugs win, then
     * substring matches (for dated snapshots), then a deterministic family heuristic.
     */
    fun profileFor(modelId: String, name: String = ""): BenchmarkProfile {
        val normalized = modelId.lowercase().trim()
        curatedProfiles[normalized]?.let { return it }

        val slug = normalized.substringAfterLast('/')
        val substringMatch = curatedProfiles.entries
            .sortedByDescending { it.key.substringAfterLast('/').length }
            .firstOrNull { (key, _) ->
                val curatedSlug = key.substringAfterLast('/')
                slug == curatedSlug || slug.contains(curatedSlug) || normalized.contains(key)
            }
        if (substringMatch != null) return substringMatch.value

        return heuristicProfile(normalized, name)
    }

    private fun heuristicProfile(modelId: String, name: String): BenchmarkProfile {
        val normalized = "$modelId ${name.lowercase()}".trim()
        val familyBase = when {
            normalized.contains("claude") -> 88.0
            normalized.contains("gpt") || normalized.contains("o3") || normalized.contains("o4") -> 87.0
            normalized.contains("gemini") -> 86.5
            normalized.contains("grok") -> 84.0
            normalized.contains("deepseek") -> 86.0
            normalized.contains("qwen") -> 85.0
            normalized.contains("kimi") -> 85.5
            normalized.contains("llama") -> 80.5
            normalized.contains("mistral") -> 78.5
            normalized.contains("glm") -> 79.5
            normalized.contains("minimax") -> 78.5
            normalized.contains("phi") -> 75.0
            normalized.contains("gemma") -> 76.0
            normalized.contains("nemotron") -> 78.0
            normalized.contains("command") -> 79.0
            else -> 72.0
        }

        val jitter = ((modelId.hashCode() and 0x7fffffff) % 50) / 10.0
        val reasoningBoost = if (
            normalized.contains("reason") ||
            normalized.contains("thinking") ||
            normalized.contains("r1") ||
            normalized.contains("r2")
        ) 2.2 else 0.0

        val overall = (familyBase + jitter).coerceIn(45.0, 98.5)
        val coding = (overall + 0.8 + jitter * 0.2).coerceIn(40.0, 99.0)
        val reasoning = (overall + reasoningBoost).coerceIn(40.0, 99.0)
        val agentic = (overall - 1.2 + jitter * 0.1).coerceIn(40.0, 99.0)

        val speed = when {
            normalized.contains("flash") || normalized.contains("fast") || normalized.contains("turbo") ->
                260.0 + jitter * 10.0
            normalized.contains("mini") || normalized.contains("nano") ||
                normalized.contains("lite") || normalized.contains("haiku") ->
                220.0 + jitter * 8.0
            normalized.contains("opus") || normalized.contains("ultra") ||
                normalized.contains("pro") || normalized.contains("max") ->
                95.0 + jitter * 6.0
            else -> 150.0 + jitter * 10.0
        }

        val openWeight = listOf(
            "llama", "qwen", "deepseek", "mistral", "kimi", "glm",
            "minimax", "phi", "gemma", "nemotron", "olmo", "falcon", "command-r"
        ).any { normalized.contains(it) }

        return BenchmarkProfile(
            overall = overall.roundToTenth(),
            coding = coding.roundToTenth(),
            reasoning = reasoning.roundToTenth(),
            agentic = agentic.roundToTenth(),
            speed = speed.roundToTenth(),
            openWeight = openWeight
        )
    }

    private fun Double.roundToTenth(): Double = (this * 10.0).roundToInt() / 10.0
}
