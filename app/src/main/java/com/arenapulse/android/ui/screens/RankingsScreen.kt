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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RankingsScreen(
    models: List<AiModel>,
    isLoading: Boolean,
    isRefreshing: Boolean,
    lastUpdated: Long,
    sourceLabel: String,
    errorMessage: String?,
    watchedIds: Set<String>,
    onRefresh: () -> Unit,
    onModelClick: (AiModel) -> Unit,
    onToggleWatch: (AiModel) -> Unit,
    onCompare: (AiModel) -> Unit,
    onOpenAbout: () -> Unit
) {
    var query by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf(RankingCategory.ALL) }
    var sortMode by rememberSaveable { mutableStateOf(SortMode.OVERALL) }
    var provider by rememberSaveable { mutableStateOf<String?>(null) }
    var watchlistOnly by rememberSaveable { mutableStateOf(false) }

    val focusManager = LocalFocusManager.current
    val nowSeconds = remember { System.currentTimeMillis() / 1_000L }
    val providerOptions = remember(models) { LeaderboardUseCases.providers(models).take(10) }

    val displayed = remember(models, query, category, sortMode, provider, watchlistOnly, watchedIds) {
        LeaderboardUseCases.refine(
            models = models,
            query = query,
            category = category,
            mode = sortMode,
            provider = provider,
            watchedIds = watchedIds,
            watchlistOnly = watchlistOnly
        )
    }
    val podium = remember(models) { models.sortedByDescending { it.overallScore }.take(3) }
    val newArrivalIds = remember(models, nowSeconds) {
        models.filter { LeaderboardUseCases.isNewArrival(it, nowSeconds) }.map { it.id }.toSet()
    }

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = Modifier.fillMaxSize()
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(key = "header") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Live Rankings",
                            style = MaterialTheme.typography.headlineMedium,
                            color = ArenaTextPrimary
                        )
                        Text(
                            text = "Frontier intelligence, throughput and unit economics",
                            style = MaterialTheme.typography.bodySmall,
                            color = ArenaTextMuted,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                    IconButton(onClick = onOpenAbout) {
                        Icon(
                            imageVector = Icons.Filled.Settings,
                            contentDescription = "About ArenaPulse and data settings",
                            tint = ArenaTextSecondary
                        )
                    }
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

            if (podium.isNotEmpty() && query.isBlank() && !watchlistOnly && provider == null) {
                item(key = "podium") {
                    PodiumCarousel(models = podium, onModelClick = onModelClick)
                }
            }

            item(key = "search") {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics { contentDescription = "Search models by name, provider or slug" },
                    placeholder = {
                        Text(
                            text = "Search ${models.size} models",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = null,
                            tint = ArenaTextMuted
                        )
                    },
                    trailingIcon = {
                        if (query.isNotEmpty()) {
                            IconButton(onClick = { query = "" }) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = "Clear search",
                                    tint = ArenaTextSecondary
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = ArenaTextPrimary,
                        unfocusedTextColor = ArenaTextPrimary,
                        focusedContainerColor = ArenaSurface,
                        unfocusedContainerColor = ArenaSurface,
                        cursorColor = ArenaCyan,
                        focusedBorderColor = ArenaCyan.copy(alpha = 0.6f),
                        unfocusedBorderColor = ArenaBorder,
                        focusedPlaceholderColor = ArenaTextMuted,
                        unfocusedPlaceholderColor = ArenaTextMuted
                    )
                )
            }

            item(key = "filters") {
                FilterBar(
                    selected = category,
                    onSelect = { category = it },
                    watchlistOnly = watchlistOnly,
                    watchedCount = watchedIds.size,
                    onToggleWatchlist = { watchlistOnly = !watchlistOnly }
                )
            }

            item(key = "sort") {
                SortBar(selected = sortMode, onSelect = { sortMode = it })
            }

            if (providerOptions.size > 1) {
                item(key = "providers") {
                    ProviderBar(
                        providers = providerOptions,
                        selected = provider,
                        onSelect = { provider = if (provider == it) null else it }
                    )
                }
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
                            style = MaterialTheme.typography.bodySmall,
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
                    EmptyState(
                        query = query,
                        watchlistOnly = watchlistOnly,
                        provider = provider,
                        onClear = {
                            query = ""
                            watchlistOnly = false
                            provider = null
                            category = RankingCategory.ALL
                        }
                    )
                }
            } else {
                itemsIndexed(
                    items = displayed,
                    key = { _, model -> model.id }
                ) { index, model ->
                    ModelRow(
                        rank = index + 1,
                        model = model,
                        isWatched = model.id in watchedIds,
                        isNew = model.id in newArrivalIds,
                        onClick = { onModelClick(model) },
                        onToggleWatch = { onToggleWatch(model) },
                        onCompare = { onCompare(model) },
                        modifier = Modifier.animateItem()
                    )
                }
            }

            item(key = "footer") {
                Text(
                    text = "Scores blend a curated arena benchmark registry with live OpenRouter catalog data. " +
                        "Blended price weights input 75% / output 25%. Pull down to re-sync.",
                    style = MaterialTheme.typography.bodySmall,
                    color = ArenaTextMuted,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun EmptyState(
    query: String,
    watchlistOnly: Boolean,
    provider: String?,
    onClear: () -> Unit
) {
    Surface(
        color = ArenaSurface,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, ArenaBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = when {
                    watchlistOnly -> "Your watchlist is empty"
                    query.isNotBlank() -> "No models match \"$query\""
                    provider != null -> "No $provider models match these filters"
                    else -> "No models match these filters"
                },
                style = MaterialTheme.typography.titleMedium,
                color = ArenaTextPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (watchlistOnly) {
                    "Tap the star on any model to pin it here for quick access."
                } else {
                    "Try a shorter search term, a different category, or clear the active filters."
                },
                style = MaterialTheme.typography.bodySmall,
                color = ArenaTextSecondary
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Clear filters",
                style = MaterialTheme.typography.labelLarge,
                color = ArenaCyan,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onClear)
                    .padding(horizontal = 4.dp, vertical = 6.dp)
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
    val relative = remember(lastUpdated) {
        LeaderboardUseCases.relativeTime(lastUpdated)
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
                    style = MaterialTheme.typography.labelSmall,
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
                    IconButton(onClick = onRefresh, modifier = Modifier.size(32.dp)) {
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
                        style = MaterialTheme.typography.headlineMedium,
                        color = ArenaTextPrimary
                    )
                    Text(
                        text = "MODELS TRACKED",
                        style = MaterialTheme.typography.labelSmall,
                        color = ArenaTextMuted
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = syncTime,
                        style = MaterialTheme.typography.titleMedium,
                        color = ArenaTextPrimary
                    )
                    Text(
                        text = "$relative \u00B7 ${sourceLabel.uppercase(Locale.US)}",
                        style = MaterialTheme.typography.labelSmall,
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
            .clickable(onClickLabel = "Open ${model.name}", onClick = onClick),
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
                        style = MaterialTheme.typography.labelLarge,
                        color = medalColor,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "PODIUM",
                    style = MaterialTheme.typography.labelSmall,
                    color = medalColor
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = tierLabel(model.tier),
                    style = MaterialTheme.typography.labelSmall,
                    color = scoreColor(model.overallScore)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = model.name,
                style = MaterialTheme.typography.titleLarge,
                color = ArenaTextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = model.provider,
                style = MaterialTheme.typography.bodySmall,
                color = ArenaTextSecondary
            )

            Spacer(modifier = Modifier.height(14.dp))

            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = String.format(Locale.US, "%.1f", model.overallScore),
                    style = MaterialTheme.typography.displaySmall,
                    color = scoreColor(model.overallScore),
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "ARENA",
                    style = MaterialTheme.typography.labelSmall,
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
                style = MaterialTheme.typography.labelSmall,
                color = ArenaTextMuted,
                fontSize = 9.sp
            )
            Text(
                text = value,
                style = MaterialTheme.typography.labelLarge,
                color = ArenaTextPrimary,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
private fun FilterBar(
    selected: RankingCategory,
    onSelect: (RankingCategory) -> Unit,
    watchlistOnly: Boolean,
    watchedCount: Int,
    onToggleWatchlist: () -> Unit
) {
    Column {
        Text(
            text = "CATEGORY",
            style = MaterialTheme.typography.labelSmall,
            color = ArenaTextMuted
        )
        Spacer(modifier = Modifier.height(6.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item(key = "watchlist") {
                FilterChip(
                    selected = watchlistOnly,
                    onClick = onToggleWatchlist,
                    label = { Text(if (watchedCount > 0) "Watchlist $watchedCount" else "Watchlist") },
                    leadingIcon = {
                        Icon(
                            imageVector = if (watchlistOnly) Icons.Filled.Star else Icons.Filled.StarBorder,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp)
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = ArenaSurface,
                        labelColor = ArenaTextSecondary,
                        iconColor = ArenaTextMuted,
                        selectedContainerColor = ArenaGold.copy(alpha = 0.16f),
                        selectedLabelColor = ArenaGold,
                        selectedLeadingIconColor = ArenaGold
                    )
                )
            }
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
            style = MaterialTheme.typography.labelSmall,
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
private fun ProviderBar(
    providers: List<String>,
    selected: String?,
    onSelect: (String) -> Unit
) {
    Column {
        Text(
            text = "PROVIDER",
            style = MaterialTheme.typography.labelSmall,
            color = ArenaTextMuted
        )
        Spacer(modifier = Modifier.height(6.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(providers) { provider ->
                val isSelected = provider == selected
                AssistChip(
                    onClick = { onSelect(provider) },
                    label = { Text(provider, maxLines = 1) },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = if (isSelected) ArenaEmerald.copy(alpha = 0.16f) else ArenaSurface,
                        labelColor = if (isSelected) ArenaEmerald else ArenaTextSecondary
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
    isWatched: Boolean,
    isNew: Boolean,
    onClick: () -> Unit,
    onToggleWatch: () -> Unit,
    onCompare: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClickLabel = "Open ${model.name}", onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = ArenaSurface),
        border = BorderStroke(1.dp, if (isWatched) ArenaGold.copy(alpha = 0.35f) else ArenaBorder)
    ) {
        Column(modifier = Modifier.padding(start = 14.dp, end = 8.dp, top = 14.dp, bottom = 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = rank.toString().padStart(2, '0'),
                    style = MaterialTheme.typography.labelLarge,
                    color = ArenaTextMuted,
                    modifier = Modifier.width(26.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = model.name,
                            style = MaterialTheme.typography.titleMedium,
                            color = ArenaTextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (isNew) {
                            Spacer(modifier = Modifier.width(6.dp))
                            TinyTag(text = "NEW", color = ArenaCyan)
                        }
                        if (model.isOpenWeight) {
                            Spacer(modifier = Modifier.width(6.dp))
                            TinyTag(text = "OPEN", color = ArenaEmerald)
                        }
                    }
                    Text(
                        text = "${model.provider} \u00B7 ${model.contextLabel} context",
                        style = MaterialTheme.typography.bodySmall,
                        color = ArenaTextMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = String.format(Locale.US, "%.1f", model.overallScore),
                        style = MaterialTheme.typography.titleLarge,
                        color = scoreColor(model.overallScore),
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = tierLabel(model.tier),
                        style = MaterialTheme.typography.labelSmall,
                        color = ArenaTextMuted,
                        fontSize = 9.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            ScoreBar(
                score = model.overallScore,
                color = scoreColor(model.overallScore),
                modifier = Modifier.padding(end = 6.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = LeaderboardUseCases.formatPrice(model.blendedPricePerMillion) + " /1M",
                    style = MaterialTheme.typography.labelLarge,
                    color = ArenaTextSecondary
                )
                model.priceChangePercent?.let { percent ->
                    Spacer(modifier = Modifier.width(6.dp))
                    TinyTag(
                        text = LeaderboardUseCases.formatPriceChange(percent),
                        color = if (percent < 0) ArenaEmerald else ArenaDanger
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "${String.format(Locale.US, "%.0f", model.speedTokensPerSec)} tok/s",
                    style = MaterialTheme.typography.bodySmall,
                    color = ArenaTextMuted
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "VALUE " + String.format(Locale.US, "%.1f", model.valueScore),
                    style = MaterialTheme.typography.labelSmall,
                    color = ArenaPurple
                )
                IconButton(onClick = onToggleWatch, modifier = Modifier.size(34.dp)) {
                    Icon(
                        imageVector = if (isWatched) Icons.Filled.Star else Icons.Filled.StarBorder,
                        contentDescription = if (isWatched) {
                            "Remove ${model.name} from watchlist"
                        } else {
                            "Add ${model.name} to watchlist"
                        },
                        tint = if (isWatched) ArenaGold else ArenaTextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(onClick = onCompare, modifier = Modifier.size(34.dp)) {
                    Icon(
                        imageVector = Icons.Filled.CompareArrows,
                        contentDescription = "Compare ${model.name} head-to-head",
                        tint = ArenaTextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }
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
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontSize = 9.sp,
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
            .semantics { contentDescription = "Arena score ${String.format(Locale.US, "%.1f", score)} out of 100" }
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
