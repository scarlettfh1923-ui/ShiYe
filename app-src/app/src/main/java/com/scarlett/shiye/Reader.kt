package com.scarlett.shiye

import android.annotation.SuppressLint
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.ViewGroup
import android.webkit.*
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.webkit.WebViewAssetLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream

@Composable
fun ReaderScreen(vm: AppVm, id: String) {
    val docs by Library.docs.collectAsState()
    val doc = docs.firstOrNull { it.id == id }
    if (doc == null) { LaunchedEffect(Unit) { vm.pop() }; return }
    if (doc.type == DocType.HTML) HtmlReader(vm, doc) else MdReader(vm, doc)
}

/** 底部分享条：4 个互相独立的入口——小红书 / 长图 / Notion / 文件 */
@Composable
fun ActionDock(visible: Boolean, doc: Doc, vm: AppVm, modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    AnimatedVisibility(visible, modifier, enter = fadeIn() + slideInVertically { it }, exit = fadeOut() + slideOutVertically { it }) {
        Row(Modifier.windowInsetsPadding(WindowInsets.navigationBars).padding(start = 18.dp, end = 18.dp, bottom = 14.dp).fillMaxWidth().glass(RoundedCornerShape(30.dp), floating = true).padding(horizontal = 6.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            DockItem(R.drawable.ic_cards, "小红书", Modifier.weight(1f)) { vm.push(Route.Xhs(doc.id)) }
            DockItem(R.drawable.ic_long_image, "长图", Modifier.weight(1f)) { vm.push(Route.Long(doc.id)) }
            DockItem(R.drawable.ic_notion, if (doc.notionUrl.isNotBlank()) "再同步" else "Notion", Modifier.weight(1f)) { vm.syncNotion(doc) }
            DockItem(R.drawable.ic_file_out, if (doc.type == DocType.HTML) "HTML 文件" else "MD 文件", Modifier.weight(1f)) {
                if (!runCatching { Share.file(ctx, doc) }.getOrDefault(false)) vm.toast("文件不存在，无法分享")
            }
        }
    }
}

@Composable
private fun DockItem(icon: Int, label: String, modifier: Modifier = Modifier, tint: androidx.compose.ui.graphics.Color = P.text, onClick: () -> Unit) {
    Column(modifier.clip(RoundedCornerShape(22.dp)).clickable(onClick = onClick).padding(vertical = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Ic(icon, tint, 22); Text(label, color = P.text2, fontSize = 11.sp, modifier = Modifier.padding(top = 3.dp), maxLines = 1)
    }
}

/** 阅读设置：字号 / 行距 / 段间距（行距与段间距相邻，可一起调）/ 字体 / 主题；改动立即全局生效 */
@Composable
fun TypographySheet(prefs: Prefs, onDismiss: () -> Unit) {
    Sheet(onDismiss) {
        Row(Modifier.padding(start = 6.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("阅读设置", color = P.text, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Text("恢复默认", color = P.accent, fontSize = 13.5.sp, modifier = Modifier.clip(Shapes.chip).clickable { prefs.resetTypography() }.padding(horizontal = 10.dp, vertical = 6.dp))
        }
        // 预览：用当前的行距和段间距排两段
        val st = ReadStyle(prefs.fontScale, prefs.serif, false, null, {}, lineMult = prefs.lineHeight, paraEm = prefs.paraSpacing)
        Column(Modifier.fillMaxWidth().clip(Shapes.box).background(P.field).padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(st.gap)) {
            val ff = if (prefs.serif) androidx.compose.ui.text.font.FontFamily.Serif else androidx.compose.ui.text.font.FontFamily.Default
            Text("把喜欢的文字收进来，慢慢读。好的排版，是让眼睛可以放松的地方。", color = P.text, fontSize = (15f * prefs.fontScale).sp, lineHeight = (15f * prefs.fontScale * prefs.lineHeight).sp, fontFamily = ff)
            Text("行距与段间距可以一起调，直到读起来最舒服。", color = P.text2, fontSize = (15f * prefs.fontScale).sp, lineHeight = (15f * prefs.fontScale * prefs.lineHeight).sp, fontFamily = ff)
        }
        Spacer(Modifier.height(8.dp))
        TypoSlider("字号", "A", "A", prefs.fontScale, 0.8f..1.6f, "%.0f%%".format(prefs.fontScale * 100)) { prefs.saveFontScale(it) }
        TypoSlider("行距", "紧", "松", prefs.lineHeight, 1.2f..2.2f, "%.2f×".format(prefs.lineHeight)) { prefs.saveLineHeight(it) }
        TypoSlider("段间距", "0", "1.5", prefs.paraSpacing, 0f..1.5f, "%.2f em".format(prefs.paraSpacing)) { prefs.saveParaSpacing(it) }
        Spacer(Modifier.height(6.dp))
        Segmented(listOf("无衬线", "衬线"), if (prefs.serif) 1 else 0, { prefs.saveSerif(it == 1) }, Modifier.fillMaxWidth())
        Spacer(Modifier.height(10.dp))
        Segmented(listOf("跟随系统", "浅色", "深色"), prefs.themeMode, { prefs.saveThemeMode(it) }, Modifier.fillMaxWidth())
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth().clip(Shapes.box).clickable { prefs.saveMdNet(!prefs.mdNet) }.padding(6.dp, 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("加载网络图片", color = P.text, fontSize = 15.sp, modifier = Modifier.weight(1f))
            androidx.compose.material3.Switch(prefs.mdNet, { prefs.saveMdNet(it) })
        }
    }
}

@Composable
private fun TypoSlider(label: String, lo: String, hi: String, v: Float, range: ClosedFloatingPointRange<Float>, shown: String, onChange: (Float) -> Unit) {
    Column(Modifier.fillMaxWidth().padding(top = 6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, color = P.text, fontSize = 15.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f).padding(start = 6.dp))
            Text(shown, color = P.accent, fontSize = 13.5.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(end = 6.dp))
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(lo, color = P.text3, fontSize = 12.sp, modifier = Modifier.width(22.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            Slider(v, onChange, valueRange = range, modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                colors = SliderDefaults.colors(thumbColor = P.accent, activeTrackColor = P.accent, inactiveTrackColor = P.field))
            Text(hi, color = P.text3, fontSize = 12.sp, modifier = Modifier.width(22.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        }
    }
}

@Composable
fun DocMenuSheet(doc: Doc, vm: AppVm, onDismiss: () -> Unit, onRename: () -> Unit, onDelete: () -> Unit, extra: (@Composable ColumnScope.() -> Unit)? = null, onAa: (() -> Unit)? = null, onToc: (() -> Unit)? = null) {
    val ctx = LocalContext.current
    Sheet(onDismiss) {
        Text(doc.title, color = P.text, fontSize = 18.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(start = 6.dp, bottom = 8.dp, top = 2.dp))
        val scope = rememberCoroutineScope()
        SheetAction(if (doc.fav) R.drawable.ic_bookmark_fill else R.drawable.ic_bookmark, if (doc.fav) "取消收藏" else "收藏") { onDismiss(); Library.mutate(doc.id) { it.copy(fav = !it.fav) } }
        if (onAa != null) SheetAction(R.drawable.ic_text_size, "排版（字号 · 行距 · 段间距）") { onDismiss(); onAa() }
        if (onToc != null) SheetAction(R.drawable.ic_toc, "目录") { onDismiss(); onToc() }
        SheetAction(R.drawable.ic_edit, "编辑原文") { onDismiss(); vm.push(Route.Edit(doc.id)) }
        extra?.invoke(this)
        SheetAction(R.drawable.ic_copy, "复制全文") { onDismiss(); scope.launch { val t = withContext(Dispatchers.IO) { Library.readText(doc) }; Share.copyText(ctx, t); vm.toast("已复制全文") } }
        SheetAction(R.drawable.ic_share, "分享文字") { onDismiss(); scope.launch { val t = withContext(Dispatchers.IO) { Library.readText(doc) }; Share.shareText(ctx, doc.title, t.take(200_000)) } }
        if (doc.notionUrl.isNotBlank()) SheetAction(R.drawable.ic_link, "在 Notion 打开") { onDismiss(); Share.openUrl(ctx, doc.notionUrl) }
        SheetAction(R.drawable.ic_text_size, "重命名") { onDismiss(); onRename() }
        SheetAction(R.drawable.ic_trash, "删除", danger = true) { onDismiss(); onDelete() }
    }
}

@Composable
private fun LinkGuard(url: String?, onClose: () -> Unit) {
    val ctx = LocalContext.current
    if (url != null) Confirm("用浏览器打开链接？", url.take(300), "打开", onOk = { Share.openUrl(ctx, url) }, onDismiss = onClose)
}

private fun relDate(t: Long) = java.text.SimpleDateFormat("yyyy年M月d日", java.util.Locale.CHINA).format(java.util.Date(t))

@Composable
fun MdReader(vm: AppVm, doc: Doc) {
    val prefs = vm.prefs
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val text by produceState<String?>(null, doc.id) { value = withContext(Dispatchers.IO) { Library.readText(doc) } }
    val blocks = remember(text) { text?.let { Markdown.parse(it) }.orEmpty().let { b -> val f = b.firstOrNull(); if (f is MdBlock.Heading && f.level == 1 && f.segs.plain().trim() == doc.title.trim()) b.drop(1) else b } }
    val tags = remember(text) { text?.let { Markdown.tags(Markdown.stripFrontMatter(it).second) }.orEmpty() }
    val state = rememberLazyListState()
    val backdrop = remember { Backdrop() }
    var dock by remember { mutableStateOf(true) }
    var showToc by remember { mutableStateOf(false) }; var showAa by remember { mutableStateOf(false) }; var showMenu by remember { mutableStateOf(false) }
    var rename by remember { mutableStateOf(false) }; var del by remember { mutableStateOf(false) }
    var link by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(state) {
        var last = 0
        snapshotFlow { state.firstVisibleItemIndex * 100000 + state.firstVisibleItemScrollOffset }.collect { v ->
            if (v - last > 40) dock = false else if (last - v > 40) dock = true
            if (v < 100000 / 4 && state.firstVisibleItemIndex == 0) dock = true
            last = v
        }
    }
    val progress by remember { derivedStateOf { val n = state.layoutInfo.totalItemsCount; if (n <= 1) 0f else (state.firstVisibleItemIndex.toFloat() / (n - 1)).coerceIn(0f, 1f) } }
    val style = ReadStyle(prefs.fontScale, prefs.serif, prefs.mdNet, Library.mainFile(doc).parentFile, onLink = { u ->
        if (u.startsWith("https://") || u.startsWith("http://")) link = u else vm.toast("已拦截不支持的链接")
    }, lineMult = prefs.lineHeight, paraEm = prefs.paraSpacing)
    val titleShown by remember { derivedStateOf { state.firstVisibleItemIndex > 0 } }
    val headings = remember(blocks) { blocks.mapIndexedNotNull { i, b -> if (b is MdBlock.Heading && b.level <= 3) i to b else null } }

    CompositionLocalProvider(LocalBackdrop provides backdrop) {
        Box(Modifier.fillMaxSize()) {
            if (text == null) Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { androidx.compose.material3.CircularProgressIndicator(color = P.accent, strokeWidth = 2.5.dp, modifier = Modifier.size(28.dp)) }
            else SelectionContainer(Modifier.glassSource(backdrop)) {
                LazyColumn(state = state, modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(style.gap),
                    contentPadding = PaddingValues(start = 22.dp, end = 22.dp, top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 72.dp, bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 130.dp)) {
                    item(key = "title") {
                        Column(Modifier.padding(bottom = 6.dp)) {
                            Text(doc.title, color = P.text, fontSize = (28 * prefs.fontScale.coerceAtMost(1.25f)).sp, lineHeight = (37 * prefs.fontScale.coerceAtMost(1.25f)).sp, fontWeight = FontWeight.ExtraBold,
                                fontFamily = if (prefs.serif) androidx.compose.ui.text.font.FontFamily.Serif else androidx.compose.ui.text.font.FontFamily.Default)
                            Text("${doc.source} · ${relDate(doc.createdAt)} · 约 ${readMinutes(doc.size)} 分钟", color = P.text3, fontSize = 13.sp, modifier = Modifier.padding(top = 10.dp))
                            if (tags.isNotEmpty()) Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                tags.take(5).forEach { Text("# $it", color = P.accent, fontSize = 12.sp, modifier = Modifier.clip(Shapes.chip).background(P.accentSoft).padding(horizontal = 10.dp, vertical = 4.dp)) }
                            }
                        }
                    }
                    items(blocks.size) { i -> Block(blocks[i], style) }
                    item(key = "end") { Text("— 读完了 —", color = P.text3, fontSize = 12.5.sp, modifier = Modifier.fillMaxWidth().padding(top = 24.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center) }
                }
            }
            Column(Modifier.align(Alignment.TopCenter)) {
                TopBar(doc.title, { vm.pop() }, titleAlpha = if (titleShown) 1f else 0f) {
                    CircleBtn(R.drawable.ic_share, "分享", { dock = true }, glass = true, size = 40); Spacer(Modifier.width(8.dp))
                    CircleBtn(R.drawable.ic_more, "更多", { showMenu = true }, glass = true, size = 40)
                }
                Box(Modifier.fillMaxWidth(progress).height(2.dp).background(P.accent))
            }
            ActionDock(dock, doc, vm, Modifier.align(Alignment.BottomCenter))
        }
    }
    if (showToc) Sheet({ showToc = false }) {
        Text("目录", color = P.text, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 6.dp, bottom = 8.dp))
        if (headings.isEmpty()) Text("这篇文档没有标题层级。", color = P.text3, fontSize = 14.sp, modifier = Modifier.padding(6.dp, 12.dp))
        Column(Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState())) {
            headings.forEach { (i, h) ->
                Text(h.segs.plain(), color = if (h.level == 1) P.text else P.text2, fontSize = if (h.level == 1) 16.sp else 15.sp, fontWeight = if (h.level == 1) FontWeight.SemiBold else FontWeight.Normal, maxLines = 2, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth().clip(Shapes.box).clickable { showToc = false; scope.launch { state.animateScrollToItem(i + 1) } }.padding(start = (6 + (h.level - 1) * 16).dp, top = 12.dp, bottom = 12.dp, end = 6.dp))
            }
        }
    }
    if (showAa) TypographySheet(prefs) { showAa = false }
    if (showMenu) DocMenuSheet(doc, vm, { showMenu = false }, { rename = true }, { del = true }, onAa = { showAa = true }, onToc = { showToc = true })
    if (rename) TextPrompt("重命名", doc.title, "保存", { v -> if (v.isNotBlank()) Library.mutate(doc.id) { it.copy(title = v.trim().take(120)) } }, { rename = false })
    if (del) Confirm("删除这篇文档？", "「${doc.title}」会从拾页移除。", "删除", danger = true, onOk = { vm.pop(); Library.delete(listOf(doc.id)); vm.toast("已删除") }, onDismiss = { del = false })
    LinkGuard(link) { link = null }
}

// ---------------- HTML 阅读：加固的 WebView ----------------
private const val ASSET_HOST = "appassets.androidplatform.net"

private fun mimeFor(name: String): String {
    val ext = name.substringAfterLast('.', "").lowercase()
    return when (ext) {
        "html", "htm" -> "text/html"; "js", "mjs" -> "text/javascript"; "css" -> "text/css"; "json" -> "application/json"; "svg" -> "image/svg+xml"
        "woff2" -> "font/woff2"; "woff" -> "font/woff"; "ttf" -> "font/ttf"; "otf" -> "font/otf"; "webp" -> "image/webp"; "txt", "md" -> "text/plain"
        else -> MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext) ?: "application/octet-stream"
    }
}

/** 只放行当前文档目录下的文件；规范化路径后再比较，挡住 ../ 越界 */
private fun docHandler(doc: Doc) = WebViewAssetLoader.PathHandler { path ->
    runCatching {
        val dir = Library.dirOf(doc).canonicalFile
        val rel = path.removePrefix(doc.id + "/")
        if (!path.startsWith(doc.id + "/")) return@runCatching null
        val f = File(dir, java.net.URLDecoder.decode(rel.replace("+", "%2B"), "UTF-8")).canonicalFile
        if (!f.path.startsWith(dir.path + File.separator) || !f.isFile || f.name.startsWith(".thumb") || f.path.startsWith(File(dir, ".backup").path + File.separator)) return@runCatching null
        WebResourceResponse(mimeFor(f.name), if (mimeFor(f.name).startsWith("text/") || f.extension == "js" || f.extension == "json" || f.extension == "svg") "utf-8" else null, FileInputStream(f))
    }.getOrNull()
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun HtmlReader(vm: AppVm, doc: Doc) {
    val prefs = vm.prefs
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var dock by remember { mutableStateOf(true) }
    var link by remember { mutableStateOf<String?>(null) }
    var askScript by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }; var rename by remember { mutableStateOf(false) }; var del by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(true) }
    var landscape by remember { mutableStateOf(false) }
    val activity = ctx.findActivity()
    DisposableEffect(landscape) {
        activity?.requestedOrientation = if (landscape) ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE else ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        onDispose { activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED }
    }
    val netOn = prefs.htmlNet
    val loader = remember(doc.id) { WebViewAssetLoader.Builder().setDomain(ASSET_HOST).addPathHandler("/lib/", docHandler(doc)).build() }
    val top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 60.dp
    val url = "https://$ASSET_HOST/lib/${doc.id}/${doc.main.split('/').joinToString("/") { java.net.URLEncoder.encode(it, "UTF-8").replace("+", "%20") }}"

    Box(Modifier.fillMaxSize().background(androidx.compose.ui.graphics.Color.White)) {
        key(doc.script, doc.desktop, netOn) {
            AndroidView(modifier = Modifier.fillMaxSize().padding(top = top), factory = { c ->
                WebView(c).apply {
                    layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                    setBackgroundColor(android.graphics.Color.WHITE)
                    with(settings) {
                        javaScriptEnabled = doc.script
                        domStorageEnabled = doc.script
                        allowFileAccess = false; allowContentAccess = false
                        @Suppress("DEPRECATION") run { allowFileAccessFromFileURLs = false; allowUniversalAccessFromFileURLs = false; savePassword = false; saveFormData = false }
                        mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                        blockNetworkLoads = !netOn
                        setGeolocationEnabled(false)
                        javaScriptCanOpenWindowsAutomatically = false
                        setSupportMultipleWindows(false)
                        builtInZoomControls = true; displayZoomControls = false
                        useWideViewPort = true; loadWithOverviewMode = true
                        textZoom = (100 * prefs.fontScale).toInt().coerceIn(80, 160)
                        if (doc.desktop) userAgentString = userAgentString.replace("Mobile", "").replace("Android", "X11; Linux x86_64")
                    }
                    webViewClient = object : WebViewClient() {
                        override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? {
                            val u = request.url
                            if (u.host == ASSET_HOST) return loader.shouldInterceptRequest(u) ?: WebResourceResponse("text/plain", "utf-8", 404, "Not Found", emptyMap(), java.io.ByteArrayInputStream(ByteArray(0)))
                            if (u.scheme != "https") return WebResourceResponse("text/plain", "utf-8", 403, "Blocked", emptyMap(), java.io.ByteArrayInputStream(ByteArray(0)))   // 不放行明文与其它协议
                            return null
                        }
                        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                            val u = request.url
                            if (u.host == ASSET_HOST && (u.path ?: "").startsWith("/lib/${doc.id}/")) return false
                            val s = u.toString()
                            if (s.startsWith("https://") || s.startsWith("http://")) link = s else vm.toast("已拦截不支持的链接")
                            return true
                        }
                        override fun onPageFinished(view: WebView, u: String?) {
                            loading = false
                            if (!Library.thumbFile(doc).isFile) scope.launch {
                                delay(700)
                                runCatching {
                                    val w = view.width; val h = (w * 0.85f).toInt().coerceAtMost(view.height)
                                    if (w > 0 && h > 0) {
                                        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                                        view.draw(Canvas(bmp))
                                        val sm = Bitmap.createScaledBitmap(bmp, 540, (540f * h / w).toInt().coerceAtLeast(1), true); bmp.recycle()
                                        withContext(Dispatchers.IO) { Library.thumbFile(doc).outputStream().use { o -> sm.compress(Bitmap.CompressFormat.PNG, 90, o) } }
                                    }
                                }
                            }
                        }
                        override fun onRenderProcessGone(view: WebView, detail: RenderProcessGoneDetail): Boolean { vm.toast("网页渲染中断了，已关闭"); vm.pop(); return true }
                    }
                    setOnScrollChangeListener { _, _, y, _, oy -> if (y - oy > 12) dock = false else if (oy - y > 12) dock = true }
                    loadUrl(url)
                }
            }, onRelease = { it.stopLoading(); it.destroy() })
        }
        if (loading) androidx.compose.material3.LinearProgressIndicator(Modifier.fillMaxWidth().padding(top = top), color = P.accent, trackColor = androidx.compose.ui.graphics.Color.Transparent)
        TopBar(doc.title, { vm.pop() }, Modifier.align(Alignment.TopCenter)) {
            CircleBtn(R.drawable.ic_share, "分享", { dock = true }, glass = true, size = 40); Spacer(Modifier.width(8.dp))
            CircleBtn(R.drawable.ic_more, "更多", { showMenu = true }, glass = true, size = 40)
        }
        if (doc.script) Row(Modifier.align(Alignment.TopCenter).padding(top = top + 8.dp).clip(Shapes.chip).background(P.accent).padding(horizontal = 12.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("脚本已开启", color = P.onAccent, fontSize = 12.sp)
        }
        ActionDock(dock, doc, vm, Modifier.align(Alignment.BottomCenter))
    }
    if (askScript) Confirm("开启这篇网页的脚本？", "脚本能让交互式页面正常工作，但网页里的代码会在这里运行。只对你信任的来源开启，之后可随时在顶栏关闭。", "开启", onOk = { Library.mutate(doc.id) { it.copy(script = true) }; loading = true }, onDismiss = { askScript = false })
    if (showMenu) DocMenuSheet(doc, vm, { showMenu = false }, { rename = true }, { del = true }, extra = {
        SheetAction(R.drawable.ic_code, if (doc.script) "关闭脚本" else "开启脚本") { showMenu = false; if (doc.script) Library.mutate(doc.id) { it.copy(script = false) } else askScript = true }
        SheetAction(R.drawable.ic_desktop, if (doc.desktop) "手机版网页" else "电脑版网页") { showMenu = false; Library.mutate(doc.id) { it.copy(desktop = !it.desktop) } }
        SheetAction(R.drawable.ic_rotate, if (landscape) "恢复竖屏" else "横屏阅读") { showMenu = false; landscape = !landscape } })
    if (rename) TextPrompt("重命名", doc.title, "保存", { v -> if (v.isNotBlank()) Library.mutate(doc.id) { it.copy(title = v.trim().take(120)) } }, { rename = false })
    if (del) Confirm("删除这篇文档？", "「${doc.title}」会从拾页移除。", "删除", danger = true, onOk = { vm.pop(); Library.delete(listOf(doc.id)); vm.toast("已删除") }, onDismiss = { del = false })
    LinkGuard(link) { link = null }
}
