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
                if (!wide) HomeNav(vm, Modifier.align(Alignment.BottomCenter).windowInsetsPadding(WindowInsets.navigationBars).padding(horizontal = 16.dp).padding(bottom = 24.dp))
            }
        }
    }

    if (showImport) Sheet({ showImport = false }) {
        Text("存入拾页", color = P.text, style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(start = 8.dp, bottom = 8.dp, top = 4.dp))
        SheetAction(R.drawable.ic_folder, "从文件选择（md / html / zip）") { showImport = false; picker.launch(arrayOf("*/*")) }
        SheetAction(R.drawable.ic_clipboard, "粘贴剪贴板里的文字") {
            showImport = false
            val t = (ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).primaryClip?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.coerceToText(ctx)?.toString()
            if (t.isNullOrBlank()) vm.toast("剪贴板里没有文字") else vm.importText(t)
        }
        Row(Modifier.padding(8.dp, 8.dp).fillMaxWidth().clip(Shapes.box).background(P.accentSoft).padding(16.dp)) {
            Ic(R.drawable.ic_wechat, P.accent, 20); Spacer(Modifier.width(12.dp))
            Text("在微信、浏览器、文件管理里长按文件或点「分享」，选「拾页」，内容会直接存进来。", color = P.text2, fontSize = 13.sp, lineHeight = 19.sp)
        }
    }
    if (showSort) Sheet({ showSort = false }) {
        Text("排序方式", color = P.text, style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(start = 8.dp, bottom = 8.dp, top = 4.dp))
        listOf("最近打开", "最近存入", "名称", "文件大小").forEachIndexed { i, n ->
            Row(Modifier.fillMaxWidth().clip(Shapes.box).clickable { prefs.saveSort(i); showSort = false }.padding(8.dp, 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(n, color = if (prefs.sort == i) P.accent else P.text, fontSize = 16.sp, modifier = Modifier.weight(1f))
                if (prefs.sort == i) Ic(R.drawable.ic_check, P.accent, 20)
            }
        }
    }
    menuDoc?.let { d ->
        Sheet({ menuDoc = null }) {
            Text(d.title, color = P.text, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(start = 8.dp, bottom = 8.dp, top = 2.dp))
            Text("${if (d.type == DocType.MD) "Markdown" else "网页"} · ${fmtSize(d.size)} · ${d.source}", color = P.text3, fontSize = 13.sp, modifier = Modifier.padding(start = 8.dp, bottom = 8.dp))
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
    Row(modifier.fillMaxWidth().widthIn(max = 520.dp).height(64.dp).glass(CircleShape, floating = true).padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Tab.entries.forEachIndexed { i, t ->
            val on = vm.tab == t
            Column(Modifier.weight(1f).height(48.dp).clip(CircleShape).background(if (on) pal.accent.copy(alpha = 0.11f) else Color.Transparent)
                .selectable(on, role = androidx.compose.ui.semantics.Role.Tab) { vm.tab = t; vm.searching = false },
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically)) {
                Ic(tabIcons[i], if (on) pal.accent else pal.text2, 22)
                Text(tabNames[i], color = if (on) pal.accent else pal.text2, fontSize = 10.sp, lineHeight = 12.sp, fontWeight = if (on) FontWeight.SemiBold else FontWeight.Medium)
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

/** 搜索栏右侧的圆形「+」：导入入口（取代原来的悬浮按钮）。 */
@Composable
private fun ImportPlus(onClick: () -> Unit) {
    Box(Modifier.size(48.dp).glass(CircleShape, floating = true)
        .clickable(role = androidx.compose.ui.semantics.Role.Button, onClick = onClick).semantics { contentDescription = "导入" }, contentAlignment = Alignment.Center) {
        Ic(R.drawable.ic_plus, P.accent, 22)
    }
}

// ---------------- 阅读 tab ----------------
@Composable
private fun ReadTab(vm: AppVm, docs: List<Doc>, onMenu: (Doc) -> Unit, onImport: () -> Unit) {
    val pal = P
    val current = remember(docs) { docs.filter { it.progress in 0.001f..0.985f }.maxByOrNull { it.openedAt } ?: docs.maxByOrNull { it.openedAt } }
    val recent = remember(docs, current) { docs.sortedByDescending { it.createdAt }.filter { it.id != current?.id }.take(8) }
    val favs = remember(docs) { docs.filter { it.fav }.sortedByDescending { it.openedAt }.take(5) }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 8.dp, bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 120.dp)) {
        item(key = "search") {
            Row(Modifier.padding(horizontal = 16.dp).widthIn(max = 720.dp).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                SearchPill("", {}, false, hint = "在书库中搜索", onActivate = { vm.tab = Tab.Library; vm.searching = true }, modifier = Modifier.weight(1f), readOnly = true)
                ImportPlus(onImport)
            }
        }
        if (docs.isEmpty()) item(key = "empty") { EmptyState(true, false, onImport) }
        current?.let { c ->
            item(key = "h1") { SectionTitle("继续阅读", null) {} }
            item(key = "cont") {
                Row(Modifier.padding(horizontal = 16.dp).widthIn(max = 720.dp).fillMaxWidth().soft(Squircle(24.dp), 1)
                    .clip(Squircle(24.dp)).background(pal.surface).clickable { vm.openDoc(c.id) }.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    TypoCover(c, CoverSize.Hero, Modifier.size(84.dp, 112.dp))
                    Column(Modifier.weight(1f).height(112.dp).padding(vertical = 2.dp), verticalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text(c.source, color = pal.text3, fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(c.title, color = pal.text, style = MaterialTheme.typography.titleMedium, maxLines = 3, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 4.dp))
                        }
                        Column {
                            Box(Modifier.fillMaxWidth().height(3.dp).clip(CircleShape).background(pal.hair)) { Box(Modifier.fillMaxWidth(c.progress.coerceIn(0f, 1f)).fillMaxHeight().background(pal.accent)) }
                            Text(if (c.progress < 0.005f) "未开始 · 约 ${readMinutes(c.size)} 分钟" else "已读 ${(c.progress * 100).toInt()}% · 还剩约 ${(readMinutes(c.size) * (1 - c.progress)).toInt().coerceAtLeast(1)} 分钟", color = pal.text2, fontSize = 12.sp, lineHeight = 16.sp, modifier = Modifier.padding(top = 8.dp))
                        }
                    }
                }
            }
        }
        if (recent.isNotEmpty()) {
            item(key = "h2") { SectionTitle("最近添加", "全部") { vm.filter = Filter.All; vm.tab = Tab.Library } }
            item(key = "recent") {
                LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(recent.size, key = { recent[it].id }) { i ->
                        val d = recent[i]
                        Column(Modifier.width(112.dp).combinedClickable(onClick = { vm.openDoc(d.id) }, onLongClick = { onMenu(d) })) {
                            TypoCover(d, CoverSize.Card, Modifier.size(112.dp, 150.dp))
                            Text("${sourceName(d.source)} · ${coverDate(d.createdAt)}", color = pal.text3, fontSize = 12.sp, lineHeight = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 8.dp))
                        }
                    }
                }
            }
        }
        if (favs.isNotEmpty()) {
            item(key = "h3") { SectionTitle("收藏", "全部") { vm.filter = Filter.Fav; vm.tab = Tab.Library } }
            items(favs.size, key = { "fav" + favs[it].id }) { i ->
                val d = favs[i]
                Row(Modifier.widthIn(max = 720.dp).fillMaxWidth().combinedClickable(onClick = { vm.openDoc(d.id) }, onLongClick = { onMenu(d) }).padding(horizontal = 20.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    TypoCover(d, CoverSize.Mini, Modifier.size(44.dp, 58.dp))
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f)) {
                        Text(d.title, color = pal.text, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Text("${sourceName(d.source)} · ${coverDate(d.openedAt)} · ${readMinutes(d.size)} 分钟", color = pal.text3, fontSize = 12.sp, lineHeight = 16.sp, modifier = Modifier.padding(top = 4.dp))
                    }
                    Spacer(Modifier.width(16.dp))
                    Ic(R.drawable.ic_bookmark_fill, pal.accent, 20)
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(title: String, action: String?, onAction: () -> Unit) {
    Row(Modifier.padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 12.dp).widthIn(max = 720.dp).fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
        Text(title, color = P.text, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
        if (action != null) Text(action, color = P.accent, fontSize = 15.sp, lineHeight = 22.sp, fontWeight = FontWeight.Medium, modifier = Modifier.clip(Shapes.chip).clickable(onClick = onAction).padding(start = 8.dp, top = 4.dp, bottom = 0.dp))
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
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 8.dp, bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 120.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp), verticalItemSpacing = 16.dp,
    ) {
        item(span = StaggeredGridItemSpan.FullLine, key = "head") {
            Column {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    SearchPill(vm.query, { vm.query = it }, vm.searching, hint = "搜索标题或内容", onActivate = { vm.searching = true }, modifier = Modifier.weight(1f))
                    ImportPlus(onImport)
                }
                Spacer(Modifier.height(16.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(horizontal = 2.dp)) {
                    items(Filter.entries.size, key = { Filter.entries[it].name }) { i -> val f = Filter.entries[i]; Chip(f.label, vm.filter == f) { vm.filter = f } }
                }
                Row(Modifier.padding(top = 8.dp, start = 4.dp), verticalAlignment = Alignment.CenterVertically) {
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
private fun SearchPill(q: String, onQ: (String) -> Unit, active: Boolean, hint: String, onActivate: () -> Unit, modifier: Modifier = Modifier, readOnly: Boolean = false) {
    val focus = LocalFocusManager.current
    val req = remember { androidx.compose.ui.focus.FocusRequester() }
    LaunchedEffect(active) { if (active && !readOnly) runCatching { req.requestFocus() } }
    Row(modifier.height(48.dp).glass(CircleShape, floating = true)
        .clickable(onClick = onActivate).padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
        Ic(R.drawable.ic_search, P.text3, 20); Spacer(Modifier.width(8.dp))
        Box(Modifier.weight(1f)) {
            if (q.isEmpty()) Text(hint, color = P.text3, fontSize = 15.sp, letterSpacing = (-0.1).sp)
            if (!readOnly) BasicTextField(q, onQ, singleLine = true, textStyle = androidx.compose.ui.text.TextStyle(color = P.text, fontSize = 15.sp), cursorBrush = SolidColor(P.accent),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search), keyboardActions = KeyboardActions(onSearch = { focus.clearFocus() }),
                modifier = Modifier.fillMaxWidth().focusRequester(req))
        }
        if (q.isNotEmpty()) Box(Modifier.size(28.dp).clip(CircleShape).clickable { onQ("") }, contentAlignment = Alignment.Center) { Ic(R.drawable.ic_close, P.text3, 18) }
    }
}

@Composable
fun BackHandlerIf(enabled: Boolean, onBack: () -> Unit) = androidx.activity.compose.BackHandler(enabled = enabled, onBack = onBack)

@Composable
fun TextPrompt(title: String, initial: String, ok: String, onOk: (String) -> Unit, onDismiss: () -> Unit) {
    var v by remember { mutableStateOf(initial) }
    AlertDialog(onDismissRequest = onDismiss, containerColor = P.surface, shape = Squircle(24.dp),
        title = { Text(title, color = P.text, fontWeight = FontWeight.SemiBold, fontSize = 18.sp) },
        text = {
            BasicTextField(v, { v = it }, singleLine = true, textStyle = androidx.compose.ui.text.TextStyle(color = P.text, fontSize = 16.sp), cursorBrush = SolidColor(P.accent),
                modifier = Modifier.fillMaxWidth().clip(Shapes.box).background(P.field).padding(16.dp))
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
        if (none) { Spacer(Modifier.height(24.dp)); PillButton("导入文件", R.drawable.ic_import, onImport) }
    }
}

fun tint(d: Doc, pal: Pal): Pair<Color, Color> = if (d.type == DocType.MD) pal.accentSoft to pal.accent else (if (pal.dark) Color(0xFF2A2440) else Color(0xFFEFEAFF)) to (if (pal.dark) Color(0xFFB7A6FF) else Color(0xFF6A4DFF))

/** 书库卡片：排版封面（撑满列宽，按 112:150 的比例整体放大）+ 一行「来源 · 日期」和收藏书签。 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DocCard(d: Doc, onClick: () -> Unit, onLong: () -> Unit, onFav: () -> Unit) {
    val pal = P
    Column(Modifier.fillMaxWidth().combinedClickable(onClick = onClick, onLongClick = onLong)) {
        TypoCover(d, CoverSize.Card, Modifier.fillMaxWidth().aspectRatio(112f / 150f))
        Row(Modifier.padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("${sourceName(d.source)} · ${coverDate(d.createdAt)}", color = pal.text3, fontSize = 12.sp, lineHeight = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            Box(Modifier.size(32.dp).clip(CircleShape).clickable(onClick = onFav).semantics { contentDescription = if (d.fav) "取消收藏" else "收藏" }, contentAlignment = Alignment.Center) {
                Ic(if (d.fav) R.drawable.ic_bookmark_fill else R.drawable.ic_bookmark, if (d.fav) pal.accent else pal.text3, 18)
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DocRow(d: Doc, onClick: () -> Unit, onLong: () -> Unit) {
    val pal = P
    Row(Modifier.fillMaxWidth().soft(Shapes.card, 1).clip(Shapes.card).background(pal.surface)
        .combinedClickable(onClick = onClick, onLongClick = onLong).padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        TypoCover(d, CoverSize.Mini, Modifier.size(44.dp, 58.dp))
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(d.title, color = pal.text, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text("${if (d.type == DocType.MD) "Markdown" else "网页"} · ${coverDate(d.openedAt)} · ${fmtSize(d.size)}", color = pal.text3, fontSize = 12.sp, lineHeight = 16.sp, maxLines = 1, modifier = Modifier.padding(top = 4.dp))
        }
        if (d.fav) { Spacer(Modifier.width(16.dp)); Ic(R.drawable.ic_bookmark_fill, pal.accent, 20) }
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
                Box(Modifier.size(180.dp).soft(CircleShape, 2).clip(CircleShape).background(pal.surface), contentAlignment = Alignment.Center) {
                    Box(Modifier.size(112.dp).clip(CircleShape).background(pal.accentSoft), contentAlignment = Alignment.Center) { Ic(ic, pal.accent, 52) }
                }
                Spacer(Modifier.height(44.dp))
                Text(t, color = pal.text, fontSize = 28.sp, lineHeight = 36.sp, letterSpacing = (-0.5).sp, fontWeight = FontWeight.Bold, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                Text(s, color = pal.text2, fontSize = 15.sp, lineHeight = 23.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center, modifier = Modifier.padding(top = 16.dp, start = 8.dp, end = 8.dp))
            }
        }
        Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.Center) {
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
