package com.phlox.tvwebbrowser.compose.runtime

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.view.View
import android.view.ViewGroup
import com.phlox.tvwebbrowser.TVBro
import com.phlox.tvwebbrowser.compose.vm.BrowserDataViewModel
import com.phlox.tvwebbrowser.compose.vm.TabsViewModel
import com.phlox.tvwebbrowser.data.AdblockRepository
import com.phlox.tvwebbrowser.model.Download
import com.phlox.tvwebbrowser.model.HomePageLink
import com.phlox.tvwebbrowser.model.WebTabState
import com.phlox.tvwebbrowser.utils.DownloadUtils
import com.phlox.tvwebbrowser.webengine.WebEngine
import com.phlox.tvwebbrowser.webengine.WebEngineWindowProviderCallback
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.InputStream
import java.net.URLEncoder

class BrowserHost(
    private val activity: Activity,
    private val tabsVm: TabsViewModel,
    private val browserDataVm: BrowserDataViewModel,
    private val adblockRepo: AdblockRepository,
    private val downloadsConnector: DownloadServiceConnector,
    private val platform: Platform,
) : WebEngineWindowProviderCallback {

    interface Platform {
        fun requestPermissions(requestCode: Int, permissions: Array<String>)
        fun launchFileChooser(intent: Intent): Boolean
        fun startVoiceSearch()
        fun copyToClipboard(text: String)
        fun shareText(text: String)
        fun openExternal(url: String)
        fun toast(msg: String)
    }

    private val scope = CoroutineScope(Dispatchers.Main.immediate)
    private val _chrome = MutableStateFlow(BrowserChromeState())
    val chrome = _chrome.asStateFlow()
    private val _events = MutableSharedFlow<BrowserUiEvent>(extraBufferCapacity = 32)
    val events = _events.asSharedFlow()

    private var webParent: ViewGroup? = null
    private var fullscreenParent: ViewGroup? = null
    private var webViewProvider: ((WebTabState) -> View?)? = null
    private var attachedTab: WebTabState? = null

    fun startOnce() {
        browserDataVm.loadOnce()
        scope.launch { adblockRepo.ensureLoaded(false) }
    }

    fun setContainers(webParent: ViewGroup, fullscreenParent: ViewGroup, webViewProvider: (WebTabState) -> View?) {
        this.webParent = webParent
        this.fullscreenParent = fullscreenParent
        this.webViewProvider = webViewProvider
    }

    fun ensureInitialTabAndAttachIfNeeded() {
        tabsVm.load()
        tabsVm.currentTab.value?.let { attachTab(it) }
    }

    fun attachTab(tab: WebTabState) {
        val wp = webParent ?: return
        val fp = fullscreenParent ?: return
        val provider = webViewProvider ?: return
        if (attachedTab?.id == tab.id) return

        attachedTab?.let { old ->
            old.webEngine.onDetachFromWindow(false, false)
            old.onPause()
            tabsVm.persistTab(old)
        }

        tabsVm.select(tab)
        val engine = tab.webEngine
        var view = engine.getView()
        var needReload = false
        if (view == null) {
            view = provider(tab)
            if (view == null) return
            needReload = !tab.restoreWebView()
        }
        engine.onAttachToWindow(this, wp, fp)
        if (needReload) engine.loadUrl(tab.url)
        attachedTab = tab
    }

    fun openUrl(url: String) { tabsVm.currentTab.value?.webEngine?.loadUrl(url) }
    fun goBack() { tabsVm.currentTab.value?.webEngine?.goBack() }
    fun goForward() { tabsVm.currentTab.value?.webEngine?.goForward() }
    fun reload() { tabsVm.currentTab.value?.webEngine?.reload() }
    fun home() { tabsVm.currentTab.value?.webEngine?.loadUrl(com.phlox.tvwebbrowser.Config.HOME_URL_ALIAS) }

    fun onVoiceQuery(text: String?) {
        val q = text?.trim() ?: return
        val url = if (q.contains(" ") || !q.contains(".")) {
            val template = TVBro.config.searchEngineURL.value
            if (template.contains("%s")) template.format(URLEncoder.encode(q, "UTF-8")) else template + URLEncoder.encode(q, "UTF-8")
        } else if (!q.startsWith("http")) "https://$q" else q
        openUrl(url)
    }

    fun searchOrNavigate(input: String) {
        val tab = tabsVm.currentTab.value ?: return
        val engine = tab.webEngine

        val trimmed = input.trim()

        val looksLikeUrl =
            trimmed.matches(Regex("^[a-zA-Z][a-zA-Z0-9+.-]*://.*")) ||
                    (!trimmed.contains(" ") && trimmed.contains("."))

        val url = if (looksLikeUrl) {
            if (trimmed.matches(Regex("^[a-zA-Z][a-zA-Z0-9+.-]*://.*"))) trimmed else "https://$trimmed"
        } else {
            buildSearchUrl(trimmed)
        }

        engine.loadUrl(url)
    }

    private fun buildSearchUrl(query: String): String {
        val template = TVBro.config.searchEngineURL.value
        val encoded = URLEncoder.encode(query, Charsets.UTF_8.name())

        return when {
            template.contains("[query]") -> template.replace("[query]", encoded)
            template.contains("%s") -> template.format(encoded)
            template.endsWith("?") || template.endsWith("=") -> template + encoded
            else -> "$template$encoded"
        }
    }

    override fun getActivity() = activity
    override fun onOpenInNewTabRequested(url: String, navigateImmediately: Boolean): WebEngine? {
        val tab = tabsVm.newTab(url, navigateImmediately)
        if (navigateImmediately) attachTab(tab)
        return tab.webEngine
    }
    override fun onReceivedTitle(title: String) {
        tabsVm.currentTab.value?.let { 
            tabsVm.updateTitle(it, title) 
            browserDataVm.onTabTitleUpdated(it.url, title)
        }
        _chrome.value = _chrome.value.copy(title = title)
    }

    override fun onProgressChanged(newProgress: Int) { _chrome.value = _chrome.value.copy(progress = newProgress) }
    override fun isAd(url: Uri, acceptHeader: String?, baseUri: Uri): Boolean? = adblockRepo.matches(url, acceptHeader, baseUri)
    override fun isAdBlockingEnabled() = TVBro.config.adBlockEnabled
    override fun isDialogsBlockingEnabled() = false
    override fun onBlockedAd(uri: String) { _chrome.value = _chrome.value.copy(blockedAds = _chrome.value.blockedAds + 1) }
    override fun onBlockedDialog(newTab: Boolean) { _chrome.value = _chrome.value.copy(blockedPopups = _chrome.value.blockedPopups + 1) }
    
    override fun onDownloadRequested(url: String) { onDownloadRequested(url, "", DownloadUtils.guessFileName(url, null, null), null, null, Download.OperationAfterDownload.NOP) }
    override fun onDownloadRequested(url: String, referer: String, originalDownloadFileName: String, userAgent: String?, mimeType: String?, operationAfterDownload: Download.OperationAfterDownload, base64BlobData: String?, stream: InputStream?, size: Long) {
        val d = Download(url, originalDownloadFileName, null, operationAfterDownload, mimeType, referer, userAgent, base64BlobData, stream, size)
        downloadsConnector.service?.startDownload(d) ?: platform.toast("Service not bound")
    }
    override fun onDownloadRequested(url: String, userAgent: String?, contentDisposition: String, mimetype: String?, contentLength: Long) {
        onDownloadRequested(url, "", DownloadUtils.guessFileName(url, contentDisposition, mimetype), userAgent, mimetype, Download.OperationAfterDownload.NOP, null, null, contentLength)
    }

    override fun requestPermissions(array: Array<String>): Int { platform.requestPermissions(1001, array); return 1001 }
    override fun onShowFileChooser(intent: Intent) = platform.launchFileChooser(intent)
    override fun onReceivedIcon(icon: android.graphics.Bitmap) {}
    override fun shouldOverrideUrlLoading(url: String) = false
    override fun onPageStarted(url: String?) { _chrome.value = _chrome.value.copy(url = url ?: "") }
    override fun onPageFinished(url: String?) {}
    override fun onPageCertificateError(url: String?) { platform.toast("Certificate error") }
    override fun onCreateWindow(dialog: Boolean, userGesture: Boolean): View? = onOpenInNewTabRequested("about:blank", false)?.getView()
    override fun closeWindow(internalRepresentation: Any) { tabsVm.currentTab.value?.let { tabsVm.close(it) } }
    override fun onScaleChanged(oldScale: Float, newScale: Float) {}
    override fun onCopyTextToClipboardRequested(url: String) = platform.copyToClipboard(url)
    override fun onShareUrlRequested(url: String) = platform.shareText(url)
    override fun onOpenInExternalAppRequested(url: String) = platform.openExternal(url)
    override fun initiateVoiceSearch() = platform.startVoiceSearch()
    override fun onEditHomePageBookmarkSelected(index: Int) { _events.tryEmit(BrowserUiEvent.EditHomePageBookmark(index)) }
    override fun getHomePageLinks() = browserDataVm.homePageLinks.value
    override fun onPrepareForFullscreen() { _chrome.value = _chrome.value.copy(isFullscreen = true) }
    override fun onExitFullscreen() { _chrome.value = _chrome.value.copy(isFullscreen = false) }
    override fun onVisited(url: String) { browserDataVm.logVisitedHistory(tabsVm.currentTab.value?.title, url, null) }
    override fun suggestActionsForLink(href: String, x: Int, y: Int) { _events.tryEmit(BrowserUiEvent.ShowLinkActions(href, x, y)) }
    override fun markBookmarkRecommendationAsUseful(bookmarkOrder: Int) = browserDataVm.markBookmarkRecommendationAsUseful(bookmarkOrder)
    fun deliverPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) { tabsVm.currentTab.value?.webEngine?.onPermissionsResult(requestCode, permissions, grantResults) }
    fun deliverFileChooserResult(resultCode: Int, data: Intent?) { tabsVm.currentTab.value?.webEngine?.onFilePicked(resultCode, data) }
}