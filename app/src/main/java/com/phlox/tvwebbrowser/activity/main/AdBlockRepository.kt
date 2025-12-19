package com.phlox.tvwebbrowser.activity.main

import android.content.Context
import android.net.Uri
import android.widget.Toast
import com.brave.adblock.AdBlockClient
import com.brave.adblock.AdBlockClient.FilterOption
import com.brave.adblock.Utils.uriHasExtension
import com.phlox.tvwebbrowser.settings.SettingsManager
import com.phlox.tvwebbrowser.utils.Utils
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.net.URL
import java.util.*

class AdBlockRepository(
    private val settingsManager: SettingsManager,
    private val context: Context
) {
    companion object {
        const val SERIALIZED_LIST_FILE = "adblock_ser.dat"
        const val AUTO_UPDATE_INTERVAL_MINUTES = 60 * 24 * 30 // 30 days
    }

    private var client: AdBlockClient? = null
    private val _clientLoading = MutableStateFlow(false)
    val clientLoading = _clientLoading.asStateFlow()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    init {
        scope.launch { loadAdBlockList(false) }
    }

    suspend fun loadAdBlockList(forceReload: Boolean) {
        if (_clientLoading.value) return

        val settings = settingsManager.current
        val checkDate = Calendar.getInstance()
        checkDate.timeInMillis = settings.adBlockListLastUpdate
        checkDate.add(Calendar.MINUTE, AUTO_UPDATE_INTERVAL_MINUTES)
        val now = Calendar.getInstance()
        val needUpdate = forceReload || checkDate.before(now)

        _clientLoading.value = true
        val newClient = AdBlockClient()
        var success = false

        withContext(Dispatchers.IO) {
            val serializedFile = File(context.filesDir, SERIALIZED_LIST_FILE)
            if ((!needUpdate) && serializedFile.exists() && newClient.deserialize(serializedFile.absolutePath)) {
                success = true
                return@withContext
            }
            try {
                val easyList = URL(settings.adBlockListURL).openConnection().inputStream.bufferedReader()
                    .use { it.readText() }
                success = newClient.parse(easyList)
                newClient.serialize(serializedFile.absolutePath)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        this.client = newClient
        settingsManager.setAdBlockListLastUpdate(now.timeInMillis)

        if (!success) {
            Toast.makeText(context, "Error loading ad-blocker list", Toast.LENGTH_SHORT).show()
        }
        _clientLoading.value = false
    }

    fun isAd(url: Uri, type: String?, baseUri: Uri): Boolean {
        val client = client ?: return false
        val baseHost = baseUri.host
        val filterOption = try {
            mapRequestToFilterOption(url, type)
        } catch (e: Exception) {
            return false
        }
        val result = try {
            baseHost != null && client.matches(url.toString(), filterOption, baseHost)
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
        return result
    }

    private fun mapRequestToFilterOption(url: Uri?, type: String?): FilterOption {
        if (type != null) {
            if (type == "image" || type.contains("image/")) return FilterOption.IMAGE
            if (type == "style" || type.contains("/css")) return FilterOption.CSS
            if (type == "script" || type.contains("javascript")) return FilterOption.SCRIPT
            if (type.contains("video/")) return FilterOption.OBJECT
        }
        if (url != null) {
            if (uriHasExtension(url, "css")) return FilterOption.CSS
            if (uriHasExtension(url, "js")) return FilterOption.SCRIPT
            if (uriHasExtension(url, "png", "jpg", "jpeg", "webp", "svg", "gif", "bmp", "tiff")) return FilterOption.IMAGE
            if (uriHasExtension(url, "mp4", "mov", "avi")) return FilterOption.OBJECT
        }
        return FilterOption.UNKNOWN
    }
}