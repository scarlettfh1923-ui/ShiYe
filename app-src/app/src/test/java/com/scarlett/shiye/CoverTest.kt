package com.scarlett.shiye

import org.junit.Assert.*
import org.junit.Test

class CoverTest {
    @Test fun splitsOnDashColonAndKeepsPlain() {
        assertEquals("在《太太万岁》与《梁祝》之间" to "桑弧的两个上海", splitTitle("在《太太万岁》与《梁祝》之间——桑弧的两个上海"))
        assertEquals("京都五日" to "被雨水洗过的街道", splitTitle("京都五日：被雨水洗过的街道"))
        assertEquals("会议纪要" to "", splitTitle("会议纪要"))
        assertEquals("a" to "b", splitTitle("a: b"))
    }
    @Test fun bookTitlesGetJoinersOnlyInsideBrackets() {
        val out = keepBookTitles("在《太太》与梁祝")
        assertEquals("在《\u2060太\u2060太\u2060》与梁祝", out)
        assertEquals("在《太太》与梁祝", out.replace("\u2060", ""))
        assertFalse(out.startsWith("在\u2060"))
    }
    @Test fun sourceNamesAndKinds() {
        assertEquals("微信", sourceName("分享 · 微信")); assertEquals("分享", sourceKind("分享 · 微信"))
        assertEquals("文件", sourceName("文件")); assertNull(sourceKind("文件"))
    }
    @Test fun wordCountAndThousands() {
        assertEquals(5, wordCount("你好 世界\n a"))
        assertEquals("4,126", fmtThousands(4126)); assertEquals("209", fmtThousands(209))
    }
    @Test fun coverVariantIsStable() {
        val d = Doc(id = "abc", title = "t", type = DocType.MD, main = "a.md", bundle = false, createdAt = 1L, openedAt = 1L, size = 1L, fav = false, source = "文件", snippet = "", hash = "h")
        assertEquals(coverVariant(d), coverVariant(d)); assertTrue(coverVariant(d) in 0..2)
    }
    @Test fun coverPalettePicksBySource() {
        fun doc(src: String) = Doc(id = "id", title = "标题", type = DocType.MD, main = "a.md", bundle = false, createdAt = 1L, openedAt = 1L, size = 1L, fav = false, source = src, snippet = "", hash = "h")
        val wx = coverPalette(doc("分享 · 微信"))
        val sf = coverPalette(doc("分享 · Safari"))
        val gh = coverPalette(doc("GitHub"))
        assertEquals(wx.top, coverPalette(doc("分享 · 微信")).top)
        assertNotEquals(wx.top, sf.top)
        assertNotEquals(sf.top, gh.top)
        assertNotEquals(wx.top, gh.top)
    }
}
