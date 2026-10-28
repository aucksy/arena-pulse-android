package com.arenapulse.android.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.arenapulse.android.domain.ScoreTier

// Cyber-Obsidian palette
val ArenaBackground = Color(0xFF07090E)
val ArenaSurface = Color(0xFF111622)
val ArenaSurfaceElevated = Color(0xFF161D2B)
val ArenaBorder = Color(0xFF1E293B)
val ArenaCyan = Color(0xFF38BDF8)
val ArenaPurple = Color(0xFFA855F7)
val ArenaEmerald = Color(0xFF22C55E)
val ArenaGold = Color(0xFFFFD166)
val ArenaSilver = Color(0xFFCBD5E1)
val ArenaBronze = Color(0xFFD08A5A)
val ArenaTextPrimary = Color(0xFFF8FAFC)
val ArenaTextSecondary = Color(0xFF94A3B8)
val ArenaTextMuted = Color(0xFF64748B)
val ArenaDanger = Color(0xFFF87171)

private val ArenaColorScheme = darkColorScheme(
    primary = ArenaCyan,
    onPrimary = Color(0xFF04121A),
    primaryContainer = Color(0xFF0B2A3A),
    onPrimaryContainer = ArenaCyan,
    secondary = ArenaPurple,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF2A1640),
    onSecondaryContainer = Color(0xFFE9D5FF),
    tertiary = ArenaEmerald,
    onTertiary = Color(0xFF052E16),
    background = ArenaBackground,
    onBackground = ArenaTextPrimary,
    surface = ArenaSurface,
    onSurface = ArenaTextPrimary,
    surfaceVariant = ArenaSurfaceElevated,
    onSurfaceVariant = ArenaTextSecondary,
    outline = ArenaBorder,
    outlineVariant = ArenaBorder,
    error = ArenaDanger,
    onError = Color(0xFF1A0505)
)

private val ArenaTypography = Typography(
    displaySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 38.sp,
        letterSpacing = (-0.6).sp
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 30.sp,
        letterSpacing = (-0.3).sp
    ),
    headlineSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 26.sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 24.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        lineHeight = 20.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 17.sp
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp,
        letterSpacing = 0.3.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 10.sp,
        letterSpacing = 1.1.sp
    )
)

fun scoreColor(score: Double): Color = when {
    score >= 94.0 -> ArenaCyan
    score >= 88.0 -> ArenaPurple
    score >= 80.0 -> ArenaEmerald
    score >= 70.0 -> ArenaGold
    else -> ArenaTextMuted
}

fun tierColor(tier: ScoreTier): Color = when (tier) {
    ScoreTier.ELITE -> ArenaCyan
    ScoreTier.FRONTIER -> ArenaPurple
    ScoreTier.STRONG -> ArenaEmerald
    ScoreTier.CAPABLE -> ArenaGold
    ScoreTier.EMERGING -> ArenaTextMuted
}

fun tierLabel(tier: ScoreTier): String = when (tier) {
    ScoreTier.ELITE -> "ELITE"
    ScoreTier.FRONTIER -> "FRONTIER"
    ScoreTier.STRONG -> "STRONG"
    ScoreTier.CAPABLE -> "CAPABLE"
    ScoreTier.EMERGING -> "EMERGING"
}

@Composable
fun ArenaPulseTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = ArenaColorScheme,
        typography = ArenaTypography,
        content = content
    )
}
