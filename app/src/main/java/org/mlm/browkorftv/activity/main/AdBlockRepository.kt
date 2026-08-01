package org.mlm.browkorftv.activity.main

import android.content.Context
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.mlm.adblock.AdblockEngine
import org.mlm.adblock.RequestTypeMapper
import org.mlm.browkorftv.settings.SettingsManager
import org.mlm.browkorftv.ui.SnackbarManager
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.Calendar

class AdBlockRepository(
    private val settingsManager: SettingsManager,
    private val context: Context
) {
    companion object : KoinComponent {
        val TAG = AdBlockRepository::class.java.simpleName
        const val SERIALIZED_LIST_FILE = "adblock_rust.bin"
        const val AUTO_UPDATE_INTERVAL_MINUTES = 60 * 24 * 7 // 7 days

        private val snackbar: SnackbarManager by inject()
    }

    @Volatile
    private var engine: AdblockEngine? = null
    private val _clientLoading = MutableStateFlow(false)
    val clientLoading = _clientLoading.asStateFlow()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    init {
        cleanupOldCache()
        scope.launch { loadAdBlockList(false) }
    }

    /** Old engine serialized a different format; remove the stale cache once. */
    private fun cleanupOldCache() {
        val old = File(context.filesDir, "adblock_ser.dat")
        if (old.exists() && old.delete()) {
            Log.i(TAG, "Removed stale adblock cache from old engine")
        }
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
        val newEngine = AdblockEngine.create()
        var success = false

        withContext(Dispatchers.IO) {
            val serializedFile = File(context.filesDir, SERIALIZED_LIST_FILE)
            if ((!needUpdate) && serializedFile.exists() && newEngine.deserializeFrom(serializedFile)) {
                success = true
                return@withContext
            }
            try {
                val listUrl = settings.adBlockListURL.ifBlank {
                    "https://easylist.to/easylist/easylist.txt"
                }
                val easyList = downloadText(listUrl)
                success = newEngine.loadFilterList(easyList)
                if (success) {
                    newEngine.serializeTo(serializedFile)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load ad block list", e)
                if (serializedFile.exists() && newEngine.deserializeFrom(serializedFile)) {
                    success = true
                    Log.w(TAG, "Using stale adblock cache")
                } else {
                    snackbar.postError("Error loading ad-blocker list", e.message)
                }
            }
        }

        val old = engine
        engine = newEngine
        old?.close()

        settingsManager.setAdBlockListLastUpdate(now.timeInMillis)

        if (!success) snackbar.show("Error loading ad-blocker list")

        _clientLoading.value = false
    }

    private fun downloadText(url: String): String {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 30_000
            readTimeout = 60_000
            setRequestProperty("User-Agent", "BrowkorfTV-Adblock/1.0")
            instanceFollowRedirects = true
        }
        return conn.inputStream.bufferedReader().use { it.readText() }
    }

    fun isAd(url: Uri, type: String?, baseUri: Uri): Boolean {
        val eng = engine ?: return false
        val baseHost = baseUri.host
        if (baseHost == null) return false
        val requestType = RequestTypeMapper.from(url, type)
        return try {
            eng.shouldBlock(url.toString(), baseUri.toString(), requestType)
        } catch (e: Exception) {
            Log.e(TAG, "Ad block match check failed", e)
            false
        }
    }
}
