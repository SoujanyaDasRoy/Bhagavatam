package com.bhagavatam.app.ui.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bhagavatam.app.data.Lang
import com.bhagavatam.app.data.SanskritScript
import com.bhagavatam.app.data.Verse
import com.bhagavatam.app.data.tr
import com.bhagavatam.app.state.AppState
import com.bhagavatam.app.ui.components.GroupCard
import com.bhagavatam.app.ui.components.Ic
import com.bhagavatam.app.ui.components.Note
import com.bhagavatam.app.ui.components.SectionLabel
import com.bhagavatam.app.ui.components.Segmented
import com.bhagavatam.app.ui.theme.Brand
import com.bhagavatam.app.ui.theme.LocalReaderColors
import com.bhagavatam.app.ui.theme.NotoDevanagari
import com.bhagavatam.app.ui.theme.NotoSerifBengali
import com.bhagavatam.app.ui.theme.EnglishReading

/*
 * The reader's language switcher.
 *
 * What the reader sees: one small pill in the top bar that names what the page is in ("বাংলা", or "Shloka + translation").
 * Tapping it opens a sheet that asks one thing at a time:
 *   1. Show: "Translation only" or "With shloka"
 *   2. translation only  -> pick ONE language (radio list)        the audio is read in it too
 *      with shloka       -> pick the script, and which translations sit under the shloka (switches)   the audio reads the Sanskrit
 * Languages that have no text for the open chapter are shown greyed out with the reason, not hidden and not an error after a tap.
 */

private val TRANSLATIONS = listOf(Lang.HI, Lang.BN, Lang.EN)

/** Translations that have text in at least one verse of the open chapter. */
internal fun availableTranslations(verses: List<Verse>): Set<Lang> = TRANSLATIONS.filter { l -> verses.any { it.hasText(l) } }.toSet()

/** A language's name written in its own script. */
internal fun ownName(l: Lang) = when (l) { Lang.HI -> "हिन्दी"; Lang.BN -> "বাংলা"; Lang.EN -> "English"; Lang.SA -> "संस्कृत" }

/** A language's name written in the app language (the small line under the own-script name). */
internal fun nameIn(ui: Lang, l: Lang) = when (l) {
    Lang.HI -> tr(ui, "Hindi", "हिन्दी", "হিন্দি")
    Lang.BN -> tr(ui, "Bengali", "बंगाली", "বাংলা")
    Lang.EN -> tr(ui, "English", "अंग्रेज़ी", "ইংরেজি")
    Lang.SA -> tr(ui, "Sanskrit", "संस्कृत", "সংস্কৃত")
}

/** The text on the pill: the language being read, or a plain description when the shloka is shown too. */
internal fun pillText(ui: Lang, withShloka: Boolean, readLang: Lang): String =
    if (withShloka) tr(ui, "Shloka + translation", "श्लोक + अनुवाद", "শ্লোক + অনুবাদ") else ownName(if (readLang == Lang.SA) Lang.EN else readLang)

/** Changing to a language that has no text for this chapter is not offered. */
internal fun canPick(available: Set<Lang>, l: Lang) = l in available

/** Turning off the last translation under the shloka would leave nothing to read, so it is refused. */
internal fun canTurnOff(layersOn: Int) = layersOn > 1

/** The pill in the top bar. [compact] shows only the icon (used once the chapter title has moved into the bar). */
@Composable
fun LanguagePill(state: AppState, compact: Boolean, onClick: () -> Unit) {
    val c = LocalReaderColors.current
    val ui = state.uiLang
    val text = pillText(ui, state.showSanskrit, state.readLang)
    val font = if (state.showSanskrit) null else readingFont(if (state.readLang == Lang.SA) Lang.EN else state.readLang)
    val description = tr(ui, "Reading language: $text. Tap to change.", "पढ़ने की भाषा: $text। बदलने के लिए छुएँ।", "পড়ার ভাষা: $text। বদলাতে ছুঁয়ে দেখুন।")
    Box(
        Modifier.height(48.dp).widthIn(min = 48.dp).semantics(mergeDescendants = true) { contentDescription = description }
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            Modifier.height(36.dp).clip(CircleShape).background(c.surface).border(1.dp, c.separator, CircleShape)
                .padding(horizontal = if (compact) 10.dp else 12.dp).animateContentSize(),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(painterResource(Ic.Languages), null, tint = c.accent, modifier = Modifier.size(18.dp))
            if (!compact) Text(
                text, Modifier.widthIn(max = 150.dp), fontFamily = font, fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                color = c.ink, maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
            Icon(painterResource(Ic.KeyboardArrowDown), null, tint = c.secondary, modifier = Modifier.size(16.dp))
        }
    }
}

/** One language row: the name in its own script, a small line under it, and a radio button or a switch on the right. */
@Composable
private fun LanguageRow(name: String, font: FontFamily?, sub: String, enabled: Boolean, on: Boolean, asSwitch: Boolean, onChange: () -> Unit) {
    val base = Modifier.fillMaxWidth().heightIn(min = 64.dp)
    val modifier = if (asSwitch) base.toggleable(value = on, enabled = enabled, role = Role.Switch) { onChange() }
    else base.selectable(selected = on, enabled = enabled, role = Role.RadioButton) { onChange() }
    Row(modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            // Only the name and the control fade when a language is unavailable; the reason stays fully readable.
            Text(name, Modifier.alpha(if (enabled) 1f else 0.5f), fontFamily = font, fontSize = 18.sp, fontWeight = FontWeight.Medium, color = Brand.Ink)
            Text(sub, fontSize = 13.sp, color = Brand.Secondary)
        }
        if (asSwitch) Switch(
            checked = on, onCheckedChange = null, enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Brand.OnKesari, checkedTrackColor = Brand.Kesari, checkedBorderColor = Brand.Kesari,
                uncheckedThumbColor = Brand.Secondary, uncheckedTrackColor = Brand.Fill, uncheckedBorderColor = Brand.Secondary,
            ),
        ) else RadioButton(selected = on, onClick = null, enabled = enabled, colors = RadioButtonDefaults.colors(selectedColor = Brand.Kesari, unselectedColor = Brand.Secondary))
    }
}

private fun fontFor(l: Lang): FontFamily? = when (l) { Lang.HI -> NotoDevanagari; Lang.BN -> NotoSerifBengali; Lang.SA -> NotoDevanagari; Lang.EN -> EnglishReading }

/** The sheet opened by the pill. [verses] are the open chapter's verses, used to grey out languages it does not have. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguageSheet(state: AppState, verses: List<Verse>, onDismiss: () -> Unit) {
    val ui = state.uiLang
    val have = remember(verses) { availableTranslations(verses) }
    val withShloka = state.showSanskrit
    val layers = state.alongsideLayers()
    val notAvailable = tr(ui, "Not available for this chapter", "इस अध्याय के लिए उपलब्ध नहीं", "এই অধ্যায়ের জন্য পাওয়া যায়নি")

    fun setMode(withShlokaNow: Boolean) {
        if (withShlokaNow == withShloka) return
        if (withShlokaNow) state.updateReadLang(Lang.SA) else state.updateReadLang(layers.first())
        state.restartNarration()
    }
    fun pick(l: Lang) {
        when (l) { Lang.HI -> state.updateShowHi(true); Lang.BN -> state.updateShowBn(true); else -> state.updateShowEn(true) }
        state.updateReadLang(l)
        state.restartNarration()
    }
    fun toggleLayer(l: Lang) {
        val on = l in layers
        if (on && !canTurnOff(layers.size)) return
        when (l) { Lang.HI -> state.updateShowHi(!on); Lang.BN -> state.updateShowBn(!on); else -> state.updateShowEn(!on) }
    }

    // Open fully, so every language row is in view and none sits half cut off at the bottom edge.
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), containerColor = Brand.Paper) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(bottom = 28.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Text(tr(ui, "Reading language", "पढ़ने की भाषा", "পড়ার ভাষা"), Modifier.padding(horizontal = 20.dp), fontSize = 22.sp, fontWeight = FontWeight.Medium, color = Brand.Ink)

            Column {
                SectionLabel(tr(ui, "Show", "दिखाएँ", "দেখান"))
                Segmented(
                    listOf(tr(ui, "Translation only", "केवल अनुवाद", "শুধু অনুবাদ"), tr(ui, "With shloka", "श्लोक सहित", "শ্লোক সহ")),
                    if (withShloka) 1 else 0, { setMode(it == 1) },
                )
            }

            if (!withShloka) {
                Column {
                    SectionLabel(tr(ui, "Language", "भाषा", "ভাষা"))
                    GroupCard {
                        TRANSLATIONS.forEachIndexed { i, l ->
                            if (i > 0) androidx.compose.material3.HorizontalDivider(Modifier.padding(start = 16.dp), thickness = 0.5.dp, color = Brand.Separator)
                            val ok = canPick(have, l)
                            LanguageRow(ownName(l), fontFor(l), if (ok) nameIn(ui, l) else notAvailable, ok, state.readLang == l, false) { pick(l) }
                        }
                    }
                    Note(tr(ui, "The audio is read in this language too.", "ऑडियो भी इसी भाषा में सुनाया जाता है।", "অডিও-ও এই ভাষায় শোনানো হয়।"))
                }
            } else {
                Column {
                    SectionLabel(tr(ui, "Shloka script", "श्लोक की लिपि", "শ্লোকের লিপি"))
                    Segmented(
                        listOf("देवनागरी", "বাংলা", "Roman"), state.script.ordinal, { state.updateScript(SanskritScript.entries[it]) },
                        fonts = listOf(NotoDevanagari, NotoSerifBengali, null),
                    )
                    if (state.script != SanskritScript.IAST) {
                        GroupCard(Modifier.padding(top = 10.dp)) {
                            Row(
                                Modifier.fillMaxWidth().toggleable(value = state.showIast, role = Role.Switch) { state.updateShowIast(it) }.heightIn(min = 56.dp).padding(horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(tr(ui, "Also show Roman letters", "रोमन अक्षर भी दिखाएँ", "রোমান অক্ষরও দেখান"), Modifier.weight(1f), fontSize = 16.sp, color = Brand.Ink)
                                Switch(
                                    checked = state.showIast, onCheckedChange = null,
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Brand.OnKesari, checkedTrackColor = Brand.Kesari, checkedBorderColor = Brand.Kesari,
                                        uncheckedThumbColor = Brand.Secondary, uncheckedTrackColor = Brand.Fill, uncheckedBorderColor = Brand.Secondary,
                                    ),
                                )
                            }
                        }
                    }
                }
                Column {
                    SectionLabel(tr(ui, "Translations under the shloka", "श्लोक के नीचे अनुवाद", "শ্লোকের নীচে অনুবাদ"))
                    GroupCard {
                        TRANSLATIONS.forEachIndexed { i, l ->
                            if (i > 0) androidx.compose.material3.HorizontalDivider(Modifier.padding(start = 16.dp), thickness = 0.5.dp, color = Brand.Separator)
                            val ok = canPick(have, l)
                            LanguageRow(ownName(l), fontFor(l), if (ok) nameIn(ui, l) else notAvailable, ok, l in layers && ok, true) { toggleLayer(l) }
                        }
                    }
                    Note(tr(ui, "At least one stays on. The audio reads the Sanskrit shloka.", "कम से कम एक चालू रहता है। ऑडियो संस्कृत श्लोक सुनाता है।", "অন্তত একটি চালু থাকে। অডিও সংস্কৃত শ্লোক শোনায়।"))
                }
            }
        }
    }
}
