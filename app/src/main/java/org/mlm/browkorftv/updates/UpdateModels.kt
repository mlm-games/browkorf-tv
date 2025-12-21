package org.mlm.browkorftv.updates

data class UpdateChannel(
    val name: String,
    val latestVersionName: String,
    val latestVersionCode: Int,
    val minApi: Int = 21,
    val url: String,
    val urls: List<String> = emptyList()
)

data class UpdateChangelogEntry(
    val versionCode: Int,
    val versionName: String,
    val changes: String
)

data class UpdateManifest(
    val channels: List<UpdateChannel>,
    val changelog: List<UpdateChangelogEntry>
)

/** Result of selecting the best update for this device + requested channels. */
data class UpdateInfo(
    val latestVersionCode: Int,
    val latestVersionName: String,
    val channel: String,
    val downloadUrl: String,
    val changelog: List<UpdateChangelogEntry>,
    val availableChannels: List<String>
) {
    fun hasUpdate(currentVersionCode: Int): Boolean = latestVersionCode > currentVersionCode

    fun changelogSince(currentVersionCode: Int): List<UpdateChangelogEntry> =
        changelog.filter { it.versionCode > currentVersionCode }
}