package com.bhagavatam.app.audio.stream

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import okhttp3.OkHttpClient
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * LRU Disk Cache for media chunks. Replaying any chapter consumes 0 bytes and plays instantly offline.
 */
@OptIn(UnstableApi::class)
object YouTubeMediaCache {
    private const val MAX_CACHE_BYTES = 100L * 1024L * 1024L // 100 MB
    private var simpleCacheInstance: SimpleCache? = null
    private var resolverInstance: YouTubeStreamResolver? = null

    val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .build()
    }

    val visionOsApi: VisionOsPlayerApi by lazy {
        VisionOsPlayerApi(okHttpClient)
    }

    @Synchronized
    fun getSimpleCache(context: Context): SimpleCache {
        return simpleCacheInstance ?: run {
            val cacheDir = File(context.cacheDir, "yt_audio_stream_cache")
            if (!cacheDir.exists()) cacheDir.mkdirs()
            val databaseProvider = StandaloneDatabaseProvider(context)
            val evictor = LeastRecentlyUsedCacheEvictor(MAX_CACHE_BYTES)
            SimpleCache(cacheDir, evictor, databaseProvider).also {
                simpleCacheInstance = it
            }
        }
    }

    @Synchronized
    fun getResolver(context: Context): YouTubeStreamResolver {
        return resolverInstance ?: run {
            val httpDataSourceFactory = DefaultHttpDataSource.Factory()
                .setUserAgent("Mozilla/5.0 (Macintosh; Apple Vision Pro 1.0) AppleWebKit/605.1.15")
                .setConnectTimeoutMs(15000)
                .setReadTimeoutMs(20000)
                .setAllowCrossProtocolRedirects(true)

            YouTubeStreamResolver(httpDataSourceFactory, visionOsApi).also {
                resolverInstance = it
            }
        }
    }

    fun createCacheDataSourceFactory(context: Context): DataSource.Factory {
        val cache = getSimpleCache(context)
        val resolver = getResolver(context)
        val resolvingFactory = resolver.createFactory()

        return CacheDataSource.Factory()
            .setCache(cache)
            .setUpstreamDataSourceFactory(resolvingFactory)
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
    }

    fun release() {
        try {
            simpleCacheInstance?.release()
            simpleCacheInstance = null
            resolverInstance = null
        } catch (_: Exception) {}
    }
}
