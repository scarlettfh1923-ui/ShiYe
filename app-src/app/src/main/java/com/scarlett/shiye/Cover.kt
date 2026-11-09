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

/**
 * 书架封面调色：按来源定主调，深色渐变 + 奶油字为主。
 * top/bottom 组成对角渐变；lightFace=true 时浅底深字（备忘录）。
 */
class CoverPalette(
    val top: Color,
    val bottom: Color,
    val fg: Color,
    val dot: Color,
    val lightFace: Boolean = false,
)

/** 每个来源族 3 档明暗变体（同族内细调，同 Doc 稳定）。 */
private fun shades(a: CoverPalette, b: CoverPalette, c: CoverPalette) = listOf(a, b, c)

private val wechatFamily = shades(
    CoverPalette(Color(0xFF5C3A22), Color(0xFF2A1810), Color(0xFFF3E6D4), Color(0xFFE8B86D)),
    CoverPalette(Color(0xFF6B4428), Color(0xFF3A2416), Color(0xFFF6EBDA), Color(0xFFD9A05C)),
    CoverPalette(Color(0xFF4A2E1A), Color(0xFF1F120C), Color(0xFFEEDCC6), Color(0xFFC9924A)),
)
private val webFamily = shades(
    CoverPalette(Color(0xFF1F4A3A), Color(0xFF0E241C), Color(0xFFE4F0E8), Color(0xFF7BC4A0)),
    CoverPalette(Color(0xFF245744), Color(0xFF123028), Color(0xFFE8F3EC), Color(0xFF6BB894)),
    CoverPalette(Color(0xFF183C30), Color(0xFF0A1C16), Color(0xFFDCEAE2), Color(0xFF5AAA88)),
)
private val githubFamily = shades(
    CoverPalette(Color(0xFF2A3038), Color(0xFF12151A), Color(0xFFE8ECF0), Color(0xFF8B95A5)),
    CoverPalette(Color(0xFF323844), Color(0xFF181C22), Color(0xFFEDF0F4), Color(0xFF9AA3B0)),
    CoverPalette(Color(0xFF222830), Color(0xFF0E1014), Color(0xFFE2E6EA), Color(0xFF7A8494)),
)
private val memoFamily = shades(
    CoverPalette(Color(0xFFF3E8D8), Color(0xFFE4D2BA), Color(0xFF3A2A1A), Color(0xFFC48A4A), lightFace = true),
    CoverPalette(Color(0xFFEFE2CE), Color(0xFFDCC6A8), Color(0xFF342416), Color(0xFFB87A3E), lightFace = true),
    CoverPalette(Color(0xFFF7EDDF), Color(0xFFE8D6C0), Color(0xFF403020), Color(0xFFD09A58), lightFace = true),
)
private val fileFamily = shades(
    CoverPalette(Color(0xFF2A3A52), Color(0xFF141E2E), Color(0xFFE4EAF4), Color(0xFF7A9AD0)),
    CoverPalette(Color(0xFF314260), Color(0xFF182436), Color(0xFFE8EEF6), Color(0xFF6A8CC4)),
    CoverPalette(Color(0xFF243248), Color(0xFF101828), Color(0xFFDEE4EE), Color(0xFF8AA8D8)),
)
/** 知乎：深蓝（与主蓝拉开明度，偏墨） */
private val zhihuFamily = shades(
    CoverPalette(Color(0xFF1B3A6B), Color(0xFF0B1A36), Color(0xFFE3ECFA), Color(0xFF6FA0E8)),
    CoverPalette(Color(0xFF22467F), Color(0xFF102244), Color(0xFFE8F0FC), Color(0xFF5C8EDC)),
    CoverPalette(Color(0xFF16325E), Color(0xFF08142B), Color(0xFFDCE6F6), Color(0xFF7FAAEC)),
)
/** 豆瓣：墨绿偏橄榄（比「网页」的墨绿更黄、更暖） */
private val doubanFamily = shades(
    CoverPalette(Color(0xFF3F4A24), Color(0xFF1E2410), Color(0xFFEEF0DC), Color(0xFFB5C26A)),
    CoverPalette(Color(0xFF4A5629), Color(0xFF262D14), Color(0xFFF1F3E2), Color(0xFFA6B45C)),
    CoverPalette(Color(0xFF353F1E), Color(0xFF181D0C), Color(0xFFE6E9D2), Color(0xFFC0CC78)),
)
private val fallbackFamily = shades(
    CoverPalette(Color(0xFF2F6BFF), Color(0xFF163680), Color(0xFFE8F0FF), Color(0xFFB8D0FF)),
    CoverPalette(Color(0xFF3A76FF), Color(0xFF1A3C8C), Color(0xFFECF2FF), Color(0xFFA8C4FF)),
    CoverPalette(Color(0xFF2558E0), Color(0xFF122E6A), Color(0xFFE2ECFF), Color(0xFF9AB8FF)),
)

private fun coverFamily(src: String): List<CoverPalette> {
    val n = sourceName(src).lowercase()
    return when {
        n.contains("微信") || n.contains("wechat") || n.contains("weixin") || n.contains("公众号") -> wechatFamily
        n.contains("知乎") || n.contains("zhihu") -> zhihuFamily
        n.contains("豆瓣") || n.contains("douban") -> doubanFamily
        n.contains("safari") || n.contains("chrome") || n.contains("网页") || n.contains("浏览器")
            || n.contains("firefox") || n.contains("edge") || n.contains("brave") -> webFamily
        n.contains("github") -> githubFamily
        n.contains("备忘录") || n.contains("备忘") || n.contains("notes") || n.contains("便签") -> memoFamily
        n.contains("文件") || n.contains("粘贴") || n.contains("导入") || n.contains("打开")
            || n.contains("file") || n.contains("paste") || n.contains("import") -> fileFamily
        else -> fallbackFamily
    }
}

fun coverPalette(d: Doc, variant: Int = coverVariant(d)): CoverPalette {
    val fam = coverFamily(d.source)
    return fam[variant.coerceIn(0, fam.lastIndex)]
}

/** 同一来源内 3 档明暗变体，按标题+id 散列，换列表仍稳定。 */
fun coverVariant(d: Doc): Int = ((d.title.hashCode() * 31 + d.id.hashCode()) and 0x7fffffff) % 3

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
    val c = remember(d.source, d.title, d.id, variant) { coverPalette(d, variant) }
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
        val brush = Brush.linearGradient(listOf(c.top, c.bottom), start = Offset.Zero, end = Offset.Infinite)
        val edge = if (c.lightFace) Color(0x220F1522) else Color(0x28FFFFFF)
        Box(Modifier.fillMaxSize().soft(shape, 3).clip(shape).background(brush)
            .drawWithContent {
                drawContent()
                // 书脊：左侧 5dp 的明暗渐变
                val sw = 5.dp.toPx() * k
                drawRect(Brush.horizontalGradient(0f to Color(0x22000000), 0.55f to Color(0x28FFFFFF), 1f to Color.Transparent, startX = 0f, endX = sw), size = androidx.compose.ui.geometry.Size(sw, this.size.height))
            }
            .border(0.5.dp, edge, shape)) {
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
