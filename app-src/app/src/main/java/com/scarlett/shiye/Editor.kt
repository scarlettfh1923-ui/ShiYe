package com.scarlett.shiye

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** 撤销 / 重做：按输入节奏合并（停顿 0.7 秒或换了编辑类型才记一步），总字符数封顶，避免大文件吃内存。 */
class EditHistory(private val budgetChars: Int = 12_000_000) {
    private val undo = ArrayDeque<TextFieldValue>()
    private val redo = ArrayDeque<TextFieldValue>()
    private var lastAt = 0L
    var canUndo by mutableStateOf(false); private set
    var canRedo by mutableStateOf(false); private set

    private fun sync() { canUndo = undo.isNotEmpty(); canRedo = redo.isNotEmpty() }
    fun clear() { undo.clear(); redo.clear(); sync() }

    /** 文本即将从 [before] 变成 [after] */
    fun record(before: TextFieldValue, after: TextFieldValue, now: Long = System.currentTimeMillis(), force: Boolean = false) {
        if (before.text == after.text) return
        if (force || undo.isEmpty() || now - lastAt > 700) {
            undo.addLast(before)
            var total = undo.sumOf { it.text.length.toLong() }
            while (undo.size > 1 && (undo.size > 200 || total > budgetChars)) { total -= undo.removeFirst().text.length }
        }
        lastAt = now; redo.clear(); sync()
    }
    fun undo(current: TextFieldValue): TextFieldValue? {
        val p = undo.removeLastOrNull() ?: return null
        redo.addLast(current); lastAt = 0; sync(); return p
    }
    fun redo(current: TextFieldValue): TextFieldValue? {
        val n = redo.removeLastOrNull() ?: return null
        undo.addLast(current); lastAt = 0; sync(); return n
    }
}

@Composable
fun EditorScreen(vm: AppVm, id: String) {
    val docs by Library.docs.collectAsState()
    val doc = docs.firstOrNull { it.id == id }
    if (doc == null) { LaunchedEffect(Unit) { vm.pop() }; return }
    val scope = rememberCoroutineScope()
    val isHtml = doc.type == DocType.HTML
    var loaded by remember { mutableStateOf(false) }
    var tooBig by remember { mutableStateOf(false) }
    var tf by remember { mutableStateOf(TextFieldValue("")) }
    var saved by remember { mutableStateOf("") }          // 上次保存/载入时的文本，用来判断有没有改动
    val hist = remember { EditHistory() }
    var confirmLeave by remember { mutableStateOf(false) }
    var confirmRestore by remember { mutableStateOf(false) }
    var backupAt by remember { mutableLongStateOf(Library.backupAt(doc)) }
    val req = remember { FocusRequester() }
    val scroll = rememberScrollState()

    LaunchedEffect(doc.id) {
        val size = withContext(Dispatchers.IO) { Library.mainFile(doc).length() }
        if (size > Library.EDIT_LIMIT) { tooBig = true; loaded = true; return@LaunchedEffect }
        val t = withContext(Dispatchers.IO) { Library.readText(doc) }
        tf = TextFieldValue(t); saved = t; loaded = true
    }
    val dirty = loaded && !tooBig && tf.text != saved
    BackHandler(dirty) { confirmLeave = true }

    fun change(n: TextFieldValue) { hist.record(tf, n); tf = n }
    fun save() = scope.launch {
        val text = tf.text
        val r = runCatching { withContext(Dispatchers.IO) { Library.saveEdited(doc, text) } }
        r.onSuccess { saved = text; backupAt = Library.backupAt(doc); vm.toast("已保存，上一版本已备份") }.onFailure { vm.toast("保存失败：${it.message ?: "存储空间不足？"}") }
    }
    fun wrap(l: String, r: String = l) {
        val s = tf.selection; val t = tf.text
        val sel = t.substring(s.min, s.max)
        val nt = t.substring(0, s.min) + l + sel + r + t.substring(s.max)
        hist.record(tf, TextFieldValue(nt), force = true)
        tf = TextFieldValue(nt, TextRange(s.min + l.length, s.min + l.length + sel.length))
    }
    fun linePrefix(p: String) {
        val t = tf.text; val s = tf.selection.min
        val ls = t.lastIndexOf('\n', (s - 1).coerceAtLeast(0)).let { if (t.isEmpty() || s == 0) 0 else it + 1 }
        val nt = t.substring(0, ls) + p + t.substring(ls)
        hist.record(tf, TextFieldValue(nt), force = true)
        tf = TextFieldValue(nt, TextRange(tf.selection.min + p.length))
    }

    var preview by remember { mutableStateOf(false) }
    val prefs = vm.prefs
    val fileLabel = Library.mainFile(doc).name
    Box(Modifier.fillMaxSize().background(P.bg).imePadding()) {
        Column(Modifier.fillMaxSize()) {
            // 顶栏：✕ · 文件名 · （MD：编辑 / 预览）· 保存
            Row(Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.statusBars).height(60.dp).padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(48.dp).clip(CircleShape).clickable(role = Role.Button) { if (dirty) confirmLeave = true else vm.pop() }.semantics { contentDescription = "关闭" }, contentAlignment = Alignment.Center) { Ic(R.drawable.ic_close, P.text, 24) }
                Column(Modifier.weight(1f).padding(horizontal = 4.dp)) {
                    Text("编辑", color = P.text, style = MaterialTheme.typography.titleMedium)
                    Text(fileLabel + if (dirty) " · 未保存" else "", color = P.text3, style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                if (!isHtml) Row(Modifier.height(36.dp).clip(Shapes.chip).background(P.field).padding(3.dp)) {
                    listOf("编辑", "预览").forEachIndexed { i, n ->
                        val on = (i == 1) == preview
                        Box(Modifier.fillMaxHeight().clip(Shapes.chip).background(if (on) P.surface else Color.Transparent).clickable { preview = i == 1 }.padding(horizontal = 16.dp), contentAlignment = Alignment.Center) {
                            Text(n, color = if (on) P.text else P.text3, style = MaterialTheme.typography.labelLarge.copy(fontWeight = if (on) FontWeight.Bold else FontWeight.Medium))
                        }
                    }
                }
                Spacer(Modifier.width(8.dp))
                Box(Modifier.height(38.dp).clip(Shapes.chip).background(if (dirty) P.accent else P.field).clickable(enabled = dirty, role = Role.Button) { save() }.padding(horizontal = 16.dp), contentAlignment = Alignment.Center) {
                    Text("保存", color = if (dirty) P.onAccent else P.text3, style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
                }
                Spacer(Modifier.width(4.dp))
            }
            Box(Modifier.fillMaxWidth().height(0.7.dp).background(P.line))
            when {
                !loaded -> Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = P.accent, strokeWidth = 2.5.dp, modifier = Modifier.size(28.dp)) }
                tooBig -> Column(Modifier.weight(1f).fillMaxWidth().padding(32.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("这个文件有点大", color = P.text, style = MaterialTheme.typography.titleLarge)
                    Text("超过 ${Library.EDIT_LIMIT / 1024} KB 的文件暂不支持在手机上编辑，请在电脑上修改后重新导入。", color = P.text3, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 8.dp), textAlign = TextAlign.Center)
                }
                preview && !isHtml -> {
                    val blocks = remember(tf.text) { Markdown.parse(tf.text) }
                    val st = ReadStyle(prefs.fontScale, prefs.serif, prefs.mdNet, Library.mainFile(doc).parentFile, onLink = {}, lineMult = prefs.lineHeight, paraEm = prefs.paraSpacing)
                    Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 16.dp)) { RenderBlocks(blocks, st) }
                }
                else -> Box(Modifier.weight(1f).fillMaxWidth().verticalScroll(scroll).clickable(indication = null, interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }) { runCatching { req.requestFocus() } }) {
                    BasicTextField(tf, { change(it) }, textStyle = TextStyle(color = P.text, fontSize = 15.sp, lineHeight = 25.sp, fontFamily = FontFamily.Monospace), cursorBrush = SolidColor(P.accent),
                        modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 600.dp).focusRequester(req).padding(horizontal = 20.dp, vertical = 16.dp))
                }
            }
            if (loaded && !tooBig) Row(Modifier.fillMaxWidth().background(P.surface).hairline(GlassEdge.Top).windowInsetsPadding(WindowInsets.navigationBars).height(56.dp).padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                KeyIcon(R.drawable.ic_undo, "撤销", hist.canUndo) { hist.undo(tf)?.let { tf = it } }
                KeyIcon(R.drawable.ic_redo, "重做", hist.canRedo) { hist.redo(tf)?.let { tf = it } }
                if (!isHtml) {
                    Box(Modifier.padding(horizontal = 4.dp).width(0.8.dp).height(24.dp).background(P.line))
                    Row(Modifier.weight(1f).horizontalScroll(rememberScrollState()), verticalAlignment = Alignment.CenterVertically) {
                        KeyText("H", "标题", !preview) { linePrefix("## ") }
                        KeyText("B", "粗体", !preview, bold = true) { wrap("**") }
                        KeyText("•", "列表", !preview) { linePrefix("- ") }
                        KeyText("❝", "引用", !preview) { linePrefix("> ") }
                        KeyText("</>", "代码", !preview) { wrap("`") }
                        KeyText("🔗", "链接", !preview) { wrap("[", "](https://)") }
                    }
                } else Text("${tf.text.length} 字符", color = P.text3, style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1f).padding(start = 12.dp))
                Row(Modifier.clip(Squircle(12.dp)).clickable(enabled = backupAt > 0) { confirmRestore = true }.padding(horizontal = 8.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Ic(R.drawable.ic_history, if (backupAt > 0) P.accent else P.text3.copy(alpha = 0.45f), 20); Spacer(Modifier.width(4.dp))
                    Text("上一版", color = if (backupAt > 0) P.accent else P.text3.copy(alpha = 0.45f), style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
    if (confirmLeave) Confirm("放弃未保存的修改？", "离开后这次的修改不会保留。", "放弃修改", danger = true, onOk = { vm.pop() }, onDismiss = { confirmLeave = false })
    if (confirmRestore) Confirm("恢复到上一版本？", "上一版保存于 ${fmtAgo(backupAt)}。恢复后，当前内容会成为新的备份，可以再换回来。${if (dirty) "\n\n未保存的修改会丢失。" else ""}", "恢复", onOk = {
        scope.launch {
            val t = withContext(Dispatchers.IO) { Library.restoreBackup(doc) }
            if (t != null) { tf = TextFieldValue(t); saved = t; hist.clear(); backupAt = Library.backupAt(doc); vm.toast("已恢复到上一版本") } else vm.toast("没有可恢复的版本")
        }
    }, onDismiss = { confirmRestore = false })
}

@Composable
private fun KeyIcon(icon: Int, desc: String, enabled: Boolean, onClick: () -> Unit) {
    Box(Modifier.size(44.dp).clip(CircleShape).clickable(enabled = enabled, role = Role.Button, onClick = onClick).semantics { contentDescription = desc }, contentAlignment = Alignment.Center) {
        Ic(icon, if (enabled) P.text else P.text3.copy(alpha = 0.4f), 22)
    }
}

@Composable
private fun KeyText(glyph: String, desc: String, enabled: Boolean, bold: Boolean = false, onClick: () -> Unit) {
    Box(Modifier.size(44.dp).clip(CircleShape).clickable(enabled = enabled, role = Role.Button, onClick = onClick).semantics { contentDescription = desc }, contentAlignment = Alignment.Center) {
        Text(glyph, color = if (enabled) P.text else P.text3.copy(alpha = 0.4f), fontSize = if (glyph.length > 2) 14.sp else 18.sp, fontWeight = if (bold) FontWeight.ExtraBold else FontWeight.Medium, fontFamily = if (glyph == "</>") FontFamily.Monospace else FontFamily.Default)
    }
}
