package com.reelmeter.presentation.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.compose.foundation.shape.RoundedCornerShape
import com.reelmeter.domain.model.ThemeMode

/** The single accent colour of the app. */
val Accent = Color(0xFF00D4FF)

object StatusColors {
    val Green = Color(0xFF00FF88)   // Neon mint
    val Orange = Color(0xFFFFB800)  // Amber gold
    val Red = Color(0xFFFF2A55)     // Hot crimson
}

/** Premium glow / gradient palette for BrainPal aesthetic. */
object PremiumColors {
    val GlowCyan = Color(0xFF00D4FF)
    val GlowPurple = Color(0xFF8B5CF6)
    val GlowAmber = Color(0xFFFFB800)
    val GlowRed = Color(0xFFFF2A55)
    val GlowMint = Color(0xFF00FF88)
    val CardBorder = Color.White.copy(alpha = 0.08f)
    val CardGlow = Color(0xFF00D4FF).copy(alpha = 0.05f)
    val GradientStart = Color(0xFF00D4FF)
    val GradientEnd = Color(0xFF8B5CF6)
}

private val LightColors = lightColorScheme(
    primary = Color(0xFF0098B8),         // Darker cyan for readability
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD6F5FB),
    onPrimaryContainer = Color(0xFF003844),
    background = Color(0xFFF4F5FA),      // Cool-toned light background
    onBackground = Color(0xFF111116),
    surface = Color.White,
    onSurface = Color(0xFF111116),
    surfaceVariant = Color(0xFFE8EBF4),  // Cooler surface variant
    onSurfaceVariant = Color(0xFF5A5C70),
    errorContainer = Color(0xFFFFE3E3),
    onErrorContainer = Color(0xFF5C1111),
    error = StatusColors.Red,
    onError = Color.White,
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF00D4FF),              // Electric cyan
    onPrimary = Color(0xFF003844),
    primaryContainer = Color(0xFF002B36),      // Dark cyan
    onPrimaryContainer = Color(0xFF80EEFF),
    background = Color(0xFF060610),            // Ultra-deep OLED
    onBackground = Color(0xFFF0F0F8),
    surface = Color(0xFF12121E),               // Dark card
    onSurface = Color(0xFFF0F0F8),
    surfaceVariant = Color(0xFF1C1C2E),        // Secondary surface
    onSurfaceVariant = Color(0xFF8888A8),
    errorContainer = Color(0xFF3D0A16),
    onErrorContainer = Color(0xFFFFDADA),
    error = Color(0xFFFF3B5C),
    onError = Color.White,
)

private val AppShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
)

@Composable
fun ReelMeterTheme(themeMode: ThemeMode, content: @Composable () -> Unit) {
    val dark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !dark
            controller.isAppearanceLightNavigationBars = !dark
            // Edge-to-edge transparent system bars
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
        }
    }
    MaterialTheme(
        colorScheme = if (dark) DarkColors else LightColors,
        shapes = AppShapes,
        typography = AppTypography,
        content = content,
    )
}
