package com.bhagavatam.app.ui.components

import android.provider.Settings
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

/** A hit area of at least 48 dp; the drawn icon keeps its own size. */
fun Modifier.minTouch(): Modifier = this.sizeIn(minWidth = 48.dp, minHeight = 48.dp)

/** False when the person has turned system animations off (or to zero): repeating and staggered motion should then be skipped. */
@Composable
fun animationsOn(): Boolean {
    val ctx = LocalContext.current
    return remember { Settings.Global.getFloat(ctx.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) > 0f }
}
