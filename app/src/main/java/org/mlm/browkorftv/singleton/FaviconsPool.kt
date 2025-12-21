package org.mlm.browkorftv.singleton

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import android.util.LruCache
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.mlm.browkorftv.core.DispatcherProvider
import org.mlm.browkorftv.model.HostConfig
import org.mlm.browkorftv.model.dao.HostsDao
import org.mlm.browkorftv.utils.FaviconExtractor
import java.io.File
import java.net.URL
import kotlin.math.abs

object FaviconsPool : KoinComponent {

    private val context: Context by inject()
    private val hostsDao: HostsDao by inject()
    private val dispatchers: DispatcherProvider by inject()

    const val FAVICONS_DIR = "favicons"
    const val FAVICON_PREFERRED_SIDE_SIZE = 120
    private val TAG: String = FaviconsPool::class.java.simpleName

    private val extractor = FaviconExtractor()

    private val cache: LruCache<String, Bitmap> =
        object : LruCache<String, Bitmap>(10 * 1024 * 1024) {
            override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount
        }

    private val inFlightMutex = Mutex()
    private val inFlight = mutableMapOf<String, Deferred<Bitmap?>>()

    private val scope by lazy { CoroutineScope(SupervisorJob() + dispatchers.io) }

    suspend fun get(urlOrHost: String): Bitmap? {
        val normalizedUrl = normalizeUrlOrHost(urlOrHost) ?: return null
        val host = runCatching { URL(normalizedUrl).host }.getOrNull().orEmpty()
        if (host.isBlank()) return null

        cache.get(host)?.let { return it }

        return inFlightMutex.withLock {
            inFlight[host]?.let { return@withLock it }

            val job = scope.async {
                try {
                    // 1) DB/disk hit?
                    val hostConfig = hostsDao.findByHostName(host)
                    loadFromDisk(host, hostConfig)?.let { bmp ->
                        cache.put(host, bmp)
                        return@async bmp
                    }

                    // 2) Network fetch icons
                    val icons = runCatching { extractor.extractFavIconsFromURL(URL(normalizedUrl)) }.getOrNull()
                        ?: emptyList()

                    val chosen = chooseNearestSizeIcon(
                        icons,
                        FAVICON_PREFERRED_SIDE_SIZE,
                        FAVICON_PREFERRED_SIDE_SIZE
                    ) ?: icons.firstOrNull()

                    val bitmap = if (chosen != null) {
                        runCatching { downloadIcon(chosen) }.getOrNull()
                    } else null

                    if (bitmap != null) {
                        cache.put(host, bitmap)
                        saveToDiskAndDb(host, bitmap, hostConfig)
                    }
                    bitmap
                } catch (t: Throwable) {
                    Log.w(TAG, "Failed to load favicon for host=$host url=$normalizedUrl", t)
                    null
                } finally {
                    inFlightMutex.withLock { inFlight.remove(host) }
                }
            }

            inFlight[host] = job
            job
        }.await()
    }

    fun clear() {
        cache.evictAll()
    }

    private fun normalizeUrlOrHost(s: String): String? {
        val t = s.trim()
        if (t.isBlank()) return null
        if (t.startsWith("http://", true) || t.startsWith("https://", true)) return t
        if (t.contains("://")) return null

        // Prefer https first
        return "https://$t"
    }

    private fun favIconsDirFile(): File =
        File(context.cacheDir.absolutePath + File.separator + FAVICONS_DIR)

    private suspend fun loadFromDisk(host: String, hostConfig: HostConfig?): Bitmap? = withContext(dispatchers.io) {
        val cfg = hostConfig ?: return@withContext null
        val name = cfg.favicon ?: return@withContext null

        val dir = favIconsDirFile()
        if (!dir.exists() && !dir.mkdir()) return@withContext null

        val file = File(dir, name)
        if (!file.exists()) return@withContext null
        BitmapFactory.decodeFile(file.absolutePath)
    }

    private suspend fun saveToDiskAndDb(host: String, bitmap: Bitmap, hostConfig: HostConfig?) = withContext(dispatchers.io) {
        val dir = favIconsDirFile()
        if (!dir.exists() && !dir.mkdir()) return@withContext

        val filename = "${host.hashCode()}.png"
        val file = File(dir, filename)
        runCatching { if (file.exists()) file.delete() }
        runCatching { file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) } }

        if (hostConfig != null) {
            hostConfig.favicon = filename
            hostsDao.update(hostConfig)
        } else {
            val newCfg = HostConfig(host).apply { favicon = filename }
            hostsDao.insert(newCfg)
        }
    }

    private suspend fun downloadIcon(iconInfo: FaviconExtractor.IconInfo): Bitmap? = withContext(dispatchers.io) {
        val url = URL(iconInfo.src)
        val conn = url.openConnection().apply {
            connectTimeout = 15_000
            readTimeout = 15_000
        }

        // Decode bounds to scale down
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        conn.getInputStream().use { BitmapFactory.decodeStream(it, null, bounds) }

        val w = bounds.outWidth.coerceAtLeast(1)
        val h = bounds.outHeight.coerceAtLeast(1)

        // target max ~512px
        val scale = (w / 512).coerceAtLeast(h / 512).coerceAtLeast(1)

        val opts = BitmapFactory.Options().apply {
            inJustDecodeBounds = false
            inSampleSize = scale
        }

        url.openConnection().getInputStream().use { BitmapFactory.decodeStream(it, null, opts) }
    }

    private fun chooseNearestSizeIcon(
        icons: List<FaviconExtractor.IconInfo>,
        w: Int,
        h: Int
    ): FaviconExtractor.IconInfo? {
        var nearest: FaviconExtractor.IconInfo? = null
        var nearestDiff = Int.MAX_VALUE

        for (icon in icons) {
            val diff = abs(icon.width - w) + abs(icon.height - h)
            if (diff < nearestDiff) {
                nearestDiff = diff
                nearest = icon
            }
        }
        return nearest
    }
}