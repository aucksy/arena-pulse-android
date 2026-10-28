package com.arenapulse.android

import com.arenapulse.android.data.ArchitectureDto
import com.arenapulse.android.data.FrontierScoresRegistry
import com.arenapulse.android.data.LeaderboardRepository
import com.arenapulse.android.data.ModelCatalogSource
import com.arenapulse.android.data.ModelDto
import com.arenapulse.android.data.PricingDto
import com.arenapulse.android.data.decodeModels
import com.arenapulse.android.data.toAiModel
import com.arenapulse.android.domain.AiModel
import com.arenapulse.android.domain.LeaderboardUseCases
import com.arenapulse.android.domain.RankingCategory
import com.arenapulse.android.domain.SortMode
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.io.IOException
import java.nio.file.Files

class LeaderboardLogicTest {

    private val sampleJson = """
        {
          "data": [
            {
              "id": "deepseek/deepseek-v4.1-flash",
              "name": "DeepSeek V4.1 Flash",
              "created": 1767225600,
              "context_length": 256000,
              "pricing": { "prompt": "0.00000028", "completion": "0.0000011" },
              "architecture": { "modality": "text->text" },
              "supported_parameters": ["reasoning", "tools", "structured_outputs"],
              "top_provider": { "is_moderated": false }
            },
            {
              "id": "newco/mystery-mini",
              "name": "Mystery Mini",
              "created": 1767225601,
              "context_length": 128000,
              "pricing": { "prompt": "0.000001", "completion": "0.000002" },
              "architecture": { "modality": "text->text" },
              "supported_parameters": ["tools"]
            }
          ]
        }
    """.trimIndent()

    private fun model(
        id: String,
        blendedCost: Double,
        overall: Double,
        openWeight: Boolean = false,
        coding: Double = overall,
        reasoning: Double = overall,
        agentic: Double = overall,
        speed: Double = 100.0,
        context: Int = 100_000
    ): AiModel = AiModel(
        id = id,
        name = id,
        provider = "Test",
        contextLength = context,
        inputPricePerMillion = blendedCost,
        outputPricePerMillion = blendedCost,
        overallScore = overall,
        codingScore = coding,
        reasoningScore = reasoning,
        agenticScore = agentic,
        speedTokensPerSec = speed,
        isOpenWeight = openWeight
    )

    @Test
    fun jsonParsing_mapsOpenRouterFieldsAndIgnoresUnknownKeys() {
        val dtos = decodeModels(sampleJson)

        assertEquals(2, dtos.size)
        assertEquals("deepseek/deepseek-v4.1-flash", dtos[0].id)
        assertEquals(256_000, dtos[0].contextLength)
        assertEquals("text->text", dtos[0].architecture.modality)
        assertEquals(listOf("reasoning", "tools", "structured_outputs"), dtos[0].supportedParameters)
    }

    @Test
    fun tokenPriceMath_convertsPerTokenToPerMillion() {
        val dtos = decodeModels(sampleJson)
        val flash = dtos[0].toAiModel()

        assertEquals(0.28, flash.inputPricePerMillion, 0.0000001)
        assertEquals(1.10, flash.outputPricePerMillion, 0.0000001)
        assertTrue(flash.supportsReasoning)
        assertTrue(flash.supportsTools)
        assertTrue(flash.supportsStructuredOutputs)
        assertTrue(flash.isOpenWeight)
        assertEquals(91.8, flash.overallScore, 0.0001)
    }

    @Test
    fun unknownModel_receivesDeterministicHeuristicScores() {
        val mysteryDto = decodeModels(sampleJson)[1]
        val first = mysteryDto.toAiModel()
        val second = mysteryDto.toAiModel()

        assertEquals(first.overallScore, second.overallScore, 0.0)
        assertTrue(first.overallScore in 45.0..98.5)
        assertTrue(first.codingScore in 40.0..99.0)
        assertTrue(first.speedTokensPerSec > 0.0)
        assertFalse(first.isOpenWeight)
    }

    @Test
    fun curatedRegistry_containsNamedFrontierModelsAndSeedList() {
        assertEquals(96.4, FrontierScoresRegistry.profileFor("anthropic/claude-fable-5.1").overall, 0.0)
        assertEquals(97.1, FrontierScoresRegistry.profileFor("openai/gpt-6.1-sol").overall, 0.0)
        assertEquals(95.2, FrontierScoresRegistry.profileFor("google/gemini-4-argon").overall, 0.0)
        assertTrue(FrontierScoresRegistry.profileFor("qwen/qwen3.8-max").openWeight)

        val names = FrontierScoresRegistry.seedModels.map { it.name }
        assertTrue(FrontierScoresRegistry.seedModels.size >= 20)
        assertTrue(names.contains("Claude Fable 5.1"))
        assertTrue(names.contains("GPT-6 Astra"))
        assertTrue(names.contains("DeepSeek V4.1 Flash"))
        assertTrue(names.contains("Llama 4 Maverick"))
    }

    @Test
    fun valueScore_usesThreeToOneBlendAndSortsDescending() {
        val premium = model("premium", blendedCost = 30.0, overall = 96.0)
        val mid = model("mid", blendedCost = 6.25, overall = 90.0)
        val cheap = model("cheap", blendedCost = 0.625, overall = 80.0)
        val freeish = model("freeish", blendedCost = 0.175, overall = 85.0)

        assertEquals(3.2, premium.valueScore, 0.0001)

        val sorted = LeaderboardUseCases.sort(
            listOf(premium, mid, cheap, freeish),
            SortMode.VALUE
        )
        assertEquals(listOf("freeish", "cheap", "mid", "premium"), sorted.map { it.id })
    }

    @Test
    fun sorting_byOverallSpeedContextAndPriceIsStable() {
        val a = model("a", 2.0, 80.0, speed = 100.0, context = 200_000)
        val b = model("b", 1.0, 90.0, speed = 300.0, context = 100_000)
        val c = model("c", 5.0, 85.0, speed = 200.0, context = 1_000_000)

        assertEquals(listOf("b", "c", "a"), LeaderboardUseCases.sort(listOf(a, b, c), SortMode.OVERALL).map { it.id })
        assertEquals(listOf("b", "c", "a"), LeaderboardUseCases.sort(listOf(a, b, c), SortMode.SPEED).map { it.id })
        assertEquals(listOf("c", "a", "b"), LeaderboardUseCases.sort(listOf(a, b, c), SortMode.CONTEXT).map { it.id })
        assertEquals(listOf("b", "a", "c"), LeaderboardUseCases.sort(listOf(a, b, c), SortMode.PRICE).map { it.id })
    }

    @Test
    fun filtering_openWeightAndUltraCheap() {
        val open = model("open", 0.5, 85.0, openWeight = true)
        val closed = model("closed", 2.0, 90.0, openWeight = false)
        val cheapClosed = model("cheap-closed", 0.8, 80.0, openWeight = false)
        val models = listOf(open, closed, cheapClosed)

        val openWeight = LeaderboardUseCases.filter(models, RankingCategory.OPEN_WEIGHT)
        assertEquals(listOf("open"), openWeight.map { it.id })

        val ultraCheap = LeaderboardUseCases.filter(models, RankingCategory.ULTRA_CHEAP)
        assertEquals(listOf("open", "cheap-closed"), ultraCheap.map { it.id })

        val coding = LeaderboardUseCases.filter(models, RankingCategory.CODING)
        assertEquals(2, coding.size)
    }

    @Test
    fun paretoFrontier_removesDominatedModelsAndKeepsCheapestFirst() {
        val a = model("a", 0.2, 70.0)
        val b = model("b", 1.0, 80.0)
        val dominated = model("dominated", 2.0, 75.0)
        val e = model("e", 4.0, 85.0)
        val d = model("d", 5.0, 90.0)
        val samePriceBetter = model("same-price-better", 5.0, 91.0)

        val frontier = LeaderboardUseCases.paretoFrontier(
            listOf(a, b, dominated, e, d, samePriceBetter)
        )

        assertEquals(
            listOf("a", "b", "e", "same-price-better"),
            frontier.map { it.id }
        )
        assertTrue(LeaderboardUseCases.isParetoLeader(b, listOf(a, b, dominated, e, d)))
        assertFalse(LeaderboardUseCases.isParetoLeader(dominated, listOf(a, b, dominated, e, d)))
    }

    @Test
    fun monthlyCost_projectsThirtyDayBill() {
        val premium = AiModel(
            id = "premium",
            name = "Premium",
            provider = "Test",
            inputPricePerMillion = 3.0,
            outputPricePerMillion = 15.0
        )

        val daily = LeaderboardUseCases.dailyCost(
            premium,
            dailyInputTokens = 1_000_000,
            dailyOutputTokens = 1_000_000
        )
        val monthly = LeaderboardUseCases.monthlyCost(
            premium,
            dailyInputTokens = 1_000_000,
            dailyOutputTokens = 1_000_000
        )

        assertEquals(18.0, daily, 0.0001)
        assertEquals(540.0, monthly, 0.0001)

        val small = LeaderboardUseCases.monthlyCost(
            premium,
            dailyInputTokens = 100_000,
            dailyOutputTokens = 50_000
        )
        assertEquals(31.5, small, 0.0001)
    }

    @Test
    fun costEfficiencyAndAggregates() {
        val expensive = model("expensive", 30.0, 96.0)
        val cheap = model("cheap", 0.5, 85.0)

        assertTrue(LeaderboardUseCases.costEfficiency(cheap) > LeaderboardUseCases.costEfficiency(expensive))
        assertEquals(90.5, LeaderboardUseCases.averageOverall(listOf(expensive, cheap)), 0.0001)
        assertEquals("cheap", LeaderboardUseCases.bestValue(listOf(expensive, cheap))?.id)
        assertEquals("cheap", LeaderboardUseCases.cheapest(listOf(expensive, cheap))?.id)
    }

    @Test
    fun repository_usesLiveDataThenCacheThenSeed() = runBlocking {
        val directory = Files.createTempDirectory("arenapulse-test").toFile()
        val cacheFile = File(directory, "leaderboard_cache.json")

        val liveDto = ModelDto(
            id = "liveco/live-mini",
            name = "Live Mini",
            created = 1_767_225_600L,
            contextLength = 128_000,
            pricing = PricingDto(prompt = "0.000001", completion = "0.000002"),
            architecture = ArchitectureDto(modality = "text->text"),
            supportedParameters = listOf("tools")
        )

        val onlineRepository = LeaderboardRepository(
            cacheFile = cacheFile,
            catalogSource = FakeCatalogSource { listOf(liveDto) }
        )

        val live = onlineRepository.load(forceRefresh = true)
        assertTrue(live.fromNetwork)
        assertEquals("live", live.sourceLabel)
        assertEquals(1, live.models.size)
        assertTrue(cacheFile.exists())

        val offlineRepository = LeaderboardRepository(
            cacheFile = cacheFile,
            catalogSource = FakeCatalogSource { throw IOException("network unavailable") }
        )

        val cached = offlineRepository.load(forceRefresh = true)
        assertFalse(cached.fromNetwork)
        assertEquals("cache", cached.sourceLabel)
        assertEquals("liveco/live-mini", cached.models.single().id)

        assertTrue(cacheFile.delete())

        val seeded = offlineRepository.load(forceRefresh = true)
        assertFalse(seeded.fromNetwork)
        assertEquals("seed", seeded.sourceLabel)
        assertTrue(seeded.models.size >= 20)
        assertEquals(
            seeded.models.size,
            seeded.models.map { it.id }.distinct().size
        )
    }

    @Test
    fun cachedJson_roundTripsAllFields() {
        val directory = Files.createTempDirectory("arenapulse-cache").toFile()
        val cacheFile = File(directory, "leaderboard_cache.json")
        val dto = ModelDto(
            id = "deepseek/deepseek-v4.1-flash",
            name = "DeepSeek V4.1 Flash",
            contextLength = 256_000,
            pricing = PricingDto(prompt = "0.00000028", completion = "0.0000011"),
            architecture = ArchitectureDto(modality = "text->text"),
            supportedParameters = listOf("reasoning", "tools")
        )

        val repository = LeaderboardRepository(
            cacheFile = cacheFile,
            catalogSource = FakeCatalogSource { listOf(dto) }
        )

        runBlocking { repository.load(forceRefresh = true) }

        val restored = LeaderboardRepository(
            cacheFile = cacheFile,
            catalogSource = FakeCatalogSource { throw IOException("offline") }
        )

        val snapshot = runBlocking { restored.load(forceRefresh = false) }
        val model = snapshot.models.single()
        assertEquals("deepseek/deepseek-v4.1-flash", model.id)
        assertEquals(0.28, model.inputPricePerMillion, 0.0000001)
        assertEquals(91.8, model.overallScore, 0.0001)
        assertTrue(model.supportsReasoning)
    }

    private class FakeCatalogSource(
        private val block: suspend () -> List<ModelDto>
    ) : ModelCatalogSource {
        override suspend fun fetchModels(): List<ModelDto> = block()
    }
}
