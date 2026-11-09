package com.scarlett.shiye

import android.app.Application
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.click
import androidx.compose.ui.test.center
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.test.core.app.ApplicationProvider
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.io.FileOutputStream

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w393dp-h852dp-xxhdpi")
class UiShotTest {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()
    private val out = File("/workspace/apk-project/ShiYe/screenshots/v2.2.0").apply { mkdirs() }

    private fun shot(name: String, wait: Long = 0) {
        if (wait > 0) { repeat(10) { Thread.sleep(wait / 10); rule.mainClock.advanceTimeBy(200); rule.waitForIdle() } }
        rule.mainClock.advanceTimeBy(600)
        rule.waitForIdle()
        val bmp = android.graphics.Bitmap.createBitmap(1179, 2556, android.graphics.Bitmap.Config.ARGB_8888)
        rule.runOnIdle {
            val c = android.graphics.Canvas(bmp)
            c.drawColor(0xFFF5F7FA.toInt())
            val main = rule.activity.window.decorView
            val k = bmp.width / main.width.toFloat()
            c.scale(k, k)
            main.draw(c)
            // 底部面板等对话框窗口：按创建顺序叠在上面
            runCatching {
                val g = Class.forName("android.view.WindowManagerGlobal").getMethod("getInstance").invoke(null)
                @Suppress("UNCHECKED_CAST") val names = g.javaClass.getMethod("getViewRootNames").invoke(g) as Array<String>
                for (n in names) {
                    val v = g.javaClass.getMethod("getRootView", String::class.java).invoke(g, n) as? android.view.View ?: continue
                    if (v === main || v.width == 0) continue
                    c.save(); val lp = v.layoutParams as? android.view.WindowManager.LayoutParams
                    c.translate(0f, (main.height - v.height).toFloat().coerceAtLeast(0f) * (if (lp != null && lp.gravity and android.view.Gravity.BOTTOM == android.view.Gravity.BOTTOM) 1f else 0f)); v.draw(c); c.restore()
                }
            }
        }
        FileOutputStream(File(out, "$name.png")).use { bmp.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
    }

    private fun setup(): Pair<AppVm, String> {
        val app = ApplicationProvider.getApplicationContext<Application>()
        Library.init(app)
        Importer.fromText("# 产品周报\n\n本周完成了三件事：\n\n- 上线新版首页\n- 修复 12 个问题\n- 完成用户访谈\n", null, "文件"); Thread.sleep(8)
        Importer.fromText("# 会议纪要\n\n> 结论先行\n\n| 事项 | 负责人 |\n|--|--|\n| 排期 | 小林 |\n", null, "粘贴"); Thread.sleep(8)
        Importer.fromText("<html><head><title>网页剪藏：设计灵感</title></head><body><h1>设计灵感</h1><p>一些好看的页面。</p></body></html>", null, "分享 · Safari", htmlHint = true); Thread.sleep(8)
        val d4 = Importer.fromText(CORO, null, "GitHub"); Thread.sleep(8)
        val d3 = Importer.fromText(TRAVEL, null, "分享 · Safari"); Thread.sleep(8)
        val d2 = Importer.fromText(ShotTest().sample, null, "备忘录"); Thread.sleep(8)
        val d1 = Importer.fromText(FILM, null, "分享 · 微信")
        Library.mutate(d1.id) { it.copy(fav = true, progress = 0.18f) }
        Library.mutate(d2.id) { it.copy(fav = true, progress = 0.42f) }
        Library.mutate(d3.id) { it.copy(progress = 0.07f) }
        Library.mutate(d4.id) { it.copy(fav = true) }
        val vm = AppVm(app)
        rule.setContent { ShiyeRoot(vm) }
        vm.prefs.setOnboarded()
        return vm to d1.id
    }
    private fun tapRoot() { rule.onRoot().performTouchInput { click(center) }; rule.mainClock.advanceTimeBy(600); rule.waitForIdle() }

    @Test fun home() {
        val (vm, _) = setup()
        shot("01_home")
        vm.tab = Tab.Library; shot("08_library")
        vm.tab = Tab.Me; shot("09_me")
    }
    @Test fun readerImmersive() { val (vm, id) = setup(); vm.openDoc(id); shot("02_reader_immersive", 1000) }
    @Test fun readerHeader() { val (vm, id) = setup(); vm.openDoc(id); shot("10_reader_header", 1000) }
    @Test fun readerHeaderDark() { val (vm, id) = setup(); vm.prefs.saveThemeMode(2); vm.openDoc(id); shot("11_reader_header_dark", 1000) }
    @Test fun readerToolbar() { val (vm, id) = setup(); vm.openDoc(id); shot("x", 800); tapRoot(); shot("03_reader_toolbar", 600) }
    @Test fun typography() {
        val (vm, id) = setup(); vm.openDoc(id); shot("x", 800); tapRoot()
        rule.onNodeWithText("排版").performClick(); shot("04_typography", 1000)
    }
    @Test fun moreMenu() {
        val (vm, id) = setup(); vm.openDoc(id); shot("x", 800); tapRoot()
        rule.onNodeWithContentDescription("更多").performClick(); shot("05_more_popover", 800)
    }
    @Test fun export() {
        val (vm, id) = setup(); vm.openDoc(id, export = true); shot("06_export", 6000)
    }
    // ---------- 2.2.0 网页抓取 ----------
    @Test fun clipPrompt() {
        val (vm, _) = setup(); shot("x", 300)
        vm.clipPrompt = "https://zhuanlan.zhihu.com/p/1895498000702363235"
        shot("12_clip_prompt", 800)
    }
    @Test fun clipPasteSheet() {
        val (vm, _) = setup(); shot("x", 300)
        rule.onNodeWithContentDescription("导入").performClick(); shot("13_clip_paste_sheet", 1000)
    }
    @Test fun clipProgress() {
        val (vm, _) = setup(); shot("x", 300)
        vm.clip.value = AppVm.ClipUi("https://mp.weixin.qq.com/s/2fBjEf6kDX0245-tMgS5uA", "微信", "下载图片 7/16", 0.62f)
        shot("14_clip_progress", 800)
    }
    @Test fun clipReader() {
        val (vm, _) = setup()
        val id = clipDoc()
        vm.openDoc(id); shot("15_clip_reader", 1500)
    }
    @Test fun clipLibrary() {
        val (vm, _) = setup()
        clipDoc(); clipDoc("zhihu_article.html", "https://zhuanlan.zhihu.com/p/1895498000702363235"); clipDoc("wechat.html", "https://mp.weixin.qq.com/s/2fBjEf6kDX0245-tMgS5uA")
        vm.tab = Tab.Library; shot("16_clip_library", 600)
    }

    /** 用保存的真实样本走一遍「提取 → Markdown → 图片落盘（这里用生成的示意图代替下载）→ 存书库」 */
    private fun clipDoc(fixture: String = "douban_note.json", url: String = "https://www.douban.com/note/870769226/"): String {
        val t = ClipUrl.classify(url)
        val raw = javaClass.classLoader!!.getResource("clip/$fixture")!!.readText()
        val ex = if (fixture.endsWith(".json")) ClipExtract.doubanJson(raw, t) else ClipExtract.extract(raw, t)
        val conv = HtmlToMd(t.pageUrl, ex.imgMap); var body = conv.convert(ex.content!!)
        val id = Library.newId(); val dir = File(Library.rootDir, id); val img = File(dir, "images").apply { mkdirs() }
        val colors = intArrayOf(0xFFDCE6F6.toInt(), 0xFFEEF0DC.toInt(), 0xFFF3E6D4.toInt())
        conv.images.indices.forEach { i ->
            if (i < 3) {
                val b = android.graphics.Bitmap.createBitmap(1080, 720, android.graphics.Bitmap.Config.ARGB_8888); b.eraseColor(colors[i % 3])
                val c = android.graphics.Canvas(b); val p = android.graphics.Paint().apply { color = 0xFF2F6BFF.toInt(); textSize = 64f; isAntiAlias = true }
                c.drawText("图 ${i + 1} · 本地图片", 80f, 380f, p)
                FileOutputStream(File(img, "%03d.png".format(i + 1))).use { b.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
                body = body.replace("(shiye-img://$i)", "(images/%03d.png)".format(i + 1))
            } else body = body.replace("(shiye-img://$i)", "(${conv.images[i]})")
        }
        val md = ClipMd.compose(ex, t, body, "2026-11-09 16:00")
        return Importer.saveClip(id, dir, md, ex.title, "抓取 · ${t.site.label}", t.url, true).id
    }

    @Test fun editor() { val (vm, id) = setup(); vm.push(Route.Edit(id)); shot("07_editor", 1000) }

    companion object {
        const val FILM = """---
tags: [电影, 上海]
description: 一部是 1947 年张爱玲编剧的都市喜剧，一部是 1954 年的越剧彩色影片。桑弧用两部几乎没有交集的电影，拍下了上海在不同年代的两种神情。
---

# 在《太太万岁》与《梁祝》之间——桑弧的两个上海

一九四七年的上海，霞飞路上的梧桐刚刚抽芽。桑弧坐在摄影棚里，听张爱玲念她写好的台词。

## 一、都市喜剧的轻与痛

《太太万岁》里的陈思珍，是一个会撒谎的好太太。她的每一次圆场，都带着一点疲倦。

## 二、越剧的另一种上海

七年之后，《梁祝》用彩色胶片把越剧搬上银幕。同一座城市，换了一种呼吸。

## 三、两种神情

喜剧里的上海在算计，戏曲里的上海在怀念。桑弧站在两者之间，什么也没有说破。
"""
        const val TRAVEL = """# 京都五日：被雨水洗过的街道

第一天落雨，石板路泛着光。我们撑着伞，从祇园走到鸭川边。

## 清晨的哲学之道

人很少，樱叶落在水面上，慢慢漂向下游。
"""
        const val CORO = """---
tags: [Kotlin, 协程]
---

# Kotlin 协程取消机制的五个坑

取消是协作式的：不检查取消状态的代码，不会因为 cancel 而停下。

## 坑一：吞掉 CancellationException

catch (e: Exception) 会把取消一并吞掉。
"""
    }
}
