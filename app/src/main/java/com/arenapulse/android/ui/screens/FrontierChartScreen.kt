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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arenapulse.android.domain.AiModel
import com.arenapulse.android.domain.LeaderboardUseCases
import com.arenapulse.android.ui.components.FrontierScatterCanvas
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
fun FrontierChartScreen(
    models: List<AiModel>,
    watchedIds: Set<String>,
    onInspect: (AiModel) -> Unit
) {
    var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
    var showAll by rememberSaveable { mutableStateOf(false) }
    val selected = models.firstOrNull { it.id == selectedId }
    // A live OpenRouter catalog can exceed 300 models; plotting all of them is noise.
    val plotModels = remember(models, showAll) {
        val ranked = models.sortedByDescending { it.overallScore }
        if (showAll) ranked else ranked.take(40)
    }
    val frontier = remember(models) { LeaderboardUseCases.paretoFrontier(models) }
    val openWeightCount = remember(models) { models.count { it.isOpenWeight } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 28.dp)
    ) {
        Text(
            text = "Pareto Value Matrix",
            style = MaterialTheme.typography.headlineMedium,
            color = ArenaTextPrimary
        )
        Text(
            text = "Intelligence versus blended cost. Tap any dot to inspect a model.",
            style = MaterialTheme.typography.bodySmall,
            color = ArenaTextMuted,
            modifier = Modifier.padding(top = 2.dp)
        )

        Spacer(modifier = Modifier.height(14.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MiniStat(
                label = "TRACKED",
                value = models.size.toString(),
                accent = ArenaCyan,
                modifier = Modifier.weight(1f)
            )
            MiniStat(
                label = "FRONTIER",
                value = frontier.size.toString(),
                accent = ArenaEmerald,
                modifier = Modifier.weight(1f)
            )
            MiniStat(
                label = "OPEN WEIGHT",
                value = openWeightCount.toString(),
                accent = ArenaPurple,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "PLOTTING ${plotModels.size} OF ${models.size}",
                style = MaterialTheme.typography.labelSmall,
                color = ArenaTextMuted
            )
            Spacer(modifier = Modifier.weight(1f))
            TextButton(onClick = { showAll = !showAll }) {
                Text(
                    text = if (showAll) "Top 40 only" else "Show all",
                    color = ArenaCyan,
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }

        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = ArenaSurface),
            border = BorderStroke(1.dp, ArenaBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                FrontierScatterCanvas(
                    models = plotModels,
                    selectedId = selectedId,
                    onSelect = { picked -> selectedId = picked?.id },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(330.dp)
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LegendDot(color = ArenaEmerald, label = "Open weight")
                    Spacer(modifier = Modifier.width(12.dp))
                    LegendDot(color = ArenaPurple, label = "Closed")
                    Spacer(modifier = Modifier.width(12.dp))
                    LegendDot(color = ArenaCyan, label = "Pareto leader")
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (selected != null) {
            val isLeader = frontier.any { it.id == selected.id }
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = ArenaSurface),
                border = BorderStroke(
                    1.dp,
                    if (isLeader) ArenaCyan.copy(alpha = 0.5f) else ArenaBorder
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = selected.name,
                                style = MaterialTheme.typography.titleLarge,
                                color = ArenaTextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${selected.provider} - ${selected.contextLabel} context",
                                style = MaterialTheme.typography.bodySmall,
                                color = ArenaTextMuted
                            )
                        }
                        if (isLeader) {
                            Surface(
                                color = ArenaCyan.copy(alpha = 0.14f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "PARETO",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ArenaCyan,
                                    fontSize = 8.sp,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                )
                            }
                        }
                        if (selected.id in watchedIds) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                color = ArenaGold.copy(alpha = 0.14f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "WATCHED",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ArenaGold,
                                    fontSize = 9.sp,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "ARENA SCORE",
                                style = MaterialTheme.typography.labelSmall,
                                color = ArenaTextMuted
                            )
                            Text(
                                String.format(Locale.US, "%.1f", selected.overallScore),
                                style = MaterialTheme.typography.headlineSmall,
                                color = scoreColor(selected.overallScore)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "BLENDED /1M",
                                style = MaterialTheme.typography.labelSmall,
                                color = ArenaTextMuted
                            )
                            Text(
                                LeaderboardUseCases.formatPrice(selected.blendedPricePerMillion),
                                style = MaterialTheme.typography.headlineSmall,
                                color = ArenaEmerald
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "SPEED",
                                style = MaterialTheme.typography.labelSmall,
                                color = ArenaTextMuted
                            )
                            Text(
                                "${String.format(Locale.US, "%.0f", selected.speedTokensPerSec)}",
                                style = MaterialTheme.typography.headlineSmall,
                                color = ArenaTextPrimary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Value score " + LeaderboardUseCases.formatValueScore(selected),
                            style = MaterialTheme.typography.bodySmall,
                            color = ArenaTextSecondary
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        TextButton(onClick = { onInspect(selected) }) {
                            Text("Open inspector", color = ArenaCyan)
                        }
                    }
                }
            }
        } else {
            Surface(
                color = ArenaSurface,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, ArenaBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Tap a plotted dot to pin its cost, score and Pareto status here.",
                    style = MaterialTheme.typography.bodySmall,
                    color = ArenaTextSecondary,
                    modifier = Modifier.padding(14.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        Text(
            text = "PARETO LEADERS",
            style = MaterialTheme.typography.labelSmall,
            color = ArenaTextMuted
        )
        Spacer(modifier = Modifier.height(8.dp))

        frontier.take(6).forEachIndexed { index, model ->
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = ArenaSurface),
                border = BorderStroke(1.dp, ArenaBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
                    .clickable { selectedId = model.id }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = (index + 1).toString().padStart(2, '0'),
                        style = MaterialTheme.typography.labelLarge,
                        color = ArenaCyan,
                        modifier = Modifier.width(28.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = model.name,
                            style = MaterialTheme.typography.titleMedium,
                            color = ArenaTextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = LeaderboardUseCases.formatPrice(model.blendedPricePerMillion) + " blended /1M",
                            style = MaterialTheme.typography.bodySmall,
                            color = ArenaTextMuted
                        )
                    }
                    Text(
                        text = String.format(Locale.US, "%.1f", model.overallScore),
                        style = MaterialTheme.typography.titleMedium,
                        color = scoreColor(model.overallScore),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        if (frontier.isEmpty()) {
            Text(
                text = "Frontier leaders will appear once the leaderboard has data.",
                style = MaterialTheme.typography.bodySmall,
                color = ArenaTextMuted
            )
        }

        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = "A model is on the frontier when no cheaper model matches or beats its intelligence. The emerald line connects those leaders.",
            style = MaterialTheme.typography.bodySmall,
            color = ArenaTextMuted
        )
    }
}

@Composable
private fun MiniStat(
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
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .background(color, CircleShape)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = ArenaTextMuted,
            fontSize = 10.sp
        )
    }
}
