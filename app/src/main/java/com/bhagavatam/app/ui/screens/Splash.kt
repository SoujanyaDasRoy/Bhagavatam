package com.bhagavatam.app.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bhagavatam.app.R
import com.bhagavatam.app.data.Lang
import com.bhagavatam.app.data.stringsFor
import com.bhagavatam.app.state.AppState
import com.bhagavatam.app.ui.theme.EnglishReading
import com.bhagavatam.app.ui.theme.NotoDevanagari
import com.bhagavatam.app.ui.theme.NotoSerifBengali

/** Shown while the text database opens. Same night background as the system splash so the hand-off is seamless. */
@Composable
fun SplashScreen(state: AppState? = null) {
    // The splash is always night-blue: the clock and battery must be light, whatever the phone's own theme is.
    val view = LocalView.current
    if (!view.isInEditMode) SideEffect {
        (view.context as? android.app.Activity)?.window?.let { w ->
            WindowCompat.getInsetsController(w, view).apply { isAppearanceLightStatusBars = false; isAppearanceLightNavigationBars = false }
        }
    }
    val logo = remember { Animatable(0f) }
    val name = remember { Animatable(0f) }
    val line = remember { Animatable(0f) }
    val greet = remember { Animatable(0f) }
    val motion = com.bhagavatam.app.ui.components.animationsOn()
    LaunchedEffect(Unit) {
        if (!motion) { logo.snapTo(1f); name.snapTo(1f); line.snapTo(1f); greet.snapTo(1f); return@LaunchedEffect }
        kotlinx.coroutines.coroutineScope {
            launch { logo.animateTo(1f, tween(700, easing = FastOutSlowInEasing)) }
            launch { delay(250); name.animateTo(1f, tween(650)) }
            launch { delay(500); line.animateTo(1f, tween(700, easing = FastOutSlowInEasing)) }
            launch { delay(750); greet.animateTo(1f, tween(650)) }
        }
    }
    val breatheAnim = rememberInfiniteTransition(label = "glow").animateFloat(
        0.85f, 1.0f, infiniteRepeatable(tween(2600, easing = LinearEasing), RepeatMode.Reverse), label = "glow",
    )
    val breathe = if (motion) breatheAnim else androidx.compose.runtime.mutableStateOf(1f)
    val ui = state?.uiLang ?: Lang.EN
    val s = stringsFor(ui)
    val bodyFont = when (ui) {
        Lang.BN -> NotoSerifBengali
        Lang.HI -> NotoDevanagari
        else -> EnglishReading
    }
    val gold = Color(0xFFF5C97A)

    Box(
        Modifier.fillMaxSize().background(
            Brush.verticalGradient(listOf(Color(0xFF0B0620), Color(0xFF160B34), Color(0xFF0B0620)))
        ),
        contentAlignment = Alignment.Center,
    ) {
        // A warm glow behind the mark, breathing slowly.
        Box(
            Modifier.size(340.dp).alpha(logo.value * breathe.value).background(
                Brush.radialGradient(listOf(gold.copy(alpha = 0.28f), gold.copy(alpha = 0.08f), Color.Transparent))
            )
        )
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier.alpha(logo.value).graphicsLayer { val k = 0.88f + 0.12f * logo.value; scaleX = k; scaleY = k }
                    .border(1.dp, gold.copy(alpha = 0.55f), RoundedCornerShape(36.dp)).padding(5.dp)
            ) {
                Image(painterResource(R.drawable.logo_mark), "Bhagavatam", Modifier.size(124.dp).clip(RoundedCornerShape(30.dp)))
            }
            Spacer(Modifier.height(26.dp))
            Text(
                "श्रीमद्भागवतम्", Modifier.alpha(name.value).graphicsLayer { translationY = (1f - name.value) * 16f },
                fontFamily = NotoDevanagari, fontSize = 34.sp, color = gold,
            )
            Spacer(Modifier.height(14.dp))
            // A thin ornament: a line that grows from the middle, with a small diamond.
            Row(Modifier.alpha(line.value), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.height(1.dp).width((56 * line.value).dp).background(gold.copy(alpha = 0.6f)))
                Box(Modifier.padding(horizontal = 10.dp).size(7.dp).rotate(45f).background(gold))
                Box(Modifier.height(1.dp).width((56 * line.value).dp).background(gold.copy(alpha = 0.6f)))
            }
            Spacer(Modifier.height(14.dp))
            Text(
                s.shrimadBhagavatMahapuran, Modifier.alpha(name.value).padding(horizontal = 24.dp), fontFamily = bodyFont, fontSize = 17.sp,
                color = Color(0xFFD9D2EC), letterSpacing = 0.4.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
            Spacer(Modifier.height(30.dp))
            Text(s.jaiShreeMadhav, Modifier.alpha(greet.value).padding(horizontal = 24.dp), fontFamily = bodyFont, fontSize = 15.sp, color = gold.copy(alpha = 0.85f), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        }
    }
}
