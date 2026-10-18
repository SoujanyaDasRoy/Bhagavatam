package com.bhagavatam.app.ui.components

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventTimeoutCancellationException
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.positionChange

/**
 * A tap, or a press and hold that goes on following the finger: [onHoldStart] when the hold is recognised, [onHoldMove] while the finger
 * drags, [onHoldEnd] when it lifts. Scrolling is left to the parent: a drag that starts before the hold is recognised is not ours.
 */
suspend fun PointerInputScope.detectTapOrHoldDrag(
    onTap: (Offset) -> Unit,
    onHoldStart: (Offset) -> Unit,
    onHoldMove: (Offset) -> Unit,
    onHoldEnd: () -> Unit,
) = awaitEachGesture {
    val down = awaitFirstDown(requireUnconsumed = false)
    var held = false
    val up = try {
        withTimeout(viewConfiguration.longPressTimeoutMillis) { waitForUpOrCancellation() }
    } catch (_: PointerEventTimeoutCancellationException) {
        held = true
        null
    }
    if (!held) {
        if (up != null) {
            up.consume()
            onTap(up.position)
        }
        return@awaitEachGesture
    }
    onHoldStart(down.position)
    // the end is reported also when the gesture is cancelled from outside (the screen changes, a system gesture takes over),
    // so a selection never stays half made
    try {
        while (true) {
            val event = awaitPointerEvent()
            val change = event.changes.firstOrNull { it.id == down.id } ?: break
            if (!change.pressed) {
                change.consume()
                break
            }
            if (change.positionChange() != Offset.Zero) {
                change.consume()
                onHoldMove(change.position)
            }
        }
    } finally {
        onHoldEnd()
    }
}
