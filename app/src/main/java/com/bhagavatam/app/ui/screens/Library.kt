package com.bhagavatam.app.ui.screens

import androidx.compose.ui.res.painterResource
import com.bhagavatam.app.ui.components.Ic
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.semantics.Role
import com.bhagavatam.app.ui.components.AppTile
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.getValue
import com.bhagavatam.app.state.ThemeMode
import com.bhagavatam.app.ui.components.reveal
import com.bhagavatam.app.ui.components.tappable
import com.bhagavatam.app.ui.theme.Motion
import com.bhagavatam.app.ui.theme.NotoSerifBengali
import com.bhagavatam.app.ui.theme.Touch
import androidx.compose.ui.platform.LocalDensity
import com.bhagavatam.app.ui.components.SectionHeading
import com.bhagavatam.app.ui.theme.Radius
import com.bhagavatam.app.ui.components.Dot
import com.bhagavatam.app.ui.components.GroupCard
import com.bhagavatam.app.ui.components.LargeTitle
import com.bhagavatam.app.ui.components.NavBar
import com.bhagavatam.app.ui.components.RowDivider
import com.bhagavatam.app.ui.components.ShlokaText
import com.bhagavatam.app.ui.components.VSpace
import com.bhagavatam.app.ui.theme.Brand
import com.bhagavatam.app.ui.theme.Jakarta
import com.bhagavatam.app.ui.theme.LocalAppColors
import com.bhagavatam.app.ui.theme.EnglishReading
import com.bhagavatam.app.ui.theme.NotoDevanagari
import com.bhagavatam.app.data.Episode
import com.bhagavatam.app.data.Episodes
import com.bhagavatam.app.data.Lang
import com.bhagavatam.app.data.tr
import androidx.compose.runtime.remember
import com.bhagavatam.app.ui.components.ArtBackdrop
import com.bhagavatam.app.ui.components.Mandala
import com.bhagavatam.app.ui.components.skArt
import com.bhagavatam.app.ui.components.chArt
import com.bhagavatam.app.ui.components.skGradient

/** Room left at the bottom of scrolling screens for the mini player and tab bar. */
val BottomRoom = 160.dp


@Composable
private fun skColor(s: Int) = if (s == 0) Brand.Gold else Brand.Skandha[s - 1]

/** Set once the Home entrance has played, so it runs once per launch and not on every visit to the tab. */
private var homeIntroPlayed = false

@Composable
fun SkandhaCard(n: Int, state: AppState, modifier: Modifier, onClick: () -> Unit) {
    val s = state.strings
    val sk = SampleData.skandha(n)
    val hue = skColor(n)
    val read = state.finished.count { it.startsWith("$n.") }
    val frac by animateFloatAsState(read.toFloat() / sk.adhyayaCount, tween(Motion.screen), label = "skandhaProgress")
    val cardAccessibility = if (n == 0) s.mahatmya else "${s.skandha} $n"
    val numeralLabel = if (n == 0) s.mahatmya else localDigits(n.toString(), state.uiLang)
    val numeralFont = if (n == 0) (if (state.uiLang == Lang.BN) NotoSerifBengali else if (state.uiLang == Lang.HI) NotoDevanagari else EnglishReading) else EnglishReading
    val numeralSize = if (n == 0) 22.sp else 36.sp

    Box(modifier.tappable(Radius.card, cardAccessibility, onClick = onClick)) {
        ArtBackdrop(skArt(n), skGradient(hue), Modifier.matchParentSize())
        Column(Modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text(numeralLabel, fontFamily = numeralFont, fontSize = numeralSize, fontWeight = FontWeight.SemiBold, color = Color.White)
                Text(sk.nameSa, fontFamily = NotoDevanagari, fontSize = 14.sp, lineHeight = 20.sp, color = Color(0xFFFFF4DC), maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(localDigits("$read / ${sk.adhyayaCount} ${s.adhyayas}", state.uiLang), fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Color(0xFFFFF4DC))
                Box(Modifier.fillMaxWidth().height(4.dp).clip(CircleShape).background(Color(0x40FFFFFF))) {
                    if (frac > 0f) Box(Modifier.fillMaxWidth(frac).height(4.dp).background(Color(0xFFFFE2A3)))
                }
            }
        }
    }
}

@Composable
fun StoryCard(e: Episode, state: AppState, onOpen: (Int, Int) -> Unit) {
    val hue = skColor(e.s)
    val refLabel = if (e.s == 0) "${state.strings.mahatmya}.${e.a}" else "${e.s}.${e.a}"
    Box(Modifier.width(200.dp).height(128.dp).tappable(Radius.card, e.title(state.uiLang)) { onOpen(e.s, e.a) }) {
        ArtBackdrop(chArt(e.s, e.a), skGradient(hue), Modifier.matchParentSize(), petals = 12)
        Column(Modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Text(localDigits(refLabel, state.uiLang), Modifier.clip(CircleShape).background(Color(0x33FFFFFF)).padding(horizontal = 9.dp, vertical = 3.dp),
                fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
            Text(e.title(state.uiLang), fontFamily = readingFont(state.uiLang), fontSize = 19.sp, lineHeight = 25.sp, fontWeight = FontWeight.SemiBold, color = Color.White, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun QuickAction(icon: Int, label: String, modifier: Modifier, onClick: () -> Unit) {
    Row(
        modifier.tappable(Radius.group, onClick = onClick).background(Brand.Card).border(1.dp, Brand.Separator, Radius.group)
            .heightIn(min = 56.dp).padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(painterResource(icon), null, tint = Brand.Kesari, modifier = Modifier.size(22.dp))
        Text(label, Modifier.weight(1f), fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Brand.Ink, maxLines = 2)
        Icon(painterResource(Ic.KeyboardArrowRight), null, tint = Brand.Chevron, modifier = Modifier.size(20.dp))
    }
}

@Composable
fun HomeScreen(
    state: AppState, onResume: () -> Unit, onSearch: () -> Unit,
    onOpenSkandha: (Int) -> Unit = {}, onOpenGranth: () -> Unit = {},
    onOpenChapter: (Int, Int) -> Unit = { _, _ -> }, onSaved: () -> Unit = {}, onGlossary: () -> Unit = {},
) {
    val s = state.strings
    val ui = state.uiLang
    val playIntro = remember { !homeIntroPlayed }
    androidx.compose.runtime.LaunchedEffect(Unit) { homeIntroPlayed = true }
    val dark = LocalAppColors.current.isDark
    val total = remember { SampleData.skandhas.sumOf { it.adhyayaCount } }
    val done = state.finished.size
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Row(Modifier.statusBarsPadding().padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            AppTile(size = 46, fontSize = 20)
            Column(Modifier.weight(1f)) {
                Text(s.jaiShreeMadhav, fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Brand.Ink)
                Text(
                    s.shrimadBhagavatMahapuran,
                    fontFamily = when (ui) { Lang.HI -> NotoDevanagari; Lang.BN -> NotoSerifBengali; else -> null },
                    fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Brand.Gold,
                )
            }
            // One tap between the light and the dark look; the full choice lives in Settings.
            Box(
                Modifier.size(Touch.min).clip(CircleShape).background(Brand.Card).border(1.dp, Brand.Separator, CircleShape)
                    .clickable(onClickLabel = tr(ui, if (dark) "Switch to light look" else "Switch to dark look", if (dark) "हल्का रूप चुनें" else "गहरा रूप चुनें", if (dark) "হালকা রূপ বেছে নিন" else "গাঢ় রূপ বেছে নিন"), role = Role.Button) {
                        state.setThemeMode(if (dark) ThemeMode.LIGHT else ThemeMode.DARK)
                    },
                contentAlignment = Alignment.Center,
            ) {
                Crossfade(dark, animationSpec = tween(Motion.sheet), label = "themeIcon") { d ->
                    Icon(painterResource(if (d) Ic.Sun else Ic.Bedtime), null, tint = Brand.Kesari, modifier = Modifier.size(22.dp))
                }
            }
        }
        // Rows of cards run edge to edge, so each section carries its own side padding.
        Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
            // Continue reading: tinted with the colour of the Skandha you are in.
            val skHue = skColor(state.lastSkandha)
            val contRef = if (state.lastSkandha == 0) "${s.mahatmya} · ${s.adhyaya} ${state.lastAdhyaya}" else "${s.skandha} ${state.lastSkandha} · ${s.adhyaya} ${state.lastAdhyaya}"
            Box(
                Modifier.reveal(0, playIntro).padding(horizontal = 16.dp).fillMaxWidth().tappable(Radius.large, s.resumeReading, onClick = onResume),
            ) {
                ArtBackdrop(chArt(state.lastSkandha, state.lastAdhyaya), skGradient(skHue), Modifier.matchParentSize(), mandala = Color(0x26FFFFFF))
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(localDigits(contRef, ui),
                        fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFFFFF4DC))
                    Text(SampleData.adhyayaTitle(state.lastSkandha, state.lastAdhyaya, state.titleLang, s),
                        fontFamily = readingFont(state.titleLang), fontSize = 24.sp, lineHeight = 32.sp, fontWeight = FontWeight.Medium, color = Color.White,
                        maxLines = 3, overflow = TextOverflow.Ellipsis)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(s.resumeReading, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFFF4DC))
                            Text(localDigits("${s.stoppedAt} ${state.lastVerse}", ui), fontSize = 13.sp, color = Color(0xFFFFF4DC))
                        }
                        Box(
                            Modifier.size(56.dp).clip(CircleShape).background(Color(0xFFF5C97A))
                                .clickable(onClickLabel = s.listen, role = Role.Button) {
                                    state.playVerses(SampleData.versesFor(state.lastSkandha, state.lastAdhyaya), (state.lastVerse - 1).coerceAtLeast(0))
                                },
                            contentAlignment = Alignment.Center,
                        ) { Icon(painterResource(Ic.PlayArrow), s.listen, tint = Color(0xFF2A1A08), modifier = Modifier.size(28.dp)) }
                    }
                }
            }
            // Shortcuts that are not already a tab
            val savedLabel = tr(ui, "Saved", "सहेजे गए", "সংরক্ষিত")
            if (LocalDensity.current.fontScale > 1.3f) {
                // At large text sizes two side-by-side labels break mid-word, so they stack.
                Column(Modifier.reveal(1, playIntro).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    QuickAction(Ic.Bookmark, savedLabel, Modifier.fillMaxWidth(), onSaved)
                    QuickAction(Ic.Scroll, s.thematicLilaIndex, Modifier.fillMaxWidth(), onGlossary)
                }
            } else Row(Modifier.reveal(1, playIntro).padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                QuickAction(Ic.Bookmark, savedLabel, Modifier.weight(1f), onSaved)
                QuickAction(Ic.Scroll, s.thematicLilaIndex, Modifier.weight(1f), onGlossary)
            }
            // Your progress
            Column(
                Modifier.reveal(2, playIntro).padding(horizontal = 16.dp).fillMaxWidth().clip(Radius.card).background(Brand.Card).border(1.dp, Brand.Separator, Radius.card).padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                val frac by animateFloatAsState((done.toFloat() / total).coerceIn(0f, 1f), tween(Motion.screen), label = "readingProgress")
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(tr(ui, "Your reading", "आपका पठन", "আপনার পাঠ"), Modifier.weight(1f), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Brand.Ink)
                    Text(localDigits("$done / $total", ui), fontFamily = EnglishReading, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = Brand.Kesari)
                }
                Box(Modifier.fillMaxWidth().height(8.dp).clip(CircleShape).background(Brand.Fill)) {
                    if (frac > 0f) Box(Modifier.fillMaxWidth(frac.coerceAtLeast(0.02f)).height(8.dp).clip(CircleShape).background(Brand.Kesari))
                }
                Text(
                    if (done == 0) tr(ui, "Finish a chapter and it is counted here.", "कोई अध्याय पूरा करें, वह यहाँ गिना जाएगा।", "একটি অধ্যায় শেষ করলে এখানে গোনা হবে।")
                    else tr(ui, "$done chapters finished. ${total - done} to go.", "$done अध्याय पूरे हुए। ${total - done} शेष हैं।", "$done টি অধ্যায় শেষ। ${total - done} টি বাকি।").let { localDigits(it, ui) },
                    fontSize = 13.sp, color = Brand.Secondary,
                )
            }
            // Well-loved stories
            Column(Modifier.reveal(3, playIntro), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                SectionHeading(tr(ui, "Well-loved stories", "प्रिय कथाएँ", "প্রিয় কথা"), Modifier.padding(horizontal = 20.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(horizontal = 16.dp)) {
                    items(Episodes.all, key = { "${it.s}.${it.a}" }) { StoryCard(it, state, onOpenChapter) }
                }
            }
            // Skandhas
            Column(Modifier.reveal(4, playIntro), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                SectionHeading(s.tabGranth, Modifier.padding(start = 20.dp, end = 12.dp), action = s.all, onAction = onOpenGranth)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(horizontal = 16.dp)) {
                    items((0..12).toList(), key = { it }) { n -> SkandhaCard(n, state, Modifier.width(150.dp).height(124.dp)) { onOpenSkandha(n) } }
                }
            }
            // Shloka of the day
            val v = SampleData.shlokaOfTheDay
            if (state.showDaily) Box(Modifier.reveal(5, playIntro).padding(horizontal = 16.dp).fillMaxWidth().clip(Radius.card).background(Brand.Card).border(1.dp, Brand.Separator, Radius.card)) {
                Mandala(Brand.Gold.copy(alpha = 0.14f), Modifier.matchParentSize())
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(Modifier.fillMaxWidth()) {
                        Text(s.shlokaOfDay, Modifier.weight(1f), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Brand.Gold)
                        Text(localDigits(v.ref, ui), fontSize = 14.sp, color = Brand.Secondary)
                    }
                    if (state.showSanskrit) ShlokaText(v, state.script, Brand.Sindoor, 19f, center = true)
                    val trl = if (state.readLang == Lang.SA) state.alongsideLayers().first() else state.readLang
                    if (v.hasText(trl)) Text(v.translation(trl), fontFamily = readingFont(trl), fontSize = 16.sp, lineHeight = 25.sp, color = Brand.Ink)
                    Row(
                        Modifier.heightIn(min = 48.dp).clip(CircleShape).background(Brand.KesariTint).clickable(role = Role.Button) { state.playVerses(listOf(v)) }.padding(horizontal = 18.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(painterResource(Ic.PlayArrow), null, tint = Brand.Kesari, modifier = Modifier.size(20.dp))
                        Text(s.listen, color = Brand.Kesari, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
            }
        }
        VSpace(160)
    }
}

@Composable
fun GranthScreen(state: AppState, onOpen: (Int) -> Unit) {
    val s = state.strings
    val ui = state.uiLang
    val headerTitle = when (ui) {
        Lang.BN -> "॥ শ্রীমদ্ভাগবত মহাপুরাণ ॥"
        Lang.HI -> "॥ श्रीमद्भागवत महापुराण ॥"
        else -> "॥ Shrimad Bhagavat Mahapuran ॥"
    }
    val headerFont = when (ui) {
        Lang.BN -> NotoSerifBengali
        Lang.HI -> NotoDevanagari
        else -> EnglishReading
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = BottomRoom),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item(span = { GridItemSpan(2) }) {
            Column(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalAlignment = Alignment.Start) {
                LargeTitle(s.tabGranth, s.granthSub, horizontalPadding = 4.dp)
                Text(headerTitle, fontFamily = headerFont, fontSize = 20.sp, color = Brand.Sindoor, modifier = Modifier.padding(start = 4.dp, top = 4.dp, bottom = 8.dp))
            }
        }
        item(span = { GridItemSpan(2) }) {
            val m = SampleData.skandha(0)
            val iconLetter = when (ui) {
                Lang.BN -> "মা"
                Lang.HI -> "मा"
                else -> "M"
            }
            Box(Modifier.fillMaxWidth().tappable(Radius.card) { onOpen(0) }) {
                ArtBackdrop(skArt(0), listOf(Color(0xFF5A3C06), Color(0xFF8A5C0F)), Modifier.matchParentSize(), petals = 12, mandala = Color(0x30FFE9B0))
                Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Box(Modifier.size(48.dp).clip(CircleShape).background(Color(0x33FFFFFF)), contentAlignment = Alignment.Center) {
                        Text(iconLetter, fontFamily = headerFont, fontSize = 20.sp, color = Color.White)
                    }
                    Column(Modifier.weight(1f)) {
                        Text(s.mahatmya, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                        Text(localDigits("${m.title(state.titleLang)} · ${m.adhyayaCount} ${s.adhyayas}", ui), fontSize = 13.sp, color = Color(0xFFFFF1D0))
                    }
                    Icon(painterResource(Ic.KeyboardArrowRight), null, tint = Color.White)
                }
            }
        }
        items(SampleData.skandhas.filter { it.num > 0 }, key = { it.num }) { sk ->
            SkandhaCard(sk.num, state, Modifier.fillMaxWidth().height(156.dp)) { onOpen(sk.num) }
        }
    }
}

@Composable
fun AdhyayasScreen(state: AppState, skandha: Int, onBack: () -> Unit, onOpen: (Int) -> Unit) {
    val s = state.strings
    val ui = state.uiLang
    val sk = SampleData.skandha(skandha)
    val headerTitle = if (skandha == 0) s.mahatmya else localDigits("${s.skandha} $skandha", ui)
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = BottomRoom)) {
        item { NavBar(s.tabGranth, onBack) }
        item {
            Box(Modifier.padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 16.dp).fillMaxWidth().clip(Radius.large)) {
                ArtBackdrop(skArt(skandha), if (skandha == 0) listOf(Color(0xFF5A3C06), Color(0xFF8A5C0F)) else skGradient(skColor(skandha)), Modifier.matchParentSize(), mandala = Color(0x33FFFFFF))
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(localDigits("${sk.adhyayaCount} ${s.adhyayas}", ui), fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFFFFF4DC))
                    Text(headerTitle, fontFamily = EnglishReading, fontSize = 34.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Text(sk.nameSa, fontFamily = NotoDevanagari, fontSize = 19.sp, color = Color.White)
                    // Skip the line when it only repeats the Sanskrit name above (Hindi).
                    if (sk.title(state.titleLang) != sk.nameSa) Text(sk.title(state.titleLang), fontSize = 16.sp, color = Color(0xFFFFF4DC))
                }
            }
        }
        items((1..sk.adhyayaCount).toList(), key = { it }) { a ->
            val first = a == 1
            val last = a == sk.adhyayaCount
            val shape = RoundedCornerShape(topStart = if (first) 16.dp else 0.dp, topEnd = if (first) 16.dp else 0.dp,
                bottomStart = if (last) 16.dp else 0.dp, bottomEnd = if (last) 16.dp else 0.dp)
            val done = state.isFinished(skandha, a)
            val reading = skandha == state.lastSkandha && a == state.lastAdhyaya && !done
            val count = SampleData.verseCount(skandha, a)
            Column(Modifier.padding(horizontal = 16.dp).fillMaxWidth().clip(shape).background(Brand.Card)) {
                if (!first) RowDivider()
                Row(
                    Modifier.fillMaxWidth().clickable(role = Role.Button) { onOpen(a) }.heightIn(min = 56.dp).padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(localDigits(a.toString(), ui), Modifier.width(28.dp), fontFamily = EnglishReading, fontSize = 17.sp, color = Brand.Secondary)
                    Column(Modifier.weight(1f)) {
                        Text(SampleData.adhyayaTitle(skandha, a, state.titleLang, s), fontSize = 16.sp, lineHeight = 21.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        if (count > 0) Text(localDigits("$count ${s.shlokas.lowercase()}", ui), fontSize = 13.sp, color = Brand.Secondary)
                    }
                    when {
                        done -> Icon(painterResource(Ic.Check), s.finished, tint = Brand.Green)
                        reading -> Text(s.readingNow, Modifier.clip(RoundedCornerShape(8.dp)).background(Brand.KesariTint).padding(horizontal = 8.dp, vertical = 4.dp),
                            fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Brand.Kesari)
                    }
                    Icon(painterResource(Ic.KeyboardArrowRight), null, tint = Brand.Chevron)
                }
            }
        }
    }
}
