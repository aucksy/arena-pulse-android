package com.arenapulse.android

import com.arenapulse.android.data.OpenRouterApiClient
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException

/**
 * Exercises the real OkHttp call path against a local server, so request
 * construction, status handling and JSON decoding are all covered together.
 */
class OpenRouterApiClientTest {

    private lateinit var server: MockWebServer

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    private fun client() = OpenRouterApiClient(
        endpoint = server.url("/api/v1/models").toString(),
        userAgent = "ArenaPulse/test (Android)"
    )

    @Test
    fun fetchModels_parsesPricingContextAndCapabilities() = runBlocking {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(SAMPLE_PAYLOAD)
        )

        val models = client().fetchModels()

        assertEquals(2, models.size)

        val pro = models.first { it.id == "openai/gpt-6.1-terra-pro" }
        assertEquals("OpenAI: GPT-6.1 Terra Pro", pro.name)
        assertEquals(1_050_000, pro.contextLength)
        assertEquals(1.0, pro.pricing.prompt.toDouble() * 1_000_000.0, 0.0001)
        assertEquals("text+image+file->text", pro.architecture.modality)
        assertTrue(pro.supportedParameters.contains("reasoning"))
        assertEquals("2026-11-11", pro.expirationDate)
        assertEquals("0.00000005", pro.pricing.inputCacheRead)
        assertEquals(1, pro.pricing.overrides.size)
        assertEquals(272_000, pro.pricing.overrides.first().minPromptTokens)

        val batch = models.first { it.id == "openai/gpt-6.1-terra-pro:batch" }
        assertNull(batch.expirationDate)
        assertTrue(batch.pricing.overrides.isEmpty())
    }

    @Test
    fun fetchModels_sendsAcceptAndUserAgentHeaders() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(200).setBody("""{"data":[]}"""))

        client().fetchModels()

        val request = server.takeRequest()
        assertEquals("GET", request.method)
        assertEquals("/api/v1/models", request.path)
        assertEquals("application/json", request.getHeader("Accept"))
        assertEquals("ArenaPulse/test (Android)", request.getHeader("User-Agent"))
    }

    @Test(expected = IOException::class)
    fun fetchModels_throwsOnServerError(): Unit = runBlocking {
        server.enqueue(MockResponse().setResponseCode(500).setBody("boom"))
        client().fetchModels()
        Unit
    }

    @Test(expected = IOException::class)
    fun fetchModels_throwsOnEmptyBody(): Unit = runBlocking {
        server.enqueue(MockResponse().setResponseCode(200).setBody(""))
        client().fetchModels()
        Unit
    }

    @Test
    fun fetchModels_toleratesUnknownFieldsAndMissingSections() = runBlocking {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(
                    """
                    {"data":[{"id":"acme/minimal","name":"Minimal","brand_new_field":true,
                    "pricing":{"prompt":"0","completion":"0"},"architecture":{}}]}
                    """.trimIndent()
                )
        )

        val models = client().fetchModels()

        assertEquals(1, models.size)
        assertEquals("acme/minimal", models.single().id)
        assertEquals(0, models.single().contextLength)
    }

    private companion object {
        val SAMPLE_PAYLOAD = """
            {
              "data": [
                {
                  "id": "openai/gpt-6.1-terra-pro",
                  "name": "OpenAI: GPT-6.1 Terra Pro",
                  "created": 1793038556,
                  "context_length": 1050000,
                  "architecture": { "modality": "text+image+file->text" },
                  "pricing": {
                    "prompt": "0.000001",
                    "completion": "0.000005",
                    "input_cache_read": "0.00000005",
                    "overrides": [
                      { "min_prompt_tokens": 272000, "prompt": "0.000002", "completion": "0.0000075" }
                    ]
                  },
                  "supported_parameters": ["tools", "reasoning", "structured_outputs"],
                  "expiration_date": "2026-11-11"
                },
                {
                  "id": "openai/gpt-6.1-terra-pro:batch",
                  "name": "OpenAI: GPT-6.1 Terra Pro (batch)",
                  "created": 1793038556,
                  "context_length": 1050000,
                  "architecture": { "modality": "text+image+file->text" },
                  "pricing": { "prompt": "0.0000005", "completion": "0.0000025" },
                  "supported_parameters": ["tools"]
                }
              ]
            }
        """.trimIndent()
    }
}
