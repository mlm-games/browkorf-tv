package org.mlm.browkorftv.updates

interface UpdateApi {
    suspend fun fetchLatestRelease(prerelease: Boolean): GitHubRelease?
}
