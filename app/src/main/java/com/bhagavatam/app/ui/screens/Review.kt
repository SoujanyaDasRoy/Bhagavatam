package com.bhagavatam.app.ui.screens

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bhagavatam.app.data.Dictionary
import com.bhagavatam.app.data.Lang
import com.bhagavatam.app.data.ReviewScheduler
import com.bhagavatam.app.data.SampleData
import com.bhagavatam.app.data.localDigits
import com.bhagavatam.app.data.reviewAnswer
import com.bhagavatam.app.data.reviewPrompt
import com.bhagavatam.app.data.tr
import com.bhagavatam.app.state.AppState
import com.bhagavatam.app.ui.components.NavBar
import com.bhagavatam.app.ui.theme.Brand
import com.bhagavatam.app.ui.theme.Radius

private fun meaningOf(state: AppState, word: String, lang: Lang): String? =
    Dictionary.lookup(word, lang.code, state.preferredMeaningLang(lang.code)).firstOrNull()?.senses?.firstOrNull()?.gloss

/** The language a verse card is answered in: the reading language, or the first translation shown when the Sanskrit is the reading language. */
private fun answerLang(state: AppState): Lang = if (state.readLang == Lang.SA) state.alongsideLayers().first() else state.readLang

/** One card at a time: the prompt, Show, then Remembered or Again. Cards with nothing to show are skipped. */
@Composable
fun ReviewScreen(state: AppState, onBack: () -> Unit) {
    val ui = state.uiLang
    // The due cards are fixed when the screen opens, so answering one does not reshuffle the rest.
    val queue = remember { state.dueReviewCards().filter { reviewPrompt(it, SampleData::verse) != null } }
    var index by remember { mutableStateOf(0) }
    var shown by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize()) {
        NavBar(tr(ui, "Back", "वापस", "ফিরুন", "ପଛକୁ"), onBack)
        val card = queue.getOrNull(index)
        if (card == null) {
            val next = state.reviewCards.filter { it.stage < ReviewScheduler.steps.size }.minOfOrNull { it.dueAt }
            val days = next?.let { ((it - System.currentTimeMillis() + 86_399_999L) / 86_400_000L).toInt().coerceAtLeast(1) }
            Column(Modifier.fillMaxSize().padding(32.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                androidx.compose.material3.Icon(androidx.compose.ui.res.painterResource(com.bhagavatam.app.ui.components.Ic.History), null, tint = Brand.Secondary, modifier = Modifier.padding(bottom = 12.dp).size(36.dp))
                Text(
                    if (state.reviewCards.isEmpty()) tr(ui, "Nothing to review yet", "अभी दोहराने को कुछ नहीं", "এখনও দেখে নেওয়ার কিছু নেই", "ଏପର୍ଯ୍ୟନ୍ତ ସମୀକ୍ଷା ପାଇଁ କିଛି ନାହିଁ")
                    else tr(ui, "Nothing due", "अभी कुछ देय नहीं", "এখন কিছু বাকি নেই", "କିଛି ବାକି ନାହିଁ"),
                    fontSize = 22.sp, fontWeight = FontWeight.SemiBold, color = Brand.Ink, textAlign = TextAlign.Center,
                )
                Text(
                    if (state.reviewCards.isEmpty()) tr(ui, "Hold a shloka and choose Memorise, or add a word from its meaning card.", "किसी श्लोक को दबाए रखकर 'याद करें' चुनें, या अर्थ कार्ड से कोई शब्द जोड़ें।", "কোনো শ্লোক চেপে ধরে 'মনে রাখুন' বাছুন, বা অর্থ কার্ড থেকে শব্দ যোগ করুন।", "ଗୋଟିଏ ଶ୍ଳୋକକୁ ଧରି ରଖି ମନେରଖନ୍ତୁ ବାଛନ୍ତୁ, କିମ୍ବା ତାହାର ଅର୍ଥ କାର୍ଡ୍‌ରୁ ଗୋଟିଏ ଶବ୍ଦ ଯୋଡ଼ନ୍ତୁ।")
                    else if (days != null) tr(ui, "Next review in $days day(s).", "अगली समीक्षा ${localDigits("$days", ui)} दिन में।", "পরের পর্যালোচনা ${localDigits("$days", ui)} দিন পরে।", "ପରବର୍ତ୍ତୀ ସମୀକ୍ଷା $days ଦିନ ପରେ।")
                    else tr(ui, "Everything is learned.", "सब याद हो गया।", "সব মনে রাখা হয়েছে।", "ସବୁ ଶିଖାହୋଇଗଲା।"),
                    Modifier.padding(top = 8.dp), fontSize = 16.sp, lineHeight = 24.sp, color = Brand.Secondary, textAlign = TextAlign.Center,
                )
            }
            return@Column
        }
        val prompt = reviewPrompt(card, SampleData::verse).orEmpty()
        val answer = remember(card, shown) { if (shown) reviewAnswer(card, SampleData::verse, { w, l -> meaningOf(state, w, l) }, answerLang(state)) else null }
        Text(localDigits("${index + 1} / ${queue.size}", ui), Modifier.padding(horizontal = 20.dp), fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Brand.Gold)
        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            if (card.kind == "verse") Text(localDigits(card.key, ui), fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Brand.Gold)
            Text(prompt, fontSize = if (card.kind == "verse") 22.sp else 28.sp, lineHeight = 36.sp, color = Brand.Ink, fontWeight = FontWeight.Medium)
            if (shown) Text(
                answer ?: tr(ui, "No meaning found for this card.", "इस कार्ड का अर्थ नहीं मिला।", "এই কার্ডের অর্থ পাওয়া যায়নি।", "ଏହି କାର୍ଡ୍ ପାଇଁ ଅର୍ଥ ମିଳିଲା ନାହିଁ।"),
                fontSize = 18.sp, lineHeight = 30.sp, color = Brand.Ink,
            )
        }
        @Composable
        fun Btn(label: String, filled: Boolean, modifier: Modifier, onClick: () -> Unit) {
            Box(
                modifier.heightIn(min = 56.dp).clip(Radius.group)
                    .then(if (filled) Modifier.background(Brand.Kesari) else Modifier.background(Brand.Card).border(1.dp, Brand.Separator, Radius.group))
                    .clickable(role = Role.Button, onClick = onClick),
                contentAlignment = Alignment.Center,
            ) { Text(label, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = if (filled) Brand.OnKesari else Brand.Ink) }
        }
        fun respond(remembered: Boolean) { state.answerReview(card, remembered); shown = false; index++ }
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 96.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (!shown) Btn(tr(ui, "Show", "दिखाएँ", "দেখান", "ଦେଖାନ୍ତୁ"), true, Modifier.fillMaxWidth()) { shown = true }
            else Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Btn(tr(ui, "Again", "फिर से", "আবার", "ପୁଣି"), false, Modifier.weight(1f)) { respond(false) }
                Btn(tr(ui, "Remembered", "याद है", "মনে আছে", "ମନେ ଅଛି"), true, Modifier.weight(1f)) { respond(true) }
            }
            Box(Modifier.fillMaxWidth().heightIn(min = 48.dp).clip(Radius.group).clickable(role = Role.Button) { state.removeReview(card); shown = false; index++ }, contentAlignment = Alignment.Center) {
                Text(tr(ui, "Remove from review", "समीक्षा से हटाएँ", "পর্যালোচনা থেকে সরান", "ସମୀକ୍ଷାରୁ ହଟାନ୍ତୁ"), fontSize = 15.sp, color = Brand.Secondary)
            }
        }
    }
}
