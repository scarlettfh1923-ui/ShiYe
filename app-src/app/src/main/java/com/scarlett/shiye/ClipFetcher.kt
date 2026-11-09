package com.scarlett.shiye

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONTokener
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.coroutineContext

class ClipException(msg: String) : Exception(msg)

/** 抓取结果：doc 已存进书库；note 不为空表示「只抓到部分内容」之类的提醒；failedImages 为没下载下来、保留原链接的图片数 */
class ClipResult(val doc: Doc, val note: String, val images: Int, val failedImages: Int, val existed: Boolean = false)

/**
 * 分享 / 剪贴板进来的链接先放这里，再由 MainActivity 取走。
 * 只在本进程内传递：不把链接塞进对外暴露的 MainActivity 的 Intent extras，避免被别的应用借道触发（android-intent-security）。
 */
object PendingClip {
    @Volatile private var url: String? = null
    fun offer(u: String) { url = u }
    fun take(): String? = url.also { url = null }
}

object ClipFetcher {
    const val UA = "Mozilla/5.0 (Linux; Android 15; 24129PN74C Build/AQ3A.240812.002) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.6723.103 Mobile Safari/537.36"
    private const val MAX_HTML = 12L * 1024 * 1024
    private const val MAX_IMG = 25L * 1024 * 1024
    private const val MAX_IMAGES = 300

    val http: OkHttpClient by lazy {
        OkHttpClient.Builder().connectTimeout(12, TimeUnit.SECONDS).readTimeout(25, TimeUnit.SECONDS).callTimeout(60, TimeUnit.SECONDS)
            .followRedirects(true).followSslRedirects(false).retryOnConnectionFailure(true).build()
    }

    private suspend fun Call.await(): Response = suspendCancellableCoroutine { c ->
        enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) { c.resume(response) }
            override fun onFailure(call: Call, e: IOException) { if (!c.isCancelled) c.resumeWithException(e) }
        })
        c.invokeOnCancellation { runCatching { cancel() } }
    }

    private fun req(url: String, referer: String?, accept: String) = Request.Builder().url(url)
        .header("User-Agent", UA).header("Accept", accept).header("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.6")
        .apply { if (referer != null) header("Referer", referer) }.build()

    private class Page(val code: Int, val body: String, val finalUrl: String)

    private suspend fun get(url: String, referer: String? = null, accept: String = "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8"): Page {
        val r = http.newCall(req(url, referer, accept)).await()
        return withContext(Dispatchers.IO) {
            r.use { resp ->
                val b = resp.body ?: return@use Page(resp.code, "", resp.request.url.toString())
                if (b.contentLength() > MAX_HTML) throw ClipException("网页太大了")
                val bytes = b.byteStream().use { readLimited(it, MAX_HTML) } ?: throw ClipException("网页太大了")
                Page(resp.code, decode(bytes, b.contentType()?.charset()?.name()), resp.request.url.toString())
            }
        }
    }

    private fun readLimited(i: java.io.InputStream, max: Long): ByteArray? {
        val out = java.io.ByteArrayOutputStream(); val buf = ByteArray(64 * 1024); var total = 0L
        while (true) { val n = i.read(buf); if (n < 0) break; total += n; if (total > max) return null; out.write(buf, 0, n) }
        return out.toByteArray()
    }

    /** 优先 HTTP 头里的编码，其次 <meta charset>，默认 UTF-8（老站点常见 GBK） */
    private fun decode(b: ByteArray, headerCs: String?): String {
        val cs = headerCs ?: Regex("<meta[^>]+charset=[\"']?([\\w-]+)", RegexOption.IGNORE_CASE).find(String(b, 0, minOf(b.size, 4096), Charsets.ISO_8859_1))?.groupValues?.get(1)
        val c = runCatching { charset(if (cs.equals("gb2312", true)) "GBK" else cs ?: "UTF-8") }.getOrDefault(Charsets.UTF_8)
        return String(b, c)
    }

    private fun weak(e: Extracted?, t: ClipTarget) = e == null || e.content == null || e.problem.isNotEmpty() ||
        e.textLen < (if (t.site == ClipSite.DOUBAN && t.kind != "page") 10 else 200)
    private fun better(a: Extracted?, b: Extracted?): Extracted? = when {
        a == null -> b; b == null -> a
        a.problem.isEmpty() && b.problem.isNotEmpty() && a.content != null -> a
        b.problem.isEmpty() && a.problem.isNotEmpty() && b.content != null -> b
        else -> if (b.textLen > a.textLen) b else a
    }

    /** 全流程：读网页 →（必要时）WebView 兜底 → 提取 → 转 Markdown → 下载图片 → 存书库。所有网络与文件操作都在 IO 线程。 */
    suspend fun run(ctx: Context, rawUrl: String, progress: (String, Float) -> Unit): ClipResult {
        val url = ClipUrl.normalize(rawUrl) ?: throw ClipException("这不是可以抓取的网页链接")
        val t = ClipUrl.classify(url)
        withContext(Dispatchers.IO) { Library.findByUrl(t.url)?.let { Library.touch(it.id); Library.get(it.id) ?: it } }?.let { return ClipResult(it, "", 0, 0, existed = true) }
        progress("正在读取${t.site.label}…", 0.05f)
        var ex: Extracted? = null
        var lastErr: String? = null
        // 1) 豆瓣：移动版接口
        t.doubanApi?.let { api ->
            runCatching { val p = get(api, referer = t.pageUrl, accept = "application/json, text/plain, */*"); if (p.code in 200..299 && p.body.trimStart().startsWith("{")) ClipExtract.doubanJson(p.body, t) else null }
                .onSuccess { ex = it }.onFailure { if (it is kotlinx.coroutines.CancellationException) throw it; lastErr = it.message }
        }
        coroutineContext.ensureActive()
        // 2) 直接请求网页（知乎直连必被 JS 验证拦下，直接走 WebView）
        if (weak(ex, t) && t.site != ClipSite.ZHIHU) {
            runCatching {
                val p = get(t.pageUrl, referer = null)
                if (p.code in 200..299 || p.body.length > 2000) withContext(Dispatchers.Default) { ClipExtract.extract(p.body, t) } else { lastErr = "网站返回 ${p.code}"; null }
            }.onSuccess { ex = better(ex, it) }.onFailure { if (it is kotlinx.coroutines.CancellationException) throw it; lastErr = it.message }
        }
        coroutineContext.ensureActive()
        // 3) 兜底：隐藏 WebView 加载（执行页面脚本、通过 JS 验证），取渲染后的 HTML
        if (weak(ex, t)) {
            progress("用浏览器内核加载页面…", 0.22f)
            val html = WebLoader.load(ctx, t.pageUrl)
            if (html != null) ex = better(ex, withContext(Dispatchers.Default) { ClipExtract.extract(html, t) })
        }
        coroutineContext.ensureActive()
        val e = ex
        if (e == null || e.content == null || e.textLen < 20) {
            throw ClipException(e?.problem?.takeIf { it.isNotEmpty() }?.let { "没能抓取：$it" }
                ?: if (lastErr != null && e == null) "没能打开这个网页（$lastErr）" else "没找到正文：页面可能需要登录，或者不是文章页")
        }
        val note = when {
            e.problem.isNotEmpty() -> "只抓到部分内容：${e.problem}"
            e.textLen < 200 && !(t.site == ClipSite.DOUBAN && t.kind != "page") -> "只抓到部分内容（正文很短，可能需要登录或页面靠脚本加载）"
            else -> ""
        }
        progress("整理排版…", 0.35f)
        val conv = HtmlToMd(t.pageUrl, e.imgMap)
        var body = withContext(Dispatchers.Default) { conv.convert(e.content) }
        // 4) 图片落盘：files/library/<id>/images/
        val id = Library.newId()
        val dir = File(Library.rootDir, id)
        var failed = 0
        try {
            val imgs = conv.images.take(MAX_IMAGES)
            val local = arrayOfNulls<String>(imgs.size)
            if (imgs.isNotEmpty()) {
                val imgDir = File(dir, "images").apply { mkdirs() }
                var done = 0
                val sem = Semaphore(4)
                coroutineScope {
                    imgs.mapIndexed { i, u ->
                        async(Dispatchers.IO) {
                            sem.withPermit {
                                val stem = "%03d".format(i + 1)
                                local[i] = runCatching { downloadImage(u, t.referer, imgDir, stem) }.getOrNull()
                                    ?: run {   // 图床偶尔限流：稍等重试一次（豆瓣换一台图片服务器）
                                        kotlinx.coroutines.delay(700)
                                        val alt = u.replace(Regex("^https://img\\d\\.doubanio\\.com/"), "https://img3.doubanio.com/")
                                        runCatching { downloadImage(alt, t.referer, imgDir, stem) }.getOrNull()
                                    }
                                synchronized(local) { done++; progress("下载图片 $done/${imgs.size}", 0.35f + 0.6f * done / imgs.size) }
                            }
                        }
                    }.awaitAll()
                }
            }
            for (i in conv.images.indices) {
                val rep = local.getOrNull(i)?.let { "images/$it" } ?: conv.images[i].also { failed++ }
                body = body.replace("(shiye-img://$i)", "($rep)")
            }
            coroutineContext.ensureActive()
            progress("存入书库…", 0.97f)
            val now = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA).format(Date())
            val md = ClipMd.compose(e, t, body, now, note)
            val doc = withContext(Dispatchers.IO) { Importer.saveClip(id, dir, md, e.title.ifBlank { "未命名网页" }, "抓取 · ${t.site.label}", t.url, conv.images.isNotEmpty()) }
            return ClipResult(doc, note, conv.images.size, failed)
        } catch (x: Throwable) {
            withContext(kotlinx.coroutines.NonCancellable + Dispatchers.IO) { dir.deleteRecursively() }   // 取消或失败：不留半成品
            throw x
        }
    }

    /** 带 Referer + 移动端 UA 下载一张图；只收图片类型，单张上限 25 MB。返回文件名，失败返回 null（调用方保留原链接）。 */
    private suspend fun downloadImage(u: String, referer: String, dir: File, stem: String): String? {
        val url = if (u.startsWith("http://")) "https://" + u.substring(7) else u
        val r = http.newCall(req(url, referer, "image/avif,image/webp,image/apng,image/*,*/*;q=0.8")).await()
        return r.use { resp ->
            if (!resp.isSuccessful) return null
            val b = resp.body ?: return null
            val type = b.contentType()?.let { "${it.type}/${it.subtype}" }?.lowercase().orEmpty()
            val ext = when {
                type.contains("jpeg") || type.contains("jpg") -> "jpg"; type.contains("png") -> "png"; type.contains("gif") -> "gif"; type.contains("webp") -> "webp"
                type.contains("avif") -> "avif"; type.contains("heic") || type.contains("heif") -> "heic"; type.contains("bmp") -> "bmp"
                type.isEmpty() || type == "application/octet-stream" || type == "binary/octet-stream" -> Regex("wx_fmt=(\\w+)").find(url)?.groupValues?.get(1)?.lowercase()?.let { if (it == "jpeg") "jpg" else it }
                    ?: url.substringBefore('?').substringAfterLast('.', "").lowercase().takeIf { it in setOf("jpg", "jpeg", "png", "gif", "webp") } ?: "jpg"
                else -> return null              // svg / html 错误页等：不落盘
            }
            if (b.contentLength() > MAX_IMG) return null
            val f = File(dir, "$stem.$ext")
            val bytes = b.byteStream().use { readLimited(it, MAX_IMG) } ?: return null
            if (bytes.size < 64) return null
            f.writeBytes(bytes); f.name
        }
    }
}

/**
 * 隐藏 WebView：加载页面、让页面脚本跑完（知乎的 JS 验证、豆瓣移动版的前端渲染），稍等片刻后取 document.documentElement.outerHTML。
 * 不注入任何 JS 接口；禁止 file/content 访问；只允许 http(s) 跳转（拦掉 zhihu://、intent:// 这类唤起 App 的链接）；不加载图片省流量。
 * 必须在主线程调用；超时、取消都会销毁 WebView。
 */
object WebLoader {
    private const val GRAB = "(function(){return document.documentElement.outerHTML})()"

    /** 返回渲染后的 HTML；几 MB 的 JSON 字符串解码放到后台线程做，不占主线程 */
    suspend fun load(ctx: Context, url: String, timeoutMs: Long = 25_000, settleMs: Long = 2_500): String? {
        val raw = withContext(Dispatchers.Main) { loadRaw(ctx, url, timeoutMs, settleMs) } ?: return null
        return withContext(Dispatchers.Default) { runCatching { JSONTokener(raw).nextValue() as? String }.getOrNull() }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private suspend fun loadRaw(ctx: Context, url: String, timeoutMs: Long, settleMs: Long): String? {
        val main = Handler(Looper.getMainLooper())
        var wv: WebView? = null
        try {
            val result = withTimeoutOrNull(timeoutMs) {
                suspendCancellableCoroutine<String?> { c ->
                    val w = WebView(ctx.applicationContext); wv = w
                    w.settings.apply {
                        javaScriptEnabled = true; domStorageEnabled = true
                        allowFileAccess = false; allowContentAccess = false
                        blockNetworkImage = true; loadsImagesAutomatically = false
                        javaScriptCanOpenWindowsAutomatically = false; setSupportMultipleWindows(false)
                        mediaPlaybackRequiresUserGesture = true
                    }
                    var grabbed = false
                    val grab = Runnable {
                        if (grabbed || !c.isActive) return@Runnable
                        grabbed = true
                        w.evaluateJavascript(GRAB) { r -> if (c.isActive) c.resume(r) }
                    }
                    w.webViewClient = object : WebViewClient() {
                        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                            val s = request.url.scheme?.lowercase(); return s != "https" && s != "http"
                        }
                        override fun onPageFinished(view: WebView, u: String?) {
                            // 验证页会自己刷新一次：每次加载完都重新计时，最后一次加载完 settleMs 后再取
                            main.removeCallbacks(grab); main.postDelayed(grab, settleMs)
                        }
                    }
                    c.invokeOnCancellation { main.removeCallbacks(grab) }
                    w.loadUrl(url)
                }
            }
            // 超时：页面还在转圈也先把已渲染的部分拿下来
            return result ?: wv?.let { w -> withTimeoutOrNull(3_000) { suspendCancellableCoroutine<String?> { c -> w.evaluateJavascript(GRAB) { r -> if (c.isActive) c.resume(r) } } } }
        } finally {
            val w = wv
            if (w != null) main.post { runCatching { w.stopLoading(); w.destroy() } }   // 取消、超时、成功都销毁
        }
    }
}
