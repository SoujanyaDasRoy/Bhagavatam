package com.bhagavatam.app.audio.stream

import android.content.Context
import android.net.Uri
import androidx.annotation.OptIn
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URLEncoder

enum class StreamStatus {
    IDLE,
    PREPARING,
    PLAYING,
    PAUSED,
    ENDED,
    ERROR
}

@OptIn(UnstableApi::class)
class YouTubeStreamPlayer(private val context: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var tickerJob: Job? = null
    private var resolveJob: Job? = null

    var status by mutableStateOf(StreamStatus.IDLE)
        private set
    var currentVideoId by mutableStateOf<String?>(null)
        private set
    var currentTitle by mutableStateOf("")
        private set
    var currentSubtitle by mutableStateOf("")
        private set
    var durationMs by mutableLongStateOf(0L)
        private set
    var positionMs by mutableLongStateOf(0L)
        private set
    var bufferedPositionMs by mutableLongStateOf(0L)
        private set
    var progress by mutableFloatStateOf(0f)
        private set
    var speed by mutableFloatStateOf(1.0f)
        private set
    var activeTrack by mutableStateOf<AudioStreamTrack?>(null)
        private set
    var errorMessage by mutableStateOf<String?>(null)
        private set

    val isPlaying: Boolean
        get() = status == StreamStatus.PLAYING || status == StreamStatus.PREPARING

    val exoPlayer: ExoPlayer by lazy {
        val dataSourceFactory = YouTubeMediaCache.createCacheDataSourceFactory(context)
        val mediaSourceFactory = DefaultMediaSourceFactory(context).setDataSourceFactory(dataSourceFactory)

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_SPEECH)
            .build()

        ExoPlayer.Builder(context)
            .setMediaSourceFactory(mediaSourceFactory)
            .setAudioAttributes(audioAttributes, true)
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .build().apply {
                addListener(playerListener)
            }
    }

    private val playerListener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            when (playbackState) {
                Player.STATE_IDLE -> {
                    if (status != StreamStatus.ERROR && status != StreamStatus.PREPARING) {
                        status = StreamStatus.IDLE
                    }
                    stopTicker()
                }
                Player.STATE_BUFFERING -> {
                    status = StreamStatus.PREPARING
                    startTicker()
                }
                Player.STATE_READY -> {
                    durationMs = exoPlayer.duration.coerceAtLeast(0L)
                    status = if (exoPlayer.playWhenReady) StreamStatus.PLAYING else StreamStatus.PAUSED
                    startTicker()
                }
                Player.STATE_ENDED -> {
                    status = StreamStatus.ENDED
                    progress = 1f
                    stopTicker()
                }
            }
        }

        override fun onIsPlayingChanged(isPlayingNow: Boolean) {
            if (isPlayingNow) {
                status = StreamStatus.PLAYING
                startTicker()
            } else if (exoPlayer.playbackState == Player.STATE_READY) {
                status = StreamStatus.PAUSED
                stopTicker()
            }
        }

        override fun onPlayerError(error: PlaybackException) {
            status = StreamStatus.ERROR
            errorMessage = error.localizedMessage ?: "Playback error: ${error.errorCodeName}"
            stopTicker()
        }
    }

    /**
     * Resolves a search query into top recitation video and initiates playback.
     */
    fun playSearchQuery(query: String, title: String, subtitle: String) {
        resolveJob?.cancel()
        status = StreamStatus.PREPARING
        currentTitle = title
        currentSubtitle = subtitle
        errorMessage = null
        progress = 0f
        positionMs = 0L

        resolveJob = scope.launch {
            val result = YouTubeMediaCache.visionOsApi.searchFirstVideo(query)
            result.onSuccess { searchVideo ->
                currentVideoId = searchVideo.videoId
                if (currentSubtitle.isEmpty() || currentSubtitle == query) {
                    currentSubtitle = "${searchVideo.channelTitle} • ${searchVideo.durationText ?: ""}"
                }
                playResolvedVideo(searchVideo.videoId, title, currentSubtitle)
            }.onFailure { error ->
                // Try fallback query with encoding directly into virtual URI
                val encodedQuery = URLEncoder.encode(query, "UTF-8")
                val mediaItem = MediaItem.Builder()
                    .setMediaId("ytquery://$encodedQuery")
                    .setUri("ytquery://$encodedQuery")
                    .setMediaMetadata(
                        MediaMetadata.Builder()
                            .setTitle(title)
                            .setArtist(subtitle)
                            .build()
                    )
                    .build()

                try {
                    exoPlayer.setMediaItem(mediaItem)
                    exoPlayer.prepare()
                    exoPlayer.playWhenReady = true
                } catch (e: Exception) {
                    status = StreamStatus.ERROR
                    errorMessage = error.localizedMessage ?: "Failed to find recitation video"
                }
            }
        }
    }

    /**
     * Plays YouTube audio directly given its videoId.
     */
    fun playVideo(videoId: String, title: String, subtitle: String) {
        resolveJob?.cancel()
        currentVideoId = videoId
        currentTitle = title
        currentSubtitle = subtitle
        errorMessage = null
        progress = 0f
        positionMs = 0L
        status = StreamStatus.PREPARING

        playResolvedVideo(videoId, title, subtitle)
    }

    private fun playResolvedVideo(videoId: String, title: String, subtitle: String) {
        scope.launch {
            val trackResult = withContext(Dispatchers.IO) {
                runCatching {
                    YouTubeMediaCache.getResolver(context).getOrFetchTrack(videoId)
                }
            }

            trackResult.onSuccess { track ->
                activeTrack = track
                val mediaItem = MediaItem.Builder()
                    .setMediaId("yt://$videoId")
                    .setUri("yt://$videoId")
                    .setMediaMetadata(
                        MediaMetadata.Builder()
                            .setTitle(title)
                            .setArtist(subtitle)
                            .build()
                    )
                    .build()

                exoPlayer.setPlaybackParameters(PlaybackParameters(speed))
                exoPlayer.setMediaItem(mediaItem)
                exoPlayer.prepare()
                exoPlayer.playWhenReady = true
            }.onFailure { e ->
                status = StreamStatus.ERROR
                errorMessage = e.localizedMessage ?: "Failed to stream audio"
            }
        }
    }

    fun pause() {
        if (isPlaying) {
            exoPlayer.playWhenReady = false
            status = StreamStatus.PAUSED
            stopTicker()
        }
    }

    fun resume() {
        if (status == StreamStatus.PAUSED || status == StreamStatus.ENDED) {
            if (status == StreamStatus.ENDED) {
                exoPlayer.seekTo(0)
            }
            exoPlayer.playWhenReady = true
            status = StreamStatus.PLAYING
            startTicker()
        }
    }

    fun togglePlay() {
        if (isPlaying) pause() else resume()
    }

    fun seekTo(posMs: Long) {
        val target = posMs.coerceIn(0L, durationMs.coerceAtLeast(1L))
        positionMs = target
        progress = if (durationMs > 0) (target.toFloat() / durationMs).coerceIn(0f, 1f) else 0f
        exoPlayer.seekTo(target)
    }

    fun seekToFraction(fraction: Float) {
        if (durationMs > 0) {
            seekTo((durationMs * fraction.coerceIn(0f, 1f)).toLong())
        }
    }

    fun setPlaybackSpeed(newSpeed: Float) {
        speed = newSpeed
        exoPlayer.setPlaybackParameters(PlaybackParameters(newSpeed))
    }

    fun stop() {
        resolveJob?.cancel()
        stopTicker()
        exoPlayer.stop()
        status = StreamStatus.IDLE
        positionMs = 0L
        progress = 0f
    }

    fun release() {
        stop()
        scope.cancel()
        exoPlayer.removeListener(playerListener)
        exoPlayer.release()
    }

    private fun startTicker() {
        stopTicker()
        tickerJob = scope.launch {
            while (isActive) {
                if (exoPlayer.playbackState == Player.STATE_READY || exoPlayer.playbackState == Player.STATE_BUFFERING) {
                    val pos = exoPlayer.currentPosition.coerceAtLeast(0L)
                    val dur = exoPlayer.duration.coerceAtLeast(0L)
                    val buf = exoPlayer.bufferedPosition.coerceAtLeast(0L)

                    positionMs = pos
                    if (dur > 0L) {
                        durationMs = dur
                        progress = (pos.toFloat() / dur.toFloat()).coerceIn(0f, 1f)
                    }
                    bufferedPositionMs = buf
                }
                delay(250)
            }
        }
    }

    private fun stopTicker() {
        tickerJob?.cancel()
        tickerJob = null
    }
}
