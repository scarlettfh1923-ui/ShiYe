package com.scarlett.shiye

import org.jsoup.Jsoup
import org.junit.Assert.*
import org.junit.Test
import java.io.File

/** 网页抓取提取器：用真实页面保存下来的 HTML / JSON 样本（src/test/resources/clip/）验证各站点规则。 */
class ClipTest {
    private fun res(name: String) = javaClass.classLoader!!.getResource("clip/$name")!!.readText()
    private val dump = File("build/clip-md").apply { mkdirs() }

    private fun render(ex: Extracted, t: ClipTarget, name: String): Pair<String, HtmlToMd> {
        val conv = HtmlToMd(t.pageUrl, ex.imgMap)
        var body = conv.convert(ex.content!!)
        conv.images.indices.forEach { body = body.replace("(shiye-img://$it)", "(images/%03d.jpg)".format(it + 1)) }
        val md = ClipMd.compose(ex, t, body, "2026-11-09 16:00")
        File(dump, "$name.md").writeText(md)
        return md to conv
    }

    @Test fun findUrl() {
        assertEquals("https://zhuanlan.zhihu.com/p/123?utm_id=0", ClipUrl.find("【智谱开源最快推理模型 - 知乎】 https://zhuanlan.zhihu.com/p/123?utm_id=0"))
        assertEquals("https://mp.weixin.qq.com/s/2fBjEf6kDX0245-tMgS5uA", ClipUrl.find("https://mp.weixin.qq.com/s/2fBjEf6kDX0245-tMgS5uA\n"))
        assertEquals("https://example.com/a", ClipUrl.find("http://example.com/a"))
        assertNull(ClipUrl.find("看这两个 https://a.com/1 和 https://b.com/2"))
        assertNull(ClipUrl.find("一".repeat(400) + " https://a.com/1"))
        assertNull(ClipUrl.find("没有链接"))
        assertNull(ClipUrl.normalize("https://localhost:8080/x"))
        assertNull(ClipUrl.normalize("https://127.0.0.1/x"))
        assertNull(ClipUrl.normalize("file:///sdcard/a.html"))
        assertNull(ClipUrl.normalize("javascript:alert(1)"))
        assertEquals("https://example.com/%E4%B8%AD", ClipUrl.normalize("https://example.com/中"))
    }

    @Test fun classify() {
        assertEquals(ClipSite.WECHAT, ClipUrl.classify("https://mp.weixin.qq.com/s/abc").site)
        ClipUrl.classify("https://zhuanlan.zhihu.com/p/1895498000702363235?utm_psn=1").let { assertEquals("article", it.kind); assertEquals("https://zhuanlan.zhihu.com/p/1895498000702363235", it.url) }
        ClipUrl.classify("https://www.zhihu.com/question/2042336304244019752/answer/2042841805306192077?share_code=x").let {
            assertEquals("answer", it.kind); assertEquals("2042841805306192077", it.id); assertEquals("https://www.zhihu.com/question/2042336304244019752/answer/2042841805306192077", it.url) }
        ClipUrl.classify("https://www.douban.com/note/870769226/").let { assertEquals("note", it.kind); assertEquals("https://m.douban.com/rexxar/api/v2/note/870769226", it.doubanApi); assertEquals("https://m.douban.com/note/870769226/", it.pageUrl) }
        ClipUrl.classify("https://book.douban.com/review/123/").let { assertEquals("review", it.kind); assertEquals("https://m.douban.com/book/review/123/", it.pageUrl) }
        ClipUrl.classify("https://m.douban.com/movie/review/17492958/").let { assertEquals("review", it.kind); assertEquals("17492958", it.id) }
        ClipUrl.classify("https://www.douban.com/group/topic/475015572/?_spm_id=x").let { assertEquals("topic", it.kind); assertEquals("https://m.douban.com/rexxar/api/v2/group/topic/475015572", it.doubanApi) }
        assertEquals(ClipSite.WEB, ClipUrl.classify("https://www.ruanyifeng.com/blog/2024/01/weekly-issue-286.html").site)
    }

    @Test fun wechat() {
        val t = ClipUrl.classify("https://mp.weixin.qq.com/s/2fBjEf6kDX0245-tMgS5uA")
        val ex = ClipExtract.extract(res("wechat.html"), t)
        assertEquals("今年最难的机器人Demo，“机器人含量”为0", ex.title)
        assertEquals("量子位", ex.account)
        assertEquals("2026-09-02 20:22", ex.published)
        assertEquals("", ex.problem)
        assertTrue("正文字数 ${ex.textLen}", ex.textLen > 2000)
        val (md, conv) = render(ex, t, "wechat")
        assertTrue(conv.images.size >= 5); assertTrue(conv.images.all { it.startsWith("https://mmbiz.qpic.cn/") })
        assertTrue(md.contains("source: 微信")); assertTrue(md.contains("![](images/001.jpg)") || md.contains("](images/001.jpg)"))
        assertFalse(md.contains("visibility"))
        assertTrue(Markdown.parse(md).any { it is MdBlock.Image && it.url.startsWith("images/") })
    }

    @Test fun wechatCaptcha() {
        val t = ClipUrl.classify("https://mp.weixin.qq.com/s?__biz=x")
        val ex = ClipExtract.extract("<html><head><title>环境异常</title></head><body>当前环境异常，完成验证后即可继续访问。</body></html>", t)
        assertTrue(ex.problem.contains("验证"))
    }

    @Test fun zhihuArticle() {
        val t = ClipUrl.classify("https://zhuanlan.zhihu.com/p/1895498000702363235")
        val ex = ClipExtract.extract(res("zhihu_article.html"), t)
        assertEquals("智谱开源最快推理模型，附MCP应用实践代码", ex.title)
        assertEquals("刘聪NLP", ex.author)
        assertEquals("2025-04-15 15:38", ex.published)
        assertTrue(ex.textLen > 1500)
        val (md, conv) = render(ex, t, "zhihu_article")
        assertTrue(conv.images.isNotEmpty()); assertTrue(conv.images.all { it.contains("zhimg.com") })
        assertEquals("noscript 里的图不能重复", conv.images.size, conv.images.map { it.substringAfterLast('/').substringBefore('_') }.distinct().size)
        assertTrue(md.contains("source: 知乎"))
    }

    @Test fun zhihuAnswer() {
        val t = ClipUrl.classify("https://www.zhihu.com/question/2042336304244019752/answer/2042841805306192077")
        val ex = ClipExtract.extract(res("zhihu_answer.html"), t)
        assertEquals("如何评价comma.ai创始人George Hotz断言AI编程Agent无法真正编程？", ex.title)
        assertEquals("平凡", ex.author)
        assertTrue(ex.published.startsWith("2026-"))
        assertTrue(ex.textLen > 2500)
        val (md, conv) = render(ex, t, "zhihu_answer")
        assertTrue(conv.images.isNotEmpty())
        assertFalse("带货卡片应去掉", md.contains("￥0.00"))
        assertTrue(md.contains("当代码生成变便宜"))
    }

    @Test fun doubanNoteApi() {
        val t = ClipUrl.classify("https://www.douban.com/note/870769226/")
        val ex = ClipExtract.doubanJson(res("douban_note.json"), t)
        assertEquals("极简生活记录：202502", ex.title)
        assertEquals("黑道少年沈來迟", ex.author)
        assertEquals("2025-03-01 18:46", ex.published)
        val (md, conv) = render(ex, t, "douban_note")
        assertTrue("图片 ${conv.images.size}", conv.images.size >= 10)
        assertTrue(conv.images.all { it.contains("doubanio.com") })
        assertTrue(md.contains("## 独居生活"))
        assertTrue(md.contains("source: 豆瓣"))
    }

    @Test fun doubanReviewAndTopic() {
        val r = ClipUrl.classify("https://movie.douban.com/review/17492958/")
        val ex = ClipExtract.doubanJson(res("douban_review.json"), r)
        assertEquals("暂列年度十佳：2026年最美好的公路片！", ex.title)
        assertEquals("评《东京出租车》", ex.account)
        assertTrue(ex.author.isNotBlank())
        val (md, conv) = render(ex, r, "douban_review"); assertTrue(conv.images.isNotEmpty()); assertTrue(md.contains("山田洋次"))
        val g = ClipUrl.classify("https://www.douban.com/group/topic/475015572/")
        val ex2 = ClipExtract.doubanJson(res("douban_topic.json"), g)
        assertEquals("一个关于开组组规补充建议的讨论楼", ex2.title)
        assertTrue(ex2.account.endsWith("小组"))
        val (_, c2) = render(ex2, g, "douban_topic"); assertTrue(c2.images.isNotEmpty())
    }

    @Test fun doubanRenderedPage() {
        val t = ClipUrl.classify("https://www.douban.com/note/870769226/")
        val ex = ClipExtract.extract(res("douban_note_rendered.html"), t)
        assertEquals("极简生活记录：202502", ex.title)
        assertEquals("黑道少年沈來迟", ex.author)
        assertTrue(ex.textLen > 3000)
        render(ex, t, "douban_rendered")
    }

    @Test fun genericBlog() {
        val t = ClipUrl.classify("https://www.ruanyifeng.com/blog/2024/01/weekly-issue-286.html")
        val ex = ClipExtract.extract(res("blog_ruanyifeng.html"), t)
        assertTrue(ex.title, ex.title.contains("科技爱好者周刊"))
        assertTrue("正文字数 ${ex.textLen}", ex.textLen > 2000)
        assertTrue(ex.published.startsWith("2024-01"))
        val (md, conv) = render(ex, t, "blog")
        assertTrue(conv.images.isNotEmpty())
        assertTrue(md.contains("source: 网页"))
        assertFalse("不应带上评论区/侧栏", md.contains("留言（"))
    }

    @Test fun htmlToMdBasics() {
        val h = """<div><h2>小标题</h2><p>一段<strong>加粗</strong>和<em>斜体</em>，还有<a href="/x">链接</a>。</p>
            <ul><li>第一</li><li>第二<ul><li>嵌套</li></ul></li></ul><ol start="3"><li>三</li><li>四</li></ol>
            <blockquote><p>引用一句</p></blockquote><pre><code class="language-kotlin">val a = 1
println(a)</code></pre>
            <table><tr><th>名</th><th>值</th></tr><tr><td>a</td><td>1</td></tr></table>
            <p><img data-src="https://img.example.com/a.png" src="data:image/gif;base64,R0"></p><p>1. 不是列表</p></div>"""
        val conv = HtmlToMd("https://example.com/post/1")
        val md = conv.convert(Jsoup.parseBodyFragment(h).body())
        assertTrue(md, md.contains("## 小标题"))
        assertTrue(md.contains("一段**加粗**和*斜体*，还有[链接](https://example.com/x)。"))
        assertTrue(md.contains("- 第一\n- 第二\n  - 嵌套"))
        assertTrue(md.contains("3. 三\n4. 四"))
        assertTrue(md.contains("> 引用一句"))
        assertTrue(md.contains("```kotlin\nval a = 1\nprintln(a)\n```"))
        assertTrue(md.contains("| 名 | 值 |"))
        assertEquals(listOf("https://img.example.com/a.png"), conv.images)
        assertTrue(md.contains("\\1. 不是列表"))
        val blocks = Markdown.parse(md)
        assertTrue(blocks.any { it is MdBlock.Lst }); assertTrue(blocks.any { it is MdBlock.Code }); assertTrue(blocks.any { it is MdBlock.Table }); assertTrue(blocks.any { it is MdBlock.Quote })
    }

    @Test fun normDate() {
        assertEquals("2025-04-15 15:38", ClipExtract.normDate("2025-04-15T07:38:02.000Z"))
        assertEquals("2025-03-01 18:46", ClipExtract.normDate("2025-03-01 18:46:15"))
        assertEquals("2024-01-05", ClipExtract.normDate("2024年1月5日"))
        assertEquals("2025-04-15 15:38", ClipExtract.normDate("发布于 2025-04-15 15:38・江苏"))
    }
}
