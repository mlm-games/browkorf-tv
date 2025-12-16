package com.phlox.tvwebbrowser.data

import android.net.Uri
import com.brave.adblock.AdBlockClient
import com.brave.adblock.AdBlockClient.FilterOption
import com.phlox.tvwebbrowser.Config
import com.phlox.tvwebbrowser.core.DispatcherProvider
import com.phlox.tvwebbrowser.data.settings.ConfigRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URL
import java.util.Calendar

data class AdblockEngineState(
    val enabled: Boolean = true,
    val loading: Boolean = false,
    val listUrl: String = Config.DEFAULT_ADBLOCK_LIST_URL,
    val lastUpdatedMillis: Long = 0L,
    val error: String? = null,
)

class AdblockRepository(
    private val appFilesDir: File,
    private val config: Config,
    private val configRepo: ConfigRepository,
    private val dispatchers: DispatcherProvider,
) {
    companion object {
        private const val SERIALIZED_LIST_FILE = "adblock_ser.dat"
        private const val AUTO_UPDATE_INTERVAL_MINUTES = 60 * 24 * 30
    }

    private val mutex = Mutex()
    private var client: AdBlockClient? = null

    private val _state = MutableStateFlow(
        AdblockEngineState(
            enabled = config.adBlockEnabled,
            listUrl = config.adBlockListURL.value,
            lastUpdatedMillis = config.adBlockListLastUpdate
        )
    )
    val state: StateFlow<AdblockEngineState> = _state.asStateFlow()

    suspend fun ensureLoaded(forceUpdate: Boolean = false) {
        mutex.withLock {
            val enabled = config.adBlockEnabled
            _state.value = _state.value.copy(enabled = enabled)
            if (!enabled) return
            if (client != null && !forceUpdate && !needsUpdate()) return

            _state.value = _state.value.copy(loading = true, error = null)
            val result = withContext(dispatchers.io) { loadOrUpdateClient(forceUpdate) }
            
            client = result.client
            if (result.success) {
                val now = System.currentTimeMillis()
                withContext(dispatchers.io) { configRepo.setAdblockLastUpdate(now) }
                _state.value = _state.value.copy(loading = false, lastUpdatedMillis = now)
            } else {
                _state.value = _state.value.copy(loading = false, error = result.error ?: "Failed")
            }
        }
    }

    suspend fun forceUpdateNow() = ensureLoaded(forceUpdate = true)

    fun matches(url: Uri, typeHint: String?, baseUri: Uri): Boolean {
        if (!config.adBlockEnabled) return false
        val c = client ?: return false
        val baseHost = baseUri.host ?: return false
        val opt = runCatching { mapRequestToFilterOption(url, typeHint) }.getOrDefault(FilterOption.UNKNOWN)
        return runCatching { c.matches(url.toString(), opt, baseHost) }.getOrDefault(false)
    }

    private fun needsUpdate(): Boolean {
        val checkDate = Calendar.getInstance().apply {
            timeInMillis = config.adBlockListLastUpdate
            add(Calendar.MINUTE, AUTO_UPDATE_INTERVAL_MINUTES)
        }
        return checkDate.before(Calendar.getInstance())
    }

    private data class LoadResult(val success: Boolean, val client: AdBlockClient, val error: String? = null)

    private fun loadOrUpdateClient(forceUpdate: Boolean): LoadResult {
        val newClient = AdBlockClient()
        val serializedFile = File(appFilesDir, SERIALIZED_LIST_FILE)

        if (!forceUpdate && !needsUpdate() && serializedFile.exists()) {
            return if (newClient.deserialize(serializedFile.absolutePath)) LoadResult(true, newClient)
            else LoadResult(false, newClient, "Deserialize failed")
        }
        return try {
            val text = URL(config.adBlockListURL.value).openConnection().getInputStream().bufferedReader().use { it.readText() }
            if (newClient.parse(text)) {
                newClient.serialize(serializedFile.absolutePath)
                LoadResult(true, newClient)
            } else LoadResult(false, newClient, "Parse failed")
        } catch (t: Throwable) {
            LoadResult(false, newClient, t.toString())
        }
    }

    private fun mapRequestToFilterOption(url: Uri?, type: String?): FilterOption {
        if (type != null) {
            val t = type.lowercase()
            if (t.contains("image")) return FilterOption.IMAGE
            if (t.contains("css") || t.contains("style")) return FilterOption.CSS
            if (t.contains("script")) return FilterOption.SCRIPT
        }
        return FilterOption.UNKNOWN
    }
}