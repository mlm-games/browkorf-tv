package org.mlm.browkorftv.webengine.webview

import android.content.Context
import android.webkit.WebView
import androidx.startup.Initializer
import androidx.webkit.WebViewCompat
import org.mlm.browkorftv.model.WebTabState
import org.mlm.browkorftv.settings.AppSettings.Companion.ENGINE_WEB_VIEW
import org.mlm.browkorftv.settings.Theme
import org.mlm.browkorftv.webengine.*
import org.mlm.browkorftv.widgets.cursor.CursorLayout

class WebViewEngineInitializer : Initializer<Unit> {
    override fun create(context: Context) {
        val appCtx = context.applicationContext

        WebEngineFactory.registerProvider(
            WebEngineProvider(ENGINE_WEB_VIEW, object : WebEngineProviderCallback {
                override suspend fun initialize(context: Context, webViewContainer: CursorLayout) {
                    // no-op for WebView
                }

                override fun createWebEngine(tab: WebTabState): WebEngine =
                    WebViewWebEngine(tab)

                override suspend fun clearCache(ctx: Context) {
                    WebView(ctx).clearCache(true)
                }

                override fun onThemeSettingUpdated(value: Theme) {
                    // TODO: handle webview dark mode globally? or let the current one be as it is?
                }

                override fun getWebEngineVersionString(): String {
                    val pkg = WebViewCompat.getCurrentWebViewPackage(appCtx)
                    return "${pkg?.packageName ?: "unknown"}:${pkg?.versionName ?: "unknown"}"
                }
            })
        )
    }

    override fun dependencies(): List<Class<out Initializer<*>>> = emptyList()
}