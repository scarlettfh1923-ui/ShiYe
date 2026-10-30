package com.scarlett.shiye

import android.graphics.*
import android.text.*
import android.text.style.LeadingMarginSpan
import kotlin.math.*

class PageCtx(val w: Int, val h: Int, val page: Int, val total: Int, val title: String, val date: String)
class CoverCtx(val w: Int, val h: Int, val title: String, val snippet: String, val meta: String, val date: String, val total: Int, val minutes: Int)

/** 一套小红书图文版式 = 一个封面 + 一个正文页。尺寸以 1080 宽、3:4 / 9:16 竖版为准。 */
abstract class XStyle(val id: String, val name: String, val desc: String, val fancy: Boolean, val skin: Skin, val paper: Int) {
    open fun insets(w: Int, h: Int): IntArray = intArrayOf(96, 190, 96, 170)   // 左 上 右 下
    abstract fun cover(c: Canvas, k: CoverCtx)
    abstract fun bodyBg(c: Canvas, p: PageCtx)
    abstract fun bodyFg(c: Canvas, p: PageCtx)
}

// ---------- 绘制小工具 ----------
internal object XD {
    fun fill(color: Int) = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color }
    fun stroke(color: Int, w: Float) = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color; style = Paint.Style.STROKE; strokeWidth = w }
    fun tp(size: Float, color: Int, bold: Boolean = false, serif: Boolean = false, spacing: Float = 0.01f) = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = size; this.color = color; letterSpacing = spacing
        typeface = Typeface.create(if (serif) Typeface.SERIF else Typeface.DEFAULT, if (bold) Typeface.BOLD else Typeface.NORMAL)
    }
    fun lay(s: CharSequence, p: TextPaint, w: Int, lh: Float = 1.3f, align: Layout.Alignment = Layout.Alignment.ALIGN_NORMAL, max: Int = Int.MAX_VALUE, justify: Boolean = false): StaticLayout {
        val fm = p.fontMetrics
        val add = (p.textSize * lh - (fm.descent - fm.ascent)).coerceAtLeast(0f)
        return StaticLayout.Builder.obtain(s, 0, s.length, p, w.coerceAtLeast(10)).setLineSpacing(add, 1f).setIncludePad(false).setAlignment(align)
            .setBreakStrategy(android.graphics.text.LineBreaker.BREAK_STRATEGY_HIGH_QUALITY).setHyphenationFrequency(Layout.HYPHENATION_FREQUENCY_NONE)
            .apply { if (max != Int.MAX_VALUE) { setMaxLines(max); setEllipsize(TextUtils.TruncateAt.END) }; if (justify) setJustificationMode(Layout.JUSTIFICATION_MODE_INTER_CHARACTER) }.build()
    }
    /** 标题自适应：从 start 字号往下缩，直到不超过 maxLines 行 */
    fun fit(s: String, color: Int, w: Int, maxLines: Int, start: Float, min: Float, lh: Float = 1.18f, serif: Boolean = false, align: Layout.Alignment = Layout.Alignment.ALIGN_NORMAL, spacing: Float = 0f): StaticLayout {
        var size = start
        while (size > min) {
            val l = lay(s, tp(size, color, true, serif, spacing), w, lh, align)
            if (l.lineCount <= maxLines) return l
            size -= 4f
        }
        return lay(s, tp(min, color, true, serif, spacing), w, lh, align, maxLines)
    }
    fun StaticLayout.at(c: Canvas, x: Float, y: Float) { c.save(); c.translate(x, y); draw(c); c.restore() }
    fun lineMaxRight(l: StaticLayout): Float { var m = 0f; for (i in 0 until l.lineCount) m = max(m, l.getLineRight(i)); return m }

    fun gradient(c: Canvas, w: Int, h: Int, vararg colors: Int, angleDown: Boolean = true) {
        val p = Paint().apply { shader = LinearGradient(0f, 0f, if (angleDown) w * 0.35f else 0f, h.toFloat(), colors, null, Shader.TileMode.CLAMP) }
        c.drawRect(0f, 0f, w.toFloat(), h.toFloat(), p)
    }
    fun blob(c: Canvas, cx: Float, cy: Float, r: Float, color: Int) {
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { shader = RadialGradient(cx, cy, r, color, color and 0x00FFFFFF, Shader.TileMode.CLAMP) }
        c.drawCircle(cx, cy, r, p)
    }
    fun star(c: Canvas, cx: Float, cy: Float, r: Float, color: Int, rot: Float = 0f, pts: Int = 5, inner: Float = 0.45f, p: Paint = fill(color)) {
        val path = Path()
        for (i in 0 until pts * 2) {
            val rr = if (i % 2 == 0) r else r * inner
            val a = Math.PI * i / pts - Math.PI / 2 + Math.toRadians(rot.toDouble())
            val x = cx + rr * cos(a).toFloat(); val y = cy + rr * sin(a).toFloat()
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close(); c.drawPath(path, p)
    }
    fun heart(c: Canvas, cx: Float, cy: Float, s: Float, p: Paint) {
        val path = Path()
        path.moveTo(cx, cy + s * 0.9f)
        path.cubicTo(cx - s * 1.5f, cy - s * 0.1f, cx - s * 0.7f, cy - s * 1.2f, cx, cy - s * 0.35f)
        path.cubicTo(cx + s * 0.7f, cy - s * 1.2f, cx + s * 1.5f, cy - s * 0.1f, cx, cy + s * 0.9f)
        c.drawPath(path, p)
    }
    fun tape(c: Canvas, cx: Float, cy: Float, w: Float, h: Float, rot: Float, color: Int, stripe: Int = 0) {
        c.save(); c.rotate(rot, cx, cy)
        val r = RectF(cx - w / 2, cy - h / 2, cx + w / 2, cy + h / 2)
        c.drawRect(r, fill(color))
        if (stripe != 0) { c.save(); c.clipRect(r); var x = r.left - h; while (x < r.right) { c.drawLine(x, r.bottom, x + h, r.top, stroke(stripe, 10f)); x += 30f }; c.restore() }
        // 锯齿边
        val z = fill(0x00000000); z.xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
        var y = r.top; while (y < r.bottom) { c.drawRect(r.left - 1, y, r.left + 5, y + 5, z); c.drawRect(r.right - 5, y, r.right + 1, y + 5, z); y += 10 }
        c.restore()
    }
    fun shadowRect(c: Canvas, r: RectF, rad: Float, color: Int, blur: Float, dy: Float, shadow: Int) {
        c.drawRoundRect(r, rad, rad, Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color; setShadowLayer(blur, 0f, dy, shadow) })
    }
    fun ell(s: String, p: TextPaint, w: Float) = TextUtils.ellipsize(s, p, w, TextUtils.TruncateAt.END).toString()
    fun center(c: Canvas, s: String, cx: Float, y: Float, p: Paint) { val old = p.textAlign; p.textAlign = Paint.Align.CENTER; c.drawText(s, cx, y, p); p.textAlign = old }
    fun right(c: Canvas, s: String, x: Float, y: Float, p: Paint) { val old = p.textAlign; p.textAlign = Paint.Align.RIGHT; c.drawText(s, x, y, p); p.textAlign = old }
    fun dots(c: Canvas, w: Int, h: Int, step: Float, r: Float, color: Int) { val p = fill(color); var y = step / 2; while (y < h) { var x = step / 2; while (x < w) { c.drawCircle(x, y, r, p); x += step }; y += step } }
    fun capLede(text: String, p: TextPaint, w: Int, capSize: Float, capColor: Int, lines: Int = 3, lh: Float = 1.5f): Pair<StaticLayout, Pair<String, Float>> {
        val cap = text.take(1); val rest = text.drop(1)
        val capW = (capSize * 0.95f)
        val span = SpannableString(rest)
        span.setSpan(object : LeadingMarginSpan.LeadingMarginSpan2 {
            override fun getLeadingMargin(first: Boolean) = if (first) capW.toInt() + 14 else 0
            override fun getLeadingMarginLineCount() = lines
            override fun drawLeadingMargin(c: Canvas?, p: Paint?, x: Int, dir: Int, top: Int, baseline: Int, bottom: Int, text: CharSequence?, start: Int, end: Int, first: Boolean, layout: Layout?) {}
        }, 0, rest.length, Spanned.SPAN_PARAGRAPH)
        return lay(span, p, w, lh, justify = true) to (cap to capSize)
    }
}

/** 由简到花共 7 套（液态玻璃与渐变弥散太像，已去掉）。 */
object XhsStyles {
    val all: List<XStyle> by lazy { listOf(Magazine(), Hairline(), Newspaper(), Note(), Aurora(), Scrap(), Pop()) }
    fun byId(id: String?) = all.firstOrNull { it.id == id } ?: all[0]
}

private fun day(date: String) = date.takeLast(2)
private fun pageStr(p: PageCtx) = "${p.page}/${p.total}"

// ================= 简约 =================
/** 留白杂志：暖白纸、左对齐大标题、锈红点缀（参考截图的气质） */
private class Magazine : XStyle("mag", "留白杂志", "暖白纸 · 大标题", false,
    Skin(0xFF2B2A27.toInt(), 0xFF8A857A.toInt(), 0xFF9A3412.toInt(), 0xFFF0E4D2.toInt(), 0xFFEFE8DA.toInt(), 0xFFDDD5C4.toInt(), boldColor = 0xFF8B2E0F.toInt(), h2 = HKind.Plain, h2Color = 0xFF1A1917.toInt(), quote = QKind.Bar), 0xFFF7F3EA.toInt()) {
    override fun insets(w: Int, h: Int) = intArrayOf(100, 200, 100, 190)
    override fun cover(c: Canvas, k: CoverCtx) = with(XD) {
        c.drawColor(paper)
        c.drawText(day(k.date), k.w - 60f, 560f, tp(560f, 0xFFEFE6D3.toInt(), true).also { it.textAlign = Paint.Align.RIGHT; it.letterSpacing = -0.04f })
        c.drawText(k.date, 100f, 130f, tp(30f, skin.text2, spacing = 0.12f))
        right(c, "READING NOTE", k.w - 100f, 130f, tp(30f, skin.text2, true, spacing = 0.18f))
        c.drawRect(100f, 160f, k.w - 100f, 162f, fill(skin.line))
        val tw = k.w - 200
        val t = fit(k.title, 0xFF1A1917.toInt(), tw, 4, 124f, 64f, 1.2f)
        val sn = if (k.snippet.isNotBlank()) lay(k.snippet, tp(38f, skin.text2), tw, 1.6f, max = 3) else null
        val total = 14 + 56 + t.height + (sn?.let { 52 + it.height } ?: 0)
        val bottom = k.h - 300
        var y = bottom - total
        c.drawRect(100f, y.toFloat(), 220f, y + 14f, fill(skin.accent)); y += 14 + 56
        t.at(c, 100f, y.toFloat()); y += t.height
        sn?.at(c, 100f, (y + 52).toFloat())
        c.drawRect(100f, k.h - 210f, k.w - 100f, k.h - 208f, fill(skin.line))
        c.drawText(k.meta, 100f, k.h - 140f, tp(30f, skin.text2))
        right(c, "拾页", k.w - 100f, k.h - 140f, tp(32f, skin.text, true, spacing = 0.2f))
    }
    override fun bodyBg(c: Canvas, p: PageCtx) { c.drawColor(paper) }
    override fun bodyFg(c: Canvas, p: PageCtx) = with(XD) {
        val hp = tp(28f, skin.text2)
        c.drawText(ell(p.title, hp, p.w - 400f), 100f, 112f, hp)
        right(c, p.date, p.w - 100f, 112f, hp)
        c.drawRect(100f, 140f, p.w - 100f, 142f, fill(skin.line))
        right(c, pageStr(p), p.w - 100f, p.h - 96f, tp(36f, skin.text, true))
        c.drawText("拾页", 100f, p.h - 96f, tp(28f, skin.text2, spacing = 0.2f))
    }
}

/** 极细线框：双层发丝线边框，居中标题，克制的衬线感 */
private class Hairline : XStyle("hair", "极细线框", "发丝双线 · 居中", false,
    Skin(0xFF222222.toInt(), 0xFF8A8A86.toInt(), 0xFF222222.toInt(), 0xFFEFEFEA.toInt(), 0xFFF0F0EC.toInt(), 0xFFD9D9D3.toInt(), headSerif = true, h2 = HKind.Underline, h2Color = 0xFF111111.toInt(), boldColor = 0xFF000000.toInt(), quote = QKind.Frame, spacing = 0.03f), 0xFFFBFBF8.toInt()) {
    override fun insets(w: Int, h: Int) = intArrayOf(130, 210, 130, 210)
    private fun frame(c: Canvas, w: Int, h: Int) = with(XD) {
        c.drawColor(paper)
        c.drawRect(44f, 44f, w - 44f, h - 44f, stroke(0xFF222222.toInt(), 2.5f))
        c.drawRect(58f, 58f, w - 58f, h - 58f, stroke(0xFF222222.toInt(), 1f))
        for ((x, y) in listOf(44f to 44f, w - 44f to 44f, 44f to h - 44f, w - 44f to h - 44f)) c.drawRect(x - 9, y - 9, x + 9, y + 9, fill(paper)).also { c.drawRect(x - 9, y - 9, x + 9, y + 9, stroke(0xFF222222.toInt(), 2.5f)) }
    }
    override fun cover(c: Canvas, k: CoverCtx) = with(XD) {
        frame(c, k.w, k.h)
        val cx = k.w / 2f; val tw = k.w - 300
        val kick = tp(28f, skin.text2, spacing = 0.5f)
        center(c, "R E A D I N G   N O T E", cx, 290f, kick)
        c.drawLine(cx - 40f, 330f, cx + 40f, 330f, stroke(skin.text, 2f))
        val t = fit(k.title, 0xFF111111.toInt(), tw, 4, 100f, 60f, 1.3f, serif = true, align = Layout.Alignment.ALIGN_CENTER, spacing = 0.04f)
        val sn = if (k.snippet.isNotBlank()) lay(k.snippet, tp(34f, skin.text2, spacing = 0.04f), tw, 1.8f, Layout.Alignment.ALIGN_CENTER, max = 4) else null
        val gap = 70
        val total = t.height + (sn?.let { gap * 2 + 10 + it.height } ?: 0)
        var y = (k.h - total) / 2f + 20
        t.at(c, (k.w - tw) / 2f, y); y += t.height
        if (sn != null) { y += gap; c.drawCircle(cx, y, 6f, fill(skin.text)); y += gap; sn.at(c, (k.w - tw) / 2f, y) }
        center(c, k.meta, cx, k.h - 150f, tp(26f, skin.text2, spacing = 0.1f))
        center(c, k.date + "  ·  拾页", cx, k.h - 190f, tp(26f, skin.text, true, spacing = 0.2f))
    }
    override fun bodyBg(c: Canvas, p: PageCtx) = frame(c, p.w, p.h)
    override fun bodyFg(c: Canvas, p: PageCtx) = with(XD) {
        val hp = tp(26f, skin.text2, spacing = 0.2f)
        center(c, ell(p.title, hp, p.w - 360f), p.w / 2f, 140f, hp)
        c.drawLine(p.w / 2f - 30f, 168f, p.w / 2f + 30f, 168f, stroke(skin.text, 2f))
        center(c, "—  ${pageStr(p)}  —", p.w / 2f, p.h - 130f, tp(28f, skin.text, spacing = 0.25f))
    }
}

/** 报纸排版：刊头双线、衬线正文两端对齐、首行缩进、首字下沉 */
private class Newspaper : XStyle("news", "报纸排版", "刊头 · 首字下沉", false,
    Skin(0xFF1C1A17.toInt(), 0xFF6F6A5E.toInt(), 0xFF8B1E1E.toInt(), 0xFFE5DFCF.toInt(), 0xFFE5DFCF.toInt(), 0xFFB9B2A0.toInt(), serif = true, headSerif = true, indent = true, h2 = HKind.Rules, h2Color = 0xFF111111.toInt(), quote = QKind.BigMark, spacing = 0.02f, boldColor = 0xFF8B1E1E.toInt(), bodyScale = 0.98f), 0xFFF0ECE1.toInt()) {
    override fun insets(w: Int, h: Int) = intArrayOf(96, 250, 96, 170)
    private fun rules(c: Canvas, x1: Float, x2: Float, y: Float) = with(XD) { c.drawRect(x1, y, x2, y + 6f, fill(skin.text)); c.drawRect(x1, y + 12f, x2, y + 14f, fill(skin.text)) }
    override fun cover(c: Canvas, k: CoverCtx) = with(XD) {
        c.drawColor(paper)
        val x1 = 90f; val x2 = k.w - 90f
        rules(c, x1, x2, 90f)
        center(c, "拾 页 日 报", k.w / 2f, 250f, tp(124f, skin.text, true, true, 0.05f))
        rules(c, x1, x2, 290f)
        val sp = tp(27f, skin.text, false, true, 0.08f)
        c.drawText("第 ${k.total} 版", x1, 372f, sp); center(c, k.date.replace('.', '年').replaceFirst("年", "年") + "日", k.w / 2f, 372f, sp); right(c, "约 ${k.minutes} 分钟", x2, 372f, sp)
        c.drawRect(x1, 400f, x2, 402f, fill(skin.text))
        val tw = (x2 - x1).toInt()
        val t = fit(k.title, 0xFF111111.toInt(), tw, 4, 112f, 62f, 1.2f, serif = true, align = Layout.Alignment.ALIGN_NORMAL)
        t.at(c, x1, 470f)
        var y = 470f + t.height + 40f
        c.drawRect(x1, y, x1 + 200f, y + 10f, fill(skin.accent)); y += 70f
        if (k.snippet.isNotBlank()) {
            val (l, cap) = capLede(k.snippet, tp(36f, skin.text, false, true, 0.02f), tw, 128f, skin.accent)
            c.drawText(cap.first, x1 - 4f, y + 112f, tp(cap.second, skin.accent, true, true, 0f))
            l.at(c, x1, y)
            y += l.height
        }
        c.drawRect(x1, k.h - 200f, x2, k.h - 198f, fill(skin.text))
        c.drawText(k.meta, x1, k.h - 140f, tp(26f, skin.text2, false, true, 0.06f))
        right(c, "拾页出品", x2, k.h - 140f, tp(26f, skin.text, true, true, 0.1f))
    }
    override fun bodyBg(c: Canvas, p: PageCtx) { c.drawColor(paper) }
    override fun bodyFg(c: Canvas, p: PageCtx) = with(XD) {
        val x1 = 90f; val x2 = p.w - 90f
        c.drawText("拾页日报", x1, 120f, tp(40f, skin.text, true, true, 0.08f))
        val hp = tp(26f, skin.text2, false, true, 0.05f)
        center(c, ell(p.title, hp, 460f), p.w / 2f + 50f, 118f, hp)
        right(c, p.date, x2, 118f, hp)
        rules(c, x1, x2, 152f)
        c.drawRect(x1, p.h - 150f, x2, p.h - 148f, fill(skin.text))
        center(c, "·  ${pageStr(p)}  ·", p.w / 2f, p.h - 96f, tp(30f, skin.text, true, true, 0.1f))
    }
}

/** 日系便签：牛皮纸底、纸片、和纸胶带、荧光笔划线、印章 */
private class Note : XStyle("note", "日系便签", "纸片 · 胶带 · 荧光笔", false,
    Skin(0xFF3A3128.toInt(), 0xFF9A8E7C.toInt(), 0xFFC8553D.toInt(), 0xFFFBEFC4.toInt(), 0xFFF3ECDC.toInt(), 0xFFE2D8C2.toInt(), h2 = HKind.Marker, h2Color = 0xFF2E261E.toInt(), h2Bg = 0xAAFFD966.toInt(), boldBg = 0x88FFE08A.toInt(), quote = QKind.Dashed), 0xFFE9E0CD.toInt()) {
    override fun insets(w: Int, h: Int) = intArrayOf(140, 220, 130, 200)
    private val paperC = 0xFFFFFDF6.toInt()
    private fun desk(c: Canvas, w: Int, h: Int) = with(XD) {
        c.drawColor(paper)
        dots(c, w, h, 36f, 2.2f, 0x22A39574)
    }
    override fun cover(c: Canvas, k: CoverCtx) = with(XD) {
        desk(c, k.w, k.h)
        val r = RectF(100f, 230f, k.w - 100f, k.h - 280f)
        c.save(); c.rotate(-2f, k.w / 2f, k.h / 2f)
        shadowRect(c, r, 14f, paperC, 36f, 14f, 0x40000000)
        // 横线
        val lp = stroke(0xFFE8DFCB.toInt(), 2f); var ly = r.top + 120f; while (ly < r.bottom - 40f) { c.drawLine(r.left + 50f, ly, r.right - 50f, ly, lp); ly += 64f }
        c.drawLine(r.left + 110f, r.top + 20f, r.left + 110f, r.bottom - 20f, stroke(0x66E58C7A, 3f))
        val tw = (r.width() - 240f).toInt()
        val t = fit(k.title, 0xFF2E261E.toInt(), tw, 5, 100f, 58f, 1.38f)
        val ty = r.top + 150f
        for (i in 0 until t.lineCount) c.drawRoundRect(r.left + 150f + t.getLineLeft(i) - 8f, ty + t.getLineBottom(i) - 44f, r.left + 150f + t.getLineRight(i) + 8f, ty + t.getLineBottom(i) - 8f, 8f, 8f, fill(0x99FFD966.toInt()))
        t.at(c, r.left + 150f, ty)
        if (k.snippet.isNotBlank()) lay(k.snippet, tp(34f, skin.text2), tw, 1.7f, max = 3).at(c, r.left + 150f, ty + t.height + 60f)
        tape(c, k.w / 2f, r.top + 6f, 260f, 64f, 3f, 0xCC9FD8C4.toInt(), 0x33FFFFFF)
        c.restore()
        // 印章
        c.save(); c.rotate(-12f, k.w - 230f, k.h - 190f)
        val sp = stroke(0xCCC8553D.toInt(), 6f); c.drawCircle(k.w - 230f, k.h - 190f, 70f, sp); c.drawCircle(k.w - 230f, k.h - 190f, 58f, stroke(0x88C8553D.toInt(), 2f))
        center(c, "拾页", k.w - 230f, k.h - 175f, tp(48f, 0xCCC8553D.toInt(), true))
        c.restore()
        c.drawText(k.date, 130f, k.h - 200f, tp(32f, skin.text2, true, spacing = 0.1f))
        c.drawText(k.meta, 130f, k.h - 150f, tp(28f, skin.text2))
    }
    override fun bodyBg(c: Canvas, p: PageCtx) = with(XD) {
        desk(c, p.w, p.h)
        shadowRect(c, RectF(60f, 90f, p.w - 60f, p.h - 90f), 16f, paperC, 30f, 12f, 0x38000000)
        c.drawLine(116f, 110f, 116f, p.h - 110f, stroke(0x55E58C7A, 3f))
        tape(c, 190f, 96f, 220f, 56f, -6f, 0xCCF5B7B1.toInt(), 0x44FFFFFF)
    }
    override fun bodyFg(c: Canvas, p: PageCtx) = with(XD) {
        val hp = tp(26f, skin.text2)
        c.drawText(ell(p.title, hp, p.w - 520f), 140f, 176f, hp)
        c.drawCircle(p.w - 150f, p.h - 150f, 46f, fill(0xFFFBEFC4.toInt())); c.drawCircle(p.w - 150f, p.h - 150f, 46f, stroke(0xFFE8C766.toInt(), 3f))
        center(c, pageStr(p), p.w - 150f, p.h - 140f, tp(28f, skin.text, true))
    }
}

// ================= 花里胡哨 =================
/** 渐变弥散：粉蓝橘弥散光斑 + 磨砂白卡片 */
private class Aurora : XStyle("aurora", "渐变弥散", "弥散光 · 磨砂卡", true,
    Skin(0xFF1F2340.toInt(), 0xFF6B7194.toInt(), 0xFF5B3FD6.toInt(), 0xFFE7DEFF.toInt(), 0xFFEEEAFB.toInt(), 0xFFD9D4F2.toInt(), h2 = HKind.Pill, h2Color = 0xFF5B3FD6.toInt(), h2Bg = 0xFFE3D9FF.toInt(), h2Text = 0xFF4A2FC0.toInt(), boldColor = 0xFF5B3FD6.toInt(), quote = QKind.Box, spacing = 0.012f), 0xFFFBE9F2.toInt()) {
    override fun insets(w: Int, h: Int) = intArrayOf(120, 220, 120, 230)
    private fun mesh(c: Canvas, w: Int, h: Int, seed: Int) = with(XD) {
        gradient(c, w, h, 0xFFFFE6EF.toInt(), 0xFFE8EEFF.toInt(), 0xFFFFEBDD.toInt())
        val f = h / 1440f
        val s = seed % 3
        blob(c, w * (0.1f + 0.2f * s), h * 0.08f, 560f * f, 0xCCFF9EC4.toInt())
        blob(c, w * 0.95f, h * (0.3f + 0.1f * s), 520f * f, 0xCC8EB5FF.toInt())
        blob(c, w * 0.15f, h * 0.62f, 540f * f, 0xB3FFC9A3.toInt())
        blob(c, w * 0.85f, h * 0.95f, 600f * f, 0xB3C3A6FF.toInt())
        blob(c, w * 0.5f, h * 0.5f, 360f * f, 0x66FFFFFF)
    }
    override fun cover(c: Canvas, k: CoverCtx) = with(XD) {
        mesh(c, k.w, k.h, 0)
        val lab = tp(30f, 0xFF5B3FD6.toInt(), true, spacing = 0.15f)
        val lw = lab.measureText("READING NOTE") + 70f
        val pill = RectF(100f, 150f, 100f + lw, 226f)
        c.drawRoundRect(pill, 38f, 38f, fill(0xAAFFFFFF.toInt())); c.drawRoundRect(pill, 38f, 38f, stroke(0xFFFFFFFF.toInt(), 3f))
        c.drawText("READING NOTE", 135f, 200f, lab)
        val tw = k.w - 200
        val t = fit(k.title, 0xFF1F2340.toInt(), tw, 4, 128f, 66f, 1.18f)
        val sn = if (k.snippet.isNotBlank()) lay(k.snippet, tp(38f, 0xFF4B5075.toInt()), tw - 40, 1.6f, max = 3) else null
        val total = t.height + (sn?.let { 60 + 80 + it.height } ?: 0)
        var y = (k.h * 0.52f - total / 2f).coerceAtLeast(300f)
        t.at(c, 100f, y); y += t.height
        if (sn != null) {
            y += 60
            val card = RectF(100f, y, k.w - 100f, y + sn.height + 80f)
            c.drawRoundRect(card, 48f, 48f, fill(0x88FFFFFF.toInt())); c.drawRoundRect(card, 48f, 48f, stroke(0xCCFFFFFF.toInt(), 3f))
            sn.at(c, 140f, y + 40f)
        }
        c.drawText(k.date, 100f, k.h - 130f, tp(32f, 0xFF4B5075.toInt(), true, spacing = 0.1f))
        right(c, "拾页", k.w - 100f, k.h - 130f, tp(34f, 0xFF5B3FD6.toInt(), true, spacing = 0.2f))
    }
    override fun bodyBg(c: Canvas, p: PageCtx) = with(XD) {
        mesh(c, p.w, p.h, p.page)
        val r = RectF(56f, 110f, p.w - 56f, p.h - 110f)
        shadowRect(c, r, 70f, 0xC4FFFFFF.toInt(), 50f, 18f, 0x22503AA0)
        c.drawRoundRect(r, 70f, 70f, stroke(0xEEFFFFFF.toInt(), 3f))
    }
    override fun bodyFg(c: Canvas, p: PageCtx) = with(XD) {
        val hp = tp(27f, skin.text2)
        c.drawCircle(130f, 190f, 9f, fill(skin.accent))
        c.drawText(ell(p.title, hp, p.w - 330f), 160f, 200f, hp)
        val pp = tp(30f, 0xFF5B3FD6.toInt(), true); val s = pageStr(p); val pw = pp.measureText(s) + 56f
        val r = RectF(p.w / 2f - pw / 2, p.h - 200f, p.w / 2f + pw / 2, p.h - 136f)
        c.drawRoundRect(r, 32f, 32f, fill(0xFFFFFFFF.toInt())); c.drawRoundRect(r, 32f, 32f, stroke(0xFFE3D9FF.toInt(), 3f))
        center(c, s, p.w / 2f, p.h - 156f, pp)
    }
}

/** 液态玻璃：深紫蓝渐变 + 高光玻璃板，浅色文字 */
private class Glass : XStyle("glass", "液态玻璃", "深色 · 高光玻璃", true,
    Skin(0xFFF2F4FF.toInt(), 0xFFB4BEEA.toInt(), 0xFF9CC3FF.toInt(), 0x30FFFFFF, 0x26000000, 0x33FFFFFF, dark = true, h2 = HKind.Dot, h2Color = 0xFFFFFFFF.toInt(), boldColor = 0xFFA8D4FF.toInt(), quote = QKind.Box, spacing = 0.012f), 0xFF1B1F4B.toInt()) {
    override fun insets(w: Int, h: Int) = intArrayOf(124, 230, 124, 240)
    private fun bg(c: Canvas, w: Int, h: Int, seed: Int) = with(XD) {
        gradient(c, w, h, 0xFF171A45.toInt(), 0xFF3B2A8F.toInt(), 0xFF0F6FB8.toInt())
        val f = h / 1440f
        blob(c, w * (0.85f - 0.3f * (seed % 2)), h * 0.12f, 520f * f, 0xCC2DD4FF.toInt())
        blob(c, w * 0.1f, h * (0.55f + 0.1f * (seed % 2)), 560f * f, 0xB3FF4FD8.toInt())
        blob(c, w * 0.9f, h * 0.88f, 520f * f, 0xB36A5BFF.toInt())
    }
    private fun glass(c: Canvas, r: RectF, rad: Float, alpha: Int = 0x2E) = with(XD) {
        c.drawRoundRect(r, rad, rad, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = (alpha shl 24) or 0xFFFFFF; setShadowLayer(40f, 0f, 16f, 0x55000020) })
        c.drawRoundRect(r, rad, rad, fill((alpha shl 24) or 0xFFFFFF))
        // 顶部高光
        c.save(); c.clipRect(r.left, r.top, r.right, r.top + r.height() * 0.45f)
        c.drawRoundRect(r, rad, rad, Paint(Paint.ANTI_ALIAS_FLAG).apply { shader = LinearGradient(0f, r.top, 0f, r.top + r.height() * 0.45f, 0x40FFFFFF, 0x00FFFFFF, Shader.TileMode.CLAMP) })
        c.restore()
        c.drawRoundRect(r, rad, rad, Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 3f; shader = LinearGradient(r.left, r.top, r.right, r.bottom, 0xCCFFFFFF.toInt(), 0x11FFFFFF, Shader.TileMode.CLAMP) })
    }
    override fun cover(c: Canvas, k: CoverCtx) = with(XD) {
        bg(c, k.w, k.h, 0)
        // 玻璃球
        val cx = k.w * 0.68f; val cy = k.h * 0.2f; val r = 230f
        c.drawCircle(cx, cy, r, Paint(Paint.ANTI_ALIAS_FLAG).apply { shader = RadialGradient(cx - 70f, cy - 80f, r * 1.2f, intArrayOf(0x88FFFFFF.toInt(), 0x22FFFFFF, 0x11FFFFFF), floatArrayOf(0f, 0.6f, 1f), Shader.TileMode.CLAMP) })
        c.drawCircle(cx, cy, r, Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 4f; shader = LinearGradient(cx - r, cy - r, cx + r, cy + r, 0xEEFFFFFF.toInt(), 0x11FFFFFF, Shader.TileMode.CLAMP) })
        c.drawCircle(cx - 80f, cy - 90f, 36f, fill(0x66FFFFFF))
        c.drawCircle(130f + 40f, k.h * 0.27f, 70f, fill(0x22FFFFFF)); c.drawCircle(170f, k.h * 0.27f, 70f, stroke(0x66FFFFFF, 3f))
        val tw = k.w - 300
        val t = fit(k.title, 0xFFFFFFFF.toInt(), tw, 4, 118f, 62f, 1.2f)
        val sn = if (k.snippet.isNotBlank()) lay(k.snippet, tp(34f, 0xFFD3DAFF.toInt()), tw, 1.65f, max = 3) else null
        val ph = 80 + t.height + (sn?.let { 50 + it.height } ?: 0) + 80
        val top = k.h * 0.4f.coerceAtLeast(0f)
        val panel = RectF(90f, top.coerceAtMost(k.h - ph - 300f).coerceAtLeast(k.h * 0.33f), k.w - 90f, 0f).also { it.bottom = it.top + ph }
        glass(c, panel, 64f)
        t.at(c, 150f, panel.top + 80f)
        sn?.at(c, 150f, panel.top + 80f + t.height + 50f)
        val lab = tp(28f, 0xFFFFFFFF.toInt(), true, spacing = 0.18f)
        c.drawText("READING NOTE", 100f, 170f, lab)
        c.drawText(k.date, 100f, k.h - 130f, tp(32f, 0xFFD3DAFF.toInt(), true, spacing = 0.1f))
        right(c, "拾页", k.w - 100f, k.h - 130f, tp(34f, 0xFFFFFFFF.toInt(), true, spacing = 0.2f))
    }
    override fun bodyBg(c: Canvas, p: PageCtx) { bg(c, p.w, p.h, p.page); glass(c, RectF(56f, 110f, p.w - 56f, p.h - 110f), 70f, 0x26) }
    override fun bodyFg(c: Canvas, p: PageCtx) = with(XD) {
        val hp = tp(27f, 0xFFB4BEEA.toInt())
        c.drawCircle(136f, 192f, 9f, fill(0xFF9CC3FF.toInt()))
        c.drawText(ell(p.title, hp, p.w - 330f), 166f, 202f, hp)
        val pp = tp(30f, 0xFFFFFFFF.toInt(), true); val s = pageStr(p); val pw = pp.measureText(s) + 56f
        glass(c, RectF(p.w / 2f - pw / 2, p.h - 204f, p.w / 2f + pw / 2, p.h - 140f), 32f, 0x33)
        center(c, s, p.w / 2f, p.h - 160f, pp)
    }
}

/** 手账贴纸：点阵纸、和纸胶带、星星爱心贴纸、荧光标签 */
private class Scrap : XStyle("scrap", "手账贴纸", "胶带 · 贴纸 · 缝线", true,
    Skin(0xFF3A2A2A.toInt(), 0xFFA58A8A.toInt(), 0xFFFF6B8B.toInt(), 0xFFFFE9ED.toInt(), 0xFFFFF1E6.toInt(), 0xFFF3D6D6.toInt(), h2 = HKind.Pill, h2Color = 0xFF3A2A2A.toInt(), h2Bg = 0xFFFFE066.toInt(), h2Text = 0xFF3A2A2A.toInt(), boldBg = 0xAAFFE6A8.toInt(), boldColor = 0xFFE0456B.toInt(), quote = QKind.Dashed, spacing = 0.012f), 0xFFFFF1F0.toInt()) {
    override fun insets(w: Int, h: Int) = intArrayOf(130, 230, 120, 220)
    private fun desk(c: Canvas, w: Int, h: Int) = with(XD) { c.drawColor(paper); dots(c, w, h, 44f, 3.2f, 0xFFFFD3D3.toInt()) }
    private fun smiley(c: Canvas, cx: Float, cy: Float, r: Float) = with(XD) {
        c.drawCircle(cx, cy, r + 8f, fill(0xFFFFFFFF.toInt())); c.drawCircle(cx, cy, r, fill(0xFFFFD84D.toInt()))
        c.drawCircle(cx - r * 0.35f, cy - r * 0.15f, r * 0.1f, fill(0xFF3A2A2A.toInt())); c.drawCircle(cx + r * 0.35f, cy - r * 0.15f, r * 0.1f, fill(0xFF3A2A2A.toInt()))
        c.drawArc(RectF(cx - r * 0.5f, cy - r * 0.25f, cx + r * 0.5f, cy + r * 0.55f), 20f, 140f, false, stroke(0xFF3A2A2A.toInt(), r * 0.09f).also { it.strokeCap = Paint.Cap.ROUND })
        c.drawCircle(cx - r * 0.6f, cy + r * 0.2f, r * 0.14f, fill(0x66FF6B8B)); c.drawCircle(cx + r * 0.6f, cy + r * 0.2f, r * 0.14f, fill(0x66FF6B8B))
    }
    private fun sticker(c: Canvas, draw: (Paint) -> Unit) { draw(XD.fill(0xFFFFFFFF.toInt())) }
    override fun cover(c: Canvas, k: CoverCtx) = with(XD) {
        desk(c, k.w, k.h)
        val tw = k.w - 300
        val t = fit(k.title, 0xFF3A2A2A.toInt(), tw, 4, 110f, 60f, 1.28f)
        val sn = if (k.snippet.isNotBlank()) lay(k.snippet, tp(34f, skin.text2), tw, 1.7f, max = 3) else null
        val ph = 120 + t.height + (sn?.let { 50 + it.height } ?: 0) + 90
        val top = (k.h * 0.5f - ph / 2f).coerceAtLeast(300f)
        val card = RectF(90f, top, k.w - 90f, top + ph)
        c.save(); c.rotate(-2.5f, k.w / 2f, card.centerY())
        shadowRect(c, card, 44f, 0xFFFFFFFF.toInt(), 30f, 14f, 0x33C06070)
        c.drawRoundRect(RectF(card.left + 22, card.top + 22, card.right - 22, card.bottom - 22), 30f, 30f, stroke(0xFFFFB3C1.toInt(), 4f).also { it.pathEffect = DashPathEffect(floatArrayOf(18f, 14f), 0f) })
        val ty = card.top + 110f
        for (i in 0 until t.lineCount) c.drawRoundRect(card.left + 80f + t.getLineLeft(i) - 8f, ty + t.getLineBottom(i) - 42f, card.left + 80f + t.getLineRight(i) + 8f, ty + t.getLineBottom(i) - 8f, 14f, 14f, fill(0xCCFFE066.toInt()))
        t.at(c, card.left + 80f, ty)
        sn?.at(c, card.left + 80f, ty + t.height + 50f)
        tape(c, card.centerX(), card.top + 4f, 300f, 70f, 2f, 0xCCFFC2D1.toInt(), 0x66FFFFFF)
        c.restore()
        star(c, 170f, top - 120f, 90f, 0xFFFFD84D.toInt(), -12f, p = fill(0xFFFFD84D.toInt()).also { it.setShadowLayer(10f, 0f, 6f, 0x33000000) })
        heart(c, k.w - 170f, top - 90f, 64f, fill(0xFFFF6B8B.toInt()).also { it.setShadowLayer(10f, 0f, 6f, 0x33000000) })
        smiley(c, k.w - 220f, card.bottom + 130f, 78f)
        star(c, 160f, card.bottom + 120f, 46f, 0xFF9DD6FF.toInt(), 14f)
        c.drawText(k.date, 100f, k.h - 130f, tp(32f, skin.text2, true, spacing = 0.1f))
        right(c, k.meta, k.w - 100f, k.h - 130f, tp(26f, skin.text2))
    }
    override fun bodyBg(c: Canvas, p: PageCtx) = with(XD) {
        desk(c, p.w, p.h)
        val r = RectF(60f, 110f, p.w - 60f, p.h - 100f)
        shadowRect(c, r, 54f, 0xFFFFFFFF.toInt(), 30f, 12f, 0x33C06070)
        c.drawRoundRect(RectF(r.left + 20, r.top + 20, r.right - 20, r.bottom - 20), 40f, 40f, stroke(0xFFFFC4CF.toInt(), 3f).also { it.pathEffect = DashPathEffect(floatArrayOf(14f, 12f), 0f) })
        tape(c, p.w * 0.5f, 112f, 280f, 62f, -2f, 0xCCFFE066.toInt(), 0x55FFFFFF)
    }
    override fun bodyFg(c: Canvas, p: PageCtx) = with(XD) {
        val hp = tp(26f, skin.text2)
        c.drawText(ell(p.title, hp, p.w - 400f), 130f, 190f, hp)
        star(c, p.w - 160f, p.h - 160f, 66f, 0xFFFFD84D.toInt(), 10f, p = fill(0xFFFFD84D.toInt()).also { it.setShadowLayer(6f, 0f, 4f, 0x33000000) })
        center(c, pageStr(p), p.w - 160f, p.h - 152f, tp(26f, skin.text, true))
        heart(c, 150f, p.h - 150f, 26f, fill(0xFFFF6B8B.toInt()))
    }
}

/** 撞色波普：明黄底、粗黑描边、硬阴影、粉蓝色块 */
private class Pop : XStyle("pop", "撞色波普", "粗描边 · 硬阴影", true,
    Skin(0xFF111111.toInt(), 0xFF6B6B6B.toInt(), 0xFF2F5BFF.toInt(), 0xFFFFD6E8.toInt(), 0xFFEFEFEF.toInt(), 0xFF111111.toInt(), h2 = HKind.Pill, h2Color = 0xFFFFD93D.toInt(), h2Bg = 0xFF111111.toInt(), h2Text = 0xFFFFD93D.toInt(), boldBg = 0xFFFFE14D.toInt(), boldColor = 0xFF111111.toInt(), quote = QKind.Box, spacing = 0.01f), 0xFFFFD93D.toInt()) {
    override fun insets(w: Int, h: Int) = intArrayOf(134, 240, 130, 230)
    private val ink = 0xFF111111.toInt()
    private fun hardBox(c: Canvas, r: RectF, rad: Float, color: Int, off: Float = 16f, shadow: Int = ink) = with(XD) {
        c.drawRoundRect(RectF(r.left + off, r.top + off, r.right + off, r.bottom + off), rad, rad, fill(shadow))
        c.drawRoundRect(r, rad, rad, fill(color)); c.drawRoundRect(r, rad, rad, stroke(ink, 7f))
    }
    override fun cover(c: Canvas, k: CoverCtx) = with(XD) {
        c.drawColor(paper)
        dots(c, k.w, k.h, 34f, 3f, 0x33FF8A00)
        c.drawCircle(k.w - 140f, 250f, 230f, fill(0xFFFF4F8B.toInt())); c.drawCircle(k.w - 140f, 250f, 230f, stroke(ink, 7f))
        c.drawCircle(130f, k.h - 330f, 150f, fill(0xFF2F5BFF.toInt())); c.drawCircle(130f, k.h - 330f, 150f, stroke(ink, 7f))
        star(c, k.w - 220f, k.h - 360f, 120f, 0xFFFFFFFF.toInt(), 8f, 8, 0.6f, stroke(ink, 7f).also { it.style = Paint.Style.FILL_AND_STROKE; it.color = 0xFFFFFFFF.toInt() })
        star(c, k.w - 220f, k.h - 360f, 120f, ink, 8f, 8, 0.6f, stroke(ink, 7f))
        val tw = k.w - 300
        val t = fit(k.title, ink, tw, 4, 120f, 64f, 1.18f)
        val sn = if (k.snippet.isNotBlank()) lay(k.snippet, tp(34f, 0xFF333333.toInt()), tw, 1.6f, max = 3) else null
        val ph = 100 + t.height + (sn?.let { 50 + it.height } ?: 0) + 90
        val top = (k.h * 0.5f - ph / 2f).coerceAtMost(k.h - ph - 360f).coerceAtLeast(300f)
        val card = RectF(90f, top, k.w - 120f, top + ph)
        hardBox(c, card, 36f, 0xFFFFFEF7.toInt(), 20f)
        t.at(c, 140f, top + 100f)
        sn?.at(c, 140f, top + 100f + t.height + 50f)
        // 标签
        val lab = tp(34f, paper, true, spacing = 0.12f)
        val lw = lab.measureText("READING NOTE") + 70f
        val tag = RectF(90f, top - 60f, 90f + lw, top + 24f)
        c.save(); c.rotate(-4f, tag.centerX(), tag.centerY()); c.drawRoundRect(tag, 42f, 42f, fill(ink)); c.drawText("READING NOTE", tag.left + 35f, tag.bottom - 28f, lab); c.restore()
        c.drawText(k.date, 100f, k.h - 130f, tp(36f, ink, true, spacing = 0.1f))
        right(c, "拾页", k.w - 100f, k.h - 130f, tp(40f, ink, true, spacing = 0.2f))
    }
    override fun bodyBg(c: Canvas, p: PageCtx) = with(XD) {
        c.drawColor(paper)
        dots(c, p.w, p.h, 34f, 3f, 0x33FF8A00)
        hardBox(c, RectF(60f, 120f, p.w - 76f, p.h - 120f), 40f, 0xFFFFFEF7.toInt(), 16f)
    }
    override fun bodyFg(c: Canvas, p: PageCtx) = with(XD) {
        val hp = tp(26f, paper, true, spacing = 0.04f)
        val tt = ell(p.title, hp, p.w - 460f)
        val w = hp.measureText(tt) + 60f
        val pill = RectF(110f, 152f, 110f + w, 222f)
        c.drawRoundRect(pill, 35f, 35f, fill(ink)); c.drawText(tt, 140f, 198f, hp)
        c.drawCircle(p.w - 150f, p.h - 166f, 54f, fill(0xFF2F5BFF.toInt())); c.drawCircle(p.w - 150f, p.h - 166f, 54f, stroke(ink, 6f))
        center(c, pageStr(p), p.w - 150f, p.h - 154f, tp(30f, 0xFFFFFFFF.toInt(), true))
    }
}
