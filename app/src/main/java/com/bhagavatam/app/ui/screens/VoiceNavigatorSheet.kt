package com.bhagavatam.app.ui.screens

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bhagavatam.app.data.Episode
import com.bhagavatam.app.data.Episodes
import com.bhagavatam.app.data.Lang
import com.bhagavatam.app.data.SampleData
import com.bhagavatam.app.data.localDigits
import com.bhagavatam.app.state.AppState
import com.bhagavatam.app.ui.components.Ic
import com.bhagavatam.app.ui.theme.Brand
import com.bhagavatam.app.ui.theme.EnglishReading
import com.bhagavatam.app.ui.theme.NotoDevanagari
import com.bhagavatam.app.ui.theme.NotoSerifBengali
import com.bhagavatam.app.ui.theme.Radius
import java.util.Locale

data class NavTarget(val skandha: Int, val chapter: Int, val label: String)

/** Parses speech text to find either chapter numbers or episode titles. */
fun parseVoiceQuery(text: String, ui: Lang): NavTarget? {
    val raw = text.trim()
    if (raw.isEmpty()) return null
    val lower = raw.lowercase()

    // 1. Check numeric pattern like "10.29" or "skandha 10 chapter 29"
    val numRegex = Regex("(\\d+)[\\s.:/\\-_]+(\\d+)")
    val match = numRegex.find(raw)
    if (match != null) {
        val s = match.groupValues[1].toIntOrNull() ?: 1
        val c = match.groupValues[2].toIntOrNull() ?: 1
        if (s in 0..12) {
            val maxA = SampleData.skandha(s).adhyayaCount
            val validC = c.coerceIn(1, maxA)
            return NavTarget(s, validC, "${SampleData.skandha(s).title(ui)} · ${validC}")
        }
    }

    // 2. Check Mahatmya
    if (lower.contains("mahatmya") || lower.contains("माहात्म्य") || lower.contains("মাহাত্ম্য")) {
        val chMatch = Regex("\\d+").find(raw)
        val c = chMatch?.value?.toIntOrNull()?.coerceIn(1, 6) ?: 1
        return NavTarget(0, c, "Mahatmya · $c")
    }

    // 3. Check Episodes list
    for (ep in Episodes.all) {
        if (ep.matchesQuery(lower)) {
            return NavTarget(ep.s, ep.a, ep.title(ui))
        }
    }

    return null
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceNavigatorSheet(
    state: AppState,
    onOpenChapter: (Int, Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val s = state.strings
    val ui = state.uiLang
    val ctx = LocalContext.current
    var spokenText by remember { mutableStateOf("") }
    var target by remember { mutableStateOf<NavTarget?>(null) }
    var isListening by remember { mutableStateOf(true) }

    val pulse = remember { Animatable(1f) }
    LaunchedEffect(isListening) {
        if (isListening) {
            pulse.animateTo(
                1.25f,
                infiniteRepeatable(tween(700, easing = LinearEasing), RepeatMode.Reverse)
            )
        } else {
            pulse.snapTo(1f)
        }
    }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { res ->
        if (res.resultCode == Activity.RESULT_OK && res.data != null) {
            val list = res.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val top = list?.firstOrNull() ?: ""
            spokenText = top
            isListening = false
            target = parseVoiceQuery(top, ui)
        } else {
            isListening = false
        }
    }

    val startRecognition: () -> Unit = {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            val langTag = when (ui) {
                Lang.BN -> "bn-IN"
                Lang.HI -> "hi-IN"
                else -> Locale.getDefault().toLanguageTag()
            }
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, langTag)
            putExtra(RecognizerIntent.EXTRA_PROMPT, s.voiceSearchHint)
        }
        runCatching { launcher.launch(intent) }.onFailure {
            isListening = false
            spokenText = "Voice recognition not supported on this device."
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) startRecognition()
        else {
            isListening = false
            spokenText = "Microphone permission is required for voice navigation."
        }
    }

    LaunchedEffect(Unit) {
        if (androidx.core.content.ContextCompat.checkSelfPermission(ctx, android.Manifest.permission.RECORD_AUDIO) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            startRecognition()
        } else {
            permissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Brand.Card,
    ) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                Modifier.size(72.dp).scale(pulse.value).clip(CircleShape).background(Brand.KesariTint),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painterResource(Ic.Mic),
                    contentDescription = null,
                    tint = Brand.Kesari,
                    modifier = Modifier.size(36.dp)
                )
            }

            Text(
                if (isListening) s.voiceSearchHint else if (spokenText.isNotEmpty()) "\"$spokenText\"" else s.noMatches,
                fontSize = 17.sp,
                fontWeight = FontWeight.Medium,
                color = Brand.Ink,
                textAlign = TextAlign.Center
            )

            if (target != null) {
                val t = target!!
                Column(
                    Modifier.fillMaxWidth().clip(Radius.card).background(Brand.Fill).padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        String.format(s.openPrompt, t.label),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Brand.Ink,
                        textAlign = TextAlign.Center
                    )
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            Modifier.weight(1f).heightIn(min = 48.dp).clip(Radius.group).background(Brand.Card)
                                .border(1.dp, Brand.Separator, Radius.group)
                                .clickable { onDismiss() },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(s.cancelAction, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Brand.Secondary)
                        }
                        Box(
                            Modifier.weight(1f).heightIn(min = 48.dp).clip(Radius.group).background(Brand.Kesari)
                                .clickable {
                                    onOpenChapter(t.skandha, t.chapter)
                                    onDismiss()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(s.openAction, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Brand.OnKesari)
                        }
                    }
                }
            }
        }
    }
}
