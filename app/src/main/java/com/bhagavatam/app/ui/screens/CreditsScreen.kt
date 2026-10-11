package com.bhagavatam.app.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bhagavatam.app.data.DictSource
import com.bhagavatam.app.data.DictionaryDb
import com.bhagavatam.app.data.localDigits
import com.bhagavatam.app.data.tr
import com.bhagavatam.app.state.AppState
import com.bhagavatam.app.ui.components.Ic
import com.bhagavatam.app.ui.components.VSpace
import com.bhagavatam.app.ui.theme.LocalAppColors
import com.bhagavatam.app.ui.theme.Radius

/**
 * Credits and licensing screen for the offline dictionary sources.
 * Displays all Wiktionary editions used under CC BY-SA 4.0 as required by the license.
 */
@Composable
fun CreditsScreen(state: AppState, onBack: () -> Unit) {
    val app = LocalAppColors.current
    val ui = state.uiLang
    val ctx = LocalContext.current
    val sources = remember { DictionaryDb.allSources() }
    val shape = Radius.card

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .background(app.bg)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top navigation bar
        Row(
            Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
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
                tr(ui, "Dictionary credits", "शब्दकोश आभार", "অভিধান কৃতজ্ঞতা স্বীকার"),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = app.ink,
                fontFamily = readingFont(ui)
            )
        }

        // License overview card
        Box(
            Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(app.surface)
                .border(1.dp, app.line, shape)
                .padding(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    tr(ui, "Creative Commons Attribution - ShareAlike 4.0", "क्रिएटिव कॉमन्स 4.0 (CC BY-SA 4.0)", "ক্রিয়েটিভ কমন্স ৪.০ (CC BY-SA 4.0)"),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = app.accent,
                    fontFamily = readingFont(ui)
                )
                Text(
                    tr(
                        ui,
                        "The offline dictionary in Bhagavatam is built from Wikimedia dumps. All definitions and word senses are created by community contributors under CC BY-SA 4.0. Note: This share-alike license applies to the dictionary database itself, not to the Bhagavatam reader codebase.",
                        "भागवतम में उपलब्ध ऑफ़लाइन शब्दकोश विकिमीडिया डेटा से निर्मित है। सभी अर्थ और परिभाषाएँ CC BY-SA 4.0 के अंतर्गत समुदाय के योगदानकर्ताओं द्वारा तैयार की गई हैं। यह लाइसेंस केवल शब्दकोश डेटाबेस पर लागू होता है, ऐप के मुख्य कोड पर नहीं।",
                        "ভাগবতমে অন্তর্ভুক্ত অফলাইন অভিধান উইকিমিডিয়া ডাম্প থেকে তৈরি। সমস্ত সংজ্ঞা এবং অর্থ CC BY-SA 4.0 লাইসেন্সের অধীনে সম্প্রদায়ের অবদানকারীদের দ্বারা লিখিত। এই শেয়ার-অ্যালাইক লাইসেন্সটি কেবল অভিধান ডাটাবেজের ক্ষেত্রে প্রযোজ্য, মূল অ্যাপ কোডের ক্ষেত্রে নয়।"
                    ),
                    fontSize = 13.sp,
                    lineHeight = 19.sp,
                    color = app.inkSecondary,
                    fontFamily = readingFont(ui)
                )
            }
        }

        // Source list
        Text(
            tr(ui, "Sources used", "उपयोग किए गए स्रोत", "ব্যবহৃত উৎসসমূহ"),
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = app.ink,
            fontFamily = readingFont(ui)
        )

        val defaultSources = if (sources.isNotEmpty()) sources else listOf(
            DictSource(1, "enwikt", "English Wiktionary", "CC BY-SA 4.0", "https://en.wiktionary.org", "Wikimedia contributors", "2026-10"),
            DictSource(2, "hiwikt", "Hindi Wiktionary", "CC BY-SA 4.0", "https://hi.wiktionary.org", "Wikimedia contributors", "2026-10"),
            DictSource(3, "bnwikt", "Bengali Wiktionary", "CC BY-SA 4.0", "https://bn.wiktionary.org", "Wikimedia contributors", "2026-10"),
            DictSource(4, "orwikt", "Odia Wiktionary", "CC BY-SA 4.0", "https://or.wiktionary.org", "Wikimedia contributors", "2026-10")
        )

        for (s in defaultSources) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(shape)
                    .background(app.surface)
                    .border(1.dp, app.line, shape)
                    .padding(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            s.name,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = app.ink
                        )
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(app.accent.copy(alpha = 0.12f))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                s.licence,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = app.accent
                            )
                        }
                    }

                    Text(
                        tr(ui, "Attribution: ", "श्रेय: ", "কৃতজ্ঞতা: ") + s.attribution,
                        fontSize = 13.sp,
                        color = app.inkSecondary
                    )

                    if (s.retrieved.isNotBlank()) {
                        Text(
                            tr(ui, "Dump version: ", "संस्करण: ", "সংস্করণ: ") + localDigits(s.retrieved, ui),
                            fontSize = 12.sp,
                            color = app.inkSecondary
                        )
                    }

                    if (s.url.isNotBlank()) {
                        Row(
                            Modifier
                                .padding(top = 4.dp)
                                .clickable {
                                    runCatching {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(s.url))
                                        ctx.startActivity(intent)
                                    }
                                },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                s.url,
                                fontSize = 13.sp,
                                color = app.accent,
                                fontWeight = FontWeight.Medium
                            )
                            Icon(
                                painter = painterResource(Ic.KeyboardArrowRight),
                                contentDescription = null,
                                tint = app.accent,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }

        // Note on Odia
        Box(
            Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(app.surface.copy(alpha = 0.6f))
                .border(1.dp, app.line.copy(alpha = 0.6f), shape)
                .padding(14.dp)
        ) {
            Text(
                tr(
                    ui,
                    "Note: Odia Wiktionary coverage is based on preliminary entries and is currently an unverified draft.",
                    "सूचना: ओड़िया विक्षनरी के अर्थ प्रारंभिक प्रविष्टियों पर आधारित हैं और वर्तमान में अपरीक्षित प्रारूप हैं।",
                    "উল্লেখ্য: ওড়িয়া উইকিঅভিধানের কভারেজ প্রাথমিক তথ্যের ওপর ভিত্তি করে তৈরি এবং এটি বর্তমানে একটি অপ্রমাণিত খসড়া।"
                ),
                fontSize = 12.sp,
                lineHeight = 17.sp,
                color = app.inkSecondary,
                fontFamily = readingFont(ui)
            )
        }

        VSpace(48)
    }
}
