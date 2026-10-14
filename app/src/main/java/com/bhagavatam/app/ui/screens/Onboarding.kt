package com.bhagavatam.app.ui.screens

import androidx.compose.ui.res.painterResource
import com.bhagavatam.app.ui.components.Ic
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.selection.selectable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bhagavatam.app.data.BENGALI_READY
import com.bhagavatam.app.data.Lang
import com.bhagavatam.app.data.SampleData
import com.bhagavatam.app.data.tr
import com.bhagavatam.app.state.AppState
import com.bhagavatam.app.ui.components.AppTile
import com.bhagavatam.app.ui.components.GroupCard
import com.bhagavatam.app.ui.components.NavBar
import com.bhagavatam.app.ui.components.PrimaryButton
import com.bhagavatam.app.ui.components.RowDivider
import com.bhagavatam.app.ui.components.SectionLabel
import com.bhagavatam.app.ui.components.Segmented
import com.bhagavatam.app.ui.components.ShlokaText
import com.bhagavatam.app.ui.components.SwitchRow
import com.bhagavatam.app.ui.components.VSpace
import com.bhagavatam.app.ui.theme.Brand
import com.bhagavatam.app.ui.theme.Radius
import com.bhagavatam.app.ui.theme.HindSiliguri
import com.bhagavatam.app.ui.theme.Jakarta
import com.bhagavatam.app.ui.theme.EnglishReading
import com.bhagavatam.app.ui.theme.Mukta
import com.bhagavatam.app.ui.theme.NotoSerifBengali
import com.bhagavatam.app.ui.theme.TiroHindi
import com.bhagavatam.app.ui.theme.NotoDevanagari

/** Two dots: where the person is in the two first-run steps. */
@Composable
internal fun StepDots(current: Int, total: Int = 2, onDark: Boolean = false) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        for (i in 1..total) {
            val on = i == current
            Box(
                Modifier.height(6.dp).width(if (on) 22.dp else 6.dp).clip(RoundedCornerShape(3.dp))
                    .background(if (on) Brand.Kesari else if (onDark) Color.White.copy(alpha = 0.45f) else Brand.Separator)
            )
        }
    }
}

@Composable
fun OnboardingLanguageScreen(state: AppState, onContinue: () -> Unit) {
    val s = state.strings
    val ui = state.uiLang
    Column(Modifier.fillMaxSize().background(Brand.Paper).navigationBarsPadding()) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            // Hero: the Kangra painting, fading into the page, with the name of the book on it.
            Box(Modifier.fillMaxWidth().height(300.dp)) {
                androidx.compose.foundation.Image(
                    painterResource(com.bhagavatam.app.R.drawable.ch_0_1), null, Modifier.matchParentSize(),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                )
                Box(
                    Modifier.matchParentSize().background(
                        androidx.compose.ui.graphics.Brush.verticalGradient(
                            0f to Color(0x66000000), 0.35f to Color(0x11000000), 0.78f to Brand.Paper.copy(alpha = 0.85f), 1f to Brand.Paper,
                        )
                    )
                )
                Box(Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(top = 14.dp, end = 20.dp)) { StepDots(1, onDark = true) }
                Column(Modifier.align(Alignment.BottomCenter).padding(bottom = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("श्रीमद्भागवतम्", fontFamily = NotoDevanagari, fontSize = 24.sp, color = Brand.Sindoor)
                    Text("Bhagavatam", fontFamily = EnglishReading, fontSize = 34.sp, fontWeight = FontWeight.Medium, color = Brand.Ink)
                }
            }
            VSpace(14)
            Text(
                s.chooseLanguage, Modifier.fillMaxWidth().padding(horizontal = 32.dp), textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyLarge, color = Brand.Secondary,
            )
            VSpace(18)
            Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                listOf(Triple(Lang.EN, "English", Jakarta), Triple(Lang.HI, "हिन्दी", Mukta), Triple(Lang.BN, "বাংলা", HindSiliguri)).forEach { (lang, native, font) ->
                    LanguageOption(
                        native, mapOf("EN" to "English", "HI" to "Hindi", "BN" to "Bengali")[lang.name]!!, font, state.uiLang == lang,
                    ) { state.updateUiLang(lang) }
                }
            }
            Text(
                s.changeLater + "  " + tr(ui, "Works fully offline. No sign-in.", "पूरी तरह ऑफ़लाइन। साइन-इन की ज़रूरत नहीं।", "সম্পূর্ণ অফলাইন। সাইন-ইন লাগে না।"),
                Modifier.padding(start = 28.dp, end = 28.dp, top = 14.dp), style = MaterialTheme.typography.bodySmall, color = Brand.Secondary,
            )
            VSpace(16)
            // The same verse in the chosen language, so the choice is visible and not only a name in a list.
            Row(
                Modifier.padding(horizontal = 16.dp).fillMaxWidth().clip(Radius.group).background(Brand.Card),
            ) {
                Box(Modifier.width(4.dp).fillMaxHeight().background(Brand.Gold))
                val v = SampleData.adhyaya1.first()
                Text(
                    v.translation(state.uiLang), Modifier.padding(16.dp), fontFamily = readingFont(state.uiLang), fontSize = 15.sp, lineHeight = 23.sp,
                    color = Brand.Ink, maxLines = 3, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                )
            }
            VSpace(16)
        }
        Column(Modifier.padding(start = 20.dp, end = 20.dp, bottom = 20.dp, top = 8.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            PrimaryButton(s.continueBtn, onContinue)
            Text(s.textsFrom, Modifier.fillMaxWidth(), textAlign = TextAlign.Center, fontSize = 12.sp, color = Brand.Secondary)
        }
    }
}

@Composable
fun LanguageOption(native: String, sub: String, font: FontFamily, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    Row(
        Modifier.fillMaxWidth().clip(shape)
            .background(if (selected) Brand.KesariTint else Brand.Card)
            .border(if (selected) 2.dp else 1.dp, if (selected) Brand.Kesari else Brand.Separator, shape)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick).heightIn(min = 68.dp).padding(horizontal = 18.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(native, fontFamily = font, fontSize = 20.sp, lineHeight = 28.sp, color = Brand.Ink, fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal)
            // English reads the same twice; skip the repeat.
            if (sub != native) Text(sub, fontSize = 13.sp, lineHeight = 18.sp, color = Brand.Secondary)
        }
        if (selected) Icon(painterResource(Ic.Check), contentDescription = null, tint = Brand.Kesari)
    }
}

@Composable
fun OnboardingPaathScreen(state: AppState, onBack: () -> Unit, onStart: () -> Unit) {
    val s = state.strings
    val langs = listOf(Lang.SA, Lang.HI, Lang.BN, Lang.EN).filter { BENGALI_READY || it != Lang.BN }
    Column(Modifier.fillMaxSize().background(Brand.Paper).navigationBarsPadding()) {
        NavBar(s.back, onBack) { Box(Modifier.padding(end = 12.dp)) { StepDots(2) } }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(s.paathQuestion, fontSize = 30.sp, lineHeight = 36.sp, fontWeight = FontWeight.Bold, color = Brand.Ink)
                Text(s.paathSub, style = MaterialTheme.typography.bodyLarge, color = Brand.Secondary)
            }
            SectionLabel(s.paathLanguage)
            Segmented(
                options = listOf("संस्कृत", "हिन्दी", "বাংলা", "English").filterIndexed { i, _ -> BENGALI_READY || i != 2 },
                selected = langs.indexOf(state.readLang),
                onSelect = { state.updateReadLang(langs[it]) },
                fonts = listOf(NotoDevanagari, TiroHindi, NotoSerifBengali, Jakarta).filterIndexed { i, _ -> BENGALI_READY || i != 2 },
            )
            VSpace(24)
            SectionLabel(s.showAlongside)
            GroupCard {
                SwitchRow(s.transliteration, state.showIast, state::updateShowIast); RowDivider()
                SwitchRow(s.hindiTr, state.showHi, state::updateShowHi); RowDivider()
                if (BENGALI_READY) { SwitchRow(s.bengaliTr, state.showBn, state::updateShowBn); RowDivider() }
                SwitchRow(s.englishTr, state.showEn, state::updateShowEn)
            }
            VSpace(24)
            Row(Modifier.padding(horizontal = 16.dp).fillMaxWidth().clip(Radius.group).background(Brand.Card)) {
              Box(Modifier.width(4.dp).fillMaxHeight().background(Brand.Gold))
              Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                val v = SampleData.adhyaya1.first()
                Text(s.preview, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Brand.Gold, letterSpacing = 0.6.sp)
                if (state.readLang == Lang.SA) ShlokaText(v, state.script, Brand.Sindoor, 16f, lines = 2)
                val tr = if (state.readLang == Lang.SA) state.alongsideLayers().first() else state.readLang
                Text(v.translation(tr), fontFamily = readingFont(tr), fontSize = 14.sp, lineHeight = 21.sp, color = Brand.Ink, maxLines = 3, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
              }
            }
            Spacer(Modifier.heightIn(min = 24.dp))
        }
        Box(Modifier.padding(start = 20.dp, end = 20.dp, bottom = 20.dp)) { PrimaryButton(s.startReading, onStart) }
    }
}

/** The reading font for a translation language. */
fun readingFont(l: Lang): FontFamily = when (l) {
    Lang.HI -> TiroHindi
    Lang.BN -> NotoSerifBengali
    Lang.OR -> androidx.compose.ui.text.font.FontFamily.Serif
    Lang.SA -> NotoDevanagari
    Lang.EN -> EnglishReading
}
