package com.scarlett.shiye

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics

/** 分层柔影：一层贴边细影 + 一层大范围淡影，比单层 elevation 更像纸。level 1 卡片 · 2 浮层 · 3 书封 */
@Composable
fun Modifier.soft(shape: androidx.compose.ui.graphics.Shape, level: Int = 1): Modifier {
    if (P.dark) return this
    val ink = 0xFF0F1522.toInt()
    fun c(a: Int) = Color((a shl 24) or (ink and 0xFFFFFF))
    return when (level) {
        1 -> shadow(1.dp, shape, false, c(0x0A), c(0x12)).shadow(12.dp, shape, false, c(0x06), c(0x14))
        2 -> shadow(1.dp, shape, false, c(0x0A), c(0x12)).shadow(8.dp, shape, false, c(0x08), c(0x14)).shadow(24.dp, shape, false, c(0x08), c(0x1A))
        else -> shadow(1.dp, shape, false, c(0x0A), c(0x1A)).shadow(6.dp, shape, false, c(0x0A), c(0x1F)).shadow(16.dp, shape, false, c(0x06), c(0x1F))
    }
}

@Composable
fun Ic(res: Int, tint: Color = P.text, size: Int = 22, modifier: Modifier = Modifier) =
    Icon(painterResource(res), null, tint = tint, modifier = modifier.size(size.dp))

/** 圆形图标按钮（Glimpse 式白色圆钮）。glass=true 时放在玻璃栏上用半透明底。 */
@Composable
fun CircleBtn(icon: Int, desc: String, onClick: () -> Unit, modifier: Modifier = Modifier, tint: Color = P.text, filled: Boolean = false, size: Int = 44, glass: Boolean = false) {
    val pal = P
    val bg = when { filled -> pal.accent; glass -> if (pal.dark) Color.White.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.78f); else -> pal.surface }
    Box(modifier.size(size.dp).then(if (!filled && !glass) Modifier.soft(CircleShape, 1) else Modifier)
        .clip(CircleShape).background(bg)
        .then(if (glass && !pal.dark) Modifier.border(0.8.dp, Color.White.copy(alpha = 0.9f), CircleShape) else Modifier)
        .clickable(role = Role.Button, onClick = onClick).semantics { contentDescription = desc }, contentAlignment = Alignment.Center) {
        Ic(icon, if (filled) pal.onAccent else tint, 22)
    }
}

@Composable
fun PillButton(text: String, icon: Int? = null, onClick: () -> Unit, modifier: Modifier = Modifier, primary: Boolean = true, enabled: Boolean = true) {
    val pal = P
    Row(modifier.height(50.dp).clip(Shapes.chip).background(if (!enabled) pal.field else if (primary) pal.accent else pal.surface)
        .then(if (!primary) Modifier.border(1.dp, pal.line, Shapes.chip) else Modifier)
        .clickable(enabled = enabled, role = Role.Button, onClick = onClick).padding(horizontal = 24.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
        if (icon != null) { Ic(icon, if (primary && enabled) pal.onAccent else if (enabled) pal.text else pal.text3, 20); Spacer(Modifier.width(8.dp)) }
        Text(text, color = if (primary && enabled) pal.onAccent else if (enabled) pal.text else pal.text3, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
    }
}

@Composable
fun Chip(text: String, selected: Boolean, onClick: () -> Unit) {
    val pal = P
    Box(Modifier.height(32.dp).clip(Shapes.chip).background(if (selected) pal.accent else if (pal.dark) pal.field else Color(0x0D0F1522))
        .clickable(onClick = onClick).padding(horizontal = 16.dp), contentAlignment = Alignment.Center) {
        Text(text, color = if (selected) pal.onAccent else pal.text2, fontSize = 14.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium, maxLines = 1)
    }
}

@Composable
fun Segmented(options: List<String>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    val pal = P
    Row(modifier.clip(Shapes.chip).background(pal.field).padding(3.dp)) {
        options.forEachIndexed { i, t ->
            Box(Modifier.weight(1f).height(36.dp).clip(Shapes.chip).background(if (i == selected) pal.surface else Color.Transparent)
                .then(if (i == selected) Modifier.shadow(0.dp) else Modifier).clickable { onSelect(i) }, contentAlignment = Alignment.Center) {
                Text(t, color = if (i == selected) pal.accent else pal.text2, fontSize = 14.sp, fontWeight = if (i == selected) FontWeight.SemiBold else FontWeight.Normal, maxLines = 1)
            }
        }
    }
}

@Composable
fun Group(title: String? = null, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val pal = P
    Column(modifier.fillMaxWidth()) {
        if (title != null) Text(title, color = pal.text3, fontSize = 13.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(start = 8.dp, bottom = 8.dp))
        Column(Modifier.fillMaxWidth().glass(Shapes.card, floating = true), content = content)
    }
}

@Composable
fun Row2(title: String, sub: String? = null, icon: Int? = null, onClick: (() -> Unit)? = null, trailing: @Composable (() -> Unit)? = null) {
    val pal = P
    Row(Modifier.fillMaxWidth().then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier).padding(horizontal = 16.dp, vertical = 12.dp).heightIn(min = 32.dp), verticalAlignment = Alignment.CenterVertically) {
        if (icon != null) { Box(Modifier.size(32.dp).clip(Squircle(8.dp)).background(pal.accentSoft), contentAlignment = Alignment.Center) { Ic(icon, pal.accent, 19) }; Spacer(Modifier.width(16.dp)) }
        Column(Modifier.weight(1f)) {
            Text(title, color = pal.text, fontSize = 16.sp)
            if (sub != null) Text(sub, color = pal.text3, fontSize = 13.sp, lineHeight = 17.sp, modifier = Modifier.padding(top = 2.dp))
        }
        if (trailing != null) { Spacer(Modifier.width(8.dp)); trailing() } else if (onClick != null) Ic(R.drawable.ic_chevron_right, pal.text3, 18)
    }
}

@Composable
fun Divider() { Box(Modifier.fillMaxWidth().padding(start = 16.dp).height(0.5.dp).background(P.hair)) }

@Composable
fun SwitchRow(title: String, sub: String? = null, icon: Int? = null, checked: Boolean, onChange: (Boolean) -> Unit) {
    val pal = P
    Row2(title, sub, icon, onClick = { onChange(!checked) }, trailing = {
        Switch(checked, onChange, colors = SwitchDefaults.colors(checkedTrackColor = pal.accent, checkedThumbColor = Color.White, uncheckedTrackColor = pal.field, uncheckedThumbColor = Color.White, uncheckedBorderColor = Color.Transparent))
    })
}

@Composable
fun Toaster(msg: String?, modifier: Modifier = Modifier) {
    AnimatedVisibility(msg != null, modifier, enter = fadeIn() + slideInVertically { it / 2 }, exit = fadeOut() + slideOutVertically { it / 2 }) {
        var last by remember { mutableStateOf(msg) }
        if (msg != null) last = msg
        Box(Modifier.padding(horizontal = 24.dp).glass(Squircle(16.dp), floating = true).padding(horizontal = 16.dp, vertical = 12.dp)) {
            Text(last ?: "", color = P.text, fontSize = 14.sp, textAlign = TextAlign.Center)
        }
    }
}

@Composable
fun Confirm(title: String, text: String, ok: String, danger: Boolean = false, onOk: () -> Unit, onDismiss: () -> Unit) {
    val pal = P
    AlertDialog(onDismissRequest = onDismiss, containerColor = pal.surface, shape = Squircle(24.dp),
        title = { Text(title, color = pal.text, fontWeight = FontWeight.SemiBold, fontSize = 18.sp) },
        text = { Text(text, color = pal.text2, fontSize = 15.sp, lineHeight = 21.sp) },
        confirmButton = { TextButton(onClick = { onOk(); onDismiss() }) { Text(ok, color = if (danger) pal.danger else pal.accent, fontWeight = FontWeight.SemiBold) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消", color = pal.text2) } })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Sheet(onDismiss: () -> Unit, scrim: Color = Color.Black.copy(alpha = 0.28f), content: @Composable ColumnScope.() -> Unit) {
    val pal = P
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), containerColor = pal.surface, shape = Shapes.sheet, dragHandle = {
        Box(Modifier.padding(top = 8.dp, bottom = 8.dp).size(36.dp, 4.dp).clip(Shapes.chip).background(pal.line))
    }, scrimColor = scrim) { Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 20.dp), content = content) }
}

@Composable
fun SheetAction(icon: Int, text: String, danger: Boolean = false, onClick: () -> Unit) {
    val pal = P
    Row(Modifier.fillMaxWidth().clip(Shapes.box).clickable(onClick = onClick).padding(horizontal = 8.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
        Ic(icon, if (danger) pal.danger else pal.text2, 22); Spacer(Modifier.width(16.dp))
        Text(text, color = if (danger) pal.danger else pal.text, fontSize = 16.sp)
    }
}

/** 页面顶栏（玻璃，贴状态栏）。 */
@Composable
fun TopBar(title: String, onBack: (() -> Unit)?, modifier: Modifier = Modifier, titleAlpha: Float = 1f, actions: @Composable RowScope.() -> Unit = {}) {
    Row(modifier.fillMaxWidth().glass().hairline(GlassEdge.Bottom).windowInsetsPadding(WindowInsets.statusBars).height(60.dp).padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        if (onBack != null) { CircleBtn(R.drawable.ic_back, "返回", onBack, glass = true, size = 40); Spacer(Modifier.width(8.dp)) } else Spacer(Modifier.width(8.dp))
        Text(title, color = P.text.copy(alpha = titleAlpha), fontSize = 17.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
        actions()
    }
}

fun fmtSize(n: Long): String = when { n < 1024 -> "$n B"; n < 1024 * 1024 -> "${n / 1024} KB"; else -> "%.1f MB".format(n / 1048576f) }
fun fmtAgo(t: Long): String {
    val d = System.currentTimeMillis() - t; val m = d / 60000
    return when { m < 1 -> "刚刚"; m < 60 -> "${m} 分钟前"; m < 60 * 24 -> "${m / 60} 小时前"; m < 60 * 24 * 2 -> "昨天"; m < 60 * 24 * 30 -> "${m / 1440} 天前"
        else -> java.text.SimpleDateFormat("M月d日", java.util.Locale.CHINA).format(java.util.Date(t)) }
}
fun readMinutes(chars: Long): Int = ((chars / 3.0) / 400).toInt().coerceAtLeast(1)   // 文件字节数粗估字数，按每分钟 400 字
