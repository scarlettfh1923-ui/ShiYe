package com.scarlett.shiye

import android.content.Context
import android.content.ContextWrapper
import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape

/** 拾页配色：冷白底 + 唯一强调色电光蓝。 */
@Immutable
class Pal(
    val dark: Boolean,
    val bg: Color, val surface: Color, val field: Color,
    val text: Color, val text2: Color, val text3: Color, val line: Color,
    val accent: Color, val accentSoft: Color, val onAccent: Color,
    val danger: Color, val codeBg: Color,
)

val AccentBlue = Color(0xFF2F6BFF)
val LightPal = Pal(
    false, bg = Color(0xFFF5F7FA), surface = Color(0xFFFFFFFF), field = Color(0xFFE9EDF3),
    text = Color(0xFF151B26), text2 = Color(0xFF58637A), text3 = Color(0xFF8E98AA), line = Color(0xFFE3E8EF),
    accent = AccentBlue, accentSoft = Color(0xFFE8EFFF), onAccent = Color.White,
    danger = Color(0xFFD64545), codeBg = Color(0xFFEEF2F8),
)
val DarkPal = Pal(
    true, bg = Color(0xFF0E1116), surface = Color(0xFF171C24), field = Color(0xFF212835),
    text = Color(0xFFE8EDF5), text2 = Color(0xFFA8B3C7), text3 = Color(0xFF7C879B), line = Color(0xFF2A3240),
    accent = Color(0xFF5B8CFF), accentSoft = Color(0xFF1B2A4D), onAccent = Color.White,
    danger = Color(0xFFFF7A7A), codeBg = Color(0xFF1C2330),
)
val LocalPal = staticCompositionLocalOf { LightPal }
val P: Pal @Composable get() = LocalPal.current

object Shapes {
    val card = RoundedCornerShape(20.dp)
    val chip = RoundedCornerShape(50)
    val box = RoundedCornerShape(14.dp)
    val sheet = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
}

@Composable
fun ShiyeTheme(mode: Int, content: @Composable () -> Unit) {
    val dark = when (mode) { 1 -> false; 2 -> true; else -> isSystemInDarkTheme() }
    val pal = if (dark) DarkPal else LightPal
    val scheme = if (dark) darkColorScheme(
        primary = pal.accent, onPrimary = pal.onAccent, background = pal.bg, surface = pal.surface, onSurface = pal.text,
        surfaceVariant = pal.field, onSurfaceVariant = pal.text2, outline = pal.line, surfaceContainerHigh = pal.surface, surfaceContainer = pal.surface,
    ) else lightColorScheme(
        primary = pal.accent, onPrimary = pal.onAccent, background = pal.bg, surface = pal.surface, onSurface = pal.text,
        surfaceVariant = pal.field, onSurfaceVariant = pal.text2, outline = pal.line, surfaceContainerHigh = pal.surface, surfaceContainer = pal.surface,
    )
    CompositionLocalProvider(LocalPal provides pal) { MaterialTheme(colorScheme = scheme, content = content) }
}

fun Context.findActivity(): Activity? { var c = this; while (c is ContextWrapper) { if (c is Activity) return c; c = c.baseContext }; return null }
