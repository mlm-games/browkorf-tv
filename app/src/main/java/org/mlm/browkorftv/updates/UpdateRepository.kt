package org.mlm.browkorftv.updates

import android.os.Build

sealed interface UpdateResult {
    data object NoUpdate : UpdateResult
    data class HasUpdate(val info: UpdateInfo) : UpdateResult
    data class Error(val throwable: Throwable) : UpdateResult
}

class UpdateRepository(
    private val api: UpdateApi
) {
    suspend fun checkForUpdates(
        manifestUrl: String,
        currentVersionCode: Int,
        channelsToCheck: List<String>,
        deviceApi: Int = Build.VERSION.SDK_INT,
        supportedAbis: List<String> = Build.SUPPORTED_ABIS.toList()
    ): UpdateResult {
        return try {
            val manifest = api.fetchManifest(manifestUrl)

            val availableChannels = manifest.channels.map { it.name }.distinct()

            // Pick "best" channel among channelsToCheck: highest latestVersionCode, and minApi satisfied.
            var chosen: UpdateChannel? = null
            for (channel in manifest.channels) {
                if (channel.name !in channelsToCheck) continue
                if (deviceApi < channel.minApi) continue
                if (chosen == null || channel.latestVersionCode > chosen!!.latestVersionCode) {
                    chosen = channel
                }
            }

            val ch = chosen ?: return UpdateResult.NoUpdate

            val bestUrl = selectBestUrlForAbi(ch, supportedAbis)

            val info = UpdateInfo(
                latestVersionCode = ch.latestVersionCode,
                latestVersionName = ch.latestVersionName,
                channel = ch.name,
                downloadUrl = bestUrl,
                changelog = manifest.changelog,
                availableChannels = availableChannels
            )

            if (info.hasUpdate(currentVersionCode)) {
                UpdateResult.HasUpdate(info)
            } else {
                UpdateResult.NoUpdate
            }
        } catch (t: Throwable) {
            UpdateResult.Error(t)
        }
    }

    private fun selectBestUrlForAbi(channel: UpdateChannel, supportedAbis: List<String>): String {
        if (channel.urls.isEmpty()) return channel.url

        // Your JSON uses APK names ending in e.g. "...arm64-v8a.apk"
        val primaryAbi = supportedAbis.firstOrNull().orEmpty()
        val abiMatch = channel.urls.firstOrNull { it.endsWith("$primaryAbi.apk", ignoreCase = true) }
        return abiMatch ?: channel.url
    }
}