package com.arenapulse.android.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.arenapulse.android.ui.components.RadarChartCanvas
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelDetailSheet(
    model: AiModel,
    rank: Int,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var dailyInputMillions by remember(model.id) { mutableStateOf(5f) }
    var dailyOutputMillions by remember(model.id) { mutableStateOf(1f) }

    val dailyInputTokens = (dailyInputMillions * 1_000_000f).toLong()
    val dailyOutputTokens = (dailyOutputMillions * 1_000_000f).toLong()
    val dailyCost = LeaderboardUseCases.dailyCost(model, dailyInputTokens, dailyOutputTokens)
    val monthlyCost = LeaderboardUseCases.monthlyCost(model, dailyInputTokens, dailyOutputTokens)
    val animatedMonthly by animateFloatAsState(
        targetValue = monthlyCost.toFloat(),
        animationSpec = tween(durationMillis = 280),
        label = "monthly-cost"
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = ArenaSurface,
        contentColor = ArenaTextPrimary,
        dragHandle = { BottomSheetDefaults.DragHandle(color = ArenaBorder) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "#$rank  ${model.provider.uppercase(Locale.US)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = ArenaTextMuted
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = model.name,
                        style = MaterialTheme.typography.headlineSmall,
                        color = ArenaTextPrimary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = model.id,
                        style = MaterialTheme.typography.bodySmall,
                        color = ArenaTextMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Close inspector",
                        tint = ArenaTextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Badge(
                    text = if (model.isOpenWeight) "OPEN WEIGHT" else "CLOSED",
                    color = if (model.isOpenWeight) ArenaEmerald else ArenaPurple
                )
                if (model.supportsReasoning) Badge("REASONING", ArenaCyan)
                if (model.supportsTools) Badge("TOOLS", ArenaGold)
                if (model.supportsStructuredOutputs) Badge("JSON", ArenaTextSecondary)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = ArenaSurface),
                border = BorderStroke(1.dp, ArenaBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "CAPABILITY RADAR",
                        style = MaterialTheme.typography.labelSmall,
                        color = ArenaTextMuted
                    )
                    RadarChartCanvas(
                        model = model,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(280.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DetailMetricTile(
                    label = "ARENA",
                    value = String.format(Locale.US, "%.1f", model.overallScore),
                    progress = (model.overallScore / 100.0).toFloat(),
                    color = scoreColor(model.overallScore),
                    modifier = Modifier.weight(1f)
                )
                DetailMetricTile(
                    label = "CODING",
                    value = String.format(Locale.US, "%.1f", model.codingScore),
                    progress = (model.codingScore / 100.0).toFloat(),
                    color = ArenaCyan,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DetailMetricTile(
                    label = "REASONING",
                    value = String.format(Locale.US, "%.1f", model.reasoningScore),
                    progress = (model.reasoningScore / 100.0).toFloat(),
                    color = ArenaPurple,
                    modifier = Modifier.weight(1f)
                )
                DetailMetricTile(
                    label = "AGENTIC",
                    value = String.format(Locale.US, "%.1f", model.agenticScore),
                    progress = (model.agenticScore / 100.0).toFloat(),
                    color = ArenaEmerald,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DetailMetricTile(
                    label = "SPEED",
                    value = String.format(Locale.US, "%.0f tok/s", model.speedTokensPerSec),
                    progress = (model.speedTokensPerSec / 350.0).toFloat(),
                    color = ArenaGold,
                    modifier = Modifier.weight(1f)
                )
                DetailMetricTile(
                    label = "CONTEXT",
                    value = model.contextLabel,
                    progress = (model.contextLength / 2_000_000.0).toFloat(),
                    color = ArenaTextSecondary,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Surface(
                color = ArenaCyan.copy(alpha = 0.08f),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, ArenaCyan.copy(alpha = 0.25f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "UNIT ECONOMICS",
                        style = MaterialTheme.typography.labelSmall,
                        color = ArenaCyan
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = "IN " + LeaderboardUseCases.formatPrice(model.inputPricePerMillion) +
                            "  /  OUT " + LeaderboardUseCases.formatPrice(model.outputPricePerMillion) +
                            "  /  BLEND " + LeaderboardUseCases.formatPrice(model.blendedPricePerMillion),
                        style = MaterialTheme.typography.bodySmall,
                        color = ArenaTextPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "API COST SIMULATOR",
                style = MaterialTheme.typography.labelSmall,
                color = ArenaTextMuted
            )
            Text(
                text = "Project a 30-day bill from daily token volume",
                style = MaterialTheme.typography.bodySmall,
                color = ArenaTextSecondary,
                modifier = Modifier.padding(top = 2.dp)
            )

            Spacer(modifier = Modifier.height(14.dp))

            SliderRow(
                label = "DAILY INPUT TOKENS",
                valueLabel = LeaderboardUseCases.formatCompactNumber(dailyInputTokens.toDouble()),
                sliderValue = dailyInputMillions,
                valueRange = 0.1f..50f,
                onValueChange = { dailyInputMillions = it },
                accent = ArenaCyan
            )

            Spacer(modifier = Modifier.height(12.dp))

            SliderRow(
                label = "DAILY OUTPUT TOKENS",
                valueLabel = LeaderboardUseCases.formatCompactNumber(dailyOutputTokens.toDouble()),
                sliderValue = dailyOutputMillions,
                valueRange = 0.05f..20f,
                onValueChange = { dailyOutputMillions = it },
                accent = ArenaPurple
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CostTile(
                    label = "PROJECTED / DAY",
                    value = LeaderboardUseCases.formatCost(dailyCost),
                    accent = ArenaCyan,
                    modifier = Modifier.weight(1f)
                )
                CostTile(
                    label = "PROJECTED / MONTH",
                    value = LeaderboardUseCases.formatCost(animatedMonthly.toDouble()),
                    accent = ArenaEmerald,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Value score " + String.format(Locale.US, "%.1f", model.valueScore) +
                    " arena points per blended dollar. Inputs: $" +
                    String.format(Locale.US, "%.2f", model.inputPricePerMillion) +
                    "/1M, outputs: $" + String.format(Locale.US, "%.2f", model.outputPricePerMillion) +
                    "/1M. Monthly projection assumes 30 days.",
                style = MaterialTheme.typography.bodySmall,
                color = ArenaTextMuted
            )

            Spacer(modifier = Modifier.height(12.dp))

            TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                Text("Close inspector", color = ArenaCyan)
            }
        }
    }
}

@Composable
private fun Badge(text: String, color: Color) {
    Surface(
        color = color.copy(alpha = 0.14f),
        shape = RoundedCornerShape(5.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontSize = 8.sp,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
        )
    }
}

@Composable
private fun DetailMetricTile(
    label: String,
    value: String,
    progress: Float,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = ArenaSurface,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, ArenaBorder),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = ArenaTextMuted,
                fontSize = 8.sp
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                color = color,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .background(ArenaBorder, RoundedCornerShape(2.dp))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress.coerceIn(0.02f, 1f))
                        .height(4.dp)
                        .background(color, RoundedCornerShape(2.dp))
                )
            }
        }
    }
}

@Composable
private fun SliderRow(
    label: String,
    valueLabel: String,
    sliderValue: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    accent: Color
) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = ArenaTextMuted
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = valueLabel,
                style = MaterialTheme.typography.labelLarge,
                color = accent
            )
        }
        Slider(
            value = sliderValue,
            onValueChange = onValueChange,
            valueRange = valueRange,
            colors = SliderDefaults.colors(
                thumbColor = accent,
                activeTrackColor = accent,
                inactiveTrackColor = ArenaBorder
            )
        )
    }
}

@Composable
private fun CostTile(
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
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(accent, CircleShape)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = ArenaTextMuted,
                    fontSize = 8.sp
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                color = accent,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
