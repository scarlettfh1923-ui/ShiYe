package com.scarlett.shiye

import androidx.compose.ui.draw.drawBehind

import android.os.Build
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.haze
import dev.chrisbanes.haze.hazeChild

/**
 * 轻量「液态玻璃」：只给浮在内容上的栏（顶栏 / 底栏 / 浮动工具条）用。
 * API 31+：真实背景模糊 + 通透色调 + 顶部高光描边；更低版本退化为高不透明度纯色。
 */
class Backdrop { val state = HazeState() }
val LocalBackdrop = staticCompositionLocalOf<Backdrop?> { null }
val blurSupported: Boolean = Build.VERSION.SDK_INT >= 31

/** 内容层：作为模糊采样来源 */
fun Modifier.glassSource(b: Backdrop?): Modifier = if (b != null && blurSupported) haze(b.state) else this

/** 浮动层：玻璃质感；hair=true 时底边加发丝线（用于贴边的栏） */
@Composable
fun Modifier.glass(shape: Shape = Squircle(0.dp), floating: Boolean = false, edge: GlassEdge = GlassEdge.None): Modifier {
    val b = LocalBackdrop.current
    val pal = P
    val tint = if (pal.dark) Color(0xFF141922).copy(alpha = 0.58f) else Color(0xFFFFFFFF).copy(alpha = 0.62f)
    val fallback = if (pal.dark) Color(0xFF161B23).copy(alpha = 0.96f) else Color(0xFFFFFFFF).copy(alpha = 0.95f)
    var m = this
    if (floating) m = m.shadow(if (pal.dark) 8.dp else 16.dp, shape, clip = false, ambientColor = Color(0x1A1B2A4D), spotColor = Color(0x261B2A4D))
    m = if (b != null && blurSupported) m.hazeChild(b.state, shape = shape, style = HazeStyle(backgroundColor = pal.bg, tint = HazeTint(tint), blurRadius = 26.dp, noiseFactor = 0f, fallbackTint = HazeTint(fallback)))
    else m.clip(shape).background(fallback)
    if (floating) {
        val hi = if (pal.dark) Color.White.copy(alpha = 0.16f) else Color.White.copy(alpha = 0.95f)
        val lo = if (pal.dark) Color.White.copy(alpha = 0.04f) else Color(0xFFD5DCEA).copy(alpha = 0.55f)
        m = m.border(BorderStroke(0.5.dp, Brush.verticalGradient(listOf(hi, lo))), shape)
    }
    return m
}

enum class GlassEdge { None, Bottom, Top }

@Composable
fun Modifier.hairline(edge: GlassEdge): Modifier {
    val c = P.hair
    return when (edge) {
        GlassEdge.None -> this
        GlassEdge.Bottom -> this.then(Modifier.drawHair(c, bottom = true))
        GlassEdge.Top -> this.then(Modifier.drawHair(c, bottom = false))
    }
}
private fun Modifier.drawHair(c: Color, bottom: Boolean): Modifier = this.then(
    drawBehind {
        val y = if (bottom) size.height - 0.5f else 0.5f
        drawLine(c, androidx.compose.ui.geometry.Offset(0f, y), androidx.compose.ui.geometry.Offset(size.width, y), 0.5.dp.toPx())
    })
