package com.scarlett.shiye

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** 设置：全部存在本机 SharedPreferences，字段是 Compose 状态，改了立即生效并落盘。 */
class Prefs(ctx: Context) {
    private val sp: SharedPreferences = ctx.applicationContext.getSharedPreferences("shiye_prefs", Context.MODE_PRIVATE)
    private val appCtx = ctx.applicationContext

    var themeMode by mutableStateOf(sp.getInt("themeMode", 0)); private set        // 0 跟随系统 1 浅色 2 深色
    var fontScale by mutableStateOf(sp.getFloat("fontScale", 1f)); private set
    var lineHeight by mutableStateOf(sp.getFloat("lineHeight", 1.75f).coerceIn(1.2f, 2.2f)); private set    // 行距倍数 1.2–2.2
    var paraSpacing by mutableStateOf(sp.getFloat("paraSpacing", 0.6f).coerceIn(0f, 1.5f)); private set    // 段间距，单位 em，0–1.5
    var readTheme by mutableStateOf(sp.getInt("readTheme", 0).coerceIn(0, 3)); private set   // 0 白(跟随应用) 1 雾蓝 2 纸 3 夜
    var serif by mutableStateOf(sp.getBoolean("serif", false)); private set
    var grid by mutableStateOf(sp.getBoolean("grid", true)); private set
    var sort by mutableStateOf(sp.getInt("sort", 0)); private set                   // 0 最近打开 1 最近存入 2 名称 3 大小
    var htmlNet by mutableStateOf(sp.getBoolean("htmlNet", true)); private set      // HTML 允许加载 https 图片/样式
    var mdNet by mutableStateOf(sp.getBoolean("mdNet", true)); private set          // Markdown 允许加载 https 图片
    var exportStyle by mutableStateOf(sp.getInt("exportStyle", 0)); private set     // 长图样式
    var xhsStyle by mutableStateOf(sp.getInt("xhsStyle", 0)); private set
    var xhsRatio by mutableStateOf(sp.getInt("xhsRatio", 0)); private set           // 0 = 3:4  1 = 9:16

    var onboarded by mutableStateOf(sp.getBoolean("onboarded", false)); private set
    fun resetOnboarded() { onboarded = false; sp.edit().putBoolean("onboarded", false).apply() }
    fun setOnboarded() { onboarded = true; sp.edit().putBoolean("onboarded", true).apply() }

    var notionType by mutableStateOf(sp.getString("notionType", "page") ?: "page"); private set
    var notionTarget by mutableStateOf(sp.getString("notionTarget", "") ?: ""); private set
    var notionTitleProp by mutableStateOf(sp.getString("notionTitleProp", "") ?: ""); private set
    var notionTagsProp by mutableStateOf(sp.getString("notionTagsProp", "") ?: ""); private set
    var notionToken by mutableStateOf(Secure.get(appCtx, sp.getString("notionTokenEnc", "") ?: "")); private set

    fun saveThemeMode(v: Int) { themeMode = v; sp.edit().putInt("themeMode", v).apply() }
    fun saveFontScale(v: Float) { fontScale = v.coerceIn(0.8f, 1.6f); sp.edit().putFloat("fontScale", fontScale).apply() }
    fun saveLineHeight(v: Float) { lineHeight = (Math.round(v * 20f) / 20f).coerceIn(1.2f, 2.2f); sp.edit().putFloat("lineHeight", lineHeight).apply() }
    fun saveParaSpacing(v: Float) { paraSpacing = (Math.round(v * 20f) / 20f).coerceIn(0f, 1.5f); sp.edit().putFloat("paraSpacing", paraSpacing).apply() }
    fun resetTypography() { saveFontScale(1f); saveLineHeight(1.75f); saveParaSpacing(0.6f); saveSerif(false); saveReadTheme(0) }
    fun saveReadTheme(v: Int) { readTheme = v.coerceIn(0, 3); sp.edit().putInt("readTheme", readTheme).apply() }
    fun saveSerif(v: Boolean) { serif = v; sp.edit().putBoolean("serif", v).apply() }
    fun saveGrid(v: Boolean) { grid = v; sp.edit().putBoolean("grid", v).apply() }
    fun saveSort(v: Int) { sort = v; sp.edit().putInt("sort", v).apply() }
    fun saveHtmlNet(v: Boolean) { htmlNet = v; sp.edit().putBoolean("htmlNet", v).apply() }
    fun saveMdNet(v: Boolean) { mdNet = v; sp.edit().putBoolean("mdNet", v).apply() }
    fun saveXhsStyle(v: Int) { xhsStyle = v; sp.edit().putInt("xhsStyle", v).apply() }
    fun saveXhsRatio(v: Int) { xhsRatio = v; sp.edit().putInt("xhsRatio", v).apply() }
    fun saveExportStyle(v: Int) { exportStyle = v; sp.edit().putInt("exportStyle", v).apply() }
    fun saveNotionType(v: String) { notionType = v; sp.edit().putString("notionType", v).apply() }
    fun saveNotionTarget(v: String) { notionTarget = v.trim(); sp.edit().putString("notionTarget", notionTarget).apply() }
    fun saveNotionTitleProp(v: String) { notionTitleProp = v.trim(); sp.edit().putString("notionTitleProp", notionTitleProp).apply() }
    fun saveNotionTagsProp(v: String) { notionTagsProp = v.trim(); sp.edit().putString("notionTagsProp", notionTagsProp).apply() }
    fun saveNotionToken(v: String) { notionToken = v.trim(); sp.edit().putString("notionTokenEnc", Secure.put(appCtx, notionToken)).apply() }
    val notionReady get() = notionToken.isNotBlank() && notionTarget.isNotBlank()
}

/** Notion Token 用 Android Keystore 里的 AES-GCM 密钥加密后再存盘，明文不落地。 */
object Secure {
    private const val ALIAS = "shiye_secret_v1"
    private fun key(): SecretKey {
        val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (ks.getKey(ALIAS, null) as? SecretKey)?.let { return it }
        val g = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        g.init(KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).setKeySize(256).build())
        return g.generateKey()
    }
    fun put(@Suppress("UNUSED_PARAMETER") ctx: Context, plain: String): String {
        if (plain.isEmpty()) return ""
        return runCatching {
            val c = Cipher.getInstance("AES/GCM/NoPadding"); c.init(Cipher.ENCRYPT_MODE, key())
            val ct = c.doFinal(plain.toByteArray())
            Base64.encodeToString(c.iv + ct, Base64.NO_WRAP)
        }.getOrDefault("")
    }
    fun get(@Suppress("UNUSED_PARAMETER") ctx: Context, enc: String): String {
        if (enc.isEmpty()) return ""
        return runCatching {
            val raw = Base64.decode(enc, Base64.NO_WRAP)
            val c = Cipher.getInstance("AES/GCM/NoPadding"); c.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, raw, 0, 12))
            String(c.doFinal(raw, 12, raw.size - 12))
        }.getOrDefault("")
    }
}
