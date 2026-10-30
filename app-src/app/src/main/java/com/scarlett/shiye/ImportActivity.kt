package com.scarlett.shiye

import android.content.ClipData
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 接收分享（ACTION_SEND / SEND_MULTIPLE）和「打开」（ACTION_VIEW）。
 * 安全约束：只读取 Intent 里的 data / extras 并复制进应用私有目录；不转发收到的 Intent；只接受 content:// ；文件名清洗、大小与 ZIP 条目限制在 Importer 里。
 */
class ImportActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Library.init(this)
        val src = intent
        val action = src?.action
        val uris = mutableListOf<Uri>()
        var text: String? = null; var subject: String? = null; var html = false
        when (action) {
            Intent.ACTION_VIEW -> src.data?.let { uris += it }
            Intent.ACTION_SEND -> {
                @Suppress("DEPRECATION") (src.getParcelableExtra<Uri>(Intent.EXTRA_STREAM))?.let { uris += it }
                if (uris.isEmpty()) {
                    val ht = src.getStringExtra(Intent.EXTRA_HTML_TEXT)
                    text = ht ?: src.getCharSequenceExtra(Intent.EXTRA_TEXT)?.toString(); html = ht != null || src.type == "text/html"
                    subject = src.getStringExtra(Intent.EXTRA_SUBJECT) ?: src.getStringExtra(Intent.EXTRA_TITLE)
                }
            }
            Intent.ACTION_SEND_MULTIPLE -> @Suppress("DEPRECATION") src.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM)?.let { uris += it }
        }
        if (uris.isEmpty() && text.isNullOrBlank()) { Toast.makeText(this, "没有收到可以保存的内容", Toast.LENGTH_SHORT).show(); finish(); return }
        val limited = uris.take(20)
        val source = if (action == Intent.ACTION_VIEW) "打开" else "分享"
        Toast.makeText(this, "正在存入拾页…", Toast.LENGTH_SHORT).show()
        lifecycleScope.launch {
            val ids = mutableListOf<String>(); val errs = mutableListOf<String>()
            withContext(Dispatchers.IO) {
                for (u in limited) runCatching { ids += Importer.fromUri(applicationContext, u, source).id }.onFailure { errs += it.message ?: "没能读取" }
                if (limited.isEmpty() && text != null) runCatching { ids += Importer.fromText(text, subject, source, html).id }.onFailure { errs += it.message ?: "没能保存" }
            }
            when {
                ids.isEmpty() -> Toast.makeText(this@ImportActivity, errs.firstOrNull() ?: "没能保存", Toast.LENGTH_LONG).show()
                else -> {
                    if (errs.isNotEmpty()) Toast.makeText(this@ImportActivity, "已存入 ${ids.size} 个，${errs.size} 个失败：${errs.first()}", Toast.LENGTH_LONG).show()
                    else Toast.makeText(this@ImportActivity, if (ids.size == 1) "已存入拾页" else "已存入 ${ids.size} 个文件", Toast.LENGTH_SHORT).show()
                    val go = Intent(this@ImportActivity, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)   // 显式、自建 Intent
                    if (ids.size == 1) go.putExtra(MainActivity.EXTRA_OPEN_ID, ids[0])
                    startActivity(go)
                }
            }
            finish()
        }
    }
}
