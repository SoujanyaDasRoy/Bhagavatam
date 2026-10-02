package com.bhagavatam.app.ui.screens

import androidx.compose.animation.core.Animatable
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
import com.bhagavatam.app.ui.theme.EnglishReading
import com.bhagavatam.app.ui.theme.NotoDevanagari

/** Shown while the text database opens. Same night background as the system splash so the hand-off is seamless. */
@Composable
fun SplashScreen() {
    val fade = remember { Animatable(0f) }
    LaunchedEffect(Unit) { fade.animateTo(1f, tween(450)) }
    Box(Modifier.fillMaxSize().background(Color(0xFF0B0620)), contentAlignment = Alignment.Center) {
        Column(Modifier.alpha(fade.value), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Image(painterResource(R.drawable.logo_mark), "Bhagavatam", Modifier.size(132.dp).clip(RoundedCornerShape(32.dp)))
            Text("श्रीमद्भागवत महापुराण", fontFamily = NotoDevanagari, fontSize = 24.sp, fontWeight = FontWeight.Medium, color = Color(0xFFF5C97A))
            Text("Hari Om", fontFamily = EnglishReading, fontSize = 18.sp, color = Color(0xFFD9D2EC))
        }
    }
}
