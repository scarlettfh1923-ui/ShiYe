package com.scarlett.shiye

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class ShotTest {
    val sample = """
# 周末读书笔记：慢下来的力量

> 真正的效率，来自于对注意力的尊重。

## 三个小习惯

1. 每天固定 **25 分钟** 阅读，不看手机
2. 读完写 *三句话* 总结
3. 周末回顾，挑一条付诸行动

| 书名 | 作者 | 评分 |
|:--|:--|--:|
| 《深度工作》 | Cal Newport | 9.0 |
| 《被讨厌的勇气》 | 岸见一郎 | 8.6 |

```kotlin
fun focus(minutes: Int = 25) = repeat(minutes) { /* 专注 */ }
```

- [x] 整理书架
- [ ] 写一篇读后感

---

最后，记得把喜欢的句子存进「拾页」，下次翻起来依然有温度。
"""
    /** 更长的示例：保证小红书图文有好几页，覆盖 H2/H3、引用、提示框、加粗、列表、表格、代码 */
    val long = sample.replace("---\n", "---\n\n### 小结\n\n慢下来**不是偷懒**，而是把力气用在**真正重要**的事情上。每天留出一段不被打断的时间，哪怕只有二十五分钟，也足够让思路沉淀下来。\n\n> [!TIP]\n> 先从最小的一步开始：关掉通知，翻开书，读完一页。\n\n## 延伸阅读\n\n- 《深度工作》：如何在分心的世界里专注\n- 《被讨厌的勇气》：课题分离与自我接纳\n- 《当下的力量》：回到此刻\n\n读书不必贪多。**一本书读透，胜过十本书翻过**。把摘录和感想写下来，几个月后再回看，会发现自己悄悄走了很远。\n\n## 写给自己的话\n\n不用急着和别人比较进度。每个人的节奏不同，**稳定地向前**，比一时冲刺更重要。\n\n" + "愿你在忙碌的日子里，也能留一点时间给自己，读一页书，喝一杯茶，慢慢地把心放回原处。\n\n".repeat(6))

    private fun save(b: android.graphics.Bitmap, f: File) = f.outputStream().use { b.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }

    @Test fun renderSamples() {
        val ctx = ApplicationProvider.getApplicationContext<Application>()
        Library.init(ctx)
        val out = File("/workspace/apk-project/ShiYe/screenshots/v1.1").apply { mkdirs() }
        val xo = File(out, "xhs").apply { mkdirs() }; val lo = File(out, "long").apply { mkdirs() }
        val doc = Doc("a".repeat(16), "周末读书笔记：慢下来的力量", DocType.MD, "doc.md", false, 0, 0, 0, false, "t", "真正的效率，来自于对注意力的尊重。", "h")
        val p = runBlocking { Exporter.prepare(doc, long, false) }
        val typo = Typo(1.75f, 0.6f)
        XhsStyles.all.forEach { st ->
            for (r in 0..1) {
                val rn = if (r == 0) "3x4" else "9x16"
                val c = Exporter.renderCards(p, st, r, doc.title, typo, File(ctx.cacheDir, "c_${st.id}_$r"))
                assertEquals(Exporter.xhsHeight(r), c.h); assertEquals(1080, c.w)
                assertTrue("pages>=3 for ${st.id}", c.files.size >= 3)
                c.files.take(3).forEachIndexed { k, f -> f.copyTo(File(xo, "${st.id}_${rn}_${if (k == 0) "cover" else "body$k"}.png"), true) }
                if (st.id == "mag") println("PAGES ${st.id} $rn = ${c.files.size}")
            }
        }
        ExStyles.all.forEachIndexed { i, st ->
            val l = Exporter.renderLong(p, st, typo, File(ctx.cacheDir, "l$i.png"))
            assertNotNull(l.file); assertEquals(1080, l.width)
            l.file!!.copyTo(File(lo, "long_${i}_${st.name}.png"), true)
        }
        // 紧凑段距对比：同一风格 0.2em 与 1.0em
        val a = Exporter.renderCards(p, XhsStyles.byId("mag"), 0, doc.title, Typo(1.75f, 0.2f), File(ctx.cacheDir, "sp_a"))
        val b = Exporter.renderCards(p, XhsStyles.byId("mag"), 0, doc.title, Typo(1.75f, 1.0f), File(ctx.cacheDir, "sp_b"))
        assertTrue("tighter paragraph spacing must not need more pages", a.files.size <= b.files.size)
        a.files[1].copyTo(File(xo, "spacing_0.2em.png"), true); b.files[1].copyTo(File(xo, "spacing_1.0em.png"), true)
    }
}
