package com.arenapulse.android.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arenapulse.android.BuildConfig
import com.arenapulse.android.domain.LeaderboardUseCases
import com.arenapulse.android.ui.theme.ArenaBorder
import com.arenapulse.android.ui.theme.ArenaCyan
import com.arenapulse.android.ui.theme.ArenaDanger
import com.arenapulse.android.ui.theme.ArenaEmerald
import com.arenapulse.android.ui.theme.ArenaGold
import com.arenapulse.android.ui.theme.ArenaPurple
import com.arenapulse.android.ui.theme.ArenaSurface
import com.arenapulse.android.ui.theme.ArenaSurfaceElevated
import com.arenapulse.android.ui.theme.ArenaTextMuted
import com.arenapulse.android.ui.theme.ArenaTextPrimary
import com.arenapulse.android.ui.theme.ArenaTextSecondary
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutSheet(
    modelCount: Int,
    watchedCount: Int,
    lastUpdated: Long,
    sourceLabel: String,
    isStale: Boolean,
    onClearCache: () -> Unit,
    onClearWatchlist: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

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
                        text = "ArenaPulse",
                        style = MaterialTheme.typography.headlineSmall,
                        color = ArenaTextPrimary
                    )
                    Text(
                        text = "Live AI leaderboard \u00B7 v${BuildConfig.VERSION_NAME}",
                        style = MaterialTheme.typography.bodySmall,
                        color = ArenaTextMuted
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Close about sheet",
                        tint = ArenaTextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (isStale) {
                Surface(
                    color = ArenaDanger.copy(alpha = 0.10f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, ArenaDanger.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "This snapshot is over a day old. Pull down on Rankings or resync below for fresh pricing.",
                        style = MaterialTheme.typography.bodySmall,
                        color = ArenaDanger,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            SectionCard(title = "CURRENT SNAPSHOT") {
                InfoRow(label = "Models tracked", value = modelCount.toString())
                InfoRow(
                    label = "Source",
                    value = when (sourceLabel) {
                        "live" -> "OpenRouter (live)"
                        "cache" -> "Local JSON cache"
                        else -> "Built-in registry seed"
                    }
                )
                InfoRow(
                    label = "Last sync",
                    value = LeaderboardUseCases.relativeTime(lastUpdated)
                )
                InfoRow(label = "Watchlist", value = "$watchedCount models")
            }

            Spacer(modifier = Modifier.height(12.dp))

            SectionCard(title = "WHERE THE NUMBERS COME FROM") {
                Text(
                    text = "Pricing, context windows, modality and capability flags stream from the public " +
                        "OpenRouter model catalog \u2014 no API key, no account and no sign-in required.",
                    style = MaterialTheme.typography.bodySmall,
                    color = ArenaTextSecondary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Arena, coding, reasoning, agentic and throughput scores come from a curated " +
                        "benchmark registry, with deterministic family heuristics for models launched after " +
                        "this build. They are directional estimates for comparison, not official vendor figures.",
                    style = MaterialTheme.typography.bodySmall,
                    color = ArenaTextSecondary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Blended price = input \u00D7 0.75 + output \u00D7 0.25. Value score = arena score " +
                        "per blended dollar, floored at \$0.05 so free tiers do not divide by zero.",
                    style = MaterialTheme.typography.bodySmall,
                    color = ArenaTextMuted
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            SectionCard(title = "PRIVACY") {
                Text(
                    text = "ArenaPulse has no analytics, no advertising SDK and no user account. The only " +
                        "network request is the read-only model catalog. Your watchlist and the leaderboard " +
                        "cache stay on this device.",
                    style = MaterialTheme.typography.bodySmall,
                    color = ArenaTextSecondary
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onClearCache,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ArenaCyan.copy(alpha = 0.16f),
                    contentColor = ArenaCyan
                )
            ) {
                Text(
                    text = "Clear cache and resync",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedButton(
                onClick = onClearWatchlist,
                enabled = watchedCount > 0,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, ArenaBorder),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = ArenaTextSecondary)
            ) {
                Text(
                    text = if (watchedCount > 0) "Clear watchlist ($watchedCount)" else "Watchlist is empty",
                    style = MaterialTheme.typography.labelLarge
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Dot(ArenaEmerald)
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Open source \u00B7 Built with Kotlin, Compose and Material 3",
                    style = MaterialTheme.typography.bodySmall,
                    color = ArenaTextMuted,
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable () -> Unit) {
    Surface(
        color = ArenaSurfaceElevated,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, ArenaBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = ArenaTextMuted
            )
            Spacer(modifier = Modifier.height(8.dp))
            content()
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = ArenaTextSecondary,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelLarge,
            color = ArenaTextPrimary
        )
    }
}

@Composable
private fun Dot(color: Color) {
    androidx.compose.foundation.layout.Box(
        modifier = Modifier
            .size(6.dp)
            .background(color, CircleShape)
    )
}
