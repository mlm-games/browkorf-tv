package org.mlm.browkorftv.updates

import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import org.mlm.browkorftv.core.DispatcherProvider
import org.mlm.browkorftv.utils.Utils
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

data class DownloadProgress(
    val bytesRead: Long,
    val totalBytes: Long?,
    val percent: Int?,          // null if unknown
    val done: Boolean = false
)

class UpdateInstaller(
    private val context: Context,
    private val dispatchers: DispatcherProvider
) {
    companion object {
        private const val UPDATE_APK_FILE_NAME = "update.apk"

        fun clearTempFilesIfAny(context: Context) {
            runCatching { File(context.cacheDir, UPDATE_APK_FILE_NAME).delete() }
            runCatching { File(context.externalCacheDir, UPDATE_APK_FILE_NAME).delete() }
        }
    }

    /**
     * Downloads update.apk into a temp cache file (best cache dir picked by Utils.createTempFile).
     * Emits progress updates; final emission has done=true.
     */
    fun downloadApk(url: String): Flow<Pair<DownloadProgress, File?>> = flow {
        val outFile = Utils.createTempFile(context, UPDATE_APK_FILE_NAME)
        emit(Pair(DownloadProgress(bytesRead = 0, totalBytes = null, percent = null), null))

        val resultFile = withContext(dispatchers.io) {
            val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                connectTimeout = 20_000
                readTimeout = 20_000
                useCaches = false
                instanceFollowRedirects = true
            }

            try {
                conn.connect()
                if (conn.responseCode != HttpURLConnection.HTTP_OK) {
                    throw IllegalStateException("HTTP ${conn.responseCode} ${conn.responseMessage}")
                }

                val total = conn.contentLengthLong.takeIf { it > 0 }
                var readTotal = 0L

                conn.inputStream.use { input ->
                    outFile.outputStream().use { output ->
                        val buf = ByteArray(64 * 1024)
                        var lastEmitTime = 0L

                        while (true) {
                            val n = input.read(buf)
                            if (n <= 0) break
                            output.write(buf, 0, n)
                            readTotal += n

                            val now = System.currentTimeMillis()
                            if (now - lastEmitTime > 100) {
                                val percent = total?.let { ((readTotal * 100) / it).toInt().coerceIn(0, 100) }
                                emit(Pair(DownloadProgress(readTotal, total, percent, done = false), null))
                                lastEmitTime = now
                            }
                        }
                    }
                }

                outFile
            } finally {
                conn.disconnect()
            }
        }

        emit(
            Pair(DownloadProgress(
                bytesRead = resultFile.length(),
                totalBytes = resultFile.length(),
                percent = 100,
                done = true
            ),
            resultFile
        ))
    }
}