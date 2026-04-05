package org.mlm.browkorftv.updates

import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import org.mlm.browkorftv.core.DispatcherProvider
import java.net.HttpURLConnection
import java.net.URL

class JsonUpdateApi(
    private val dispatchers: DispatcherProvider
) : UpdateApi {

    companion object {
        private const val BASE_URL = "https://api.github.com/repos/mlm-games/browkorf-tv/releases"
    }

    override suspend fun fetchLatestRelease(prerelease: Boolean): GitHubRelease? = withContext(dispatchers.io) {
        val url = if (prerelease) {
            BASE_URL
        } else {
            "$BASE_URL/latest"
        }

        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 20_000
            readTimeout = 20_000
            useCaches = false
            setRequestProperty("Accept", "application/vnd.github.v3+json")
        }

        try {
            if (conn.responseCode != HttpURLConnection.HTTP_OK) return@withContext null

            val content = conn.inputStream.bufferedReader().use { it.readText() }

            if (prerelease) {
                val array = JSONArray(content)
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    if (obj.getBoolean("prerelease")) {
                        return@withContext parseRelease(obj)
                    }
                }
                null
            } else {
                val obj = JSONObject(content)
                parseRelease(obj)
            }
        } finally {
            conn.disconnect()
        }
    }

    private fun parseRelease(obj: JSONObject): GitHubRelease {
        val assetsJson = obj.getJSONArray("assets")
        val assets = buildList {
            for (i in 0 until assetsJson.length()) {
                val asset = assetsJson.getJSONObject(i)
                add(
                    GitHubAsset(
                        name = asset.getString("name"),
                        downloadUrl = asset.getString("browser_download_url")
                    )
                )
            }
        }

        return GitHubRelease(
            tagName = obj.getString("tag_name"),
            name = obj.getString("name"),
            prerelease = obj.getBoolean("prerelease"),
            body = obj.optString("body", ""),
            assets = assets
        )
    }
}
