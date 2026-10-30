package com.scarlett.shiye

import android.content.ClipboardManager
import android.content.Context
import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(vm: AppVm) {
    val prefs = vm.prefs
    if (!prefs.onboarded) { Onboarding(vm); return }
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val docs by Library.docs.collectAsState()
    var query by rememberSaveable { mutableStateOf("") }
    var searching by rememberSaveable { mutableStateOf(false) }
    var filter by rememberSaveable { mutableIntStateOf(0) }
    var menuDoc by remember { mutableStateOf<Doc?>(null) }
    var renameDoc by remember { mutableStateOf<Doc?>(null) }
    var deleteDoc by remember { mutableStateOf<Doc?>(null) }
    var showSort by remember { mutableStateOf(false) }
    var showImport by remember { mutableStateOf(false) }
    val backdrop = remember { Backdrop() }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { if (it.isNotEmpty()) vm.importUris(it) }

    val shown = remember(docs, query, filter, prefs.sort) {
        val q = query.trim().lowercase()
        docs.filter { d ->
            (when (filter) { 1 -> d.type == DocType.MD; 2 -> d.type == DocType.HTML; 3 -> d.fav; else -> true }) &&
                (q.isEmpty() || d.title.lowercase().contains(q) || d.snippet.lowercase().contains(q) || d.source.contains(q))
        }.sortedWith(when (prefs.sort) {
            1 -> compareByDescending<Doc> { it.createdAt }
            2 -> compareBy<Doc> { it.title.lowercase() }
            3 -> compareByDescending<Doc> { it.size }
            else -> compareByDescending<Doc> { it.openedAt }
        })
    }
    val gridState = rememberLazyStaggeredGridState()
    val scrolled by remember { derivedStateOf { gridState.firstVisibleItemIndex > 0 || gridState.firstVisibleItemScrollOffset > 140 } }
    BackHandlerIf(searching) { searching = false; query = "" }

    CompositionLocalProvider(LocalBackdrop provides backdrop) {
        Box(Modifier.fillMaxSize()) {
            LazyVerticalStaggeredGrid(
                columns = StaggeredGridCells.Fixed(if (prefs.grid) 2 else 1), state = gridState,
                modifier = Modifier.fillMaxSize().glassSource(backdrop),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 62.dp, bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 120.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp), verticalItemSpacing = 12.dp,
            ) {
                item(span = StaggeredGridItemSpan.FullLine, key = "head") {
                    Column(Modifier.padding(top = 6.dp, bottom = 6.dp, start = 4.dp, end = 4.dp)) {
                        Text(greeting(), color = P.text, fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
                        Text(subLine(docs.size, docs.count { it.fav }), color = P.text3, fontSize = 13.5.sp, modifier = Modifier.padding(top = 4.dp))
                        Spacer(Modifier.height(18.dp))
                        SearchPill(query, { query = it }, searching, { searching = true })
                        Spacer(Modifier.height(14.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            val names = listOf("全部", "Markdown", "网页", "收藏")
                            items(names.size) { i -> Chip(names[i], filter == i) { filter = i } }
                        }
                        Spacer(Modifier.height(6.dp))
                    }
                }
                if (shown.isEmpty()) {
                    item(span = StaggeredGridItemSpan.FullLine, key = "empty") {
                        EmptyState(docs.isEmpty(), query.isNotBlank(), onImport = { showImport = true })
                    }
                }
                items(shown.size, key = { shown[it].id }) { i ->
                    val d = shown[i]
                    if (prefs.grid) DocCard(d, onClick = { vm.openDoc(d.id) }, onLong = { menuDoc = d }, onFav = { Library.mutate(d.id) { x -> x.copy(fav = !x.fav) } })
                    else DocRow(d, onClick = { vm.openDoc(d.id) }, onLong = { menuDoc = d })
                }
            }
            Row(Modifier.align(Alignment.TopCenter).fillMaxWidth().glass().hairline(if (scrolled) GlassEdge.Bottom else GlassEdge.None).windowInsetsPadding(WindowInsets.statusBars).height(56.dp).padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Wordmark(); Spacer(Modifier.weight(1f))
                CircleBtn(if (prefs.grid) R.drawable.ic_list else R.drawable.ic_grid, if (prefs.grid) "切换为列表" else "切换为卡片", { prefs.saveGrid(!prefs.grid) }, glass = true, size = 38)
                Spacer(Modifier.width(8.dp))
                CircleBtn(R.drawable.ic_sort, "排序", { showSort = true }, glass = true, size = 38)
                Spacer(Modifier.width(8.dp))
                CircleBtn(R.drawable.ic_sliders, "设置", { vm.push(Route.Settings) }, glass = true, size = 38)
            }
            Row(Modifier.align(Alignment.BottomCenter).windowInsetsPadding(WindowInsets.navigationBars).padding(bottom = 22.dp)
                .glass(CircleShape, floating = true).clip(CircleShape).clickable { showImport = true }.height(56.dp).padding(horizontal = 26.dp), verticalAlignment = Alignment.CenterVertically) {
                Ic(R.drawable.ic_plus, P.accent, 22); Spacer(Modifier.width(8.dp))
                Text("导入", color = P.text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }

    if (showImport) Sheet({ showImport = false }) {
        Text("存入拾页", color = P.text, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 6.dp, bottom = 8.dp, top = 4.dp))
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
        Text("排序方式", color = P.text, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 6.dp, bottom = 8.dp, top = 4.dp))
        listOf("最近打开", "最近存入", "名称", "文件大小").forEachIndexed { i, n ->
            Row(Modifier.fillMaxWidth().clip(Shapes.box).clickable { prefs.saveSort(i); showSort = false }.padding(6.dp, 14.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(n, color = if (prefs.sort == i) P.accent else P.text, fontSize = 16.sp, modifier = Modifier.weight(1f))
                if (prefs.sort == i) Ic(R.drawable.ic_check, P.accent, 20)
            }
        }
    }
    menuDoc?.let { d ->
        Sheet({ menuDoc = null }) {
            Text(d.title, color = P.text, fontSize = 18.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(start = 6.dp, bottom = 6.dp, top = 2.dp))
            Text("${if (d.type == DocType.MD) "Markdown" else "网页"} · ${fmtSize(d.size)} · ${d.source}", color = P.text3, fontSize = 12.5.sp, modifier = Modifier.padding(start = 6.dp, bottom = 8.dp))
            SheetAction(if (d.fav) R.drawable.ic_bookmark_fill else R.drawable.ic_bookmark, if (d.fav) "取消收藏" else "收藏") { Library.mutate(d.id) { it.copy(fav = !it.fav) }; menuDoc = null }
            SheetAction(R.drawable.ic_cards, "小红书图文") { menuDoc = null; vm.push(Route.Xhs(d.id)) }
            SheetAction(R.drawable.ic_long_image, "生成长图") { menuDoc = null; vm.push(Route.Long(d.id)) }
            SheetAction(R.drawable.ic_notion, "同步到 Notion") { menuDoc = null; vm.syncNotion(d) }
            SheetAction(R.drawable.ic_text_size, "重命名") { menuDoc = null; renameDoc = d }
            SheetAction(R.drawable.ic_trash, "删除", danger = true) { menuDoc = null; deleteDoc = d }
        }
    }
    renameDoc?.let { d -> TextPrompt("重命名", d.title, "保存", { v -> if (v.isNotBlank()) Library.mutate(d.id) { it.copy(title = v.trim().take(120)) } }, { renameDoc = null }) }
    deleteDoc?.let { d -> Confirm("删除这篇文档？", "「${d.title}」会从拾页移除，原文件不受影响。", "删除", danger = true, onOk = { Library.delete(listOf(d.id)); vm.toast("已删除") }, onDismiss = { deleteDoc = null }) }
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

/** 左上角小字标：圆角蓝块里一页纸的线条 + 「拾页」 */
@Composable
private fun Wordmark() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(26.dp).clip(RoundedCornerShape(8.dp)).background(P.accent), contentAlignment = Alignment.Center) { Ic(R.drawable.ic_doc, P.onAccent, 16) }
        Spacer(Modifier.width(9.dp))
        Text("拾页", color = P.text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp)
    }
}

private fun greeting(): String {
    val h = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
    return when { h < 5 -> "夜深了"; h < 11 -> "早上好"; h < 14 -> "中午好"; h < 18 -> "下午好"; else -> "晚上好" }
}
private fun subLine(n: Int, fav: Int): String {
    val d = java.text.SimpleDateFormat("M月d日 EEEE", java.util.Locale.CHINA).format(java.util.Date())
    return if (n == 0) "$d · 把喜欢的文字收进来慢慢读" else "$d · 共 $n 篇${if (fav > 0) "，$fav 篇收藏" else ""}"
}

@Composable
private fun SearchPill(q: String, onQ: (String) -> Unit, active: Boolean, onActivate: () -> Unit) {
    val focus = LocalFocusManager.current
    val req = remember { androidx.compose.ui.focus.FocusRequester() }
    LaunchedEffect(active) { if (active) runCatching { req.requestFocus() } }
    Row(Modifier.fillMaxWidth().height(54.dp).shadow(10.dp, CircleShape, ambientColor = Color(0x121B2A4D), spotColor = Color(0x1A1B2A4D)).clip(CircleShape).background(P.surface)
        .clickable(onClick = onActivate).padding(horizontal = 18.dp), verticalAlignment = Alignment.CenterVertically) {
        Ic(R.drawable.ic_search, P.text3, 20); Spacer(Modifier.width(10.dp))
        Box(Modifier.weight(1f)) {
            if (q.isEmpty()) Text("搜索标题或内容", color = P.text3, fontSize = 15.5.sp)
            BasicTextField(q, onQ, singleLine = true, textStyle = androidx.compose.ui.text.TextStyle(color = P.text, fontSize = 15.sp), cursorBrush = SolidColor(P.accent),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search), keyboardActions = KeyboardActions(onSearch = { focus.clearFocus() }),
                modifier = Modifier.fillMaxWidth().focusRequester(req))
        }
        if (q.isNotEmpty()) Box(Modifier.size(28.dp).clip(CircleShape).clickable { onQ("") }, contentAlignment = Alignment.Center) { Ic(R.drawable.ic_close, P.text3, 18) }
    }
}

@Composable
private fun EmptyState(none: Boolean, searching: Boolean, onImport: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(top = 56.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(96.dp).clip(CircleShape).background(P.accentSoft), contentAlignment = Alignment.Center) { Ic(if (searching) R.drawable.ic_search else R.drawable.ic_doc, P.accent, 40) }
        Spacer(Modifier.height(20.dp))
        Text(if (searching) "没有找到相关内容" else if (none) "这里还是空的" else "这个分类下暂时没有文档", color = P.text, fontSize = 19.sp, fontWeight = FontWeight.Bold)
        Text(if (none) "从微信或浏览器分享到「拾页」，\n或导入手机里的 md / html 文件。" else "换个关键词或分类试试。", color = P.text3, fontSize = 14.sp, lineHeight = 21.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center, modifier = Modifier.padding(top = 8.dp))
        if (none) { Spacer(Modifier.height(22.dp)); PillButton("导入文件", R.drawable.ic_import, onImport) }
    }
}

private fun tint(d: Doc, pal: Pal): Pair<Color, Color> = if (d.type == DocType.MD) pal.accentSoft to pal.accent else (if (pal.dark) Color(0xFF2A2440) else Color(0xFFEFEAFF)) to (if (pal.dark) Color(0xFFB7A6FF) else Color(0xFF6A4DFF))

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DocCard(d: Doc, onClick: () -> Unit, onLong: () -> Unit, onFav: () -> Unit) {
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
private fun DocRow(d: Doc, onClick: () -> Unit, onLong: () -> Unit) {
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
