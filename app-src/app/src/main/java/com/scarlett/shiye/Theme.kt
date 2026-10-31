package com.scarlett.shiye

import android.content.Context
import android.content.ContextWrapper
import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.Shapes as M3Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

/** 拾页配色：冷白底 + 唯一强调色电光蓝。 */
@Immutable
class Pal(
    val dark: Boolean,
    val bg: Color, val surface: Color, val field: Color,
    val text: Color, val text2: Color, val text3: Color, val line: Color,
    val accent: Color, val accentSoft: Color, val onAccent: Color,
    val danger: Color, val codeBg: Color,
    val hair: Color = Color(0x170F1522),
)

val AccentBlue = Color(0xFF2F6BFF)
val LightPal = Pal(
    false, bg = Color(0xFFF5F7FA), surface = Color(0xFFFFFFFF), field = Color(0xFFE9EDF3),
    text = Color(0xFF0F1522), text2 = Color(0xFF566074), text3 = Color(0xFF8A93A4), line = Color(0xFFE3E8EF),
    accent = AccentBlue, accentSoft = Color(0xFFE8EFFF), onAccent = Color.White,
    danger = Color(0xFFD64545), codeBg = Color(0xFFEEF2F8),
)
val DarkPal = Pal(
    true, bg = Color(0xFF0E1116), surface = Color(0xFF171C24), field = Color(0xFF212835),
    text = Color(0xFFE8EDF5), text2 = Color(0xFFA8B3C7), text3 = Color(0xFF7C879B), line = Color(0xFF2A3240),
    accent = Color(0xFF5B8CFF), accentSoft = Color(0xFF1B2A4D), onAccent = Color.White,
    danger = Color(0xFFFF7A7A), codeBg = Color(0xFF1C2330), hair = Color(0x1FFFFFFF),
)
val LocalPal = staticCompositionLocalOf { LightPal }
val P: Pal @Composable get() = LocalPal.current

object Shapes {
    val card: Shape = Squircle(20.dp)
    val chip = RoundedCornerShape(50)
    val box: Shape = Squircle(16.dp)
    val sheet = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
}

/**
 * iOS 式连续圆角（squircle）。曲线从圆角起点前移 (1+平滑度)·r，过渡比普通圆角更柔。
 * 算法同 Figma 的 cornerSmoothing（默认 0.6），半径超过短边一半时自动退化为圆弧 / 胶囊。
 */
class Squircle(private val radius: Dp, private val smoothing: Float = 0.6f) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val w = size.width; val h = size.height
        val budget = minOf(w, h) / 2f
        val r = with(density) { radius.toPx() }.coerceAtMost(budget)
        if (r < 0.5f) return Outline.Rectangle(androidx.compose.ui.geometry.Rect(0f, 0f, w, h))
        val xi = minOf(smoothing, budget / r - 1f).coerceAtLeast(0f)
        val p = minOf((1f + xi) * r, budget)
        val m = 90f * (1f - xi); val mr = Math.toRadians(m.toDouble())
        val arcLen = (sin(mr / 2) * r * sqrt(2.0)).toFloat()
        val alpha = (90f - m) / 2f
        val p3p4 = (r * tan(Math.toRadians(alpha / 2.0))).toFloat()
        val beta = Math.toRadians((45f * xi).toDouble())
        val c = (p3p4 * cos(beta)).toFloat(); val d = (c * tan(beta)).toFloat()
        val b = (p - arcLen - c - d) / 3f; val a = 2f * b
        val k = (4.0 / 3.0 * tan(mr / 4) * r).toFloat()
        val t0x = cos(beta).toFloat(); val t0y = sin(beta).toFloat()
        val t1x = cos(beta + mr).toFloat(); val t1y = sin(beta + mr).toFloat()
        val seg = listOf(
            floatArrayOf(a, 0f, a + b, 0f, a + b + c, d),
            floatArrayOf(k * t0x, k * t0y, arcLen - k * t1x, arcLen - k * t1y, arcLen, arcLen),
            floatArrayOf(d, c, d, b + c, d, a + b + c),
        )
        val starts = listOf(w - p to 0f, w to h - p, p to h, 0f to p)
        val path = Path()
        for (q in 0..3) {
            val (sx, sy) = starts[q]
            if (q == 0) path.moveTo(sx, sy) else path.lineTo(sx, sy)
            for (s in seg) {
                fun rot(x: Float, y: Float) = when (q) { 0 -> x to y; 1 -> -y to x; 2 -> -x to -y; else -> y to -x }
                val (x1, y1) = rot(s[0], s[1]); val (x2, y2) = rot(s[2], s[3]); val (x3, y3) = rot(s[4], s[5])
                path.relativeCubicTo(x1, y1, x2, y2, x3, y3)
            }
        }
        path.close()
        return Outline.Generic(path)
    }
}

/**
 * 阅读主题（仅作用于阅读页）：0 跟随应用（白 / 夜）· 1 雾蓝 · 2 纸 · 3 夜。
 * 用 Pal 的整套角色重新着色，阅读页里的 Markdown 块、玻璃栏、面板都会一起跟随。
 */
object ReadThemes {
    val names = listOf("白", "雾蓝", "纸", "夜")
    private val mist = Pal(false, bg = Color(0xFFE8EEF8), surface = Color(0xFFF4F7FC), field = Color(0xFFD9E2F2), text = Color(0xFF1A2438), text2 = Color(0xFF53607C), text3 = Color(0xFF8793AB), line = Color(0xFFD0DAEC),
        accent = AccentBlue, accentSoft = Color(0xFFD6E3FF), onAccent = Color.White, danger = Color(0xFFD64545), codeBg = Color(0xFFDDE6F4))
    private val paper = Pal(false, bg = Color(0xFFF4EDDC), surface = Color(0xFFFAF5E8), field = Color(0xFFE9E0CA), text = Color(0xFF3A3326), text2 = Color(0xFF6B6350), text3 = Color(0xFF9A917B), line = Color(0xFFE0D6BC),
        accent = Color(0xFF2F6BFF), accentSoft = Color(0xFFE9E4D0), onAccent = Color.White, danger = Color(0xFFC0433A), codeBg = Color(0xFFEBE3CD))
    private val night = Pal(true, bg = Color(0xFF121722), surface = Color(0xFF1A2130), field = Color(0xFF242C3D), text = Color(0xFFD9DFEA), text2 = Color(0xFFA2ADC2), text3 = Color(0xFF78839A), line = Color(0xFF2B3446),
        accent = Color(0xFF6C97FF), accentSoft = Color(0xFF1F2E52), onAccent = Color.White, danger = Color(0xFFFF7A7A), codeBg = Color(0xFF1B2333))
    /** 预览色块用：背景、文字 */
    val swatch = listOf(Color(0xFFFAFBFD) to LightPal.text, mist.bg to mist.text, paper.bg to paper.text, night.bg to night.text)
    /** 阅读用「白」：比应用底色略亮一点点的冷白，与设计稿 #FAFBFD 一致 */
    private val white = Pal(false, bg = Color(0xFFFAFBFD), surface = Color(0xFFFFFFFF), field = LightPal.field, text = LightPal.text, text2 = LightPal.text2, text3 = LightPal.text3, line = LightPal.line,
        accent = AccentBlue, accentSoft = LightPal.accentSoft, onAccent = Color.White, danger = LightPal.danger, codeBg = LightPal.codeBg)
    fun pal(theme: Int, app: Pal): Pal = when (theme) { 1 -> mist; 2 -> paper; 3 -> night; else -> if (app.dark) app else white }
}

/** 字型层级（M3 type scale）。中文正文行高放宽，标题收紧。 */
val ShiyeType = Typography(
    displaySmall = TextStyle(fontSize = 32.sp, lineHeight = 40.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.6).sp),
    headlineMedium = TextStyle(fontSize = 28.sp, lineHeight = 36.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp),
    headlineSmall = TextStyle(fontSize = 22.sp, lineHeight = 28.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.4).sp),
    titleLarge = TextStyle(fontSize = 20.sp, lineHeight = 28.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.4).sp),
    titleMedium = TextStyle(fontSize = 17.sp, lineHeight = 24.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.3).sp),
    titleSmall = TextStyle(fontSize = 15.sp, lineHeight = 22.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.2).sp),
    bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontSize = 12.sp, lineHeight = 16.sp),
    labelLarge = TextStyle(fontSize = 15.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium),
    labelMedium = TextStyle(fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium),
    labelSmall = TextStyle(fontSize = 10.sp, lineHeight = 14.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.2.sp),
)

/** 把 Pal 铺成完整的 M3 配色角色，组件库里的控件（Slider / Switch / 对话框 / 导航栏…）不用再单独上色。 */
fun schemeOf(pal: Pal): ColorScheme {
    val onBlue = pal.onAccent
    return if (pal.dark) darkColorScheme(
        primary = pal.accent, onPrimary = onBlue, primaryContainer = pal.accentSoft, onPrimaryContainer = Color(0xFFDCE6FF),
        secondary = pal.text2, onSecondary = pal.bg, secondaryContainer = pal.field, onSecondaryContainer = pal.text,
        tertiary = Color(0xFFB7A6FF), onTertiary = Color(0xFF1E1640), tertiaryContainer = Color(0xFF2A2440), onTertiaryContainer = Color(0xFFE5DDFF),
        background = pal.bg, onBackground = pal.text, surface = pal.bg, onSurface = pal.text, surfaceVariant = pal.field, onSurfaceVariant = pal.text2,
        surfaceTint = pal.accent, inverseSurface = pal.text, inverseOnSurface = pal.bg, inversePrimary = AccentBlue,
        error = pal.danger, onError = Color(0xFF3B0A0A), errorContainer = Color(0xFF4A1F1F), onErrorContainer = Color(0xFFFFDAD6),
        outline = pal.text3, outlineVariant = pal.line, scrim = Color.Black,
        surfaceDim = pal.bg, surfaceBright = pal.surface, surfaceContainerLowest = pal.bg, surfaceContainerLow = pal.surface, surfaceContainer = pal.surface, surfaceContainerHigh = pal.field, surfaceContainerHighest = pal.field,
    ) else lightColorScheme(
        primary = pal.accent, onPrimary = onBlue, primaryContainer = pal.accentSoft, onPrimaryContainer = Color(0xFF0B2A80),
        secondary = pal.text2, onSecondary = Color.White, secondaryContainer = pal.field, onSecondaryContainer = pal.text,
        tertiary = Color(0xFF6A4DFF), onTertiary = Color.White, tertiaryContainer = Color(0xFFEFEAFF), onTertiaryContainer = Color(0xFF2A1A80),
        background = pal.bg, onBackground = pal.text, surface = pal.bg, onSurface = pal.text, surfaceVariant = pal.field, onSurfaceVariant = pal.text2,
        surfaceTint = pal.accent, inverseSurface = pal.text, inverseOnSurface = Color.White, inversePrimary = Color(0xFF9DB8FF),
        error = pal.danger, onError = Color.White, errorContainer = Color(0xFFFFE3E0), onErrorContainer = Color(0xFF5A1010),
        outline = pal.text3, outlineVariant = pal.line, scrim = Color.Black,
        surfaceDim = pal.field, surfaceBright = pal.surface, surfaceContainerLowest = pal.surface, surfaceContainerLow = pal.surface, surfaceContainer = pal.surface, surfaceContainerHigh = pal.surface, surfaceContainerHighest = pal.field,
    )
}

/**
 * 主题入口：MaterialTheme 是唯一来源（配色角色 + 字型 + 形状）。
 * 旧代码里的 P.xxx 只是对同一份调色的快捷读取；阅读页用 [ReadThemes] 在局部再套一层。
 */
@Composable
fun ShiyeTheme(mode: Int, content: @Composable () -> Unit) {
    val dark = when (mode) { 1 -> false; 2 -> true; else -> isSystemInDarkTheme() }
    ShiyeThemeWith(if (dark) DarkPal else LightPal, content)
}

@Composable
fun ShiyeThemeWith(pal: Pal, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalPal provides pal) {
        MaterialTheme(colorScheme = schemeOf(pal), typography = ShiyeType,
            shapes = M3Shapes(extraSmall = RoundedCornerShape(8.dp), small = RoundedCornerShape(12.dp), medium = RoundedCornerShape(16.dp), large = RoundedCornerShape(24.dp), extraLarge = RoundedCornerShape(28.dp)),
            content = content)
    }
}

fun Context.findActivity(): Activity? { var c = this; while (c is ContextWrapper) { if (c is Activity) return c; c = c.baseContext }; return null }
