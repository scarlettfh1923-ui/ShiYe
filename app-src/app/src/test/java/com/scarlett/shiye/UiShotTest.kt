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
    private val out = File("/workspace/apk-project/ShiYe/screenshots/v2.0").apply { mkdirs() }

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
        val d1 = Importer.fromText(ShotTest().sample, null, "分享 · 微信")
        Importer.fromText("# 产品周报\n\n本周完成了三件事：\n\n- 上线新版首页\n- 修复 12 个问题\n- 完成用户访谈\n", null, "文件")
        Importer.fromText("# 会议纪要\n\n> 结论先行\n\n| 事项 | 负责人 |\n|--|--|\n| 排期 | 小林 |\n", null, "粘贴")
        Importer.fromText("<html><head><title>网页剪藏：设计灵感</title></head><body><h1>设计灵感</h1><p>一些好看的页面。</p></body></html>", null, "分享 · Safari", htmlHint = true)
        Library.mutate(d1.id) { it.copy(fav = true, progress = 0.42f) }
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
    @Test fun editor() { val (vm, id) = setup(); vm.push(Route.Edit(id)); shot("07_editor", 1000) }
}
