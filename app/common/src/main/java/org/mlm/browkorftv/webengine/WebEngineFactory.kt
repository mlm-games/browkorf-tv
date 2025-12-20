package org.mlm.browkorftv.webengine

import android.content.Context
import android.util.Log
import androidx.annotation.UiThread
import org.mlm.browkorftv.AppContext
import org.mlm.browkorftv.model.WebTabState
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
    private val engineProviders = mutableListOf<WebEngineProvider>()
    private var initializedProvider: WebEngineProvider? = null  // Changed to nullable

    val isInitialized: Boolean
        get() = initializedProvider != null

    fun registerProvider(provider: WebEngineProvider) {
        engineProviders.add(provider)
    }

    fun getProviders(): List<WebEngineProvider> {
        return engineProviders
    }

    @UiThread
    suspend fun initialize(context: Context, webViewContainer: CursorLayout) {
        val settingsManager = AppContext.provideSettingsManager()
        val settings = settingsManager.current

        var webEngineProvider = engineProviders.find { it.name == settings.webEngine }
        if (webEngineProvider == null && engineProviders.isNotEmpty()) {
            webEngineProvider = engineProviders[0]
            Log.w(TAG, "WebEngineProvider with name ${settings.webEngine} not found, using ${webEngineProvider.name}")
            settingsManager.setWebEngine(
                org.mlm.browkorftv.settings.AppSettings.SupportedWebEngines.indexOf(webEngineProvider.name)
            )
        }
        if (webEngineProvider != null) {
            webEngineProvider.callback.initialize(context, webViewContainer)
            initializedProvider = webEngineProvider
        } else {
            throw IllegalArgumentException("WebEngineProvider with name ${settings.webEngine} not found")
        }
    }

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
        val provider = initializedProvider
            ?: return "Not initialized"
        return provider.callback.getWebEngineVersionString()
    }
}

fun WebEngine.isGecko(): Boolean {
    return this.getWebEngineName() == org.mlm.browkorftv.settings.AppSettings.ENGINE_GECKO_VIEW
}