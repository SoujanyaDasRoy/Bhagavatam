package com.bhagavatam.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bhagavatam.app.data.Lang
import com.bhagavatam.app.data.SampleData
import com.bhagavatam.app.data.localDigits
import com.bhagavatam.app.state.AppState
import com.bhagavatam.app.ui.components.Segmented
import com.bhagavatam.app.ui.components.ShlokaText
import com.bhagavatam.app.ui.theme.Brand
import com.bhagavatam.app.ui.theme.Jakarta
import com.bhagavatam.app.ui.theme.TiroBangla
import com.bhagavatam.app.ui.theme.TiroHindi
import com.bhagavatam.app.ui.theme.TiroSanskrit

@Composable
fun PlayerScreen(state: AppState, onClose: () -> Unit) {
    val s = state.strings
    val ui = state.uiLang
    val v = state.current
    val langs = listOf(Lang.SA, Lang.HI, Lang.BN, Lang.EN)
    Column(Modifier.fillMaxSize().background(Brand.Paper).statusBarsPadding().navigationBarsPadding()) {
        Box(Modifier.fillMaxWidth().padding(top = 8.dp), contentAlignment = Alignment.Center) {
            Box(Modifier.width(40.dp).height(5.dp).clip(CircleShape).background(Color(0xFFD6CFC2)))
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onClose) { Icon(Icons.Rounded.Close, "Close player", tint = Brand.Secondary) }
            Text(
                localDigits("${v.skandha}.${v.adhyaya}", ui) + " · " + SampleData.adhyayaTitle(v.skandha, v.adhyaya, state.titleLang, s),
                Modifier.weight(1f), textAlign = TextAlign.Center, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Brand.Secondary, maxLines = 1,
            )
            Box(Modifier.size(48.dp))
        }
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 28.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(localDigits("${s.shloka.uppercase()} ${v.num}", ui), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Brand.Gold, letterSpacing = 0.6.sp)
            if (state.audioLang == Lang.SA) {
                ShlokaText(v, state.script, Brand.Sindoor, 21f, center = true)
                Text(v.translation(state.alongsideLayers().first()), fontFamily = readingFont(state.alongsideLayers().first()),
                    fontSize = 15.sp, lineHeight = 22.sp, color = Brand.Secondary, textAlign = TextAlign.Center, maxLines = 3)
            } else {
                Text(v.translation(state.audioLang), fontFamily = readingFont(state.audioLang), fontSize = 20.sp, lineHeight = 32.sp,
                    color = Brand.Ink, textAlign = TextAlign.Center)
            }
            state.audioNote?.let { Text(it, fontSize = 12.sp, color = Brand.Tertiary, textAlign = TextAlign.Center) }
        }
        Column(Modifier.padding(horizontal = 28.dp, vertical = 12.dp)) {
            LinearProgressIndicator(
                progress = { state.progress }, modifier = Modifier.fillMaxWidth().height(5.dp).clip(CircleShape),
                color = Brand.Teal, trackColor = Brand.Separator, strokeCap = StrokeCap.Round, gapSize = 0.dp, drawStopIndicator = {},
            )
        }
        Row(Modifier.fillMaxWidth().padding(bottom = 20.dp), horizontalArrangement = Arrangement.spacedBy(32.dp, Alignment.CenterHorizontally), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = state::previous, modifier = Modifier.size(56.dp)) { Icon(Icons.Rounded.SkipPrevious, "Previous shloka", tint = Brand.Ink, modifier = Modifier.size(34.dp)) }
            Box(Modifier.size(76.dp).clip(CircleShape).background(Brand.Kesari).clickable { state.togglePlay() }, contentAlignment = Alignment.Center) {
                Icon(if (state.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, if (state.isPlaying) "Pause" else "Play",
                    tint = Color.White, modifier = Modifier.size(38.dp))
            }
            IconButton(onClick = state::next, modifier = Modifier.size(56.dp)) { Icon(Icons.Rounded.SkipNext, "Next shloka", tint = Brand.Ink, modifier = Modifier.size(34.dp)) }
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp), horizontalArrangement = Arrangement.SpaceAround) {
            PlayerAction("${state.speed}×", s.speed) { state.cycleSpeed() }
            Column(Modifier.clickable { state.toggleLoop() }.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Rounded.Repeat, null, tint = if (state.loop) Brand.Teal else Brand.Ink)
                Text(s.loop, fontSize = 12.sp, color = if (state.loop) Brand.Teal else Brand.Secondary)
            }
        }
        Box(Modifier.padding(top = 10.dp, bottom = 20.dp)) {
            Segmented(
                listOf("संस्कृत", "हिन्दी", "বাংলা", "English"), langs.indexOf(state.readLang),
                { state.updateReadLang(langs[it]); if (state.isPlaying) { state.togglePlay(); state.togglePlay() } },
                fonts = listOf(TiroSanskrit, TiroHindi, TiroBangla, Jakarta),
            )
        }
    }
}

@Composable
private fun PlayerAction(top: String, label: String, onClick: () -> Unit) {
    Column(Modifier.clickable(onClick = onClick).padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.border(1.dp, Brand.Separator, CircleShape).padding(horizontal = 10.dp, vertical = 2.dp)) {
            Text(top, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Brand.Ink)
        }
        Text(label, fontSize = 12.sp, color = Brand.Secondary)
    }
}
