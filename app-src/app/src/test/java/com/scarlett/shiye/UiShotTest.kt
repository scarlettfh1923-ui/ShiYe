package com.scarlett.shiye

import android.app.Application
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.onRoot
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
    private val out = File("/workspace/apk-project/ShiYe/screenshots/v1.1").apply { mkdirs() }

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

    @Test fun screens() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        Library.init(app)
        val md = ShotTest().sample
        val d1 = Importer.fromText(md, null, "分享 · 微信")
        Importer.fromText("# 产品周报\n\n本周完成了三件事：\n\n- 上线新版首页\n- 修复 12 个问题\n- 完成用户访谈\n", null, "文件")
        Importer.fromText("# 会议纪要\n\n> 结论先行\n\n| 事项 | 负责人 |\n|--|--|\n| 排期 | 小林 |\n", null, "粘贴")
        Importer.fromText("<html><head><title>网页剪藏：设计灵感</title></head><body><h1>设计灵感</h1><p>一些好看的页面。</p></body></html>", null, "分享 · Safari", htmlHint = true)
        Library.mutate(d1.id) { it.copy(fav = true) }
        val vm = AppVm(app)
        rule.setContent { ShiyeRoot(vm) }
        vm.prefs.setOnboarded()
        shot("home_after")
        vm.openDoc(d1.id); shot("reader_sharebar")
        rule.onNodeWithContentDescription("更多").performClick(); shot("reader_more_sheet", 1000)
        rule.onNodeWithText("排版（字号 · 行距 · 段间距）").performClick(); shot("typography_panel", 1000)
        vm.pop(); vm.push(Route.Edit(d1.id)); shot("editor", 1000)
        vm.pop(); vm.push(Route.Xhs(d1.id)); shot("xhs_screen", 5000); rule.onNodeWithText("版式").performScrollTo(); shot("xhs_picker", 1500)
        vm.pop(); vm.push(Route.Long(d1.id)); shot("long_screen", 5000)
    }
}
