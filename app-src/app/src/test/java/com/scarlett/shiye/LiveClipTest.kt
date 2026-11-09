package com.scarlett.shiye

import okhttp3.OkHttpClient
import okhttp3.Request
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.util.concurrent.TimeUnit

/**
 * 真实网络实测（默认跳过）：./gradlew testDebugUnitTest --tests '*LiveClipTest*' （环境变量 SHIYE_LIVE=1）
 * 与 App 用同样的 UA / Referer / 接口顺序请求真实页面，跑提取器并试下载前 3 张图。WebView 兜底这一步在 JVM 里跑不了，只报告「需要 WebView」。
 */
class LiveClipTest {
    private val http = OkHttpClient.Builder().connectTimeout(15, TimeUnit.SECONDS).readTimeout(30, TimeUnit.SECONDS).build()
    private fun get(u: String, ref: String? = null, accept: String = "text/html,*/*"): Pair<Int, String> =
        http.newCall(Request.Builder().url(u).header("User-Agent", ClipFetcher.UA).header("Accept", accept).header("Accept-Language", "zh-CN,zh;q=0.9").apply { if (ref != null) header("Referer", ref) }.build())
            .execute().use { it.code to (it.body?.string() ?: "") }
    private fun img(u: String, ref: String): String = http.newCall(Request.Builder().url(u).header("User-Agent", ClipFetcher.UA).header("Referer", ref).header("Accept", "image/*").build()).execute()
        .use { "${it.code} ${it.body?.contentType()} ${it.body?.bytes()?.size}" }

    @Test fun live() {
        assumeTrue(System.getenv("SHIYE_LIVE") == "1")
        val urls = listOf(
            "https://mp.weixin.qq.com/s/2fBjEf6kDX0245-tMgS5uA",
            "https://zhuanlan.zhihu.com/p/1895498000702363235",
            "https://www.zhihu.com/question/2042336304244019752/answer/2042841805306192077",
            "https://www.douban.com/note/870769226/",
            "https://movie.douban.com/review/17492958/",
            "https://www.douban.com/group/topic/475015572/",
            "https://www.ruanyifeng.com/blog/2024/01/weekly-issue-286.html",
            "https://sspai.com/post/73145",
        )
        val sb = StringBuilder()
        for (u in urls) {
            val t = ClipUrl.classify(u)
            val line = runCatching {
                var ex: Extracted? = null; var how = ""
                t.doubanApi?.let { api -> val (c, b) = get(api, t.pageUrl, "application/json"); if (c == 200) { ex = ClipExtract.doubanJson(b, t); how = "接口 200" } else how = "接口 $c" }
                if (ex == null) { val (c, b) = get(t.pageUrl); how += " 网页 $c"; if (c in 200..299) ex = ClipExtract.extract(b, t) }
                val e = ex
                if (e == null || e.textLen < 200) "${t.site.label} $u → 直连拿不到正文（$how），App 会用 WebView 兜底" + (e?.problem?.let { " · $it" } ?: "")
                else {
                    val conv = HtmlToMd(t.pageUrl, e.imgMap); conv.convert(e.content!!)
                    val imgs = conv.images.take(3).map { img(it, t.referer) }
                    "${t.site.label} $u → 成功（$how）标题「${e.title}」作者「${e.author}」${e.account} 时间 ${e.published} 正文 ${e.textLen} 字 图 ${conv.images.size} 张；前 3 张下载：$imgs"
                }
            }.getOrElse { "${t.site.label} $u → 异常 ${it.message}" }
            println(line); sb.appendLine(line)
        }
        java.io.File("build/live-clip.txt").writeText(sb.toString())
    }
}
