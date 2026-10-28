package com.arenapulse.android

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import com.arenapulse.android.domain.AiModel
import com.arenapulse.android.ui.screens.FrontierChartScreen
import com.arenapulse.android.ui.screens.InsightsScreen
import com.arenapulse.android.ui.screens.RankingsScreen
import com.arenapulse.android.ui.screens.VersusScreen
import com.arenapulse.android.ui.theme.ArenaPulseTheme
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Runtime smoke tests: every screen is composed for real with realistic catalog
 * data. This catches constraint crashes, missing resources and broken state
 * hoisting that pure JVM logic tests cannot see.
 *
 * The screens run infinite "live sync" pulses, so the test clock is frozen and
 * assertions are made on the settled first frame.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w411dp-h1600dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ScreenRenderTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val nowSeconds = System.currentTimeMillis() / 1_000L

    private val models = listOf(
        AiModel(
            id = "openai/gpt-6.1-terra-pro",
            name = "GPT-6.1 Terra Pro",
            provider = "OpenAI",
            created = nowSeconds - 3 * 86_400L,
            contextLength = 1_050_000,
            inputPricePerMillion = 1.0,
            outputPricePerMillion = 5.0,
            modality = "text+image+file->text",
            supportsReasoning = true,
            supportsTools = true,
            overallScore = 96.5,
            codingScore = 97.0,
            reasoningScore = 96.0,
            agenticScore = 95.0,
            speedTokensPerSec = 88.0,
            cacheReadPricePerMillion = 0.05,
            overrideThresholdTokens = 272_000,
            overrideInputPricePerMillion = 2.0,
            overrideOutputPricePerMillion = 7.5,
            expiresAt = nowSeconds + 5 * 86_400L
        ),
        AiModel(
            id = "qwen/qwen4-max",
            name = "Qwen4 Max",
            provider = "Alibaba Qwen",
            created = nowSeconds - 40 * 86_400L,
            contextLength = 262_144,
            inputPricePerMillion = 0.4,
            outputPricePerMillion = 1.6,
            overallScore = 92.0,
            codingScore = 93.0,
            reasoningScore = 91.0,
            agenticScore = 90.0,
            speedTokensPerSec = 210.0,
            isOpenWeight = true
        ),
        AiModel(
            id = "acme/mini-free",
            name = "Mini Free",
            provider = "Acme",
            contextLength = 32_768,
            inputPricePerMillion = 0.0,
            outputPricePerMillion = 0.0,
            overallScore = 74.0,
            codingScore = 70.0,
            reasoningScore = 72.0,
            agenticScore = 68.0,
            speedTokensPerSec = 300.0
        ),
        AiModel(
            id = "openai/gpt-6.1-terra-pro:batch",
            name = "GPT-6.1 Terra Pro (batch)",
            provider = "OpenAI",
            contextLength = 1_050_000,
            inputPricePerMillion = 0.5,
            outputPricePerMillion = 2.5,
            overallScore = 96.5,
            codingScore = 97.0,
            reasoningScore = 96.0,
            agenticScore = 95.0,
            speedTokensPerSec = 20.0
        )
    )

    private fun render(content: @Composable () -> Unit) {
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent { ArenaPulseTheme { content() } }
        composeRule.waitForIdle()
    }

    private fun assertVisible(text: String) {
        assertTrue(
            "Expected to find \"$text\" on screen",
            composeRule.onAllNodesWithText(text, substring = true).fetchSemanticsNodes().isNotEmpty()
        )
    }

    private fun assertNotVisible(text: String) {
        assertFalse(
            "Did not expect to find \"$text\" on screen",
            composeRule.onAllNodesWithText(text, substring = true).fetchSemanticsNodes().isNotEmpty()
        )
    }

    @Test
    fun rankings_rendersPodiumRowsAndTags() {
        render {
            RankingsScreen(
                models = models,
                isLoading = false,
                isRefreshing = false,
                lastUpdated = System.currentTimeMillis(),
                sourceLabel = "live",
                errorMessage = null,
                watchedIds = setOf("qwen/qwen4-max"),
                includeVariants = false,
                variantCount = 1,
                onRefresh = {},
                onModelClick = {},
                onToggleWatch = {},
                onCompare = {},
                onToggleVariants = {},
                onOpenAbout = {}
            )
        }

        assertVisible("Live Rankings")
        assertVisible("MODELS TRACKED")
        assertVisible("GPT-6.1 Terra Pro")
        assertVisible("Qwen4 Max")
        assertVisible("RETIRING")
        assertVisible("BATCH")
        assertVisible("batch/free variants hidden")
    }

    @Test
    fun rankings_searchNarrowsTheList() {
        render {
            RankingsScreen(
                models = models,
                isLoading = false,
                isRefreshing = false,
                lastUpdated = System.currentTimeMillis(),
                sourceLabel = "live",
                errorMessage = null,
                watchedIds = emptySet(),
                includeVariants = false,
                variantCount = 1,
                onRefresh = {},
                onModelClick = {},
                onToggleWatch = {},
                onCompare = {},
                onToggleVariants = {},
                onOpenAbout = {}
            )
        }

        assertVisible("Qwen4 Max")

        composeRule.onNode(hasSetTextAction()).performTextReplacement("qwen")
        // The clock is frozen for the live-sync pulse, so drive the recomposition frame manually.
        composeRule.mainClock.advanceTimeByFrame()
        composeRule.waitForIdle()

        assertVisible("Qwen4 Max")
        assertNotVisible("GPT-6.1 Terra Pro")
    }

    @Test
    fun rankings_variantToggleInvokesCallback() {
        var requested: Boolean? = null
        render {
            RankingsScreen(
                models = models,
                isLoading = false,
                isRefreshing = false,
                lastUpdated = System.currentTimeMillis(),
                sourceLabel = "live",
                errorMessage = null,
                watchedIds = emptySet(),
                includeVariants = false,
                variantCount = 1,
                onRefresh = {},
                onModelClick = {},
                onToggleWatch = {},
                onCompare = {},
                onToggleVariants = { requested = it },
                onOpenAbout = {}
            )
        }

        composeRule.onNodeWithText("Show").performClick()
        composeRule.waitForIdle()

        assertTrue("Variant toggle should request variants to be shown", requested == true)
    }

    @Test
    fun versus_rendersMetricsAndVerdict() {
        render {
            VersusScreen(
                models = models,
                leftId = "openai/gpt-6.1-terra-pro",
                rightId = "qwen/qwen4-max",
                onSelectLeft = {},
                onSelectRight = {},
                onSwap = {},
                onInspect = {}
            )
        }

        assertVisible("Head-to-Head")
        assertVisible("VERDICT")
        assertVisible("CODING")
        assertVisible("GPT-6.1 Terra Pro")
        assertVisible("Qwen4 Max")
    }

    @Test
    fun signals_rendersLeaderboardSections() {
        render {
            InsightsScreen(
                models = models,
                watchedIds = setOf("qwen/qwen4-max"),
                onInspect = {}
            )
        }

        assertVisible("Market Signals")
        assertVisible("BEST IN CLASS")
        assertVisible("NEW ARRIVALS")
        assertVisible("RETIRING SOON")
        assertVisible("YOUR WATCHLIST")
        assertVisible("Qwen4 Max")
    }

    @Test
    fun matrix_rendersPlotAndParetoLeaders() {
        render {
            FrontierChartScreen(
                models = models,
                watchedIds = setOf("qwen/qwen4-max"),
                onInspect = {}
            )
        }

        assertVisible("Pareto Value Matrix")
        assertVisible("PLOTTING 4 OF 4")
        assertVisible("PARETO LEADERS")
    }
}
