package com.scarlett.shiye

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

class NotionException(msg: String) : Exception(msg)

data class NotionResult(val url: String, val pageId: String, val note: String)

/** Notion 同步逻辑（API 版本 2025-09-03）：Markdown → Notion 块，页面 / 数据库两种目标，本地图片走 file_uploads。 */
object Notion {
    const val VERSION = "2025-09-03"
    private const val BASE = "https://api.notion.com"

    // ---- 网络（单测可替换）----
    var transport: (method: String, url: String, headers: Map<String, String>, body: ByteArray?) -> Pair<Int, String> = ::realHttp

    private fun realHttp(method: String, url: String, headers: Map<String, String>, body: ByteArray?): Pair<Int, String> {
        val c = URL(url).openConnection() as HttpURLConnection
        c.connectTimeout = 12000; c.readTimeout = 40000; c.requestMethod = method
        headers.forEach { (k, v) -> c.setRequestProperty(k, v) }
        if (body != null) { c.doOutput = true; c.outputStream.use { it.write(body) } }
        val code = c.responseCode
        val text = (if (code in 200..299) c.inputStream else c.errorStream)?.use { String(it.readBytes()) } ?: ""
        return code to text
    }

    private fun call(token: String, method: String, path: String, body: JSONObject? = null): JSONObject {
        val h = mapOf("Authorization" to "Bearer $token", "Notion-Version" to VERSION, "Content-Type" to "application/json")
        var attempt = 0
        while (true) {
            val (code, text) = transport(method, BASE + path, h, body?.toString()?.toByteArray())
            if (code == 429 && attempt < 3) { attempt++; Thread.sleep(1500L * attempt); continue }
            if (code in 200..299) return runCatching { JSONObject(text) }.getOrDefault(JSONObject())
            throw NotionException(explain(code, text))
        }
    }

    fun explain(code: Int, text: String): String {
        val j = runCatching { JSONObject(text) }.getOrNull()
        val msg = j?.optString("message").orEmpty()
        return when {
            code == 401 -> "Token 无效或已被撤销，请到 notion.so/my-integrations 重新复制"
            code == 403 -> "这个集成没有权限做这件事，请检查集成的「内容功能」是否勾选了读取、更新、插入"
            code == 404 -> "找不到目标，或这个集成还没被添加到它里面。请在目标页面 / 数据库右上角「…」→「连接」里选中你的集成"
            code == 429 -> "Notion 限流了，稍等一会儿再试"
            code >= 500 -> "Notion 服务暂时不可用（$code），稍后再试"
            else -> "Notion 拒绝了请求（$code）：${msg.take(160)}"
        }
    }

    /** 从链接或纯 ID 里取出 32 位十六进制 ID */
    fun parseId(input: String): String? {
        val s = input.trim().substringBefore('#')
        val path = s.substringBefore('?')
        Regex("([0-9a-fA-F]{32})(?![0-9a-fA-F])").findAll(path.replace("-", "")).lastOrNull()?.let { return dash(it.groupValues[1]) }
        Regex("[?&]p=([0-9a-fA-F]{32})").find(s.replace("-", ""))?.let { return dash(it.groupValues[1]) }
        return null
    }
    private fun dash(h: String) = h.lowercase().let { "${it.substring(0, 8)}-${it.substring(8, 12)}-${it.substring(12, 16)}-${it.substring(16, 20)}-${it.substring(20)}" }

    // ---- 连接测试 ----
    suspend fun test(token: String, target: String, type: String): String = withContext(Dispatchers.IO) {
        if (token.isBlank()) throw NotionException("还没填 Token")
        val id = parseId(target) ?: throw NotionException("链接里没找到页面 ID，请粘贴 Notion 页面 / 数据库的完整链接")
        val me = call(token, "GET", "/v1/users/me")
        val bot = me.optString("name", "集成")
        if (type == "database") {
            val ds = resolveDataSource(token, id)
            "已连接「$bot」，数据库「${ds.name}」可用（标题列：${ds.titleProp}）"
        } else {
            val p = call(token, "GET", "/v1/pages/$id")
            if (p.optBoolean("in_trash") || p.optBoolean("archived")) throw NotionException("这个页面在废纸篓里")
            "已连接「$bot」，目标页面可用"
        }
    }

    class DataSource(val id: String, val name: String, val titleProp: String, val props: Map<String, String>)

    private fun resolveDataSource(token: String, id: String): DataSource {
        var dsId = id; var name = "数据库"
        val db = runCatching { call(token, "GET", "/v1/databases/$id") }.getOrNull()
        if (db != null) {
            name = db.optJSONArray("title")?.optJSONObject(0)?.optString("plain_text").orEmpty().ifBlank { "数据库" }
            db.optJSONArray("data_sources")?.optJSONObject(0)?.optString("id")?.takeIf { it.isNotBlank() }?.let { dsId = it }
            if (db.has("properties") && !db.has("data_sources")) return ds(dsId, name, db.getJSONObject("properties"))
        }
        val d = call(token, "GET", "/v1/data_sources/$dsId")
        if (name == "数据库") name = d.optJSONArray("title")?.optJSONObject(0)?.optString("plain_text").orEmpty().ifBlank { "数据库" }
        return ds(dsId, name, d.getJSONObject("properties"))
    }
    private fun ds(id: String, name: String, props: JSONObject): DataSource {
        val m = props.keys().asSequence().associateWith { props.getJSONObject(it).optString("type") }
        val title = m.entries.firstOrNull { it.value == "title" }?.key ?: "Name"
        return DataSource(id, name, title, m)
    }

    // ---- 发布 ----
    suspend fun publish(cfg: Prefs, doc: Doc, mdText: String, baseDir: File?): NotionResult = withContext(Dispatchers.IO) {
        publishWith(cfg.notionToken, cfg.notionTarget, cfg.notionType, cfg.notionTitleProp, cfg.notionTagsProp, doc, mdText, baseDir)
    }

    fun publishWith(token: String, target: String, type: String, titleProp: String, tagsProp: String, doc: Doc, mdText: String, baseDir: File?): NotionResult {
        if (token.isBlank() || target.isBlank()) throw NotionException("请先在「设置 → Notion 同步」里填好 Token 和目标链接")
        val id = parseId(target) ?: throw NotionException("目标链接里没找到页面 ID")
        val (body, meta) = Markdown.stripFrontMatter(mdText.replace("\r\n", "\n"))
        val blocks = Markdown.parse(mdText)
        val notes = mutableListOf<String>()
        val conv = Conv(token, baseDir, notes)
        val all = conv.blocks(blocks, 0)
        if (all.isEmpty()) throw NotionException("文档是空的，没有可同步的内容")

        val props = JSONObject(); val parent = JSONObject()
        if (type == "database") {
            val d = resolveDataSource(token, id)
            parent.put("type", "data_source_id").put("data_source_id", d.id)
            val tp = titleProp.ifBlank { d.titleProp }
            props.put(tp, titleProp(doc.title))
            val tags = Markdown.tags(meta)
            val tg = tagsProp.ifBlank { "" }
            if (tg.isNotBlank() && tags.isNotEmpty()) {
                if (d.props[tg] == "multi_select") props.put(tg, JSONObject().put("multi_select", JSONArray(tags.map { JSONObject().put("name", it.take(90).replace(",", " ")) })))
                else notes += "数据库里没有名为「$tg」的多选列，标签没写入"
            }
        } else {
            parent.put("type", "page_id").put("page_id", id)
            props.put("title", titleProp(doc.title))
        }
        val first = all.take(100)
        val req = JSONObject().put("parent", parent).put("properties", props).put("children", JSONArray(first))
        conv.firstImage?.let { req.put("cover", JSONObject().put("type", "external").put("external", JSONObject().put("url", it))) }
        val created = try { call(token, "POST", "/v1/pages", req) } catch (e: NotionException) {
            if (req.has("cover")) { req.remove("cover"); notes += "封面图没设上"; call(token, "POST", "/v1/pages", req) } else throw e
        }
        val pageId = created.getString("id"); val url = created.optString("url")
        var rest = all.drop(100)
        while (rest.isNotEmpty()) {
            call(token, "PATCH", "/v1/blocks/$pageId/children", JSONObject().put("children", JSONArray(rest.take(100))))
            rest = rest.drop(100)
        }
        // 重新同步：新页面成功后，再把上次同步出的旧页面移入废纸篓
        if (doc.notionPageId.isNotBlank() && doc.notionPageId != pageId && doc.notionTarget == target) {
            runCatching { call(token, "PATCH", "/v1/pages/${doc.notionPageId}", JSONObject().put("in_trash", true)) }
                .recoverCatching { call(token, "PATCH", "/v1/pages/${doc.notionPageId}", JSONObject().put("archived", true)) }
                .onFailure { notes += "上一次同步的旧页面没能自动移除，可以手动删掉" }
        }
        return NotionResult(url, pageId, notes.joinToString("；"))
    }

    private fun titleProp(t: String) = JSONObject().put("title", JSONArray().put(JSONObject().put("type", "text").put("text", JSONObject().put("content", t.take(1900)))))

    // ---- Markdown → Notion 块 ----
    private val langMap = setOf("abap", "bash", "c", "c++", "c#", "css", "dart", "diff", "docker", "elixir", "go", "graphql", "groovy", "html", "java", "javascript", "json", "kotlin",
        "latex", "lua", "makefile", "markdown", "matlab", "nix", "objective-c", "perl", "php", "powershell", "protobuf", "python", "r", "ruby", "rust", "scala", "scss", "shell", "sql", "swift", "toml", "typescript", "xml", "yaml", "plain text")
    fun mapLang(l: String): String { val x = l.lowercase().trim(); return when (x) { "js", "jsx", "node" -> "javascript"; "ts", "tsx" -> "typescript"; "py", "python3" -> "python"; "sh", "zsh" -> "shell"; "yml" -> "yaml"
        "md" -> "markdown"; "cpp" -> "c++"; "cs", "csharp" -> "c#"; "kt" -> "kotlin"; "rs" -> "rust"; "golang" -> "go"; "dockerfile" -> "docker"; "objc" -> "objective-c"; "ps1" -> "powershell"; "" , "text", "txt" -> "plain text"
        else -> if (x in langMap) x else "plain text" } }

    fun richText(segs: List<Seg>): JSONArray {
        val arr = JSONArray()
        for (s in segs) {
            if (s.img != null || s.text.isEmpty()) continue
            val ann = JSONObject()
            if (s.b) ann.put("bold", true); if (s.i) ann.put("italic", true); if (s.s) ann.put("strikethrough", true); if (s.code) ann.put("code", true)
            val link = s.link?.takeIf { it.startsWith("https://") || it.startsWith("http://") || it.startsWith("mailto:") }?.take(1900)
            s.text.chunked(1900).forEach { chunk ->
                val t = JSONObject().put("content", chunk); if (link != null) t.put("link", JSONObject().put("url", link))
                arr.put(JSONObject().put("type", "text").put("text", t).apply { if (ann.length() > 0) put("annotations", ann) })
            }
        }
        return JSONArray((0 until minOf(arr.length(), 100)).map { arr.get(it) })
    }
    private fun plainRt(s: String): JSONArray = richText(listOf(Seg(s)))

    private fun blk(type: String, inner: JSONObject) = JSONObject().put("object", "block").put("type", type).put(type, inner)

    class Conv(val token: String, val baseDir: File?, val notes: MutableList<String>) {
        var firstImage: String? = null
        private var uploaded = 0; private var skipped = 0

        fun blocks(l: List<MdBlock>, depth: Int): List<JSONObject> = l.flatMap { one(it, depth) }.also { if (depth == 0 && skipped > 0) notes += "$skipped 张本地图片没能上传（太大、超过 8 张或格式不支持）" }

        private fun one(b: MdBlock, depth: Int): List<JSONObject> = when (b) {
            is MdBlock.Heading -> listOf(blk("heading_${b.level.coerceAtMost(3)}", JSONObject().put("rich_text", richText(b.segs))))
            is MdBlock.Para -> listOf(blk("paragraph", JSONObject().put("rich_text", richText(b.segs))))
            is MdBlock.Code -> listOf(blk("code", JSONObject().put("rich_text", plainRt(b.text.ifEmpty { " " })).put("language", mapLang(b.lang))))
            is MdBlock.Hr -> listOf(blk("divider", JSONObject()))
            is MdBlock.Image -> image(b)
            is MdBlock.Quote -> {
                val (head, tail) = splitHead(b.blocks)
                listOf(blk("quote", JSONObject().put("rich_text", head).apply { kids(tail, depth)?.let { put("children", it) } }))
            }
            is MdBlock.Callout -> {
                val (head, tail) = splitHead(b.blocks)
                val title = if (b.title.isNotBlank()) richText(listOf(Seg(b.title, b = true))) else null
                val rt = if (title != null && head.length() == 0) title else if (title != null) JSONArray().apply { for (i in 0 until title.length()) put(title.get(i)); put(JSONObject().put("type", "text").put("text", JSONObject().put("content", "\n"))); for (i in 0 until head.length()) put(head.get(i)) } else head
                listOf(blk("callout", JSONObject().put("rich_text", if (rt.length() == 0) plainRt(" ") else rt).put("icon", JSONObject().put("type", "emoji").put("emoji", emoji(b.kind))).apply { kids(tail, depth)?.let { put("children", it) } }))
            }
            is MdBlock.Lst -> b.items.map { it ->
                val (head, tail) = splitHead(it.blocks)
                val type = if (it.checked != null) "to_do" else if (b.ordered) "numbered_list_item" else "bulleted_list_item"
                blk(type, JSONObject().put("rich_text", head).apply { if (it.checked != null) put("checked", it.checked); kids(tail, depth)?.let { k -> put("children", k) } })
            }
            is MdBlock.Table -> {
                val w = b.header.size.coerceIn(1, 100)
                val rows = (listOf(b.header) + b.rows.take(98)).map { r -> blk("table_row", JSONObject().put("cells", JSONArray(r.take(w).map { richText(it) }))) }
                if (b.rows.size > 98) notes += "表格超过 99 行，后面的行没同步"
                listOf(blk("table", JSONObject().put("table_width", w).put("has_column_header", true).put("has_row_header", false).put("children", JSONArray(rows))))
            }
        }
        /** Notion 单次请求最多嵌套两层：超过的子内容摊平到当前层 */
        private fun kids(tail: List<MdBlock>, depth: Int): JSONArray? {
            if (tail.isEmpty()) return null
            val sub = if (depth >= 1) tail.flatMap { flat(it) } else blocks(tail, depth + 1)
            return if (sub.isEmpty()) null else JSONArray(sub.take(100))
        }
        private fun flat(b: MdBlock): List<JSONObject> = when (b) {
            is MdBlock.Lst -> b.items.flatMap { listOf(blk("paragraph", JSONObject().put("rich_text", splitHead(it.blocks).first))) + it.blocks.drop(1).flatMap { x -> flat(x) } }
            is MdBlock.Quote -> b.blocks.flatMap { flat(it) }
            is MdBlock.Callout -> b.blocks.flatMap { flat(it) }
            is MdBlock.Table -> listOf(blk("paragraph", plainRt(b.rows.joinToString("\n") { r -> r.joinToString(" | ") { it.plain() } }).let { JSONObject().put("rich_text", it) }))
            else -> one(b, 1)
        }
        private fun splitHead(bl: List<MdBlock>): Pair<JSONArray, List<MdBlock>> {
            val f = bl.firstOrNull()
            return if (f is MdBlock.Para) richText(f.segs) to bl.drop(1) else JSONArray() to bl
        }
        private fun emoji(k: String) = when (k) { "tip", "hint", "success", "done" -> "💡"; "warning", "caution", "attention" -> "⚠️"; "danger", "error", "bug", "failure" -> "🚫"; "question", "faq", "help" -> "❓"; "quote", "cite" -> "💬"; "example" -> "🧪"; else -> "📝" }

        private fun image(b: MdBlock.Image): List<JSONObject> {
            val u = b.url
            val cap = if (b.alt.isNotBlank()) plainRt(b.alt) else JSONArray()
            if (u.startsWith("https://") && u.length < 1900) {
                if (firstImage == null) firstImage = u
                return listOf(blk("image", JSONObject().put("type", "external").put("external", JSONObject().put("url", u)).put("caption", cap)))
            }
            if (u.startsWith("http://") || u.startsWith("data:") || baseDir == null) { skipped++; return emptyList() }
            val f = runCatching { File(baseDir, java.net.URLDecoder.decode(u.replace("+", "%2B"), "UTF-8").substringBefore('#')).canonicalFile }.getOrNull()
            if (f == null || !f.path.startsWith(baseDir.canonicalPath + File.separator) || !f.isFile || f.length() > 5L * 1024 * 1024 || uploaded >= 8) { skipped++; return emptyList() }
            val mime = when (f.extension.lowercase()) { "png" -> "image/png"; "jpg", "jpeg" -> "image/jpeg"; "gif" -> "image/gif"; "webp" -> "image/webp"; else -> { skipped++; return emptyList() } }
            val id = runCatching { upload(token, f.name, mime, f.readBytes()) }.getOrNull() ?: run { skipped++; return emptyList() }
            uploaded++
            return listOf(blk("image", JSONObject().put("type", "file_upload").put("file_upload", JSONObject().put("id", id)).put("caption", cap)))
        }
    }

    /** POST /v1/file_uploads → POST /v1/file_uploads/{id}/send（multipart）。返回 file_upload id */
    fun upload(token: String, filename: String, mime: String, bytes: ByteArray): String {
        val created = call(token, "POST", "/v1/file_uploads", JSONObject().put("mode", "single_part").put("filename", filename).put("content_type", mime))
        val id = created.getString("id")
        val boundary = "----shiye${System.nanoTime()}"
        val head = "--$boundary\r\nContent-Disposition: form-data; name=\"file\"; filename=\"${filename.replace("\"", "_")}\"\r\nContent-Type: $mime\r\n\r\n".toByteArray()
        val tail = "\r\n--$boundary--\r\n".toByteArray()
        val (code, text) = transport("POST", "$BASE/v1/file_uploads/$id/send", mapOf("Authorization" to "Bearer $token", "Notion-Version" to VERSION, "Content-Type" to "multipart/form-data; boundary=$boundary"), head + bytes + tail)
        if (code !in 200..299) throw NotionException(explain(code, text))
        return id
    }
}
