package com.phlox.tvwebbrowser.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

class ApkDownloader {
    suspend fun download(url: String, targetFile: File, onProgress: (Int) -> Unit) = withContext(Dispatchers.IO) {
        val conn = (URL(url).openConnection() as HttpURLConnection)
        conn.connectTimeout = 20_000
        conn.readTimeout = 20_000
        conn.connect()

        try {
            if (conn.responseCode != HttpURLConnection.HTTP_OK) {
                throw IllegalStateException("HTTP ${conn.responseCode} ${conn.responseMessage}")
            }

            val len = conn.contentLengthLong
            targetFile.outputStream().use { out ->
                conn.inputStream.use { input ->
                    val buf = ByteArray(8 * 1024)
                    var total = 0L
                    while (true) {
                        val r = input.read(buf)
                        if (r <= 0) break
                        out.write(buf, 0, r)
                        total += r
                        if (len > 0) {
                            val p = ((total * 100) / len).toInt().coerceIn(0, 100)
                            onProgress(p)
                        }
                    }
                }
            }
        } finally {
            conn.disconnect()
        }
    }
}