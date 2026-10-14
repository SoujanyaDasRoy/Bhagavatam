package com.bhagavatam.app.audio.stream

import android.net.Uri
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.ResolvingDataSource
import kotlinx.coroutines.runBlocking
import java.io.IOException
import java.net.URLDecoder
import java.util.concurrent.ConcurrentHashMap

/**
 * Resolves virtual URIs (`yt://<videoId>` or `ytquery://<encodedQuery>`) into
 * fresh, direct `googlevideo.com` CDN stream URLs when ExoPlayer requests chunks.
 */
class YouTubeStreamResolver(
    private val upstreamFactory: DataSource.Factory,
    private val visionOsApi: VisionOsPlayerApi
) : ResolvingDataSource.Resolver {

    // Cache: videoId -> AudioStreamTrack
    private val resolvedTracks = ConcurrentHashMap<String, AudioStreamTrack>()
    // Cache: query -> videoId
    private val queryToVideoId = ConcurrentHashMap<String, String>()

    override fun resolveDataSpec(dataSpec: DataSpec): DataSpec {
        val uri = dataSpec.uri
        val scheme = uri.scheme

        if (scheme != "yt" && scheme != "ytquery") {
            return dataSpec
        }

        val videoId = when (scheme) {
            "yt" -> uri.host ?: uri.lastPathSegment ?: error("Invalid yt URI: $uri")
            "ytquery" -> {
                val rawQuery = uri.schemeSpecificPart.removePrefix("//")
                val decodedQuery = URLDecoder.decode(rawQuery, "UTF-8")
                resolveQueryToVideoId(decodedQuery)
            }
            else -> error("Unsupported scheme: $scheme")
        }

        val track = getOrFetchTrack(videoId)
        return dataSpec.buildUpon()
            .setUri(Uri.parse(track.url))
            .setCustomData(track)
            .build()
    }

    fun getOrFetchTrack(videoId: String): AudioStreamTrack {
        val now = System.currentTimeMillis() / 1000
        val cached = resolvedTracks[videoId]
        if (cached != null && cached.expireAtEpochSec > (now + 120)) {
            return cached
        }

        val result = runBlocking {
            visionOsApi.fetchAudioStream(videoId)
        }

        val track = result.getOrElse { e ->
            throw IOException("Failed to resolve stream for $videoId: ${e.message}", e)
        }

        resolvedTracks[videoId] = track
        return track
    }

    private fun resolveQueryToVideoId(query: String): String {
        queryToVideoId[query]?.let { return it }

        val result = runBlocking {
            visionOsApi.searchFirstVideo(query)
        }

        val searchVideo = result.getOrElse { e ->
            throw IOException("Failed to search YouTube for '$query': ${e.message}", e)
        }

        queryToVideoId[query] = searchVideo.videoId
        return searchVideo.videoId
    }

    fun createFactory(): ResolvingDataSource.Factory {
        return ResolvingDataSource.Factory(upstreamFactory, this)
    }
}
