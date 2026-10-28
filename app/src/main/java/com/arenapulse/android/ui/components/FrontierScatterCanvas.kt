package com.arenapulse.android.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arenapulse.android.domain.AiModel
import com.arenapulse.android.domain.LeaderboardUseCases
import com.arenapulse.android.ui.theme.ArenaBorder
import com.arenapulse.android.ui.theme.ArenaCyan
import com.arenapulse.android.ui.theme.ArenaEmerald
import com.arenapulse.android.ui.theme.ArenaPurple
import com.arenapulse.android.ui.theme.ArenaTextMuted
import com.arenapulse.android.ui.theme.ArenaTextSecondary
import java.util.Locale
import kotlin.math.log10
import kotlin.math.pow

data class ScatterBounds(
    val minLogCost: Float,
    val maxLogCost: Float,
    val minScore: Float,
    val maxScore: Float
)

private const val MIN_PLOT_COST = 0.05

fun computeScatterBounds(models: List<AiModel>): ScatterBounds {
    if (models.isEmpty()) return ScatterBounds(-1.5f, 1.5f, 60f, 100f)

    val logs = models.map {
        log10(it.blendedPricePerMillion.coerceAtLeast(MIN_PLOT_COST)).toFloat()
    }
    val scores = models.map { it.overallScore.toFloat() }

    var minLog = logs.minOrNull() ?: -1f
    var maxLog = logs.maxOrNull() ?: 1f
    if (maxLog - minLog < 0.5f) {
        minLog -= 0.25f
        maxLog += 0.25f
    }
    val logPadding = (maxLog - minLog) * 0.08f

    var minScore = (scores.minOrNull() ?: 60f) - 3f
    var maxScore = (scores.maxOrNull() ?: 100f) + 2f
    if (maxScore - minScore < 12f) {
        minScore -= 6f
        maxScore += 6f
    }

    return ScatterBounds(
        minLogCost = minLog - logPadding,
        maxLogCost = maxLog + logPadding,
        minScore = minScore.coerceAtLeast(0f),
        maxScore = maxScore.coerceAtMost(100f)
    )
}

private fun plotRect(width: Float, height: Float, padding: Float): Rect = Rect(
    left = padding * 1.9f,
    top = padding * 0.6f,
    right = width - padding * 0.7f,
    bottom = height - padding * 1.9f
)

private fun pointFor(model: AiModel, bounds: ScatterBounds, rect: Rect): Offset {
    val logCost = log10(model.blendedPricePerMillion.coerceAtLeast(MIN_PLOT_COST)).toFloat()
    val xSpan = (bounds.maxLogCost - bounds.minLogCost).coerceAtLeast(0.001f)
    val ySpan = (bounds.maxScore - bounds.minScore).coerceAtLeast(0.001f)

    val xFraction = ((logCost - bounds.minLogCost) / xSpan).coerceIn(0f, 1f)
    val yFraction = ((model.overallScore.toFloat() - bounds.minScore) / ySpan).coerceIn(0f, 1f)

    return Offset(
        x = rect.left + xFraction * rect.width,
        y = rect.bottom - yFraction * rect.height
    )
}

private fun costTick(logValue: Float): String {
    val cost = 10.0.pow(logValue.toDouble())
    return when {
        cost < 0.1 -> "$" + String.format(Locale.US, "%.2f", cost)
        cost < 1.0 -> "$" + String.format(Locale.US, "%.1f", cost)
        else -> "$" + String.format(Locale.US, "%.0f", cost)
    }
}

/**
 * Log-scale cost vs. intelligence scatter plot with the Pareto frontier
 * highlighted. Tapping a dot selects it (handled by the caller's tooltip card).
 */
@Composable
fun FrontierScatterCanvas(
    models: List<AiModel>,
    selectedId: String?,
    onSelect: (AiModel?) -> Unit,
    modifier: Modifier = Modifier
) {
    val bounds = remember(models) { computeScatterBounds(models) }
    val frontier = remember(models) { LeaderboardUseCases.paretoFrontier(models) }
    val frontierIds = remember(frontier) { frontier.map { it.id }.toSet() }

    val density = LocalDensity.current
    val paddingPx = with(density) { 24.dp.toPx() }
    val tapRadiusPx = with(density) { 28.dp.toPx() }

    Canvas(
        modifier = modifier.pointerInput(models, bounds) {
            detectTapGestures { tap ->
                if (models.isEmpty()) {
                    onSelect(null)
                    return@detectTapGestures
                }
                val rect = plotRect(size.width.toFloat(), size.height.toFloat(), paddingPx)
                val nearest = models.minByOrNull {
                    (pointFor(it, bounds, rect) - tap).getDistance()
                }
                if (nearest == null) {
                    onSelect(null)
                } else {
                    val distance = (pointFor(nearest, bounds, rect) - tap).getDistance()
                    onSelect(if (distance <= tapRadiusPx) nearest else null)
                }
            }
        }
    ) {
        val rect = plotRect(size.width, size.height, paddingPx)
        if (rect.width <= 0f || rect.height <= 0f) return@Canvas

        val gridPaint = android.graphics.Paint().apply {
            isAntiAlias = true
            color = ArenaTextMuted.toArgb()
            textSize = 9.sp.toPx()
            textAlign = android.graphics.Paint.Align.RIGHT
        }
        val xLabelPaint = android.graphics.Paint().apply {
            isAntiAlias = true
            color = ArenaTextMuted.toArgb()
            textSize = 9.sp.toPx()
            textAlign = android.graphics.Paint.Align.CENTER
        }
        val emptyPaint = android.graphics.Paint().apply {
            isAntiAlias = true
            color = ArenaTextSecondary.toArgb()
            textSize = 13.sp.toPx()
            textAlign = android.graphics.Paint.Align.CENTER
        }

        if (models.isEmpty()) {
            drawContext.canvas.nativeCanvas.drawText(
                "Awaiting leaderboard data",
                size.width / 2f,
                size.height / 2f,
                emptyPaint
            )
            return@Canvas
        }

        // Horizontal intelligence grid.
        val horizontalTicks = 4
        for (tick in 0..horizontalTicks) {
            val fraction = tick / horizontalTicks.toFloat()
            val y = rect.bottom - fraction * rect.height
            drawLine(
                color = ArenaBorder.copy(alpha = 0.55f),
                start = Offset(rect.left, y),
                end = Offset(rect.right, y),
                strokeWidth = 1.dp.toPx()
            )
            val score = bounds.minScore + fraction * (bounds.maxScore - bounds.minScore)
            drawContext.canvas.nativeCanvas.drawText(
                String.format(Locale.US, "%.0f", score),
                rect.left - 6.dp.toPx(),
                y + 3.dp.toPx(),
                gridPaint
            )
        }

        // Vertical blended-cost grid (log scale).
        val verticalTicks = 4
        for (tick in 0..verticalTicks) {
            val fraction = tick / verticalTicks.toFloat()
            val x = rect.left + fraction * rect.width
            drawLine(
                color = ArenaBorder.copy(alpha = 0.55f),
                start = Offset(x, rect.top),
                end = Offset(x, rect.bottom),
                strokeWidth = 1.dp.toPx()
            )
            val logCost = bounds.minLogCost + fraction * (bounds.maxLogCost - bounds.minLogCost)
            drawContext.canvas.nativeCanvas.drawText(
                costTick(logCost),
                x,
                rect.bottom + 16.dp.toPx(),
                xLabelPaint
            )
        }

        // Axis captions.
        drawContext.canvas.nativeCanvas.drawText(
            "BLENDED COST / 1M TOKENS (LOG)",
            rect.center.x,
            size.height - 2.dp.toPx(),
            xLabelPaint
        )

        // Pareto frontier line.
        if (frontier.size >= 2) {
            val path = Path()
            frontier.forEachIndexed { index, model ->
                val point = pointFor(model, bounds, rect)
                if (index == 0) path.moveTo(point.x, point.y) else path.lineTo(point.x, point.y)
            }
            drawPath(
                path = path,
                color = ArenaEmerald.copy(alpha = 0.16f),
                style = Stroke(width = 7.dp.toPx())
            )
            drawPath(
                path = path,
                color = ArenaEmerald.copy(alpha = 0.9f),
                style = Stroke(width = 2.dp.toPx())
            )
        }

        // Model dots.
        models.forEach { model ->
            val point = pointFor(model, bounds, rect)
            val baseColor = if (model.isOpenWeight) ArenaEmerald else ArenaPurple
            val isSelected = model.id == selectedId
            val isFrontier = model.id in frontierIds

            drawCircle(
                color = baseColor.copy(alpha = if (isSelected) 0.28f else 0.16f),
                radius = if (isSelected) 13.dp.toPx() else 9.dp.toPx(),
                center = point
            )
            drawCircle(
                color = baseColor,
                radius = if (isSelected) 6.5.dp.toPx() else 4.5.dp.toPx(),
                center = point
            )
            if (isFrontier) {
                drawCircle(
                    color = ArenaCyan.copy(alpha = 0.9f),
                    radius = if (isSelected) 10.dp.toPx() else 7.5.dp.toPx(),
                    style = Stroke(width = 1.5.dp.toPx()),
                    center = point
                )
            }
            if (isSelected) {
                drawCircle(
                    color = Color.White.copy(alpha = 0.95f),
                    radius = 12.dp.toPx(),
                    style = Stroke(width = 2.dp.toPx()),
                    center = point
                )
            }
        }

        // A small marker for the cheapest Pareto leader.
        frontier.firstOrNull()?.let { leader ->
            val point = pointFor(leader, bounds, rect)
            drawLine(
                color = ArenaCyan.copy(alpha = 0.5f),
                start = Offset(point.x, point.y),
                end = Offset(point.x, rect.bottom),
                strokeWidth = 1.dp.toPx()
            )
        }

    }
}
