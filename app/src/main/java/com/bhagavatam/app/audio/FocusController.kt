package com.bhagavatam.app.audio

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import androidx.core.content.ContextCompat

/**
 * Plays politely: asks for audio focus before speaking, steps back when a call or another player needs the speaker,
 * and stops when headphones are pulled out. Android does not duck spoken content on its own (a listener could miss
 * words), so the player has to pause itself.
 *
 * [onLoss] is told whether the loss is temporary (a call, a notification) so playback can resume when it ends.
 */
class FocusController(
    private val app: Application,
    private val onLoss: (temporary: Boolean) -> Unit,
    private val onGain: () -> Unit,
) {
    private val audio = app.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var request: AudioFocusRequest? = null
    private var noisyRegistered = false

    private val change = AudioManager.OnAudioFocusChangeListener { what ->
        when (what) {
            AudioManager.AUDIOFOCUS_LOSS -> onLoss(false)
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT, AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> onLoss(true)
            AudioManager.AUDIOFOCUS_GAIN -> onGain()
        }
    }

    private val noisy = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == AudioManager.ACTION_AUDIO_BECOMING_NOISY) onLoss(false)
        }
    }

    /** True when focus was granted (it is refused during a phone call). */
    fun acquire(): Boolean {
        if (request == null) {
            val attrs = AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build()
            request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(attrs).setOnAudioFocusChangeListener(change).setWillPauseWhenDucked(true).build()
        }
        val granted = audio.requestAudioFocus(request!!) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        if (granted && !noisyRegistered) {
            ContextCompat.registerReceiver(app, noisy, IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY), ContextCompat.RECEIVER_NOT_EXPORTED)
            noisyRegistered = true
        }
        return granted
    }

    fun release() {
        request?.let { audio.abandonAudioFocusRequest(it) }
        if (noisyRegistered) { runCatching { app.unregisterReceiver(noisy) }; noisyRegistered = false }
    }
}
