package com.arenapulse.android.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arenapulse.android.data.FrontierScoresRegistry
import com.arenapulse.android.domain.AiModel
import com.arenapulse.android.domain.LeaderboardUseCases
import com.arenapulse.android.ui.theme.ArenaBorder
import com.arenapulse.android.ui.theme.ArenaCyan
import com.arenapulse.android.ui.theme.ArenaEmerald
import com.arenapulse.android.ui.theme.ArenaGold
import com.arenapulse.android.ui.theme.ArenaPurple
import com.arenapulse.android.ui.theme.ArenaSurface
import com.arenapulse.android.ui.theme.ArenaTextMuted
import com.arenapulse.android.ui.theme.ArenaTextPrimary
import com.arenapulse.android.ui.theme.ArenaTextSecondary
import com.arenapulse.android.ui.theme.scoreColor
import java.util.Locale

@Composable
fun InsightsScreen(
    models: List<AiModel>,
    watchedIds: Set<String>,
    onInspect: (AiModel) -> Unit
) {
    val openWeightCount = remember(models) { models.count { it.isOpenWeight } }
    val nowSeconds = remember { System.currentTimeMillis() / 1_000L }
    val newArrivals = remember(models, nowSeconds) {
        models.filter { LeaderboardUseCases.isNewArrival(it, nowSeconds) }
            .sortedByDescending { it.created }
            .take(5)
    }
    val priceDrops = remember(models) { LeaderboardUseCases.biggestPriceDrops(models, 3) }
    val watchedModels = remember(models, watchedIds) {
        models.filter { it.id in watchedIds }.sortedByDescending { it.overallScore }
    }
    val openShare = remember(models, openWeightCount) {
        if (models.isEmpty()) 0 else (openWeightCount * 100.0 / models.size).toInt()
    }
    val averageScore = remember(models) { LeaderboardUseCases.averageOverall(models) }
    val frontierCount = remember(models) { LeaderboardUseCases.paretoFrontier(models).size }

    val bestValue = remember(models) { LeaderboardUseCases.bestValue(models) }
    val fastest = remember(models) { LeaderboardUseCases.fastest(models) }
    val cheapest = remember(models) { LeaderboardUseCases.cheapest(models) }

    val bestCoding = remember(models) { models.maxByOrNull { it.codingScore } }
    val bestReasoning = remember(models) { models.maxByOrNull { it.reasoningScore } }
    val bestAgentic = remember(models) { models.maxByOrNull { it.agenticScore } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 28.dp)
    ) {
        Text(
            text = "Market Signals",
            style = MaterialTheme.typography.headlineMedium,
            color = ArenaTextPrimary
        )
        Text(
            text = "What the frontier says about cost, speed and open weights",
            style = MaterialTheme.typography.bodySmall,
            color = ArenaTextMuted,
            modifier = Modifier.padding(top = 2.dp)
        )

        Spacer(modifier = Modifier.height(14.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SignalStat(
                label = "AVG ARENA",
                value = String.format(Locale.US, "%.1f", averageScore),
                accent = ArenaCyan,
                modifier = Modifier.weight(1f)
            )
            SignalStat(
                label = "OPEN SHARE",
                value = "$openShare%",
                accent = ArenaEmerald,
                modifier = Modifier.weight(1f)
            )
            SignalStat(
                label = "PARETO",
                value = frontierCount.toString(),
                accent = ArenaPurple,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "BEST IN CLASS",
            style = MaterialTheme.typography.labelSmall,
            color = ArenaTextMuted
        )
        Spacer(modifier = Modifier.height(8.dp))

        val leaders = listOfNotNull(
            bestValue?.let {
                LeaderHighlight(
                    title = "Best value",
                    model = it,
                    metric = String.format(Locale.US, "%.1f", it.valueScore) + " pts / $",
                    accent = ArenaEmerald
                )
            },
            fastest?.let {
                LeaderHighlight(
                    title = "Fastest throughput",
                    model = it,
                    metric = String.format(Locale.US, "%.0f", it.speedTokensPerSec) + " tok/s",
                    accent = ArenaGold
                )
            },
            cheapest?.let {
                LeaderHighlight(
                    title = "Lowest blended price",
                    model = it,
                    metric = LeaderboardUseCases.formatPrice(it.blendedPricePerMillion) + " /1M",
                    accent = ArenaCyan
                )
            },
            bestCoding?.let {
                LeaderHighlight(
                    title = "Coding leader",
                    model = it,
                    metric = String.format(Locale.US, "%.1f", it.codingScore) + " coding",
                    accent = ArenaPurple
                )
            },
            bestReasoning?.let {
                LeaderHighlight(
                    title = "Reasoning leader",
                    model = it,
                    metric = String.format(Locale.US, "%.1f", it.reasoningScore) + " reasoning",
                    accent = ArenaCyan
                )
            },
            bestAgentic?.let {
                LeaderHighlight(
                    title = "Agentic leader",
                    model = it,
                    metric = String.format(Locale.US, "%.1f", it.agenticScore) + " agentic",
                    accent = ArenaEmerald
                )
            }
        )

        leaders.forEach { highlight ->
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = ArenaSurface),
                border = BorderStroke(1.dp, ArenaBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
                    .clickable { onInspect(highlight.model) }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(highlight.accent, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = highlight.title.uppercase(Locale.US),
                            style = MaterialTheme.typography.labelSmall,
                            color = ArenaTextMuted,
                            fontSize = 8.sp
                        )
                        Text(
                            text = highlight.model.name,
                            style = MaterialTheme.typography.titleMedium,
                            color = ArenaTextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = highlight.metric,
                            style = MaterialTheme.typography.labelLarge,
                            color = highlight.accent
                        )
                        Text(
                            text = String.format(Locale.US, "%.1f", highlight.model.overallScore) + " arena",
                            style = MaterialTheme.typography.bodySmall,
                            color = ArenaTextMuted
                        )
                    }
                }
            }
        }

        if (priceDrops.isNotEmpty()) {
            Spacer(modifier = Modifier.height(14.dp))
            CompactModelSection(
                title = "PRICE MOVES",
                emptyText = "",
                models = priceDrops,
                accent = ArenaEmerald,
                valueFor = { LeaderboardUseCases.formatPriceChange(it.priceChangePercent ?: 0.0) },
                onInspect = onInspect
            )
        }

        if (newArrivals.isNotEmpty()) {
            Spacer(modifier = Modifier.height(14.dp))
            CompactModelSection(
                title = "NEW ARRIVALS",
                emptyText = "",
                models = newArrivals,
                accent = ArenaCyan,
                valueFor = {
                    if (it.created > 0L) {
                        LeaderboardUseCases.relativeTime(it.created * 1_000L)
                    } else {
                        "new"
                    }
                },
                onInspect = onInspect
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "OPEN-WEIGHT WATCH",
            style = MaterialTheme.typography.labelSmall,
            color = ArenaTextMuted
        )
        Spacer(modifier = Modifier.height(8.dp))

        val openLeaders = models.filter { it.isOpenWeight }
            .sortedByDescending { it.overallScore }
            .take(5)

        Surface(
            color = ArenaSurface,
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, ArenaBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)) {
                if (openLeaders.isEmpty()) {
                    Text(
                        text = "No open-weight models in the current feed.",
                        style = MaterialTheme.typography.bodySmall,
                        color = ArenaTextSecondary,
                        modifier = Modifier.padding(vertical = 10.dp)
                    )
                } else {
                    openLeaders.forEach { model ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onInspect(model) }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = model.name,
                                style = MaterialTheme.typography.bodyMedium,
                                color = ArenaTextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = String.format(Locale.US, "%.1f", model.overallScore),
                                style = MaterialTheme.typography.labelLarge,
                                color = scoreColor(model.overallScore),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        CompactModelSection(
            title = "YOUR WATCHLIST",
            emptyText = "Star a model in Rankings to pin it here for quick access.",
            models = watchedModels,
            accent = ArenaGold,
            valueFor = { String.format(Locale.US, "%.1f", it.overallScore) },
            onInspect = onInspect
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "METHODOLOGY",
            style = MaterialTheme.typography.labelSmall,
            color = ArenaTextMuted
        )
        Spacer(modifier = Modifier.height(8.dp))

        Surface(
            color = ArenaSurface,
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, ArenaBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Live catalog + deterministic benchmark engine",
                    style = MaterialTheme.typography.titleMedium,
                    color = ArenaTextPrimary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Pricing, context windows, modality and supported parameters stream from the public OpenRouter model catalog. Arena scores come from a curated registry of ${FrontierScoresRegistry.seedModels.size} frontier models plus family heuristics for newly launched slugs, so the leaderboard stays stable between refreshes.",
                    style = MaterialTheme.typography.bodySmall,
                    color = ArenaTextSecondary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Blended price = input x 0.75 + output x 0.25. Value score = arena score / blended price. Offline-first cache lives in leaderboard_cache.json.",
                    style = MaterialTheme.typography.bodySmall,
                    color = ArenaTextMuted
                )
            }
        }
    }
}

private data class LeaderHighlight(
    val title: String,
    val model: AiModel,
    val metric: String,
    val accent: Color
)

@Composable
private fun SignalStat(
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = ArenaSurface,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, ArenaBorder),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = accent,
                fontSize = 8.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                color = ArenaTextPrimary
            )
        }
    }
}

@Composable
private fun CompactModelSection(
    title: String,
    emptyText: String,
    models: List<AiModel>,
    accent: Color,
    valueFor: (AiModel) -> String,
    onInspect: (AiModel) -> Unit
) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelSmall,
        color = ArenaTextMuted
    )
    Spacer(modifier = Modifier.height(8.dp))
    Surface(
        color = ArenaSurface,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, ArenaBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)) {
            if (models.isEmpty()) {
                Text(
                    text = emptyText,
                    style = MaterialTheme.typography.bodySmall,
                    color = ArenaTextSecondary,
                    modifier = Modifier.padding(vertical = 10.dp)
                )
            } else {
                models.forEach { model ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onInspect(model) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = model.name,
                            style = MaterialTheme.typography.bodyMedium,
                            color = ArenaTextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = valueFor(model),
                            style = MaterialTheme.typography.labelLarge,
                            color = accent,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
