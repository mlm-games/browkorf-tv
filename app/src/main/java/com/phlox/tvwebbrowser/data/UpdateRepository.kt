package com.phlox.tvwebbrowser.data

import android.os.Build
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class UpdateInfo(
    val latestVersionCode: Int,
    val latestVersionName: String,
    val channel: String,
    val downloadUrl: String,
    val availableChannels: List<String>,
    val changelog: List<ChangelogEntry>
) {
    data class ChangelogEntry(val versionCode: Int, val versionName: String, val changes: String)
}

class UpdateRepository(
    private val versionFileUrl: String = "https://raw.githubusercontent.com/truefedex/tv-bro/master/latest_version.json"
) {
    fun check(currentVersionCode: Int, channelsToCheck: Set<String>): UpdateInfo {
        val conn = (URL(versionFileUrl).openConnection() as HttpURLConnection)
        conn.connectTimeout = 20_000
        conn.readTimeout = 20_000

        try {
            val json = JSONObject(conn.inputStream.bufferedReader().use { it.readText() })

            val channelsJson = json.getJSONArray("channels")
            val available = mutableListOf<String>()

            var bestVersionCode = 0
            var bestVersionName = ""
            var bestUrl = ""
            var bestChannel = ""

            val currentAbi = Build.SUPPORTED_ABIS.firstOrNull().orEmpty()

            for (i in 0 until channelsJson.length()) {
                val ch = channelsJson.getJSONObject(i)
                val name = ch.getString("name")
                available += name

                if (!channelsToCheck.contains(name)) continue

                val minApi = if (ch.has("minAPI")) ch.getInt("minAPI") else 21
                if (minApi > Build.VERSION.SDK_INT) continue

                val vCode = ch.getInt("latestVersionCode")
                if (vCode <= bestVersionCode) continue

                var url = ch.getString("url")
                if (ch.has("urls")) {
                    val urls = ch.getJSONArray("urls")
                    for (j in 0 until urls.length()) {
                        val fileUrl = urls.getString(j)
                        if (fileUrl.endsWith("$currentAbi.apk")) {
                            url = fileUrl
                            break
                        }
                    }
                }

                bestVersionCode = vCode
                bestVersionName = ch.getString("latestVersionName")
                bestUrl = url
                bestChannel = name
            }

            val changelog = mutableListOf<UpdateInfo.ChangelogEntry>()
            val changelogJson: JSONArray = json.getJSONArray("changelog")
            for (i in 0 until changelogJson.length()) {
                val e = changelogJson.getJSONObject(i)
                changelog += UpdateInfo.ChangelogEntry(
                    versionCode = e.getInt("versionCode"),
                    versionName = e.getString("versionName"),
                    changes = e.getString("changes")
                )
            }

            return UpdateInfo(
                latestVersionCode = bestVersionCode,
                latestVersionName = bestVersionName.ifBlank { "Unknown" },
                channel = bestChannel.ifBlank { channelsToCheck.firstOrNull().orEmpty() },
                downloadUrl = bestUrl,
                availableChannels = available,
                changelog = changelog
            )
        } finally {
            conn.disconnect()
        }
    }
}