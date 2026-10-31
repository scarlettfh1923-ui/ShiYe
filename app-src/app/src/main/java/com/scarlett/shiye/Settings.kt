package com.scarlett.shiye

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun PageScaffold(title: String, vm: AppVm, content: @Composable ColumnScope.() -> Unit) {
    val backdrop = remember { Backdrop() }
    CompositionLocalProvider(LocalBackdrop provides backdrop) {
        Box(Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize().glassSource(backdrop).verticalScroll(rememberScrollState()).imePadding()
                .padding(top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 72.dp, bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 32.dp)
                .padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(24.dp), content = content)
            TopBar(title, { vm.pop() }, Modifier.align(Alignment.TopCenter))
        }
    }
}

@Composable
fun MeTab(vm: AppVm) {
    val prefs = vm.prefs
    val ctx = LocalContext.current
    val docs by Library.docs.collectAsState()
    var clear by remember { mutableStateOf(false) }
    val used by produceState(0L, docs) { value = withContext(Dispatchers.IO) { Library.rootDir.walkTopDown().filter { it.isFile }.sumOf { it.length() } } }
    val ver = remember { runCatching { ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName }.getOrNull() ?: "1.1.0" }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).windowInsetsPadding(WindowInsets.statusBars)
        .padding(top = 16.dp, bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 120.dp).padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
      Column(Modifier.widthIn(max = 640.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(24.dp)) {
        Row(Modifier.padding(start = 8.dp, top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(52.dp).clip(androidx.compose.foundation.shape.CircleShape).background(androidx.compose.ui.graphics.Brush.linearGradient(listOf(P.accent, androidx.compose.ui.graphics.Color(0xFF7FA2FF)))), contentAlignment = Alignment.Center) { Ic(R.drawable.ic_user, P.onAccent, 26) }
            Spacer(Modifier.width(16.dp))
            Column {
                Text("我的", color = P.text, style = androidx.compose.material3.MaterialTheme.typography.headlineSmall)
                Text("${docs.size} 篇文档 · 仅保存在本机", color = P.text3, style = androidx.compose.material3.MaterialTheme.typography.bodySmall)
            }
        }
        Group("外观") {
            Column(Modifier.padding(16.dp)) {
                Text("主题", color = P.text, fontSize = 16.sp, modifier = Modifier.padding(bottom = 8.dp))
                Segmented(listOf("跟随系统", "浅色", "深色"), prefs.themeMode, { prefs.saveThemeMode(it) }, Modifier.fillMaxWidth())
            }
            Divider()
            Row2("阅读样式", "字体、字号、行距、段间距、阅读主题：在阅读页点「排版」调整", R.drawable.ic_text_size)
        }
        Group("同步与分享") {
            Row2("Notion 同步", if (prefs.notionReady) "已配置 · ${if (prefs.notionType == "database") "数据库" else "页面"}" else "未配置，点这里设置", R.drawable.ic_notion, onClick = { vm.push(Route.NotionSettings) })
        }
        Group("阅读与安全") {
            SwitchRow("Markdown 加载网络图片", "关闭后只显示本地图片", R.drawable.ic_cards, prefs.mdNet) { prefs.saveMdNet(it) }
            Divider()
            SwitchRow("网页允许联网加载资源", "仅 https。关闭后网页只显示自带内容", R.drawable.ic_globe, prefs.htmlNet) { prefs.saveHtmlNet(it) }
            Divider()
            Row2("网页脚本默认关闭", "需要时在网页阅读页顶栏按文档单独开启，不会影响其他文档", R.drawable.ic_shield)
        }
        Group("书库") {
            Row2("已存入 ${docs.size} 篇", "占用 ${fmtSize(used)} · 仅保存在本机，不上传", R.drawable.ic_folder)
            Divider()
            Row2("清空书库", "删除拾页里的全部文档", R.drawable.ic_trash, onClick = { if (docs.isNotEmpty()) clear = true else vm.toast("书库已经是空的") })
        }
        Group("关于") {
            Row2("拾页 $ver", "只在你点「同步到 Notion」或开启网络图片时联网", R.drawable.ic_info)
            Divider()
            Row2("重看新手引导", null, R.drawable.ic_sparkle, onClick = { prefs.resetOnboarded() })
        }
      }
    }
    if (clear) Confirm("清空书库？", "会删除拾页里的 ${docs.size} 篇文档，原文件不受影响。此操作不能撤销。", "清空", danger = true, onOk = { Library.clearAll(); vm.toast("书库已清空") }, onDismiss = { clear = false })
}

@Composable
private fun Field(label: String, value: String, onChange: (String) -> Unit, hint: String, secret: Boolean = false, single: Boolean = true) {
    var show by remember { mutableStateOf(false) }
    Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
        Text(label, color = P.text2, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        Row(Modifier.padding(top = 8.dp).fillMaxWidth().clip(Shapes.box).background(P.field).padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f)) {
                if (value.isEmpty()) Text(hint, color = P.text3, fontSize = 15.sp)
                BasicTextField(value, onChange, singleLine = single, textStyle = TextStyle(color = P.text, fontSize = 15.sp), cursorBrush = SolidColor(P.accent),
                    visualTransformation = if (secret && !show) PasswordVisualTransformation() else VisualTransformation.None,
                    keyboardOptions = KeyboardOptions(keyboardType = if (secret) KeyboardType.Password else KeyboardType.Uri), modifier = Modifier.fillMaxWidth())
            }
            if (secret) Text(if (show) "隐藏" else "显示", color = P.accent, fontSize = 13.sp, modifier = Modifier.clip(Shapes.chip).clickable { show = !show }.padding(8.dp, 4.dp))
        }
    }
}

@Composable
fun NotionScreen(vm: AppVm) {
    val prefs = vm.prefs
    val scope = rememberCoroutineScope()
    var token by remember { mutableStateOf(prefs.notionToken) }
    var target by remember { mutableStateOf(prefs.notionTarget) }
    var type by remember { mutableStateOf(prefs.notionType) }
    var titleProp by remember { mutableStateOf(prefs.notionTitleProp) }
    var tagsProp by remember { mutableStateOf(prefs.notionTagsProp) }
    var testing by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<Pair<Boolean, String>?>(null) }
    fun save() { prefs.saveNotionToken(token); prefs.saveNotionTarget(target); prefs.saveNotionType(type); prefs.saveNotionTitleProp(titleProp); prefs.saveNotionTagsProp(tagsProp) }
    DisposableEffect(Unit) { onDispose { save() } }
    PageScaffold("Notion 同步", vm) {
        Group("先做这三步") {
            val steps = listOf("打开 notion.so/my-integrations，新建一个「内部集成」，复制它的 Token。", "在 Notion 里打开要存放的页面或数据库，右上角「…」→「连接」→ 选刚建的集成。", "复制该页面 / 数据库的链接，粘贴到下面。")
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                steps.forEachIndexed { i, s ->
                    Row {
                        Box(Modifier.size(24.dp).clip(androidx.compose.foundation.shape.CircleShape).background(P.accentSoft), contentAlignment = Alignment.Center) { Text("${i + 1}", color = P.accent, fontSize = 13.sp, fontWeight = FontWeight.Bold) }
                        Spacer(Modifier.width(12.dp)); Text(s, color = P.text2, fontSize = 14.sp, lineHeight = 20.sp)
                    }
                }
            }
        }
        Group("连接信息") {
            Column(Modifier.padding(16.dp)) { Text("存放方式", color = P.text2, fontSize = 13.sp, modifier = Modifier.padding(bottom = 8.dp)); Segmented(listOf("子页面", "数据库条目"), if (type == "database") 1 else 0, { type = if (it == 1) "database" else "page" }, Modifier.fillMaxWidth()) }
            Divider()
            Field("集成 Token", token, { token = it }, "ntn_… 或 secret_…", secret = true)
            Divider()
            Field(if (type == "database") "数据库链接" else "父页面链接", target, { target = it }, "https://www.notion.so/…")
            if (type == "database") {
                Divider(); Field("标题列名（可留空自动识别）", titleProp, { titleProp = it }, "例如 名称 / Name")
                Divider(); Field("标签列名（多选类型，可留空）", tagsProp, { tagsProp = it }, "例如 标签 / Tags")
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            PillButton(if (testing) "正在测试…" else "保存并测试连接", R.drawable.ic_cloud_up, {
                save(); testing = true; result = null
                scope.launch { result = runCatching { Notion.test(token.trim(), target, type) }.fold({ true to it }, { false to (if (it is NotionException) it.message ?: "连接失败" else "网络不通或超时") }); testing = false }
            }, Modifier.fillMaxWidth(), enabled = !testing)
            result?.let { (ok, m) ->
                Row(Modifier.fillMaxWidth().clip(Shapes.box).background(if (ok) P.accentSoft else P.danger.copy(alpha = 0.1f)).padding(16.dp), verticalAlignment = Alignment.Top) {
                    Ic(if (ok) R.drawable.ic_check else R.drawable.ic_info, if (ok) P.accent else P.danger, 20); Spacer(Modifier.width(8.dp))
                    Text(m, color = if (ok) P.accent else P.danger, fontSize = 14.sp, lineHeight = 20.sp)
                }
            }
            Text("Token 用手机的安全存储加密保存，只发给 Notion 官方接口。同步时，文档里的本地图片（最多 8 张、单张不超过 5 MB）会一并上传；再次同步同一篇文档，会替换上一次的页面。", color = P.text3, fontSize = 13.sp, lineHeight = 18.sp, modifier = Modifier.padding(horizontal = 8.dp))
        }
    }
}
