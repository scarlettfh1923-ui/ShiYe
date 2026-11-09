package com.scarlett.shiye

import org.json.JSONObject
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.nodes.Node
import org.jsoup.nodes.TextNode
import java.net.URI
import java.net.URLDecoder
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.IdentityHashMap

/**
 * 网页抓取的纯逻辑部分（不依赖 Android，方便 JVM 单测）：
 * 识别链接 → 判断站点 → 从 HTML / 接口 JSON 里取出标题、作者、时间和正文 → 转成 Markdown。
 * 联网、WebView 兜底、图片落盘在 ClipFetcher 里。
 */
enum class ClipSite(val label: String) { WECHAT("微信"), ZHIHU("知乎"), DOUBAN("豆瓣"), WEB("网页") }

/** kind：article 文章 / answer 回答 / note 日记 / review 评论 / topic 小组帖 / page 其他页面 */
data class ClipTarget(val url: String, val site: ClipSite, val kind: String, val id: String = "") {
    /** 豆瓣移动版接口（不需要登录，返回 JSON）；其他站点为 null */
    val doubanApi: String? get() = if (site != ClipSite.DOUBAN || id.isEmpty()) null else when (kind) {
        "note" -> "https://m.douban.com/rexxar/api/v2/note/$id"
        "review" -> "https://m.douban.com/rexxar/api/v2/review/$id"
        "topic" -> "https://m.douban.com/rexxar/api/v2/group/topic/$id"
        else -> null
    }
    /** 兜底时让 WebView 打开的页面：豆瓣用移动版（桌面版常被拦到登录页） */
    val pageUrl: String get() = if (site == ClipSite.DOUBAN && id.isNotEmpty()) when (kind) {
        "note" -> "https://m.douban.com/note/$id/"
        "review" -> "https://m.douban.com/" + (Regex("^https://(movie|book|music)\\.").find(url)?.groupValues?.get(1) ?: "movie") + "/review/$id/"
        "topic" -> "https://m.douban.com/group/topic/$id/"
        else -> url
    } else url
    /** 下载图片时带的 Referer：公众号、豆瓣、知乎的图床都校验来源 */
    val referer: String get() = when (site) {
        ClipSite.WECHAT -> "https://mp.weixin.qq.com/"
        ClipSite.DOUBAN -> "https://m.douban.com/"
        ClipSite.ZHIHU -> "https://www.zhihu.com/"
        ClipSite.WEB -> url
    }
}

object ClipUrl {
    private val urlRe = Regex("https?://[^\\s<>\"'，。！？；：“”‘’（）【】《》\\u3000]+", RegexOption.IGNORE_CASE)
    private const val MAX = 2048

    /** 文字里恰好有一个链接、其余文字不多（分享卡片常带「【标题】」）时，返回规范化后的链接；否则 null。 */
    fun find(text: String?): String? {
        if (text.isNullOrBlank() || text.length > 3000) return null
        val all = urlRe.findAll(text).map { it.value.trimEnd('.', ',', ')', ']', '}', '>', '…', '!', '?') }.distinct().toList()
        if (all.size != 1) return null
        val rest = urlRe.replace(text, "").trim()
        if (rest.length > 240) return null       // 一大段文字里夹了个链接：按文字保存，不当作「分享链接」
        return normalize(all[0])
    }

    /** 只接受 http(s)，明文 http 升级为 https（应用禁止明文流量）；拒绝本机地址；非 ASCII 字符百分号编码。 */
    fun normalize(raw: String): String? {
        var s = raw.trim()
        if (s.length > MAX) return null
        if (s.startsWith("http://", true)) s = "https://" + s.substring(7)
        if (!s.startsWith("https://", true)) return null
        s = buildString { for (c in s) { if (c.code > 0x7e || c == ' ') append(java.net.URLEncoder.encode(c.toString(), "UTF-8").replace("+", "%20")) else append(c) } }
        val u = runCatching { URI(s) }.getOrNull() ?: return null
        val host = u.host?.lowercase() ?: return null
        if (host.isEmpty() || host == "localhost" || host.endsWith(".localhost") || host == "0.0.0.0" || host.startsWith("127.") || host == "[::1]" || host == "::1") return null
        return s
    }

    fun classify(url: String): ClipTarget {
        val u = runCatching { URI(url) }.getOrNull() ?: return ClipTarget(url, ClipSite.WEB, "page")
        val host = u.host?.lowercase().orEmpty(); val path = u.rawPath.orEmpty()
        fun clean(p: String) = "https://$host$p"
        return when {
            host == "mp.weixin.qq.com" -> ClipTarget(url, ClipSite.WECHAT, "article")
            host == "zhuanlan.zhihu.com" -> Regex("^/p/(\\d+)").find(path)?.let { ClipTarget(clean("/p/${it.groupValues[1]}"), ClipSite.ZHIHU, "article", it.groupValues[1]) }
                ?: ClipTarget(url, ClipSite.ZHIHU, "page")
            host == "zhihu.com" || host.endsWith(".zhihu.com") -> Regex("^(/question/\\d+)?/answer/(\\d+)").find(path)?.let {
                ClipTarget("https://www.zhihu.com${it.groupValues[1]}/answer/${it.groupValues[2]}", ClipSite.ZHIHU, "answer", it.groupValues[2]) } ?: ClipTarget(url, ClipSite.ZHIHU, "page")
            host == "douban.com" || host.endsWith(".douban.com") -> {
                Regex("^/note/(\\d+)").find(path)?.let { ClipTarget(url, ClipSite.DOUBAN, "note", it.groupValues[1]) }
                    ?: Regex("^/(?:(?:movie|book|music)/)?review/(\\d+)").find(path)?.let { ClipTarget(url, ClipSite.DOUBAN, "review", it.groupValues[1]) }
                    ?: Regex("^/group/topic/(\\d+)").find(path)?.let { ClipTarget(url, ClipSite.DOUBAN, "topic", it.groupValues[1]) }
                    ?: ClipTarget(url, ClipSite.DOUBAN, "page")
            }
            else -> ClipTarget(url, ClipSite.WEB, "page")
        }
    }
}

/** 提取结果。content 为正文所在元素；problem 不为空表示被拦（验证码 / 登录 / 已删除）。 */
class Extracted(
    val title: String, val author: String, val account: String, val published: String,
    val content: Element?, val imgMap: Map<String, String> = emptyMap(), val problem: String = "",
) {
    val textLen: Int get() = content?.text()?.count { !it.isWhitespace() } ?: 0
}

object ClipExtract {
    private val SH = ZoneId.of("Asia/Shanghai")

    fun extract(html: String, t: ClipTarget): Extracted {
        val doc = Jsoup.parse(html, t.pageUrl)
        return when (t.site) {
            ClipSite.WECHAT -> wechat(doc, html)
            ClipSite.ZHIHU -> zhihu(doc, t)
            ClipSite.DOUBAN -> doubanPage(doc) ?: generic(doc)
            ClipSite.WEB -> generic(doc)
        }
    }

    // ---------------- 微信公众号 ----------------
    fun wechat(doc: Document, html: String): Extracted {
        val bad = when {
            html.contains("wappoc_appmsgcaptcha") || doc.title().contains("环境异常") || html.contains("完成验证后即可继续访问") -> "公众号要求验证（请在微信里打开文章，点右上角「复制链接」拿短链接再试）"
            html.contains("该内容已被发布者删除") -> "这篇文章已被发布者删除"
            html.contains("此内容因违规无法查看") || html.contains("此内容被投诉且经审核涉嫌侵权") -> "这篇文章因违规或投诉无法查看"
            else -> ""
        }
        val title = doc.selectFirst("#activity-name")?.text()?.trim().orEmpty().ifEmpty { meta(doc, "og:title") }
            .ifEmpty { Regex("var msg_title\\s*=\\s*['\"](.*?)['\"]").find(html)?.groupValues?.get(1).orEmpty() }
        val account = doc.selectFirst("#js_name")?.text()?.trim().orEmpty().ifEmpty { Regex("var nickname\\s*=\\s*(?:htmlDecode\\()?\"(.*?)\"").find(html)?.groupValues?.get(1).orEmpty() }
        val author = (doc.selectFirst("#js_author_name")?.text() ?: meta(doc, "author")).trim().takeIf { it != account }.orEmpty()
        val ct = Regex("var ct\\s*=\\s*\"(\\d{9,11})\"").find(html)?.groupValues?.get(1)?.toLongOrNull()
        val published = ct?.let { DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").format(Instant.ofEpochSecond(it).atZone(SH)) }
            ?: Regex("create_time:\\s*'(\\d{4}-\\d\\d-\\d\\d \\d\\d:\\d\\d)'").find(html)?.groupValues?.get(1).orEmpty()
        val c = doc.selectFirst("#js_content") ?: doc.selectFirst(".rich_media_content")
        c?.select("mp-common-profile, .mp_profile_iframe_wrp, .js_product_container, .qr_code_pc, mp-common-mpaudio, .js_uneditable.custom_select_card_wrp, script, style")?.remove()
        c?.removeAttr("style")
        // 图片消息 / 纯文字帖没有 js_content：退回到描述 + 封面图
        val content = c ?: run {
            val desc = meta(doc, "og:description"); val img = meta(doc, "og:image")
            if (desc.isEmpty() && img.isEmpty()) null else Element("div").apply { if (desc.isNotEmpty()) appendElement("p").text(desc); if (img.isNotEmpty()) appendElement("img").attr("src", img) }
        }
        return Extracted(title, author, account, published, content, problem = bad)
    }

    // ---------------- 知乎 ----------------
    fun zhihu(doc: Document, t: ClipTarget): Extracted {
        val title0 = doc.title()
        val blocked = title0.contains("安全验证") || doc.selectFirst("meta#zh-zse-ck") != null && doc.selectFirst(".RichText") == null
        val notFound = title0.contains("没有知识存在的荒原")
        cleanZhihu(doc)
        return if (t.kind == "answer") {
            val item = doc.selectFirst(".AnswerItem, .AnswerCard .ContentItem, [itemprop=acceptedAnswer], [itemprop=suggestedAnswer]") ?: doc.body()
            val q = doc.selectFirst(".QuestionHeader-title")?.text()?.trim().orEmpty().ifEmpty { meta(doc, "og:title").substringBefore(" - ") }
            val author = item.selectFirst("[itemprop=author] meta[itemprop=name]")?.attr("content").orEmpty().ifEmpty { item.selectFirst(".AuthorInfo-name")?.text().orEmpty() }
            val date = item.selectFirst("meta[itemprop=dateCreated]")?.attr("content").orEmpty().ifEmpty { item.selectFirst(".ContentItem-time")?.text().orEmpty() }
            val content = item.selectFirst("[itemprop=text]") ?: item.selectFirst(".RichContent-inner .RichText") ?: item.selectFirst(".RichText")
            Extracted(q, author, "", normDate(date), content, problem = problemOf(blocked, notFound, content))
        } else {
            val title = doc.selectFirst("h1.Post-Title, .Post-Header h1, article h1")?.text()?.trim().orEmpty().ifEmpty { meta(doc, "og:title") }.ifEmpty { title0.substringBefore(" - 知乎") }
            val author = doc.selectFirst("[itemprop=author] meta[itemprop=name]")?.attr("content").orEmpty().ifEmpty { doc.selectFirst(".AuthorInfo-name")?.text().orEmpty() }
            val date = doc.selectFirst("meta[itemprop=datePublished]")?.attr("content").orEmpty().ifEmpty { doc.selectFirst(".ContentItem-time")?.text().orEmpty() }
            val column = doc.selectFirst(".Post-TimeExtra")?.text()?.substringAfter("来自专栏", "")?.trim().orEmpty()
            val content = doc.selectFirst(".Post-RichText") ?: doc.selectFirst(".RichText.ztext") ?: doc.selectFirst("[itemprop=articleBody]")
            Extracted(title, author, column, normDate(date), content, problem = problemOf(blocked, notFound, content))
        }
    }
    private fun problemOf(blocked: Boolean, notFound: Boolean, content: Element?) = when {
        content != null && content.text().length > 50 -> ""
        notFound -> "知乎返回「内容不存在」（可能已删除，或这次访问被限制）"
        blocked -> "知乎要求安全验证或登录"
        else -> ""
    }
    private fun cleanZhihu(doc: Document) {
        doc.select(".RichText-MCNLinkCardContainer, .RichText-ADLinkCardContainer, .RichText-EduCardContainer, [data-draft-type=edu-card], [data-draft-type=mcn-link-card], a.MCNLinkCard, .LinkCard-ecommerce, .RichText-LinkCardContainer:has(.MCNLinkCard), .ContentItem-actions, .Reward, .Post-SideActions").remove()
        // 知乎公式图：用 data-formula 换成行内代码
        doc.select("img.ztext-math, span.ztext-math").forEach { e -> val f = e.attr("data-formula").ifEmpty { e.attr("alt") }; if (f.isNotEmpty()) e.replaceWith(Element("code").text(f)) }
    }

    // ---------------- 豆瓣 ----------------
    /** 豆瓣移动版接口返回的 JSON（日记 / 影评书评 / 小组帖） */
    fun doubanJson(json: String, t: ClipTarget): Extracted {
        val j = JSONObject(json)
        val title = j.optString("title").trim()
        val who = (j.optJSONObject("author") ?: j.optJSONObject("user"))?.optString("name").orEmpty()
        val account = when (t.kind) {
            "review" -> j.optJSONObject("subject")?.optString("title")?.takeIf { it.isNotBlank() }?.let { "评《$it》" }.orEmpty()
            "topic" -> j.optJSONObject("group")?.optString("name")?.takeIf { it.isNotBlank() }?.let { if (it.endsWith("小组")) it else "${it}小组" }.orEmpty()
            else -> ""
        }
        val raw = j.optString("content")
        val imgs = mutableMapOf<String, String>()
        j.optJSONArray("photos")?.let { a ->
            for (k in 0 until a.length()) {
                val p = a.optJSONObject(k) ?: continue
                val u = p.optJSONObject("image")?.let { it.optJSONObject("large") ?: it.optJSONObject("normal") }?.optString("url").orEmpty()
                val tag = p.optString("tag_name").ifEmpty { "img_" + p.optString("id") }
                if (u.isNotEmpty()) imgs[tag] = u
            }
        }
        val body = if (raw.contains('<')) Jsoup.parseBodyFragment(raw, "https://m.douban.com/").body()
        else Element("div").apply { raw.split(Regex("\\r?\\n")).map { it.trim() }.filter { it.isNotEmpty() }.forEach { appendElement("p").text(it) } }
        // 小组帖如果是纯文字 + 图片列表，把图片补在文末
        if (!raw.contains('<') && imgs.isNotEmpty()) imgs.values.forEach { body.appendElement("img").attr("src", it) }
        if (t.kind == "review") j.optJSONObject("rating")?.optDouble("value", 0.0)?.takeIf { it > 0 }?.let { body.prependElement("p").text("评分：" + "★".repeat(it.toInt().coerceIn(0, 5)) + "☆".repeat((5 - it.toInt()).coerceIn(0, 5))) }
        return Extracted(title, who, account, normDate(j.optString("create_time")), body, imgs)
    }

    /** WebView 渲染后的豆瓣移动版页面 */
    fun doubanPage(doc: Document): Extracted? {
        val c = doc.selectFirst(".note-content-root-body, .article-rich-content-body, .frc-rich-content, #link-report, .review-content, .topic-richtext, .topic-content, .rich-content") ?: return null
        val title = doc.selectFirst("h1")?.takeIf { it.text().trim() != "豆瓣" }?.text()?.trim().orEmpty().ifEmpty { meta(doc, "og:title").substringBefore(" - ") }
        val author = doc.selectFirst(".content-with-meta-author-name, .author-name, .note-author .name, .user-name, .main-hd .name")?.text()?.trim().orEmpty()
        val date = doc.selectFirst(".content-with-meta-info time, time, .pub-date, .create-time, .main-meta")?.text()?.trim().orEmpty()
        val login = doc.location().contains("accounts.douban.com") || doc.location().contains("sec.douban.com")
        return Extracted(title, author, "", normDate(date), c, problem = if (login && c.text().length < 50) "豆瓣要求登录" else "")
    }

    // ---------------- 通用网页（Readability 式打分） ----------------
    private val unlikely = Regex("comment|sidebar|footer|footnote-ad|nav|menu|share|social|related|recommend|advert|\\bads?\\b|ad-|banner|sponsor|subscribe|newsletter|login|signup|modal|popup|cookie|breadcrumb|pagination|pager|tags?-list|author-box|toolbar|masthead", RegexOption.IGNORE_CASE)
    private val likely = Regex("article|content|post|entry|main|body|text|story|blog|hentry|rich", RegexOption.IGNORE_CASE)
    private val preferred = listOf("[itemprop=articleBody]", ".post-content", ".entry-content", ".article-content", ".article-body", ".post-body", ".markdown-body", "#article-content", ".rich_media_content", "#js_content", ".asset-content", "#main-content", ".post__content", ".content-body")

    fun generic(doc: Document): Extracted {
        val title = (meta(doc, "og:title").ifEmpty { meta(doc, "twitter:title") }).ifEmpty {
            val t = doc.title().trim(); val parts = t.split(Regex("\\s[-|_–—·]\\s"))
            if (parts.size > 1 && parts[0].length >= 4) parts[0].trim() else t
        }.ifEmpty { doc.selectFirst("h1")?.text().orEmpty() }
        val author = listOf(meta(doc, "author"), meta(doc, "article:author").takeUnless { it.startsWith("http") }.orEmpty(),
            doc.selectFirst("[itemprop=author] [itemprop=name]")?.let { it.attr("content").ifEmpty { it.text() } }.orEmpty(),
            doc.selectFirst("[rel=author], .author-name, .byline .author, .author")?.text().orEmpty())
            .map { it.trim().replace(Regex("^(作者\\s*[:：]?|文\\s*[:：|丨/]|[Bb][Yy]\\s+)\\s*"), "").trim() }.firstOrNull { it.isNotEmpty() && it.length <= 40 }.orEmpty()
        val site = meta(doc, "og:site_name").ifEmpty { runCatching { URI(doc.location()).host?.removePrefix("www.") }.getOrNull().orEmpty() }
        val date = listOf(meta(doc, "article:published_time"), meta(doc, "datePublished"), meta(doc, "pubdate"), meta(doc, "publishdate"), meta(doc, "date"),
            doc.selectFirst("time[datetime]")?.attr("datetime").orEmpty(),
            doc.selectFirst("abbr.published[title], .published[title], .dtstart[title]")?.attr("title").orEmpty(),
            Regex("\"datePublished\"\\s*:\\s*\"([^\"]+)\"").find(doc.select("script[type=application/ld+json]").joinToString { it.data() })?.groupValues?.get(1).orEmpty(),
            doc.selectFirst("time, .post-date, .entry-date, .publish-time, .pub-date, .published, .post-meta .date, .article-meta .date")?.text().orEmpty())
            .firstOrNull { it.isNotBlank() && ClipExtract.normDate(it).isNotEmpty() }.orEmpty()
        val content = readable(doc)
        // 标题常带「 - 站点名」后缀
        val siteShort = site.split(Regex("\\s[-|_–—·]\\s|｜")).first().trim()
        var tt = title.trim()
        if (siteShort.length >= 2) for (sep in listOf(" - ", " | ", " _ ", " – ", " — ", "｜", " · ")) tt = tt.removeSuffix(sep + siteShort).trim()
        return Extracted(tt, author, site, normDate(date), content)
    }

    fun readable(doc0: Document): Element? {
        val doc = doc0.clone()
        val body = doc.body() ?: return null
        body.select("script, style, noscript:not(:has(img)), iframe, form, nav, footer, aside, button, input, select, textarea, svg, canvas, [hidden], [aria-hidden=true]:not(:has(img)), .hidden, .sr-only, template").remove()
        for (sel in preferred) {
            val best = body.select(sel).maxByOrNull { it.text().length }
            if (best != null && best.text().length >= 200) return best
        }
        val arts = body.select("article").filter { it.text().length >= 200 }
        if (arts.size == 1) return arts[0]
        // 去掉明显不是正文的块
        body.getAllElements().toList().forEach { e ->
            if (e === body || e.tagName() in setOf("html", "body", "article", "main")) return@forEach
            val ci = e.className() + " " + e.id()
            if (ci.isNotBlank() && unlikely.containsMatchIn(ci) && !likely.containsMatchIn(ci) && e.selectFirst("article") == null && e.parent() != null) e.remove()
        }
        val scores = IdentityHashMap<Element, Double>()
        fun base(e: Element): Double {
            var s = when (e.tagName()) { "div", "article", "section", "main" -> 5.0; "pre", "td", "blockquote" -> 3.0; "ol", "ul", "dl", "form" -> -3.0; "h1", "h2", "h3", "h4", "th" -> -5.0; else -> 0.0 }
            val ci = e.className() + " " + e.id()
            if (likely.containsMatchIn(ci)) s += 25; if (unlikely.containsMatchIn(ci)) s -= 25
            return s
        }
        for (p in body.select("p, pre, td, blockquote, div:not(:has(div, p, section, article, table, ul, ol, pre, blockquote)), section:not(:has(div, p, section))")) {
            val txt = p.text(); if (txt.length < 25) continue
            val s = 1.0 + txt.count { it == ',' || it == '，' || it == '。' } + minOf(txt.length / 100.0, 3.0)
            val par = p.parent() ?: continue
            scores[par] = (scores[par] ?: base(par)) + s
            par.parent()?.let { gp -> scores[gp] = (scores[gp] ?: base(gp)) + s / 2 }
        }
        if (scores.isEmpty()) return body.takeIf { it.text().length > 0 }
        val top = scores.entries.maxByOrNull { (e, s) -> s * (1 - linkDensity(e)) }!!.key
        val topScore = scores[top]!! * (1 - linkDensity(top))
        val parent = top.parent() ?: return top
        val threshold = maxOf(10.0, topScore * 0.2)
        val sibs = parent.children().filter { it === top || ((scores[it] ?: 0.0) * (1 - linkDensity(it)) >= threshold) || (it.tagName() == "p" && it.text().length > 80 && linkDensity(it) < 0.25) }
        if (sibs.size <= 1) return top
        return Element("div").apply { sibs.forEach { appendChild(it.clone()) } }
    }
    private fun linkDensity(e: Element): Double { val t = e.text().length; if (t == 0) return 0.0; return e.select("a").sumOf { it.text().length }.toDouble() / t }

    fun meta(doc: Document, name: String): String =
        (doc.selectFirst("meta[property=$name]") ?: doc.selectFirst("meta[name=$name]") ?: doc.selectFirst("meta[itemprop=$name]"))?.attr("content")?.trim().orEmpty()

    /** 各种时间写法统一成「yyyy-MM-dd HH:mm」或「yyyy-MM-dd」（北京时间） */
    fun normDate(s0: String): String {
        val s = s0.trim().removePrefix("发布于").removePrefix("编辑于").trim()
        if (s.isEmpty()) return ""
        runCatching { return DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").format(OffsetDateTime.parse(s).atZoneSameInstant(SH)) }
        runCatching { return DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").format(Instant.parse(s).atZone(SH)) }
        Regex("(\\d{4})[-/年.](\\d{1,2})[-/月.](\\d{1,2})日?(?:[ T]+(\\d{1,2}):(\\d{2}))?").find(s)?.let { m ->
            val (y, mo, d, h, mi) = m.destructured
            val date = "%s-%02d-%02d".format(y, mo.toInt(), d.toInt())
            return if (h.isNotEmpty()) "%s %02d:%s".format(date, h.toInt(), mi) else date
        }
        return ""
    }
}

/** 正文 HTML → Markdown。图片先写成占位符 `shiye-img://序号`，下载完再换成本地路径或原链接。 */
class HtmlToMd(private val base: String, private val imgMap: Map<String, String> = emptyMap()) {
    val images = mutableListOf<String>()
    private val blockTags = setOf("p", "div", "section", "article", "main", "header", "footer", "aside", "h1", "h2", "h3", "h4", "h5", "h6", "ul", "ol", "li", "blockquote",
        "pre", "table", "figure", "figcaption", "hr", "dl", "dt", "dd", "center", "details", "summary", "nav", "tbody", "thead", "tr")
    private val blockCss = blockTags.joinToString(",")

    fun convert(root: Element): String {
        prep(root)
        val out = mutableListOf<String>()
        container(root, out)
        return out.joinToString("\n\n").replace(Regex("\n{3,}"), "\n\n").trim()
    }

    /** noscript 里藏着真图（知乎）：同级已有图就丢掉，否则展开 */
    private fun prep(root: Element) {
        root.select("script, style, template").remove()
        for (ns in root.select("noscript")) {
            val par = ns.parent()
            if (par != null && par.select("img").any { it.parents().none { p -> p.tagName() == "noscript" } }) ns.remove()
            else { val frag = Jsoup.parseBodyFragment(ns.html().ifBlank { ns.text() }, base).body(); ns.after(frag.html()); ns.remove() }
        }
    }

    private fun isBlock(n: Node) = n is Element && (n.tagName() in blockTags || n.selectFirst(blockCss) != null)

    private fun container(e: Element, out: MutableList<String>) {
        val buf = StringBuilder()
        fun flush() { para(buf.toString())?.let { out += it }; buf.setLength(0) }
        for (n in e.childNodes()) {
            if (isBlock(n)) { flush(); block(n as Element, out) } else buf.append(inline(n, false))
        }
        flush()
    }

    private fun para(s: String): String? {
        val t = s.lines().joinToString("\n") { it.trim() }.trim('\n', ' ')
        if (t.isBlank() || t.replace(Regex("[\\s\u200b\u00a0]"), "").isEmpty()) return null
        return t.lines().joinToString("\n") { l -> if (Regex("^(#{1,6}\\s|>|[-+*]\\s|\\d+[.)]\\s|```|~~~|---)").containsMatchIn(l)) "\\$l" else l }
    }

    private fun block(e: Element, out: MutableList<String>) {
        when (val tag = e.tagName()) {
            "h1", "h2", "h3", "h4", "h5", "h6" -> {
                val imgs = e.select("img"); imgs.forEach { img -> imgTag(img)?.let { out += it } }
                val t = inline(e, false).replace(Regex("!\\[[^]]*]\\([^)]*\\)"), "").replace('\n', ' ').trim()
                if (t.isNotEmpty()) out += "#".repeat(tag[1].digitToInt()) + " " + t.removeSurrounding("**")
            }
            "p" -> if (e.selectFirst(blockCss) != null) container(e, out) else para(inline(e, false))?.let { out += it }
            "ul", "ol" -> list(e)?.let { out += it }
            "blockquote" -> { val sub = mutableListOf<String>(); container(e, sub); if (sub.isNotEmpty()) out += sub.joinToString("\n\n").lines().joinToString("\n") { if (it.isBlank()) ">" else "> $it" } }
            "pre" -> code(e)?.let { out += it }
            "table", "tbody", "thead" -> table(e, out)
            "hr" -> out += "---"
            "figcaption" -> para(inline(e, false))?.let { out += "*${it.replace("*", "")}*" }
            else -> container(e, out)
        }
    }

    private fun list(e: Element): String? {
        val ordered = e.tagName() == "ol"
        var n = e.attr("start").toIntOrNull() ?: 1
        val items = mutableListOf<String>()
        for (li in e.children()) {
            if (li.tagName() != "li") { if (li.tagName() in setOf("ul", "ol")) list(li)?.let { items += it.prependIndent("  ") }; continue }
            val sub = mutableListOf<String>(); container(li, sub)
            if (sub.isEmpty()) continue
            val marker = if (ordered) "${n++}. " else "- "
            val pad = " ".repeat(marker.length)
            val lines = sub.joinToString("\n").lines()
            items += marker + lines.first().removePrefix("\\") + lines.drop(1).joinToString("") { "\n" + if (it.isBlank()) "" else pad + it }
        }
        return items.takeIf { it.isNotEmpty() }?.joinToString("\n")
    }

    private fun code(e: Element): String? {
        val codes = e.select("code")
        val text = if (codes.size > 1) codes.joinToString("\n") { it.wholeText() } else e.wholeText()
        if (text.isBlank()) return null
        val lang = (Regex("(?:lang|language)-([\\w+#-]+)").find(e.className() + " " + (codes.firstOrNull()?.className() ?: ""))?.groupValues?.get(1) ?: e.attr("data-lang")).trim()
        val fence = if (text.contains("```")) "~~~~" else "```"
        return "$fence$lang\n${text.trimEnd('\n', ' ')}\n$fence"
    }

    private fun table(e: Element, out: MutableList<String>) {
        val rows = if (e.tagName() == "table") e.select("> tr, > tbody > tr, > thead > tr, > tfoot > tr") else e.select("> tr")
        val cells = rows.map { r -> r.children().filter { it.tagName() == "td" || it.tagName() == "th" } }
        val cols = cells.maxOfOrNull { it.size } ?: 0
        val layout = cols <= 1 || e.selectFirst("table table, td img, td pre, td ul, td blockquote") != null || rows.size > 300
        if (layout) { rows.forEach { r -> r.children().forEach { c -> container(c, out) } }; if (rows.isEmpty()) container(e, out); return }
        fun cell(c: Element) = inline(c, false).replace('\n', ' ').replace("|", "\\|").trim()
        val head = cells.first().map(::cell) + List(cols - cells.first().size) { "" }
        val sb = StringBuilder("| " + head.joinToString(" | ") + " |\n|" + " --- |".repeat(cols))
        cells.drop(1).forEach { r -> val v = r.map(::cell) + List(cols - r.size) { "" }; sb.append("\n| ").append(v.joinToString(" | ")).append(" |") }
        out += sb.toString()
    }

    private fun imgTag(img: Element): String? {
        val u = imgUrl(img) ?: return null
        val w = img.attr("width").toIntOrNull() ?: img.attr("data-w").toIntOrNull()
        if (w != null && w in 1..24) return null           // 小图标、表情占位
        val cls = img.className()
        if (Regex("avatar|emoji|icon|logo", RegexOption.IGNORE_CASE).containsMatchIn(cls) && (w == null || w < 120)) return null
        var idx = images.indexOf(u); if (idx < 0) { images += u; idx = images.lastIndex }
        val alt = img.attr("alt").replace(Regex("[\\[\\]\\n]"), " ").trim().take(80)
        return "![${alt}](shiye-img://$idx)"
    }

    fun imgUrl(img: Element): String? {
        imgMap[img.id()]?.let { return it }
        for (a in listOf("data-original", "data-actualsrc", "data-src", "data-lazy-src", "data-lazyload", "data-url", "data-echo")) {
            val v = img.attr(a).trim(); if (v.isNotEmpty() && !v.startsWith("data:") && !v.startsWith("blob:")) return abs(v)
        }
        val ss = img.attr("data-srcset").ifEmpty { img.attr("srcset") }
        if (ss.isNotBlank()) {
            val best = ss.split(Regex(",\\s+")).mapNotNull { part -> val p = part.trim().split(Regex("\\s+")); val u = p.getOrNull(0) ?: return@mapNotNull null
                val d = p.getOrNull(1)?.let { it.dropLast(1).toDoubleOrNull() } ?: 1.0; u to d }.maxByOrNull { it.second }?.first
            if (!best.isNullOrBlank() && !best.startsWith("data:")) return abs(best)
        }
        val src = img.attr("src").trim()
        if (src.isNotEmpty() && !src.startsWith("data:") && !src.startsWith("blob:")) return abs(src)
        return null
    }

    private fun abs(v: String): String? {
        val s = when { v.startsWith("//") -> "https:$v"; else -> runCatching { URI(base).resolve(v.replace(" ", "%20")).toString() }.getOrElse { v } }
        return if (s.startsWith("http://") || s.startsWith("https://")) s.replace(" ", "%20") else null
    }

    private fun link(href0: String): String? {
        var href = href0.trim()
        if (href.isEmpty() || href.startsWith("#") || href.startsWith("javascript:", true) || href.startsWith("data:", true)) return null
        if (href.startsWith("mailto:", true)) return href.replace(" ", "%20")
        if (href.startsWith("https://link.zhihu.com/?target=")) href = runCatching { URLDecoder.decode(href.substringAfter("target=").substringBefore('&'), "UTF-8") }.getOrDefault(href)
        val a = abs(href) ?: return null
        return a.replace("(", "%28").replace(")", "%29")
    }

    private fun esc(s: String) = s.replace("\\", "\\\\").replace("*", "\\*").replace("_", "\\_").replace("`", "\\`").replace("[", "\\[").replace("]", "\\]").replace("<", "\\<").replace("~", "\\~")

    private fun boldStyle(e: Element): Boolean {
        val st = e.attr("style").lowercase().replace(" ", "")
        return Regex("font-weight:(bold|bolder|[6-9]00)").containsMatchIn(st)
    }

    private fun inline(n: Node, bold: Boolean): String = when (n) {
        is TextNode -> esc(n.text().replace(Regex("[\\u200b\\u200c\\u200d\\ufeff]"), "").replace('\u00a0', ' ').replace(Regex("[ \\t\\r\\n]+"), " "))
        is Element -> when (n.tagName()) {
            "br" -> "\n"
            "img" -> imgTag(n)?.let { "\n$it\n" } ?: ""
            "strong", "b" -> wrap(n, "**", bold) { inline(it, true) }
            "em", "i", "cite" -> wrap(n, "*", false) { inline(it, bold) }
            "del", "s", "strike" -> wrap(n, "~~", false) { inline(it, bold) }
            "code", "kbd", "tt" -> { val t = n.text().replace('\n', ' '); if (t.isBlank()) "" else if (t.contains('`')) "`` $t ``" else "`$t`" }
            "a" -> {
                val inner = kids(n, bold)
                val h = link(n.attr("href"))
                when { h == null || inner.isBlank() -> inner; inner.contains("](shiye-img://") -> inner
                    h.startsWith("https://zhida.zhihu.com/") || n.hasClass("RichContent-EntityWord") -> inner    // 知乎「直答」自动词条链接：只留文字
                    else -> "[${inner.trim().replace('\n', ' ')}]($h)" }
            }
            "script", "style", "button", "input", "select", "textarea", "svg", "iframe", "video", "audio", "canvas", "object", "embed" -> ""
            "span", "font" -> if (!bold && boldStyle(n)) wrap(n, "**", false) { inline(it, true) } else kids(n, bold)
            else -> kids(n, bold)
        }
        else -> ""
    }
    private fun kids(e: Element, bold: Boolean) = e.childNodes().joinToString("") { inline(it, bold) }
    private fun wrap(e: Element, mark: String, already: Boolean, f: (Node) -> String): String {
        val inner = e.childNodes().joinToString("") { f(it) }
        if (already || inner.isBlank() || inner.contains("](shiye-img://")) return inner
        val lead = inner.takeWhile { it == ' ' }; val trail = inner.takeLastWhile { it == ' ' }
        val core = inner.trim()
        return if (core.contains('\n')) inner else "$lead$mark$core$mark$trail"
    }
}

/** 拼成最终 Markdown：文首 YAML 元信息 + 标题 + 一行可见的来源信息 + 正文 */
object ClipMd {
    fun yamlSafe(s: String) = s.replace('\n', ' ').replace("\"", "'").trim()

    fun compose(ex: Extracted, t: ClipTarget, body: String, clippedAt: String, note: String = ""): String {
        val title = ex.title.ifBlank { "未命名网页" }
        val sb = StringBuilder("---\n")
        sb.append("title: \"").append(yamlSafe(title)).append("\"\n")
        if (ex.author.isNotBlank()) sb.append("author: \"").append(yamlSafe(ex.author)).append("\"\n")
        if (ex.account.isNotBlank()) sb.append(if (t.site == ClipSite.WECHAT) "account: \"" else "column: \"").append(yamlSafe(ex.account)).append("\"\n")
        sb.append("source: ").append(t.site.label).append('\n')
        sb.append("url: ").append(t.url).append('\n')
        if (ex.published.isNotBlank()) sb.append("published: ").append(ex.published).append('\n')
        sb.append("clipped: ").append(clippedAt).append('\n')
        sb.append("---\n\n# ").append(title.replace('\n', ' ')).append("\n\n")
        val metaLine = listOf(ex.author, ex.account, t.site.label, ex.published).filter { it.isNotBlank() }.distinct().joinToString(" · ")
        sb.append("> ").append(metaLine).append(" · [查看原文](").append(t.url.replace("(", "%28").replace(")", "%29")).append(")\n")
        if (note.isNotBlank()) sb.append(">\n> ⚠ ").append(note).append('\n')
        sb.append('\n').append(body).append('\n')
        return sb.toString()
    }
}
