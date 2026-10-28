package com.arenapulse.android.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arenapulse.android.domain.AiModel
import com.arenapulse.android.domain.LeaderboardUseCases
import com.arenapulse.android.domain.RankingCategory
import com.arenapulse.android.domain.SortMode
import com.arenapulse.android.ui.theme.ArenaBackground
import com.arenapulse.android.ui.theme.ArenaBorder
import com.arenapulse.android.ui.theme.ArenaBronze
import com.arenapulse.android.ui.theme.ArenaCyan
import com.arenapulse.android.ui.theme.ArenaDanger
import com.arenapulse.android.ui.theme.ArenaEmerald
import com.arenapulse.android.ui.theme.ArenaGold
import com.arenapulse.android.ui.theme.ArenaPurple
import com.arenapulse.android.ui.theme.ArenaSilver
import com.arenapulse.android.ui.theme.ArenaSurface
import com.arenapulse.android.ui.theme.ArenaTextMuted
import com.arenapulse.android.ui.theme.ArenaTextPrimary
import com.arenapulse.android.ui.theme.ArenaTextSecondary
import com.arenapulse.android.ui.theme.scoreColor
import com.arenapulse.android.ui.theme.tierLabel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun RankingsScreen(
    models: List<AiModel>,
    isLoading: Boolean,
    isRefreshing: Boolean,
    lastUpdated: Long,
    sourceLabel: String,
    errorMessage: String?,
    onRefresh: () -> Unit,
    onModelClick: (AiModel) -> Unit
) {
    var category by rememberSaveable { mutableStateOf(RankingCategory.ALL) }
    var sortMode by rememberSaveable { mutableStateOf(SortMode.OVERALL) }

    val displayed = remember(models, category, sortMode) {
        LeaderboardUseCases.apply(models, category, sortMode)
    }
    val podium = remember(models) {
        models.sortedByDescending { it.overallScore }.take(3)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(key = "header") {
            Column {
                Text(
                    text = "Live Rankings",
                    style = androidx.compose.material3.MaterialTheme.typography.headlineMedium,
                    color = ArenaTextPrimary
                )
                Text(
                    text = "Frontier intelligence, throughput and unit economics",
                    style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                    color = ArenaTextMuted,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }

        item(key = "live-status") {
            LiveStatusHeader(
                modelCount = models.size,
                lastUpdated = lastUpdated,
                sourceLabel = sourceLabel,
                isRefreshing = isRefreshing,
                onRefresh = onRefresh
            )
        }

        if (podium.isNotEmpty()) {
            item(key = "podium") {
                PodiumCarousel(models = podium, onModelClick = onModelClick)
            }
        }

        item(key = "filters") {
            FilterBar(selected = category, onSelect = { category = it })
        }

        item(key = "sort") {
            SortBar(selected = sortMode, onSelect = { sortMode = it })
        }

        if (errorMessage != null) {
            item(key = "error") {
                Surface(
                    color = ArenaDanger.copy(alpha = 0.10f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, ArenaDanger.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = errorMessage,
                        color = ArenaDanger,
                        style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                    )
                }
            }
        }

        if (isLoading && models.isEmpty()) {
            item(key = "loading") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = ArenaCyan, strokeWidth = 2.dp)
                }
            }
        } else if (displayed.isEmpty()) {
            item(key = "empty") {
                Surface(
                    color = ArenaSurface,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, ArenaBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "No models match this filter yet. Try another category or refresh the live sync.",
                        color = ArenaTextSecondary,
                        style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(18.dp)
                    )
                }
            }
        } else {
            itemsIndexed(
                items = displayed,
                key = { _, model -> model.id }
            ) { index, model ->
                ModelRow(
                    rank = index + 1,
                    model = model,
                    onClick = { onModelClick(model) },
                    modifier = Modifier.animateItem()
                )
            }
        }

        item(key = "footer") {
            Text(
                text = "Scores are a deterministic blend of curated arena benchmarks and live OpenRouter catalog data. Blended price weights input 75% / output 25%.",
                style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                color = ArenaTextMuted,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@Composable
private fun LiveStatusHeader(
    modelCount: Int,
    lastUpdated: Long,
    sourceLabel: String,
    isRefreshing: Boolean,
    onRefresh: () -> Unit
) {
    val transition = rememberInfiniteTransition(label = "live-pulse")
    val pulseAlpha by transition.animateFloat(
        initialValue = 0.30f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse-alpha"
    )
    val pulseScale by transition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse-scale"
    )

    val syncTime = remember(lastUpdated) {
        if (lastUpdated <= 0L) "--:--" else runCatching {
            DateTimeFormatter.ofPattern("HH:mm:ss").format(
                Instant.ofEpochMilli(lastUpdated).atZone(ZoneId.systemDefault())
            )
        }.getOrDefault("--:--")
    }

    Surface(
        color = ArenaSurface,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, ArenaBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(18.dp), contentAlignment = Alignment.Center) {
                    Box(
                        modifier = Modifier
                            .size(11.dp)
                            .graphicsLayer {
                                scaleX = pulseScale
                                scaleY = pulseScale
                                alpha = pulseAlpha
                            }
                            .background(ArenaEmerald, CircleShape)
                    )
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(ArenaEmerald, CircleShape)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "LIVE SYNC",
                    style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                    color = ArenaEmerald,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.weight(1f))
                if (isRefreshing) {
                    CircularProgressIndicator(
                        color = ArenaCyan,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(18.dp)
                    )
                } else {
                    IconButton(onClick = onRefresh, modifier = Modifier.size(28.dp)) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = "Refresh leaderboard",
                            tint = ArenaTextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(verticalAlignment = Alignment.Bottom) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = modelCount.toString(),
                        style = androidx.compose.material3.MaterialTheme.typography.headlineMedium,
                        color = ArenaTextPrimary
                    )
                    Text(
                        text = "MODELS TRACKED",
                        style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                        color = ArenaTextMuted
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = syncTime,
                        style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
                        color = ArenaTextPrimary
                    )
                    Text(
                        text = "LAST SYNC - ${sourceLabel.uppercase(Locale.US)}",
                        style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                        color = ArenaTextMuted
                    )
                }
            }
        }
    }
}

@Composable
private fun PodiumCarousel(
    models: List<AiModel>,
    onModelClick: (AiModel) -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(vertical = 2.dp)
    ) {
        itemsIndexed(items = models, key = { _, model -> model.id }) { index, model ->
            val medal = when (index) {
                0 -> ArenaGold
                1 -> ArenaSilver
                else -> ArenaBronze
            }
            PodiumCard(
                rank = index + 1,
                model = model,
                medalColor = medal,
                onClick = { onModelClick(model) }
            )
        }
    }
}

@Composable
private fun PodiumCard(
    rank: Int,
    model: AiModel,
    medalColor: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(268.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = ArenaSurface),
        border = BorderStroke(
            width = 1.5.dp,
            brush = Brush.linearGradient(
                colors = listOf(
                    medalColor.copy(alpha = 0.85f),
                    ArenaBorder,
                    medalColor.copy(alpha = 0.20f)
                )
            )
        )
    ) {
        Column(
            modifier = Modifier
                .background(
                    Brush.verticalGradient(
                        colors = listOf(medalColor.copy(alpha = 0.10f), Color.Transparent)
                    )
                )
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .background(medalColor.copy(alpha = 0.18f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "#$rank",
                        style = androidx.compose.material3.MaterialTheme.typography.labelLarge,
                        color = medalColor,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "PODIUM",
                    style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                    color = medalColor
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = tierLabel(model.tier),
                    style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                    color = scoreColor(model.overallScore)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = model.name,
                style = androidx.compose.material3.MaterialTheme.typography.titleLarge,
                color = ArenaTextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = model.provider,
                style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                color = ArenaTextSecondary
            )

            Spacer(modifier = Modifier.height(14.dp))

            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = String.format(Locale.US, "%.1f", model.overallScore),
                    style = androidx.compose.material3.MaterialTheme.typography.displaySmall,
                    color = scoreColor(model.overallScore),
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "ARENA",
                    style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                    color = ArenaTextMuted,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))
            ScoreBar(score = model.overallScore, color = medalColor)

            Spacer(modifier = Modifier.height(12.dp))

            Row {
                MetricPill(label = "IN", value = LeaderboardUseCases.formatPrice(model.inputPricePerMillion))
                Spacer(modifier = Modifier.width(8.dp))
                MetricPill(label = "OUT", value = LeaderboardUseCases.formatPrice(model.outputPricePerMillion))
                Spacer(modifier = Modifier.width(8.dp))
                MetricPill(label = "CTX", value = model.contextLabel)
            }
        }
    }
}

@Composable
private fun MetricPill(label: String, value: String) {
    Surface(
        color = ArenaBackground.copy(alpha = 0.65f),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, ArenaBorder)
    ) {
        Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)) {
            Text(
                text = label,
                style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                color = ArenaTextMuted,
                fontSize = 8.sp
            )
            Text(
                text = value,
                style = androidx.compose.material3.MaterialTheme.typography.labelLarge,
                color = ArenaTextPrimary,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
private fun FilterBar(
    selected: RankingCategory,
    onSelect: (RankingCategory) -> Unit
) {
    Column {
        Text(
            text = "CATEGORY",
            style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
            color = ArenaTextMuted
        )
        Spacer(modifier = Modifier.height(6.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(RankingCategory.entries) { category ->
                val isSelected = category == selected
                FilterChip(
                    selected = isSelected,
                    onClick = { onSelect(category) },
                    label = { Text(category.label) },
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = ArenaSurface,
                        labelColor = ArenaTextSecondary,
                        selectedContainerColor = ArenaCyan.copy(alpha = 0.16f),
                        selectedLabelColor = ArenaCyan
                    )
                )
            }
        }
    }
}

@Composable
private fun SortBar(
    selected: SortMode,
    onSelect: (SortMode) -> Unit
) {
    Column {
        Text(
            text = "SORT BY",
            style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
            color = ArenaTextMuted
        )
        Spacer(modifier = Modifier.height(6.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(SortMode.entries) { mode ->
                val isSelected = mode == selected
                AssistChip(
                    onClick = { onSelect(mode) },
                    label = { Text(mode.label) },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = if (isSelected) ArenaPurple.copy(alpha = 0.16f) else ArenaSurface,
                        labelColor = if (isSelected) ArenaPurple else ArenaTextSecondary
                    )
                )
            }
        }
    }
}

@Composable
private fun ModelRow(
    rank: Int,
    model: AiModel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = ArenaSurface),
        border = BorderStroke(1.dp, ArenaBorder)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = rank.toString().padStart(2, '0'),
                    style = androidx.compose.material3.MaterialTheme.typography.labelLarge,
                    color = ArenaTextMuted,
                    modifier = Modifier.width(26.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = model.name,
                            style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
                            color = ArenaTextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (model.isOpenWeight) {
                            Spacer(modifier = Modifier.width(6.dp))
                            TinyTag(text = "OPEN", color = ArenaEmerald)
                        }
                    }
                    Text(
                        text = "${model.provider} - ${model.contextLabel} context",
                        style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                        color = ArenaTextMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = String.format(Locale.US, "%.1f", model.overallScore),
                        style = androidx.compose.material3.MaterialTheme.typography.titleLarge,
                        color = scoreColor(model.overallScore),
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = tierLabel(model.tier),
                        style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                        color = ArenaTextMuted,
                        fontSize = 8.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            ScoreBar(score = model.overallScore, color = scoreColor(model.overallScore))
            Spacer(modifier = Modifier.height(10.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = LeaderboardUseCases.formatPrice(model.blendedPricePerMillion) + " /1M",
                    style = androidx.compose.material3.MaterialTheme.typography.labelLarge,
                    color = ArenaTextSecondary
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "${String.format(Locale.US, "%.0f", model.speedTokensPerSec)} tok/s",
                    style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                    color = ArenaTextMuted
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "VALUE " + String.format(Locale.US, "%.1f", model.valueScore),
                    style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                    color = ArenaPurple
                )
            }
        }
    }
}

@Composable
private fun TinyTag(text: String, color: Color) {
    Surface(
        color = color.copy(alpha = 0.14f),
        shape = RoundedCornerShape(4.dp)
    ) {
        Text(
            text = text,
            style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
            color = color,
            fontSize = 8.sp,
            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
        )
    }
}

@Composable
fun ScoreBar(
    score: Double,
    color: Color,
    modifier: Modifier = Modifier
) {
    val progress by animateFloatAsState(
        targetValue = (score / 100.0).toFloat().coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing),
        label = "score-bar"
    )
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(ArenaBorder)
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(progress.coerceAtLeast(0.01f))
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(color.copy(alpha = 0.45f), color)
                    )
                )
        )
    }
}
