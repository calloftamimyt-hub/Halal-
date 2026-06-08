package com.example

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

@OptIn(UnstableApi::class)
object VideoCacheManager {
    @Volatile
    private var simpleCache: SimpleCache? = null
    private const val CACHE_SIZE = 100 * 1024 * 1024L // 100 MB cache size

    // Thread-safe asynchronous cache initializer that runs on background thread (IO)
    fun initCacheAsync(context: Context) {
        if (simpleCache != null) return
        CoroutineScope(Dispatchers.IO).launch {
            try {
                synchronized(this@VideoCacheManager) {
                    if (simpleCache == null) {
                        val cacheDir = File(context.applicationContext.cacheDir, "video_cache")
                        if (!cacheDir.exists()) {
                            cacheDir.mkdirs()
                        }
                        val evictor = LeastRecentlyUsedCacheEvictor(CACHE_SIZE)
                        val databaseProvider = StandaloneDatabaseProvider(context.applicationContext)
                        simpleCache = SimpleCache(cacheDir, evictor, databaseProvider)
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("VideoCacheManager", "Failed to initialize SimpleCache asynchronously", e)
            }
        }
    }

    // Instantly returns DataSource.Factory. If cache is not ready, falls back to normal streamer & triggers async init.
    fun getCacheDataSourceFactory(context: Context): DataSource.Factory {
        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            .setAllowCrossProtocolRedirects(true)
        val defaultDataSourceFactory = DefaultDataSource.Factory(context, httpDataSourceFactory)
        
        val cache = simpleCache
        if (cache == null) {
            // Trigger asynchronous initialization on first request
            initCacheAsync(context.applicationContext)
            return defaultDataSourceFactory
        }
        
        return try {
            CacheDataSource.Factory()
                .setCache(cache)
                .setUpstreamDataSourceFactory(defaultDataSourceFactory)
                .setCacheWriteDataSinkFactory(null) // Keep as null, cache is populated by reading
                .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
        } catch (e: Exception) {
            android.util.Log.e("VideoCacheManager", "Error creating CacheDataSource.Factory, using default", e)
            defaultDataSourceFactory
        }
    }

    // Prefetches video segment on background thread
    fun prefetchVideo(context: Context, videoUrl: String) {
        if (videoUrl.isEmpty()) return
        
        val cache = simpleCache
        if (cache == null) {
            initCacheAsync(context.applicationContext)
            return
        }
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val dataSource = DefaultHttpDataSource.Factory()
                    .setAllowCrossProtocolRedirects(true)
                    .createDataSource()
                // Use a modest head-chunk of 256KB to keep prefetch speedy and efficient
                val dataSpec = androidx.media3.datasource.DataSpec(android.net.Uri.parse(videoUrl), 0, 256 * 1024L)
                androidx.media3.datasource.cache.CacheWriter(
                    androidx.media3.datasource.cache.CacheDataSource(cache, dataSource),
                    dataSpec,
                    null,
                    null
                ).cache()
            } catch (e: Exception) {
                android.util.Log.e("VideoCache", "Prefetch failed for $videoUrl", e)
            }
        }
    }
}
