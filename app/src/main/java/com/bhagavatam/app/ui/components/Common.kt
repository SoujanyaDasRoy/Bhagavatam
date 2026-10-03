package com.bhagavatam.app.ui.components

import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextLayoutResult
import com.bhagavatam.app.data.wordAt
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import com.bhagavatam.app.ui.theme.Motion
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.ui.semantics.Role
import com.bhagavatam.app.ui.theme.Radius
import com.bhagavatam.app.ui.theme.Touch
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
import com.bhagavatam.app.ui.theme.EnglishReading
import com.bhagavatam.app.ui.theme.NotoSerifBengali
import com.bhagavatam.app.ui.theme.NotoDevanagari
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
            Icon(painterResource(Ic.ArrowBackIos), contentDescription = null, tint = Brand.Kesari, modifier = Modifier.size(20.dp))
            Text(backLabel, color = Brand.Kesari, fontSize = 16.sp, fontWeight = FontWeight.Medium)
        }
        Text(title, Modifier.weight(1f), textAlign = TextAlign.Center, style = MaterialTheme.typography.titleMedium)
        Row(verticalAlignment = Alignment.CenterVertically, content = trailing)
    }
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(text, modifier.padding(start = 36.dp, end = 36.dp, bottom = 6.dp), style = MaterialTheme.typography.labelMedium.copy(fontSize = 13.sp, letterSpacing = 0.2.sp), color = Brand.Secondary)
}

/** Heading of a block of content on a screen, with an optional trailing action. One style everywhere. */
@Composable
fun SectionHeading(title: String, modifier: Modifier = Modifier, action: String? = null, onAction: () -> Unit = {}) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, Modifier.weight(1f), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Brand.Ink)
        if (action != null) Text(
            action,
            Modifier.clip(RoundedCornerShape(10.dp)).clickable(role = Role.Button, onClick = onAction).heightIn(min = Touch.min).padding(horizontal = 8.dp).wrapContentHeight(Alignment.CenterVertically),
            fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Brand.Kesari,
        )
    }
}

/** Slider with a round thumb and a thin track, replacing the Material 3 bar thumb and stop dot. */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun AppSlider(
    value: Float, onValueChange: (Float) -> Unit, modifier: Modifier = Modifier, valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    enabled: Boolean = true, onValueChangeFinished: (() -> Unit)? = null, active: Color = Brand.Kesari, inactive: Color = Brand.Fill,
) {
    val colors = SliderDefaults.colors(thumbColor = active, activeTrackColor = active, inactiveTrackColor = inactive)
    Slider(
        value = value, onValueChange = onValueChange, modifier = modifier, enabled = enabled, valueRange = valueRange,
        onValueChangeFinished = onValueChangeFinished, colors = colors,
        thumb = { Box(Modifier.size(22.dp).clip(CircleShape).background(if (enabled) active else active.copy(alpha = 0.4f))) },
        track = { st ->
            val frac = ((st.value - valueRange.start) / (valueRange.endInclusive - valueRange.start)).coerceIn(0f, 1f)
            Box(Modifier.fillMaxWidth().height(4.dp).clip(CircleShape).background(inactive)) {
                Box(Modifier.fillMaxWidth(frac).fillMaxHeight().background(if (enabled) active else active.copy(alpha = 0.4f)))
            }
        },
    )
}

/** Asks before something that cannot be undone. The confirming action is in the danger colour. */
@Composable
fun ConfirmDialog(title: String, message: String, confirm: String, cancel: String, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss, containerColor = Brand.Card, shape = Radius.card,
        title = { Text(title, color = Brand.Ink, style = MaterialTheme.typography.titleLarge) },
        text = { Text(message, color = Brand.Secondary, style = MaterialTheme.typography.bodyMedium) },
        confirmButton = {
            androidx.compose.material3.TextButton(onClick = { onConfirm(); onDismiss() }) {
                Text(confirm, color = com.bhagavatam.app.ui.theme.LocalAppColors.current.danger, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = { androidx.compose.material3.TextButton(onClick = onDismiss) { Text(cancel, color = Brand.Ink) } },
    )
}

@Composable
fun Note(text: String) {
    Text(text, Modifier.padding(start = 36.dp, end = 36.dp, top = 8.dp), style = MaterialTheme.typography.bodySmall, color = Brand.Secondary)
}

/** iOS-style grouped list: white rounded card, rows separated by hairlines. */
@Composable
fun GroupCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier.padding(horizontal = 16.dp).fillMaxWidth().clip(Radius.group).background(Brand.Card),
        content = content,
    )
}

@Composable
fun RowDivider() = HorizontalDivider(Modifier.padding(start = 16.dp), thickness = 0.5.dp, color = Brand.Separator)

@Composable
fun ValueRow(title: String, value: String = "", icon: Int? = null, onClick: (() -> Unit)? = null) {
    Row(
        Modifier.fillMaxWidth().then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
            .heightIn(min = if (icon != null) 58.dp else 50.dp).padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(if (icon != null) 12.dp else 8.dp),
    ) {
        if (icon != null) Box(Modifier.size(34.dp).clip(RoundedCornerShape(10.dp)).background(Brand.KesariTint), contentAlignment = Alignment.Center) {
            Icon(painterResource(icon), null, tint = Brand.Kesari, modifier = Modifier.size(19.dp))
        }
        Text(title, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge, color = Brand.Ink)
        if (value.isNotEmpty()) Text(value, style = MaterialTheme.typography.bodyMedium, color = Brand.Secondary)
        if (onClick != null) Icon(painterResource(Ic.KeyboardArrowRight), null, tint = Brand.Chevron)
    }
}

@Composable
fun SwitchRow(title: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    // The whole row toggles, so the target is the row, not just the switch.
    Row(
        Modifier.fillMaxWidth().toggleable(value = checked, role = Role.Switch, onValueChange = onChange).heightIn(min = 52.dp).padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge, color = Brand.Ink)
        Switch(
            checked = checked, onCheckedChange = null,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Brand.OnKesari, checkedTrackColor = Brand.Kesari, checkedBorderColor = Brand.Kesari,
                uncheckedThumbColor = Brand.Secondary, uncheckedTrackColor = Brand.Fill, uncheckedBorderColor = Brand.Secondary,
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
            val thumb by animateColorAsState(if (on) Brand.Card else Brand.Card.copy(alpha = 0f), tween(Motion.sheet), label = "segThumb")
            val ink by animateColorAsState(if (on) Brand.Ink else Brand.Secondary, tween(Motion.sheet), label = "segInk")
            Box(
                Modifier.weight(1f).height(44.dp).clip(RoundedCornerShape(9.dp))
                    .background(thumb).selectable(selected = on, role = Role.Tab) { onSelect(i) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label, Modifier.opticallyCentred(fonts?.getOrNull(i)), fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                    fontFamily = fonts?.getOrNull(i) ?: MaterialTheme.typography.bodyLarge.fontFamily,
                    color = ink, maxLines = 1,
                )
            }
        }
    }
}

/** Tiro Devanagari Hindi has tall ascent metrics, so its labels sit about 2dp high in a centred chip; nudge them down. */
fun Modifier.opticallyCentred(font: FontFamily?): Modifier = if (font === com.bhagavatam.app.ui.theme.TiroHindi) offset(y = 2.dp) else this

@Composable
fun Pill(text: String, on: Boolean, onClick: () -> Unit, fontFamily: FontFamily? = null, count: String? = null) {
    // The visible pill is 36dp; the touch target around it is 48dp.
    val fill by animateColorAsState(if (on) Brand.Kesari else Brand.Card, tween(Motion.press), label = "pillFill")
    val edge by animateColorAsState(if (on) Brand.Kesari else Brand.Separator, tween(Motion.press), label = "pillEdge")
    Box(Modifier.heightIn(min = Touch.min).clip(CircleShape).selectable(selected = on, role = Role.Tab, onClick = onClick), contentAlignment = Alignment.Center) {
        Row(
            Modifier.height(36.dp).clip(CircleShape)
                .background(fill)
                .border(1.dp, edge, CircleShape)
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Text(text, Modifier.opticallyCentred(fontFamily), fontSize = 14.sp, fontWeight = FontWeight.SemiBold, fontFamily = fontFamily ?: MaterialTheme.typography.bodyLarge.fontFamily,
                color = if (on) Brand.OnKesari else Brand.Ink)
            if (count != null) Text(count, fontSize = 12.sp, color = if (on) Brand.OnKesari.copy(alpha = 0.85f) else Brand.Secondary)
        }
    }
}

@Composable
fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier.fillMaxWidth().height(54.dp).tappable(Radius.group, onClick = onClick).background(Brand.Kesari),
        contentAlignment = Alignment.Center,
    ) { Text(text, color = Brand.OnKesari, fontSize = 17.sp, fontWeight = FontWeight.SemiBold) }
}

/**
 * A tappable surface that dips slightly while pressed. Put it first in the chain so the whole card scales and
 * paints its background after it. Follows the system animation scale, so it is instant when animations are off.
 */
@Composable
fun Modifier.tappable(shape: Shape, onClickLabel: String? = null, role: Role = Role.Button, onClick: () -> Unit): Modifier {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.97f else 1f, tween(Motion.press), label = "press")
    return this.graphicsLayer { scaleX = scale; scaleY = scale }.clip(shape)
        .clickable(interactionSource = source, indication = LocalIndication.current, onClickLabel = onClickLabel, role = role, onClick = onClick)
}

/** Fades and lifts a block into place once, [index] steps after the first. Pass `enabled = false` to skip. */
@Composable
fun Modifier.reveal(index: Int, enabled: Boolean = true): Modifier {
    val progress = remember { Animatable(if (enabled) 0f else 1f) }
    LaunchedEffect(Unit) { if (enabled) progress.animateTo(1f, tween(Motion.screen, delayMillis = index * 60, easing = FastOutSlowInEasing)) }
    val lift = with(LocalDensity.current) { 20.dp.toPx() }
    return this.graphicsLayer { alpha = progress.value; translationY = (1f - progress.value) * lift }
}

@Composable
fun AppTile(size: Int = 40, fontSize: Int = 19) {
    androidx.compose.foundation.Image(
        painter = androidx.compose.ui.res.painterResource(com.bhagavatam.app.R.drawable.logo_mark),
        contentDescription = "Bhagavatam", modifier = Modifier.size(size.dp).clip(RoundedCornerShape((size * 0.24f).dp)),
    )
}

private val verseMarker = Regex("॥\\s*([^\\s॥]+)\\s*॥")

/** Binds the closing "॥ २ ॥" with no-break spaces and word joiners (a danda alone still lets Android break before a no-break space). */
fun keepMarkerTogether(s: String) = verseMarker.replace(s) { "॥⁠ ⁠${it.groupValues[1]}⁠ ⁠॥" }

/** The mool shloka in the chosen script (Devanagari, Bengali or IAST). */
@Composable
fun ShlokaText(
    verse: Verse,
    script: SanskritScript,
    color: Color,
    size: Float,
    center: Boolean = false,
    lines: Int = Int.MAX_VALUE,
    onWordLongPress: ((String) -> Unit)? = null,
) {
    val (text, font) = when (script) {
        SanskritScript.DEVANAGARI -> verse.sa.take(lines).joinToString("\n") to NotoDevanagari
        SanskritScript.BENGALI -> verse.sa.take(lines).joinToString("\n") { Transliterate.toBengali(it) } to NotoSerifBengali
        SanskritScript.IAST -> verse.iast.take(lines).joinToString("\n") to EnglishReading
    }
    val processed = keepMarkerTogether(text)
    var layoutResult by remember(processed) { mutableStateOf<TextLayoutResult?>(null) }
    Text(
        processed,
        fontFamily = font,
        fontSize = size.sp,
        lineHeight = (size * 1.8f).sp,
        color = color,
        textAlign = if (center) TextAlign.Center else TextAlign.Start,
        modifier = Modifier.fillMaxWidth().then(
            if (onWordLongPress != null) {
                Modifier.pointerInput(processed) {
                    detectTapGestures(
                        onLongPress = { offset ->
                            val layout = layoutResult ?: return@detectTapGestures
                            val charOffset = layout.getOffsetForPosition(offset).coerceIn(0, processed.length)
                            val range = wordAt(processed, charOffset)
                            if (range != null) {
                                val w = processed.substring(range).trim()
                                if (w.isNotEmpty()) onWordLongPress(w)
                            }
                        }
                    )
                }
            } else Modifier
        ),
        onTextLayout = { layoutResult = it },
    )
}

@Composable
fun Dot(color: Color, size: Int = 8) = Box(Modifier.size(size.dp).clip(CircleShape).background(color))

@Composable
fun VSpace(h: Int) = Spacer(Modifier.height(h.dp))

@Composable
fun HSpace(w: Int) = Spacer(Modifier.width(w.dp))
