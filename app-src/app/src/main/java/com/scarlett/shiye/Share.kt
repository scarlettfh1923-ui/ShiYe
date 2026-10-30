package com.scarlett.shiye

import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import java.io.File

/** 分享与保存：全部用「自己构造的显式 Intent + FileProvider 临时授权」，不转发任何收到的 Intent。 */
object Share {
    const val XHS = "com.xingin.xhs"
    const val WECHAT = "com.tencent.mm"

    fun installed(ctx: Context, pkg: String) = runCatching { ctx.packageManager.getPackageInfo(pkg, 0); true }.getOrDefault(false)

    fun uriOf(ctx: Context, f: File): Uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.files", f)

    private fun imageIntent(ctx: Context, files: List<File>): Intent {
        val uris = ArrayList(files.map { uriOf(ctx, it) })
        val i = if (uris.size == 1) Intent(Intent.ACTION_SEND).putExtra(Intent.EXTRA_STREAM, uris[0])
        else Intent(Intent.ACTION_SEND_MULTIPLE).putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
        i.type = "image/png"
        val clip = ClipData.newUri(ctx.contentResolver, "ShiYe", uris[0]); uris.drop(1).forEach { clip.addItem(ClipData.Item(it)) }
        i.clipData = clip
        i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        return i
    }

    fun copyText(ctx: Context, text: String) {
        (ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText("拾页", text))
    }

    /** 一键分享到小红书：图片无损 PNG 直送；没装小红书就走系统分享面板。返回是否直达。 */
    fun toXhs(ctx: Context, files: List<File>, caption: String): Boolean {
        copyText(ctx, caption)           // 小红书不读 EXTRA_TEXT，文案放进剪贴板，到发布页长按粘贴
        val i = imageIntent(ctx, files.take(18))
        if (installed(ctx, XHS)) {
            i.setPackage(XHS); i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (runCatching { ctx.startActivity(i); true }.getOrDefault(false)) return true
            i.setPackage(null)
        }
        chooser(ctx, i); return false
    }

    fun toWechat(ctx: Context, files: List<File>): Boolean {
        val i = imageIntent(ctx, files)
        if (installed(ctx, WECHAT)) { i.setPackage(WECHAT); i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); if (runCatching { ctx.startActivity(i); true }.getOrDefault(false)) return true; i.setPackage(null) }
        chooser(ctx, i); return false
    }

    fun toAny(ctx: Context, files: List<File>) = chooser(ctx, imageIntent(ctx, files))

    private fun chooser(ctx: Context, i: Intent) {
        val c = Intent.createChooser(i, "分享图片").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        ctx.startActivity(c)
    }

    /** 以文件形式分享：Markdown 发 .md、网页发 .html，文件名取标题。FileProvider 临时授权 + 系统分享面板。 */
    fun file(ctx: Context, doc: Doc): Boolean {
        val src = Library.mainFile(doc); if (!src.isFile) return false
        val ext = if (doc.type == DocType.HTML) "html" else "md"
        val base = Importer.safeName(doc.title).ifBlank { "拾页文档" }
        val dir = File(ctx.cacheDir, "export/share-${System.currentTimeMillis()}").apply { mkdirs() }
        val out = File(dir, "$base.$ext")
        src.copyTo(out, overwrite = true)
        val uri = uriOf(ctx, out)
        val i = Intent(Intent.ACTION_SEND).setType(if (doc.type == DocType.HTML) "text/html" else "text/markdown")
            .putExtra(Intent.EXTRA_STREAM, uri).putExtra(Intent.EXTRA_SUBJECT, doc.title)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        i.clipData = ClipData.newUri(ctx.contentResolver, base, uri)
        ctx.startActivity(Intent.createChooser(i, "分享文件").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        return true
    }

    fun shareText(ctx: Context, title: String, text: String) {
        val i = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_SUBJECT, title).putExtra(Intent.EXTRA_TEXT, text)
        ctx.startActivity(Intent.createChooser(i, "分享").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    /** 存入系统相册（Pictures/拾页），用 MediaStore，不需要任何存储权限。返回成功张数。 */
    fun saveToGallery(ctx: Context, files: List<File>): Int {
        var n = 0
        for (f in files) {
            val v = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, "拾页_${System.currentTimeMillis()}_${f.nameWithoutExtension.takeLast(2)}.png")
                put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/拾页")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
            val uri = ctx.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, v) ?: continue
            val ok = runCatching { ctx.contentResolver.openOutputStream(uri)?.use { o -> f.inputStream().use { it.copyTo(o) } }; true }.getOrDefault(false)
            if (ok) { ctx.contentResolver.update(uri, ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING, 0) }, null, null); n++ } else ctx.contentResolver.delete(uri, null, null)
        }
        return n
    }

    fun openUrl(ctx: Context, url: String): Boolean {
        if (!(url.startsWith("https://") || url.startsWith("http://") || url.startsWith("mailto:"))) return false
        return runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addCategory(Intent.CATEGORY_BROWSABLE).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); true }.getOrDefault(false)
    }
}
