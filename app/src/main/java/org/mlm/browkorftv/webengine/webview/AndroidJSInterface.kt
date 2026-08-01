package org.mlm.browkorftv.webengine.webview

import android.net.http.SslError
import android.webkit.JavascriptInterface
import androidx.webkit.URLUtilCompat
import org.mlm.browkorftv.R
import org.mlm.browkorftv.BrowkorfTV
import org.mlm.browkorftv.model.Download

class AndroidJSInterface(private val webEngine: WebViewWebEngine) {

    @JavascriptInterface
    fun currentUrl(): String {
        if (!webEngine.tab.url.startsWith(WebViewEx.INTERNAL_SCHEME)) return ""
        return webEngine.tab.url
    }

    /** Used by the adblock document-start bootstrap to fetch cosmetic rules for the page. */
    @JavascriptInterface
    fun getCosmetic(url: String): String? {
        val result = webEngine.getCosmeticFor(url)
        android.util.Log.d(
            "btv-adblock",
            "getCosmetic($url) -> " + (if (result == null) "null" else "len=${result.length}")
        )
        return result
    }

    @JavascriptInterface
    fun reloadWithSslTrust() {
        val callback = webEngine.callback ?: return
        callback.getActivity().runOnUiThread {
            val webview = webEngine.getView() as? WebViewEx ?: return@runOnUiThread
            webview.trustSsl = true
            val target = webview.lastSslErrorUrl ?: webview.lastSSLError?.url ?: webEngine.tab.url
            webEngine.loadUrl(target)
        }
    }

    @JavascriptInterface
    fun getStringByName(name: String): String {
        val ctx = BrowkorfTV.instance
        return when (name) {
            "connection_isnt_secure" -> ctx.getString(R.string.connection_isnt_secure)
            "hostname" -> ctx.getString(R.string.hostname)
            "err_desk" -> ctx.getString(R.string.err_desk)
            "details" -> ctx.getString(R.string.details)
            "back_to_safety" -> ctx.getString(R.string.back_to_safety)
            "go_im_aware" -> ctx.getString(R.string.go_im_aware)
            else -> ""
        }
    }

    @JavascriptInterface
    fun lastSSLError(getDetails: Boolean): String {
        val lastSSLError = (webEngine.getView() as? WebViewEx)?.lastSSLError ?: return "unknown"
        return if (getDetails) {
            lastSSLError.toString()
        } else {
            when (lastSSLError.primaryError) {
                SslError.SSL_EXPIRED -> BrowkorfTV.instance.getString(R.string.ssl_expired)
                SslError.SSL_IDMISMATCH -> BrowkorfTV.instance.getString(R.string.ssl_idmismatch)
                SslError.SSL_DATE_INVALID -> BrowkorfTV.instance.getString(R.string.ssl_date_invalid)
                SslError.SSL_INVALID -> BrowkorfTV.instance.getString(R.string.ssl_invalid)
                else -> "unknown"
            }
        }
    }

    @JavascriptInterface
    fun takeBlobDownloadData(
        base64BlobData: String,
        fileName: String?,
        url: String,
        mimetype: String
    ) {
        val callback = webEngine.callback ?: return
        val finalFileName = fileName ?: URLUtilCompat.guessFileName(url, null, mimetype)
        callback.onDownloadRequested(
            url, "",
            finalFileName, "Browkorf TV",
            mimetype, Download.OperationAfterDownload.NOP, base64BlobData
        )
    }
}