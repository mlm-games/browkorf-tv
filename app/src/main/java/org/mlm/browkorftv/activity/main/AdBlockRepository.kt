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
import org.mlm.adblock.BlockDecision
import org.mlm.adblock.CosmeticResources
import org.mlm.adblock.RequestTypeMapper
import org.mlm.browkorftv.settings.AppSettings
import org.mlm.browkorftv.settings.SettingsManager
import org.mlm.browkorftv.ui.SnackbarManager
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.Calendar
import org.mlm.browkorftv.network.ProxyManager

class AdBlockRepository(
    private val settingsManager: SettingsManager,
    private val context: Context
) {
    companion object : KoinComponent {
        val TAG = AdBlockRepository::class.java.simpleName
        const val SERIALIZED_LIST_FILE = "adblock_rust_v2.bin"
        const val AUTO_UPDATE_INTERVAL_MINUTES = 60 * 24 * 7 // 7 days

        /**
         * EasyList/EasyPrivacy cover general network + cosmetic rules; uBO filters +
         * quick-fixes add scriptlet rules that improve coverage on video/other sites.
         */
        val DEFAULT_LISTS = listOf(
            "https://easylist.to/easylist/easylist.txt",
            "https://easylist.to/easylist/easyprivacy.txt",
            "https://raw.githubusercontent.com/uBlockOrigin/uAssets/master/filters/filters.txt",
            "https://raw.githubusercontent.com/uBlockOrigin/uAssets/master/filters/quick-fixes.txt",
        )

        private val snackbar: SnackbarManager by inject()
    }

    @Volatile
    private var engine: AdblockEngine? = null
    @Volatile
    private var resourcesJson: String? = null
    private val _clientLoading = MutableStateFlow(false)
    val clientLoading = _clientLoading.asStateFlow()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    init {
        cleanupOldCache()
        scope.launch { loadAdBlockList(false) }
    }

    /** Old engines serialized a different format / filter set; remove stale caches once. */
    private fun cleanupOldCache() {
        for (name in listOf("adblock_ser.dat", "adblock_rust.bin")) {
            val old = File(context.filesDir, name)
            if (old.exists() && old.delete()) {
                Log.i(TAG, "Removed stale adblock cache: $name")
            }
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
                val urls = if (settings.adBlockListURL.isBlank() ||
                    settings.adBlockListURL == AppSettings.DEFAULT_ADBLOCK_LIST_URL
                ) {
                    DEFAULT_LISTS
                } else {
                    listOf(settings.adBlockListURL)
                }

                val combined = buildString {
                    var any = false
                    for (u in urls) {
                        try {
                            val text = downloadText(u)
                            if (text.isNotBlank()) {
                                append(text)
                                append('\n')
                                any = true
                                Log.i(TAG, "Loaded filter list: $u (${text.length} chars)")
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to download filter list: $u", e)
                        }
                    }
                    if (!any) throw IllegalStateException("No filter lists downloaded")
                }
                success = newEngine.loadFilterList(combined)
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

        loadResources(newEngine)

        // Diagnostic: confirm the engine returns YouTube scriptlets/cosmetic rules.
        val probe = newEngine.cosmeticResources("https://www.youtube.com/watch?v=probe")
        Log.i(
            TAG,
            "Engine ready: youtube probe selectors=${probe?.selectors?.size} jsLen=${probe?.js?.length}"
        )

        val old = engine
        engine = newEngine
        old?.close()

        // A total failure doesn't delay the next auto-retry by a full interval.
        if (success) {
            settingsManager.setAdBlockListLastUpdate(now.timeInMillis)
        }

        if (!success) snackbar.show("Error loading ad-blocker list")

        _clientLoading.value = false
    }

    private fun downloadText(url: String): String {
        val conn = (ProxyManager.openConnection(URL(url)) as HttpURLConnection).apply {
            connectTimeout = 30_000
            readTimeout = 60_000
            instanceFollowRedirects = true
            setRequestProperty("User-Agent", "BrowkorfTV-AdBlock/1.0")
        }
        val code = conn.responseCode
        if (code !in 200..299) {
            conn.disconnect()
            throw IllegalStateException("HTTP $code for $url")
        }
        return conn.inputStream.bufferedReader().use { it.readText() }
    }

    /** Reads the bundled scriptlet resources once and registers them on the engine. */
    private fun loadResources(engine: AdblockEngine) {
        var json = resourcesJson
        if (json == null) {
            json = try {
                context.assets.open("adblock/resources.json").bufferedReader().use { it.readText() }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to read adblock resources asset", e)
                null
            }
            resourcesJson = json
        }
        if (json != null) engine.loadResources(json)
    }

    /** Cosmetic filtering (hide selectors + scriptlet JS) for a page URL. */
    fun cosmeticResources(url: String): CosmeticResources? {
        return engine?.cosmeticResources(url)
    }

    /**
     * JSON for the in-page document-start bootstrap: `{"css": "...", "js": "..."}`,
     * or null when the engine isn't ready or no rules apply.
     */
    fun cosmeticResourcesJson(url: String): String? {
        val res = engine?.cosmeticResources(url) ?: return null
        if (res.selectors.isEmpty() && res.js.isBlank()) return null
        return try {
            JSONObject()
                .put("css", res.css())
                .put("js", res.js)
                .toString()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to build cosmetic JSON", e)
            null
        }
    }

    fun isAd(url: Uri, type: String?, baseUri: Uri): Boolean =
        checkNetworkRequest(url, type, baseUri)?.matched ?: false

    /**
     * Returns null when the engine isn't ready or the check failed.
     */
    fun checkNetworkRequest(url: Uri, type: String?, baseUri: Uri): BlockDecision? {
        val eng = engine ?: return null
        val baseHost = baseUri.host
        if (baseHost == null) return null
        val requestType = RequestTypeMapper.from(url, type)
        return try {
            eng.checkNetworkRequest(url.toString(), baseUri.toString(), requestType)
        } catch (e: Exception) {
            Log.e(TAG, "Ad block match check failed", e)
            null
        }
    }
}
