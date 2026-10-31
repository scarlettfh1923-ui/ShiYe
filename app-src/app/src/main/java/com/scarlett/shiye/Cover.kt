package com.scarlett.shiye

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/** 把标题拆成「主标题」与「副标题」：按 —— / ： / : 第一处分隔。 */
fun splitTitle(t: String): Pair<String, String> {
    for (sep in listOf("——", "：", ":")) {
        val i = t.indexOf(sep)
        if (i > 0) { val a = t.substring(0, i).trim(); val b = t.substring(i + sep.length).trim(); if (a.isNotEmpty() && b.isNotEmpty()) return a to b }
    }
    return t.trim() to ""
}

private const val WJ = "\u2060"

/** 书名号整组不换行：在《…》内部的每个字之间放一个 WORD JOINER，换行算法就不会在组内断开。 */
fun keepBookTitles(s: String): String = Regex("《[^》]*》").replace(s) { m -> m.value.toList().joinToString(WJ) }

/** 来源：「分享 · 微信」→「微信」，「文件」→「文件」 */
fun sourceName(src: String): String = src.substringAfterLast(" · ").trim().ifEmpty { "拾页" }
fun sourceKind(src: String): String? = src.substringBeforeLast(" · ", "").trim().ifEmpty { null }

/** 封面脚注日期：今天 / 昨天 / M月d日 */
fun coverDate(t: Long, now: Long = System.currentTimeMillis()): String {
    fun day(x: Long) = Calendar.getInstance().apply { timeInMillis = x; set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }.timeInMillis
    val diff = (day(now) - day(t)) / 86_400_000L
    return when { diff <= 0 -> "今天"; diff == 1L -> "昨天"; else -> SimpleDateFormat("M月d日", Locale.CHINA).format(Date(t)) }
}

private class CoverColors(val bg: Color, val fg: Color, val dot: Color)
private val lightCovers = listOf(
    CoverColors(Color(0xFFFFFFFF), Color(0xFF0F1522), Color(0xFF2F6BFF)),
    CoverColors(Color(0xFFE8EFFF), Color(0xFF142A5C), Color(0xFF2F6BFF)),
    CoverColors(Color(0xFF2F6BFF), Color(0xFFFFFFFF), Color(0xFFFFFFFF)),
    CoverColors(Color(0xFFEDF0F5), Color(0xFF1B2434), Color(0xFF2F6BFF)),
)
private val darkCovers = listOf(
    CoverColors(Color(0xFF1B2230), Color(0xFFE8EDF5), Color(0xFF5B8CFF)),
    CoverColors(Color(0xFF1B2A4D), Color(0xFFDCE6FF), Color(0xFF5B8CFF)),
    CoverColors(Color(0xFF3A6BEA), Color(0xFFFFFFFF), Color(0xFFFFFFFF)),
    CoverColors(Color(0xFF242C3A), Color(0xFFDDE3EE), Color(0xFF5B8CFF)),
)

/** 每篇文档固定一个底色（白 / 浅蓝 / 主蓝 / 灰），按标题散列，换列表也不会变。 */
fun coverVariant(d: Doc): Int = ((d.title.hashCode() * 31 + d.id.hashCode()) and 0x7fffffff) % 4

enum class CoverSize(val w: Int, val h: Int, val p: Float, val eyebrow: Float, val title: Float, val lh: Float, val sub: Float, val sl: Float, val radius: Int, val lines: Int) {
    Card(112, 150, 12f, 8f, 14f, 19f, 10.5f, 15f, 10, 4),
    Hero(84, 112, 9f, 6.5f, 10f, 14f, 8f, 11f, 8, 3),
    Mini(44, 58, 5f, 0f, 6.5f, 8.6f, 0f, 0f, 5, 3),
}

/**
 * 排版封面：来源小标 → 发丝线 → 衬线粗体主标题（字距 −0.02em，《》不拆行）→ 副标题 → 日期 + 圆点。
 * 尺寸按设计稿的 card / hero / mini 三档；width 不固定时（书库双列）整体按比例放大。
 */
@Composable
fun TypoCover(d: Doc, size: CoverSize, modifier: Modifier = Modifier, variant: Int = coverVariant(d)) {
    val dark = P.dark
    val c = (if (dark) darkCovers else lightCovers)[variant and 3]
    val density = LocalDensity.current
    BoxWithConstraints(modifier) {
        val k = (maxWidth.value / size.w).coerceAtLeast(0.3f)
        fun dp(v: Float): Dp = (v * k).dp
        fun sp(v: Float): TextUnit = with(density) { (v * k).dp.toSp() }      // 封面是图形：不随系统字号缩放
        val shape: Shape = remember(size, k) { Squircle((size.radius * k).dp) }
        val (main, sub) = remember(d.title) { splitTitle(d.title) }
        val mini = size == CoverSize.Mini
        var tl by remember(d.title, k) { mutableIntStateOf(0) }
        val titleMax = if (!mini && sub.isNotEmpty()) size.lines - 1 else size.lines
        Box(Modifier.fillMaxSize().soft(shape, 3).clip(shape).background(c.bg)
            .drawWithContent {
                drawContent()
                // 书脊：左侧 5dp 的明暗渐变
                val sw = 5.dp.toPx() * k
                drawRect(Brush.horizontalGradient(0f to Color(0x17000000), 0.55f to Color(0x24FFFFFF), 1f to Color.Transparent, startX = 0f, endX = sw), size = androidx.compose.ui.geometry.Size(sw, this.size.height))
            }
            .border(0.5.dp, Color(if (dark) 0x1FFFFFFF else 0x140F1522), shape)) {
            Column(Modifier.fillMaxSize().padding(start = dp(size.p + 3f), top = dp(size.p), end = dp(size.p), bottom = dp(size.p))) {
                if (!mini) {
                    Text(sourceName(d.source), color = c.fg.copy(alpha = 0.78f), maxLines = 1, overflow = TextOverflow.Ellipsis,
                        style = TextStyle(fontSize = sp(size.eyebrow), lineHeight = sp(size.eyebrow * 1.3f), fontWeight = FontWeight.SemiBold, letterSpacing = 0.08.em))
                    Box(Modifier.padding(top = dp(size.p * .55f), bottom = dp(size.p * .7f)).fillMaxWidth().height(0.5.dp).background(c.fg.copy(alpha = 0.4f)))
                } else {
                    Box(Modifier.padding(top = dp(3f), bottom = dp(4f)).fillMaxWidth().height(0.5.dp).background(c.fg.copy(alpha = 0.4f)))
                }
                Text(keepBookTitles(main), color = c.fg, maxLines = titleMax, overflow = TextOverflow.Ellipsis, onTextLayout = { tl = it.lineCount },
                    style = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = sp(size.title), lineHeight = sp(size.lh), letterSpacing = (-0.02).em, lineBreak = LineBreak.Heading))
                if (!mini && sub.isNotEmpty()) Text(keepBookTitles(sub), color = c.fg.copy(alpha = 0.72f), maxLines = if (tl >= titleMax && titleMax >= 3) 1 else 2, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = dp(size.p * .4f)),
                    style = TextStyle(fontSize = sp(size.sub), lineHeight = sp(size.sl), lineBreak = LineBreak.Heading))
                if (!mini) {
                    Spacer(Modifier.weight(1f))
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(coverDate(d.createdAt), color = c.fg.copy(alpha = 0.55f), maxLines = 1, style = TextStyle(fontSize = sp(size.eyebrow), lineHeight = sp(size.eyebrow * 1.3f), letterSpacing = 0.04.em))
                        Box(Modifier.size(dp(size.eyebrow * .9f)).clip(androidx.compose.foundation.shape.CircleShape).background(c.dot.copy(alpha = 0.9f)))
                    }
                }
            }
        }
    }
}
