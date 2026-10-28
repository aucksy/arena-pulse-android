package com.arenapulse.android.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit

/** Abstraction over the live catalog so the repository is trivially testable. */
interface ModelCatalogSource {
    suspend fun fetchModels(): List<ModelDto>
}

/**
 * Unauthenticated OpenRouter catalog client. No API key is required for the
 * public model list, which keeps ArenaPulse useful out of the box.
 */
class OpenRouterApiClient(
    private val httpClient: OkHttpClient = defaultClient(),
    private val json: Json = ArenaJson,
    private val endpoint: String = ENDPOINT
) : ModelCatalogSource {

    override suspend fun fetchModels(): List<ModelDto> = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(endpoint)
            .header("Accept", "application/json")
            .header("User-Agent", "ArenaPulse/1.0 (Android)")
            .get()
            .build()

        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("OpenRouter returned HTTP ${response.code}")
            }
            val body = response.body?.string() ?: throw IOException("Empty response body")
            decodeModels(body, json)
        }
    }

    companion object {
        const val ENDPOINT = "https://openrouter.ai/api/v1/models"

        fun defaultClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(25, TimeUnit.SECONDS)
            .callTimeout(35, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }
}
