package com.scarlett.shiye

import android.graphics.Bitmap
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File

private fun captionOf(doc: Doc): String {
    val tags = Markdown.tags(Markdown.stripFrontMatter(Library.readText(doc)).second).take(5).joinToString(" ") { "#$it" }
    return buildString { append(doc.title); if (doc.snippet.isNotBlank()) append("\n\n").append(doc.snippet.lineSequence().first().take(120)); if (tags.isNotBlank()) append("\n\n").append(tags) }
}

@Composable
private fun Spin(size: Int = 28) = CircularProgressIndicator(color = P.accent, strokeWidth = 2.5.dp, modifier = Modifier.size(size.dp))

@Composable
private fun Label(t: String) = Text(t, color = P.text2, style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 16.dp, bottom = 10.dp))

/** 底部「⋯」小圆钮 / 下载钮：描边圆形 */
@Composable
private fun RoundOutline(icon: Int, desc: String, enabled: Boolean = true, onClick: () -> Unit) {
    Box(Modifier.size(50.dp).clip(CircleShape).border(1.dp, P.line, CircleShape).clickable(enabled = enabled, role = androidx.compose.ui.semantics.Role.Button, onClick = onClick).semantics { contentDescription = desc }, contentAlignment = Alignment.Center) {
        Ic(icon, if (enabled) P.text else P.text3, 22)
    }
}

/**
 * 导出与分享：阅读页里的一张面板，四个互相独立的入口——小红书 / 长图 / Notion / 文件。
 * tab：0 小红书 1 长图 2 Notion 3 文件
 */
@Composable
fun ExportSheet(vm: AppVm, doc: Doc, startTab: Int = 0, onDismiss: () -> Unit) {
    val prefs = vm.prefs
    var tab by rememberSaveable { mutableIntStateOf(startTab) }
    var prepared by remember { mutableStateOf<Prepared?>(null) }
    var err by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(doc.id, prefs.mdNet) {
        prepared = runCatching { withContext(Dispatchers.IO) { Exporter.prepare(doc, Library.readText(doc), prefs.mdNet) } }.getOrElse { err = "没能读取文档"; null }
    }
    val tag = remember { java.util.UUID.randomUUID().toString().take(8) }
    val ctx = LocalContext.current
    DisposableEffect(Unit) { onDispose { runCatching { File(ctx.cacheDir, "export/$tag").deleteRecursively() } } }
    val names = listOf("小红书", "长图", "Notion", "文件")
    val icons = listOf(R.drawable.ic_cards, R.drawable.ic_long_image, R.drawable.ic_notion, R.drawable.ic_doc)

    Sheet(onDismiss) {
        Row(Modifier.padding(start = 4.dp, top = 2.dp, bottom = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("导出与分享", color = P.text, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), modifier = Modifier.weight(1f))
            Box(Modifier.size(40.dp).clip(CircleShape).clickable(onClick = onDismiss).semantics { contentDescription = "关闭" }, contentAlignment = Alignment.Center) { Ic(R.drawable.ic_close, P.text, 22) }
        }
        // 四个分段：连在一起的分段按钮，选中项主色实心
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            names.forEachIndexed { i, n ->
                val on = tab == i
                val shape = when (i) { 0 -> RoundedCornerShape(24.dp, 8.dp, 8.dp, 24.dp); 3 -> RoundedCornerShape(8.dp, 24.dp, 24.dp, 8.dp); else -> RoundedCornerShape(8.dp) }
                val sel = if (on) RoundedCornerShape(24.dp) else shape
                Row(Modifier.weight(1f).height(46.dp).clip(sel).background(if (on) P.accent else P.field).clickable(role = androidx.compose.ui.semantics.Role.Tab) { tab = i },
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                    Ic(icons[i], if (on) P.onAccent else P.text, 18); Spacer(Modifier.width(5.dp))
                    Text(n, color = if (on) P.onAccent else P.text, style = MaterialTheme.typography.labelLarge.copy(fontWeight = if (on) FontWeight.Bold else FontWeight.Medium), maxLines = 1)
                }
            }
        }
        when (tab) {
            0 -> XhsPanel(vm, doc, prepared, err, tag)
            1 -> LongPanel(vm, doc, prepared, err, tag)
            2 -> NotionPanel(vm, doc, onDismiss)
            else -> FilePanel(vm, doc)
        }
    }
}

/** 小红书图文：只出分页卡片。竖版 3:4 / 9:16 二选一，共 7 套版式。 */
@Composable
private fun ColumnScope.XhsPanel(vm: AppVm, doc: Doc, prepared: Prepared?, err0: String?, tag: String) {
    val prefs = vm.prefs
    val ctx = LocalContext.current
    val styles = XhsStyles.all
    var styleIx by rememberSaveable { mutableIntStateOf(prefs.xhsStyle.coerceIn(0, styles.lastIndex)) }
    var ratio by rememberSaveable { mutableIntStateOf(prefs.xhsRatio.coerceIn(0, 1)) }
    var title by rememberSaveable(doc.id) { mutableStateOf(doc.title) }
    var titleDebounced by remember { mutableStateOf(title) }
    LaunchedEffect(title) { delay(450); titleDebounced = title }
    var files by remember { mutableStateOf<List<File>>(emptyList()) }
    var thumbs by remember { mutableStateOf<List<Bitmap>>(emptyList()) }
    var covers by remember { mutableStateOf<List<Bitmap>>(emptyList()) }
    var truncated by remember { mutableStateOf(false) }
    var working by remember { mutableStateOf(true) }
    var err by remember { mutableStateOf(err0) }
    var editTitle by remember { mutableStateOf(false) }
    var more by remember { mutableStateOf(false) }
    val typo = Typo(prefs.lineHeight, prefs.paraSpacing)
    LaunchedEffect(prepared, ratio, titleDebounced) {
        val p = prepared ?: return@LaunchedEffect
        covers = withContext(Dispatchers.Default) { styles.map { runCatching { Exporter.coverThumb(p, it, ratio, titleDebounced) }.getOrNull() ?: Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888) } }
    }
    LaunchedEffect(prepared, styleIx, ratio, titleDebounced, prefs.lineHeight, prefs.paraSpacing) {
        val p = prepared ?: return@LaunchedEffect
        working = true; err = null
        delay(120)
        runCatching { withContext(Dispatchers.Default) { Exporter.renderCards(p, styles[styleIx], ratio, titleDebounced, typo, File(ctx.cacheDir, "export/$tag/cards")) } }
            .onSuccess { files = it.files; thumbs = it.thumbs; truncated = it.truncated }
            .onFailure { err = "生成失败：${it.message ?: "内存不足，试试短一点的文档"}" }
        working = false
    }
    val caption = remember(doc) { captionOf(doc) }
    val ready = files.isNotEmpty() && !working

    Box(Modifier.fillMaxWidth().height(232.dp).padding(top = 16.dp), contentAlignment = Alignment.Center) {
        when {
            err != null -> Text(err!!, color = P.danger, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(30.dp))
            thumbs.isEmpty() -> Spin()
            else -> LazyRow(Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                itemsIndexed(thumbs) { i, b ->
                    Box(Modifier.fillMaxHeight().aspectRatio(b.width.toFloat() / b.height).shadow(12.dp, RoundedCornerShape(14.dp), ambientColor = Color(0x1A1B2A4D), spotColor = Color(0x261B2A4D)).clip(RoundedCornerShape(14.dp))) {
                        Image(b.asImageBitmap(), "第${i + 1}张", Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
                    }
                }
            }
        }
        if (working && thumbs.isNotEmpty()) Box(Modifier.matchParentSize().background(P.surface.copy(alpha = 0.55f)), contentAlignment = Alignment.Center) { Spin() }
    }
    Text(if (thumbs.isEmpty()) "正在生成…" else "共 ${thumbs.size} 页 · 点这里修改封面标题" + if (truncated) " · 已截取前 18 页" else "", color = P.text3, style = MaterialTheme.typography.bodySmall,
        modifier = Modifier.align(Alignment.CenterHorizontally).clip(Shapes.chip).clickable { editTitle = true }.padding(horizontal = 12.dp, vertical = 6.dp))

    Label("样式")
    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(horizontal = 2.dp, vertical = 3.dp)) {
        itemsIndexed(styles) { i, s ->
            val on = styleIx == i
            Column(Modifier.width(62.dp).clip(RoundedCornerShape(12.dp)).clickable { styleIx = i; prefs.saveXhsStyle(i) }, horizontalAlignment = Alignment.CenterHorizontally) {
                val bmp = covers.getOrNull(i)
                Box(Modifier.fillMaxWidth().aspectRatio(0.76f).clip(RoundedCornerShape(11.dp)).background(Color(s.paper)).border(if (on) 2.5.dp else 0.8.dp, if (on) P.accent else P.line, RoundedCornerShape(11.dp))) {
                    if (bmp != null) Image(bmp.asImageBitmap(), s.name, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                }
                Text(s.name, color = if (on) P.accent else P.text2, style = MaterialTheme.typography.labelMedium.copy(fontWeight = if (on) FontWeight.Bold else FontWeight.Medium), maxLines = 1, softWrap = false, modifier = Modifier.padding(top = 7.dp))
            }
        }
    }
    Label("比例")
    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        listOf("3:4 · 1080×1440", "9:16 · 1080×1920").forEachIndexed { i, t ->
            val on = ratio == i
            Row(Modifier.weight(1f).height(44.dp).clip(if (on) RoundedCornerShape(22.dp) else if (i == 0) RoundedCornerShape(22.dp, 8.dp, 8.dp, 22.dp) else RoundedCornerShape(8.dp, 22.dp, 22.dp, 8.dp))
                .background(if (on) P.accent else P.field).clickable { ratio = i; prefs.saveXhsRatio(i) }, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                if (on) { Ic(R.drawable.ic_check, P.onAccent, 17); Spacer(Modifier.width(5.dp)) }
                Text(t, color = if (on) P.onAccent else P.text, style = MaterialTheme.typography.labelLarge.copy(fontWeight = if (on) FontWeight.Bold else FontWeight.Medium), maxLines = 1)
            }
        }
    }
    Spacer(Modifier.height(18.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
        PillButton("发到小红书", R.drawable.ic_send, {
            val direct = Share.toXhs(ctx, files, caption)
            vm.toast(if (direct) "标题和文案已复制，发布页长按粘贴即可" else "没找到小红书，请在列表里选 App；文案已复制")
        }, Modifier.weight(1f), enabled = ready)
        RoundOutline(R.drawable.ic_download, "存到相册", ready) {
            val n = runCatching { Share.saveToGallery(ctx, files) }.getOrDefault(0)
            vm.toast(if (n > 0) "已保存 $n 张到相册「拾页」" else "保存失败")
        }
        Box {
            RoundOutline(R.drawable.ic_more, "更多分享", true) { more = true }
            DropdownMenu(more, { more = false }, containerColor = P.surface, shape = RoundedCornerShape(16.dp)) {
                DropdownMenuItem(text = { Text("发给微信") }, leadingIcon = { Ic(R.drawable.ic_wechat, P.text2, 20) }, onClick = { more = false; if (ready) Share.toWechat(ctx, files) })
                DropdownMenuItem(text = { Text("更多分享") }, leadingIcon = { Ic(R.drawable.ic_share, P.text2, 20) }, onClick = { more = false; if (ready) Share.toAny(ctx, files) })
                DropdownMenuItem(text = { Text("复制文案") }, leadingIcon = { Ic(R.drawable.ic_copy, P.text2, 20) }, onClick = { more = false; Share.copyText(ctx, caption); vm.toast("文案已复制") })
            }
        }
    }
    if (editTitle) TextPrompt("封面标题", title, "好", { title = it.take(60).ifBlank { doc.title } }, { editTitle = false })
}

/** 长图：就是一张连续的长图，不分页。 */
@Composable
private fun LongPanel(vm: AppVm, doc: Doc, prepared: Prepared?, err0: String?, tag: String) {
    val prefs = vm.prefs
    val ctx = LocalContext.current
    var styleIx by rememberSaveable { mutableIntStateOf(prefs.exportStyle.coerceIn(0, ExStyles.all.lastIndex)) }
    var out by remember { mutableStateOf<Exporter.LongOut?>(null) }
    var working by remember { mutableStateOf(true) }
    var err by remember { mutableStateOf(err0) }
    var more by remember { mutableStateOf(false) }
    val typo = Typo(prefs.lineHeight, prefs.paraSpacing)
    LaunchedEffect(prepared, styleIx, prefs.lineHeight, prefs.paraSpacing) {
        val p = prepared ?: return@LaunchedEffect
        working = true; err = null
        delay(120)
        runCatching { withContext(Dispatchers.Default) { Exporter.renderLong(p, ExStyles.all[styleIx], typo, File(ctx.cacheDir, "export/$tag/${Importer.safeName(doc.title).ifBlank { "长图" }}.png")) } }
            .onSuccess { out = it }.onFailure { err = "生成失败：${it.message ?: "内存不足，试试短一点的文档"}" }
        working = false
    }
    val caption = remember(doc) { captionOf(doc) }
    val files = listOfNotNull(out?.file)
    val ready = files.isNotEmpty() && !working

    Row(Modifier.fillMaxWidth().padding(top = 18.dp), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.width(112.dp).height(168.dp).shadow(12.dp, RoundedCornerShape(14.dp), ambientColor = Color(0x1A1B2A4D), spotColor = Color(0x261B2A4D)).clip(RoundedCornerShape(14.dp)).background(P.field), contentAlignment = Alignment.Center) {
            val o = out
            when {
                err != null -> Text("失败", color = P.danger, style = MaterialTheme.typography.bodySmall)
                o == null -> Spin(24)
                else -> Image(o.thumb.asImageBitmap(), "长图预览", Modifier.fillMaxSize(), contentScale = ContentScale.Crop, alignment = Alignment.TopCenter)
            }
            if (working && out != null) Box(Modifier.matchParentSize().background(P.surface.copy(alpha = 0.55f)), contentAlignment = Alignment.Center) { Spin(24) }
        }
        Column(Modifier.weight(1f)) {
            Text("一张连续的长图", color = P.text, style = MaterialTheme.typography.titleMedium)
            Text(out?.let { "${it.width} × ${it.height} 像素" } ?: err ?: "正在生成…", color = P.text3, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp))
            Text(if (out?.truncated == true) "内容很长，已截取前面的部分。" else "不分页，适合发朋友圈、微信、长文备份；小红书图文请用左侧的「小红书」。", color = P.text2, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 8.dp))
        }
    }
    Label("样式")
    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(horizontal = 2.dp, vertical = 3.dp)) {
        itemsIndexed(ExStyles.all) { i, s ->
            val on = styleIx == i
            Column(Modifier.width(62.dp).clip(RoundedCornerShape(12.dp)).clickable { styleIx = i; prefs.saveExportStyle(i) }, horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.size(62.dp, 76.dp).clip(RoundedCornerShape(11.dp)).background(Brush.verticalGradient(listOf(Color(s.bgTop), Color(s.bgBottom)))).border(if (on) 2.5.dp else 0.8.dp, if (on) P.accent else P.line, RoundedCornerShape(11.dp)).padding(8.dp)) {
                    Column(Modifier.fillMaxSize().then(if (s.panel) Modifier.clip(RoundedCornerShape(6.dp)).background(Color(s.panelColor)).padding(4.dp) else Modifier), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(Modifier.fillMaxWidth(0.7f).height(6.dp).clip(Shapes.chip).background(if (s.solidCover) Color.White else Color(s.accent)))
                        repeat(4) { Box(Modifier.fillMaxWidth(if (it == 3) 0.5f else 1f).height(3.dp).clip(Shapes.chip).background(Color(if (s.solidCover) 0xFFFFFFFF.toInt() else s.text2).copy(alpha = 0.55f))) }
                    }
                }
                Text(s.name, color = if (on) P.accent else P.text2, style = MaterialTheme.typography.labelMedium.copy(fontWeight = if (on) FontWeight.Bold else FontWeight.Medium), maxLines = 1, softWrap = false, modifier = Modifier.padding(top = 7.dp))
            }
        }
    }
    Spacer(Modifier.height(18.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
        PillButton("分享长图", R.drawable.ic_send, { Share.copyText(ctx, caption); Share.toAny(ctx, files); vm.toast("文案已复制，发布时长按粘贴") }, Modifier.weight(1f), enabled = ready)
        RoundOutline(R.drawable.ic_download, "存到相册", ready) {
            val n = runCatching { Share.saveToGallery(ctx, files) }.getOrDefault(0)
            vm.toast(if (n > 0) "已保存到相册「拾页」" else "保存失败")
        }
        Box {
            RoundOutline(R.drawable.ic_more, "更多", true) { more = true }
            DropdownMenu(more, { more = false }, containerColor = P.surface, shape = RoundedCornerShape(16.dp)) {
                DropdownMenuItem(text = { Text("发给微信") }, leadingIcon = { Ic(R.drawable.ic_wechat, P.text2, 20) }, onClick = { more = false; if (ready) Share.toWechat(ctx, files) })
                DropdownMenuItem(text = { Text("复制文案") }, leadingIcon = { Ic(R.drawable.ic_copy, P.text2, 20) }, onClick = { more = false; Share.copyText(ctx, caption); vm.toast("文案已复制") })
            }
        }
    }
}

@Composable
private fun NotionPanel(vm: AppVm, doc: Doc, onDismiss: () -> Unit) {
    val ctx = LocalContext.current
    val ready = vm.prefs.notionReady
    Column(Modifier.padding(top = 20.dp).fillMaxWidth().clip(Shapes.card).background(P.field).padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(if (doc.notionUrl.isNotBlank()) "已同步过" else if (ready) "同步到你的 Notion" else "还没有配置 Notion", color = P.text, style = MaterialTheme.typography.titleMedium)
        Text(when {
            doc.notionUrl.isNotBlank() -> "上次同步：${fmtAgo(doc.notionAt)}。再次同步会新建一页，不会覆盖原来的内容。"
            ready -> "正文、图片、表格、代码都会按 Notion 的块写入，不用再手动复制。"
            else -> "需要一个内部集成 Token 和目标页面链接，两分钟配好。"
        }, color = P.text2, style = MaterialTheme.typography.bodyMedium)
    }
    Spacer(Modifier.height(18.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        PillButton(if (ready) (if (doc.notionUrl.isNotBlank()) "再同步一次" else "同步到 Notion") else "去配置", R.drawable.ic_notion, { onDismiss(); if (ready) vm.syncNotion(doc) else vm.push(Route.NotionSettings) }, Modifier.weight(1f))
        if (doc.notionUrl.isNotBlank()) PillButton("在 Notion 打开", R.drawable.ic_link, { Share.openUrl(ctx, doc.notionUrl) }, Modifier.weight(1f), primary = false)
    }
}

@Composable
private fun FilePanel(vm: AppVm, doc: Doc) {
    val ctx = LocalContext.current
    val ext = if (doc.type == DocType.HTML) "HTML" else "MD"
    Column(Modifier.padding(top = 20.dp).fillMaxWidth().clip(Shapes.card).background(P.field).padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("分享原文件", color = P.text, style = MaterialTheme.typography.titleMedium)
        Text("以 .${ext.lowercase()} 文件发出，原样不改，微信、邮件、网盘都能收。", color = P.text2, style = MaterialTheme.typography.bodyMedium)
        Text("${doc.title}.${ext.lowercase()} · ${fmtSize(doc.size)}", color = P.text3, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 4.dp))
    }
    Spacer(Modifier.height(18.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        PillButton("分享 $ext 文件", R.drawable.ic_file_out, { if (!runCatching { Share.file(ctx, doc) }.getOrDefault(false)) vm.toast("文件不存在，无法分享") }, Modifier.weight(1f))
        PillButton("复制全文", R.drawable.ic_copy, { Share.copyText(ctx, Library.readText(doc)); vm.toast("已复制全文") }, Modifier.weight(1f), primary = false)
    }
}
