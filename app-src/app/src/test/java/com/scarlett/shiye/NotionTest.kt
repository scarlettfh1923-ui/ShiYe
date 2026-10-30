package com.scarlett.shiye

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class NotionTest {
    private val hex = "0123456789abcdef0123456789abcdef"
    private val dashed = "01234567-89ab-cdef-0123-456789abcdef"
    @Test fun parseIdFromUrls() {
        assertEquals(dashed, Notion.parseId("https://www.notion.so/workspace/My-Page-$hex?pvs=4"))
        assertEquals(dashed, Notion.parseId(hex))
        assertEquals(dashed, Notion.parseId(dashed))
        assertNull(Notion.parseId("https://example.com/nothing"))
    }
    @Test fun publishToPageSendsBlocks() {
        val calls = mutableListOf<Triple<String, String, String>>()
        Notion.transport = { m, u, h, b ->
            calls += Triple(m, u, b?.toString(Charsets.UTF_8) ?: "")
            assertEquals("2025-09-03", h["Notion-Version"]); assertTrue(h["Authorization"]!!.startsWith("Bearer "))
            when {
                u.endsWith("/v1/pages") && m == "POST" -> 200 to """{"id":"11111111-1111-1111-1111-111111111111","url":"https://notion.so/x"}"""
                else -> 200 to "{}"
            }
        }
        val doc = Doc("a".repeat(16), "测试", DocType.MD, "doc.md", false, 0, 0, 0, false, "t", "", "h")
        val r = Notion.publishWith("secret_x", hex, "page", "", "", doc, "# Hi\n\n" + "长".repeat(4000) + "\n\n- a\n- b\n", null)
        assertEquals("https://notion.so/x", r.url)
        val create = calls.first { it.second.endsWith("/v1/pages") }
        val j = JSONObject(create.third)
        assertEquals(dashed, j.getJSONObject("parent").getString("page_id"))
        val kids = j.getJSONArray("children")
        assertTrue(kids.length() >= 3)
        // 富文本不超过 2000 字
        val para = (0 until kids.length()).map { kids.getJSONObject(it) }.first { it.optString("type") == "paragraph" }
        val rt = para.getJSONObject("paragraph").getJSONArray("rich_text")
        for (i in 0 until rt.length()) assertTrue(rt.getJSONObject(i).getJSONObject("text").getString("content").length <= 2000)
    }
    @Test fun explainsErrors() {
        assertTrue(Notion.explain(401, "{}").isNotBlank())
        assertTrue(Notion.explain(404, "{}").contains("连接"))
    }
}
