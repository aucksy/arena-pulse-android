package com.arenapulse.android.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arenapulse.android.domain.AiModel
import com.arenapulse.android.ui.theme.ArenaBorder
import com.arenapulse.android.ui.theme.ArenaCyan
import com.arenapulse.android.ui.theme.ArenaTextSecondary
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * Custom five-axis radar chart (Overall, Coding, Reasoning, Agentic, Speed)
 * with an animated fill sweep. Speed is normalized against 350 tok/s.
 */
@Composable
fun RadarChartCanvas(
    model: AiModel,
    modifier: Modifier = Modifier
) {
    var targetProgress by remember(model.id) { mutableStateOf(0f) }
    val progress by animateFloatAsState(
        targetValue = targetProgress,
        animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing),
        label = "radar-progress"
    )

    LaunchedEffect(model.id) {
        targetProgress = 0f
        targetProgress = 1f
    }

    val axes = remember(model.id) {
        listOf(
            "Overall" to model.overallScore,
            "Coding" to model.codingScore,
            "Reason" to model.reasoningScore,
            "Agent" to model.agenticScore,
            "Speed" to (model.speedTokensPerSec / 3.5).coerceIn(0.0, 100.0)
        )
    }

    Canvas(modifier = modifier) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val radius = min(size.width, size.height) / 2f - 42.dp.toPx()
        if (radius <= 0f) return@Canvas

        fun axisAngle(index: Int): Double =
            -PI / 2.0 + index * (2.0 * PI / axes.size)

        fun pointAt(index: Int, fraction: Float): Offset {
            val angle = axisAngle(index)
            val r = radius * fraction
            return Offset(
                x = center.x + cos(angle).toFloat() * r,
                y = center.y + sin(angle).toFloat() * r
            )
        }

        // Concentric grid rings.
        for (level in 1..4) {
            val fraction = level / 4f
            val path = Path()
            axes.indices.forEach { index ->
                val point = pointAt(index, fraction)
                if (index == 0) path.moveTo(point.x, point.y) else path.lineTo(point.x, point.y)
            }
            path.close()
            drawPath(
                path = path,
                color = ArenaBorder.copy(alpha = if (level == 4) 0.95f else 0.55f),
                style = Stroke(width = 1.dp.toPx())
            )
        }

        // Axis spokes.
        axes.indices.forEach { index ->
            drawLine(
                color = ArenaBorder.copy(alpha = 0.8f),
                start = center,
                end = pointAt(index, 1f),
                strokeWidth = 1.dp.toPx()
            )
        }

        // Value polygon.
        val valuePath = Path()
        axes.forEachIndexed { index, (_, value) ->
            val fraction = (value / 100.0).toFloat().coerceIn(0.04f, 1f) * progress
            val point = pointAt(index, fraction)
            if (index == 0) valuePath.moveTo(point.x, point.y) else valuePath.lineTo(point.x, point.y)
        }
        valuePath.close()

        drawPath(
            path = valuePath,
            color = ArenaCyan.copy(alpha = 0.20f)
        )
        drawPath(
            path = valuePath,
            color = ArenaCyan,
            style = Stroke(width = 2.dp.toPx())
        )

        // Vertex dots.
        axes.forEachIndexed { index, (_, value) ->
            val fraction = (value / 100.0).toFloat().coerceIn(0.04f, 1f) * progress
            val point = pointAt(index, fraction)
            drawCircle(color = ArenaCyan.copy(alpha = 0.25f), radius = 7.dp.toPx(), center = point)
            drawCircle(color = ArenaCyan, radius = 3.dp.toPx(), center = point)
        }

        // Axis labels.
        val labelPaint = android.graphics.Paint().apply {
            isAntiAlias = true
            color = ArenaTextSecondary.toArgb()
            textSize = 10.sp.toPx()
            textAlign = android.graphics.Paint.Align.CENTER
            typeface = android.graphics.Typeface.create(
                android.graphics.Typeface.DEFAULT,
                android.graphics.Typeface.BOLD
            )
        }
        axes.forEachIndexed { index, (label, _) ->
            val angle = axisAngle(index)
            val labelRadius = radius + 22.dp.toPx()
            val x = center.x + cos(angle).toFloat() * labelRadius
            val y = center.y + sin(angle).toFloat() * labelRadius + 4.dp.toPx()
            drawContext.canvas.nativeCanvas.drawText(label, x, y, labelPaint)
        }

        // Center pin.
        drawCircle(color = Color.White.copy(alpha = 0.55f), radius = 2.dp.toPx(), center = center)
    }
}
