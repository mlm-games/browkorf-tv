package org.mlm.browkorftv.updates

data class GitHubRelease(
    val tagName: String,
    val name: String,
    val prerelease: Boolean,
    val body: String,
    val assets: List<GitHubAsset>
)

data class GitHubAsset(
    val name: String,
    val downloadUrl: String
)

data class UpdateInfo(
    val versionName: String,
    val downloadUrl: String,
    val changelog: String
) {
    fun hasUpdate(currentVersionName: String): Boolean {
        val remote = parseVersion(versionName)
        val current = parseVersion(currentVersionName)
        return remote > current
    }

    private fun parseVersion(version: String): List<Int> {
        val cleaned = version.removePrefix("v")
        return cleaned.split(".").mapNotNull { it.toIntOrNull() }
    }

    private operator fun List<Int>.compareTo(other: List<Int>): Int {
        val maxLen = maxOf(size, other.size)
        for (i in 0 until maxLen) {
            val a = getOrNull(i) ?: 0
            val b = other.getOrNull(i) ?: 0
            if (a != b) return a.compareTo(b)
        }
        return 0
    }
}
