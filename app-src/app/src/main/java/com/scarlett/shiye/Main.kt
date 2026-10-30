package com.scarlett.shiye

import android.app.Application
import android.content.Context
import android.content.Intent
import android.graphics.Color as AColor
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ShiyeApp : Application() {
    override fun onCreate() {
        super.onCreate(); Library.init(this)
        Thread { runCatching { java.io.File(cacheDir, "export").deleteRecursively() } }.start()   // 清掉上次遗留的临时导出图
    }
}

/** 导航键：Navigation 3 的返回栈直接存这些不可变对象。导出面板、排版面板都是阅读页内的二级面板，不再是整页路由。 */
sealed interface Route {
    data object Home : Route
    data class Reader(val id: String, val export: Boolean = false) : Route
    data class Edit(val id: String) : Route
    data object NotionSettings : Route
}

enum class Tab { Read, Library, Me }
enum class Filter(val label: String) { All("全部"), Md("Markdown"), Html("网页"), Fav("收藏") }

class AppVm(app: Application) : AndroidViewModel(app) {
    val prefs = Prefs(app)
    val stack = mutableStateListOf<Route>(Route.Home)
    var forward by mutableStateOf(true)
    var tab by mutableStateOf(Tab.Read)
    var filter by mutableStateOf(Filter.All)
    var query by mutableStateOf("")
    var searching by mutableStateOf(false)
    /** 阅读页临时覆盖系统栏图标深浅（夜间阅读主题下用） */
    var barsDark by mutableStateOf<Boolean?>(null)
    var toastMsg by mutableStateOf<String?>(null); private set
    var busy by mutableStateOf<String?>(null); private set
    var notionDone by mutableStateOf<Pair<Doc, NotionResult>?>(null)
    private var toastJob: Job? = null

    fun push(r: Route) { forward = true; stack.add(r) }
    fun pop() { if (stack.size > 1) { forward = false; stack.removeAt(stack.lastIndex) } }
    fun openDoc(id: String, export: Boolean = false) {
        val d = Library.get(id) ?: return
        Library.touch(id)
        while (stack.size > 1) stack.removeAt(stack.lastIndex)
        forward = true; stack.add(Route.Reader(d.id, export))
    }
    fun toast(m: String) { toastMsg = m; toastJob?.cancel(); toastJob = viewModelScope.launch { delay(2600); toastMsg = null } }

    fun importUris(uris: List<android.net.Uri>) {
        val ctx = getApplication<Application>()
        busy = "正在导入…"
        viewModelScope.launch {
            val ids = mutableListOf<String>(); val errs = mutableListOf<String>()
            withContext(Dispatchers.IO) { for (u in uris.take(30)) runCatching { ids += Importer.fromUri(ctx, u, "导入").id }.onFailure { errs += it.message ?: "没能读取" } }
            busy = null
            when {
                ids.isEmpty() -> toast(errs.firstOrNull() ?: "没能导入")
                errs.isNotEmpty() -> toast("已导入 ${ids.size} 个，${errs.size} 个失败：${errs.first()}")
                ids.size == 1 -> openDoc(ids[0])
                else -> toast("已导入 ${ids.size} 个文件")
            }
        }
    }

    fun importText(text: String) {
        viewModelScope.launch {
            val r = withContext(Dispatchers.IO) { runCatching { Importer.fromText(text, null, "粘贴") } }
            r.onSuccess { openDoc(it.id) }.onFailure { toast(it.message ?: "没能保存") }
        }
    }

    fun syncNotion(doc: Doc) {
        if (busy != null) return
        if (!prefs.notionReady) { toast("先配置 Notion：设置 → Notion 同步"); push(Route.NotionSettings); return }
        busy = "正在同步到 Notion…"
        viewModelScope.launch {
            val r = runCatching {
                val text = withContext(Dispatchers.IO) { Library.readText(doc) }
                val md = if (doc.type == DocType.HTML) HtmlText.toMarkdown(text) else text
                val dir = Library.mainFile(doc).parentFile
                Notion.publish(prefs, doc, md, dir)
            }
            busy = null
            r.onSuccess { res ->
                Library.mutate(doc.id) { it.copy(notionUrl = res.url, notionPageId = res.pageId, notionTarget = prefs.notionTarget, notionAt = System.currentTimeMillis()) }
                notionDone = (Library.get(doc.id) ?: doc) to res
            }.onFailure { toast(if (it is NotionException) it.message ?: "同步失败" else "同步失败：网络不通或超时，稍后再试") }
        }
    }
}

class MainActivity : ComponentActivity() {
    companion object { const val EXTRA_OPEN_ID = "com.scarlett.shiye.OPEN_ID" }
    private val vm: AppVm by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        handle(intent)
        setContent { ShiyeRoot(vm) }
    }
    override fun onNewIntent(intent: Intent) { super.onNewIntent(intent); setIntent(intent); handle(intent) }

    /** 只接受自己 ImportActivity 传来的、符合格式的文档 ID；其余内容一概忽略 */
    private fun handle(i: Intent?) {
        val id = i?.getStringExtra(EXTRA_OPEN_ID)?.takeIf { Regex("[0-9a-f]{16}").matches(it) } ?: return
        i.removeExtra(EXTRA_OPEN_ID)
        vm.openDoc(id)
    }
}

@Composable
fun ShiyeRoot(vm: AppVm) {
    val prefs = vm.prefs
    ShiyeTheme(prefs.themeMode) {
        val appDark = P.dark
        val dark = vm.barsDark ?: appDark
        val ctx = LocalContext.current
        LaunchedEffect(dark) {
            (ctx.findActivity() as? ComponentActivity)?.enableEdgeToEdge(
                statusBarStyle = if (dark) SystemBarStyle.dark(AColor.TRANSPARENT) else SystemBarStyle.light(AColor.TRANSPARENT, AColor.TRANSPARENT),
                navigationBarStyle = if (dark) SystemBarStyle.dark(AColor.TRANSPARENT) else SystemBarStyle.light(AColor.TRANSPARENT, AColor.TRANSPARENT))
        }
        Box(Modifier.fillMaxSize().background(P.bg)) {
            NavDisplay(
                backStack = vm.stack,
                onBack = { vm.pop() },
                transitionSpec = {
                    (slideInHorizontally(tween(300)) { it / 6 } + fadeIn(tween(240))) togetherWith (slideOutHorizontally(tween(300)) { -it / 10 } + fadeOut(tween(180)))
                },
                popTransitionSpec = {
                    (slideInHorizontally(tween(300)) { -it / 10 } + fadeIn(tween(240))) togetherWith (slideOutHorizontally(tween(300)) { it / 6 } + fadeOut(tween(180)))
                },
                predictivePopTransitionSpec = {
                    (slideInHorizontally(tween(300)) { -it / 10 } + fadeIn(tween(240))) togetherWith (slideOutHorizontally(tween(300)) { it / 6 } + fadeOut(tween(180)))
                },
                entryProvider = entryProvider {
                    entry<Route.Home> { HomeScreen(vm) }
                    entry<Route.Reader> { ReaderScreen(vm, it.id, it.export) }
                    entry<Route.Edit> { EditorScreen(vm, it.id) }
                    entry<Route.NotionSettings> { NotionScreen(vm) }
                },
            )
            Toaster(vm.toastMsg, Modifier.align(Alignment.BottomCenter).windowInsetsPadding(WindowInsets.navigationBars).padding(bottom = 96.dp))
            vm.busy?.let { BusyOverlay(it) }
            vm.notionDone?.let { (doc, res) ->
                Confirm("已同步到 Notion", "「${doc.title}」已经写进 Notion。${if (res.note.isNotBlank()) "\n\n提示：${res.note}" else ""}", if (res.url.isNotBlank()) "在 Notion 打开" else "好的",
                    onOk = { if (res.url.isNotBlank()) Share.openUrl(ctx, res.url) }, onDismiss = { vm.notionDone = null })
            }
        }
    }
}

@Composable
fun BusyOverlay(msg: String) {
    Box(Modifier.fillMaxSize().background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.18f)).androidxClickBlock(), contentAlignment = Alignment.Center) {
        Row(Modifier.glass(androidx.compose.foundation.shape.RoundedCornerShape(20.dp), floating = true).padding(horizontal = 22.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            androidx.compose.material3.CircularProgressIndicator(Modifier.size(22.dp), color = P.accent, strokeWidth = 2.5.dp)
            Spacer(Modifier.width(14.dp))
            androidx.compose.material3.Text(msg, color = P.text, fontSize = androidx.compose.ui.unit.TextUnit(15f, androidx.compose.ui.unit.TextUnitType.Sp))
        }
    }
}
private fun Modifier.androidxClickBlock(): Modifier = this.then(Modifier.clickable(interactionSource = androidx.compose.foundation.interaction.MutableInteractionSource(), indication = null) {})
