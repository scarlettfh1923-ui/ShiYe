package com.scarlett.shiye

import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.view.textclassifier.TextClassifier
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/**
 * 剪贴板链接检测。
 * - 只在窗口拿到焦点后读（Android 10+ 后台读不到）；先看 ClipDescription（不会触发系统「已读取剪贴板」提示），
 *   Android 12+ 若系统分类已判定不含网址就不读正文；同一条剪贴（时间戳相同）只检查一次。
 * - 只读 item.text，不调用 coerceToText（那会去打开 content:// 内容，可能卡主线程）。
 * - 同一个链接只提示一次（记在 Prefs）；已经在书库里的链接不提示。
 */
object ClipboardLink {
    private var lastStamp = -1L

    fun peek(ctx: Context, prefs: Prefs): String? = runCatching {
        val cm = ctx.getSystemService(ClipboardManager::class.java) ?: return null
        val desc = cm.primaryClipDescription ?: return null
        if (!desc.hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN) && !desc.hasMimeType(ClipDescription.MIMETYPE_TEXT_HTML) && !desc.hasMimeType(ClipDescription.MIMETYPE_TEXT_URILIST)) return null
        val stamp = desc.timestamp
        if (stamp != 0L && (stamp == lastStamp || stamp == prefs.clipStamp)) return null
        lastStamp = stamp; prefs.saveClipStamp(stamp)
        if (Build.VERSION.SDK_INT >= 31 && desc.classificationStatus == ClipDescription.CLASSIFICATION_COMPLETE &&
            runCatching { desc.getConfidenceScore(TextClassifier.TYPE_URL) }.getOrDefault(1f) <= 0f) return null
        val item = cm.primaryClip?.takeIf { it.itemCount > 0 }?.getItemAt(0) ?: return null
        val text = item.text?.toString() ?: item.uri?.takeIf { it.scheme == "https" || it.scheme == "http" }?.toString() ?: return null
        val url = ClipUrl.find(text) ?: return null
        if (prefs.wasPrompted(url) || Library.findByUrl(ClipUrl.classify(url).url) != null) return null
        prefs.markPrompted(url); url
    }.getOrNull()

    /** 用户主动点「粘贴」时读剪贴板文字 */
    fun readText(ctx: Context): String? = runCatching {
        val cm = ctx.getSystemService(ClipboardManager::class.java) ?: return null
        cm.primaryClip?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.text?.toString()
    }.getOrNull()
}

/** 首页：回到前台（窗口拿到焦点）时检查剪贴板，有新链接就放进 vm.clipPrompt */
@Composable
fun ClipboardWatcher(vm: AppVm, enabled: Boolean) {
    val ctx = LocalContext.current
    val focused = LocalWindowInfo.current.isWindowFocused
    LaunchedEffect(focused, enabled) {
        if (!focused || !enabled) return@LaunchedEffect
        delay(400)               // 等焦点稳定后再读
        ClipboardLink.peek(ctx, vm.prefs)?.let { vm.clipPrompt = it }
    }
}

fun hostOf(url: String): String = runCatching { java.net.URI(url).host?.removePrefix("www.")?.removePrefix("m.") }.getOrNull().orEmpty()

/** 「检测到链接，抓取文章？」提示条：玻璃小卡片，12 秒后自动收起 */
@Composable
fun ClipPromptBar(url: String?, onFetch: (String) -> Unit, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    var last by remember { mutableStateOf(url) }
    if (url != null) last = url
    LaunchedEffect(url) { if (url != null) { delay(12_000); onDismiss() } }
    AnimatedVisibility(url != null, modifier, enter = fadeIn() + slideInVertically { it / 2 }, exit = fadeOut() + slideOutVertically { it / 2 }) {
        val u = last ?: return@AnimatedVisibility
        val pal = P
        val site = ClipUrl.classify(u).site
        Row(Modifier.padding(horizontal = 16.dp).widthIn(max = 520.dp).fillMaxWidth().glass(Squircle(18.dp), floating = true).padding(start = 12.dp, end = 6.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(36.dp).clip(CircleShape).background(pal.accentSoft), contentAlignment = Alignment.Center) { Ic(R.drawable.ic_link, pal.accent, 18) }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("检测到链接，抓取文章？", color = pal.text, fontSize = 14.sp, lineHeight = 19.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                Text("${site.label} · ${hostOf(u)}", color = pal.text3, fontSize = 12.sp, lineHeight = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.width(8.dp))
            Box(Modifier.height(32.dp).clip(Shapes.chip).background(pal.accent).clickable { onFetch(u) }.padding(horizontal = 14.dp), contentAlignment = Alignment.Center) {
                Text("抓取", color = pal.onAccent, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
            Box(Modifier.size(36.dp).clip(CircleShape).clickable(onClick = onDismiss).semantics { contentDescription = "不用了" }, contentAlignment = Alignment.Center) { Ic(R.drawable.ic_close, pal.text3, 18) }
        }
    }
}

/** 抓取进度卡片：站点 + 阶段 + 细进度条 + 取消；不遮挡页面，可以继续浏览 */
@Composable
fun ClipProgressCard(ui: AppVm.ClipUi?, onCancel: () -> Unit, modifier: Modifier = Modifier) {
    var last by remember { mutableStateOf(ui) }
    if (ui != null) last = ui
    AnimatedVisibility(ui != null, modifier, enter = fadeIn() + slideInVertically { it / 2 }, exit = fadeOut() + slideOutVertically { it / 2 }) {
        val u = last ?: return@AnimatedVisibility
        val pal = P
        Column(Modifier.padding(horizontal = 16.dp).widthIn(max = 520.dp).fillMaxWidth().glass(Squircle(18.dp), floating = true).padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Ic(R.drawable.ic_link, pal.accent, 18); Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text("正在抓取 · ${u.site}", color = pal.text, fontSize = 14.sp, lineHeight = 19.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                    Text(u.stage, color = pal.text2, fontSize = 12.sp, lineHeight = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Box(Modifier.height(32.dp).clip(Shapes.chip).clickable(onClick = onCancel).padding(horizontal = 12.dp), contentAlignment = Alignment.Center) {
                    Text("取消", color = pal.text2, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                }
            }
            Spacer(Modifier.height(10.dp))
            LinearProgressIndicator(progress = { u.progress.coerceIn(0f, 1f) }, modifier = Modifier.padding(end = 8.dp).fillMaxWidth().height(3.dp).clip(Shapes.chip),
                color = pal.accent, trackColor = pal.line, strokeCap = StrokeCap.Round, gapSize = 0.dp, drawStopIndicator = {})
        }
    }
}

/** 「+」面板里的粘贴链接输入框 */
@Composable
fun PasteLinkField(onGo: (String) -> Unit, onInvalid: () -> Unit, initial: String = "") {
    val pal = P
    val ctx = LocalContext.current
    var v by remember { mutableStateOf(initial) }
    fun go() { val u = ClipUrl.find(v) ?: ClipUrl.normalize(v.trim()); if (u == null) onInvalid() else onGo(u) }
    Column(Modifier.padding(horizontal = 8.dp, vertical = 8.dp)) {
        Text("粘贴网页链接", color = pal.text2, fontSize = 13.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(start = 4.dp, bottom = 8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Row(Modifier.weight(1f).height(48.dp).clip(Shapes.chip).background(pal.field).padding(start = 16.dp, end = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Ic(R.drawable.ic_link, pal.text3, 18); Spacer(Modifier.width(8.dp))
                Box(Modifier.weight(1f)) {
                    if (v.isEmpty()) Text("https://…", color = pal.text3, fontSize = 15.sp, maxLines = 1)
                    BasicTextField(v, { v = it }, singleLine = true, textStyle = TextStyle(color = pal.text, fontSize = 15.sp), cursorBrush = SolidColor(pal.accent),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Go), keyboardActions = KeyboardActions(onGo = { go() }),
                        modifier = Modifier.fillMaxWidth().semantics { contentDescription = "网页链接" })
                }
                if (v.isEmpty()) Box(Modifier.height(32.dp).clip(Shapes.chip).background(pal.surface).clickable { ClipboardLink.readText(ctx)?.let { v = it.trim().take(3000) } }.padding(horizontal = 12.dp), contentAlignment = Alignment.Center) {
                    Text("粘贴", color = pal.accent, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                } else Box(Modifier.size(32.dp).clip(CircleShape).clickable { v = "" }, contentAlignment = Alignment.Center) { Ic(R.drawable.ic_close, pal.text3, 16) }
            }
            Spacer(Modifier.width(8.dp))
            Box(Modifier.height(48.dp).clip(Shapes.chip).background(if (v.isBlank()) pal.field else pal.accent).clickable(enabled = v.isNotBlank()) { go() }.padding(horizontal = 18.dp), contentAlignment = Alignment.Center) {
                Text("抓取", color = if (v.isBlank()) pal.text3 else pal.onAccent, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            }
        }
        Text("支持公众号、知乎、豆瓣和大多数网页；正文和图片会存到手机里，离线也能读。", color = pal.text3, fontSize = 12.sp, lineHeight = 17.sp, modifier = Modifier.padding(start = 4.dp, top = 8.dp))
    }
}
