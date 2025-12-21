package org.mlm.browkorftv.updates

interface UpdateApi {
    suspend fun fetchManifest(manifestUrl: String): UpdateManifest
}