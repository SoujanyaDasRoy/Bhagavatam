package com.bhagavatam.app.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Optional artwork. Drop a WebP named e.g. `sk_03.webp` or `ch_10_29.webp` into res/drawable-nodpi
 * (tools/import_art.py does the resizing and naming) and it appears; with no file the card keeps its
 * gradient and mandala. Looked up by name, so nothing breaks when an image is missing.
 */
@Composable
fun artRes(name: String): Int {
    val ctx = LocalContext.current
    return remember(name) { ctx.resources.getIdentifier(name, "drawable", ctx.packageName) }
}

fun skArt(n: Int) = "sk_" + n.toString().padStart(2, '0')
fun chArt(s: Int, a: Int) = "ch_${s}_$a"

/** Cream text must reach 4.5:1 on both stops; the far stop is darkened only as far as that needs. */
fun skGradient(hue: Color): List<Color> {
    var far = lerp(hue, Color.Black, .28f)
    var f = .28f
    while (contrastWithCream(far) < 4.6f && f < .6f) { f += .02f; far = lerp(hue, Color.Black, f) }
    return listOf(lerp(hue, Color.Black, .58f), far)
}

private fun contrastWithCream(c: Color): Float = (0.9113f + 0.05f) / (c.luminance() + 0.05f) // FFF4DC has relative luminance 0.9113

/** Fills its parent: the gradient, then the artwork under a scrim if it exists, else a mandala. */
@Composable
fun ArtBackdrop(art: String, colors: List<Color>, modifier: Modifier = Modifier, petals: Int = 16, mandala: Color = Color(0x30FFFFFF)) {
    val id = artRes(art)
    Box(modifier.background(Brush.linearGradient(colors))) {
        if (id != 0) {
            Image(painterResource(id), null, Modifier.matchParentSize(), contentScale = ContentScale.Crop)
            Box(Modifier.matchParentSize().background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.10f), Color.Black.copy(alpha = 0.74f)))))
        } else {
            Mandala(mandala, Modifier.matchParentSize(), petals)
        }
    }
}

/** Reader chapter banner: shown only when that chapter has artwork. */
@Composable
fun ChapterBanner(s: Int, a: Int, height: Dp = 160.dp) {
    val name = chArt(s, a)
    if (artRes(name) == 0) return
    Box(Modifier.fillMaxWidth().height(height).clip(RoundedCornerShape(20.dp))) {
        Image(painterResource(artRes(name)), null, Modifier.matchParentSize(), contentScale = ContentScale.Crop)
        Box(Modifier.matchParentSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.45f)))))
    }
}
