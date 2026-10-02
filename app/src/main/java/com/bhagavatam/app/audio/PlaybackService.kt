package com.bhagavatam.app.audio

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.MediaMetadata
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.Build
import android.os.IBinder
import android.graphics.drawable.Icon
import androidx.core.content.ContextCompat
import androidx.compose.runtime.snapshotFlow
import com.bhagavatam.app.BhagavatamApp
import com.bhagavatam.app.MainActivity
import com.bhagavatam.app.R
import com.bhagavatam.app.data.SampleData
import com.bhagavatam.app.state.AppState
import com.bhagavatam.app.state.AudioStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Keeps the voice going when the screen is off or the app is in the background, and gives the lock screen and the
 * notification shade previous, play or pause, and next. It only shows and forwards: all playback logic stays in
 * [AppState]. It is started when playback begins and stops itself when the listener stops or the chapter ends.
 */
class PlaybackService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private lateinit var session: MediaSession
    private val state: AppState get() = (application as BhagavatamApp).state
    private var foreground = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        val channel = NotificationChannel(CHANNEL, getString(R.string.playback_channel), NotificationManager.IMPORTANCE_LOW).apply { setShowBadge(false) }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        session = MediaSession(this, "Bhagavatam").apply {
            setCallback(object : MediaSession.Callback() {
                override fun onPlay() = state.togglePlay()
                override fun onPause() = state.pausePlayback()
                override fun onSkipToNext() = state.next()
                override fun onSkipToPrevious() = state.previous()
                override fun onStop() = state.pausePlayback()
            })
            isActive = true
        }
        // Follow the player: whenever what is playing, or whether it is, changes, refresh the notification and the session.
        scope.launch {
            snapshotFlow { Triple(state.audioStatus, state.index, state.hasSession) }.collectLatest { (status, _, _) -> render(status) }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PLAY_PAUSE -> state.togglePlay()
            ACTION_NEXT -> state.next()
            ACTION_PREV -> state.previous()
            ACTION_CLOSE -> { state.pausePlayback(); stopSelf() }
        }
        // Android requires startForeground soon after startForegroundService, even if there is nothing to show yet.
        if (!foreground) render(state.audioStatus)
        return START_NOT_STICKY
    }

    private fun render(status: AudioStatus) {
        val s = state
        val playing = status == AudioStatus.PLAYING || status == AudioStatus.PREPARING
        if (!s.hasSession || status == AudioStatus.IDLE || status == AudioStatus.ENDED) {
            if (foreground) { stopForeground(STOP_FOREGROUND_REMOVE); foreground = false }
            session.setPlaybackState(PlaybackState.Builder().setState(PlaybackState.STATE_STOPPED, 0, 1f).build())
            if (status == AudioStatus.ENDED || !s.hasSession) stopSelf()
            return
        }
        val v = s.current
        val title = SampleData.adhyayaTitle(v.skandha, v.adhyaya, s.titleLang, s.strings)
        val line = if (v.skandha == 0) "${s.strings.mahatmya} ${v.adhyaya}.${v.numLabel}" else "${v.skandha}.${v.adhyaya}.${v.numLabel}"
        session.setMetadata(MediaMetadata.Builder().putString(MediaMetadata.METADATA_KEY_TITLE, title).putString(MediaMetadata.METADATA_KEY_ARTIST, line).build())
        val actions = PlaybackState.ACTION_PLAY or PlaybackState.ACTION_PAUSE or PlaybackState.ACTION_PLAY_PAUSE or PlaybackState.ACTION_SKIP_TO_NEXT or PlaybackState.ACTION_SKIP_TO_PREVIOUS
        session.setPlaybackState(PlaybackState.Builder().setActions(actions).setState(if (playing) PlaybackState.STATE_PLAYING else PlaybackState.STATE_PAUSED, PlaybackState.PLAYBACK_POSITION_UNKNOWN, 1f).build())

        val open = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP), PendingIntent.FLAG_IMMUTABLE)
        fun act(icon: Int, label: String, pi: PendingIntent) = Notification.Action.Builder(Icon.createWithResource(this, icon), label, pi).build()
        val n = Notification.Builder(this, CHANNEL)
            .setSmallIcon(R.drawable.ic_headphones)
            .setContentTitle(title).setContentText(line)
            .setContentIntent(open).setOnlyAlertOnce(true).setShowWhen(false)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setOngoing(playing)
            .setDeleteIntent(action(ACTION_CLOSE, 9))
            .addAction(act(R.drawable.ic_skip_back, getString(R.string.prev_shloka), action(ACTION_PREV, 1)))
            .addAction(act(if (playing) R.drawable.ic_pause else R.drawable.ic_play, getString(if (playing) R.string.pause else R.string.play), action(ACTION_PLAY_PAUSE, 2)))
            .addAction(act(R.drawable.ic_skip_forward, getString(R.string.next_shloka), action(ACTION_NEXT, 3)))
            .setStyle(Notification.MediaStyle().setMediaSession(session.sessionToken).setShowActionsInCompactView(0, 1, 2))
            .build()
        if (playing || !foreground) {
            if (Build.VERSION.SDK_INT >= 29) startForeground(ID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK) else startForeground(ID, n)
            foreground = true
        } else {
            // Paused: leave foreground so the notification can be swiped away, but keep showing it.
            stopForeground(STOP_FOREGROUND_DETACH); foreground = false
            getSystemService(NotificationManager::class.java).notify(ID, n)
        }
    }

    private fun action(name: String, code: Int): PendingIntent =
        PendingIntent.getService(this, code, Intent(this, PlaybackService::class.java).setAction(name), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

    override fun onDestroy() {
        scope.cancel(); session.release()
        super.onDestroy()
    }

    companion object {
        private const val CHANNEL = "playback"
        private const val ID = 7
        const val ACTION_PLAY_PAUSE = "com.bhagavatam.app.PLAY_PAUSE"
        const val ACTION_NEXT = "com.bhagavatam.app.NEXT"
        const val ACTION_PREV = "com.bhagavatam.app.PREV"
        const val ACTION_CLOSE = "com.bhagavatam.app.CLOSE"

        /** Called when playback begins. Safe to call again while it runs. */
        fun start(context: Context) = ContextCompat.startForegroundService(context, Intent(context, PlaybackService::class.java))
    }
}
