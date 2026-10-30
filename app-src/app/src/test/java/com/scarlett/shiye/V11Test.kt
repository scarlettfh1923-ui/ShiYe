package com.scarlett.shiye

import android.app.Application
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class V11Test {
    private fun tf(s: String) = TextFieldValue(s, TextRange(s.length))

    @Test fun undoRedoRoundTrip() {
        val h = EditHistory()
        var cur = tf("a")
        fun type(next: String, t: Long) { val n = tf(next); h.record(cur, n, now = t); cur = n }
        type("ab", 0); type("abc", 100)       // 连续输入合并成一步
        type("abcd", 2000)                    // 停顿后另起一步
        assertTrue(h.canUndo); assertFalse(h.canRedo)
        cur = h.undo(cur)!!; assertEquals("abc", cur.text)
        cur = h.undo(cur)!!; assertEquals("a", cur.text)
        assertNull(h.undo(cur))
        cur = h.redo(cur)!!; assertEquals("abc", cur.text)
        cur = h.redo(cur)!!; assertEquals("abcd", cur.text)
        assertNull(h.redo(cur))
        // 撤销后再输入，重做栈作废
        cur = h.undo(cur)!!; type("abcX", 9000); assertFalse(h.canRedo)
    }

    @Test fun undoBudgetCapsMemory() {
        val h = EditHistory(budgetChars = 1000)
        var cur = tf("x".repeat(400))
        repeat(20) { i -> val n = tf("x".repeat(400) + i); h.record(cur, n, now = i * 5000L); cur = n }
        var steps = 0; while (true) { cur = h.undo(cur) ?: break; steps++ }
        assertTrue("history should be trimmed, was $steps", steps in 1..4)
    }

    @Test fun saveKeepsOneBackupAndRestoreSwaps() {
        val app = ApplicationProvider.getApplicationContext<Application>(); Library.init(app)
        val d = Importer.fromText("# 标题\n\n第一版", null, "测试")
        assertEquals(0L, Library.backupAt(d))
        val d2 = Library.saveEdited(d, "# 标题\n\n第二版")
        assertEquals("# 标题\n\n第二版", Library.readText(d2))
        assertTrue(Library.backupAt(d2) > 0)
        assertEquals("# 标题\n\n第一版", Library.backupFile(d2).readText())
        Library.saveEdited(d2, "# 标题\n\n第三版")
        assertEquals("# 标题\n\n第二版", Library.backupFile(d2).readText())   // 只留一份：上一版
        assertEquals(1, Library.backupFile(d2).parentFile!!.listFiles()!!.size)
        val back = Library.restoreBackup(d2)
        assertEquals("# 标题\n\n第二版", back)
        assertEquals("# 标题\n\n第二版", Library.readText(d2))
        assertEquals("# 标题\n\n第三版", Library.backupFile(d2).readText())      // 恢复本身也能反悔
        // 内容未变不产生新备份
        val t = Library.backupAt(d2); Library.saveEdited(d2, "# 标题\n\n第二版"); assertEquals(t, Library.backupAt(d2))
    }

    @Test fun fileNameFromTitleIsSafe() {
        assertEquals("a_b_c_d", Importer.safeName("a/b\\c:d"))
        assertEquals("周末读书笔记：慢下来的力量".replace("：", "："), Importer.safeName("周末读书笔记：慢下来的力量"))
        assertFalse(Importer.safeName("x?y*z\"<>|").any { it in "?*\"<>|" })
        assertTrue(Importer.safeName("   ...  ").isEmpty())
    }

    @Test fun typographyPrefsClampAndRound() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val p = Prefs(app)
        p.saveLineHeight(0.5f); assertEquals(1.2f, p.lineHeight, 0.001f)
        p.saveLineHeight(9f); assertEquals(2.2f, p.lineHeight, 0.001f)
        p.saveLineHeight(1.73f); assertEquals(1.75f, p.lineHeight, 0.001f)
        p.saveParaSpacing(-1f); assertEquals(0f, p.paraSpacing, 0.001f)
        p.saveParaSpacing(3f); assertEquals(1.5f, p.paraSpacing, 0.001f)
        p.resetTypography(); assertEquals(0.6f, p.paraSpacing, 0.001f); assertEquals(1.75f, p.lineHeight, 0.001f)
        assertEquals(1f, p.fontScale, 0.001f)
    }

    @Test fun xhsSizesAndStyleSets() {
        assertEquals(1440, Exporter.xhsHeight(0)); assertEquals(1920, Exporter.xhsHeight(1))
        assertEquals(listOf("mag", "hair", "news", "note", "aurora", "scrap", "pop"), XhsStyles.all.map { it.id })
        assertEquals(7, XhsStyles.all.map { it.id }.toSet().size)
        assertEquals("mag", XhsStyles.byId("nope").id)
    }
}
