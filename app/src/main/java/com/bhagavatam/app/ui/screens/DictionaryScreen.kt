package com.bhagavatam.app.ui.screens

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bhagavatam.app.data.DictEntry
import com.bhagavatam.app.data.DictSense
import com.bhagavatam.app.data.Dictionary
import com.bhagavatam.app.data.SavedWord
import com.bhagavatam.app.data.localDigits
import com.bhagavatam.app.data.tr
import com.bhagavatam.app.state.AppState
import com.bhagavatam.app.ui.components.Ic
import com.bhagavatam.app.ui.components.VSpace
import com.bhagavatam.app.ui.theme.EnglishReading
import com.bhagavatam.app.ui.theme.LocalAppColors
import com.bhagavatam.app.ui.theme.NotoDevanagari
import com.bhagavatam.app.ui.theme.NotoSerifBengali
import com.bhagavatam.app.ui.theme.Radius
import com.bhagavatam.app.ui.theme.TiroHindi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

private fun fontForLang(langCode: String): FontFamily = when (langCode.lowercase()) {
    "hi" -> TiroHindi
    "bn" -> NotoSerifBengali
    "or" -> NotoSerifBengali
    "sa" -> NotoDevanagari
    else -> EnglishReading
}

/**
 * Offline dictionary screen with prefix search, language filtering,
 * full word definitions and saved words management.
 */
@Composable
fun DictionaryScreen(
    state: AppState,
    initialWord: String? = null,
    onBack: () -> Unit,
    onOpenCredits: () -> Unit
) {
    val app = LocalAppColors.current
    val ui = state.uiLang
    val ctx = LocalContext.current
    val shape = Radius.card

    var query by remember { mutableStateOf(initialWord ?: "") }
    var selectedLang by remember { mutableStateOf("all") }
    var searchResults by remember { mutableStateOf<List<DictEntry>>(emptyList()) }
    var selectedEntry by remember { mutableStateOf<DictEntry?>(null) }
    var isSearching by remember { mutableStateOf(false) }

    // Debounced search off the main thread (150ms)
    LaunchedEffect(query, selectedLang) {
        val trimmed = query.trim()
        if (trimmed.length < 2) {
            searchResults = emptyList()
            selectedEntry = null
            isSearching = false
            return@LaunchedEffect
        }
        isSearching = true
        delay(150)
        val results = withContext(Dispatchers.IO) {
            val exactLookups = Dictionary.lookup(
                trimmed,
                if (selectedLang == "all") "en" else selectedLang,
                state.preferredMeaningLang(selectedLang)
            )
            val prefixResults = Dictionary.search(
                prefix = trimmed,
                lang = if (selectedLang == "all") null else selectedLang,
                limit = 35
            )
            (exactLookups + prefixResults).distinctBy { it.id }
        }
        searchResults = results
        isSearching = false
        if (results.isNotEmpty() && (selectedEntry == null || results.none { it.id == selectedEntry?.id })) {
            selectedEntry = results.first()
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .background(app.bg)
    ) {
        // Top navigation bar
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                Icon(
                    painter = painterResource(Ic.ArrowBackIos),
                    contentDescription = tr(ui, "Back", "वापस", "ফিরে যান"),
                    tint = app.ink,
                    modifier = Modifier.size(20.dp)
                )
            }
            Text(
                tr(ui, "Dictionary", "शब्दकोश", "অভিধান"),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = app.ink,
                fontFamily = readingFont(ui),
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onOpenCredits, modifier = Modifier.size(48.dp)) {
                Icon(
                    painter = painterResource(Ic.Scroll),
                    contentDescription = tr(ui, "Credits", "आभार", "স্বীকার"),
                    tint = app.inkSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // Search text box
        Box(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
                .clip(shape)
                .background(app.surface)
                .border(1.dp, app.line, shape)
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(Ic.Search),
                    contentDescription = null,
                    tint = app.inkSecondary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(10.dp))
                BasicTextField(
                    value = query,
                    onValueChange = { query = it },
                    singleLine = true,
                    textStyle = TextStyle(
                        color = app.ink,
                        fontSize = 16.sp,
                        fontFamily = readingFont(ui)
                    ),
                    cursorBrush = SolidColor(app.accent),
                    modifier = Modifier.weight(1f),
                    decorationBox = { inner ->
                        if (query.isEmpty()) {
                            Text(
                                tr(ui, "Search word or prefix...", "शब्द या अक्षर खोजें...", "শব্দ বা উপসর্গ খুঁজুন..."),
                                color = app.inkSecondary,
                                fontSize = 16.sp,
                                fontFamily = readingFont(ui)
                            )
                        }
                        inner()
                    }
                )
                if (query.isNotEmpty()) {
                    IconButton(
                        onClick = {
                            query = ""
                            searchResults = emptyList()
                            selectedEntry = null
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            painter = painterResource(Ic.Close),
                            contentDescription = tr(ui, "Clear", "साफ़ करें", "মুছুন"),
                            tint = app.inkSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        // Language filter chips
        Row(
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val langs = listOf(
                "all" to tr(ui, "All", "सभी", "সব"),
                "en" to tr(ui, "English", "अंग्रेज़ी", "ইংরেজি"),
                "hi" to tr(ui, "Hindi", "हिन्दी", "হিন্দি"),
                "bn" to tr(ui, "Bengali", "বাংলা", "বাংলা"),
                "or" to tr(ui, "Odia (Draft)", "ओड़िया (प्रारूप)", "ওড়িয়া (খসড়া)")
            )
            langs.forEach { (code, label) ->
                val selected = selectedLang == code
                FilterChip(
                    selected = selected,
                    onClick = { selectedLang = code },
                    label = { Text(label, fontSize = 13.sp, fontFamily = readingFont(ui)) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = app.accent.copy(alpha = 0.15f),
                        selectedLabelColor = app.accent,
                        containerColor = app.surface,
                        labelColor = app.inkSecondary
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = selected,
                        borderColor = if (selected) app.accent else app.line
                    )
                )
            }
        }

        // Body content: Search results & details or Saved words
        LazyColumn(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (query.trim().length >= 2) {
                // Active query mode
                if (searchResults.isEmpty() && !isSearching) {
                    item {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .clip(shape)
                                .background(app.surface)
                                .border(1.dp, app.line, shape)
                                .padding(20.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                tr(ui, "No dictionary entry found for this prefix.", "इस शब्द के लिए कोई प्रविष्टि नहीं मिली।", "এই শব্দের জন্য কোনো অভিধান অর্থ পাওয়া যায়নি।"),
                                fontSize = 14.sp,
                                color = app.inkSecondary,
                                fontFamily = readingFont(ui)
                            )
                        }
                    }
                }

                // If an entry is selected, show its full card first
                selectedEntry?.let { entry ->
                    item(key = "detail_${entry.id}") {
                        DictionaryEntryDetailCard(
                            entry = entry,
                            state = state,
                            onOpenCredits = onOpenCredits
                        )
                    }
                }

                // Suggestions / other entries list
                if (searchResults.size > 1) {
                    item {
                        Text(
                            tr(ui, "Matching entries", "मिलती-जुलती प्रविष्टियाँ", "অনুরূপ প্রবিষ্টসমূহ"),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = app.inkSecondary,
                            fontFamily = readingFont(ui),
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                    items(searchResults.filter { it.id != selectedEntry?.id }, key = { it.id }) { itemEntry ->
                        DictionaryResultRow(
                            entry = itemEntry,
                            onClick = { selectedEntry = itemEntry }
                        )
                    }
                }
            } else {
                // Idle mode: Saved words and info
                item {
                    Text(
                        tr(ui, "Saved words", "सहेजे गए शब्द", "সংরক্ষিত শব্দসমূহ"),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = app.ink,
                        fontFamily = readingFont(ui),
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                if (state.savedWords.isEmpty()) {
                    item {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .clip(shape)
                                .background(app.surface)
                                .border(1.dp, app.line, shape)
                                .padding(20.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                tr(
                                    ui,
                                    "No saved words yet. Press and hold any word while reading to save it.",
                                    "अभी कोई सहेजा हुआ शब्द नहीं है। पढ़ते समय किसी भी शब्द को दबाकर रखें।",
                                    "এখনও কোনো সংরক্ষিত শব্দ নেই। পড়ার সময় যেকোনো শব্দ চেপে ধরে সংরক্ষণ করুন।"
                                ),
                                fontSize = 13.sp,
                                lineHeight = 19.sp,
                                color = app.inkSecondary,
                                fontFamily = readingFont(ui)
                            )
                        }
                    }
                } else {
                    items(state.savedWords, key = { it.id }) { sw ->
                        SavedWordRow(
                            saved = sw,
                            state = state,
                            onSelect = { query = sw.word }
                        )
                    }
                }

                // Note on offline dictionary
                item {
                    VSpace(8)
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .clip(shape)
                            .background(app.surface.copy(alpha = 0.5f))
                            .border(1.dp, app.line.copy(alpha = 0.5f), shape)
                            .padding(14.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                tr(ui, "Offline Granth Dictionary", "ऑफ़लाइन ग्रंथ शब्दकोश", "অফলাইন গ্রন্থ অভিধান"),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = app.ink,
                                fontFamily = readingFont(ui)
                            )
                            Text(
                                tr(
                                    ui,
                                    "Works 100% offline. Covers Sanskrit, Hindi, Bengali and English definitions from Wiktionary (CC BY-SA 4.0).",
                                    "पूरी तरह ऑफ़लाइन काम करता है। विक्षनरी (CC BY-SA 4.0) से संस्कृत, हिन्दी, बंगाली और अंग्रेज़ी अर्थ शामिल हैं।",
                                    "সম্পূর্ণ অফলাইনে কাজ করে। উইকিঅভিধান (CC BY-SA 4.0) থেকে সংস্কৃত, হিন্দি, বাংলা ও ইংরেজি অর্থ যুক্ত।"
                                ),
                                fontSize = 12.sp,
                                lineHeight = 17.sp,
                                color = app.inkSecondary,
                                fontFamily = readingFont(ui)
                            )
                        }
                    }
                }
            }

            item { VSpace(48) }
        }
    }
}

/** Detailed card displaying all senses, etymology, and action buttons for an entry. */
@Composable
private fun DictionaryEntryDetailCard(
    entry: DictEntry,
    state: AppState,
    onOpenCredits: () -> Unit
) {
    val app = LocalAppColors.current
    val ui = state.uiLang
    val ctx = LocalContext.current
    val shape = Radius.card
    val isSaved = state.isWordSaved(entry.headword, entry.lang)

    Box(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(app.surface)
            .border(1.dp, app.line, shape)
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // Header: Headword, POS, Saved toggle & Share
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            entry.headword,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = app.ink,
                            fontFamily = fontForLang(entry.lang)
                        )
                        if (entry.pos.isNotBlank()) {
                            Box(
                                Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(app.accent.copy(alpha = 0.12f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    "[${entry.pos}]",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = app.accent
                                )
                            }
                        }
                    }

                    if (entry.ipa != null && entry.ipa.isNotBlank()) {
                        Text(
                            entry.ipa,
                            fontSize = 13.sp,
                            color = app.inkSecondary
                        )
                    }
                    if (entry.isCrossScript) {
                        Text(
                            tr(
                                ui,
                                "(Sanskrit origin - also in ${entry.originLang?.uppercase() ?: "Hindi"})",
                                "(संस्कृत मूल - ${entry.originLang?.uppercase() ?: "हिन्दी"} में भी)",
                                "(সংস্কৃত মূল - ${entry.originLang?.uppercase() ?: "হিন্দি"}তেও)"
                            ),
                            fontSize = 12.sp,
                            color = app.accent,
                            fontFamily = readingFont(ui)
                        )
                    }
                }

                // Bookmark toggle button
                IconButton(
                    onClick = { state.toggleSavedWord(entry.headword, entry.lang, "") },
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        painter = painterResource(Ic.Bookmark),
                        contentDescription = "Save word",
                        tint = if (isSaved) app.accent else app.inkSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Share definition button
                IconButton(
                    onClick = {
                        val shareText = buildString {
                            append(entry.headword)
                            if (entry.pos.isNotBlank()) append(" [${entry.pos}]")
                            append("\n")
                            entry.senses.forEachIndexed { i, s ->
                                append("${i + 1}. ${s.gloss}\n")
                            }
                            append("\n - Shrimad Bhagavatam App (Wiktionary, CC BY-SA 4.0)")
                        }
                        val sendIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, shareText)
                            type = "text/plain"
                        }
                        ctx.startActivity(Intent.createChooser(sendIntent, null))
                    },
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        painter = painterResource(Ic.Copy),
                        contentDescription = "Share",
                        tint = app.inkSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Etymology if present
            if (entry.etymology != null && entry.etymology.isNotBlank()) {
                Text(
                    tr(ui, "Etymology: ", "व्युत्पत्ति: ", "ব্যুৎপত্তি: ") + entry.etymology,
                    fontSize = 12.sp,
                    color = app.inkSecondary,
                    fontFamily = fontForLang(entry.lang)
                )
            }

            // Senses list
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (entry.senses.isEmpty()) {
                    Text(
                        tr(ui, "No specific definitions recorded.", "कोई विशिष्ट अर्थ दर्ज नहीं है।", "কোনো নির্দিষ্ট অর্থ নথিভুক্ত নেই।"),
                        fontSize = 13.sp,
                        color = app.inkSecondary,
                        fontFamily = readingFont(ui)
                    )
                } else {
                    entry.senses.forEachIndexed { idx, sense ->
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                localDigits("${idx + 1}.", ui),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = app.accent
                            )
                            Column(Modifier.weight(1f)) {
                                Text(
                                    sense.gloss,
                                    fontSize = 14.sp,
                                    lineHeight = 20.sp,
                                    color = app.ink,
                                    fontFamily = fontForLang(entry.lang)
                                )
                                if (sense.example != null && sense.example.isNotBlank()) {
                                    Text(
                                        "\"${sense.example}\"",
                                        fontSize = 12.sp,
                                        lineHeight = 17.sp,
                                        color = app.inkSecondary,
                                        fontFamily = fontForLang(entry.lang),
                                        modifier = Modifier.padding(top = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Source Attribution line
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
                    .clickable { onOpenCredits() },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    (entry.source?.name ?: "Wiktionary") + " · CC BY-SA 4.0",
                    fontSize = 11.sp,
                    color = app.inkSecondary
                )
                Text(
                    tr(ui, "Credits", "आभार", "স্বীকার"),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = app.accent
                )
            }
        }
    }
}

/** Result row for suggestion list. */
@Composable
private fun DictionaryResultRow(
    entry: DictEntry,
    onClick: () -> Unit
) {
    val app = LocalAppColors.current
    val shape = RoundedCornerShape(10.dp)
    val firstSense = entry.senses.firstOrNull()?.gloss ?: ""

    Box(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(app.surface)
            .border(1.dp, app.line, shape)
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        entry.headword,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = app.ink,
                        fontFamily = fontForLang(entry.lang)
                    )
                    if (entry.pos.isNotBlank()) {
                        Text(
                            "[${entry.pos}]",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = app.accent
                        )
                    }
                }
                if (firstSense.isNotBlank()) {
                    Text(
                        firstSense,
                        fontSize = 13.sp,
                        color = app.inkSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontFamily = fontForLang(entry.lang)
                    )
                }
            }
            Icon(
                painter = painterResource(Ic.KeyboardArrowRight),
                contentDescription = null,
                tint = app.inkSecondary,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

/** Row displaying a saved word with delete and tap actions. */
@Composable
private fun SavedWordRow(
    saved: SavedWord,
    state: AppState,
    onSelect: () -> Unit
) {
    val app = LocalAppColors.current
    val ui = state.uiLang
    val shape = RoundedCornerShape(12.dp)

    Box(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(app.surface)
            .border(1.dp, app.line, shape)
            .clickable { onSelect() }
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(Ic.Bookmark),
                contentDescription = null,
                tint = app.accent,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    saved.word,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = app.ink,
                    fontFamily = fontForLang(saved.lang)
                )
                if (saved.ref.isNotBlank()) {
                    Text(
                        tr(ui, "From verse: ", "श्लोक से: ", "শ্লোক থেকে: ") + localDigits(saved.ref, ui),
                        fontSize = 12.sp,
                        color = app.inkSecondary
                    )
                }
            }
            IconButton(
                onClick = { state.removeSavedWord(saved.id) },
                modifier = Modifier.size(44.dp)
            ) {
                Icon(
                    painter = painterResource(Ic.DeleteOutline),
                    contentDescription = tr(ui, "Remove", "हटाएँ", "মুছুন"),
                    tint = app.inkSecondary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
