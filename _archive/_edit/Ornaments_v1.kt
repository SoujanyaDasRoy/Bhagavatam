package com.bhagavatam.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate

/** Lotus-mandala line art anchored at the top-right corner; clipped by its parent. Purely decorative. */
@Composable
fun Mandala(color: Color, modifier: Modifier = Modifier, petals: Int = 16) {
    Canvas(modifier) {
        val c = Offset(size.width * 0.98f, size.height * 0.02f)
        val r = size.maxDimension * 0.95f
        val st = Stroke(width = 1.2.dp.toPx())
        listOf(0.22f, 0.42f, 0.62f, 0.82f).forEach { drawCircle(color, r * it, c, style = st) }
        for (i in 0 until petals) rotate(i * 360f / petals, c) {
            val p = Path().apply {
                moveTo(c.x, c.y + r * 0.22f)
                cubicTo(c.x + r * 0.14f, c.y + r * 0.34f, c.x + r * 0.10f, c.y + r * 0.54f, c.x, c.y + r * 0.64f)
                cubicTo(c.x - r * 0.10f, c.y + r * 0.54f, c.x - r * 0.14f, c.y + r * 0.34f, c.x, c.y + r * 0.22f)
                close()
            }
            drawPath(p, color, style = st)
        }
    }
}
