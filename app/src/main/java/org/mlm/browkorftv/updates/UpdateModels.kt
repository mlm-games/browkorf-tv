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
    val downloadUrl: String,
    val digest: String? = null
)

data class UpdateInfo(
    val versionName: String,
    val downloadUrl: String,
    val changelog: String,
    val sha256: String? = null
) {
    fun hasUpdate(currentVersionName: String): Boolean {
        val remote = parseVersion(versionName) ?: return false
        val current = parseVersion(currentVersionName) ?: return false
        return remote > current
    }

    fun matchesVersion(otherVersionName: String): Boolean {
        val expected = parseVersion(versionName) ?: return false
        val actual = parseVersion(otherVersionName) ?: return false
        return expected.compareTo(actual) == 0
    }

    private fun parseVersion(version: String): ParsedVersion? {
        var normalized = version.trim()
        if (normalized.startsWith("v", ignoreCase = true)) {
            normalized = normalized.substring(1)
        }

        val withoutBuildMetadata = normalized.substringBefore('+')
        val prereleaseSeparator = withoutBuildMetadata.indexOf('-')
        val core = if (prereleaseSeparator >= 0) {
            withoutBuildMetadata.substring(0, prereleaseSeparator)
        } else {
            withoutBuildMetadata
        }
        val prerelease: List<String>?
        if (prereleaseSeparator >= 0) {
            val identifiers = withoutBuildMetadata.substring(prereleaseSeparator + 1).split('.')
            if (identifiers.isEmpty() || identifiers.any(String::isEmpty)) return null
            prerelease = identifiers
        } else {
            prerelease = null
        }
        val release = core.split('.').map { it.toLongOrNull() ?: return null }
        if (release.isEmpty() || release.any { it < 0 }) return null
        return ParsedVersion(release, prerelease)
    }
}

private data class ParsedVersion(
    val release: List<Long>,
    val prerelease: List<String>?
) : Comparable<ParsedVersion> {
    override fun compareTo(other: ParsedVersion): Int {
        val size = maxOf(release.size, other.release.size)
        for (index in 0 until size) {
            val comparison = (release.getOrNull(index) ?: 0L)
                .compareTo(other.release.getOrNull(index) ?: 0L)
            if (comparison != 0) return comparison
        }

        if (prerelease == null && other.prerelease == null) return 0
        if (prerelease == null) return 1
        if (other.prerelease == null) return -1

        val identifierCount = maxOf(prerelease.size, other.prerelease.size)
        for (index in 0 until identifierCount) {
            val left = prerelease.getOrNull(index) ?: return -1
            val right = other.prerelease.getOrNull(index) ?: return 1
            val leftNumeric = left.all(Char::isDigit)
            val rightNumeric = right.all(Char::isDigit)
            val comparison = when {
                leftNumeric && rightNumeric -> compareNumericIdentifier(left, right)
                leftNumeric -> -1
                rightNumeric -> 1
                else -> left.compareTo(right)
            }
            if (comparison != 0) return comparison
        }
        return 0
    }

    private fun compareNumericIdentifier(left: String, right: String): Int {
        val normalizedLeft = left.trimStart('0').ifEmpty { "0" }
        val normalizedRight = right.trimStart('0').ifEmpty { "0" }
        val lengthComparison = normalizedLeft.length.compareTo(normalizedRight.length)
        return if (lengthComparison != 0) {
            lengthComparison
        } else {
            normalizedLeft.compareTo(normalizedRight)
        }
    }
}
