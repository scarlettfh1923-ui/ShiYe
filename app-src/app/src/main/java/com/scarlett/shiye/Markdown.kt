package com.scarlett.shiye

/** 行内片段：文字 + 样式；img 不为空时表示一张图（text 是 alt）。 */
data class Seg(val text: String, val b: Boolean = false, val i: Boolean = false, val s: Boolean = false,
               val code: Boolean = false, val link: String? = null, val img: String? = null)

data class ListItem(val blocks: List<MdBlock>, val checked: Boolean?)

sealed class MdBlock {
    data class Heading(val level: Int, val segs: List<Seg>) : MdBlock()
    data class Para(val segs: List<Seg>) : MdBlock()
    data class Code(val lang: String, val text: String) : MdBlock()
    data class Quote(val blocks: List<MdBlock>) : MdBlock()
    data class Callout(val kind: String, val title: String, val blocks: List<MdBlock>) : MdBlock()
    data class Lst(val ordered: Boolean, val start: Int, val items: List<ListItem>) : MdBlock()
    data class Table(val header: List<List<Seg>>, val align: List<Int>, val rows: List<List<List<Seg>>>) : MdBlock()   // align: 0 左 1 中 2 右
    data class Image(val alt: String, val url: String) : MdBlock()
    data object Hr : MdBlock()
}

fun List<Seg>.plain() = joinToString("") { it.text }

/** 自带的 Markdown 解析器：标题 / 段落 / 列表（含任务、嵌套）/ 引用与 Obsidian 标注 / 围栏代码 / 表格 / 图片 / 分隔线 / 行内样式。 */
object Markdown {
    private val fenceRe = Regex("^\\s{0,3}(```+|~~~+)\\s*([^`\\s]*).*$")
    private val headRe = Regex("^\\s{0,3}(#{1,6})\\s+(.*?)\\s*#*\\s*$")
    private val hrRe = Regex("^\\s{0,3}([-*_])(\\s*\\1){2,}\\s*$")
    private val listRe = Regex("^(\\s*)([-*+]|\\d{1,9}[.)])\\s+(.*)$")
    private val tableSepRe = Regex("^\\s*\\|?\\s*:?-{1,}:?\\s*(\\|\\s*:?-{1,}:?\\s*)*\\|?\\s*$")
    private val calloutRe = Regex("^\\[!([A-Za-z]+)][+-]?\\s*(.*)$")
    private val htmlImgRe = Regex("<img\\b[^>]*>", RegexOption.IGNORE_CASE)

    fun stripFrontMatter(src: String): Pair<String, Map<String, String>> {
        val s = src.removePrefix("\uFEFF")
        if (!s.startsWith("---")) return s to emptyMap()
        val lines = s.lines()
        if (lines.firstOrNull()?.trim() != "---") return s to emptyMap()
        val end = (1 until minOf(lines.size, 60)).firstOrNull { lines[it].trim() == "---" || lines[it].trim() == "..." } ?: return s to emptyMap()
        val meta = mutableMapOf<String, String>()
        lines.subList(1, end).forEach { l -> val k = l.substringBefore(':', "").trim(); if (k.isNotEmpty() && !l.startsWith(" ")) meta[k.lowercase()] = l.substringAfter(':').trim().trim('"', '\'') }
        return lines.drop(end + 1).joinToString("\n") to meta
    }

    fun parse(src: String): List<MdBlock> = parseBlocks(stripFrontMatter(src.replace("\r\n", "\n").replace('\r', '\n')).first.lines())

    fun tags(meta: Map<String, String>): List<String> =
        (meta["tags"] ?: meta["tag"] ?: "").trim('[', ']').split(',', ' ').map { it.trim().trim('"', '\'', '#') }.filter { it.isNotEmpty() }.take(10)

    private fun indentOf(l: String): Int { var n = 0; for (c in l) { if (c == ' ') n++ else if (c == '\t') n += 4 else break }; return n }
    private fun isBlockStart(l: String) = fenceRe.matches(l) || headRe.matches(l) || hrRe.matches(l) || l.trimStart().startsWith(">") ||
        (listRe.matchEntire(l)?.let { it.groupValues[1].length < 4 } == true)

    fun parseBlocks(lines: List<String>): List<MdBlock> {
        val out = mutableListOf<MdBlock>()
        var i = 0
        while (i < lines.size) {
            val line = lines[i]
            if (line.isBlank()) { i++; continue }
            val fm = fenceRe.matchEntire(line)
            if (fm != null) {
                val fence = fm.groupValues[1]; val lang = fm.groupValues[2]; val ind = indentOf(line)
                val buf = mutableListOf<String>(); i++
                while (i < lines.size && !(lines[i].trim().startsWith(fence.take(3)) && lines[i].trim().trimStart(fence[0]).isBlank())) {
                    buf += lines[i].let { l -> if (indentOf(l) >= ind) l.drop(ind.coerceAtMost(l.length)) else l.trimStart() }; i++
                }
                i++; out += MdBlock.Code(lang, buf.joinToString("\n")); continue
            }
            val hm = headRe.matchEntire(line)
            if (hm != null) { out += MdBlock.Heading(hm.groupValues[1].length, parseInline(hm.groupValues[2])); i++; continue }
            if (hrRe.matches(line)) { out += MdBlock.Hr; i++; continue }
            // 表格
            if (line.contains('|') && i + 1 < lines.size && tableSepRe.matches(lines[i + 1]) && lines[i + 1].contains('-')) {
                val header = splitRow(line); val sep = splitRow(lines[i + 1])
                if (header.size == sep.size || sep.size >= 1) {
                    val align = header.indices.map { k -> val c = sep.getOrNull(k)?.trim().orEmpty(); when { c.startsWith(":") && c.endsWith(":") -> 1; c.endsWith(":") -> 2; else -> 0 } }
                    val rows = mutableListOf<List<List<Seg>>>(); i += 2
                    while (i < lines.size && lines[i].isNotBlank() && lines[i].contains('|')) { val r = splitRow(lines[i]); rows += header.indices.map { k -> parseInline(r.getOrElse(k) { "" }) }; i++ }
                    out += MdBlock.Table(header.map { parseInline(it) }, align, rows); continue
                }
            }
            // 引用 / 标注
            if (line.trimStart().startsWith(">")) {
                val buf = mutableListOf<String>()
                while (i < lines.size && lines[i].isNotBlank() && (lines[i].trimStart().startsWith(">") || buf.isNotEmpty() && !isBlockStart(lines[i]))) {
                    buf += lines[i].trimStart().let { if (it.startsWith(">")) it.removePrefix(">").removePrefix(" ") else it }; i++
                }
                val first = buf.firstOrNull()?.trim().orEmpty()
                val cm = calloutRe.matchEntire(first)
                if (cm != null) out += MdBlock.Callout(cm.groupValues[1].lowercase(), cm.groupValues[2].trim(), parseBlocks(buf.drop(1)))
                else out += MdBlock.Quote(parseBlocks(buf))
                continue
            }
            // 列表
            val m0 = listRe.matchEntire(line)?.takeIf { it.groupValues[1].length < 4 }
            if (m0 != null) {
                val baseIndent = m0.groupValues[1].length
                val ordered = m0.groupValues[2][0].isDigit(); val start = if (ordered) m0.groupValues[2].takeWhile { it.isDigit() }.toIntOrNull() ?: 1 else 1
                val items = mutableListOf<ListItem>()
                while (i < lines.size) {
                    val lm = listRe.matchEntire(lines[i]) ?: break
                    if (lm.groupValues[1].length != baseIndent && !(lm.groupValues[1].length in baseIndent..baseIndent + 1)) break
                    if (lm.groupValues[2][0].isDigit() != ordered) break
                    val contentIndent = lm.groupValues[1].length + lm.groupValues[2].length + 1
                    var first = lm.groupValues[3]; var checked: Boolean? = null
                    Regex("^\\[( |x|X)]\\s+(.*)$").matchEntire(first)?.let { checked = it.groupValues[1] != " "; first = it.groupValues[2] }
                    val body = mutableListOf(first); i++
                    while (i < lines.size) {
                        val l = lines[i]
                        if (l.isBlank()) { // 空行后若仍是缩进内容则属于本项
                            val nx = lines.drop(i + 1).firstOrNull { it.isNotBlank() }
                            if (nx != null && indentOf(nx) >= contentIndent) { body += ""; i++; continue } else break
                        }
                        if (indentOf(l) >= contentIndent || indentOf(l) > baseIndent + 1) { body += l.drop(minOf(contentIndent, indentOf(l).coerceAtMost(l.length))); i++; continue }
                        if (listRe.matches(l) || isBlockStart(l)) break
                        body += l.trim(); i++   // 懒续行
                    }
                    items += ListItem(parseBlocks(body), checked)
                }
                out += MdBlock.Lst(ordered, start, items); continue
            }
            // 段落（含 setext 标题、HTML 图片）
            val buf = mutableListOf<String>()
            while (i < lines.size && lines[i].isNotBlank()) {
                if (buf.isNotEmpty()) {
                    if (Regex("^\\s{0,3}=+\\s*$").matches(lines[i])) { out += MdBlock.Heading(1, parseInline(buf.joinToString(" "))); buf.clear(); i++; break }
                    if (Regex("^\\s{0,3}-+\\s*$").matches(lines[i])) { out += MdBlock.Heading(2, parseInline(buf.joinToString(" "))); buf.clear(); i++; break }
                    if (isBlockStart(lines[i]) || (lines[i].contains('|') && i + 1 < lines.size && tableSepRe.matches(lines[i + 1]) && lines[i + 1].contains('-'))) break
                }
                buf += lines[i]; i++
            }
            if (buf.isNotEmpty()) addParagraph(out, buf)
        }
        return out
    }

    private fun addParagraph(out: MutableList<MdBlock>, buf: List<String>) {
        var text = buf.joinToString("\n") { it.trimEnd().removeSuffix("\\") }
        // HTML <img> → Markdown 图片；其余 HTML 标签直接去掉
        text = htmlImgRe.replace(text) { m ->
            val src = Regex("src=[\"']([^\"']+)[\"']", RegexOption.IGNORE_CASE).find(m.value)?.groupValues?.get(1) ?: return@replace ""
            val alt = Regex("alt=[\"']([^\"']*)[\"']", RegexOption.IGNORE_CASE).find(m.value)?.groupValues?.get(1).orEmpty()
            "![$alt]($src)" }
        text = text.replace(Regex("<br\\s*/?>", RegexOption.IGNORE_CASE), "\n").replace(Regex("</?(p|div|span|center|details|summary|section|font|u|sup|sub|small|mark|kbd)[^>]*>", RegexOption.IGNORE_CASE), "")
        val segs = parseInline(text)
        var cur = mutableListOf<Seg>()
        fun flush() { if (cur.isNotEmpty() && cur.any { it.text.isNotBlank() }) out += MdBlock.Para(trimEnds(cur)); cur = mutableListOf() }
        for (s in segs) { if (s.img != null) { flush(); out += MdBlock.Image(s.text, s.img) } else cur += s }
        flush()
    }
    private fun trimEnds(l: List<Seg>): List<Seg> {
        if (l.isEmpty()) return l
        val r = l.toMutableList()
        while (r.isNotEmpty() && r.first().text.isBlank() && r.first().img == null) r.removeAt(0)
        while (r.isNotEmpty() && r.last().text.isBlank() && r.last().img == null) r.removeAt(r.lastIndex)
        return r
    }

    private fun splitRow(l: String): List<String> {
        var s = l.trim(); if (s.startsWith("|")) s = s.drop(1); if (s.endsWith("|") && !s.endsWith("\\|")) s = s.dropLast(1)
        val cells = mutableListOf<String>(); val sb = StringBuilder(); var inCode = false; var k = 0
        while (k < s.length) { val c = s[k]
            if (c == '`') inCode = !inCode
            if (c == '\\' && k + 1 < s.length && s[k + 1] == '|') { sb.append('|'); k += 2; continue }
            if (c == '|' && !inCode) { cells += sb.toString().trim(); sb.clear() } else sb.append(c); k++ }
        cells += sb.toString().trim(); return cells
    }

    // ---------- 行内 ----------
    fun parseInline(s: String): List<Seg> {
        val out = mutableListOf<Seg>(); inline(s, Seg(""), out); return merge(out)
    }
    private fun merge(l: List<Seg>): List<Seg> {
        val r = mutableListOf<Seg>()
        for (s in l) { val p = r.lastOrNull()
            if (p != null && p.img == null && s.img == null && p.copy(text = "") == s.copy(text = "")) r[r.lastIndex] = p.copy(text = p.text + s.text) else if (s.text.isNotEmpty() || s.img != null) r += s }
        return r
    }
    private fun inline(s: String, st: Seg, out: MutableList<Seg>) {
        val sb = StringBuilder()
        fun flush() { if (sb.isNotEmpty()) { out += st.copy(text = sb.toString()); sb.clear() } }
        var i = 0
        while (i < s.length) {
            val c = s[i]
            when {
                c == '\\' && i + 1 < s.length && s[i + 1] in "\\`*_{}[]()#+-.!|~<>" -> { sb.append(s[i + 1]); i += 2 }
                c == '`' -> {
                    val n = s.drop(i).takeWhile { it == '`' }.length; val close = s.indexOf("`".repeat(n), i + n)
                    if (close > 0) { flush(); out += st.copy(text = s.substring(i + n, close).trim(), code = true); i = close + n } else { sb.append(c); i++ }
                }
                c == '!' && i + 1 < s.length && s[i + 1] == '[' -> {
                    val r = linkAt(s, i + 1)
                    if (r != null) { flush(); out += st.copy(text = r.first, img = r.second.substringBefore(" \"").trim().trim('<', '>')); i = r.third } else { sb.append(c); i++ }
                }
                c == '[' -> {
                    val r = linkAt(s, i)
                    if (r != null) { flush(); val inner = mutableListOf<Seg>(); inline(r.first, st.copy(link = r.second.substringBefore(" \"").trim().trim('<', '>')), inner); out += inner; i = r.third }
                    else if (s.startsWith("[[", i) && s.indexOf("]]", i) > 0) { val e = s.indexOf("]]", i); sb.append(s.substring(i + 2, e).substringAfter('|')); i = e + 2 }   // Obsidian [[wikilink]]
                    else { sb.append(c); i++ }
                }
                c == '<' && Regex("^<(https?://[^>\\s]+)>").find(s.substring(i)) != null -> { val m = Regex("^<(https?://[^>\\s]+)>").find(s.substring(i))!!; flush(); out += st.copy(text = m.groupValues[1], link = m.groupValues[1]); i += m.value.length }
                (c == '*' || c == '_') && s.startsWith("$c$c", i) && i + 2 < s.length && !s[i + 2].isWhitespace() -> {
                    val close = findClose(s, "$c$c", i + 2)
                    if (close > 0) { flush(); inline(s.substring(i + 2, close), st.copy(b = true), out); i = close + 2 } else { sb.append(c); i++ }
                }
                (c == '*' || c == '_') && i + 1 < s.length && !s[i + 1].isWhitespace() && s[i + 1] != c && (c == '*' || i == 0 || !s[i - 1].isLetterOrDigit()) -> {
                    val close = findClose(s, c.toString(), i + 1)
                    if (close > 0 && (c == '*' || close + 1 >= s.length || !s[close + 1].isLetterOrDigit())) { flush(); inline(s.substring(i + 1, close), st.copy(i = true), out); i = close + 1 } else { sb.append(c); i++ }
                }
                c == '~' && s.startsWith("~~", i) -> {
                    val close = s.indexOf("~~", i + 2)
                    if (close > 0) { flush(); inline(s.substring(i + 2, close), st.copy(s = true), out); i = close + 2 } else { sb.append(c); i++ }
                }
                c == '=' && s.startsWith("==", i) && s.indexOf("==", i + 2) > 0 -> { val e = s.indexOf("==", i + 2); sb.append(s.substring(i + 2, e)); i = e + 2 }
                c == 'h' && (s.startsWith("http://", i) || s.startsWith("https://", i)) && (i == 0 || !s[i - 1].isLetterOrDigit()) && st.link == null -> {
                    val m = Regex("^https?://[^\\s<>\"'）)】」，。；、]+").find(s.substring(i))
                    if (m != null) { flush(); out += st.copy(text = m.value, link = m.value); i += m.value.length } else { sb.append(c); i++ }
                }
                c == '\n' -> { sb.append('\n'); i++ }
                else -> { sb.append(c); i++ }
            }
        }
        flush()
    }
    private fun findClose(s: String, mark: String, from: Int): Int {
        var k = from
        while (k < s.length) { val j = s.indexOf(mark, k); if (j < 0) return -1
            if (j > from && !s[j - 1].isWhitespace() && s[j - 1] != '\\') { if (mark.length == 1 && j + 1 < s.length && s[j + 1] == mark[0]) { k = j + 2; continue }; return j }
            k = j + 1 }
        return -1
    }
    /** 返回 (文字, 链接, 结束位置) */
    private fun linkAt(s: String, start: Int): Triple<String, String, Int>? {
        var depth = 0; var k = start
        while (k < s.length) { if (s[k] == '\\') { k += 2; continue }; if (s[k] == '[') depth++; if (s[k] == ']') { depth--; if (depth == 0) break }; k++ }
        if (k >= s.length || k + 1 >= s.length || s[k + 1] != '(') return null
        var d = 1; var e = k + 2
        while (e < s.length && d > 0) { if (s[e] == '(') d++; if (s[e] == ')') d--; e++ }
        if (d != 0) return null
        return Triple(s.substring(start + 1, k), s.substring(k + 2, e - 1), e)
    }
}
