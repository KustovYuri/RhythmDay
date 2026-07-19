package com.rhythmday.app.core.designsystem

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object RhythmDayPalette {
    val Background = Color(0xFFF7F8FB)
    val Panel = Color(0xFFFFFFFF)
    val PanelSoft = Color(0xFFF0F4F7)
    val Ink = Color(0xFF1F2933)
    val Muted = Color(0xFF6C7786)
    val Line = Color(0xFFDDE4EA)
    val Teal = Color(0xFF159A8C)
    val TealDark = Color(0xFF0F756C)
    val Amber = Color(0xFFDFA425)
    val Green = Color(0xFF4F9F63)
    val HardDay = Color(0xFFB86D63)
    val MediumDay = Color(0xFFBD891B)
    val GoodDay = Color(0xFF3F8F54)
}

private val RhythmDayColors = lightColorScheme(
    primary = RhythmDayPalette.Teal,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE9F4F2),
    onPrimaryContainer = RhythmDayPalette.TealDark,
    secondary = Color(0xFFE2644A),
    onSecondary = Color.White,
    tertiary = RhythmDayPalette.Amber,
    onTertiary = Color.White,
    background = RhythmDayPalette.Background,
    onBackground = RhythmDayPalette.Ink,
    surface = RhythmDayPalette.Panel,
    onSurface = RhythmDayPalette.Ink,
    surfaceVariant = RhythmDayPalette.PanelSoft,
    onSurfaceVariant = RhythmDayPalette.Muted,
    outline = RhythmDayPalette.Line,
    error = Color(0xFFCC4D52),
    onError = Color.White
)

private val RhythmDayTypography = Typography(
    headlineSmall = TextStyle(
        fontSize = 25.sp,
        lineHeight = 28.sp,
        fontWeight = FontWeight.Black,
        letterSpacing = 0.sp
    ),
    titleLarge = TextStyle(
        fontSize = 18.sp,
        lineHeight = 22.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.sp
    ),
    titleMedium = TextStyle(
        fontSize = 15.sp,
        lineHeight = 19.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.sp
    ),
    bodyLarge = TextStyle(
        fontSize = 15.sp,
        lineHeight = 22.sp,
        fontWeight = FontWeight.Normal,
        letterSpacing = 0.sp
    ),
    bodyMedium = TextStyle(
        fontSize = 13.sp,
        lineHeight = 19.sp,
        fontWeight = FontWeight.Normal,
        letterSpacing = 0.sp
    ),
    bodySmall = TextStyle(
        fontSize = 12.sp,
        lineHeight = 16.sp,
        fontWeight = FontWeight.Normal,
        letterSpacing = 0.sp
    ),
    labelLarge = TextStyle(
        fontSize = 14.sp,
        lineHeight = 18.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.sp
    )
)

private val RhythmDayShapes = Shapes(
    small = androidx.compose.foundation.shape.RoundedCornerShape(7.dp),
    medium = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
    large = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
)

@Composable
fun RhythmDayTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = RhythmDayColors,
        typography = RhythmDayTypography,
        shapes = RhythmDayShapes,
        content = content
    )
}
