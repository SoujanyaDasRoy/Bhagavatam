package com.bhagavatam.app.audio.stream

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

data class AudioStreamTrack(
    val videoId: String,
    val url: String,
    val itag: Int,
    val mimeType: String,
    val bitrate: Int,
    val contentLength: Long,
    val audioQuality: String?,
    val approxDurationMs: Long,
    val expireAtEpochSec: Long
)

data class SearchResultVideo(
    val videoId: String,
    val title: String,
    val channelTitle: String,
    val durationText: String?
)

/**
 * Direct InnerTube Player & Search API using the VISIONOS client context.
 * Bypasses YouTube frontend ad-injection layers and JS cipher requirements,
 * yielding pure unthrottled audio streams (Opus 160kbps / AAC 128kbps).
 */
class VisionOsPlayerApi(
    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()
) {
    companion object {
        private const val INNERTUBE_PLAYER_URL = "https://www.youtube.com/youtubei/v1/player?prettyPrint=false"
        private const val INNERTUBE_SEARCH_URL = "https://www.youtube.com/youtubei/v1/search?prettyPrint=false"
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
        private const val VISIONOS_USER_AGENT = "Mozilla/5.0 (Macintosh; Apple Vision Pro 1.0) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.0 Mobile/15E148 Safari/604.1"
    }

    /**
     * Searches YouTube via InnerTube endpoint and returns the first matching video.
     */
    suspend fun searchFirstVideo(query: String): Result<SearchResultVideo> = withContext(Dispatchers.IO) {
        try {
            val payload = JSONObject().apply {
                put("context", JSONObject().apply {
                    put("client", JSONObject().apply {
                        put("clientName", "WEB")
                        put("clientVersion", "2.20240101.00.00")
                        put("hl", "en")
                        put("gl", "IN")
                    })
                })
                put("query", query)
            }

            val request = Request.Builder()
                .url(INNERTUBE_SEARCH_URL)
                .addHeader("Content-Type", "application/json")
                .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .post(payload.toString().toRequestBody(JSON_MEDIA_TYPE))
                .build()

            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext Result.failure(IOException("InnerTube search HTTP error ${response.code}"))
            }

            val responseBody = response.body?.string() ?: return@withContext Result.failure(IOException("Empty search response"))
            val json = JSONObject(responseBody)

            val sectionListRenderer = json.optJSONObject("contents")
                ?.optJSONObject("twoColumnSearchResultsRenderer")
                ?.optJSONObject("primaryContents")
                ?.optJSONObject("sectionListRenderer")
                ?: return@withContext Result.failure(IOException("Invalid search result structure"))

            val candidates = mutableListOf<SearchResultVideo>()
            val contents = sectionListRenderer.optJSONArray("contents")
            if (contents != null && contents.length() > 0) {
                for (i in 0 until contents.length()) {
                    val itemSection = contents.getJSONObject(i).optJSONObject("itemSectionRenderer") ?: continue
                    val itemContents = itemSection.optJSONArray("contents") ?: continue
                    for (j in 0 until itemContents.length()) {
                        val videoRenderer = itemContents.getJSONObject(j).optJSONObject("videoRenderer") ?: continue
                        val videoId = videoRenderer.optString("videoId")
                        if (videoId.isNotEmpty()) {
                            val title = videoRenderer.optJSONObject("title")
                                ?.optJSONArray("runs")
                                ?.optJSONObject(0)
                                ?.optString("text") ?: query

                            val channel = videoRenderer.optJSONObject("ownerText")
                                ?.optJSONArray("runs")
                                ?.optJSONObject(0)
                                ?.optString("text") ?: "YouTube"

                            val lengthText = videoRenderer.optJSONObject("lengthText")?.optString("simpleText")
                            val viewCount = videoRenderer.optJSONObject("viewCountText")?.optString("simpleText") ?: ""

                            // Skip shorts / teasers (< 1 minute)
                            val isShort = lengthText?.let {
                                val parts = it.split(":")
                                parts.size == 1 || (parts.size == 2 && (parts[0].toIntOrNull() ?: 0) < 1)
                            } ?: false

                            if (!isShort) {
                                candidates.add(
                                    SearchResultVideo(
                                        videoId = videoId,
                                        title = title,
                                        channelTitle = channel,
                                        durationText = if (viewCount.isNotEmpty()) "$channel • $viewCount" else channel
                                    )
                                )
                            }
                        }
                    }
                }
            }

            if (candidates.isNotEmpty()) {
                // Return top scored candidate
                return@withContext Result.success(candidates.first())
            }

            Result.failure(IOException("No matching video found for query: $query"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Fetches the direct audio stream URL for a given YouTube video ID.
     */
    suspend fun fetchAudioStream(videoId: String): Result<AudioStreamTrack> = withContext(Dispatchers.IO) {
        try {
            val payload = JSONObject().apply {
                put("context", JSONObject().apply {
                    put("client", JSONObject().apply {
                        put("clientName", "VISIONOS")
                        put("clientVersion", "0.1.0")
                        put("hl", "en")
                        put("gl", "US")
                        put("deviceMake", "Apple")
                        put("deviceModel", "Vision Pro")
                        put("osName", "visionOS")
                        put("osVersion", "1.0")
                    })
                })
                put("videoId", videoId)
                put("contentCheckOk", true)
                put("racyCheckOk", true)
            }

            val request = Request.Builder()
                .url(INNERTUBE_PLAYER_URL)
                .addHeader("Content-Type", "application/json")
                .addHeader("User-Agent", VISIONOS_USER_AGENT)
                .post(payload.toString().toRequestBody(JSON_MEDIA_TYPE))
                .build()

            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext Result.failure(IOException("InnerTube /player HTTP error ${response.code}"))
            }

            val responseBody = response.body?.string() ?: return@withContext Result.failure(IOException("Empty /player response"))
            val json = JSONObject(responseBody)

            val playabilityStatus = json.optJSONObject("playabilityStatus")
            val status = playabilityStatus?.optString("status")
            if (status != null && status != "OK") {
                val reason = playabilityStatus.optString("reason", "Video unavailable ($status)")
                return@withContext Result.failure(IOException(reason))
            }

            val streamingData = json.optJSONObject("streamingData")
                ?: return@withContext Result.failure(IOException("No streaming data available"))

            val adaptiveFormats = streamingData.optJSONArray("adaptiveFormats")
                ?: return@withContext Result.failure(IOException("No adaptive formats available"))

            val audioTracks = mutableListOf<AudioStreamTrack>()
            val approxDurationMs = json.optJSONObject("videoDetails")?.optLong("lengthSeconds", 0L)?.times(1000L) ?: 0L

            for (i in 0 until adaptiveFormats.length()) {
                val format = adaptiveFormats.getJSONObject(i)
                val mimeType = format.optString("mimeType")
                if (mimeType.startsWith("audio/")) {
                    val streamUrl = format.optString("url")
                    if (streamUrl.isNotEmpty()) {
                        val expireSec = extractExpiry(streamUrl) ?: ((System.currentTimeMillis() / 1000) + 5 * 3600)
                        audioTracks.add(
                            AudioStreamTrack(
                                videoId = videoId,
                                url = streamUrl,
                                itag = format.optInt("itag"),
                                mimeType = mimeType,
                                bitrate = format.optInt("bitrate"),
                                contentLength = format.optLong("contentLength", -1L),
                                audioQuality = format.optString("audioQuality"),
                                approxDurationMs = approxDurationMs,
                                expireAtEpochSec = expireSec
                            )
                        )
                    }
                }
            }

            // Priority: Opus 160kbps (itag 251) > AAC 128kbps (itag 140) > Opus 70kbps (itag 250) > Highest bitrate
            val bestTrack = audioTracks.find { it.itag == 251 }
                ?: audioTracks.find { it.itag == 140 }
                ?: audioTracks.find { it.itag == 250 }
                ?: audioTracks.maxByOrNull { it.bitrate }
                ?: return@withContext Result.failure(IOException("No playable audio tracks found"))

            // Verify tail with HTTP Range to avoid 403 mid-stream drop
            if (verifyTailRange(bestTrack)) {
                Result.success(bestTrack)
            } else {
                // If tail verification failed on best, try secondary candidate
                val secondaryTrack = audioTracks.find { it.itag == 140 && it != bestTrack }
                if (secondaryTrack != null && verifyTailRange(secondaryTrack)) {
                    Result.success(secondaryTrack)
                } else {
                    Result.success(bestTrack) // Return track anyway if network verification is ambiguous
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun verifyTailRange(track: AudioStreamTrack): Boolean {
        if (track.contentLength <= 2000L) return true
        return try {
            val rangeHeader = "bytes=${track.contentLength - 2000}-${track.contentLength - 1}"
            val verifyRequest = Request.Builder()
                .url(track.url)
                .addHeader("Range", rangeHeader)
                .addHeader("User-Agent", VISIONOS_USER_AGENT)
                .head()
                .build()

            httpClient.newCall(verifyRequest).execute().use { res ->
                res.code == 206 || res.code == 200
            }
        } catch (_: Exception) {
            true // Don't block playback if range probe encounters network blip
        }
    }

    private fun extractExpiry(url: String): Long? {
        val query = url.substringAfter('?', "")
        if (query.isEmpty()) return null
        for (param in query.split("&")) {
            val parts = param.split("=")
            if (parts.size == 2 && parts[0] == "expire") {
                return parts[1].toLongOrNull()
            }
        }
        return null
    }
}
