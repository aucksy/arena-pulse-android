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
import org.junit.Assert.assertNull
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

    @Test
    fun search_matchesNameProviderAndSlugWithAllTerms() {
        val claude = AiModel(
            id = "anthropic/claude-fable-5.1",
            name = "Claude Fable 5.1",
            provider = "Anthropic"
        )
        val gpt = AiModel(
            id = "openai/gpt-6.1-sol",
            name = "GPT-6.1 Sol",
            provider = "OpenAI"
        )
        val deepseek = AiModel(
            id = "deepseek/deepseek-v4.1-flash",
            name = "DeepSeek V4.1 Flash",
            provider = "DeepSeek"
        )
        val all = listOf(claude, gpt, deepseek)

        assertEquals(1, LeaderboardUseCases.search(all, "fable").size)
        assertEquals(1, LeaderboardUseCases.search(all, "anthropic").size)
        assertEquals(1, LeaderboardUseCases.search(all, "v4.1").size)
        assertEquals(3, LeaderboardUseCases.search(all, "   ").size)
        assertEquals(0, LeaderboardUseCases.search(all, "gemini").size)

        // Every whitespace-separated term must match.
        assertEquals(1, LeaderboardUseCases.search(all, "deepseek flash").size)
        assertEquals(0, LeaderboardUseCases.search(all, "deepseek sol").size)
    }

    @Test
    fun providers_areOrderedByModelCountThenName() {
        val models = listOf(
            AiModel("a/1", "A1", "Alpha"),
            AiModel("a/2", "A2", "Alpha"),
            AiModel("b/1", "B1", "Beta"),
            AiModel("c/1", "C1", "Gamma")
        )

        assertEquals(listOf("Alpha", "Beta", "Gamma"), LeaderboardUseCases.providers(models))
    }

    @Test
    fun newestSort_ordersByCreationTimestamp() {
        val old = model("old", 1.0, 80.0).copy(created = 100L)
        val mid = model("mid", 1.0, 80.0).copy(created = 200L)
        val fresh = model("fresh", 1.0, 80.0).copy(created = 300L)

        assertEquals(
            listOf("fresh", "mid", "old"),
            LeaderboardUseCases.sort(listOf(old, fresh, mid), SortMode.NEWEST).map { it.id }
        )
    }

    @Test
    fun newArrivalWindow_respectsCreatedTimestamp() {
        val now = 2_000_000_000L
        val recent = AiModel("recent", "Recent", "Test", created = now - 86_400L)
        val stale = AiModel("stale", "Stale", "Test", created = now - 120L * 86_400L)
        val unknown = AiModel("unknown", "Unknown", "Test", created = 0L)

        assertTrue(LeaderboardUseCases.isNewArrival(recent, now))
        assertFalse(LeaderboardUseCases.isNewArrival(stale, now))
        assertFalse(LeaderboardUseCases.isNewArrival(unknown, now))
    }

    @Test
    fun priceChangePercent_detectsDropsAndIgnoresNoise() {
        val dropped = AiModel(
            id = "drop",
            name = "Drop",
            provider = "Test",
            inputPricePerMillion = 0.5,
            outputPricePerMillion = 2.0,
            previousInputPricePerMillion = 1.0,
            previousOutputPricePerMillion = 4.0
        )
        assertEquals(-50.0, dropped.priceChangePercent ?: 0.0, 0.0001)
        assertTrue(dropped.hasPriceChange)

        val stable = AiModel(
            id = "stable",
            name = "Stable",
            provider = "Test",
            inputPricePerMillion = 1.0,
            outputPricePerMillion = 2.0,
            previousInputPricePerMillion = 1.0,
            previousOutputPricePerMillion = 2.0
        )
        assertNull(stable.priceChangePercent)
        assertFalse(stable.hasPriceChange)

        val firstSeen = AiModel(
            id = "first",
            name = "First",
            provider = "Test",
            inputPricePerMillion = 1.0
        )
        assertNull(firstSeen.priceChangePercent)
    }

    @Test
    fun biggestPriceDrops_returnsSteepestDropsFirst() {
        fun priced(id: String, current: Double, previous: Double) = AiModel(
            id = id,
            name = id,
            provider = "Test",
            inputPricePerMillion = current,
            outputPricePerMillion = current,
            previousInputPricePerMillion = previous,
            previousOutputPricePerMillion = previous
        )

        val models = listOf(
            priced("flat", 1.0, 1.0),
            priced("small", 0.95, 1.0),
            priced("big", 0.5, 1.0)
        )

        assertEquals(
            listOf("big", "small"),
            LeaderboardUseCases.biggestPriceDrops(models).map { it.id }
        )
    }

    @Test
    fun relativeTime_formatsFreshnessBuckets() {
        val now = 1_000_000_000L

        assertEquals("just now", LeaderboardUseCases.relativeTime(now - 10_000L, now))
        assertEquals("5m ago", LeaderboardUseCases.relativeTime(now - 5 * 60_000L, now))
        assertEquals("3h ago", LeaderboardUseCases.relativeTime(now - 3 * 3_600_000L, now))
        assertEquals("2d ago", LeaderboardUseCases.relativeTime(now - 2 * 86_400_000L, now))
        assertEquals("never", LeaderboardUseCases.relativeTime(0L, now))
    }

    @Test
    fun refine_combinesSearchProviderWatchlistAndCategory() {
        val open = AiModel(
            id = "open/cheap",
            name = "Cheap Open",
            provider = "OpenCo",
            inputPricePerMillion = 0.2,
            outputPricePerMillion = 0.4,
            overallScore = 90.0,
            codingScore = 90.0,
            reasoningScore = 90.0,
            agenticScore = 90.0,
            isOpenWeight = true
        )
        val closed = AiModel(
            id = "closed/premium",
            name = "Premium Closed",
            provider = "ClosedCo",
            inputPricePerMillion = 10.0,
            outputPricePerMillion = 30.0,
            overallScore = 95.0,
            codingScore = 96.0,
            reasoningScore = 96.0,
            agenticScore = 96.0
        )
        val models = listOf(open, closed)

        val result = LeaderboardUseCases.refine(
            models = models,
            query = "open",
            category = RankingCategory.OPEN_WEIGHT,
            mode = SortMode.OVERALL,
            provider = "OpenCo",
            watchedIds = setOf("open/cheap"),
            watchlistOnly = true
        )

        assertEquals(listOf("open/cheap"), result.map { it.id })
    }

    @Test
    fun repository_preservesPreviousPricesForDeltaDetection() = runBlocking {
        val directory = Files.createTempDirectory("arenapulse-delta").toFile()
        val cacheFile = File(directory, "leaderboard_cache.json")

        fun dto(input: String, output: String) = ModelDto(
            id = "acme/model-x",
            name = "Model X",
            contextLength = 100_000,
            pricing = PricingDto(prompt = input, completion = output)
        )

        val first = LeaderboardRepository(
            cacheFile = cacheFile,
            catalogSource = FakeCatalogSource { listOf(dto("0.000002", "0.000008")) }
        )
        first.load(forceRefresh = true)

        val second = LeaderboardRepository(
            cacheFile = cacheFile,
            catalogSource = FakeCatalogSource { listOf(dto("0.000001", "0.000004")) }
        )
        val snapshot = second.load(forceRefresh = true)
        val model = snapshot.models.single()

        assertEquals(1.0, model.inputPricePerMillion, 0.0001)
        assertEquals(2.0, model.previousInputPricePerMillion ?: 0.0, 0.0001)
        assertEquals(-50.0, model.priceChangePercent ?: 0.0, 0.0001)
    }

    private class FakeCatalogSource(
        private val block: suspend () -> List<ModelDto>
    ) : ModelCatalogSource {
        override suspend fun fetchModels(): List<ModelDto> = block()
    }
}
