package com.arenapulse.android.data

import com.arenapulse.android.domain.AiModel
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Shared JSON configuration: tolerant of the extra fields OpenRouter adds over time. */
val ArenaJson: Json = Json {
    ignoreUnknownKeys = true
    isLenient = true
    coerceInputValues = true
    encodeDefaults = true
}

@Serializable
data class ModelsResponse(
    val data: List<ModelDto> = emptyList()
)

@Serializable
data class ModelDto(
    val id: String = "",
    val name: String = "",
    val created: Long = 0L,
    @SerialName("context_length") val contextLength: Int = 0,
    val pricing: PricingDto = PricingDto(),
    val architecture: ArchitectureDto = ArchitectureDto(),
    @SerialName("supported_parameters") val supportedParameters: List<String> = emptyList()
)

@Serializable
data class PricingDto(
    val prompt: String = "0",
    val completion: String = "0"
)

@Serializable
data class ArchitectureDto(
    val modality: String = "text->text"
)

/** Decode an OpenRouter `/api/v1/models` payload. Exposed for unit tests. */
fun decodeModels(raw: String, json: Json = ArenaJson): List<ModelDto> =
    json.decodeFromString<ModelsResponse>(raw).data

/** OpenRouter quotes USD per token; the UI works in USD per 1M tokens. */
fun String.toPricePerMillion(): Double =
    ((toDoubleOrNull() ?: 0.0) * 1_000_000.0).coerceAtLeast(0.0)

fun providerDisplayName(modelId: String): String = when (modelId.substringBefore('/').lowercase()) {
    "anthropic" -> "Anthropic"
    "openai" -> "OpenAI"
    "google" -> "Google DeepMind"
    "moonshotai" -> "Moonshot AI"
    "qwen" -> "Alibaba Qwen"
    "deepseek" -> "DeepSeek"
    "x-ai" -> "xAI"
    "meta-llama" -> "Meta"
    "mistralai" -> "Mistral AI"
    "z-ai" -> "Z.ai"
    "minimax" -> "MiniMax"
    "microsoft" -> "Microsoft"
    "cohere" -> "Cohere"
    "nvidia" -> "NVIDIA"
    "openrouter" -> "OpenRouter"
    else -> modelId.substringBefore('/')
        .replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
}

fun ModelDto.toAiModel(
    profile: FrontierScoresRegistry.BenchmarkProfile = FrontierScoresRegistry.profileFor(id, name),
    previous: AiModel? = null
): AiModel {
    val params = supportedParameters.map { it.lowercase() }
    return AiModel(
        id = id,
        name = name.ifBlank { id },
        provider = providerDisplayName(id),
        created = created,
        contextLength = contextLength,
        inputPricePerMillion = pricing.prompt.toPricePerMillion(),
        outputPricePerMillion = pricing.completion.toPricePerMillion(),
        modality = architecture.modality.ifBlank { "text->text" },
        supportsReasoning = params.contains("reasoning"),
        supportsTools = params.contains("tools"),
        supportsStructuredOutputs = params.contains("structured_outputs"),
        overallScore = profile.overall,
        codingScore = profile.coding,
        reasoningScore = profile.reasoning,
        agenticScore = profile.agentic,
        speedTokensPerSec = profile.speed,
        isOpenWeight = profile.openWeight,
        previousInputPricePerMillion = previous?.inputPricePerMillion,
        previousOutputPricePerMillion = previous?.outputPricePerMillion
    )
}
