package com.phlox.tvwebbrowser.data
import com.phlox.tvwebbrowser.Config
import com.phlox.tvwebbrowser.model.FavoriteItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Locale

class RecommendationsRepository {
    suspend fun fetch(countryCode: String): List<FavoriteItem>? = withContext(Dispatchers.IO) {
        try {
            val url = "${Config.HOME_PAGE_URL}recommendations/$countryCode.json"
            val response = URL(url).readText()
            val jsonArray = JSONArray(response)
            val result = mutableListOf<FavoriteItem>()
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            for (i in 0 until jsonArray.length()) {
                val o = jsonArray.getJSONObject(i)
                val f = FavoriteItem().apply {
                    title = o.getString("title")
                    this.url = o.getString("url")
                    favicon = o.optString("favicon", null)
                    destUrl = o.optString("dest_url", null)
                    description = o.optString("description", null)
                    order = i
                    homePageBookmark = true
                    val validUntilStr = o.optString("valid_until", null)
                    if (!validUntilStr.isNullOrBlank()) validUntil = dateFormat.parse(validUntilStr)
                }
                result.add(f)
            }
            result
        } catch (_: Throwable) { null }
    }
}