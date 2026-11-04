package com.scarlett.shiye

import androidx.compose.ui.draw.drawBehind

import android.os.Build
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
 * 简洁「液态玻璃」：只给浮在内容上的栏（搜索 / + / 底栏 / 设置分组 / 阅读进度）用。
 * API 31+：真实背景模糊 + 冷白通透色调 + 克制顶部高光描边；更低版本退化为高不透明度纯色。
 * 观感对齐 liquid3/_elegant.css：tint ≈ 0.64、blur ≈ 24、细高光 rim。
 */
class Backdrop { val state = HazeState() }
val LocalBackdrop = staticCompositionLocalOf<Backdrop?> { null }
val blurSupported: Boolean = Build.VERSION.SDK_INT >= 31

/** 内容层：作为模糊采样来源 */
fun Modifier.glassSource(b: Backdrop?): Modifier = if (b != null && blurSupported) haze(b.state) else this

/** 浮动层：玻璃质感；floating=true 时加轻阴影与高光描边 */
@Composable
fun Modifier.glass(shape: Shape = Squircle(0.dp), floating: Boolean = false, edge: GlassEdge = GlassEdge.None): Modifier {
    val b = LocalBackdrop.current
    val pal = P
    // elegant：浅色 tint 0.55–0.65；深色略低以保可读
    val tint = if (pal.dark) Color(0xFF141922).copy(alpha = 0.55f) else Color(0xFFFFFFFF).copy(alpha = 0.64f)
    val fallback = if (pal.dark) Color(0xFF161B23).copy(alpha = 0.94f) else Color(0xFFFFFFFF).copy(alpha = 0.92f)
    var m = this
    if (floating) {
        // 0 10 28 -12 rgba(20,40,90,.14) 近似
        m = m.shadow(
            if (pal.dark) 8.dp else 14.dp,
            shape,
            clip = false,
            ambientColor = Color(0x1414285A),
            spotColor = Color(0x1A14285A),
        )
    }
    m = if (b != null && blurSupported) {
        m.hazeChild(
            b.state,
            shape = shape,
            style = HazeStyle(
                backgroundColor = pal.bg,
                tint = HazeTint(tint),
                blurRadius = 24.dp,
                noiseFactor = 0f,
                fallbackTint = HazeTint(fallback),
            ),
        )
    } else {
        m.clip(shape).background(fallback)
    }
    if (floating) {
        // 顶部近白高光 → 底部极淡冷边，克制不花
        val hi = if (pal.dark) Color.White.copy(alpha = 0.14f) else Color.White.copy(alpha = 0.88f)
        val lo = if (pal.dark) Color.White.copy(alpha = 0.03f) else Color(0xFFC8DCFF).copy(alpha = 0.22f)
        m = m.border(BorderStroke(0.8.dp, Brush.verticalGradient(listOf(hi, lo))), shape)
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
