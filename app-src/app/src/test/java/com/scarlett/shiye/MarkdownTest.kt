package com.scarlett.shiye

import org.junit.Assert.*
import org.junit.Test

class MarkdownTest {
    @Test fun headingsAndParas() {
        val b = Markdown.parse("# 标题\n\n正文 **粗体** 和 `code`\n")
        assertTrue(b[0] is MdBlock.Heading)
        assertEquals(1, (b[0] as MdBlock.Heading).level)
        val p = b[1] as MdBlock.Para
        assertTrue(p.segs.any { it.b })
    }
    @Test fun tableParses() {
        val b = Markdown.parse("| a | b |\n|:--|--:|\n| 1 | 2 |\n| 3 | 4 |\n")
        val t = b.filterIsInstance<MdBlock.Table>().single()
        assertEquals(2, t.header.size); assertEquals(2, t.rows.size)
        assertEquals(listOf(0, 2), t.align)
    }
    @Test fun codeFence() {
        val c = Markdown.parse("```kotlin\nval x = 1\n```\n").filterIsInstance<MdBlock.Code>().single()
        assertEquals("kotlin", c.lang); assertEquals("val x = 1", c.text.trim())
    }
    @Test fun listsAndTasks() {
        val l = Markdown.parse("- [x] 完成\n- [ ] 待办\n").filterIsInstance<MdBlock.Lst>().single()
        assertEquals(listOf<Boolean?>(true, false), l.items.map { it.checked })
    }
    @Test fun imageAndQuote() {
        val b = Markdown.parse("> 引用\n\n![图](a.png)\n")
        assertTrue(b.any { it is MdBlock.Quote }); assertTrue(b.any { it is MdBlock.Image && it.url == "a.png" })
    }
    @Test fun frontMatter() {
        val (body, meta) = Markdown.stripFrontMatter("---\ntitle: 你好\ntags: [a, b]\n---\n正文")
        assertEquals("正文", body.trim()); assertEquals("你好", meta["title"])
    }
}
