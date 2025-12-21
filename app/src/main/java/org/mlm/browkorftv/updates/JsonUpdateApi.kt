package org.mlm.browkorftv.updates

import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.mlm.browkorftv.core.DispatcherProvider
import java.net.HttpURLConnection
import java.net.URL

class JsonUpdateApi(
    private val dispatchers: DispatcherProvider
) : UpdateApi {

    override suspend fun fetchManifest(manifestUrl: String): UpdateManifest = withContext(dispatchers.io) {
        val conn = (URL(manifestUrl).openConnection() as HttpURLConnection).apply {
            connectTimeout = 20_000
            readTimeout = 20_000
            useCaches = false
        }

        try {
            val content = conn.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(content)

            val channelsJson = json.getJSONArray("channels")
            val channels = buildList {
                for (i in 0 until channelsJson.length()) {
                    val o = channelsJson.getJSONObject(i)

                    val urls = if (o.has("urls")) {
                        val arr = o.getJSONArray("urls")
                        buildList {
                            for (j in 0 until arr.length()) add(arr.getString(j))
                        }
                    } else emptyList()

                    add(
                        UpdateChannel(
                            name = o.getString("name"),
                            latestVersionName = o.getString("latestVersionName"),
                            latestVersionCode = o.getInt("latestVersionCode"),
                            minApi = if (o.has("minAPI")) o.getInt("minAPI") else 21,
                            url = o.getString("url"),
                            urls = urls
                        )
                    )
                }
            }

            val changelogJson = json.getJSONArray("changelog")
            val changelog = buildList {
                for (i in 0 until changelogJson.length()) {
                    val o = changelogJson.getJSONObject(i)
                    add(
                        UpdateChangelogEntry(
                            versionCode = o.getInt("versionCode"),
                            versionName = o.getString("versionName"),
                            changes = o.getString("changes")
                        )
                    )
                }
            }

            UpdateManifest(channels = channels, changelog = changelog)
        } finally {
            conn.disconnect()
        }
    }
}