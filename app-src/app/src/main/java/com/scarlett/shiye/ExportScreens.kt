package com.scarlett.shiye

import android.graphics.Bitmap
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
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
private fun Spin() = androidx.compose.material3.CircularProgressIndicator(color = P.accent, strokeWidth = 2.5.dp, modifier = Modifier.size(30.dp))

/** 小红书图文：只出分页卡片。竖版 3:4 / 9:16 二选一，共 7 套版式（简约 → 花哨）。 */
@Composable
fun XhsScreen(vm: AppVm, id: String) {
    val prefs = vm.prefs
    val ctx = LocalContext.current
    val doc = Library.get(id)
    if (doc == null) { LaunchedEffect(Unit) { vm.pop() }; return }
    val styles = XhsStyles.all
    var styleIx by rememberSaveable { mutableIntStateOf(prefs.xhsStyle.coerceIn(0, styles.lastIndex)) }
    var ratio by rememberSaveable { mutableIntStateOf(prefs.xhsRatio.coerceIn(0, 1)) }
    var title by rememberSaveable(doc.id) { mutableStateOf(doc.title) }
    var titleDebounced by remember { mutableStateOf(doc.title) }
    LaunchedEffect(title) { delay(450); titleDebounced = title }
    val backdrop = remember { Backdrop() }
    var prepared by remember { mutableStateOf<Prepared?>(null) }
    var files by remember { mutableStateOf<List<File>>(emptyList()) }
    var thumbs by remember { mutableStateOf<List<Bitmap>>(emptyList()) }
    var covers by remember { mutableStateOf<List<Bitmap>>(emptyList()) }
    var truncated by remember { mutableStateOf(false) }
    var working by remember { mutableStateOf(true) }
    var err by remember { mutableStateOf<String?>(null) }
    val typo = Typo(prefs.lineHeight, prefs.paraSpacing)

    LaunchedEffect(doc.id, prefs.mdNet) {
        prepared = runCatching { withContext(Dispatchers.IO) { Exporter.prepare(doc, Library.readText(doc), prefs.mdNet) } }.getOrElse { err = "没能读取文档"; null }
    }
    val tag = remember { java.util.UUID.randomUUID().toString().take(8) }
    // 选择器里每套版式的真实封面缩略图（跟随比例与标题）
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
    DisposableEffect(Unit) { onDispose { runCatching { File(ctx.cacheDir, "export/$tag").deleteRecursively() } } }
    val caption = remember(doc) { captionOf(doc) }

    CompositionLocalProvider(LocalBackdrop provides backdrop) {
        Box(Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize().glassSource(backdrop).verticalScroll(rememberScrollState()).padding(top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 72.dp, bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 190.dp)) {
                Box(Modifier.fillMaxWidth().height(330.dp), contentAlignment = Alignment.Center) {
                    when {
                        err != null -> Text(err!!, color = P.danger, fontSize = 14.sp, modifier = Modifier.padding(30.dp))
                        thumbs.isEmpty() -> Spin()
                        else -> LazyRow(Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            itemsIndexed(thumbs) { i, b ->
                                Box(Modifier.height(300.dp).aspectRatio(b.width.toFloat() / b.height).shadow(14.dp, RoundedCornerShape(14.dp), ambientColor = Color(0x1A1B2A4D), spotColor = Color(0x261B2A4D)).clip(RoundedCornerShape(14.dp))) {
                                    Image(b.asImageBitmap(), "第${i + 1}张", Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
                                    Text("${i + 1}/${thumbs.size}", color = Color.White, fontSize = 10.5.sp, modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 7.dp).clip(Shapes.chip).background(Color.Black.copy(alpha = 0.45f)).padding(horizontal = 8.dp, vertical = 2.dp))
                                }
                            }
                        }
                    }
                    if (working && thumbs.isNotEmpty()) Box(Modifier.matchParentSize().background(P.bg.copy(alpha = 0.5f)), contentAlignment = Alignment.Center) { Spin() }
                }
                if (truncated) Text("内容较长，图文最多 18 张，已截取前面的部分；完整内容可以用「长图」。", color = P.text3, fontSize = 12.5.sp, modifier = Modifier.padding(horizontal = 24.dp, vertical = 6.dp))

                Label("尺寸")
                Segmented(listOf("3:4  ·  1080×1440", "9:16  ·  1080×1920"), ratio, { ratio = it; prefs.saveXhsRatio(it) }, Modifier.padding(horizontal = 20.dp).fillMaxWidth())

                Label("版式")
                val list = styles.withIndex().toList()
                LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    itemsIndexed(list) { _, (i, s) ->
                        val on = styleIx == i
                        Column(Modifier.width(86.dp).clip(RoundedCornerShape(14.dp)).clickable { styleIx = i; prefs.saveXhsStyle(i) }, horizontalAlignment = Alignment.CenterHorizontally) {
                            val bmp = covers.getOrNull(i)
                            Box(Modifier.fillMaxWidth().aspectRatio(if (ratio == 1) 9f / 16f else 3f / 4f).clip(RoundedCornerShape(12.dp)).background(Color(s.paper)).border(if (on) 2.5.dp else 0.8.dp, if (on) P.accent else P.line, RoundedCornerShape(12.dp))) {
                                if (bmp != null) Image(bmp.asImageBitmap(), s.name, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                            }
                            Text(s.name, color = if (on) P.accent else P.text, fontSize = 13.5.sp, fontWeight = if (on) FontWeight.SemiBold else FontWeight.Normal, modifier = Modifier.padding(top = 8.dp))
                            Text(s.desc, color = P.text3, fontSize = 11.sp, maxLines = 1)
                        }
                    }
                }

                Label("封面标题")
                Box(Modifier.padding(horizontal = 20.dp).fillMaxWidth().clip(Shapes.card).background(P.field).border(0.8.dp, P.line, Shapes.card).padding(horizontal = 16.dp, vertical = 14.dp)) {
                    if (title.isEmpty()) Text("用文档标题", color = P.text3, fontSize = 15.sp)
                    BasicTextField(title, { title = it.take(60) }, textStyle = TextStyle(color = P.text, fontSize = 15.sp), cursorBrush = SolidColor(P.accent), modifier = Modifier.fillMaxWidth())
                }

            }
            TopBar("小红书图文", { vm.pop() }, Modifier.align(Alignment.TopCenter))
            Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth().glass(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)).hairline(GlassEdge.Top).windowInsetsPadding(WindowInsets.navigationBars).padding(horizontal = 20.dp, vertical = 16.dp)) {
                val ready = files.isNotEmpty() && !working
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    PillButton("发到小红书", R.drawable.ic_cards, {
                        val direct = Share.toXhs(ctx, files, caption)
                        vm.toast(if (direct) "标题和文案已复制，发布页长按粘贴即可" else "没找到小红书，请在列表里选 App；文案已复制")
                    }, Modifier.weight(1f), enabled = ready)
                    PillButton("存到相册", R.drawable.ic_import, {
                        val n = runCatching { Share.saveToGallery(ctx, files) }.getOrDefault(0)
                        vm.toast(if (n > 0) "已保存 $n 张到相册「拾页」" else "保存失败")
                    }, Modifier.weight(1f), primary = false, enabled = ready)
                }
                Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Chip("发给微信", false) { if (ready) Share.toWechat(ctx, files) }
                    Chip("更多分享", false) { if (ready) Share.toAny(ctx, files) }
                    Chip("复制文案", false) { Share.copyText(ctx, caption); vm.toast("文案已复制") }
                }
            }
        }
    }
}

@Composable
private fun Label(t: String) = Text(t, color = P.text3, fontSize = 12.5.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(start = 26.dp, top = 22.dp, bottom = 10.dp))

/** 长图：就是一张连续的长图，不分页。 */
@Composable
fun LongScreen(vm: AppVm, id: String) {
    val prefs = vm.prefs
    val ctx = LocalContext.current
    val doc = Library.get(id)
    if (doc == null) { LaunchedEffect(Unit) { vm.pop() }; return }
    var styleIx by rememberSaveable { mutableIntStateOf(prefs.exportStyle.coerceIn(0, ExStyles.all.lastIndex)) }
    val backdrop = remember { Backdrop() }
    var prepared by remember { mutableStateOf<Prepared?>(null) }
    var out by remember { mutableStateOf<Exporter.LongOut?>(null) }
    var working by remember { mutableStateOf(true) }
    var err by remember { mutableStateOf<String?>(null) }
    val typo = Typo(prefs.lineHeight, prefs.paraSpacing)
    LaunchedEffect(doc.id, prefs.mdNet) {
        prepared = runCatching { withContext(Dispatchers.IO) { Exporter.prepare(doc, Library.readText(doc), prefs.mdNet) } }.getOrElse { err = "没能读取文档"; null }
    }
    val tag = remember { java.util.UUID.randomUUID().toString().take(8) }
    LaunchedEffect(prepared, styleIx, prefs.lineHeight, prefs.paraSpacing) {
        val p = prepared ?: return@LaunchedEffect
        working = true; err = null
        delay(120)
        runCatching { withContext(Dispatchers.Default) { Exporter.renderLong(p, ExStyles.all[styleIx], typo, File(ctx.cacheDir, "export/$tag/${Importer.safeName(doc.title).ifBlank { "长图" }}.png")) } }
            .onSuccess { out = it }.onFailure { err = "生成失败：${it.message ?: "内存不足，试试短一点的文档"}" }
        working = false
    }
    DisposableEffect(Unit) { onDispose { runCatching { File(ctx.cacheDir, "export/$tag").deleteRecursively() } } }
    val caption = remember(doc) { captionOf(doc) }
    val files = listOfNotNull(out?.file)

    CompositionLocalProvider(LocalBackdrop provides backdrop) {
        Box(Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize().glassSource(backdrop).verticalScroll(rememberScrollState()).padding(top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 72.dp, bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 190.dp)) {
                Label("版式")
                LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    itemsIndexed(ExStyles.all) { i, s ->
                        val on = styleIx == i
                        Column(Modifier.width(88.dp).clip(RoundedCornerShape(16.dp)).clickable { styleIx = i; prefs.saveExportStyle(i) }, horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(Modifier.size(88.dp, 108.dp).clip(RoundedCornerShape(16.dp)).background(Brush.verticalGradient(listOf(Color(s.bgTop), Color(s.bgBottom)))).border(if (on) 2.dp else 0.8.dp, if (on) P.accent else P.line, RoundedCornerShape(16.dp)).padding(10.dp)) {
                                Column(Modifier.fillMaxSize().then(if (s.panel) Modifier.clip(RoundedCornerShape(8.dp)).background(Color(s.panelColor)).padding(6.dp) else Modifier), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                    Box(Modifier.fillMaxWidth(0.7f).height(7.dp).clip(Shapes.chip).background(if (s.solidCover) Color.White else Color(s.accent)))
                                    repeat(4) { Box(Modifier.fillMaxWidth(if (it == 3) 0.5f else 1f).height(4.dp).clip(Shapes.chip).background(Color(if (s.solidCover) 0xFFFFFFFF.toInt() else s.text2).copy(alpha = 0.55f))) }
                                }
                            }
                            Text(s.name, color = if (on) P.accent else P.text, fontSize = 13.5.sp, fontWeight = if (on) FontWeight.SemiBold else FontWeight.Normal, modifier = Modifier.padding(top = 8.dp))
                            Text(s.desc, color = P.text3, fontSize = 11.sp)
                        }
                    }
                }
                Label("预览" + (out?.let { "  ·  ${it.width}×${it.height}" } ?: ""))
                Box(Modifier.fillMaxWidth().padding(horizontal = 40.dp), contentAlignment = Alignment.TopCenter) {
                    val o = out
                    when {
                        err != null -> Text(err!!, color = P.danger, fontSize = 14.sp, modifier = Modifier.padding(30.dp))
                        o == null -> Box(Modifier.fillMaxWidth().height(300.dp), contentAlignment = Alignment.Center) { Spin() }
                        else -> Box(Modifier.fillMaxWidth().shadow(14.dp, RoundedCornerShape(12.dp), ambientColor = Color(0x1A1B2A4D), spotColor = Color(0x261B2A4D)).clip(RoundedCornerShape(12.dp))) {
                            Image(o.thumb.asImageBitmap(), "长图预览", Modifier.fillMaxWidth().aspectRatio(o.width.toFloat() / o.height), contentScale = ContentScale.FillWidth)
                        }
                    }
                    if (working && out != null) Box(Modifier.matchParentSize().background(P.bg.copy(alpha = 0.5f)), contentAlignment = Alignment.Center) { Spin() }
                }
                if (out?.truncated == true) Text("内容很长，长图已截取前面的部分。", color = P.text3, fontSize = 12.5.sp, modifier = Modifier.padding(horizontal = 24.dp, vertical = 10.dp))
            }
            TopBar("长图", { vm.pop() }, Modifier.align(Alignment.TopCenter))
            Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth().glass(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)).hairline(GlassEdge.Top).windowInsetsPadding(WindowInsets.navigationBars).padding(horizontal = 20.dp, vertical = 16.dp)) {
                val ready = files.isNotEmpty() && !working
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    PillButton("分享长图", R.drawable.ic_share, { Share.copyText(ctx, caption); Share.toAny(ctx, files); vm.toast("文案已复制，发布时长按粘贴") }, Modifier.weight(1f), enabled = ready)
                    PillButton("存到相册", R.drawable.ic_import, {
                        val n = runCatching { Share.saveToGallery(ctx, files) }.getOrDefault(0)
                        vm.toast(if (n > 0) "已保存到相册「拾页」" else "保存失败")
                    }, Modifier.weight(1f), primary = false, enabled = ready)
                }
                Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Chip("发给微信", false) { if (ready) Share.toWechat(ctx, files) }
                    Chip("复制文案", false) { Share.copyText(ctx, caption); vm.toast("文案已复制") }
                }
            }
        }
    }
}
