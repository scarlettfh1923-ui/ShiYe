package com.scarlett.shiye

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.security.MessageDigest
import java.util.UUID
import java.util.zip.ZipFile

enum class DocType { MD, HTML }

/** 书库里的一份文档。文件放在 files/library/<id>/ 下；ZIP 包会整体解开放在同一目录。 */
data class Doc(
    val id: String, val title: String, val type: DocType, val main: String, val bundle: Boolean,
    val createdAt: Long, val openedAt: Long, val size: Long, val fav: Boolean, val source: String,
    val snippet: String, val hash: String,
    val notionUrl: String = "", val notionPageId: String = "", val notionTarget: String = "", val notionAt: Long = 0,
    val script: Boolean = false, val desktop: Boolean = false,
    val progress: Float = 0f, val pos: Int = 0,
    val url: String = "",          // 网页抓取的原文链接（去重、回看原文用）
) {
    fun toJson() = JSONObject().put("id", id).put("title", title).put("type", type.name).put("main", main).put("bundle", bundle)
        .put("createdAt", createdAt).put("openedAt", openedAt).put("size", size).put("fav", fav).put("source", source)
        .put("snippet", snippet).put("hash", hash).put("notionUrl", notionUrl).put("notionPageId", notionPageId)
        .put("notionTarget", notionTarget).put("notionAt", notionAt).put("script", script).put("desktop", desktop).put("progress", progress.toDouble()).put("pos", pos).put("url", url)
    companion object {
        fun from(j: JSONObject) = Doc(j.getString("id"), j.optString("title"), runCatching { DocType.valueOf(j.optString("type")) }.getOrDefault(DocType.MD),
            j.optString("main"), j.optBoolean("bundle"), j.optLong("createdAt"), j.optLong("openedAt"), j.optLong("size"), j.optBoolean("fav"),
            j.optString("source"), j.optString("snippet"), j.optString("hash"), j.optString("notionUrl"), j.optString("notionPageId"),
            j.optString("notionTarget"), j.optLong("notionAt"), j.optBoolean("script"), j.optBoolean("desktop"), j.optDouble("progress", 0.0).toFloat(), j.optInt("pos"), j.optString("url"))
    }
}

class ImportException(msg: String) : Exception(msg)

object Library {
    private lateinit var root: File
    private lateinit var index: File
    private val _docs = MutableStateFlow<List<Doc>>(emptyList())
    val docs: StateFlow<List<Doc>> = _docs
    private val lock = Any()
    private var inited = false

    fun init(ctx: Context) {
        synchronized(lock) {
            if (inited) return
            root = File(ctx.filesDir, "library").apply { mkdirs() }
            index = File(ctx.filesDir, "library.json")
            _docs.value = load()
            inited = true
        }
    }
    val rootDir get() = root
    fun dirOf(d: Doc) = File(root, d.id)
    fun mainFile(d: Doc) = File(dirOf(d), d.main)
    fun thumbFile(d: Doc) = File(dirOf(d), ".thumb.png")
    fun get(id: String) = _docs.value.firstOrNull { it.id == id }

    private fun load(): List<Doc> = runCatching {
        if (!index.isFile) return emptyList()
        val a = JSONArray(index.readText())
        (0 until a.length()).mapNotNull { runCatching { Doc.from(a.getJSONObject(it)) }.getOrNull() }.filter { File(root, it.id).isDirectory }
    }.getOrDefault(emptyList())

    private fun persist(list: List<Doc>) {
        val tmp = File(index.parentFile, "library.json.tmp")
        tmp.writeText(JSONArray().apply { list.forEach { put(it.toJson()) } }.toString())
        if (!tmp.renameTo(index)) { index.delete(); tmp.renameTo(index) }
    }

    fun update(d: Doc) = synchronized(lock) {
        val l = _docs.value.map { if (it.id == d.id) d else it }
        persist(l); _docs.value = l
    }
    fun mutate(id: String, f: (Doc) -> Doc) = synchronized(lock) {
        val l = _docs.value.map { if (it.id == id) f(it) else it }
        persist(l); _docs.value = l
    }
    fun setProgress(id: String, p: Float, pos: Int) { val d = get(id) ?: return; if (kotlin.math.abs(d.progress - p) > 0.002f || d.pos != pos) mutate(id) { it.copy(progress = p.coerceIn(0f, 1f), pos = pos) } }
    fun touch(id: String) = mutate(id) { it.copy(openedAt = System.currentTimeMillis()) }
    fun delete(ids: Collection<String>) = synchronized(lock) {
        ids.forEach { File(root, it).deleteRecursively() }
        val l = _docs.value.filterNot { it.id in ids }
        persist(l); _docs.value = l
    }
    fun clearAll() = delete(_docs.value.map { it.id })

    internal fun add(d: Doc) = synchronized(lock) { val l = listOf(d) + _docs.value; persist(l); _docs.value = l }
    fun findByHash(h: String) = _docs.value.firstOrNull { it.hash == h }
    fun findByUrl(u: String) = if (u.isBlank()) null else _docs.value.firstOrNull { it.url == u }
    fun newId() = UUID.randomUUID().toString().replace("-", "").take(16)
    fun readText(d: Doc): String = runCatching { mainFile(d).readText(Charsets.UTF_8) }.getOrDefault("")
    // ---- 编辑：覆盖保存，并保留「上一版」备份（只留一份）----
    private fun ext(d: Doc) = if (d.type == DocType.HTML) "html" else "md"
    fun backupFile(d: Doc) = File(dirOf(d), ".backup/prev.${ext(d)}")
    fun backupAt(d: Doc): Long = backupFile(d).let { if (it.isFile) it.lastModified() else 0L }
    const val EDIT_LIMIT = 600 * 1024

    /** 保存编辑：先把现有内容存成上一版，再原子写入新内容。返回更新后的文档。 */
    fun saveEdited(d: Doc, text: String): Doc = synchronized(lock) {
        val cur = mainFile(d)
        val old = runCatching { cur.readText(Charsets.UTF_8) }.getOrDefault("")
        if (old == text) return get(d.id) ?: d
        val bk = backupFile(d); bk.parentFile?.mkdirs()
        if (cur.isFile) cur.copyTo(bk, overwrite = true)
        bk.setLastModified(System.currentTimeMillis())
        val tmp = File(cur.parentFile, cur.name + ".tmp"); tmp.writeText(text, Charsets.UTF_8)
        if (!tmp.renameTo(cur)) { cur.delete(); tmp.renameTo(cur) }
        thumbFile(d).delete()
        val nd = (get(d.id) ?: d).let { it.copy(size = text.toByteArray().size.toLong(), snippet = Importer.snippetOf(text, it.type), hash = Importer.sha1(text.toByteArray()), openedAt = System.currentTimeMillis()) }
        update(nd); nd
    }

    /** 恢复上一版：与当前内容互换，所以恢复本身也能反悔。返回恢复后的文本。 */
    fun restoreBackup(d: Doc): String? = synchronized(lock) {
        val bk = backupFile(d); if (!bk.isFile) return null
        val prev = bk.readText(Charsets.UTF_8)
        saveEdited(d, prev)       // 当前内容 → 备份；上一版 → 正文
        prev
    }

    fun writeText(d: Doc, text: String) {
        mainFile(d).writeText(text, Charsets.UTF_8)
        update(d.copy(size = text.toByteArray().size.toLong(), snippet = Importer.snippetOf(text, d.type), hash = Importer.sha1(text.toByteArray())))
    }
}

object Importer {
    const val MAX_FILE = 30L * 1024 * 1024
    const val MAX_ZIP_TOTAL = 120L * 1024 * 1024
    private val mdExt = setOf("md", "markdown", "mdown", "mkd", "txt", "text")
    private val htmlExt = setOf("html", "htm", "xhtml")

    fun sha1(b: ByteArray): String = MessageDigest.getInstance("SHA-1").digest(b).joinToString("") { "%02x".format(it) }.take(20)

    fun snippetOf(text: String, type: DocType): String {
        val plain = if (type == DocType.HTML) HtmlText.toMarkdown(text) else Markdown.stripFrontMatter(text).first
        return plain.lineSequence().map { it.trim() }.filter { it.isNotEmpty() && !it.startsWith("```") && !it.startsWith("---") && !it.startsWith("# ") && !it.contains("](shiye-img://") && !(it.startsWith(">") && it.contains("[查看原文](")) && !it.startsWith("> ⚠") }
            .map { it.trimStart('#', '>', '-', '*', ' ').replace(Regex("!\\[[^]]*]\\([^)]*\\)"), "").replace(Regex("\\[([^]]*)]\\([^)]*\\)"), "$1").replace(Regex("[*_`~]"), "") }
            .filter { it.isNotBlank() }.take(8).joinToString("\n").take(360)
    }

    fun safeName(s: String): String = s.replace(Regex("[\\\\/:*?\"<>|\\p{Cntrl}]"), "_").trim().trim('.').take(80)
    private fun extOf(name: String) = name.substringAfterLast('.', "").lowercase()
    private fun stem(name: String) = if (name.contains('.')) name.substringBeforeLast('.') else name

    private fun displayName(ctx: Context, uri: Uri): String {
        runCatching {
            ctx.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c -> if (c.moveToFirst()) c.getString(0)?.let { return it } }
        }
        return uri.lastPathSegment?.substringAfterLast('/') ?: "未命名"
    }

    /** 只接受 content:// ——file:// 和本应用自己的 provider 一律拒绝，避免被人诱导读取私有目录。 */
    fun fromUri(ctx: Context, uri: Uri, source: String): Doc {
        if (uri.scheme != "content") throw ImportException("不支持这种来源的文件")
        if (uri.authority == "${ctx.packageName}.files") throw ImportException("不能导入拾页自己的缓存文件")
        val name = displayName(ctx, uri)
        val tmp = File(ctx.cacheDir, "imp_${System.nanoTime()}.tmp")
        try {
            val input = ctx.contentResolver.openInputStream(uri) ?: throw ImportException("读不到这个文件")
            input.use { copyLimited(it, tmp) }
            return fromFile(ctx, tmp, name, source)
        } finally { tmp.delete() }
    }

    private fun copyLimited(i: InputStream, out: File) {
        var total = 0L
        FileOutputStream(out).use { o ->
            val buf = ByteArray(64 * 1024)
            while (true) { val n = i.read(buf); if (n < 0) break; total += n; if (total > MAX_ZIP_TOTAL) throw ImportException("文件太大了（上限 120 MB）"); o.write(buf, 0, n) }
        }
    }

    private fun isZip(f: File): Boolean = f.inputStream().use { val b = ByteArray(4); it.read(b) == 4 && b[0] == 0x50.toByte() && b[1] == 0x4B.toByte() && b[2] == 3.toByte() && b[3] == 4.toByte() }

    private fun looksHtml(s: String): Boolean {
        val h = s.take(2000).trimStart('\uFEFF', ' ', '\n', '\r', '\t').lowercase()
        return h.startsWith("<!doctype html") || h.startsWith("<html") || (h.startsWith("<") && Regex("<(head|body|div|section|style|meta|title)[\\s>]").containsMatchIn(h))
    }

    private fun decode(f: File): String {
        val b = f.readBytes()
        var s = String(b, Charsets.UTF_8)
        if (s.contains('\uFFFD')) {   // 不是 UTF-8：试 GBK（常见于国内 Windows 导出的文本）
            val g = runCatching { String(b, charset("GBK")) }.getOrNull()
            if (g != null && g.count { it == '\uFFFD' } < s.count { it == '\uFFFD' }) s = g
        }
        return s.removePrefix("\uFEFF")
    }

    fun fromFile(ctx: Context, f: File, name: String, source: String): Doc {
        if (f.length() == 0L) throw ImportException("这是个空文件")
        val ext = extOf(name)
        val zip = ext == "zip" || (ext !in mdExt && ext !in htmlExt && isZip(f))
        if (zip) return fromZip(f, name, source)
        if (f.length() > MAX_FILE) throw ImportException("文件太大了（单个文本上限 30 MB）")
        val text = decode(f)
        if (text.take(4096).count { it == '\u0000' } > 0) throw ImportException("这看起来不是文本文件，拾页只能打开 .md / .html / .txt 和网页压缩包")
        val type = when { ext in htmlExt -> DocType.HTML; ext in mdExt -> if (ext == "txt" && looksHtml(text)) DocType.HTML else DocType.MD
            else -> if (looksHtml(text)) DocType.HTML else if (ext.isEmpty() || ext in setOf("log", "json", "csv", "yml", "yaml", "")) DocType.MD else throw ImportException("暂不支持 .$ext 文件。拾页能打开 .md / .html / .txt 和网页压缩包") }
        return saveText(text, type, titleFor(text, type, stem(name)), source)
    }

    /** 分享进来的一段文字（不是文件） */
    fun fromText(text: String, hintTitle: String?, source: String, htmlHint: Boolean = false): Doc {
        val t = text.trim()
        if (t.isEmpty()) throw ImportException("没有收到内容")
        val type = if (htmlHint || looksHtml(t)) DocType.HTML else DocType.MD
        val fallback = (hintTitle?.takeIf { it.isNotBlank() } ?: t.lineSequence().map { it.trim().trimStart('#', ' ') }.firstOrNull { it.isNotEmpty() } ?: "分享的内容").take(40)
        return saveText(t, type, titleFor(t, type, fallback), source)
    }

    private fun titleFor(text: String, type: DocType, fallback: String): String {
        val t = if (type == DocType.HTML) Regex("<title[^>]*>([\\s\\S]*?)</title>", RegexOption.IGNORE_CASE).find(text)?.groupValues?.get(1)?.let { HtmlText.unescape(it).trim() }
        else Regex("^\\s*#\\s+(.+)$", RegexOption.MULTILINE).find(text.take(4000))?.groupValues?.get(1)?.replace(Regex("[*_`]"), "")?.trim()
        return (t?.takeIf { it.isNotBlank() } ?: fallback).take(80)
    }

    private fun saveText(text: String, type: DocType, title: String, source: String): Doc {
        val bytes = text.toByteArray(Charsets.UTF_8)
        val h = sha1(bytes)
        Library.findByHash(h)?.let { Library.touch(it.id); return Library.get(it.id)!! }
        val id = Library.newId(); val dir = File(Library.rootDir, id).apply { mkdirs() }
        val main = if (type == DocType.HTML) "index.html" else "doc.md"
        File(dir, main).writeBytes(bytes)
        val now = System.currentTimeMillis()
        val d = Doc(id, title, type, main, false, now, now, bytes.size.toLong(), false, source, snippetOf(text, type), h)
        Library.add(d); return d
    }

    /** 网页抓取：目录（含 images/）已由 ClipFetcher 写好，这里写正文并登记进书库 */
    fun saveClip(id: String, dir: File, md: String, title: String, source: String, url: String, hasImages: Boolean): Doc {
        dir.mkdirs()
        val bytes = md.toByteArray(Charsets.UTF_8)
        File(dir, "doc.md").writeBytes(bytes)
        val now = System.currentTimeMillis()
        val d = Doc(id, title.take(80), DocType.MD, "doc.md", hasImages, now, now, bytes.size.toLong(), false, source, snippetOf(md, DocType.MD), sha1(bytes), url = url)
        Library.add(d); return d
    }

    private fun fromZip(f: File, name: String, source: String): Doc {
        val h = f.inputStream().use { i -> val md = MessageDigest.getInstance("SHA-1"); val b = ByteArray(65536); while (true) { val n = i.read(b); if (n < 0) break; md.update(b, 0, n) }; md.digest().joinToString("") { "%02x".format(it) }.take(20) }
        Library.findByHash(h)?.let { Library.touch(it.id); return Library.get(it.id)!! }
        val id = Library.newId(); val dir = File(Library.rootDir, id).apply { mkdirs() }
        try {
            val zf = openZip(f)
            var total = 0L; var count = 0
            val canon = dir.canonicalPath + File.separator
            zf.use { z ->
                for (e in z.entries()) {
                    if (e.isDirectory) continue
                    val n = e.name.replace('\\', '/')
                    if (n.startsWith("__MACOSX/") || n.substringAfterLast('/').startsWith("._") || n.endsWith(".DS_Store")) continue
                    if (++count > 3000) throw ImportException("压缩包里文件太多了")
                    val out = File(dir, n)
                    if (!out.canonicalPath.startsWith(canon)) continue            // 防 zip-slip
                    out.parentFile?.mkdirs()
                    z.getInputStream(e).use { i -> FileOutputStream(out).use { o ->
                        val buf = ByteArray(64 * 1024)
                        while (true) { val r = i.read(buf); if (r < 0) break; total += r; if (total > MAX_ZIP_TOTAL) throw ImportException("压缩包解开后太大了（上限 120 MB）"); o.write(buf, 0, r) }
                    } }
                }
            }
            val files = dir.walkTopDown().filter { it.isFile }.map { it.relativeTo(dir).path.replace(File.separatorChar, '/') }.toList()
            val htmls = files.filter { extOf(it) in htmlExt }
            val mds = files.filter { extOf(it) in setOf("md", "markdown", "mdown") }
            fun depth(s: String) = s.count { it == '/' }
            val mainHtml = htmls.sortedWith(compareBy({ !it.substringAfterLast('/').lowercase().startsWith("index") }, { depth(it) }, { it.length })).firstOrNull()
            val mainMd = mds.sortedWith(compareBy({ depth(it) }, { it.length })).firstOrNull()
            val (main, type) = when { mainHtml != null -> mainHtml to DocType.HTML; mainMd != null -> mainMd to DocType.MD
                else -> throw ImportException("压缩包里没有找到 .html 或 .md 文件") }
            val text = runCatching { decode(File(dir, main)) }.getOrDefault("")
            val now = System.currentTimeMillis()
            val d = Doc(id, titleFor(text, type, stem(name)), type, main, true, now, now, f.length(), false, source, snippetOf(text, type), h)
            Library.add(d); return d
        } catch (e: Exception) { dir.deleteRecursively(); throw if (e is ImportException) e else ImportException("压缩包打不开：${e.message ?: "格式不对"}") }
    }

    private fun openZip(f: File): ZipFile = try { ZipFile(f, Charsets.UTF_8).also { z -> z.entries().asSequence().forEach { } } } catch (_: Exception) { ZipFile(f, charset("GBK")) }
}

/** HTML → 近似 Markdown（用于摘要、Notion 同步、导出长图）。不执行任何脚本，只做文本抽取。 */
object HtmlText {
    fun unescape(s: String): String = android.text.Html.fromHtml(s, android.text.Html.FROM_HTML_MODE_LEGACY).toString()
    fun toMarkdown(html: String): String {
        var s = html.replace(Regex("<(script|style|noscript|svg|head)[\\s\\S]*?</\\1>", RegexOption.IGNORE_CASE), "")
        s = s.replace(Regex("<!--[\\s\\S]*?-->"), "")
        s = s.replace(Regex("<img[^>]*?src=[\"'](https?://[^\"']+)[\"'][^>]*?>", RegexOption.IGNORE_CASE)) { m ->
            val alt = Regex("alt=[\"']([^\"']*)[\"']", RegexOption.IGNORE_CASE).find(m.value)?.groupValues?.get(1).orEmpty()
            "\n\n![${alt}](${m.groupValues[1]})\n\n" }
        for (n in 1..6) s = s.replace(Regex("<h$n[^>]*>([\\s\\S]*?)</h$n>", RegexOption.IGNORE_CASE)) { "\n\n" + "#".repeat(n) + " " + it.groupValues[1].replace(Regex("<[^>]+>"), "").trim() + "\n\n" }
        s = s.replace(Regex("<li[^>]*>", RegexOption.IGNORE_CASE), "\n- ")
        s = s.replace(Regex("<(br|/p|/div|/section|/article|/tr|/ul|/ol|/table|/blockquote)[^>]*>", RegexOption.IGNORE_CASE), "\n\n")
        s = s.replace(Regex("<(p|div|section|article|tr|blockquote)[^>]*>", RegexOption.IGNORE_CASE), "\n")
        s = s.replace(Regex("</t[dh]>", RegexOption.IGNORE_CASE), "  ")
        s = s.replace(Regex("<[^>]+>"), "")
        s = unescape(s).replace("\u00A0", " ")
        return s.lines().joinToString("\n") { it.trimEnd() }.replace(Regex("\n{3,}"), "\n\n").trim()
    }
}
