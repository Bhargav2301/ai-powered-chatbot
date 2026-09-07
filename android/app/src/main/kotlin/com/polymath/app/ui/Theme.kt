package com.polymath.app.ui

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val Sage = Color(0xFF9BB59A)
val Muted = Color(0xFFA9AFB7)
val CanvasColor = Color(0xFF0B0D0F)
val SurfaceColor = Color(0xFF171A1E)
val BorderColor = Color(0xFF30363D)
val Ink = Color(0xFFF1F0EB)
private val DarkPalette = darkColorScheme(primary = Sage, onPrimary = CanvasColor, secondary = Color(0xFFC8D4DD),
    primaryContainer = Color(0xFF29382D), onPrimaryContainer = Sage,
    secondaryContainer = Color(0xFF29382D), onSecondaryContainer = Sage,
    inverseSurface = Color(0xFFC8D4DD), inverseOnSurface = CanvasColor, inversePrimary = Color(0xFF355B3B),
    background = CanvasColor, surface = SurfaceColor, surfaceContainer = SurfaceColor,
    onBackground = Ink, onSurface = Ink, onSurfaceVariant = Muted, outline = BorderColor)
private val FolioTypography = Typography(
    displaySmall = TextStyle(fontFamily = FontFamily.Serif, fontSize = 36.sp, lineHeight = 42.sp),
    headlineLarge = TextStyle(fontFamily = FontFamily.Serif, fontSize = 32.sp, lineHeight = 38.sp),
    headlineMedium = TextStyle(fontFamily = FontFamily.Serif, fontSize = 28.sp, lineHeight = 34.sp),
    titleLarge = TextStyle(fontFamily = FontFamily.Serif, fontSize = 23.sp, lineHeight = 29.sp),
    bodyLarge = TextStyle(fontSize = 17.sp, lineHeight = 27.sp),
    bodyMedium = TextStyle(fontSize = 15.sp, lineHeight = 23.sp),
    labelLarge = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium),
    labelSmall = TextStyle(fontSize = 11.sp, letterSpacing = 1.4.sp),
)
@Composable fun PolymathTheme(content: @Composable () -> Unit) = MaterialTheme(colorScheme = DarkPalette, typography = FolioTypography) {
    Surface(Modifier.fillMaxSize(), color = CanvasColor, contentColor = Ink, content = content)
}
