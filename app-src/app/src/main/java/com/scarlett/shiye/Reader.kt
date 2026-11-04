package com.scarlett.shiye

import android.annotation.SuppressLint
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.ViewGroup
import android.webkit.*
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
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
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.roundToInt

/** 阅读页：沉浸式正文 + 点按唤出的玻璃栏。二级操作（目录 / 排版 / 编辑 / 导出）都从这里的工具条进入。 */
@Composable
fun ReaderScreen(vm: AppVm, id: String, export: Boolean = false) {
    val docs by Library.docs.collectAsState()
    val doc = docs.firstOrNull { it.id == id }
    if (doc == null) { LaunchedEffect(Unit) { vm.pop() }; return }
    val isMd = doc.type == DocType.MD
    val appPal = P
    val pal = if (isMd) ReadThemes.pal(vm.prefs.readTheme, appPal) else appPal
    var showExport by rememberSaveable { mutableStateOf(export) }
    DisposableEffect(pal.dark, isMd) { if (isMd) vm.barsDark = pal.dark; onDispose { vm.barsDark = null } }
    ShiyeThemeWith(pal) {
        Box(Modifier.fillMaxSize().background(P.bg)) {
            if (isMd) MdReader(vm, doc) { showExport = true } else HtmlReader(vm, doc) { showExport = true }
        }
        if (showExport) ExportSheet(vm, doc) { showExport = false }
    }
}

// ---------------- 共用：顶栏 / 菜单 / 对话框 ----------------

/** 阅读页里一组弹出层的状态，MD 与网页阅读共用 */
class ReaderUi {
    var menu by mutableStateOf(false); var rename by mutableStateOf(false); var del by mutableStateOf(false)
    var info by mutableStateOf(false); var link by mutableStateOf<String?>(null)
}

class MenuItem(val icon: Int, val text: String, val danger: Boolean = false, val onClick: () -> Unit)

@Composable
fun ReaderTopBar(doc: Doc, onBack: () -> Unit, ui: ReaderUi, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().glass(edge = GlassEdge.Bottom).hairline(GlassEdge.Bottom).windowInsetsPadding(WindowInsets.statusBars).height(60.dp).padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        BarIcon(R.drawable.ic_back, "返回", onClick = onBack)
        Text(doc.title, color = P.text, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f).padding(horizontal = 8.dp))
        BarIcon(if (doc.fav) R.drawable.ic_bookmark_fill else R.drawable.ic_bookmark, if (doc.fav) "取消收藏" else "收藏", tint = if (doc.fav) P.accent else P.text) { Library.mutate(doc.id) { it.copy(fav = !it.fav) } }
        BarIcon(R.drawable.ic_more, "更多", onClick = { ui.menu = true })
    }
}

@Composable
private fun BarIcon(icon: Int, desc: String, tint: Color = P.text, onClick: () -> Unit) {
    Box(Modifier.size(48.dp).clip(CircleShape).clickable(role = Role.Button, onClick = onClick).semantics { contentDescription = desc }, contentAlignment = Alignment.Center) { Ic(icon, tint, 24) }
}

/** 工具条上的一项：图标 + 文字；primary 的图标放在主色圆底里 */
@Composable
fun ToolItem(icon: Int, label: String, modifier: Modifier = Modifier, primary: Boolean = false, onClick: () -> Unit) {
    Column(modifier.clip(Squircle(16.dp)).clickable(role = Role.Button, onClick = onClick).padding(vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(44.dp, 32.dp).clip(Shapes.chip).background(if (primary) P.accent else Color.Transparent), contentAlignment = Alignment.Center) { Ic(icon, if (primary) P.onAccent else P.text, 22) }
        Text(label, color = if (primary) P.accent else P.text2, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(top = 2.dp))
    }
}

@Composable
fun BoxScope.ReaderMenu(vm: AppVm, doc: Doc, ui: ReaderUi, extras: List<MenuItem> = emptyList()) {
    val ctx = LocalContext.current
    BackHandler(ui.menu) { ui.menu = false }
    AnimatedVisibility(ui.menu, Modifier.matchParentSize(), enter = fadeIn(), exit = fadeOut()) {
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.22f)).pointerInput(Unit) { detectTapGestures { ui.menu = false } })
    }
    AnimatedVisibility(ui.menu, Modifier.align(Alignment.TopEnd).windowInsetsPadding(WindowInsets.statusBars).padding(top = 56.dp, end = 12.dp),
        enter = fadeIn() + scaleIn(transformOrigin = androidx.compose.ui.graphics.TransformOrigin(1f, 0f), initialScale = 0.9f), exit = fadeOut()) {
        val items = buildList {
            add(MenuItem(R.drawable.ic_copy, "复制全文") { Share.copyText(ctx, Library.readText(doc)); vm.toast("已复制全文") })
            add(MenuItem(R.drawable.ic_edit, "重命名") { ui.rename = true })
            add(MenuItem(R.drawable.ic_info, "文档信息") { ui.info = true })
            if (doc.notionUrl.isNotBlank()) add(MenuItem(R.drawable.ic_notion, "在 Notion 打开") { Share.openUrl(ctx, doc.notionUrl) })
            addAll(extras)
            add(MenuItem(R.drawable.ic_trash, "删除", danger = true) { ui.del = true })
        }
        Column(Modifier.width(214.dp).soft(Squircle(20.dp), 2).clip(Squircle(20.dp)).background(P.surface).padding(vertical = 8.dp)) {
            items.forEach { it ->
                if (it.danger) Box(Modifier.fillMaxWidth().padding(vertical = 4.dp, horizontal = 16.dp).height(0.7.dp).background(P.line))
                Row(Modifier.fillMaxWidth().clickable { ui.menu = false; it.onClick() }.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Ic(it.icon, if (it.danger) P.danger else P.text2, 21); Spacer(Modifier.width(16.dp))
                    Text(it.text, color = if (it.danger) P.danger else P.text, style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }
}

@Composable
fun ReaderDialogs(vm: AppVm, doc: Doc, ui: ReaderUi) {
    if (ui.rename) TextPrompt("重命名", doc.title, "保存", { v -> if (v.isNotBlank()) Library.mutate(doc.id) { it.copy(title = v.trim().take(120)) } }, { ui.rename = false })
    if (ui.del) Confirm("删除这篇文档？", "「${doc.title}」会从拾页移除。", "删除", danger = true, onOk = { vm.pop(); Library.delete(listOf(doc.id)); vm.toast("已删除") }, onDismiss = { ui.del = false })
    if (ui.info) AlertDialog(onDismissRequest = { ui.info = false }, containerColor = P.surface, shape = Squircle(24.dp),
        title = { Text("文档信息", color = P.text, style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("类型" to if (doc.type == DocType.HTML) "网页 HTML" else "Markdown", "大小" to fmtSize(doc.size), "来源" to doc.source.ifBlank { "—" },
                    "存入" to relDate(doc.createdAt), "同步" to (if (doc.notionUrl.isNotBlank()) "Notion · " + fmtAgo(doc.notionAt) else "未同步")).forEach { (k, v) ->
                    Row { Text(k, color = P.text3, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.width(56.dp)); Text(v, color = P.text, style = MaterialTheme.typography.bodyMedium) }
                }
            }
        },
        confirmButton = { TextButton({ ui.info = false }) { Text("好的", color = P.accent, fontWeight = FontWeight.SemiBold) } })
    LinkGuard(ui.link) { ui.link = null }
}

@Composable
private fun LinkGuard(url: String?, onClose: () -> Unit) {
    val ctx = LocalContext.current
    if (url != null) Confirm("用浏览器打开链接？", url.take(300), "打开", onOk = { Share.openUrl(ctx, url) }, onDismiss = onClose)
}

private fun relDate(t: Long) = java.text.SimpleDateFormat("yyyy年M月d日", java.util.Locale.CHINA).format(java.util.Date(t))

// ---------------- Markdown 阅读 ----------------

private class Head(val item: Int, val level: Int, val title: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MdReader(vm: AppVm, doc: Doc, onExport: () -> Unit) {
    val prefs = vm.prefs
    val scope = rememberCoroutineScope()
    val text by produceState<String?>(null, doc.id) { value = withContext(Dispatchers.IO) { Library.readText(doc) } }
    val blocks = remember(text) { text?.let { Markdown.parse(it) }.orEmpty().let { b -> val f = b.firstOrNull(); if (f is MdBlock.Heading && f.level == 1 && f.segs.plain().trim() == doc.title.trim()) b.drop(1) else b } }
    val tags = remember(text) { text?.let { Markdown.tags(Markdown.stripFrontMatter(it).second) }.orEmpty() }
    val words = remember(text) { text?.let { wordCount(Markdown.stripFrontMatter(it).first) } ?: 0 }
    val heads = remember(blocks) { blocks.mapIndexedNotNull { i, b -> if (b is MdBlock.Heading && b.level <= 3) Head(i + 1, b.level, b.segs.plain()) else null } }
    val state = rememberLazyListState(initialFirstVisibleItemIndex = doc.pos)
    val backdrop = remember { Backdrop() }
    val ui = remember { ReaderUi() }
    var chrome by rememberSaveable { mutableStateOf(false) }
    var showToc by remember { mutableStateOf(false) }; var showAa by remember { mutableStateOf(false) }
    var scrub by remember { mutableStateOf<Float?>(null) }
    val style = ReadStyle(prefs.fontScale, prefs.serif, prefs.mdNet, Library.mainFile(doc).parentFile, onLink = { u ->
        if (u.startsWith("https://") || u.startsWith("http://")) ui.link = u else vm.toast("已拦截不支持的链接")
    }, lineMult = prefs.lineHeight, paraEm = prefs.paraSpacing)

    val progress by remember { derivedStateOf {
        val info = state.layoutInfo; val n = info.totalItemsCount
        if (n <= 1) 0f else if (!state.canScrollForward) 1f else {
            val first = info.visibleItemsInfo.firstOrNull()
            val frac = if (first != null && first.size > 0) state.firstVisibleItemScrollOffset.toFloat() / first.size else 0f
            ((state.firstVisibleItemIndex + frac) / (n - 1)).coerceIn(0f, 1f)
        }
    } }
    val curHead by remember(heads) { derivedStateOf { heads.lastOrNull { it.item <= state.firstVisibleItemIndex } } }
    val sectionNo by remember(heads) { derivedStateOf { heads.indexOfFirst { it === curHead } + 1 } }
    val minutesLeft = ceil(readMinutes(doc.size) * (1f - progress).toDouble()).toInt()

    LaunchedEffect(state) {
        var last = 0
        snapshotFlow { state.firstVisibleItemIndex * 100000 + state.firstVisibleItemScrollOffset }.collect { v ->
            if (scrub == null && kotlin.math.abs(v - last) > 40 && last != 0) chrome = false
            last = v
        }
    }
    // 阅读位置：停下 1.2 秒后记一次；离开时再记一次
    LaunchedEffect(state) { snapshotFlow { progress to state.firstVisibleItemIndex }.collect { (p, i) -> delay(1200); Library.setProgress(doc.id, p, i) } }
    DisposableEffect(Unit) { onDispose { Library.setProgress(doc.id, progress, state.firstVisibleItemIndex) } }
    BackHandler(chrome && !ui.menu) { chrome = false }

    CompositionLocalProvider(LocalBackdrop provides backdrop) {
        BoxWithConstraints(Modifier.fillMaxSize().background(P.bg)) {
            val side = maxOf(28.dp, (maxWidth - 680.dp) / 2)
            val statusH = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
            val navH = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
            Box(Modifier.fillMaxSize().glassSource(backdrop).pointerInput(Unit) { detectTapGestures(onTap = { chrome = !chrome }) }) {
                if (text == null) Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = P.accent, strokeWidth = 2.5.dp, modifier = Modifier.size(28.dp)) }
                else SelectionContainer {
                    LazyColumn(state = state, modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(style.gap),
                        contentPadding = PaddingValues(start = side, end = side, top = statusH + 16.dp, bottom = navH + 120.dp)) {
                        item(key = "title") {
                            ReaderTitleHeader(doc, tags, words, heads.size, prefs.fontScale, Modifier.padding(bottom = 16.dp))
                        }
                        items(blocks.size) { i -> Block(blocks[i], style) }
                        item(key = "end") { Text("— 读完了 —", color = P.text3, style = MaterialTheme.typography.bodySmall, modifier = Modifier.fillMaxWidth().padding(top = 28.dp), textAlign = TextAlign.Center) }
                    }
                }
            }
            // 沉浸态：极淡的页眉 / 页脚，不抢正文
            AnimatedVisibility(!chrome && (state.firstVisibleItemIndex > 0 || state.firstVisibleItemScrollOffset > 600), Modifier.align(Alignment.TopCenter), enter = fadeIn(), exit = fadeOut()) {
                Box(Modifier.fillMaxWidth().background(Brush.verticalGradient(listOf(P.bg, P.bg.copy(alpha = 0.9f), Color.Transparent))).padding(top = statusH + 8.dp, bottom = 16.dp, start = side, end = side)) {
                    Text(curHead?.title ?: doc.title, color = P.text3, style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                }
            }
            AnimatedVisibility(!chrome, Modifier.align(Alignment.BottomCenter), enter = fadeIn(), exit = fadeOut()) {
                Column(Modifier.fillMaxWidth().background(Brush.verticalGradient(listOf(Color.Transparent, P.bg.copy(alpha = 0.9f), P.bg)))) {
                    Spacer(Modifier.height(16.dp))
                    Row(Modifier.padding(horizontal = side).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(doc.title, color = P.text3, style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                        Text("${(progress * 100).toInt()}%  ·  " + if (minutesLeft <= 0) "即将读完" else "还剩 $minutesLeft 分钟", color = P.text3, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(start = 12.dp))
                    }
                    Spacer(Modifier.height(8.dp))
                    Box(Modifier.fillMaxWidth().height(2.dp).background(P.line.copy(alpha = 0.6f))) { Box(Modifier.fillMaxWidth(progress).fillMaxHeight().background(P.accent)) }
                    Spacer(Modifier.height(navH))
                }
            }
            // 点按唤出的玻璃栏
            AnimatedVisibility(chrome, Modifier.align(Alignment.TopCenter), enter = fadeIn() + slideInVertically { -it / 2 }, exit = fadeOut() + slideOutVertically { -it / 2 }) {
                ReaderTopBar(doc, { vm.pop() }, ui)
            }
            AnimatedVisibility(chrome, Modifier.align(Alignment.BottomCenter), enter = fadeIn() + slideInVertically { it / 2 }, exit = fadeOut() + slideOutVertically { it / 2 }) {
                Column(Modifier.windowInsetsPadding(WindowInsets.navigationBars).padding(start = 16.dp, end = 16.dp, bottom = 8.dp).widthIn(max = 560.dp).fillMaxWidth().glass(Squircle(30.dp), floating = true).padding(horizontal = 16.dp, vertical = 12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(if (sectionNo > 0) "第 $sectionNo 节 · ${curHead?.title.orEmpty()}" else doc.title, color = P.text2, style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                        Text(if (minutesLeft <= 0) "即将读完" else "还剩约 $minutesLeft 分钟", color = P.text3, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(start = 12.dp))
                    }
                    val colors = SliderDefaults.colors(thumbColor = P.accent, activeTrackColor = P.accent, inactiveTrackColor = P.line)
                    Slider(value = scrub ?: progress, onValueChange = { v ->
                        scrub = v
                        val n = state.layoutInfo.totalItemsCount
                        if (n > 1) scope.launch { state.scrollToItem((v * (n - 1)).toInt().coerceIn(0, n - 1)) }
                    }, onValueChangeFinished = { scrub = null }, modifier = Modifier.height(32.dp), colors = colors,
                        thumb = { Box(Modifier.size(24.dp).shadow(3.dp, CircleShape).clip(CircleShape).background(Color.White).border(5.dp, P.accent, CircleShape)) },
                        track = { s -> SliderDefaults.Track(s, Modifier.height(4.dp), colors = colors, thumbTrackGapSize = 0.dp, trackInsideCornerSize = 0.dp, drawStopIndicator = null) })
                    Row(Modifier.fillMaxWidth().padding(top = 4.dp)) {
                        ToolItem(R.drawable.ic_toc, "目录", Modifier.weight(1f)) { showToc = true }
                        ToolItem(R.drawable.ic_text_size, "排版", Modifier.weight(1f)) { showAa = true }
                        ToolItem(R.drawable.ic_edit, "编辑", Modifier.weight(1f)) { vm.push(Route.Edit(doc.id)) }
                        ToolItem(R.drawable.ic_share, "分享", Modifier.weight(1f), primary = true) { onExport() }
                    }
                }
            }
            ReaderMenu(vm, doc, ui)
        }
    }
    if (showToc) TocSheet(heads, curHead, onPick = { showToc = false; scope.launch { state.animateScrollToItem(it) } }, onDismiss = { showToc = false })
    if (showAa) TypographySheet(prefs) { showAa = false }
    ReaderDialogs(vm, doc, ui)
}

@Composable
private fun TocSheet(heads: List<Head>, cur: Head?, onPick: (Int) -> Unit, onDismiss: () -> Unit) {
    Sheet(onDismiss) {
        Text("目录", color = P.text, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), modifier = Modifier.padding(start = 4.dp, bottom = 8.dp))
        if (heads.isEmpty()) Text("这篇文档没有标题层级。", color = P.text3, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(8.dp, 12.dp))
        heads.forEach { h ->
            val on = h === cur
            Row(Modifier.fillMaxWidth().clip(Shapes.box).background(if (on) P.accentSoft else Color.Transparent).clickable { onPick(h.item) }.padding(start = (12 + (h.level - 1) * 18).dp, top = 12.dp, bottom = 12.dp, end = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(h.title, color = if (on) P.accent else if (h.level == 1) P.text else P.text2, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f),
                    style = if (h.level == 1) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyLarge.copy(fontWeight = if (on) FontWeight.SemiBold else FontWeight.Normal))
                if (on) Ic(R.drawable.ic_check, P.accent, 18)
            }
        }
    }
}

private fun trim(v: Float) = "%.2f".format(v).trimEnd('0').trimEnd('.')

/** 排版：阅读主题 / 字体 / 字号 · 行距 · 段间距。压在阅读页上，不挡遮罩，调的时候后面的正文实时变化。 */
@Composable
fun TypographySheet(prefs: Prefs, onDismiss: () -> Unit) {
    Sheet(onDismiss, scrim = Color.Transparent) {
        Row(Modifier.padding(start = 4.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("排版", color = P.text, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), modifier = Modifier.weight(1f))
            Text("重置", color = P.accent, style = MaterialTheme.typography.labelLarge, modifier = Modifier.clip(Shapes.chip).clickable { prefs.resetTypography() }.padding(horizontal = 16.dp, vertical = 8.dp))
        }
        SheetLabel("阅读主题")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ReadThemes.names.forEachIndexed { i, n ->
                val on = prefs.readTheme == i; val (bg, fg) = ReadThemes.swatch[i]
                Column(Modifier.weight(1f).clip(Squircle(16.dp)).clickable { prefs.saveReadTheme(i) }, horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.fillMaxWidth().height(60.dp).clip(Squircle(16.dp)).background(bg).border(if (on) 2.5.dp else 1.dp, if (on) P.accent else P.line, Squircle(16.dp)), contentAlignment = Alignment.Center) {
                        Text("Aa", color = fg, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Text(n, color = if (on) P.accent else P.text2, style = MaterialTheme.typography.labelMedium.copy(fontWeight = if (on) FontWeight.Bold else FontWeight.Medium), modifier = Modifier.padding(top = 8.dp))
                }
            }
        }
        SheetLabel("字体")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("黑体" to false, "宋体" to true).forEach { (n, serif) ->
                val on = prefs.serif == serif
                Row(Modifier.height(44.dp).clip(Shapes.chip).background(if (on) P.accentSoft else P.field).then(if (on) Modifier.border(1.5.dp, P.accent, Shapes.chip) else Modifier)
                    .clickable { prefs.saveSerif(serif) }.padding(horizontal = 24.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (on) { Ic(R.drawable.ic_check, P.accent, 17); Spacer(Modifier.width(8.dp)) }
                    Text(n, color = if (on) P.accent else P.text, fontSize = 16.sp, fontFamily = if (serif) FontFamily.Serif else FontFamily.Default, fontWeight = if (on) FontWeight.Bold else FontWeight.Medium)
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        val px = (17 * prefs.fontScale).roundToInt()
        Stepper("字号", "$px", when { px < 16 -> "偏小"; px <= 18 -> "标准"; px <= 21 -> "偏大"; else -> "很大" }, px > 14, px < 27,
            { prefs.saveFontScale((px - 1) / 17f) }, { prefs.saveFontScale((px + 1) / 17f) })
        Spacer(Modifier.height(8.dp))
        val lh = prefs.lineHeight
        Stepper("行距", trim(lh), when { lh < 1.5f -> "紧凑"; lh < 1.9f -> "舒适"; else -> "宽松" }, lh > 1.21f, lh < 2.19f,
            { prefs.saveLineHeight(((lh - 0.1f) * 10).roundToInt() / 10f) }, { prefs.saveLineHeight(((lh + 0.1f) * 10).roundToInt() / 10f) })
        Spacer(Modifier.height(8.dp))
        val ps = prefs.paraSpacing
        Stepper("段间距", trim(ps) + " em", when { ps < 0.3f -> "紧密"; ps < 0.9f -> "适中"; else -> "宽松" }, ps > 0.01f, ps < 1.49f,
            { prefs.saveParaSpacing(((ps - 0.1f) * 10).roundToInt() / 10f) }, { prefs.saveParaSpacing(((ps + 0.1f) * 10).roundToInt() / 10f) })
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun SheetLabel(t: String) = Text(t, color = P.text3, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(start = 4.dp, top = 16.dp, bottom = 8.dp))

@Composable
fun Stepper(label: String, value: String, desc: String, canMinus: Boolean, canPlus: Boolean, onMinus: () -> Unit, onPlus: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = P.text2, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.width(60.dp).padding(start = 4.dp))
        Row(Modifier.weight(1f).height(56.dp).clip(Shapes.chip).background(P.field).padding(horizontal = 7.dp), verticalAlignment = Alignment.CenterVertically) {
            StepBtn(R.drawable.ic_minus, "减小$label", canMinus, onMinus)
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                Text(value, color = P.text, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                Text("  $desc", color = P.text3, style = MaterialTheme.typography.bodySmall)
            }
            StepBtn(R.drawable.ic_plus, "增大$label", canPlus, onPlus)
        }
    }
}

@Composable
private fun StepBtn(icon: Int, desc: String, enabled: Boolean, onClick: () -> Unit) {
    Box(Modifier.size(40.dp).clip(CircleShape).background(P.surface).clickable(enabled = enabled, role = Role.Button, onClick = onClick).semantics { contentDescription = desc }, contentAlignment = Alignment.Center) {
        Ic(icon, if (enabled) P.text else P.text3.copy(alpha = 0.5f), 20)
    }
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
fun HtmlReader(vm: AppVm, doc: Doc, onExport: () -> Unit) {
    val prefs = vm.prefs
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var chrome by remember { mutableStateOf(true) }
    val ui = remember { ReaderUi() }
    var askScript by remember { mutableStateOf(false) }
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
                            if (s.startsWith("https://") || s.startsWith("http://")) ui.link = s else vm.toast("已拦截不支持的链接")
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
                    setOnScrollChangeListener { _, _, y, _, oy -> if (y - oy > 12) chrome = false else if (oy - y > 12) chrome = true }
                    loadUrl(url)
                }
            }, onRelease = { it.stopLoading(); it.destroy() })
        }
        if (loading) androidx.compose.material3.LinearProgressIndicator(Modifier.fillMaxWidth().padding(top = top), color = P.accent, trackColor = androidx.compose.ui.graphics.Color.Transparent)
        if (doc.script) Row(Modifier.align(Alignment.TopCenter).padding(top = top + 8.dp).clip(Shapes.chip).background(P.accent).padding(horizontal = 12.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("脚本已开启", color = P.onAccent, style = MaterialTheme.typography.labelMedium)
        }
        AnimatedVisibility(chrome, Modifier.align(Alignment.TopCenter), enter = fadeIn() + slideInVertically { -it / 2 }, exit = fadeOut() + slideOutVertically { -it / 2 }) { ReaderTopBar(doc, { vm.pop() }, ui) }
        AnimatedVisibility(chrome, Modifier.align(Alignment.BottomCenter), enter = fadeIn() + slideInVertically { it / 2 }, exit = fadeOut() + slideOutVertically { it / 2 }) {
            Row(Modifier.windowInsetsPadding(WindowInsets.navigationBars).padding(start = 16.dp, end = 16.dp, bottom = 8.dp).widthIn(max = 420.dp).fillMaxWidth().glass(Squircle(30.dp), floating = true).padding(horizontal = 16.dp, vertical = 8.dp)) {
                ToolItem(R.drawable.ic_edit, "编辑", Modifier.weight(1f)) { vm.push(Route.Edit(doc.id)) }
                ToolItem(R.drawable.ic_share, "分享", Modifier.weight(1f), primary = true) { onExport() }
            }
        }
        ReaderMenu(vm, doc, ui, listOf(
            MenuItem(R.drawable.ic_code, if (doc.script) "关闭脚本" else "开启脚本") { if (doc.script) Library.mutate(doc.id) { it.copy(script = false) } else askScript = true },
            MenuItem(R.drawable.ic_desktop, if (doc.desktop) "手机版网页" else "电脑版网页") { Library.mutate(doc.id) { it.copy(desktop = !it.desktop) } },
            MenuItem(R.drawable.ic_rotate, if (landscape) "恢复竖屏" else "横屏阅读") { landscape = !landscape }))
    }
    if (askScript) Confirm("开启这篇网页的脚本？", "脚本能让交互式页面正常工作，但网页里的代码会在这里运行。只对你信任的来源开启，之后可随时在顶栏关闭。", "开启", onOk = { Library.mutate(doc.id) { it.copy(script = true) }; loading = true }, onDismiss = { askScript = false })
    ReaderDialogs(vm, doc, ui)
}
