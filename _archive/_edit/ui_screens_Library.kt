package com.bhagavatam.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bhagavatam.app.data.SampleData
import com.bhagavatam.app.data.localDigits
import com.bhagavatam.app.state.AppState
import com.bhagavatam.app.ui.components.Dot
import com.bhagavatam.app.ui.components.GroupCard
import com.bhagavatam.app.ui.components.LargeTitle
import com.bhagavatam.app.ui.components.NavBar
import com.bhagavatam.app.ui.components.RowDivider
import com.bhagavatam.app.ui.components.ShlokaText
import com.bhagavatam.app.ui.components.VSpace
import com.bhagavatam.app.ui.theme.Brand
import com.bhagavatam.app.ui.theme.Literata
import com.bhagavatam.app.ui.theme.TiroSanskrit

/** Room left at the bottom of scrolling screens for the mini player and tab bar. */
val BottomRoom = 160.dp

private fun skColor(s: Int) = if (s == 0) Brand.Gold else Brand.Skandha[s - 1]

@Composable
fun HomeScreen(state: AppState, onResume: () -> Unit, onSearch: () -> Unit) {
    val s = state.strings
    val ui = state.uiLang
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        LargeTitle("Bhagavatam", s.homeGreeting)
        Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            // Continue reading
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Brand.Card).clickable(onClick = onResume).padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Dot(skColor(state.lastSkandha))
                    Text(
                        localDigits("${s.skandha} ${state.lastSkandha} · ${s.adhyaya} ${state.lastAdhyaya}", ui),
                        Modifier.weight(1f), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Brand.Secondary,
                    )
                    Box(
                        Modifier.size(48.dp).clip(CircleShape).background(Brand.KesariTint).clickable {
                            state.playVerses(SampleData.versesFor(state.lastSkandha, state.lastAdhyaya), (state.lastVerse - 1).coerceAtLeast(0))
                        },
                        contentAlignment = Alignment.Center,
                    ) { Icon(Icons.Rounded.PlayArrow, s.listen, tint = Brand.Kesari) }
                }
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(SampleData.adhyayaTitle(state.lastSkandha, state.lastAdhyaya, state.titleLang, s), fontFamily = readingFont(state.titleLang), fontSize = 22.sp, fontWeight = FontWeight.Medium)
                    Text(localDigits("${s.stoppedAt} ${state.lastVerse}", ui), fontSize = 14.sp, color = Brand.Secondary)
                }
            }
            // Shloka of the day
            val v = SampleData.shlokaOfTheDay
            Column(
                Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row {
                    Text(s.shlokaOfDay, Modifier.weight(1f), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Brand.Gold)
                    Text(localDigits(v.ref, ui), fontSize = 13.sp, color = Brand.Secondary)
                }
                if (state.showSanskrit) ShlokaText(v, state.script, Brand.Sindoor, 18f)
                val tr = if (state.readLang == com.bhagavatam.app.data.Lang.SA) state.alongsideLayers().first() else state.readLang
                if (v.hasText(tr)) Text(v.translation(tr), fontFamily = readingFont(tr), fontSize = 15.sp, lineHeight = 22.sp)
                Row(
                    Modifier.clip(CircleShape).background(Brand.KesariTint).clickable { state.playVerses(listOf(v)) }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(Icons.Rounded.PlayArrow, null, tint = Brand.Kesari, modifier = Modifier.size(18.dp))
                    Text(s.listen, color = Brand.Kesari, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                }
            }
            // Quick jump
            Row(
                Modifier.fillMaxWidth().height(48.dp).clip(RoundedCornerShape(12.dp)).background(Brand.Fill).clickable(onClick = onSearch)
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(Icons.Rounded.Search, null, tint = Brand.Secondary)
                Text(s.goToHint, color = Brand.Secondary, fontSize = 15.sp)
            }
        }
        VSpace(160)
    }
}

@Composable
fun GranthScreen(state: AppState, onOpen: (Int) -> Unit) {
    val s = state.strings
    val ui = state.uiLang
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = BottomRoom),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item(span = { GridItemSpan(2) }) { LargeTitle(s.tabGranth, s.granthSub, horizontalPadding = 4.dp) }
        item(span = { GridItemSpan(2) }) {
            val m = SampleData.skandha(0)
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Brand.Card).clickable { onOpen(0) }.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xFFF6EEDD)), contentAlignment = Alignment.Center) {
                    Text("मा", fontFamily = TiroSanskrit, fontSize = 18.sp, color = Brand.Gold)
                }
                Column(Modifier.weight(1f)) {
                    Text(s.mahatmya, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    Text(localDigits("${m.title(state.titleLang)} · ${m.adhyayaCount} ${s.adhyayas}", ui), fontSize = 13.sp, color = Brand.Secondary)
                }
                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = Brand.Chevron)
            }
        }
        items(SampleData.skandhas.filter { it.num > 0 }) { sk ->
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Brand.Card).clickable { onOpen(sk.num) }
                    .padding(start = 14.dp, end = 14.dp, top = 14.dp, bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(localDigits(sk.num.toString(), ui), Modifier.weight(1f), fontFamily = Literata, fontSize = 28.sp, fontWeight = FontWeight.Medium, color = Brand.Ink)
                    Text(localDigits("${sk.adhyayaCount} ${s.adhyayas}", ui), fontSize = 12.sp, color = Brand.Secondary)
                }
                Text(sk.nameSa, fontFamily = TiroSanskrit, fontSize = 15.sp)
                val readCount = state.finished.count { it.startsWith("${sk.num}.") }
                Text(localDigits("$readCount / ${sk.adhyayaCount}", ui), fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Brand.Secondary)
                Box(Modifier.fillMaxWidth().height(3.dp).clip(CircleShape).background(Color(0xFFEFEAE1))) {
                    if (readCount > 0) Box(Modifier.fillMaxWidth(readCount.toFloat() / sk.adhyayaCount).height(3.dp).background(skColor(sk.num)))
                }
            }
        }
    }
}

@Composable
fun AdhyayasScreen(state: AppState, skandha: Int, onBack: () -> Unit, onOpen: (Int) -> Unit) {
    val s = state.strings
    val ui = state.uiLang
    val sk = SampleData.skandha(skandha)
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = BottomRoom)) {
        item { NavBar(s.tabGranth, onBack) }
        item {
            Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Dot(skColor(skandha), 10)
                    Text(localDigits("${sk.adhyayaCount} ${s.adhyayas}", ui), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Brand.Secondary)
                }
                Text(if (skandha == 0) s.mahatmya else localDigits("${s.skandha} $skandha", ui), style = MaterialTheme.typography.displaySmall)
                Text(sk.nameSa, fontFamily = TiroSanskrit, fontSize = 18.sp, color = Brand.Sindoor)
                Text(sk.title(state.titleLang), fontSize = 15.sp, color = Brand.Secondary)
            }
        }
        items((1..sk.adhyayaCount).toList(), key = { it }) { a ->
            val first = a == 1
            val last = a == sk.adhyayaCount
            val shape = RoundedCornerShape(topStart = if (first) 18.dp else 0.dp, topEnd = if (first) 18.dp else 0.dp,
                bottomStart = if (last) 18.dp else 0.dp, bottomEnd = if (last) 18.dp else 0.dp)
            val done = state.isFinished(skandha, a)
            val reading = skandha == state.lastSkandha && a == state.lastAdhyaya && !done
            val count = SampleData.verseCount(skandha, a)
            Column(Modifier.padding(horizontal = 16.dp).fillMaxWidth().clip(shape).background(Brand.Card)) {
                if (!first) RowDivider()
                Row(
                    Modifier.fillMaxWidth().clickable { onOpen(a) }.heightIn(min = 56.dp).padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(localDigits(a.toString(), ui), Modifier.width(28.dp), fontFamily = Literata, fontSize = 17.sp, color = Brand.Secondary)
                    Column(Modifier.weight(1f)) {
                        Text(SampleData.adhyayaTitle(skandha, a, state.titleLang, s), fontSize = 16.sp, lineHeight = 21.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        if (count > 0) Text(localDigits("$count ${s.shlokas}", ui), fontSize = 12.sp, color = Brand.Secondary)
                    }
                    when {
                        done -> Icon(Icons.Rounded.Check, s.finished, tint = Brand.Green)
                        reading -> Text(s.readingNow, Modifier.clip(RoundedCornerShape(8.dp)).background(Color(0xFFFBEFE3)).padding(horizontal = 8.dp, vertical = 4.dp),
                            fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Brand.Kesari)
                    }
                    Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = Brand.Chevron)
                }
            }
        }
    }
}
