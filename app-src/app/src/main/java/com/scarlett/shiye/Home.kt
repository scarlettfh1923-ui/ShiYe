package com.scarlett.shiye

import android.content.ClipboardManager
import android.content.Context
import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.lazy.staggeredgrid.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.ImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun rememberThumb(doc: Doc): ImageBitmap? {
    val v = remember(doc.id, doc.openedAt, doc.size) { Library.thumbFile(doc) }
    return produceState<ImageBitmap?>(null, v) {
        value = withContext(Dispatchers.IO) {
            runCatching { if (v.isFile) BitmapFactory.decodeFile(v.path, BitmapFactory.Options().apply { inSampleSize = 2 })?.asImageBitmap() else null }.getOrNull()
        }
    }.value
}


/** 自适应：>=600dp 使用侧边导航栏（NavigationRail）与多列网格，手机用底部悬浮玻璃导航。 */
@Composable
fun isWide(): Boolean = androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp >= 600

@Composable
fun HomeScreen(vm: AppVm) {
    val prefs = vm.prefs
    if (!prefs.onboarded) { Onboarding(vm); return }
    val ctx = LocalContext.current
    val docs by Library.docs.collectAsState()
    val backdrop = remember { Backdrop() }
    val wide = isWide()
    var menuDoc by remember { mutableStateOf<Doc?>(null) }
    var renameDoc by remember { mutableStateOf<Doc?>(null) }
    var deleteDoc by remember { mutableStateOf<Doc?>(null) }
    var showSort by remember { mutableStateOf(false) }
    var showImport by remember { mutableStateOf(false) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { if (it.isNotEmpty()) vm.importUris(it) }
    BackHandlerIf(vm.tab != Tab.Read) { vm.tab = Tab.Read; vm.searching = false }

    CompositionLocalProvider(LocalBackdrop provides backdrop) {
        Row(Modifier.fillMaxSize()) {
            if (wide) HomeRail(vm) { showImport = true }
            Box(Modifier.weight(1f).fillMaxHeight()) {
                Box(Modifier.fillMaxSize().glassSource(backdrop)) {
                    Crossfade(vm.tab, label = "tab") { t ->
                        when (t) {
                            Tab.Read -> ReadTab(vm, docs, onMenu = { menuDoc = it }, onImport = { showImport = true })
                            Tab.Library -> LibraryTab(vm, docs, onMenu = { menuDoc = it }, onSort = { showSort = true }, onImport = { showImport = true })
                            Tab.Me -> MeTab(vm)
                        }
                    }
                }
                if (!wide) {
                    if (vm.tab != Tab.Me) ImportFab(Modifier.align(Alignment.BottomEnd).windowInsetsPadding(WindowInsets.navigationBars).padding(end = 20.dp, bottom = 96.dp)) { showImport = true }
                    HomeNav(vm, Modifier.align(Alignment.BottomCenter).windowInsetsPadding(WindowInsets.navigationBars).padding(horizontal = 12.dp).padding(bottom = 14.dp))
                } else if (vm.tab != Tab.Me) ImportFab(Modifier.align(Alignment.BottomEnd).windowInsetsPadding(WindowInsets.navigationBars).padding(end = 24.dp, bottom = 24.dp)) { showImport = true }
            }
        }
    }

    if (showImport) Sheet({ showImport = false }) {
        Text("存入拾页", color = P.text, style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(start = 6.dp, bottom = 8.dp, top = 4.dp))
        SheetAction(R.drawable.ic_folder, "从文件选择（md / html / zip）") { showImport = false; picker.launch(arrayOf("*/*")) }
        SheetAction(R.drawable.ic_clipboard, "粘贴剪贴板里的文字") {
            showImport = false
            val t = (ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).primaryClip?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.coerceToText(ctx)?.toString()
            if (t.isNullOrBlank()) vm.toast("剪贴板里没有文字") else vm.importText(t)
        }
        Row(Modifier.padding(6.dp, 10.dp).fillMaxWidth().clip(Shapes.box).background(P.accentSoft).padding(14.dp)) {
            Ic(R.drawable.ic_wechat, P.accent, 20); Spacer(Modifier.width(12.dp))
            Text("在微信、浏览器、文件管理里长按文件或点「分享」，选「拾页」，内容会直接存进来。", color = P.text2, fontSize = 13.sp, lineHeight = 19.sp)
        }
    }
    if (showSort) Sheet({ showSort = false }) {
        Text("排序方式", color = P.text, style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(start = 6.dp, bottom = 8.dp, top = 4.dp))
        listOf("最近打开", "最近存入", "名称", "文件大小").forEachIndexed { i, n ->
            Row(Modifier.fillMaxWidth().clip(Shapes.box).clickable { prefs.saveSort(i); showSort = false }.padding(6.dp, 14.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(n, color = if (prefs.sort == i) P.accent else P.text, fontSize = 16.sp, modifier = Modifier.weight(1f))
                if (prefs.sort == i) Ic(R.drawable.ic_check, P.accent, 20)
            }
        }
    }
    menuDoc?.let { d ->
        Sheet({ menuDoc = null }) {
            Text(d.title, color = P.text, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(start = 6.dp, bottom = 6.dp, top = 2.dp))
            Text("${if (d.type == DocType.MD) "Markdown" else "网页"} · ${fmtSize(d.size)} · ${d.source}", color = P.text3, fontSize = 12.5.sp, modifier = Modifier.padding(start = 6.dp, bottom = 8.dp))
            SheetAction(if (d.fav) R.drawable.ic_bookmark_fill else R.drawable.ic_bookmark, if (d.fav) "取消收藏" else "收藏") { Library.mutate(d.id) { it.copy(fav = !it.fav) }; menuDoc = null }
            SheetAction(R.drawable.ic_share, "导出与分享") { menuDoc = null; vm.openDoc(d.id, export = true) }
            SheetAction(R.drawable.ic_text_size, "重命名") { menuDoc = null; renameDoc = d }
            SheetAction(R.drawable.ic_trash, "删除", danger = true) { menuDoc = null; deleteDoc = d }
        }
    }
    renameDoc?.let { d -> TextPrompt("重命名", d.title, "保存", { v -> if (v.isNotBlank()) Library.mutate(d.id) { it.copy(title = v.trim().take(120)) } }, { renameDoc = null }) }
    deleteDoc?.let { d -> Confirm("删除这篇文档？", "「${d.title}」会从拾页移除，原文件不受影响。", "删除", danger = true, onOk = { Library.delete(listOf(d.id)); vm.toast("已删除") }, onDismiss = { deleteDoc = null }) }
}

private val tabIcons = listOf(R.drawable.ic_home, R.drawable.ic_library, R.drawable.ic_user)
private val tabNames = listOf("阅读", "书库", "我的")

/** 底部悬浮玻璃导航：三个目的地，当前项用主色淡底胶囊标示 */
@Composable
private fun HomeNav(vm: AppVm, modifier: Modifier) {
    val pal = P
    Row(modifier.fillMaxWidth().widthIn(max = 520.dp).height(66.dp).glass(CircleShape, floating = true).padding(horizontal = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Tab.entries.forEachIndexed { i, t ->
            val on = vm.tab == t
            Column(Modifier.weight(1f).height(50.dp).clip(CircleShape).background(if (on) pal.accent.copy(alpha = 0.12f) else Color.Transparent)
                .selectable(on, role = androidx.compose.ui.semantics.Role.Tab) { vm.tab = t; vm.searching = false },
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Ic(tabIcons[i], if (on) pal.accent else pal.text2, 23)
                Text(tabNames[i], color = if (on) pal.accent else pal.text2, style = MaterialTheme.typography.labelSmall.copy(fontWeight = if (on) FontWeight.Bold else FontWeight.Medium))
            }
        }
    }
}

/** 平板 / 折叠屏：侧边导航栏 */
@Composable
private fun HomeRail(vm: AppVm, onImport: () -> Unit) {
    val pal = P
    NavigationRail(containerColor = pal.surface, modifier = Modifier.fillMaxHeight(), windowInsets = WindowInsets.systemBars.only(WindowInsetsSides.Vertical + WindowInsetsSides.Start),
        header = { Spacer(Modifier.height(8.dp)) }) {
        Spacer(Modifier.weight(0.3f))
        Tab.entries.forEachIndexed { i, t ->
            NavigationRailItem(selected = vm.tab == t, onClick = { vm.tab = t; vm.searching = false }, icon = { Ic(tabIcons[i], if (vm.tab == t) pal.accent else pal.text2, 24) },
                label = { Text(tabNames[i], style = MaterialTheme.typography.labelMedium) },
                colors = NavigationRailItemDefaults.colors(indicatorColor = pal.accentSoft, selectedTextColor = pal.accent, unselectedTextColor = pal.text2))
        }
        Spacer(Modifier.weight(1f))
    }
}

@Composable
private fun ImportFab(modifier: Modifier, onClick: () -> Unit) {
    Box(modifier.size(56.dp).shadow(10.dp, RoundedCornerShape(18.dp), ambientColor = Color(0x402F6BFF), spotColor = Color(0x552F6BFF)).clip(RoundedCornerShape(18.dp)).background(P.accent)
        .clickable(role = androidx.compose.ui.semantics.Role.Button, onClick = onClick).semantics { contentDescription = "导入" }, contentAlignment = Alignment.Center) {
        Ic(R.drawable.ic_plus, P.onAccent, 26)
    }
}

// ---------------- 阅读 tab ----------------
@Composable
private fun ReadTab(vm: AppVm, docs: List<Doc>, onMenu: (Doc) -> Unit, onImport: () -> Unit) {
    val pal = P
    val current = remember(docs) { docs.filter { it.progress in 0.001f..0.985f }.maxByOrNull { it.openedAt } ?: docs.maxByOrNull { it.openedAt } }
    val recent = remember(docs, current) { docs.sortedByDescending { it.createdAt }.filter { it.id != current?.id }.take(8) }
    val favs = remember(docs) { docs.filter { it.fav }.sortedByDescending { it.openedAt }.take(5) }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 12.dp, bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 170.dp)) {
        item(key = "search") {
            Box(Modifier.padding(horizontal = 16.dp).widthIn(max = 720.dp).fillMaxWidth()) {
                SearchPill("", {}, false, hint = "在书库中搜索", onActivate = { vm.tab = Tab.Library; vm.searching = true }, trailing = {
                    Box(Modifier.size(38.dp).clip(CircleShape).background(Brush.linearGradient(listOf(pal.accent, Color(0xFF7FA2FF)))).clickable { vm.tab = Tab.Me }.semantics { contentDescription = "我的" }, contentAlignment = Alignment.Center) { Ic(R.drawable.ic_user, pal.onAccent, 20) }
                }, readOnly = true)
            }
        }
        if (docs.isEmpty()) item(key = "empty") { EmptyState(true, false, onImport) }
        current?.let { c ->
            item(key = "h1") { SectionTitle("继续阅读", null) {} }
            item(key = "cont") {
                Row(Modifier.padding(horizontal = 16.dp).widthIn(max = 720.dp).fillMaxWidth().shadow(if (pal.dark) 0.dp else 8.dp, RoundedCornerShape(28.dp), ambientColor = Color(0x0F1B2A4D), spotColor = Color(0x141B2A4D))
                    .clip(RoundedCornerShape(28.dp)).background(pal.surface).clickable { vm.openDoc(c.id) }.padding(14.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    DocCover(c, 92, 124, 16)
                    Column(Modifier.weight(1f).height(124.dp), verticalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text("${c.source} · ${fmtAgo(c.openedAt)}", color = pal.text3, style = MaterialTheme.typography.bodySmall, maxLines = 1)
                            Text(c.title, color = pal.text, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, lineHeight = 24.sp), maxLines = 3, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 4.dp))
                        }
                        Column {
                            LinearProgressIndicator(progress = { c.progress.coerceIn(0f, 1f) }, Modifier.fillMaxWidth().height(5.dp).clip(CircleShape), color = pal.accent, trackColor = pal.field, strokeCap = androidx.compose.ui.graphics.StrokeCap.Round, gapSize = 0.dp, drawStopIndicator = {})
                            Text(if (c.progress < 0.005f) "未开始 · 约 ${readMinutes(c.size)} 分钟" else "已读 ${(c.progress * 100).toInt()}% · 还剩约 ${(readMinutes(c.size) * (1 - c.progress)).toInt().coerceAtLeast(1)} 分钟", color = pal.text2, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 8.dp))
                        }
                    }
                }
            }
        }
        if (recent.isNotEmpty()) {
            item(key = "h2") { SectionTitle("最近添加", "全部") { vm.filter = Filter.All; vm.tab = Tab.Library } }
            item(key = "recent") {
                LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(recent.size, key = { recent[it].id }) { i ->
                        val d = recent[i]
                        Column(Modifier.width(112.dp).clip(RoundedCornerShape(16.dp)).combinedClickable(onClick = { vm.openDoc(d.id) }, onLongClick = { onMenu(d) })) {
                            DocCover(d, 112, 150, 44)
                            Text(d.title, color = pal.text, style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold, lineHeight = 18.sp), maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 8.dp, start = 2.dp).heightIn(min = 36.dp))
                            Text("${d.source} · ${fmtAgo(d.createdAt)}", color = pal.text3, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Normal), maxLines = 1, modifier = Modifier.padding(top = 2.dp, start = 2.dp))
                        }
                    }
                }
            }
        }
        if (favs.isNotEmpty()) {
            item(key = "h3") { SectionTitle("收藏", "全部") { vm.filter = Filter.Fav; vm.tab = Tab.Library } }
            items(favs.size, key = { "fav" + favs[it].id }) { i ->
                val d = favs[i]
                Row(Modifier.padding(horizontal = 16.dp).widthIn(max = 720.dp).fillMaxWidth().clip(RoundedCornerShape(16.dp)).combinedClickable(onClick = { vm.openDoc(d.id) }, onLongClick = { onMenu(d) }).padding(horizontal = 4.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    DocCover(d, 44, 58, 18, small = true)
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(d.title, color = pal.text, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("${if (d.type == DocType.MD) "Markdown" else "网页"} · ${fmtAgo(d.openedAt)} · ${readMinutes(d.size)} 分钟", color = pal.text3, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 3.dp))
                    }
                    Ic(R.drawable.ic_bookmark_fill, pal.accent, 20)
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(title: String, action: String?, onAction: () -> Unit) {
    Row(Modifier.padding(start = 20.dp, end = 20.dp, top = 22.dp, bottom = 10.dp).widthIn(max = 720.dp).fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
        Text(title, color = P.text, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), modifier = Modifier.weight(1f))
        if (action != null) Text(action, color = P.accent, style = MaterialTheme.typography.labelLarge, modifier = Modifier.clip(Shapes.chip).clickable(onClick = onAction).padding(8.dp, 4.dp))
    }
}

/** 封面：已有网页缩略图则用真图；否则用淡蓝渐变 + 首字 + 类型标 + 两条细线（和设计稿一致） */
@Composable
fun DocCover(d: Doc, w: Int, h: Int, fs: Int, small: Boolean = false) {
    val pal = P
    val thumb = rememberThumb(d)
    val (soft, strong) = tint(d, pal)
    val i = (d.id.hashCode() and 3)
    val g = if (pal.dark) listOf(Color(0xFF1B2744), Color(0xFF243255)) else when (i) { 0 -> listOf(Color(0xFFE8EFFF), Color(0xFFC9D9FF)); 1 -> listOf(Color(0xFFEEF2F8), Color(0xFFD5DFF0)); 2 -> listOf(Color(0xFFE3ECFF), Color(0xFFBFD1FA)); else -> listOf(Color(0xFFF0F4FB), Color(0xFFD6E0F3)) }
    Box(Modifier.size(w.dp, h.dp).clip(RoundedCornerShape(if (small) 10.dp else 16.dp)).background(Brush.linearGradient(g)), contentAlignment = Alignment.Center) {
        if (thumb != null) Image(thumb, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop, alignment = Alignment.TopCenter)
        else {
            if (!small) Text(d.title.firstOrNull { !it.isWhitespace() && it != '#' }?.toString() ?: "拾", color = strong.copy(alpha = 0.28f), fontSize = fs.sp, fontWeight = FontWeight.ExtraBold)
            if (!small) Column(Modifier.align(Alignment.BottomStart).padding(start = 10.dp, end = 10.dp, bottom = 12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Box(Modifier.fillMaxWidth(0.7f).height(3.dp).clip(CircleShape).background(strong.copy(alpha = 0.22f)))
                Box(Modifier.fillMaxWidth(0.5f).height(3.dp).clip(CircleShape).background(strong.copy(alpha = 0.22f)))
            }
        }
        if (!small) Box(Modifier.align(Alignment.TopStart).padding(8.dp).clip(RoundedCornerShape(6.dp)).background(pal.surface.copy(alpha = 0.88f)).padding(horizontal = 6.dp, vertical = 2.dp)) {
            Text(if (d.type == DocType.MD) "MD" else "HTML", color = strong, fontSize = 9.5.sp, fontWeight = FontWeight.ExtraBold)
        }
    }
}

// ---------------- 书库 tab ----------------
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LibraryTab(vm: AppVm, docs: List<Doc>, onMenu: (Doc) -> Unit, onSort: () -> Unit, onImport: () -> Unit) {
    val prefs = vm.prefs
    val shown = remember(docs, vm.query, vm.filter, prefs.sort) {
        val q = vm.query.trim().lowercase()
        docs.filter { d ->
            (when (vm.filter) { Filter.Md -> d.type == DocType.MD; Filter.Html -> d.type == DocType.HTML; Filter.Fav -> d.fav; else -> true }) &&
                (q.isEmpty() || d.title.lowercase().contains(q) || d.snippet.lowercase().contains(q) || d.source.contains(q))
        }.sortedWith(when (prefs.sort) {
            1 -> compareByDescending<Doc> { it.createdAt }
            2 -> compareBy<Doc> { it.title.lowercase() }
            3 -> compareByDescending<Doc> { it.size }
            else -> compareByDescending<Doc> { it.openedAt }
        })
    }
    BackHandlerIf(vm.searching) { vm.searching = false; vm.query = "" }
    val wide = isWide()
    LazyVerticalStaggeredGrid(
        columns = if (prefs.grid) StaggeredGridCells.Adaptive(if (wide) 190.dp else 160.dp) else StaggeredGridCells.Fixed(1),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 12.dp, bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 170.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp), verticalItemSpacing = 12.dp,
    ) {
        item(span = StaggeredGridItemSpan.FullLine, key = "head") {
            Column {
                SearchPill(vm.query, { vm.query = it }, vm.searching, hint = "搜索标题或内容", onActivate = { vm.searching = true })
                Spacer(Modifier.height(14.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(horizontal = 2.dp)) {
                    items(Filter.entries.size) { i -> val f = Filter.entries[i]; Chip(f.label, vm.filter == f) { vm.filter = f } }
                }
                Row(Modifier.padding(top = 10.dp, start = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("${shown.size} 篇 · ${listOf("最近打开", "最近存入", "名称", "文件大小")[prefs.sort]}", color = P.text3, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                    CircleBtn(R.drawable.ic_sort, "排序", onSort, glass = true, size = 36)
                    Spacer(Modifier.width(8.dp))
                    CircleBtn(if (prefs.grid) R.drawable.ic_list else R.drawable.ic_grid, if (prefs.grid) "切换为列表" else "切换为卡片", { prefs.saveGrid(!prefs.grid) }, glass = true, size = 36)
                }
            }
        }
        if (shown.isEmpty()) item(span = StaggeredGridItemSpan.FullLine, key = "empty") { EmptyState(docs.isEmpty(), vm.query.isNotBlank(), onImport) }
        items(shown.size, key = { shown[it].id }) { i ->
            val d = shown[i]
            if (prefs.grid) DocCard(d, onClick = { vm.openDoc(d.id) }, onLong = { onMenu(d) }, onFav = { Library.mutate(d.id) { x -> x.copy(fav = !x.fav) } })
            else DocRow(d, onClick = { vm.openDoc(d.id) }, onLong = { onMenu(d) })
        }
    }
}

@Composable
private fun SearchPill(q: String, onQ: (String) -> Unit, active: Boolean, hint: String, onActivate: () -> Unit, trailing: (@Composable () -> Unit)? = null, readOnly: Boolean = false) {
    val focus = LocalFocusManager.current
    val req = remember { androidx.compose.ui.focus.FocusRequester() }
    LaunchedEffect(active) { if (active && !readOnly) runCatching { req.requestFocus() } }
    Row(Modifier.fillMaxWidth().height(54.dp).shadow(10.dp, CircleShape, ambientColor = Color(0x121B2A4D), spotColor = Color(0x1A1B2A4D)).clip(CircleShape).background(P.surface)
        .clickable(onClick = onActivate).padding(start = 18.dp, end = if (trailing != null) 8.dp else 18.dp), verticalAlignment = Alignment.CenterVertically) {
        Ic(R.drawable.ic_search, P.text2, 22); Spacer(Modifier.width(12.dp))
        Box(Modifier.weight(1f)) {
            if (q.isEmpty()) Text(hint, color = P.text3, fontSize = 15.5.sp)
            if (!readOnly) BasicTextField(q, onQ, singleLine = true, textStyle = androidx.compose.ui.text.TextStyle(color = P.text, fontSize = 15.sp), cursorBrush = SolidColor(P.accent),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search), keyboardActions = KeyboardActions(onSearch = { focus.clearFocus() }),
                modifier = Modifier.fillMaxWidth().focusRequester(req))
        }
        if (q.isNotEmpty()) Box(Modifier.size(28.dp).clip(CircleShape).clickable { onQ("") }, contentAlignment = Alignment.Center) { Ic(R.drawable.ic_close, P.text3, 18) }
        trailing?.invoke()
    }
}

@Composable
fun BackHandlerIf(enabled: Boolean, onBack: () -> Unit) = androidx.activity.compose.BackHandler(enabled = enabled, onBack = onBack)

@Composable
fun TextPrompt(title: String, initial: String, ok: String, onOk: (String) -> Unit, onDismiss: () -> Unit) {
    var v by remember { mutableStateOf(initial) }
    AlertDialog(onDismissRequest = onDismiss, containerColor = P.surface, shape = RoundedCornerShape(24.dp),
        title = { Text(title, color = P.text, fontWeight = FontWeight.SemiBold, fontSize = 18.sp) },
        text = {
            BasicTextField(v, { v = it }, singleLine = true, textStyle = androidx.compose.ui.text.TextStyle(color = P.text, fontSize = 16.sp), cursorBrush = SolidColor(P.accent),
                modifier = Modifier.fillMaxWidth().clip(Shapes.box).background(P.field).padding(14.dp))
        },
        confirmButton = { TextButton(onClick = { onOk(v); onDismiss() }) { Text(ok, color = P.accent, fontWeight = FontWeight.SemiBold) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消", color = P.text2) } })
}

@Composable
fun EmptyState(none: Boolean, searching: Boolean, onImport: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(top = 56.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(96.dp).clip(CircleShape).background(P.accentSoft), contentAlignment = Alignment.Center) { Ic(if (searching) R.drawable.ic_search else R.drawable.ic_doc, P.accent, 40) }
        Spacer(Modifier.height(20.dp))
        Text(if (searching) "没有找到相关内容" else if (none) "这里还是空的" else "这个分类下暂时没有文档", color = P.text, fontSize = 19.sp, fontWeight = FontWeight.Bold)
        Text(if (none) "从微信或浏览器分享到「拾页」，\n或导入手机里的 md / html 文件。" else "换个关键词或分类试试。", color = P.text3, fontSize = 14.sp, lineHeight = 21.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center, modifier = Modifier.padding(top = 8.dp))
        if (none) { Spacer(Modifier.height(22.dp)); PillButton("导入文件", R.drawable.ic_import, onImport) }
    }
}

fun tint(d: Doc, pal: Pal): Pair<Color, Color> = if (d.type == DocType.MD) pal.accentSoft to pal.accent else (if (pal.dark) Color(0xFF2A2440) else Color(0xFFEFEAFF)) to (if (pal.dark) Color(0xFFB7A6FF) else Color(0xFF6A4DFF))

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DocCard(d: Doc, onClick: () -> Unit, onLong: () -> Unit, onFav: () -> Unit) {
    val pal = P
    val thumb = rememberThumb(d)
    val (soft, strong) = tint(d, pal)
    Column(Modifier.fillMaxWidth().shadow(if (pal.dark) 0.dp else 8.dp, Shapes.card, ambientColor = Color(0x0F1B2A4D), spotColor = Color(0x1A1B2A4D)).clip(Shapes.card).background(pal.surface)
        .combinedClickable(onClick = onClick, onLongClick = onLong)) {
        Box {
            if (thumb != null) {
                Image(thumb, null, Modifier.fillMaxWidth().aspectRatio(1.18f), contentScale = ContentScale.Crop, alignment = Alignment.TopCenter)
            } else {
                Box(Modifier.fillMaxWidth().height((74 + (d.title.hashCode() and 15) * 3).dp).background(Brush.linearGradient(listOf(soft, pal.surface.copy(alpha = 0f).compositeOver(soft)))), contentAlignment = Alignment.BottomStart) {
                    Text(d.title.firstOrNull { !it.isWhitespace() && it != '#' }?.toString() ?: "拾", color = strong.copy(alpha = 0.22f), fontSize = 64.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(start = 14.dp, bottom = 0.dp))
                }
            }
            Box(Modifier.align(Alignment.TopStart).padding(10.dp).clip(Shapes.chip).background(pal.surface.copy(alpha = 0.88f)).padding(horizontal = 8.dp, vertical = 3.dp)) {
                Text(if (d.type == DocType.MD) "MD" else "HTML", color = strong, fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
            }
            Box(Modifier.align(Alignment.TopEnd).padding(8.dp).size(32.dp).clip(CircleShape).background(pal.surface.copy(alpha = 0.9f)).clickable(onClick = onFav), contentAlignment = Alignment.Center) {
                Ic(if (d.fav) R.drawable.ic_bookmark_fill else R.drawable.ic_bookmark, if (d.fav) pal.accent else pal.text3, 17)
            }
        }
        Column(Modifier.padding(start = 14.dp, end = 14.dp, top = 12.dp, bottom = 14.dp)) {
            Text(d.title, color = pal.text, fontSize = 15.5.sp, fontWeight = FontWeight.SemiBold, lineHeight = 21.sp, maxLines = 3, overflow = TextOverflow.Ellipsis)
            if (d.snippet.isNotBlank()) Text(d.snippet, color = pal.text2, fontSize = 12.5.sp, lineHeight = 18.sp, maxLines = if (thumb != null) 2 else 2 + (d.hash.hashCode() and 3), overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 6.dp))
            Row(Modifier.padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("${fmtAgo(d.openedAt)} · ${d.source}", color = pal.text3, fontSize = 11.5.sp, maxLines = 1, modifier = Modifier.weight(1f))
                if (d.notionUrl.isNotBlank()) { Spacer(Modifier.width(6.dp)); Ic(R.drawable.ic_notion, pal.text3, 14) }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DocRow(d: Doc, onClick: () -> Unit, onLong: () -> Unit) {
    val pal = P
    val (soft, strong) = tint(d, pal)
    Row(Modifier.fillMaxWidth().shadow(if (pal.dark) 0.dp else 6.dp, Shapes.card, ambientColor = Color(0x0F1B2A4D), spotColor = Color(0x141B2A4D)).clip(Shapes.card).background(pal.surface)
        .combinedClickable(onClick = onClick, onLongClick = onLong).padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(46.dp).clip(RoundedCornerShape(14.dp)).background(soft), contentAlignment = Alignment.Center) { Ic(if (d.type == DocType.MD) R.drawable.ic_file_md else R.drawable.ic_file_html, strong, 24) }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(d.title, color = pal.text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("${fmtAgo(d.openedAt)} · ${fmtSize(d.size)} · ${d.source}", color = pal.text3, fontSize = 12.sp, maxLines = 1, modifier = Modifier.padding(top = 3.dp))
        }
        if (d.fav) { Spacer(Modifier.width(8.dp)); Ic(R.drawable.ic_bookmark_fill, pal.accent, 18) }
    }
}

// ---------------- 引导页 ----------------
@Composable
fun Onboarding(vm: AppVm) {
    val pal = P
    val pages = listOf(
        Triple(R.drawable.ic_wechat, "从微信直接收进来", "在微信、浏览器里点「分享」选「拾页」，文章、文件、网页都会自动存进书库，不用再翻聊天记录。"),
        Triple(R.drawable.ic_doc, "舒服地读 md 和网页", "Markdown 排成好读的版面，表格、代码、图片都在；网页默认关闭脚本，安全地看。"),
        Triple(R.drawable.ic_cards, "一键变成小红书图文", "选个样式，生成图文卡片或一张长图，也能同步到 Notion，读完就能发。"),
    )
    val state = rememberPagerState { pages.size }
    val scope = rememberCoroutineScope()
    Column(Modifier.fillMaxSize().background(pal.bg).windowInsetsPadding(WindowInsets.systemBars).padding(horizontal = 28.dp)) {
        Row(Modifier.fillMaxWidth().padding(top = 16.dp), horizontalArrangement = Arrangement.End) {
            Text("跳过", color = pal.text3, fontSize = 15.sp, modifier = Modifier.clip(Shapes.chip).clickable { vm.prefs.setOnboarded() }.padding(12.dp, 8.dp))
        }
        HorizontalPager(state, Modifier.weight(1f)) { i ->
            val (ic, t, s) = pages[i]
            Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Box(Modifier.size(180.dp).shadow(24.dp, CircleShape, ambientColor = Color(0x302F6BFF), spotColor = Color(0x402F6BFF)).clip(CircleShape).background(pal.surface), contentAlignment = Alignment.Center) {
                    Box(Modifier.size(112.dp).clip(CircleShape).background(pal.accentSoft), contentAlignment = Alignment.Center) { Ic(ic, pal.accent, 52) }
                }
                Spacer(Modifier.height(44.dp))
                Text(t, color = pal.text, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                Text(s, color = pal.text2, fontSize = 15.sp, lineHeight = 23.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center, modifier = Modifier.padding(top = 14.dp, start = 6.dp, end = 6.dp))
            }
        }
        Row(Modifier.fillMaxWidth().padding(bottom = 10.dp), horizontalArrangement = Arrangement.Center) {
            repeat(pages.size) { i ->
                val on = state.currentPage == i
                Box(Modifier.padding(4.dp).height(8.dp).width(if (on) 24.dp else 8.dp).clip(Shapes.chip).background(if (on) pal.accent else pal.line))
            }
        }
        PillButton(if (state.currentPage == pages.lastIndex) "开始使用" else "继续", null, {
            if (state.currentPage == pages.lastIndex) vm.prefs.setOnboarded() else scope.launch { state.animateScrollToPage(state.currentPage + 1) }
        }, Modifier.fillMaxWidth().padding(bottom = 20.dp))
    }
}
