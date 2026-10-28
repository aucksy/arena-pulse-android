package com.arenapulse.android.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arenapulse.android.domain.AiModel
import com.arenapulse.android.domain.LeaderboardUseCases
import com.arenapulse.android.ui.theme.ArenaBorder
import com.arenapulse.android.ui.theme.ArenaCyan
import com.arenapulse.android.ui.theme.ArenaEmerald
import com.arenapulse.android.ui.theme.ArenaPurple
import com.arenapulse.android.ui.theme.ArenaSurface
import com.arenapulse.android.ui.theme.ArenaTextMuted
import com.arenapulse.android.ui.theme.ArenaTextPrimary
import com.arenapulse.android.ui.theme.ArenaTextSecondary
import com.arenapulse.android.ui.theme.scoreColor
import java.util.Locale

private data class VersusMetric(
    val label: String,
    val leftValue: Double,
    val rightValue: Double,
    val leftLabel: String,
    val rightLabel: String
)

@Composable
fun VersusScreen(
    models: List<AiModel>,
    onInspect: (AiModel) -> Unit
) {
    val ranked = remember(models) { models.sortedByDescending { it.overallScore } }

    var leftId by rememberSaveable { mutableStateOf<String?>(null) }
    var rightId by rememberSaveable { mutableStateOf<String?>(null) }

    LaunchedEffect(ranked) {
        if (ranked.isEmpty()) return@LaunchedEffect
        if (leftId == null || ranked.none { it.id == leftId }) {
            leftId = ranked.first().id
        }
        if (rightId == null || ranked.none { it.id == rightId } || rightId == leftId) {
            rightId = (ranked.firstOrNull { it.id != leftId } ?: ranked.first()).id
        }
    }

    val left = ranked.firstOrNull { it.id == leftId } ?: ranked.firstOrNull()
    val right = ranked.firstOrNull { it.id == rightId } ?: ranked.getOrNull(1) ?: ranked.firstOrNull()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 28.dp)
    ) {
        Text(
            text = "Head-to-Head",
            style = MaterialTheme.typography.headlineMedium,
            color = ArenaTextPrimary
        )
        Text(
            text = "Tug-of-war across the metrics that decide production model choice",
            style = MaterialTheme.typography.bodySmall,
            color = ArenaTextMuted,
            modifier = Modifier.padding(top = 2.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (left == null || right == null) {
            Surface(
                color = ArenaSurface,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, ArenaBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Loading contenders...",
                    color = ArenaTextSecondary,
                    modifier = Modifier.padding(20.dp)
                )
            }
        } else {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ModelSelector(
                label = "CONTENDER A",
                selected = left,
                models = ranked,
                accent = ArenaCyan,
                onSelect = { picked ->
                    leftId = picked.id
                    if (rightId == picked.id) {
                        rightId = ranked.firstOrNull { it.id != picked.id }?.id
                    }
                },
                modifier = Modifier.weight(1f)
            )
            ModelSelector(
                label = "CONTENDER B",
                selected = right,
                models = ranked,
                accent = ArenaPurple,
                onSelect = { picked ->
                    rightId = picked.id
                    if (leftId == picked.id) {
                        leftId = ranked.firstOrNull { it.id != picked.id }?.id
                    }
                },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = {
                val swap = leftId
                leftId = rightId
                rightId = swap
            }) {
                Text("Swap", color = ArenaTextSecondary, style = MaterialTheme.typography.labelLarge)
            }
            Spacer(modifier = Modifier.weight(1f))
            TextButton(onClick = { onInspect(left) }) {
                Text("Inspect A", color = ArenaCyan, style = MaterialTheme.typography.labelLarge)
            }
            TextButton(onClick = { onInspect(right) }) {
                Text("Inspect B", color = ArenaPurple, style = MaterialTheme.typography.labelLarge)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = ArenaSurface),
            border = BorderStroke(
                width = 1.dp,
                brush = Brush.linearGradient(
                    listOf(ArenaCyan.copy(alpha = 0.55f), ArenaBorder, ArenaPurple.copy(alpha = 0.55f))
                )
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = left.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = ArenaTextPrimary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = left.provider,
                        style = MaterialTheme.typography.bodySmall,
                        color = ArenaTextMuted
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = String.format(Locale.US, "%.1f", left.overallScore),
                        style = MaterialTheme.typography.headlineMedium,
                        color = scoreColor(left.overallScore),
                        fontWeight = FontWeight.Bold
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "VS",
                        style = MaterialTheme.typography.labelSmall,
                        color = ArenaPurple,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(ArenaBorder, CircleShape)
                    )
                }
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = right.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = ArenaTextPrimary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = right.provider,
                        style = MaterialTheme.typography.bodySmall,
                        color = ArenaTextMuted
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = String.format(Locale.US, "%.1f", right.overallScore),
                        style = MaterialTheme.typography.headlineMedium,
                        color = scoreColor(right.overallScore),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        val metrics = listOf(
            VersusMetric(
                "CODING",
                left.codingScore,
                right.codingScore,
                formatScore(left.codingScore),
                formatScore(right.codingScore)
            ),
            VersusMetric(
                "REASONING",
                left.reasoningScore,
                right.reasoningScore,
                formatScore(left.reasoningScore),
                formatScore(right.reasoningScore)
            ),
            VersusMetric(
                "AGENTIC",
                left.agenticScore,
                right.agenticScore,
                formatScore(left.agenticScore),
                formatScore(right.agenticScore)
            ),
            VersusMetric(
                "SPEED",
                left.speedTokensPerSec,
                right.speedTokensPerSec,
                "${String.format(Locale.US, "%.0f", left.speedTokensPerSec)} tok/s",
                "${String.format(Locale.US, "%.0f", right.speedTokensPerSec)} tok/s"
            ),
            VersusMetric(
                "CONTEXT",
                left.contextLength.toDouble(),
                right.contextLength.toDouble(),
                left.contextLabel,
                right.contextLabel
            ),
            VersusMetric(
                "COST EFFICIENCY",
                LeaderboardUseCases.costEfficiency(left),
                LeaderboardUseCases.costEfficiency(right),
                LeaderboardUseCases.formatPrice(left.blendedPricePerMillion) + "/1M",
                LeaderboardUseCases.formatPrice(right.blendedPricePerMillion) + "/1M"
            )
        )

        val leftWins = metrics.count { it.leftValue > it.rightValue }
        val rightWins = metrics.count { it.rightValue > it.leftValue }

        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = ArenaSurface),
            border = BorderStroke(1.dp, ArenaBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                metrics.forEach { metric ->
                    VersusMetricRow(
                        metric = metric,
                        leftColor = ArenaCyan,
                        rightColor = ArenaPurple
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Surface(
            color = when {
                leftWins > rightWins -> ArenaCyan.copy(alpha = 0.10f)
                rightWins > leftWins -> ArenaPurple.copy(alpha = 0.10f)
                else -> ArenaSurface
            },
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(
                1.dp,
                when {
                    leftWins > rightWins -> ArenaCyan.copy(alpha = 0.45f)
                    rightWins > leftWins -> ArenaPurple.copy(alpha = 0.45f)
                    else -> ArenaBorder
                }
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "VERDICT",
                    style = MaterialTheme.typography.labelSmall,
                    color = ArenaTextMuted
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = when {
                        leftWins > rightWins -> "${left.name} takes the crown"
                        rightWins > leftWins -> "${right.name} takes the crown"
                        else -> "A dead heat across the frontier"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    color = ArenaTextPrimary
                )
                Text(
                    text = "Metric wins: $leftWins - $rightWins. Cost efficiency favors the lower blended price per 1M tokens.",
                    style = MaterialTheme.typography.bodySmall,
                    color = ArenaTextSecondary,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Surface(
                color = ArenaSurface,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, ArenaBorder),
                modifier = Modifier.weight(1f)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("A VALUE SCORE", style = MaterialTheme.typography.labelSmall, color = ArenaCyan)
                    Text(
                        String.format(Locale.US, "%.1f", left.valueScore),
                        style = MaterialTheme.typography.titleLarge,
                        color = ArenaTextPrimary
                    )
                }
            }
            Surface(
                color = ArenaSurface,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, ArenaBorder),
                modifier = Modifier.weight(1f)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("B VALUE SCORE", style = MaterialTheme.typography.labelSmall, color = ArenaPurple)
                    Text(
                        String.format(Locale.US, "%.1f", right.valueScore),
                        style = MaterialTheme.typography.titleLarge,
                        color = ArenaTextPrimary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = "Value score = arena intelligence per blended dollar. Bars show each contender's share of the stronger metric.",
            style = MaterialTheme.typography.bodySmall,
            color = ArenaTextMuted
        )
        }
    }
}

@Composable
private fun ModelSelector(
    label: String,
    selected: AiModel?,
    models: List<AiModel>,
    accent: Color,
    onSelect: (AiModel) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = ArenaTextPrimary),
            border = BorderStroke(1.dp, ArenaBorder),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = accent,
                    fontSize = 8.sp
                )
                Text(
                    text = selected?.name ?: "Select model",
                    style = MaterialTheme.typography.titleMedium,
                    color = ArenaTextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(
                imageVector = Icons.Filled.KeyboardArrowDown,
                contentDescription = "Open model list",
                tint = ArenaTextMuted,
                modifier = Modifier.size(18.dp)
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .heightIn(max = 340.dp)
                .background(ArenaSurface)
        ) {
            models.forEach { model ->
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(
                                text = model.name,
                                style = MaterialTheme.typography.titleMedium,
                                color = ArenaTextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${model.provider} - ${formatScore(model.overallScore)} arena",
                                style = MaterialTheme.typography.bodySmall,
                                color = ArenaTextMuted
                            )
                        }
                    },
                    onClick = {
                        onSelect(model)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun VersusMetricRow(
    metric: VersusMetric,
    leftColor: Color,
    rightColor: Color
) {
    val leftWins = metric.leftValue > metric.rightValue
    val rightWins = metric.rightValue > metric.leftValue
    val total = (metric.leftValue + metric.rightValue).coerceAtLeast(0.000001)
    val leftFraction = (metric.leftValue / total).toFloat().coerceIn(0.02f, 0.98f)

    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = metric.leftLabel,
                style = MaterialTheme.typography.labelLarge,
                color = if (leftWins) ArenaEmerald else ArenaTextSecondary,
                fontWeight = if (leftWins) FontWeight.Bold else FontWeight.Normal
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = metric.label,
                style = MaterialTheme.typography.labelSmall,
                color = ArenaTextMuted
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = metric.rightLabel,
                style = MaterialTheme.typography.labelLarge,
                color = if (rightWins) ArenaEmerald else ArenaTextSecondary,
                fontWeight = if (rightWins) FontWeight.Bold else FontWeight.Normal
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(9.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(ArenaBorder)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(leftFraction)
                    .background(
                        Brush.horizontalGradient(
                            listOf(leftColor.copy(alpha = 0.40f), leftColor)
                        )
                    )
            )
            Spacer(modifier = Modifier.width(2.dp))
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(1f - leftFraction)
                    .background(
                        Brush.horizontalGradient(
                            listOf(rightColor, rightColor.copy(alpha = 0.40f))
                        )
                    )
            )
        }
    }
}

private fun formatScore(score: Double): String = String.format(Locale.US, "%.1f", score)
