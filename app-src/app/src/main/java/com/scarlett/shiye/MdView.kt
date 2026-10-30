package com.scarlett.shiye

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.util.LruCache
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/** 阅读排版参数 */
@Immutable
class ReadStyle(val scale: Float, val serif: Boolean, val netImages: Boolean, val baseDir: File?, val onLink: (String) -> Unit, val lineMult: Float = 1.75f, val paraEm: Float = 0.6f) {
    /** 块与块之间的距离：段间距（em）× 正文字号 */
    val gap get() = (16f * scale * paraEm).dp
}

/** 图片加载：本地相对路径（限定在文档目录内）/ data: / https（可关闭）。解码时按目标宽度降采样。 */
object Imgs {
    private val cache = object : LruCache<String, Bitmap>(48 * 1024 * 1024) { override fun sizeOf(k: String, v: Bitmap) = v.byteCount }
    private const val MAX_NET = 12L * 1024 * 1024

    suspend fun load(url: String, baseDir: File?, net: Boolean, maxW: Int = 1400): Bitmap? = withContext(Dispatchers.IO) {
        val key = "${baseDir?.name}|$url|$maxW"
        cache.get(key)?.let { return@withContext it }
        val bytes = runCatching {
            when {
                url.startsWith("data:image/") -> Base64.decode(url.substringAfter("base64,"), Base64.DEFAULT)
                url.startsWith("https://") || url.startsWith("//") -> if (!net) null else fetch(if (url.startsWith("//")) "https:$url" else url)
                url.startsWith("http://") -> null          // 明文 http 一律不加载
                baseDir != null && !url.contains("://") -> {
                    val rel = java.net.URLDecoder.decode(url.replace("+", "%2B"), "UTF-8").substringBefore('#').substringBefore('?')
                    val f = File(baseDir, rel).canonicalFile
                    if (f.path.startsWith(baseDir.canonicalPath + File.separator) && f.isFile && f.length() < 40L * 1024 * 1024) f.readBytes() else null
                }
                else -> null
            }
        }.getOrNull() ?: return@withContext null
        val o = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, o)
        if (o.outWidth <= 0) return@withContext null
        var sample = 1; while (o.outWidth / sample > maxW * 2) sample *= 2
        val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, BitmapFactory.Options().apply { inSampleSize = sample }) ?: return@withContext null
        cache.put(key, bmp); bmp
    }

    private fun fetch(u: String): ByteArray? {
        val c = URL(u).openConnection() as HttpURLConnection
        c.connectTimeout = 8000; c.readTimeout = 12000; c.instanceFollowRedirects = true
        c.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 15) ShiYe/1.0")
        c.setRequestProperty("Referer", "https://${URL(u).host}/")        // 部分图床校验 Referer（如微信公众号图片）
        if (c.responseCode !in 200..299) return null
        if (c.contentLengthLong > MAX_NET) return null
        return c.inputStream.use { it.readBytes().takeIf { b -> b.size <= MAX_NET } }
    }
}

private fun segsToAnnotated(segs: List<Seg>, pal: Pal, onLink: (String) -> Unit, baseSize: TextUnit? = null): AnnotatedString = buildAnnotatedString {
    for (s in segs) {
        val style = SpanStyle(
            fontWeight = if (s.b) FontWeight.Bold else null,
            fontStyle = if (s.i) FontStyle.Italic else null,
            textDecoration = if (s.s) TextDecoration.LineThrough else null,
            fontFamily = if (s.code) FontFamily.Monospace else null,
            background = if (s.code) pal.codeBg else Color.Unspecified,
            fontSize = if (s.code) (baseSize ?: TextUnit.Unspecified) * 0.88f else TextUnit.Unspecified,
        )
        if (s.link != null) {
            pushLink(LinkAnnotation.Url(s.link, TextLinkStyles(SpanStyle(color = pal.accent, textDecoration = TextDecoration.Underline))) { onLink((it as LinkAnnotation.Url).url) })
            withStyle(style) { append(s.text) }; pop()
        } else withStyle(style) { append(s.text) }
    }
}

@Composable
fun RenderBlocks(blocks: List<MdBlock>, st: ReadStyle, depth: Int = 0) {
    Column(verticalArrangement = Arrangement.spacedBy(st.gap)) { blocks.forEach { Block(it, st, depth) } }
}

@Composable
fun Block(b: MdBlock, st: ReadStyle, depth: Int = 0) {
    val pal = P
    val fam = if (st.serif) FontFamily.Serif else FontFamily.Default
    val body = (16f * st.scale).sp
    val lh = (16f * st.scale * st.lineMult).sp
    when (b) {
        is MdBlock.Heading -> {
            val size = when (b.level) { 1 -> 27f; 2 -> 22f; 3 -> 19f; 4 -> 17f; else -> 16f } * st.scale
            Text(segsToAnnotated(b.segs, pal, st.onLink, size.sp), color = pal.text, fontSize = size.sp, fontFamily = fam,
                fontWeight = if (b.level <= 2) FontWeight.Bold else FontWeight.SemiBold, lineHeight = (size * (1.15f + (st.lineMult - 1.2f) * 0.3f)).sp,
                modifier = Modifier.padding(top = when (b.level) { 1 -> 6.dp; 2 -> 10.dp; else -> 4.dp }))
            if (b.level == 2) Box(Modifier.width(28.dp).height(3.dp).clip(Shapes.chip).background(pal.accent))
        }
        is MdBlock.Para -> Text(segsToAnnotated(b.segs, pal, st.onLink, body), color = pal.text, fontSize = body, lineHeight = lh, fontFamily = fam)
        is MdBlock.Code -> CodeBox(b, st)
        is MdBlock.Quote -> Row(Modifier.height(IntrinsicSize.Min)) {
            Box(Modifier.width(3.dp).fillMaxHeight().clip(Shapes.chip).background(pal.accent.copy(alpha = 0.45f)))
            Spacer(Modifier.width(12.dp))
            RenderBlocks(b.blocks, st, depth + 1)
        }
        is MdBlock.Callout -> {
            val (tone, icon) = calloutTone(b.kind, pal)
            Row(Modifier.fillMaxWidth().clip(Shapes.box).background(tone.copy(alpha = if (pal.dark) 0.16f else 0.10f)).padding(14.dp), verticalAlignment = Alignment.Top) {
                Icon(painterResource(icon), null, tint = tone, modifier = Modifier.padding(top = 2.dp).size(18.dp))
                Spacer(Modifier.width(10.dp))
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(b.title.ifBlank { calloutName(b.kind) }, color = tone, fontWeight = FontWeight.SemiBold, fontSize = body)
                    if (b.blocks.isNotEmpty()) RenderBlocks(b.blocks, st, depth + 1)
                }
            }
        }
        is MdBlock.Lst -> Column(verticalArrangement = Arrangement.spacedBy(st.gap * 0.55f)) {
            b.items.forEachIndexed { idx, it ->
                Row {
                    Box(Modifier.width(if (b.ordered) 28.dp * st.scale else 20.dp * st.scale).padding(top = 1.dp), contentAlignment = Alignment.TopStart) {
                        when {
                            it.checked != null -> Icon(painterResource(if (it.checked) R.drawable.ic_check else R.drawable.ic_circle), null, tint = if (it.checked) pal.accent else pal.text3, modifier = Modifier.padding(top = 4.dp).size(16.dp * st.scale))
                            b.ordered -> Text("${b.start + idx}.", color = pal.accent, fontSize = body, lineHeight = lh, fontWeight = FontWeight.SemiBold)
                            else -> Box(Modifier.padding(top = (lh.value / 2f - 3.5f).dp * 1f, start = 4.dp).size(6.dp).clip(RoundedCornerShape(50)).background(if (depth % 2 == 0) pal.accent else pal.text3))
                        }
                    }
                    Column(Modifier.weight(1f)) { RenderBlocks(it.blocks, st, depth + 1) }
                }
            }
        }
        is MdBlock.Table -> TableBox(b, st)
        is MdBlock.Image -> MdImage(b.alt, b.url, st)
        MdBlock.Hr -> Box(Modifier.fillMaxWidth().padding(vertical = 8.dp).height(1.dp).background(pal.line))
    }
}

private fun calloutName(k: String) = when (k) { "tip", "hint", "success", "done" -> "提示"; "warning", "caution", "attention" -> "注意"; "danger", "error", "bug", "failure" -> "警告"
    "question", "faq", "help" -> "问题"; "quote", "cite" -> "引用"; "example" -> "示例"; "abstract", "summary", "tldr" -> "摘要"; "todo" -> "待办"; else -> "笔记" }
private fun calloutTone(k: String, pal: Pal): Pair<Color, Int> = when (k) {
    "tip", "hint", "success", "done", "check" -> Color(0xFF1FA971) to R.drawable.ic_sparkle
    "warning", "caution", "attention", "question", "faq" -> Color(0xFFD98A00) to R.drawable.ic_info
    "danger", "error", "bug", "failure", "fail" -> Color(0xFFD64545) to R.drawable.ic_info
    else -> pal.accent to R.drawable.ic_info
}

@Composable
private fun CodeBox(b: MdBlock.Code, st: ReadStyle) {
    val pal = P; val clip = LocalClipboardManager.current
    var copied by remember { mutableStateOf(false) }
    LaunchedEffect(copied) { if (copied) { kotlinx.coroutines.delay(1400); copied = false } }
    Column(Modifier.fillMaxWidth().clip(Shapes.box).background(pal.codeBg).border(1.dp, pal.line, Shapes.box)) {
        Row(Modifier.fillMaxWidth().padding(start = 14.dp, end = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(b.lang.ifBlank { "代码" }, color = pal.text3, fontSize = 12.sp, modifier = Modifier.weight(1f).padding(vertical = 8.dp))
            Box(Modifier.clip(Shapes.chip).clickable { clip.setText(AnnotatedString(b.text)); copied = true }.padding(horizontal = 10.dp, vertical = 6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(painterResource(if (copied) R.drawable.ic_check else R.drawable.ic_copy), null, tint = pal.text3, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp)); Text(if (copied) "已复制" else "复制", color = pal.text3, fontSize = 12.sp)
                }
            }
        }
        Text(b.text, color = pal.text, fontFamily = FontFamily.Monospace, fontSize = (13f * st.scale).sp, lineHeight = (13f * st.scale * 1.55f).sp,
            softWrap = false, modifier = Modifier.horizontalScroll(rememberScrollState()).padding(start = 14.dp, end = 14.dp, bottom = 14.dp))
    }
}

@Composable
private fun TableBox(t: MdBlock.Table, st: ReadStyle) {
    val pal = P
    val cols = t.header.size
    fun w(c: List<Seg>): Int = c.plain().sumOf { ch -> if (ch.code < 0x2E80) 1 else 2 as Int }
    val widths = (0 until cols).map { k ->
        val mx = (listOf(t.header[k]) + t.rows.map { it[k] }).maxOf { w(it) }
        (mx * 8.4f * st.scale + 28).coerceIn(72f, 280f).dp
    }
    val fs = (14.5f * st.scale).sp
    Column(Modifier.fillMaxWidth().clip(Shapes.box).border(1.dp, pal.line, Shapes.box).horizontalScroll(rememberScrollState())) {
        Row(Modifier.background(pal.accentSoft).height(IntrinsicSize.Min)) { t.header.forEachIndexed { k, c -> Cell(c, widths[k], t.align[k], true, st, fs) } }
        t.rows.forEachIndexed { r, row ->
            Box(Modifier.height(1.dp).fillMaxWidth().background(pal.line))
            Row(Modifier.background(if (r % 2 == 1) pal.field.copy(alpha = 0.35f) else Color.Transparent).height(IntrinsicSize.Min)) { row.forEachIndexed { k, c -> Cell(c, widths[k], t.align[k], false, st, fs) } }
        }
    }
}

@Composable
private fun Cell(c: List<Seg>, w: Dp, align: Int, head: Boolean, st: ReadStyle, fs: TextUnit) {
    val pal = P
    Text(segsToAnnotated(c, pal, st.onLink, fs), color = if (head) pal.accent else pal.text, fontSize = fs, lineHeight = fs * 1.5f,
        fontWeight = if (head) FontWeight.SemiBold else FontWeight.Normal,
        textAlign = when (align) { 1 -> TextAlign.Center; 2 -> TextAlign.End; else -> TextAlign.Start },
        modifier = Modifier.width(w).padding(horizontal = 12.dp, vertical = 9.dp))
}

@Composable
fun MdImage(alt: String, url: String, st: ReadStyle) {
    val pal = P
    var bmp by remember(url) { mutableStateOf<Bitmap?>(null) }
    var failed by remember(url) { mutableStateOf(false) }
    LaunchedEffect(url, st.netImages) { failed = false; bmp = Imgs.load(url, st.baseDir, st.netImages); if (bmp == null) failed = true }
    val b = bmp
    if (b != null) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            androidx.compose.foundation.Image(b.asImageBitmap(), alt, Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).border(1.dp, pal.line, RoundedCornerShape(12.dp)), contentScale = androidx.compose.ui.layout.ContentScale.FillWidth)
            if (alt.isNotBlank()) Text(alt, color = pal.text3, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp), textAlign = TextAlign.Center)
        }
    } else {
        Row(Modifier.fillMaxWidth().clip(Shapes.box).background(pal.field.copy(alpha = 0.6f)).height(if (failed) 56.dp else 160.dp).padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            Icon(painterResource(R.drawable.ic_cards), null, tint = pal.text3, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(8.dp))
            Text(if (!failed) "图片加载中…" else if (!st.netImages && url.startsWith("http")) "已关闭网络图片${if (alt.isNotBlank()) "：$alt" else ""}" else "图片没能显示${if (alt.isNotBlank()) "：$alt" else ""}", color = pal.text3, fontSize = 13.sp, maxLines = 1)
        }
    }
}
