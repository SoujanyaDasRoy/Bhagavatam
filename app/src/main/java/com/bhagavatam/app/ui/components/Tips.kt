package com.bhagavatam.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bhagavatam.app.data.tr
import com.bhagavatam.app.state.AppState
import com.bhagavatam.app.ui.theme.Brand
import com.bhagavatam.app.ui.theme.Radius

/**
 * One short first-use hint with "Got it" and "Don't show tips". Shows the next tip the person has not seen; nothing when
 * tips are off or all were seen. The same switch is in Settings > Reading > Tips (turning it on shows them again).
 */
@Composable
fun TipBanner(state: AppState, modifier: Modifier = Modifier) {
    val id = state.nextTip() ?: return
    val ui = state.uiLang
    val text = when (id) {
        "listen" -> tr(ui, "Tap Listen to hear this chapter read aloud.", "इस अध्याय को सुनने के लिए \"सुनें\" दबाएँ।", "এই অধ্যায় শুনতে \"শুনুন\" চাপুন।", "ଏହି ଅଧ୍ଯାୟକୁ ଉଚ୍ଚସ୍ୱରରେ ଶୁଣିବାକୁ ଶୁଣନ୍ତୁ ଦବାନ୍ତୁ।")
        "holdWord" -> tr(ui, "Press and hold any word to see its meaning.", "किसी भी शब्द को दबाकर रखें, उसका अर्थ दिखेगा।", "যেকোনো শব্দ চেপে ধরে রাখুন, অর্থ দেখা যাবে।", "ଯେକୌଣସି ଶବ୍ଦକୁ ଧରି ରଖିଲେ ତାହାର ଅର୍ଥ ଦେଖିପାରିବେ।")
        else -> tr(ui, "The language button at the top changes what you read and shows the shloka.", "ऊपर का भाषा बटन पढ़ने की भाषा बदलता है और श्लोक दिखाता है।", "উপরের ভাষা বোতাম পড়ার ভাষা বদলায় ও শ্লোক দেখায়।", "ଉପରେ ଥିବା ଭାଷା ବଟନ୍ ଆପଣ କେଉଁ ଭାଷାରେ ପଢ଼ିବେ ତାହା ବଦଳାଏ ଓ ଶ୍ଳୋକ ଦେଖାଏ।")
    }
    Column(
        modifier.fillMaxWidth().clip(Radius.field).background(Brand.Card).border(1.dp, Brand.Separator, Radius.field)
            .padding(start = 14.dp, end = 6.dp, top = 8.dp, bottom = 2.dp),
    ) {
        Text(text, fontSize = 15.sp, lineHeight = 22.sp, color = Brand.Ink)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
            Text(
                tr(ui, "Don't show tips", "सुझाव न दिखाएँ", "টিপস দেখাবেন না", "ଟିପ୍ସ ଦେଖାନ୍ତୁ ନାହିଁ"),
                Modifier.heightIn(min = 48.dp).clickable(role = Role.Button) { state.updateTipsEnabled(false) }.padding(horizontal = 10.dp, vertical = 14.dp),
                fontSize = 13.sp, color = Brand.Secondary,
            )
            Text(
                tr(ui, "Got it", "समझ गया", "বুঝেছি", "ବୁଝିଲି"),
                Modifier.heightIn(min = 48.dp).clickable(role = Role.Button) { state.markTipSeen(id) }.padding(horizontal = 12.dp, vertical = 14.dp),
                fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Brand.Kesari,
            )
        }
    }
}
