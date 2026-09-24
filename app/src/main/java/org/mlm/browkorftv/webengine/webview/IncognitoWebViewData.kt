package org.mlm.browkorftv.webengine.webview

import android.os.Build
import android.util.Log
import android.webkit.WebView

object IncognitoWebViewData {
    const val SUFFIX = "incognito"
    private const val TAG = "IncognitoWebViewData"
    private var configured = false

    fun configure() {
        if (configured || Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return
        configured = true
        runCatching {
            WebView.setDataDirectorySuffix(SUFFIX)
        }.onFailure { error ->
            Log.e(TAG, "Unable to configure the incognito WebView data directory", error)
        }
    }
}
