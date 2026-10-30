package com.scarlett.shiye

import android.graphics.*
import android.os.Build
import android.text.*
import android.text.style.*
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** 长图用的四套版式（冷白 / 雾蓝玻璃 / 电光 / 石墨）。小红书图文的版式见 XhsStyles.kt。 */
class ExStyle(
    val name: String, val desc: String,
    val bgTop: Int, val bgBottom: Int, val panel: Boolean, val panelColor: Int, val panelBorder: Int,
    val text: Int, val text2: Int, val accent: Int, val accentSoft: Int, val codeBg: Int, val line: Int,
    val solidCover: Boolean, val band: Boolean, val dark: Boolean, val glow: Boolean,
)

object ExStyles {
    val all = listOf(
        ExStyle("冷白", "干净利落", 0xFFF5F7FA.toInt(), 0xFFF5F7FA.toInt(), false, 0, 0, 0xFF151B26.toInt(), 0xFF58637A.toInt(), 0xFF2F6BFF.toInt(), 0xFFE5EDFF.toInt(), 0xFFEAEFF6.toInt(), 0xFFDDE3EC.toInt(), false, false, false, false),
        ExStyle("雾蓝", "玻璃面板", 0xFFDDE8FF.toInt(), 0xFFF4F7FC.toInt(), true, 0xD9FFFFFF.toInt(), 0xFFFFFFFF.toInt(), 0xFF151B26.toInt(), 0xFF58637A.toInt(), 0xFF2F6BFF.toInt(), 0xFFE5EDFF.toInt(), 0xFFEDF1F8.toInt(), 0xFFDCE3EE.toInt(), false, false, false, true),
        ExStyle("电光", "整页蓝", 0xFF2F6BFF.toInt(), 0xFF2F6BFF.toInt(), false, 0, 0, 0xFF151B26.toInt(), 0xFF58637A.toInt(), 0xFF2F6BFF.toInt(), 0xFFE5EDFF.toInt(), 0xFFEAEFF6.toInt(), 0xFFDDE3EC.toInt(), true, true, false, false),
        ExStyle("石墨", "深色夜读", 0xFF0F131A.toInt(), 0xFF171D28.toInt(), true, 0xFF1A2130.toInt(), 0xFF2A3447.toInt(), 0xFFE8EDF5.toInt(), 0xFFA8B3C7.toInt(), 0xFF6E9BFF.toInt(), 0xFF22304F.toInt(), 0xFF111722.toInt(), 0xFF2A3447.toInt(), false, false, true, true),
    )
}

/** 行距（倍数）与段间距（em）——与阅读页共用同一组全局设置 */
class Typo(val line: Float = 1.75f, val para: Float = 0.6f)

enum class HKind { Plain, BarTop, Underline, Marker, Pill, Rules, Dot, Neon }
enum class QKind { Bar, BigMark, Box, Dashed, Frame }

/** 正文排版的皮肤：颜色、字体、对齐、二级标题 / 加粗 / 引用的做法。 */
class Skin(
    val text: Int, val text2: Int, val accent: Int, val accentSoft: Int, val codeBg: Int, val line: Int,
    val dark: Boolean = false, val serif: Boolean = false, val headSerif: Boolean = false,
    val justify: Boolean = true, val indent: Boolean = false, val spacing: Float = 0.015f,
    val h2: HKind = HKind.Plain, val h2Color: Int = text, val h2Bg: Int = accentSoft, val h2Text: Int = 0,
    val boldColor: Int = 0, val boldBg: Int = 0, val quote: QKind = QKind.Bar, val bodyScale: Float = 1f,
) {
    companion object {
        fun of(st: ExStyle) = Skin(st.text, st.text2, st.accent, st.accentSoft, st.codeBg, st.line, st.dark)
    }
}

abstract class Item(val before: Int, val keepNext: Boolean = false) {
    var x = 0; var w = 0; var h = 0
    open val cuts: IntArray = IntArray(0)
    abstract fun draw(c: Canvas)
}

private fun fillP(color: Int) = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color }

private class TextItem(before: Int, val layout: StaticLayout, val padT: Int, val padB: Int, val box: Int?, val marker: String?, val markerPaint: Paint?, val markerGap: Int, val radius: Float, keep: Boolean = false, val flatBottom: Boolean = false) : Item(before, keep) {
    init { h = layout.height + padT + padB }
    override val cuts: IntArray by lazy { IntArray(layout.lineCount) { layout.getLineBottom(it) + padT }.also { if (it.isNotEmpty()) it[it.lastIndex] = h } }
    override fun draw(c: Canvas) {
        if (box != null) {
            if (flatBottom) c.drawPath(Exporter.roundPath(w.toFloat(), h.toFloat(), radius, true, false), fillP(box))
            else c.drawRoundRect(0f, 0f, w.toFloat(), h.toFloat(), radius, radius, fillP(box))
        }
        if (marker != null && markerPaint != null) c.drawText(marker, 0f, padT + layout.getLineBaseline(0).toFloat(), markerPaint)
        c.save(); c.translate(markerGap.toFloat(), padT.toFloat()); layout.draw(c); c.restore()
    }
}

/** 二级 / 三级标题：文字 + 各风格自己的装饰（短横条、下划线、荧光笔、标签、霓虹……） */
private class HeadItem(before: Int, val layout: StaticLayout, val padT: Int, val padB: Int, val inset: Int, val deco: (Canvas, StaticLayout, Int, Int) -> Unit) : Item(before, true) {
    init { h = layout.height + padT + padB }
    override fun draw(c: Canvas) {
        deco(c, layout, w, h)
        c.save(); c.translate(inset.toFloat(), padT.toFloat()); layout.draw(c); c.restore()
    }
}

private class HrItem(before: Int, val lineColor: Int) : Item(before) { init { h = 4 }; override fun draw(c: Canvas) { c.drawRect(0f, 1f, w.toFloat(), 3f, fillP(lineColor)) } }

private class ImgItem(before: Int, val bmp: Bitmap, val cap: StaticLayout?) : Item(before) {
    var dh = 0
    override fun draw(c: Canvas) {
        val p = Path().apply { addRoundRect(0f, 0f, w.toFloat(), dh.toFloat(), 22f, 22f, Path.Direction.CW) }
        c.save(); c.clipPath(p); c.drawBitmap(bmp, null, Rect(0, 0, w, dh), Paint(Paint.FILTER_BITMAP_FLAG)); c.restore()
        cap?.let { c.save(); c.translate(0f, dh + 12f); it.draw(c); c.restore() }
    }
}

private class TableItem(before: Int, val cols: IntArray, val rows: List<List<StaticLayout>>, val rowH: IntArray, val sk: Skin, val pad: Int) : Item(before) {
    init { h = rowH.sum() }
    override val cuts: IntArray by lazy { var a = 0; IntArray(rowH.size) { a += rowH[it]; a } }
    override fun draw(c: Canvas) {
        val line = Paint().apply { color = sk.line; strokeWidth = 2f }
        val tot = cols.sum().toFloat()
        c.drawRoundRect(0f, 0f, tot, h.toFloat(), 18f, 18f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = sk.line; style = Paint.Style.STROKE; strokeWidth = 3f })
        var y = 0
        rows.forEachIndexed { r, row ->
            if (r == 0) { c.save(); c.clipRect(0f, 0f, tot, rowH[0].toFloat()); c.drawRoundRect(0f, 0f, tot, rowH[0] + 40f, 18f, 18f, Paint().apply { color = sk.accentSoft }); c.restore() }
            else c.drawLine(0f, y.toFloat(), tot, y.toFloat(), line)
            var x = 0
            row.forEachIndexed { k, l -> c.save(); c.translate((x + pad).toFloat(), (y + pad).toFloat()); l.draw(c); c.restore(); x += cols[k] }
            y += rowH[r]
        }
    }
}

/** 给内部块加底色 / 竖线 / 引号等背景的外壳；顶、底各留 padding，分页时可以跟着切。 */
private class Wrap(val inner: Item, before: Int, val topPad: Int, val botPad: Int, x0: Int, w0: Int, val bg: (Canvas, Int, Int) -> Unit) : Item(before) {
    init { x = x0; w = w0; h = topPad + inner.h + botPad }
    override val cuts: IntArray by lazy { inner.cuts.map { it + topPad }.toIntArray().also { if (it.isNotEmpty()) it[it.lastIndex] = h } }
    override fun draw(c: Canvas) {
        bg(c, w, h)
        c.save(); c.translate((inner.x - x).toFloat(), topPad.toFloat()); inner.draw(c); c.restore()
    }
}

class Prepared(val title: String, val blocks: List<MdBlock>, val images: Map<String, Bitmap>, val snippet: String, val minutes: Int, val isHtml: Boolean)

object Exporter {
    suspend fun prepare(doc: Doc, text: String, net: Boolean): Prepared {
        val md = if (doc.type == DocType.HTML) HtmlText.toMarkdown(text) else text
        val blocks = Markdown.parse(md).let { b -> val f = b.firstOrNull(); if (f is MdBlock.Heading && f.level == 1 && f.segs.plain().trim() == doc.title.trim()) b.drop(1) else b }   // 标题已在封面/页头出现，正文里不再重复
        val urls = mutableListOf<String>()
        fun walk(l: List<MdBlock>) { l.forEach { when (it) { is MdBlock.Image -> urls += it.url; is MdBlock.Quote -> walk(it.blocks); is MdBlock.Callout -> walk(it.blocks); is MdBlock.Lst -> it.items.forEach { i -> walk(i.blocks) }; else -> {} } } }
        walk(blocks)
        val dir = if (doc.bundle || doc.type == DocType.MD) Library.dirOf(doc).let { File(it, doc.main).parentFile ?: it } else null
        val imgs = HashMap<String, Bitmap>()
        for (u in urls.distinct().take(24)) Imgs.load(u, dir, net, 1200)?.let { imgs[u] = it }
        val chars = md.length
        return Prepared(doc.title, blocks, imgs, doc.snippet.lineSequence().firstOrNull().orEmpty(), (chars / 400).coerceAtLeast(1), doc.type == DocType.HTML)
    }

    fun today(): String = SimpleDateFormat("yyyy.MM.dd", Locale.CHINA).format(Date())

    // ---------- 排版 ----------
    /** 所有尺寸都是 1080 宽画布上的像素。body = 正文字号；段间距 = typo.para × body，行距 = typo.line × 字号。 */
    private class Lay(val sk: Skin, val images: Map<String, Bitmap>, val typo: Typo, val body: Float) {
        val reg: Typeface = if (sk.serif) Typeface.SERIF else Typeface.DEFAULT
        val bold: Typeface = Typeface.create(reg, Typeface.BOLD)
        val headTf: Typeface = Typeface.create(if (sk.headSerif) Typeface.SERIF else Typeface.DEFAULT, Typeface.BOLD)
        val gapP = (typo.para * body).toInt()

        fun tp(size: Float, color: Int, tf: Typeface = reg) = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { textSize = size; this.color = color; typeface = tf; if (tf !== Typeface.MONOSPACE) letterSpacing = sk.spacing }

        /** lh = 行高 / 字号（em 倍数） */
        fun layout(sp: CharSequence, p: TextPaint, w: Int, lh: Float, max: Int = Int.MAX_VALUE, ell: Boolean = false, align: Layout.Alignment = Layout.Alignment.ALIGN_NORMAL, justify: Boolean = false): StaticLayout {
            val fm = p.fontMetrics
            val add = (p.textSize * lh - (fm.descent - fm.ascent)).coerceAtLeast(0f)
            return StaticLayout.Builder.obtain(sp, 0, sp.length, p, w.coerceAtLeast(10)).setLineSpacing(add, 1f).setIncludePad(false).setAlignment(align)
                .setBreakStrategy(android.graphics.text.LineBreaker.BREAK_STRATEGY_HIGH_QUALITY).setHyphenationFrequency(Layout.HYPHENATION_FREQUENCY_NONE)
                .apply { if (justify) setJustificationMode(android.text.Layout.JUSTIFICATION_MODE_INTER_CHARACTER); if (max != Int.MAX_VALUE) setMaxLines(max); if (ell) setEllipsize(TextUtils.TruncateAt.END) }.build()
        }
        fun span(segs: List<Seg>, baseColor: Int, bold: Boolean = false, hi: Boolean = true): SpannableStringBuilder {
            val sb = SpannableStringBuilder()
            for (s in segs) {
                if (s.img != null) continue
                val a = sb.length; sb.append(s.text); val b = sb.length
                if (b == a) continue
                val isB = s.b || bold
                if (isB && s.i) sb.setSpan(StyleSpan(Typeface.BOLD_ITALIC), a, b, 0) else if (isB) sb.setSpan(StyleSpan(Typeface.BOLD), a, b, 0) else if (s.i) sb.setSpan(StyleSpan(Typeface.ITALIC), a, b, 0)
                if (s.b && hi) {
                    if (sk.boldColor ushr 24 != 0) sb.setSpan(ForegroundColorSpan(sk.boldColor), a, b, 0)
                    if (sk.boldBg ushr 24 != 0) sb.setSpan(BackgroundColorSpan(sk.boldBg), a, b, 0)
                }
                if (s.s) sb.setSpan(StrikethroughSpan(), a, b, 0)
                if (s.code) { sb.setSpan(TypefaceSpan("monospace"), a, b, 0); sb.setSpan(BackgroundColorSpan(sk.codeBg), a, b, 0); sb.setSpan(RelativeSizeSpan(0.9f), a, b, 0) }
                if (s.link != null) sb.setSpan(ForegroundColorSpan(sk.accent), a, b, 0)
            }
            return sb
        }

        fun headDeco(level: Int, size: Float): Triple<Int, Int, (Canvas, StaticLayout, Int, Int) -> Unit> {  // padT, padB, painter
            val none: (Canvas, StaticLayout, Int, Int) -> Unit = { _, _, _, _ -> }
            if (level != 2 && level != 1) return Triple(0, 0, none)
            return when (sk.h2) {
                HKind.BarTop -> Triple(30, 0) { c, _, _, _ -> c.drawRoundRect(0f, 0f, 76f, 8f, 4f, 4f, fillP(sk.accent)) }
                HKind.Underline -> Triple(0, 22) { c, _, w, h -> c.drawRect(0f, h - 4f, w.toFloat(), h - 2f, fillP(sk.line)); c.drawRect(0f, h - 5f, 110f, h - 1f, fillP(sk.accent)) }
                HKind.Marker -> Triple(0, 6) { c, l, _, _ ->
                    for (i in 0 until l.lineCount) c.drawRoundRect(l.getLineLeft(i) - 6f, l.getLineBottom(i) - size * 0.52f, l.getLineRight(i) + 6f, l.getLineBottom(i) - size * 0.10f, 10f, 10f, fillP(sk.h2Bg))
                }
                HKind.Pill -> Triple(0, 0) { c, l, _, h -> c.drawRoundRect(-22f, -2f, l.width.toFloat().coerceAtMost(maxLineRight(l) + 22f), h + 2f, 22f, 22f, fillP(sk.h2Bg)) }
                HKind.Rules -> Triple(26, 22) { c, _, w, h -> c.drawRect(0f, 4f, w.toFloat(), 9f, fillP(sk.text)); c.drawRect(0f, h - 3f, w.toFloat(), h - 1f, fillP(sk.text)) }
                HKind.Dot -> Triple(0, 0) { c, l, _, _ -> c.drawCircle(10f, l.getLineBaseline(0) - size * 0.32f, size * 0.17f, fillP(sk.accent)) }
                HKind.Neon -> Triple(0, 24) { c, _, w, h ->
                    val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = sk.accent; setShadowLayer(16f, 0f, 0f, sk.accent) }
                    c.drawRect(0f, h - 5f, w * 0.42f, h - 2f, p)
                }
                HKind.Plain -> Triple(0, 0, none)
            }
        }
        private fun maxLineRight(l: StaticLayout): Float { var m = 0f; for (i in 0 until l.lineCount) m = maxOf(m, l.getLineRight(i)); return m }

        fun build(blocks: List<MdBlock>, width: Int, indent: Int, gap: Int, out: MutableList<Item>) {
            for (b in blocks) {
                val w = width - indent
                val before = if (out.isEmpty()) 0 else gap
                fun add(i: Item) { i.x = indent; i.w = w; out += i }
                when (b) {
                    is MdBlock.Heading -> {
                        val size = when (b.level) { 1 -> body * 1.65f; 2 -> body * 1.38f; 3 -> body * 1.2f; else -> body * 1.08f }
                        val colr = if (b.level <= 2) sk.h2Color else sk.text
                        val inset = if (sk.h2 == HKind.Dot && b.level <= 2) (size * 0.62f).toInt() else if (sk.h2 == HKind.Pill && b.level <= 2) 0 else 0
                        val tc = if (sk.h2 == HKind.Pill && b.level <= 2 && sk.h2Text ushr 24 != 0) sk.h2Text else colr
                        val p = tp(size, tc, headTf)
                        if (sk.h2 == HKind.Neon && b.level <= 2) p.setShadowLayer(18f, 0f, 0f, sk.accent)
                        val l = layout(span(b.segs, tc, true, hi = false), p, w - inset - (if (sk.h2 == HKind.Pill) 20 else 0), 1.3f)
                        val (pt, pb, deco) = headDeco(b.level, size)
                        val bef = if (out.isEmpty()) 0 else (gap + body * 0.55f).toInt()
                        val it = HeadItem(bef, l, pt, pb, inset + (if (sk.h2 == HKind.Pill && b.level <= 2) 4 else 0), deco)
                        add(it)
                    }
                    is MdBlock.Para -> {
                        val sp = span(b.segs, sk.text)
                        if (sp.isNotEmpty()) {
                            if (sk.indent) sp.setSpan(LeadingMarginSpan.Standard((body * 2f).toInt(), 0), 0, sp.length, Spanned.SPAN_PARAGRAPH)
                            add(TextItem(before, layout(sp, tp(body, sk.text), w, typo.line, justify = sk.justify), 0, 0, null, null, null, 0, 0f))
                        }
                    }
                    is MdBlock.Code -> {
                        val pad = 26
                        add(TextItem(before, layout(b.text.ifEmpty { " " }.take(6000), tp(body * 0.8f, sk.text, Typeface.MONOSPACE), w - pad * 2, 1.45f), pad, pad, sk.codeBg, null, null, pad, 20f))
                    }
                    is MdBlock.Quote -> {
                        val q = sk.quote
                        val padL = when (q) { QKind.Bar -> 34; QKind.BigMark -> 74; QKind.Box -> 34; QKind.Dashed -> 38; QKind.Frame -> 36 }
                        val padR = if (q == QKind.Box || q == QKind.Frame) 34 else 0
                        val boxy = q == QKind.Box || q == QKind.Frame
                        val inner = mutableListOf<Item>(); build(b.blocks, w - padL - padR, 0, (gapP * 0.7f).toInt(), inner)
                        inner.forEachIndexed { k, ni ->
                            val first = k == 0; val last = k == inner.lastIndex
                            ni.x += indent + padL
                            val topPad = if (first) (if (boxy) 30 else 0) else ni.before
                            val botPad = if (last && boxy) 30 else 0
                            out += Wrap(ni, if (first) before else 0, topPad, botPad, indent, w) { c, ww, hh -> quoteBg(c, q, ww, hh, first, last) }
                        }
                    }
                    is MdBlock.Callout -> {
                        val pad = 30
                        val head = if (b.title.isNotBlank()) b.title else when (b.kind) { "tip", "hint", "success" -> "提示"; "warning", "caution" -> "注意"; "danger", "error" -> "警告"; else -> "笔记" }
                        val tone = when (b.kind) { "tip", "hint", "success", "done" -> 0xFF1FA971.toInt(); "warning", "caution", "attention" -> 0xFFD98A00.toInt(); "danger", "error", "bug" -> 0xFFD64545.toInt(); else -> sk.accent }
                        val bg = (tone and 0x00FFFFFF) or (if (sk.dark) 0x2E000000 else 0x17000000)
                        val hl = layout(head, tp(body * 0.95f, tone, bold), w - pad * 2, 1.4f)
                        val ci = TextItem(before, hl, pad, if (b.blocks.isEmpty()) pad else 8, bg, null, null, pad, 22f, flatBottom = b.blocks.isNotEmpty()); ci.x = indent; ci.w = w; out += ci
                        val sub = mutableListOf<Item>(); build(b.blocks, w - pad * 2, 0, (gapP * 0.6f).toInt(), sub)
                        sub.forEachIndexed { k, it ->
                            it.x += indent + pad
                            val last = k == sub.lastIndex
                            out += Wrap(it, 0, if (k == 0) 0 else it.before, if (last) pad else 0, indent, w) { c, ww, hh ->
                                if (last) c.drawPath(roundPath(ww.toFloat(), hh.toFloat(), 22f, false, true), fillP(bg)) else c.drawRect(0f, 0f, ww.toFloat(), hh.toFloat(), fillP(bg))
                            }
                        }
                    }
                    is MdBlock.Lst -> b.items.forEachIndexed { idx, li ->
                        val head = li.blocks.firstOrNull()
                        val gapI = if (out.isEmpty()) 0 else (gapP * 0.55f).toInt()
                        val mk = when { li.checked == true -> "☑"; li.checked == false -> "☐"; b.ordered -> "${b.start + idx}."; else -> "•" }
                        val mp = tp(body, sk.accent, bold)
                        val mg = if (b.ordered) 62 else 42
                        if (head is MdBlock.Para) {
                            val it = TextItem(if (idx == 0) before else gapI, layout(span(head.segs, sk.text), tp(body, sk.text), w - mg, typo.line - 0.05f), 0, 0, null, mk, mp, mg, 0f); add(it)
                            build(li.blocks.drop(1), w - mg, indent + mg, (gapP * 0.5f).toInt(), out)
                        } else {
                            val it = TextItem(if (idx == 0) before else gapI, layout(" ", tp(body, sk.text), w - mg, typo.line - 0.05f), 0, 0, null, mk, mp, mg, 0f); add(it)
                            build(li.blocks, w - mg, indent + mg, (gapP * 0.5f).toInt(), out)
                        }
                    }
                    is MdBlock.Table -> { val t = table(b, w, before); t.x = indent; t.w = w; out += t }
                    is MdBlock.Image -> images[b.url]?.let { bmp ->
                        val dh = (w.toFloat() * bmp.height / bmp.width).toInt().coerceAtMost(950)
                        val cap = if (b.alt.isNotBlank()) layout(b.alt, tp(body * 0.72f, sk.text2), w, 1.35f, 2, align = Layout.Alignment.ALIGN_CENTER) else null
                        val i = ImgItem(before, bmp, cap); i.dh = dh; i.h = dh + (cap?.let { it.height + 12 } ?: 0); add(i)
                    }
                    MdBlock.Hr -> add(HrItem(before + 6, sk.line))
                }
            }
        }

        private fun quoteBg(c: Canvas, q: QKind, w: Int, h: Int, first: Boolean, last: Boolean) {
            val a = sk.accent
            when (q) {
                QKind.Bar -> c.drawRoundRect(0f, 0f, 8f, h.toFloat(), 4f, 4f, fillP((a and 0x00FFFFFF) or 0x99000000.toInt()))
                QKind.BigMark -> if (first) {
                    val p = tp(150f, (a and 0x00FFFFFF) or 0x66000000, Typeface.create(Typeface.SERIF, Typeface.BOLD)); p.letterSpacing = 0f
                    c.drawText("“", -4f, 96f, p)
                }
                QKind.Box -> c.drawPath(roundPath(w.toFloat(), h.toFloat(), 22f, first, last), fillP(sk.accentSoft))
                QKind.Dashed -> {
                    val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = a; style = Paint.Style.STROKE; strokeWidth = 4f; strokeCap = Paint.Cap.ROUND; pathEffect = DashPathEffect(floatArrayOf(14f, 12f), 0f) }
                    c.drawLine(3f, 0f, 3f, h.toFloat(), p)
                }
                QKind.Frame -> {
                    val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = sk.text2; style = Paint.Style.STROKE; strokeWidth = 2f }
                    val path = Path(); path.moveTo(1f, if (first) 1f else 0f); if (first) { path.lineTo(w - 1f, 1f) } else path.moveTo(w - 1f, 0f)
                    path.moveTo(1f, 0f); path.lineTo(1f, h.toFloat()); path.moveTo(w - 1f, 0f); path.lineTo(w - 1f, h.toFloat())
                    if (first) { path.moveTo(1f, 1f); path.lineTo(w - 1f, 1f) }
                    if (last) { path.moveTo(1f, h - 1f); path.lineTo(w - 1f, h - 1f) }
                    c.drawPath(path, p)
                }
            }
        }

        fun table(t: MdBlock.Table, w: Int, before: Int): TableItem {
            val n = t.header.size
            val fs = body * (if (n >= 5) 0.68f else if (n == 4) 0.76f else 0.84f)
            val pad = 16
            val allRows = listOf(t.header) + t.rows.take(60)
            val weights = (0 until n).map { k -> allRows.maxOf { r -> r[k].plain().sumOf { ch -> if (ch.code < 0x2E80) 1 else 2 as Int } }.coerceIn(4, 40).toFloat() }
            val tot = weights.sum()
            val cols = IntArray(n) { (w * weights[it] / tot).toInt().coerceAtLeast(80) }
            val scale = w.toFloat() / cols.sum(); for (k in cols.indices) cols[k] = (cols[k] * scale).toInt()
            val layouts = allRows.mapIndexed { r, row -> row.mapIndexed { k, cell -> layout(span(cell, if (r == 0) sk.accent else sk.text, r == 0, hi = false), tp(fs, if (r == 0) sk.accent else sk.text), cols[k] - pad * 2, 1.4f) } }
            val rh = IntArray(layouts.size) { r -> layouts[r].maxOf { it.height } + pad * 2 }
            return TableItem(before, cols, layouts, rh, sk, pad)
        }
    }

    internal fun roundPath(w: Float, h: Float, r: Float, top: Boolean, bottom: Boolean): Path {
        val p = Path(); val t = if (top) r else 0f; val b = if (bottom) r else 0f
        p.addRoundRect(RectF(0f, 0f, w, h), floatArrayOf(t, t, t, t, b, b, b, b), Path.Direction.CW); return p
    }

    // ---------- 分页 ----------
    private class Placed(val item: Item, val from: Int, val to: Int, val y: Int)
    private class Page { val list = mutableListOf<Placed>(); var used = 0 }

    private fun paginate(items: List<Item>, pageH: Int): List<Page> {
        val pages = mutableListOf(Page())
        for (it in items) {
            var from = 0
            var pg: Page
            var firstPiece = true
            while (true) {
                pg = pages.last()
                val gap = if (pg.list.isEmpty()) 0 else if (firstPiece) it.before else 0
                val avail = pageH - pg.used - gap
                val rem = it.h - from
                val keepFail = it.keepNext && firstPiece && pg.list.isNotEmpty() && avail - rem < 190
                if (rem <= avail && !keepFail) { pg.list += Placed(it, from, it.h, pg.used + gap); pg.used += gap + rem; break }
                val cut = if (keepFail) -1 else it.cuts.filter { c -> c > from && c <= from + avail }.maxOrNull() ?: -1
                if (cut > from && (pg.list.isEmpty() || cut - from > 60)) {
                    pg.list += Placed(it, from, cut, pg.used + gap); pg.used += gap + (cut - from)
                    pages += Page(); from = cut; firstPiece = false
                } else if (pg.list.isNotEmpty()) { pages += Page(); firstPiece = true; continue }
                else { // 整页放不下的不可分割块：裁到页高
                    val to = from + pageH; pg.list += Placed(it, from, minOf(to, it.h), 0); pg.used = pageH
                    if (to >= it.h) break; pages += Page(); from = to; firstPiece = false
                }
            }
        }
        if (pages.size > 1 && pages.last().list.isEmpty()) pages.removeAt(pages.lastIndex)
        return pages
    }

    private fun drawPlaced(c: Canvas, pg: Page, ox: Int, oy: Int) {
        for (pl in pg.list) {
            c.save()
            c.clipRect(ox.toFloat() + pl.item.x, oy + pl.y.toFloat(), ox.toFloat() + pl.item.x + pl.item.w, oy + pl.y + (pl.to - pl.from).toFloat())
            c.translate(ox.toFloat() + pl.item.x, oy + pl.y - pl.from.toFloat()); pl.item.draw(c); c.restore()
        }
    }

    // ---------- 小红书图文：只出分页卡片，竖版 3:4 / 9:16 ----------
    const val XW = 1080
    fun xhsHeight(ratio: Int) = if (ratio == 1) 1920 else 1440
    const val CARD_LIMIT = 18
    class CardsOut(val files: List<File>, val thumbs: List<Bitmap>, val truncated: Boolean, val w: Int, val h: Int)

    /** coverTitle 为封面上显示的标题（可编辑）；正文页的页头仍用文档原标题。 */
    fun renderCards(p: Prepared, style: XStyle, ratio: Int, coverTitle: String, typo: Typo, dir: File?): CardsOut {
        dir?.let { it.mkdirs(); it.listFiles()?.forEach { f -> f.delete() } }
        val w = XW; val h = xhsHeight(ratio)
        val lay = Lay(style.skin, p.images, typo, 38f * style.skin.bodyScale)
        val ins = style.insets(w, h)   // 左 上 右 下
        val items = mutableListOf<Item>(); lay.build(p.blocks, w - ins[0] - ins[2], 0, lay.gapP, items)
        var pages = paginate(items, h - ins[1] - ins[3])
        if (pages.size == 1 && pages[0].list.isEmpty()) pages = emptyList()
        val truncated = pages.size > CARD_LIMIT - 1
        val use = pages.take(CARD_LIMIT - 1)
        val total = use.size + 1
        val files = mutableListOf<File>(); val thumbs = mutableListOf<Bitmap>()
        fun emit(bmp: Bitmap, i: Int) {
            dir?.let { d -> val f = File(d, "shiye_%02d.png".format(i + 1)); FileOutputStream(f).use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }; files += f }
            thumbs += Bitmap.createScaledBitmap(bmp, 360, 360 * h / w, true); bmp.recycle()
        }
        val date = today()
        run {
            val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            style.cover(Canvas(bmp), CoverCtx(w, h, coverTitle.ifBlank { p.title }, p.snippet.replace(Regex("\\s+"), " ").trim(), (if (p.isHtml) "网页" else "Markdown") + " · 约 ${p.minutes} 分钟读完", date, total, p.minutes))
            emit(bmp, 0)
        }
        use.forEachIndexed { i, pg ->
            val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888); val c = Canvas(bmp)
            val pc = PageCtx(w, h, i + 2, total, p.title, date)
            style.bodyBg(c, pc)
            drawPlaced(c, pg, ins[0], ins[1])
            style.bodyFg(c, pc)
            emit(bmp, i + 1)
        }
        return CardsOut(files, thumbs, truncated, w, h)
    }

    /** 风格选择器里的真实封面缩略图 */
    fun coverThumb(p: Prepared, style: XStyle, ratio: Int, title: String): Bitmap {
        val w = XW; val h = xhsHeight(ratio)
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        style.cover(Canvas(bmp), CoverCtx(w, h, title.ifBlank { p.title }, p.snippet.replace(Regex("\\s+"), " ").trim(), "Markdown · 约 ${p.minutes} 分钟读完", today(), 6, p.minutes))
        val t = Bitmap.createScaledBitmap(bmp, 270, 270 * h / w, true); bmp.recycle(); return t
    }

    // ---------- 长图：一张完整的长图，不分页 ----------
    const val LW = 1080
    const val LONG_MAX = 24000
    const val LONG_MAX_SAFE = 14000
    class LongOut(val file: File?, val thumb: Bitmap, val width: Int, val height: Int, val truncated: Boolean)

    fun renderLong(p: Prepared, st: ExStyle, typo: Typo, file: File?): LongOut {
        return try { renderLongCap(p, st, typo, file, LONG_MAX) } catch (e: OutOfMemoryError) { System.gc(); renderLongCap(p, st, typo, file, LONG_MAX_SAFE) }
    }

    private fun renderLongCap(p: Prepared, st: ExStyle, typo: Typo, file: File?, cap: Int): LongOut {
        val sk = Skin.of(st)
        val lay = Lay(sk, p.images, typo, 36f)
        val mx = if (st.panel) 104 else 84
        val cw = LW - mx * 2
        val items = mutableListOf<Item>(); lay.build(p.blocks, cw, 0, lay.gapP, items)
        val onBlue = st.solidCover
        val title = lay.layout(p.title, lay.tp(66f, if (onBlue) 0xFFFFFFFF.toInt() else st.text, lay.headTf), cw, 1.28f)
        val metaP = lay.tp(27f, if (onBlue) 0xCCFFFFFF.toInt() else st.text2)
        val meta = "${today()}  ·  约 ${p.minutes} 分钟读完"
        val topPad = if (st.panel) 170 else 120
        val headH = 56 + title.height + 24 + 40 + 60
        val footH = 190
        // 摆放：从上到下一条线，超过上限就截断并提示
        var y = 0; var truncated = false
        val placed = ArrayList<Pair<Item, Int>>()
        val bodyMax = cap - topPad - headH - footH
        for (it in items) {
            val gap = if (placed.isEmpty()) 0 else it.before
            if (y + gap + minOf(it.h, 200) > bodyMax) { truncated = true; break }
            placed += it to (y + gap); y += gap + it.h
        }
        val bodyH = y.coerceAtMost(bodyMax)
        val total = topPad + headH + bodyH + footH
        val bmp = Bitmap.createBitmap(LW, total, Bitmap.Config.ARGB_8888); val c = Canvas(bmp)
        longBackground(c, st, LW, total)
        val by = (if (st.panel) 150 else 110).toFloat()
        if (onBlue) c.drawRoundRect(0f, 0f, LW.toFloat(), by + headH + 10f, 0f, 0f, fillP(st.accent)).also { c.drawPath(roundPath(LW.toFloat(), 60f, 60f, true, false).also { it.offset(0f, by + headH - 50f) }, fillP(0xFFF5F7FA.toInt())) }
        c.drawRoundRect(mx.toFloat(), by, mx + 96f, by + 12f, 6f, 6f, fillP(if (onBlue) 0xFFFFFFFF.toInt() else st.accent))
        c.save(); c.translate(mx.toFloat(), by + 56f); title.draw(c); c.restore()
        c.drawText(meta, mx.toFloat(), by + 56f + title.height + 56f, metaP)
        val y0 = (by + headH - 10).toInt()
        for ((it, yy) in placed) {
            c.save(); c.clipRect(mx.toFloat() + it.x, y0 + yy.toFloat(), mx.toFloat() + it.x + it.w, y0 + yy + it.h.toFloat())
            c.translate(mx.toFloat() + it.x, y0 + yy.toFloat()); it.draw(c); c.restore()
        }
        val fy = (y0 + bodyH + 80).toFloat()
        c.drawLine(mx.toFloat(), fy, LW - mx.toFloat(), fy, Paint().apply { color = st.line; strokeWidth = 3f })
        c.drawCircle(LW / 2f - 86f, fy + 62f, 9f, fillP(st.accent))
        val fp = lay.tp(30f, st.text2); fp.textAlign = Paint.Align.CENTER
        c.drawText(if (truncated) "内容较长，已截取前面的部分  ·  拾页" else "由拾页整理", LW / 2f + 14f, fy + 72f, fp)
        file?.let { f -> f.parentFile?.mkdirs(); FileOutputStream(f).use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) } }
        val th = Bitmap.createScaledBitmap(bmp, 420, (420f * total / LW).toInt().coerceAtLeast(10), true)
        bmp.recycle()
        return LongOut(file, th, LW, total, truncated)
    }

    private fun longBackground(c: Canvas, st: ExStyle, w: Int, h: Int) {
        if (st.solidCover) c.drawColor(0xFFF5F7FA.toInt())
        else c.drawRect(0f, 0f, w.toFloat(), h.toFloat(), Paint().apply { shader = LinearGradient(0f, 0f, 0f, h.toFloat(), st.bgTop, st.bgBottom, Shader.TileMode.CLAMP) })
        if (st.glow) {
            val col = if (st.dark) 0x556E9BFF else 0x552F6BFF
            c.drawCircle(w * 0.1f, 160f, w * 0.7f, Paint(Paint.ANTI_ALIAS_FLAG).apply { shader = RadialGradient(w * 0.1f, 160f, w * 0.7f, col, col and 0x00FFFFFF, Shader.TileMode.CLAMP) })
        }
        if (st.panel) {
            val r = RectF(36f, 90f, w - 36f, h - 60f)
            c.drawRoundRect(r, 56f, 56f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = st.panelColor; setShadowLayer(34f, 0f, 12f, if (st.dark) 0x66000000 else 0x1F1B2A4D) })
            c.drawRoundRect(r, 56f, 56f, Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 3f; color = if (st.dark) st.panelBorder else 0xCCFFFFFF.toInt() })
        }
    }
}
