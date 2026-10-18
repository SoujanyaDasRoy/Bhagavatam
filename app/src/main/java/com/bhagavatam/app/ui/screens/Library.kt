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
import com.bhagavatam.app.data.stringsFor
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

/** Soft dark halo behind white text laid over a painting. */
private val cardShadow = androidx.compose.ui.text.TextStyle(shadow = androidx.compose.ui.graphics.Shadow(Color(0xB3000000), androidx.compose.ui.geometry.Offset(0f, 2f), 6f))

@Composable
fun SkandhaCard(n: Int, state: AppState, modifier: Modifier, onClick: () -> Unit) {
    val current = n == state.lastSkandha && state.hasRead
    val s = state.strings
    val sk = SampleData.skandha(n)
    val hue = skColor(n)
    val read = state.finished.count { it.startsWith("$n.") }
    val frac by animateFloatAsState(read.toFloat() / sk.adhyayaCount, tween(Motion.screen), label = "skandhaProgress")
    val cardAccessibility = if (n == 0) s.mahatmya else "${s.skandha} $n"
    // Every card has the same three lines: a big mark (the Skandha number, or Om for the Mahatmya), its name, its chapters.
    val numeralLabel = if (n == 0) "ॐ" else localDigits(n.toString(), state.uiLang)
    val numeralFont = when (state.uiLang) {
        Lang.BN -> NotoSerifBengali
        Lang.HI -> NotoDevanagari
        else -> EnglishReading
    }
    val numeralSize = 36.sp

    Box(modifier.tappable(Radius.card, cardAccessibility, onClick = onClick).then(if (current) Modifier.border(2.dp, Color(0xFFF5C97A), Radius.card) else Modifier)) {
        ArtBackdrop(skArt(n), skGradient(hue), Modifier.matchParentSize())
        // a darker lower half so the name and the count are readable on any picture or colour
        Box(Modifier.matchParentSize().background(androidx.compose.ui.graphics.Brush.verticalGradient(0.3f to Color.Transparent, 0.6f to Color(0x66000000), 1f to Color(0xB3000000))))
        Column(Modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Column {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(numeralLabel, fontFamily = numeralFont, fontSize = numeralSize, fontWeight = FontWeight.SemiBold, color = Color.White, style = cardShadow)
                    if (current) Text(s.readingNow, Modifier.clip(CircleShape).background(Color(0xFFF5C97A)).padding(horizontal = 10.dp, vertical = 3.dp), fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2A1A08))
                }
                val subTitle = if (state.contentLang == Lang.EN) sk.name(state.uiLang) else sk.title(state.contentLang)
                val subFont = if (state.contentLang == Lang.EN) {
                    when (state.uiLang) {
                        Lang.BN -> NotoSerifBengali
                        Lang.HI -> NotoDevanagari
                        else -> NotoDevanagari
                    }
                } else readingFont(state.contentLang)
                Text(subTitle, fontFamily = subFont, fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight.SemiBold, color = Color.White, style = cardShadow, maxLines = 1, overflow = TextOverflow.Ellipsis)
                // what the Skandha is about, in the reading language: this is the line that tells the cards apart
                val about = sk.title(state.contentLang)
                if (about.isNotEmpty() && about != subTitle) Text(about, fontFamily = readingFont(state.contentLang), fontSize = 15.sp, lineHeight = 20.sp, color = Color(0xFFFFF4DC), style = cardShadow, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                // nothing read yet: just the size of the Skandha; the bar appears once there is something to show
                Text(localDigits(if (read == 0) "${sk.adhyayaCount} ${s.adhyayas}" else "$read / ${sk.adhyayaCount} ${s.adhyayas}", state.uiLang), fontSize = 15.sp, fontWeight = FontWeight.Medium, color = Color.White, style = cardShadow, maxLines = 1)
                if (read > 0) Box(Modifier.fillMaxWidth().height(4.dp).clip(CircleShape).background(Color(0x40FFFFFF))) {
                    Box(Modifier.fillMaxWidth(frac).height(4.dp).background(Color(0xFFFFE2A3)))
                }
            }
        }
    }
}

@Composable
fun StoryCard(e: Episode, state: AppState, onOpen: (Int, Int) -> Unit) {
    val hue = skColor(e.s)
    val refLabel = if (e.s == 0) "${stringsFor(state.titleLang).mahatmya}.${e.a}" else "${e.s}.${e.a}"
    // Names of stories are part of the book: they follow the reading language (Odia has none, so the app language is used).
    val storyLang = state.contentLang
    Box(Modifier.width(200.dp).height(128.dp).tappable(Radius.card, e.title(storyLang)) { onOpen(e.s, e.a) }) {
        ArtBackdrop(if (com.bhagavatam.app.ui.components.artRes(chArt(e.s, e.a)) != 0) chArt(e.s, e.a) else skArt(e.s), skGradient(hue), Modifier.matchParentSize(), petals = 12)
        Column(Modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Text(localDigits(refLabel, storyLang), Modifier.clip(CircleShape).background(Color(0x99000000)).padding(horizontal = 9.dp, vertical = 3.dp),
                fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
            Text(e.title(storyLang), fontFamily = readingFont(storyLang), fontSize = 19.sp, lineHeight = 30.sp, fontWeight = FontWeight.SemiBold, color = Color.White, style = cardShadow, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}

/** A short shortcut: icon over its name, two of them side by side. */
@Composable
private fun QuickTile(icon: Int, label: String, modifier: Modifier, onClick: () -> Unit) {
    Column(
        modifier.tappable(Radius.group, onClick = onClick).background(Brand.Card).border(1.dp, Brand.Separator, Radius.group)
            .heightIn(min = 88.dp).padding(horizontal = 12.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterVertically),
    ) {
        Icon(painterResource(icon), null, tint = Brand.Kesari, modifier = Modifier.size(24.dp))
        Text(label, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Brand.Ink, maxLines = 2, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
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
        Text(label, Modifier.weight(1f), fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = Brand.Ink, maxLines = 2)
        Icon(painterResource(Ic.KeyboardArrowRight), null, tint = Brand.Chevron, modifier = Modifier.size(20.dp))
    }
}

@Composable
fun HomeScreen(
    state: AppState, onResume: () -> Unit, onSearch: () -> Unit,
    onOpenSkandha: (Int) -> Unit = {}, onOpenGranth: () -> Unit = {},
    onOpenChapter: (Int, Int) -> Unit = { _, _ -> }, onSaved: () -> Unit = {}, onReview: () -> Unit = {}, onGlossary: () -> Unit = {},
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
                // the book's name and the greeting are the page's: they follow the reading language
                val ps = stringsFor(state.titleLang)
                val pf = if (state.titleLang == Lang.EN || state.titleLang == Lang.SA) null else readingFont(state.titleLang)
                com.bhagavatam.app.ui.components.FitText(ps.jaiShreeMadhav, size = 26f, minSize = 18f, color = Brand.Ink, fontWeight = FontWeight.Bold, fontFamily = pf)
                com.bhagavatam.app.ui.components.FitText(
                    ps.shrimadBhagavatMahapuran,
                    fontFamily = pf,
                    size = 16f, color = Brand.Gold, fontWeight = FontWeight.Medium,
                )
            }
            // One tap between the light and the dark look; the full choice lives in Settings.
            Box(
                Modifier.size(Touch.min).clip(CircleShape).background(Brand.Card).border(1.dp, Brand.Separator, CircleShape)
                    .clickable(onClickLabel = tr(ui, if (dark) "Switch to light look" else "Switch to dark look", if (dark) "हल्का रूप चुनें" else "गहरा रूप चुनें", if (dark) "হালকা রূপ বেছে নিন" else "গাঢ় রূপ বেছে নিন", if (dark) "ହାଲୁକା ରୂପ ବାଛନ୍ତୁ" else "ଗାଢ଼ ରୂପ ବାଛନ୍ତୁ"), role = Role.Button) {
                        state.setThemeMode(if (dark) ThemeMode.LIGHT else ThemeMode.DARK)
                    },
                contentAlignment = Alignment.Center,
            ) {
                Crossfade(dark, animationSpec = tween(Motion.sheet), label = "themeIcon") { d ->
                    Icon(painterResource(if (d) Ic.Sun else Ic.Bedtime), tr(ui, if (dark) "Switch to light look" else "Switch to dark look", if (dark) "हल्का रूप चुनें" else "गहरा रूप चुनें", if (dark) "হালকা রূপ বেছে নিন" else "গাঢ় রূপ বেছে নিন", if (dark) "ହାଲୁକା ରୂପ ବାଛନ୍ତୁ" else "ଗାଢ଼ ରୂପ ବାଛନ୍ତୁ"), tint = Brand.Kesari, modifier = Modifier.size(22.dp))
                }
            }
        }
        com.bhagavatam.app.ui.components.UpdateBanner(state, Modifier.padding(horizontal = 16.dp).padding(bottom = 12.dp))
        // Rows of cards run edge to edge, so each section carries its own side padding.
        Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
            // Continue reading: tinted with the colour of the Skandha you are in.
            val skHue = skColor(state.lastSkandha)
            val rs = stringsFor(state.titleLang)
            val contRef = if (state.lastSkandha == 0) "${rs.mahatmya} · ${rs.adhyaya} ${state.lastAdhyaya}" else "${rs.skandha} ${state.lastSkandha} · ${rs.adhyaya} ${state.lastAdhyaya}"
            Box(
                Modifier.reveal(0, playIntro).padding(horizontal = 16.dp).fillMaxWidth().tappable(Radius.large, s.resumeReading, onClick = onResume),
            ) {
                ArtBackdrop(chArt(state.lastSkandha, state.lastAdhyaya), skGradient(skHue), Modifier.matchParentSize(), mandala = Color(0x1AFFFFFF))
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(localDigits(contRef, state.titleLang),
                        fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFFFFF4DC))
                    Text(SampleData.adhyayaTitle(state.lastSkandha, state.lastAdhyaya, state.titleLangFor(state.lastSkandha, state.lastAdhyaya), s),
                        fontFamily = readingFont(state.contentLang), fontSize = 24.sp, lineHeight = 38.sp, fontWeight = FontWeight.Medium, color = Color.White,
                        maxLines = 3, overflow = TextOverflow.Ellipsis)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            if (state.hasRead) {
                                Text(s.resumeReading, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFFF4DC))
                                Text(localDigits("${s.stoppedAt} ${state.lastVerse}", ui), fontSize = 15.sp, color = Color(0xFFFFF4DC))
                            } else {
                                Text(tr(ui, "Start with Skandha 1", "स्कन्ध १ से शुरू करें", "স্কন্ধ ১ দিয়ে শুরু করুন", "ସ୍କନ୍ଧ ୧ ରୁ ଆରମ୍ଭ କରନ୍ତୁ"), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFFF4DC))
                                Text(tr(ui, "Begin the first chapter", "पहला अध्याय आरम्भ करें", "প্রথম অধ্যায় শুরু করুন", "ପ୍ରଥମ ଅଧ୍ଯାୟ ଆରମ୍ଭ କରନ୍ତୁ"), fontSize = 15.sp, color = Color(0xFFFFF4DC))
                            }
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
            // Cards that have come due: the way back into what the reader chose to remember.
            val due = state.dueReviewCount()
            if (due > 0) Box(Modifier.padding(horizontal = 16.dp)) {
                QuickAction(Ic.Bookmark, tr(ui, "Review ($due due)", "दोहराएँ ($due देय)", "পর্যালোচনা ($due বাকি)", "ସମୀକ୍ଷା ($due ବାକି)"), Modifier.fillMaxWidth(), onReview)
            }
            // Shloka of the day
            val v = SampleData.shlokaOfTheDay
            if (state.showDaily) Box(Modifier.reveal(5, playIntro).padding(horizontal = 16.dp).fillMaxWidth().clip(Radius.card).background(Brand.Card).border(1.dp, Brand.Separator, Radius.card)) {
                Mandala(Brand.Gold.copy(alpha = 0.14f), Modifier.matchParentSize())
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(Modifier.fillMaxWidth()) {
                        Text(s.shlokaOfDay, Modifier.weight(1f), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Brand.Gold)
                        Text(localDigits(v.ref, ui), fontSize = 16.sp, color = Brand.Secondary)
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
            // Shortcuts that are not already a tab
            Row(Modifier.reveal(1, playIntro).padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                QuickTile(Ic.Bookmark, tr(ui, "Saved", "सहेजे गए", "সংরক্ষিত", "ସେଭ୍ ହୋଇଥିବା"), Modifier.weight(1f), onSaved)
                QuickTile(Ic.Scroll, s.thematicLilaIndex, Modifier.weight(1f), onGlossary)
            }
            // The last chapters opened, other than the one the Resume card already offers: plain rows, no box.
            val others = state.recentChapters.mapNotNull { k -> k.split(".").mapNotNull { it.toIntOrNull() }.takeIf { it.size == 2 } }.filter { !(it[0] == state.lastSkandha && it[1] == state.lastAdhyaya) }.take(3)
            if (others.isNotEmpty()) Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                SectionHeading(tr(ui, "Recently read", "हाल में पढ़े", "সম্প্রতি পড়া", "ଅଧୁନା ପଢ଼ା"), Modifier.padding(horizontal = 20.dp))
                others.forEach { (sk, ad) ->
                    Row(Modifier.fillMaxWidth().clickable(role = Role.Button) { onOpenChapter(sk, ad) }.heightIn(min = 52.dp).padding(horizontal = 20.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(localDigits(if (sk == 0) "M.$ad" else "$sk.$ad", ui), Modifier.width(48.dp), fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Brand.Gold)
                        Text(SampleData.adhyayaTitle(sk, ad, state.titleLangFor(sk, ad), s), Modifier.weight(1f), fontFamily = readingFont(state.titleLangFor(sk, ad)), fontSize = 16.sp, color = Brand.Ink, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Icon(painterResource(Ic.KeyboardArrowRight), null, tint = Brand.Chevron, modifier = Modifier.size(20.dp))
                    }
                }
            }
            // Your progress: one line and a thin bar
            // Shown from the first finished chapter; before that an empty bar says nothing.
            if (done > 0) Column(Modifier.reveal(2, playIntro).padding(horizontal = 20.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                val frac by animateFloatAsState((done.toFloat() / total).coerceIn(0f, 1f), tween(Motion.screen), label = "readingProgress")
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(tr(ui, "Your reading", "आपका पठन", "আপনার পাঠ", "ଆପଣଙ୍କ ପଠନ"), Modifier.weight(1f), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Brand.Ink)
                    Text(localDigits(tr(ui, "$done of $total chapters read", "$done / $total अध्याय पढ़े", "$done / $total অধ্যায় পড়া", "$total ମଧ୍ଯରୁ $done ଅଧ୍ଯାୟ ପଢ଼ା"), ui), fontSize = 15.sp, color = Brand.Secondary)
                }
                Box(Modifier.fillMaxWidth().height(6.dp).clip(CircleShape).background(Brand.Fill)) {
                    if (frac > 0f) Box(Modifier.fillMaxWidth(frac.coerceAtLeast(0.02f)).height(6.dp).clip(CircleShape).background(Brand.Kesari))
                }
            }
            // Well-loved stories
            Column(Modifier.reveal(3, playIntro), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                SectionHeading(tr(ui, "Well-loved stories", "प्रिय कथाएँ", "প্রিয় কথা", "ପ୍ରିୟ କଥା"), Modifier.padding(horizontal = 20.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(horizontal = 16.dp)) {
                    items(Episodes.all, key = { "${it.s}.${it.a}" }) { StoryCard(it, state, onOpenChapter) }
                }
            }
            // Skandhas
            Column(Modifier.reveal(4, playIntro), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                SectionHeading(s.tabGranth, Modifier.padding(start = 20.dp, end = 12.dp), action = s.all, onAction = onOpenGranth)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(horizontal = 16.dp)) {
                    items((state.lastSkandha..minOf(12, state.lastSkandha + 2)).toList(), key = { it }) { n -> SkandhaCard(n, state, Modifier.width(184.dp).height((184 * LocalDensity.current.fontScale.coerceIn(1f, 1.8f)).dp)) { onOpenSkandha(n) } }
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
    // the name of the book and the count line follow the reading language, not the app language
    val pageLang = state.titleLang
    val ps = stringsFor(pageLang)
    val headerFont = when (pageLang) {
        Lang.BN -> NotoSerifBengali
        Lang.HI, Lang.SA -> NotoDevanagari
        Lang.OR -> com.bhagavatam.app.ui.theme.NotoSerifOriya
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
                LargeTitle(s.tabGranth, ps.granthSub, horizontalPadding = 4.dp)
            }
        }
        item(span = { GridItemSpan(2) }) {
            val m = SampleData.skandha(0)
            val iconLetter = when (pageLang) {
                Lang.BN -> "মা"
                Lang.HI, Lang.SA -> "मा"
                Lang.OR -> "ମା"
                else -> "M"
            }
            val mCurrent = state.lastSkandha == 0 && state.hasRead
            Box(Modifier.fillMaxWidth().tappable(Radius.card) { onOpen(0) }.then(if (mCurrent) Modifier.border(2.dp, Color(0xFFF5C97A), Radius.card) else Modifier)) {
                ArtBackdrop(skArt(0), listOf(Color(0xFF5A3C06), Color(0xFF8A5C0F)), Modifier.matchParentSize(), petals = 12, mandala = Color(0x30FFE9B0))
                Box(Modifier.matchParentSize().background(androidx.compose.ui.graphics.Brush.horizontalGradient(0f to Color(0xB3000000), 1f to Color(0x33000000))))
                Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Box(Modifier.size(48.dp).clip(CircleShape).background(Color(0x33FFFFFF)), contentAlignment = Alignment.Center) {
                        Text(iconLetter, fontFamily = headerFont, fontSize = 20.sp, color = Color.White)
                    }
                    Column(Modifier.weight(1f)) {
                        Text(ps.mahatmya, fontFamily = headerFont, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = Color.White, style = cardShadow)
                        Text(localDigits("${m.title(state.contentLang)} · ${m.adhyayaCount} ${ps.adhyayas}", pageLang), fontFamily = readingFont(state.titleLang), fontSize = 15.sp, color = Color(0xFFFFF1D0), style = cardShadow)
                        if (mCurrent) Text(s.readingNow, Modifier.padding(top = 4.dp).clip(CircleShape).background(Color(0xFFF5C97A)).padding(horizontal = 10.dp, vertical = 2.dp), fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2A1A08))
                    }
                    Icon(painterResource(Ic.KeyboardArrowRight), null, tint = Color.White)
                }
            }
        }
        items(SampleData.skandhas.filter { it.num > 0 }, key = { it.num }) { sk ->
            SkandhaCard(sk.num, state, Modifier.fillMaxWidth().height((156 * LocalDensity.current.fontScale.coerceIn(1f, 1.8f)).dp)) { onOpen(sk.num) }
        }
    }
}

@Composable
fun AdhyayasScreen(state: AppState, skandha: Int, onBack: () -> Unit, onOpen: (Int) -> Unit) {
    val s = state.strings
    val ui = state.uiLang
    val sk = SampleData.skandha(skandha)
    val bs = stringsFor(state.titleLang)
    val headerTitle = if (skandha == 0) bs.mahatmya else localDigits("${bs.skandha} $skandha", state.titleLang)
    val headerFont = when (ui) {
        Lang.BN -> NotoSerifBengali
        Lang.HI -> NotoDevanagari
        else -> EnglishReading
    }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = BottomRoom)) {
        item { NavBar(s.tabGranth, onBack) }
        item {
            Box(Modifier.padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 16.dp).fillMaxWidth().clip(Radius.large)) {
                ArtBackdrop(skArt(skandha), if (skandha == 0) listOf(Color(0xFF5A3C06), Color(0xFF8A5C0F)) else skGradient(skColor(skandha)), Modifier.matchParentSize(), mandala = Color(0x33FFFFFF))
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    val (readN, ofN) = com.bhagavatam.app.data.skandhaProgress(state.finished.toSet(), sk.adhyayaCount, skandha)
                    Text(headerTitle, fontFamily = headerFont, fontSize = 34.sp, fontWeight = FontWeight.Bold, color = Color.White, style = cardShadow)
                    if (skandha > 0) {
                        val skName = when (state.contentLang) {
                            Lang.BN -> sk.nameBn
                            Lang.HI -> sk.nameSa
                            else -> sk.nameSa
                        }
                        val skFont = when (state.contentLang) {
                            Lang.BN -> NotoSerifBengali
                            Lang.HI, Lang.SA -> NotoDevanagari
                            else -> NotoDevanagari
                        }
                        Text(skName, fontFamily = skFont, fontSize = 19.sp, color = Color.White, style = cardShadow)
                    }
                    Text(sk.title(state.contentLang), fontFamily = readingFont(state.contentLang), fontSize = 16.sp, color = Color(0xFFFFF4DC), style = cardShadow)
                    val counts = "${sk.adhyayaCount} ${s.adhyayas}" + if (readN > 0) " · " + tr(ui, "$readN read", "$readN पढ़े", "$readN পড়া", "$readN ପଢ଼ା") + if (readN >= ofN) "  ✓" else "" else ""
                    Text(localDigits(counts, ui), Modifier.padding(top = 6.dp), fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFFFFF4DC), style = cardShadow)
                }
            }
        }
        if (state.hasRead && state.lastSkandha == skandha && !state.isFinished(skandha, state.lastAdhyaya)) item {
            Row(
                Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp).fillMaxWidth().heightIn(min = 56.dp).clip(Radius.group).background(Color(0xFFF5C97A))
                    .clickable(role = Role.Button) { onOpen(state.lastAdhyaya) }.padding(horizontal = 18.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(painterResource(Ic.PlayArrow), null, tint = Color(0xFF2A1A08), modifier = Modifier.size(22.dp))
                Text(localDigits(tr(ui, "Continue chapter ${state.lastAdhyaya}", "अध्याय ${state.lastAdhyaya} जारी रखें", "অধ্যায় ${state.lastAdhyaya} চালিয়ে যান", "ଅଧ୍ଯାୟ ${state.lastAdhyaya} ଜାରି ରଖନ୍ତୁ"), ui), Modifier.weight(1f), fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2A1A08))
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
            val numFont = when (ui) {
                Lang.BN -> NotoSerifBengali
                Lang.HI -> NotoDevanagari
                else -> EnglishReading
            }
            Column(Modifier.padding(horizontal = 16.dp).fillMaxWidth().clip(shape).background(Brand.Card)) {
                if (!first) RowDivider()
                Row(
                    Modifier.fillMaxWidth().clickable(role = Role.Button) { onOpen(a) }.heightIn(min = 56.dp).padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(localDigits(a.toString(), ui), Modifier.width(28.dp), fontFamily = numFont, fontSize = 17.sp, color = Brand.Secondary)
                    Column(Modifier.weight(1f)) {
                        Text(SampleData.adhyayaTitle(skandha, a, state.titleLangFor(skandha, a), s), fontFamily = readingFont(state.titleLangFor(skandha, a)), fontSize = 16.sp, lineHeight = 26.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        if (count > 0) Text(localDigits("$count ${if (count == 1 && ui == Lang.EN) "shloka" else s.shlokas.lowercase()}", ui), fontSize = 15.sp, color = Brand.Secondary)
                    }
                    when {
                        done -> Icon(painterResource(Ic.Check), s.finished, tint = Brand.Green)
                        reading -> Text(s.readingNow, Modifier.clip(RoundedCornerShape(8.dp)).background(Brand.KesariTint).padding(horizontal = 8.dp, vertical = 4.dp),
                            fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Brand.Kesari)
                    }
                    Icon(painterResource(Ic.KeyboardArrowRight), null, tint = Brand.Chevron)
                }
            }
        }
    }
}
