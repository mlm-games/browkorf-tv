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
    companion object {
        private const val GITHUB_RELEASES_URL = "https://api.github.com/repos/mlm-games/browkorf-tv/releases"
    }

    suspend fun checkForUpdates(
        currentVersionName: String,
        prerelease: Boolean,
        supportedAbis: List<String> = Build.SUPPORTED_ABIS.toList()
    ): UpdateResult {
        return try {
            val release = api.fetchLatestRelease(prerelease)
                ?: return UpdateResult.NoUpdate

            val bestAsset = selectBestAsset(release.assets, supportedAbis)
                ?: return UpdateResult.NoUpdate

            val info = UpdateInfo(
                versionName = release.tagName.removePrefix("v"),
                downloadUrl = bestAsset.downloadUrl,
                changelog = release.body
            )

            if (info.hasUpdate(currentVersionName)) {
                UpdateResult.HasUpdate(info)
            } else {
                UpdateResult.NoUpdate
            }
        } catch (t: Throwable) {
            UpdateResult.Error(t)
        }
    }

    private fun selectBestAsset(assets: List<GitHubAsset>, supportedAbis: List<String>): GitHubAsset? {
        val apkAssets = assets.filter { it.name.endsWith(".apk") }
        if (apkAssets.isEmpty()) return null

        val abiSpecific = apkAssets.filterNot { it.name.contains("-universal") }
        for (abi in supportedAbis) {
            abiSpecific.firstOrNull { it.name.contains(abi) }?.let { return it }
        }
        return abiSpecific.firstOrNull() ?: apkAssets.first()
    }
}
