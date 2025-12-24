package org.mlm.browkorftv.webengine

import android.content.Context
import android.util.Log
import androidx.annotation.UiThread
import androidx.startup.Initializer
import org.mlm.browkorftv.model.WebTabState
import org.mlm.browkorftv.settings.AppSettings
import org.mlm.browkorftv.settings.SettingsManager
import org.mlm.browkorftv.settings.Theme
import org.mlm.browkorftv.widgets.cursor.CursorLayout

interface WebEngineProviderCallback {
    suspend fun initialize(context: Context, webViewContainer: CursorLayout)
    fun createWebEngine(tab: WebTabState): WebEngine
    suspend fun clearCache(ctx: Context)
    fun onThemeSettingUpdated(value: Theme)
    fun getWebEngineVersionString(): String
}

data class WebEngineProvider(
    val name: String,
    val callback: WebEngineProviderCallback
)

object WebEngineFactory {
    const val TAG = "WebEngineFactory"

    private val engineProviders = LinkedHashMap<String, WebEngineProvider>()

    @Volatile
    private var initializedProvider: WebEngineProvider? = null

    @Volatile
    private var attemptedBootstrap: Boolean = false

    val isInitialized: Boolean
        get() = initializedProvider != null

    fun registerProvider(provider: WebEngineProvider) {
        val existing = engineProviders.put(provider.name, provider)
        if (existing != null) {
            Log.w(TAG, "Provider '${provider.name}' replaced (duplicate registration).")
        } else {
            Log.d(TAG, "Provider '${provider.name}' registered.")
        }
    }

    fun getProviders(): List<WebEngineProvider> = engineProviders.values.toList()

    private fun ensureProvidersRegistered(context: Context) {
        if (engineProviders.isNotEmpty()) return
        if (attemptedBootstrap) return
        attemptedBootstrap = true

        fun tryInit(className: String) {
            runCatching {
                val cls = Class.forName(className)
                // DIRECT FIX: Manually instantiate and call create()
                // This bypasses AppInitializer caching which fails in multi-process scenarios
                val initializer = cls.getDeclaredConstructor().newInstance() as Initializer<*>
                initializer.create(context)

                Log.d(TAG, "Bootstrapped initializer manually: $className")
            }.onFailure {
                Log.w(
                    TAG,
                    "Initializer not available: $className (${it.javaClass.simpleName}: ${it.message})"
                )
            }
        }

        // WebView provider
        tryInit("org.mlm.browkorftv.webengine.webview.WebViewEngineInitializer")

        // Gecko provider
        tryInit("org.mlm.browkorftv.webengine.gecko.GeckoEngineInitializer")
    }

    @UiThread
    suspend fun initialize(
        context: Context,
        webViewContainer: CursorLayout,
        settingsManager: SettingsManager
    ) {
        ensureProvidersRegistered(context)

        val settings = settingsManager.current

        if (engineProviders.isEmpty()) {
            Log.e(TAG, "CRITICAL: No WebEngineProviders found after bootstrap attempt.")
            throw IllegalStateException(
                "No WebEngineProviders registered in process ${android.os.Process.myPid()}. " +
                        "ProGuard might be stripping 'WebViewEngineInitializer'."
            )
        }

        var provider = engineProviders[settings.webEngine]

        if (provider == null) {
            // Fallback to first registered provider
            provider = engineProviders.values.firstOrNull()

            if (provider == null) {
                // Should be caught by isEmpty check above, but for safety:
                throw IllegalArgumentException("WebEngineProvider with name ${settings.webEngine} not found and fallback failed.")
            }

            Log.w(
                TAG,
                "WebEngineProvider '${settings.webEngine}' not found, using '${provider.name}'"
            )

            runCatching {
                settingsManager.setWebEngine(
                    AppSettings.SupportedWebEngines.indexOf(provider.name)
                )
            }
        }

        provider.callback.initialize(context, webViewContainer)
        initializedProvider = provider
    }

    // ... (Rest of the file remains unchanged) ...

    fun createWebEngine(tab: WebTabState): WebEngine {
        val provider = initializedProvider
            ?: throw IllegalStateException("WebEngineFactory not initialized")
        return provider.callback.createWebEngine(tab)
    }

    suspend fun clearCache(ctx: Context) {
        val provider = initializedProvider
            ?: throw IllegalStateException("WebEngineFactory not initialized")
        provider.callback.clearCache(ctx)
    }

    fun onThemeSettingUpdated(value: Theme) {
        initializedProvider?.callback?.onThemeSettingUpdated(value)
    }

    fun getWebEngineVersionString(): String {
        val provider = initializedProvider ?: return "Not initialized"
        return provider.callback.getWebEngineVersionString()
    }
}

fun WebEngine.isGecko(): Boolean {
    return this.getWebEngineName() == AppSettings.ENGINE_GECKO_VIEW
}