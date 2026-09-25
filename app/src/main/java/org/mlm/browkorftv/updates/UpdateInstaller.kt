package org.mlm.browkorftv.updates

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.Signature
import android.os.Build
import android.os.SystemClock
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.DisposableHandle
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.mlm.browkorftv.BuildConfig
import org.mlm.browkorftv.core.DispatcherProvider
import org.mlm.browkorftv.network.ProxyManager
import org.mlm.browkorftv.utils.Utils
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.UUID
import java.util.concurrent.atomic.AtomicReference

data class DownloadProgress(
    val bytesRead: Long,
    val totalBytes: Long?,
    val percent: Int?,
    val done: Boolean = false
)

sealed interface UpdateDownloadEvent {
    data class Progress(val progress: DownloadProgress) : UpdateDownloadEvent
    data class Completed(val file: File, val progress: DownloadProgress) : UpdateDownloadEvent
}

class UpdateInstaller(
    private val context: Context,
    private val dispatchers: DispatcherProvider
) {
    private val pendingMarkerMutex = Mutex()

    companion object {
        private const val UPDATE_APK_FILE_NAME = "update.apk"
        private const val PENDING_UPDATE_FILE_NAME = ".pending_update_path"
        private const val MAX_UPDATE_BYTES = 1024L * 1024L * 1024L
        private const val STALE_UPDATE_AGE_MS = 24L * 60L * 60L * 1000L

        fun clearTempFilesIfAny(context: Context) {
            val marker = File(context.filesDir, PENDING_UPDATE_FILE_NAME)
            val pendingPath = if (marker.isFile) runCatching { marker.readText() }.getOrNull() else null
            cleanupDirectory(context.cacheDir, pendingPath)
            context.externalCacheDir?.let { cleanupDirectory(it, pendingPath) }
        }

        private fun cleanupDirectory(directory: File, pendingPath: String?) {
            val now = System.currentTimeMillis()
            directory.listFiles()
                ?.filter { file ->
                    val isStalePartial = file.name.startsWith("update-") &&
                        file.name.endsWith(".part.apk") &&
                        now - file.lastModified() > STALE_UPDATE_AGE_MS
                    val isStaleApk = file.name.startsWith("update-") &&
                        file.name.endsWith(".apk") &&
                        now - file.lastModified() > STALE_UPDATE_AGE_MS
                    file.absolutePath != pendingPath &&
                        (file.name == UPDATE_APK_FILE_NAME || isStalePartial || isStaleApk)
                }
                ?.forEach { it.delete() }
        }
    }

    suspend fun persistPendingUpdate(file: File) {
        withContext(dispatchers.io) {
            pendingMarkerMutex.withLock {
                val marker = File(context.filesDir, PENDING_UPDATE_FILE_NAME)
                val temporaryMarker = File(context.filesDir, "$PENDING_UPDATE_FILE_NAME.tmp")
                marker.parentFile?.mkdirs()
                temporaryMarker.writeText(file.absolutePath)
                if (marker.exists() && !marker.delete()) {
                    temporaryMarker.delete()
                    throw IOException("Unable to replace pending update state")
                }
                if (!temporaryMarker.renameTo(marker)) {
                    temporaryMarker.delete()
                    throw IOException("Unable to persist pending update state")
                }
            }
        }
    }

    suspend fun clearPendingUpdate(file: File? = null) {
        withContext(dispatchers.io) {
            pendingMarkerMutex.withLock {
                val marker = File(context.filesDir, PENDING_UPDATE_FILE_NAME)
                File(context.filesDir, "$PENDING_UPDATE_FILE_NAME.tmp").delete()
                if (!marker.exists()) return@withLock
                if (file == null || marker.readText() == file.absolutePath) {
                    marker.delete()
                }
            }
        }
    }

    suspend fun recoverPendingUpdate(): File? {
        return withContext(dispatchers.io) {
            pendingMarkerMutex.withLock {
                val marker = File(context.filesDir, PENDING_UPDATE_FILE_NAME)
                File(context.filesDir, "$PENDING_UPDATE_FILE_NAME.tmp").delete()
                if (!marker.isFile) return@withLock null
                val file = File(marker.readText())
                if (!file.isFile) {
                    marker.delete()
                    return@withLock null
                }
                try {
                    validatePackage(file)
                    file
                } catch (t: CancellationException) {
                    throw t
                } catch (_: Throwable) {
                    marker.delete()
                    file.delete()
                    null
                }
            }
        }
    }

    fun downloadApk(url: String): Flow<UpdateDownloadEvent> {
        return downloadApk(UpdateInfo(versionName = "", downloadUrl = url, changelog = ""))
    }

    fun downloadApk(info: UpdateInfo): Flow<UpdateDownloadEvent> = flow {
        val directory = Utils.createTempFile(context, UPDATE_APK_FILE_NAME).parentFile
            ?: context.cacheDir
        if (!directory.exists() && !directory.mkdirs()) {
            throw IOException("Unable to create update cache directory")
        }

        val partialFile = File.createTempFile("update-", ".part.apk", directory)
        val resultFile = File(directory, "update-${UUID.randomUUID()}.apk")
        val connection = AtomicReference<HttpURLConnection?>(null)
        var completed = false
        var completionEmitted = false
        var expectedDigest: String? = null
        val cancellationHandle: DisposableHandle? = currentCoroutineContext()[Job]
            ?.invokeOnCompletion { connection.get()?.disconnect() }

        try {
            expectedDigest = normalizeExpectedDigest(info.sha256)
            emit(UpdateDownloadEvent.Progress(DownloadProgress(0L, null, null)))

            currentCoroutineContext().ensureActive()
            val url = URL(info.downloadUrl)
            require(url.protocol.equals("https", ignoreCase = true) &&
                url.host.equals("github.com", ignoreCase = true)
            ) {
                "Update URL must use HTTPS on github.com"
            }

            currentCoroutineContext().ensureActive()
            val activeConnection = ProxyManager.openConnection(url).apply {
                connectTimeout = 20_000
                readTimeout = 20_000
                useCaches = false
                instanceFollowRedirects = true
                setRequestProperty("User-Agent", "BrowkorfTV/${BuildConfig.VERSION_NAME}")
            }
            connection.set(activeConnection)
            currentCoroutineContext().ensureActive()
            activeConnection.connect()

            currentCoroutineContext().ensureActive()
            if (!activeConnection.url.protocol.equals("https", ignoreCase = true) ||
                !isAllowedUpdateHost(activeConnection.url.host)
            ) {
                throw IOException("Update redirect must use an allowed HTTPS host")
            }
            if (activeConnection.responseCode != HttpURLConnection.HTTP_OK) {
                throw IOException(
                    "HTTP ${activeConnection.responseCode} ${activeConnection.responseMessage.orEmpty()}"
                )
            }

            val total = activeConnection.contentLengthLong.takeIf { it >= 0L }
            if (total != null && total > MAX_UPDATE_BYTES) {
                throw IOException("Update is larger than the supported limit")
            }
            val digest = MessageDigest.getInstance("SHA-256")
            var readTotal = 0L

            activeConnection.inputStream.use { input ->
                partialFile.outputStream().use { output ->
                    val buffer = ByteArray(64 * 1024)
                    var lastEmitTime = 0L

                    while (true) {
                        currentCoroutineContext().ensureActive()
                        val count = input.read(buffer)
                        if (count <= 0) break
                        output.write(buffer, 0, count)
                        digest.update(buffer, 0, count)
                        readTotal += count
                        if (readTotal > MAX_UPDATE_BYTES) {
                            throw IOException("Update is larger than the supported limit")
                        }

                        val now = SystemClock.elapsedRealtime()
                        if (now - lastEmitTime > 100L) {
                            val percent = total?.let {
                                ((readTotal * 100L) / it).toInt().coerceIn(0, 100)
                            }
                            emit(
                                UpdateDownloadEvent.Progress(
                                    DownloadProgress(readTotal, total, percent)
                                )
                            )
                            lastEmitTime = now
                        }
                    }
                }
            }

            if (readTotal == 0L) {
                throw IOException("Downloaded update is empty")
            }
            if (total != null && readTotal != total) {
                throw IOException("Downloaded update is truncated: $readTotal of $total bytes")
            }

            if (expectedDigest != null && digest.digest().toHexString() != expectedDigest) {
                throw IOException("Downloaded update failed SHA-256 verification")
            }

            validatePackage(partialFile, info)

            if (resultFile.exists() && !resultFile.delete()) {
                throw IOException("Unable to replace the previous update file")
            }
            if (!partialFile.renameTo(resultFile)) {
                throw IOException("Unable to finalize the downloaded update")
            }

            completed = true
            val resultSize = resultFile.length()
            emit(
                UpdateDownloadEvent.Completed(
                    file = resultFile,
                    progress = DownloadProgress(
                        bytesRead = resultSize,
                        totalBytes = total ?: resultSize,
                        percent = 100,
                        done = true
                    )
                )
            )
            completionEmitted = true
        } finally {
            cancellationHandle?.dispose()
            connection.getAndSet(null)?.disconnect()
            if (!completed || !completionEmitted) {
                partialFile.delete()
                resultFile.delete()
            }
        }
    }.flowOn(dispatchers.io)

    private fun isAllowedUpdateHost(host: String): Boolean {
        return host.equals("github.com", ignoreCase = true) ||
            host.endsWith(".githubusercontent.com", ignoreCase = true)
    }

    private fun normalizeExpectedDigest(value: String?): String? {
        val trimmed = value?.trim().orEmpty()
        if (trimmed.isEmpty()) return null

        val digest = if (trimmed.contains(':')) {
            val separator = trimmed.indexOf(':')
            val algorithm = trimmed.substring(0, separator)
            require(algorithm.equals("sha256", ignoreCase = true)) {
                "Unsupported update digest algorithm: $algorithm"
            }
            trimmed.substring(separator + 1)
        } else {
            trimmed
        }
        require(digest.length == 64 && digest.all { it.isDigit() || it.lowercaseChar() in 'a'..'f' }) {
            "Invalid SHA-256 digest"
        }
        return digest.lowercase()
    }

    private fun validatePackage(file: File, info: UpdateInfo? = null) {
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            PackageManager.GET_SIGNING_CERTIFICATES
        } else {
            @Suppress("DEPRECATION")
            PackageManager.GET_SIGNATURES
        }
        val archiveInfo = context.packageManager.getPackageArchiveInfo(file.absolutePath, flags)
            ?: throw IOException("Downloaded update is not a valid APK")
        if (archiveInfo.packageName != context.packageName) {
            throw IOException("Downloaded APK has an unexpected package name")
        }
        if (info != null && info.versionName.isNotBlank() &&
            !info.matchesVersion(archiveInfo.versionName.orEmpty())
        ) {
            throw IOException("Downloaded APK version does not match the release")
        }
        if (versionCode(archiveInfo) <= BuildConfig.VERSION_CODE) {
            throw IOException("Downloaded APK is not newer than the installed app")
        }

        val installedInfo = try {
            context.packageManager.getPackageInfo(context.packageName, flags)
        } catch (e: PackageManager.NameNotFoundException) {
            throw IOException("Unable to inspect the installed app", e)
        }
        if (!hasCompatibleSigners(installedInfo, archiveInfo)) {
            throw IOException("Downloaded APK has incompatible signing certificates")
        }
    }

    @Suppress("DEPRECATION")
    private fun versionCode(info: PackageInfo): Long {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            info.longVersionCode
        } else {
            info.versionCode.toLong()
        }
    }

    @Suppress("DEPRECATION")
    private fun hasCompatibleSigners(installed: PackageInfo, candidate: PackageInfo): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            val installedSigners = installed.signatures?.toList().orEmpty()
            val candidateSigners = candidate.signatures?.toList().orEmpty()
            return sameSignerSet(installedSigners, candidateSigners)
        }

        val installedSigning = installed.signingInfo ?: return false
        val candidateSigning = candidate.signingInfo ?: return false
        if (installedSigning.hasMultipleSigners() || candidateSigning.hasMultipleSigners()) {
            return sameSignerSet(
                installedSigning.apkContentsSigners.toList(),
                candidateSigning.apkContentsSigners.toList()
            )
        }

        val installedCurrent = installedSigning.apkContentsSigners.toList()
        val candidateCurrent = candidateSigning.apkContentsSigners.toList()
        if (sameSignerSet(installedCurrent, candidateCurrent)) return true

        val candidateHistory = candidateSigning.signingCertificateHistory.orEmpty().toList()
        return candidateCurrent.isNotEmpty() && candidateHistory.isNotEmpty() &&
            installedCurrent.any { installedSigner ->
                candidateHistory.any { historySigner ->
                    sameSignature(installedSigner, historySigner)
                }
            }
    }

    @Suppress("DEPRECATION")
    private fun sameSignerSet(first: List<Signature>, second: List<Signature>): Boolean {
        return first.size == second.size && first.all { signer ->
            second.any { other -> sameSignature(signer, other) }
        }
    }

    private fun sameSignature(first: Signature, second: Signature): Boolean {
        return first.toByteArray().contentEquals(second.toByteArray())
    }

    private fun ByteArray.toHexString(): String {
        val characters = CharArray(size * 2)
        val digits = "0123456789abcdef"
        forEachIndexed { index, byte ->
            val value = byte.toInt() and 0xff
            characters[index * 2] = digits[value ushr 4]
            characters[index * 2 + 1] = digits[value and 0x0f]
        }
        return String(characters)
    }
}
