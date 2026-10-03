package com.bhagavatam.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBackIos
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bhagavatam.app.data.SanskritScript
import com.bhagavatam.app.data.Verse
import com.bhagavatam.app.ui.theme.Brand
import com.bhagavatam.app.ui.theme.Literata
import com.bhagavatam.app.ui.theme.TiroBangla
import com.bhagavatam.app.ui.theme.TiroSanskrit
import com.bhagavatam.app.util.Transliterate

@Composable
fun LargeTitle(title: String, subtitle: String? = null, modifier: Modifier = Modifier, horizontalPadding: Dp = 20.dp) {
    Column(
        modifier.fillMaxWidth().statusBarsPadding().padding(start = horizontalPadding, end = horizontalPadding, top = 14.dp, bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(title, style = MaterialTheme.typography.displaySmall, color = Brand.Ink)
        if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = Brand.Secondary)
    }
}

@Composable
fun NavBar(backLabel: String, onBack: () -> Unit, title: String = "", trailing: @Composable RowScope.() -> Unit = {}) {
    Row(
        Modifier.fillMaxWidth().statusBarsPadding().height(52.dp).padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            Modifier.clip(RoundedCornerShape(12.dp)).clickable(onClick = onBack).heightIn(min = 44.dp).padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBackIos, contentDescription = null, tint = Brand.Kesari, modifier = Modifier.size(20.dp))
            Text(backLabel, color = Brand.Kesari, fontSize = 16.sp, fontWeight = FontWeight.Medium)
        }
        Text(title, Modifier.weight(1f), textAlign = TextAlign.Center, style = MaterialTheme.typography.titleMedium)
        Row(verticalAlignment = Alignment.CenterVertically, content = trailing)
    }
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(text, modifier.padding(start = 36.dp, end = 36.dp, bottom = 6.dp), style = MaterialTheme.typography.labelMedium, color = Brand.Secondary)
}

@Composable
fun Note(text: String) {
    Text(text, Modifier.padding(start = 36.dp, end = 36.dp, top = 8.dp), style = MaterialTheme.typography.bodySmall, color = Brand.Secondary)
}

/** iOS-style grouped list: white rounded card, rows separated by hairlines. */
@Composable
fun GroupCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier.padding(horizontal = 16.dp).fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Brand.Card),
        content = content,
    )
}

@Composable
fun RowDivider() = HorizontalDivider(Modifier.padding(start = 16.dp), thickness = 0.5.dp, color = Brand.Separator)

@Composable
fun ValueRow(title: String, value: String = "", onClick: (() -> Unit)? = null) {
    Row(
        Modifier.fillMaxWidth().then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .heightIn(min = 50.dp).padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(title, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge, color = Brand.Ink)
        if (value.isNotEmpty()) Text(value, style = MaterialTheme.typography.bodyMedium, color = Brand.Secondary)
        if (onClick != null) Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = Color(0xFFB5AEA2))
    }
}

@Composable
fun SwitchRow(title: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 52.dp).padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge, color = Brand.Ink)
        Switch(
            checked = checked, onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White, checkedTrackColor = Brand.Green, checkedBorderColor = Brand.Green,
                uncheckedThumbColor = Color.White, uncheckedTrackColor = Color(0xFFE4DED3), uncheckedBorderColor = Color(0xFFE4DED3),
            ),
        )
    }
}

/** Segmented control with a white "thumb" on a warm grey track. */
@Composable
fun Segmented(options: List<String>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier, fonts: List<FontFamily?>? = null) {
    Row(
        modifier.padding(horizontal = 16.dp).fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Brand.Fill).padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        options.forEachIndexed { i, label ->
            val on = i == selected
            Box(
                Modifier.weight(1f).height(38.dp).clip(RoundedCornerShape(9.dp))
                    .background(if (on) Brand.Card else Color.Transparent).clickable { onSelect(i) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                    fontFamily = fonts?.getOrNull(i) ?: MaterialTheme.typography.bodyLarge.fontFamily,
                    color = if (on) Brand.Ink else Brand.Secondary, maxLines = 1,
                )
            }
        }
    }
}

@Composable
fun Pill(text: String, on: Boolean, onClick: () -> Unit, fontFamily: FontFamily? = null, count: String? = null) {
    Row(
        Modifier.clip(CircleShape)
            .background(if (on) Brand.Kesari else Brand.Card)
            .border(1.dp, if (on) Brand.Kesari else Brand.Separator, CircleShape)
            .clickable(onClick = onClick).height(34.dp).padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Text(text, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold, fontFamily = fontFamily ?: MaterialTheme.typography.bodyLarge.fontFamily,
            color = if (on) Color.White else Color(0xFF3E3831))
        if (count != null) Text(count, fontSize = 12.sp, color = if (on) Color.White.copy(alpha = 0.8f) else Brand.Tertiary)
    }
}

@Composable
fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier.fillMaxWidth().height(54.dp).clip(RoundedCornerShape(16.dp)).background(Brand.Kesari).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Text(text, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.SemiBold) }
}

@Composable
fun AppTile(size: Int = 40, fontSize: Int = 19) {
    Box(
        Modifier.size(size.dp).clip(RoundedCornerShape((size * 0.28f).dp)).background(Brand.Kesari),
        contentAlignment = Alignment.Center,
    ) { Text("भा", fontFamily = TiroSanskrit, fontSize = fontSize.sp, color = Color.White, modifier = Modifier.padding(top = (size / 9).dp)) }
}

/** The mool shloka in the chosen script (Devanagari, Bengali or IAST). */
@Composable
fun ShlokaText(verse: Verse, script: SanskritScript, color: Color, size: Float, center: Boolean = false, lines: Int = Int.MAX_VALUE) {
    val (text, font) = when (script) {
        SanskritScript.DEVANAGARI -> verse.sa.take(lines).joinToString("\n") to TiroSanskrit
        SanskritScript.BENGALI -> verse.sa.take(lines).joinToString("\n") { Transliterate.toBengali(it) } to TiroBangla
        SanskritScript.IAST -> verse.iast.take(lines).joinToString("\n") to Literata
    }
    Text(
        text, fontFamily = font, fontSize = size.sp, lineHeight = (size * 1.8f).sp, color = color,
        textAlign = if (center) TextAlign.Center else TextAlign.Start, modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
fun Dot(color: Color, size: Int = 8) = Box(Modifier.size(size.dp).clip(CircleShape).background(color))

@Composable
fun VSpace(h: Int) = Spacer(Modifier.height(h.dp))

@Composable
fun HSpace(w: Int) = Spacer(Modifier.width(w.dp))
