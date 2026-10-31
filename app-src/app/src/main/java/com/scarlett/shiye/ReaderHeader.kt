package com.scarlett.shiye

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** 正文字数：去掉空白后的字符数（中文按字，英文数字按字符，与设计稿「4,126 字」同口径）。 */
fun wordCount(body: String): Int = body.count { !it.isWhitespace() }

fun fmtThousands(n: Int): String = "%,d".format(Locale.US, n)

/**
 * 阅读页标题头（Bear 式）：来源 · 平台 · 日期 → 衬线粗体大标题（《》不拆）→ 衬线副标题 → 摘要 → 标签 → 发丝线夹住的「时长 / 字数 / 节数」。
 * 标题永远用衬线体，和书库封面一致；颜色全部走 P，跟随阅读主题。
 */
@Composable
fun ReaderTitleHeader(doc: Doc, tags: List<String>, dek: String, words: Int, sections: Int, fontScale: Float, modifier: Modifier = Modifier) {
    val pal = P
    val s = fontScale.coerceIn(0.9f, 1.25f)
    val (main, sub) = remember(doc.title) { splitTitle(doc.title) }
    val date = remember(doc.createdAt) { SimpleDateFormat("M月d日", Locale.CHINA).format(Date(doc.createdAt)) }
    val kind = sourceKind(doc.source)
    Column(modifier.fillMaxWidth()) {
        // 来源行
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(16.dp).clip(Squircle(5.dp)).background(pal.accent))
            Spacer(Modifier.width(8.dp))
            Text(sourceName(doc.source).uppercase(), color = pal.accent, fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.06.em, maxLines = 1)
            Text(listOfNotNull(null, kind, date).joinToString(" · ", prefix = "· "), color = pal.text3, fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.02.em, maxLines = 1,
                overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(start = 8.dp))
        }
        Text(keepBookTitles(main), color = pal.text, modifier = Modifier.padding(top = 16.dp),
            style = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = (30 * s).sp, lineHeight = (42 * s).sp, letterSpacing = (-0.025).em, lineBreak = LineBreak.Heading))
        if (sub.isNotEmpty()) Text(keepBookTitles(sub), color = pal.text2, modifier = Modifier.padding(top = 4.dp),
            style = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Normal, fontSize = (20 * s).sp, lineHeight = (28 * s).sp, letterSpacing = (-0.01).em, lineBreak = LineBreak.Heading))
        if (dek.isNotBlank()) Text(dek, color = pal.text2, maxLines = 3, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 16.dp),
            style = TextStyle(fontSize = (15 * s).sp, lineHeight = (24 * s).sp, letterSpacing = (-0.05).sp))
        if (tags.isNotEmpty()) Row(Modifier.padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            tags.take(5).forEach {
                Box(Modifier.height(20.dp).clip(Squircle(10.dp)).background(pal.accent.copy(alpha = 0.09f)).padding(horizontal = 8.dp), contentAlignment = Alignment.Center) {
                    Text("#$it", color = pal.accent, fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium, maxLines = 1)
                }
            }
        }
        // 发丝线夹住的信息条
        Column(Modifier.padding(top = 20.dp)) {
            Box(Modifier.fillMaxWidth().height(0.5.dp).background(pal.hair))
            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min).padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                MetaCell(R.drawable.ic_clock, "约 ", readMinutes(doc.size).toString(), " 分钟", Modifier.weight(1f), first = true)
                MetaCell(R.drawable.ic_doc, "", fmtThousands(words), " 字", Modifier.weight(1f))
                if (sections > 0) MetaCell(R.drawable.ic_toc, "", sections.toString(), " 节", Modifier.weight(1f))
            }
            Box(Modifier.fillMaxWidth().height(0.5.dp).background(pal.hair))
        }
    }
}

@Composable
private fun MetaCell(icon: Int, pre: String, num: String, post: String, modifier: Modifier, first: Boolean = false) {
    val pal = P
    Row(modifier.fillMaxHeight().then(if (first) Modifier else Modifier.drawLeftHair(pal.hair)).padding(start = if (first) 0.dp else 16.dp), verticalAlignment = Alignment.CenterVertically) {
        Ic(icon, pal.text2, 16); Spacer(Modifier.width(8.dp))
        Text(buildAnnotatedString {
            append(pre); withStyle(SpanStyle(color = pal.text, fontWeight = FontWeight.SemiBold)) { append(num) }; append(post)
        }, color = pal.text2, fontSize = 12.sp, lineHeight = 16.sp, maxLines = 1)
    }
}

private fun Modifier.drawLeftHair(c: androidx.compose.ui.graphics.Color): Modifier = this.then(
    drawBehind { drawLine(c, Offset(0.25f, 0f), Offset(0.25f, size.height), 0.5.dp.toPx()) })
