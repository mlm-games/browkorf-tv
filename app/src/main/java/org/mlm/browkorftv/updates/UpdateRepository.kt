package org.mlm.browkorftv.updates

import android.os.Build
import kotlinx.coroutines.CancellationException
import org.mlm.browkorftv.BuildConfig

sealed interface UpdateResult {
    data object NoUpdate : UpdateResult
    data class HasUpdate(val info: UpdateInfo) : UpdateResult
    data class Error(val throwable: Throwable) : UpdateResult
}

class UpdateRepository(
    private val api: UpdateApi
) {
    suspend fun checkForUpdates(
        currentVersionName: String,
        prerelease: Boolean,
        supportedAbis: List<String> = Build.SUPPORTED_ABIS.toList(),
        variantName: String = BuildConfig.FLAVOR
    ): UpdateResult {
        return try {
            val release = api.fetchLatestRelease(prerelease)
                ?: return UpdateResult.NoUpdate

            val bestAsset = selectBestAsset(release.assets, variantName, supportedAbis)
                ?: return UpdateResult.Error(
                    IllegalStateException("No compatible update asset for $variantName")
                )
            if (bestAsset.digest.isNullOrBlank()) {
                return UpdateResult.Error(
                    IllegalStateException("Update asset has no SHA-256 digest")
                )
            }

            val info = UpdateInfo(
                versionName = release.tagName,
                downloadUrl = bestAsset.downloadUrl,
                changelog = release.body,
                sha256 = bestAsset.digest
            )

            if (info.hasUpdate(currentVersionName)) {
                UpdateResult.HasUpdate(info)
            } else {
                UpdateResult.NoUpdate
            }
        } catch (t: CancellationException) {
            throw t
        } catch (t: Throwable) {
            UpdateResult.Error(t)
        }
    }

    private fun selectBestAsset(
        assets: List<GitHubAsset>,
        variantName: String,
        supportedAbis: List<String>
    ): GitHubAsset? {
        if (variantName.isBlank()) return null
        val variantMarker = variantName.replaceFirstChar { it.uppercase() }
        val apkAssets = assets.filter { it.name.endsWith(".apk", ignoreCase = true) }
        val variantAssets = apkAssets.filter { it.name.contains(variantMarker, ignoreCase = true) }
        if (variantAssets.isEmpty()) return null

        val abiSpecific = variantAssets.filterNot { it.isUniversal() }
        for (abi in supportedAbis) {
            abiSpecific.firstOrNull { it.matchesAbi(abi) }?.let { return it }
        }
        return variantAssets.firstOrNull { it.isUniversal() }
    }

    private fun GitHubAsset.isUniversal(): Boolean {
        return name.substringBeforeLast('.').endsWith("-universal", ignoreCase = true)
    }

    private fun GitHubAsset.matchesAbi(abi: String): Boolean {
        return name.substringBeforeLast('.').endsWith("-$abi", ignoreCase = true)
    }
}
