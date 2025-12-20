package org.mlm.browkorftv.webengine.gecko

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.ViewGroup
import androidx.annotation.UiThread
import org.mlm.browkorftv.AppContext
import org.mlm.browkorftv.widgets.cursor.CursorLayout
import org.mlm.browkorftv.model.WebTabState
import org.mlm.browkorftv.settings.AppSettings.Companion.HOME_PAGE_URL
import org.mlm.browkorftv.settings.AppSettings.Companion.HOME_URL_ALIAS
import org.mlm.browkorftv.settings.HomePageMode
import org.mlm.browkorftv.settings.Theme
import org.mlm.browkorftv.settings.toGeckoPreferredColorScheme
import org.mlm.browkorftv.utils.observable.ObservableValue
import org.mlm.browkorftv.webengine.WebEngine
import org.mlm.browkorftv.webengine.WebEngineFactory
import org.mlm.browkorftv.webengine.WebEngineProvider
import org.mlm.browkorftv.webengine.WebEngineProviderCallback
import org.mlm.browkorftv.webengine.WebEngineWindowProviderCallback
import org.mlm.browkorftv.webengine.gecko.delegates.*
import org.mlm.browkorftv.widgets.cursor.CursorDrawerDelegate
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.mozilla.geckoview.*
import org.mozilla.geckoview.GeckoSession.SessionState
import org.mozilla.geckoview.WebExtension.MessageDelegate
import java.lang.ref.WeakReference
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class GeckoWebEngine(val tab: WebTabState) : WebEngine, CursorDrawerDelegate.TextSelectionCallback,
    CursorDrawerDelegate.Callback {

    companion object {
        const val ENGINE_NAME = "GeckoView"
        private const val APP_WEB_EXTENSION_VERSION = 48
        val TAG: String = GeckoWebEngine::class.java.simpleName
        lateinit var runtime: GeckoRuntime
        var appWebExtension = ObservableValue<WebExtension?>(null)
        var weakRefToSingleGeckoView: WeakReference<GeckoViewWithVirtualCursor?> = WeakReference(null)
        val uiHandler = Handler(Looper.getMainLooper())

        @OptIn(DelicateCoroutinesApi::class)
        @UiThread
        fun initialize(context: Context, webViewContainer: CursorLayout) {
            if (!this::runtime.isInitialized) {
                val settings = AppContext.settings  // Use new accessor
                val settingsManager = AppContext.provideSettingsManager()

                val builder = GeckoRuntimeSettings.Builder()
                if (BuildConfig.DEBUG) {
                    builder.remoteDebuggingEnabled(true)
                    builder.consoleOutput(true)
                }
                builder.aboutConfigEnabled(true)
                    .preferredColorScheme(settings.themeEnum.toGeckoPreferredColorScheme())
                    .forceUserScalableEnabled(true)
                builder.contentBlocking(
                    ContentBlocking.Settings.Builder()
                        .antiTracking(
                            ContentBlocking.AntiTracking.DEFAULT or ContentBlocking.AntiTracking.STP)
                        .safeBrowsing(ContentBlocking.SafeBrowsing.DEFAULT)
                        .cookieBehavior(ContentBlocking.CookieBehavior.ACCEPT_NON_TRACKERS)
                        .cookieBehaviorPrivateMode(ContentBlocking.CookieBehavior.ACCEPT_NON_TRACKERS)
                        .enhancedTrackingProtectionLevel(ContentBlocking.EtpLevel.STRICT)
                        .build())
                runtime = GeckoRuntime.create(context, builder.build())

                val webExtInstallResult = if (APP_WEB_EXTENSION_VERSION == settings.appWebExtensionVersion) {
                    Log.d(TAG, "appWebExtension already installed")
                    runtime.webExtensionController.ensureBuiltIn(
                        "resource://android/assets/extensions/generic/",
                        "browkorftv@mock.com"
                    )
                } else {
                    Log.d(TAG, "installing appWebExtension")
                    runtime.webExtensionController.installBuiltIn("resource://android/assets/extensions/generic/")
                }

                webExtInstallResult.accept({ extension ->
                    Log.d(TAG, "extension accepted: ${extension?.metaData?.description}")
                    appWebExtension.value = extension
                    // Update setting in coroutine
                    GlobalScope.launch {
                        settingsManager.update {
                            it.copy(appWebExtensionVersion = APP_WEB_EXTENSION_VERSION)
                        }
                    }
                }) { e -> Log.e(TAG, "Error registering WebExtension", e) }
            }

            val webView = GeckoViewWithVirtualCursor(context)
            webViewContainer.addView(webView)
            weakRefToSingleGeckoView = WeakReference(webView)
            webViewContainer.setWillNotDraw(true)
        }

        suspend fun clearCache(ctx: Context) {
            suspendCoroutine { cont ->
                runtime.storageController.clearData(StorageController.ClearFlags.ALL_CACHES).then({
                    cont.resume(null)
                    GeckoResult.fromValue(null)
                }, {
                    it.printStackTrace()
                    cont.resumeWithException(it)
                    GeckoResult.fromValue(null)
                })
            }
        }

        fun onThemeSettingUpdated(theme: Theme) {
            runtime.settings.preferredColorScheme = theme.toGeckoPreferredColorScheme()
        }

        init {
            WebEngineFactory.registerProvider(WebEngineProvider(ENGINE_NAME, object : WebEngineProviderCallback {
                override suspend fun initialize(context: Context, webViewContainer: CursorLayout) {
                    GeckoWebEngine.initialize(context, webViewContainer)
                }

                override fun createWebEngine(tab: WebTabState): WebEngine {
                    return GeckoWebEngine(tab)
                }

                override suspend fun clearCache(ctx: Context) {
                    GeckoWebEngine.clearCache(ctx)
                }

                override fun onThemeSettingUpdated(value: Theme) {
                    GeckoWebEngine.onThemeSettingUpdated(value)
                }

                override fun getWebEngineVersionString(): String {
                    return BuildConfig.LIBRARY_PACKAGE_NAME + ":" +
                            BuildConfig.MOZ_APP_VERSION + "." +
                            BuildConfig.MOZ_APP_BUILDID + "-" +
                            BuildConfig.MOZ_UPDATE_CHANNEL
                }
            }))
        }
    }

    private var webView: GeckoViewWithVirtualCursor? = null
    var session: GeckoSession
    var callback: WebEngineWindowProviderCallback? = null
    val navigationDelegate = MyNavigationDelegate(this)
    val progressDelegate = MyProgressDelegate(this)
    val promptDelegate = MyPromptDelegate(this)
    val contentDelegate = MyContentDelegate(this)
    val permissionDelegate = MyPermissionDelegate(this)
    val historyDelegate = MyHistoryDelegate(this)
    val contentBlockingDelegate = MyContentBlockingDelegate(this)
    val mediaSessionDelegate = MyMediaSessionDelegate()
    val selectionActionDelegate = MySelectionActionDelegate()

    var appHomeContentScriptPortDelegate: AppHomeContentScriptPortDelegate? = null
    var appContentScriptPortDelegate: AppContentScriptPortDelegate? = null
    var appWebExtensionBackgroundPortDelegate: AppWebExtensionBackgroundPortDelegate? = null
    private var webExtObserver: (WebExtension?) -> Unit

    override val url: String?
        get() = navigationDelegate.locationURL
    override var userAgentString: String?
        get() = session.settings.userAgentOverride
        set(value) { session.settings.userAgentOverride = value }

    init {
        Log.d(TAG, "init")
        val settings = AppContext.settings  // Use new accessor

        session = GeckoSession(GeckoSessionSettings.Builder()
            .usePrivateMode(settings.incognitoMode)
            .viewportMode(GeckoSessionSettings.VIEWPORT_MODE_MOBILE)
            .userAgentMode(GeckoSessionSettings.USER_AGENT_MODE_MOBILE)
            .useTrackingProtection(tab.adblock ?: settings.adBlockEnabled)
            .build()
        )
        session.navigationDelegate = navigationDelegate
        session.progressDelegate = progressDelegate
        session.contentDelegate = contentDelegate
        session.promptDelegate = promptDelegate
        session.permissionDelegate = permissionDelegate
        session.historyDelegate = historyDelegate
        session.contentBlockingDelegate = contentBlockingDelegate
        session.mediaSessionDelegate = mediaSessionDelegate
        session.selectionActionDelegate = selectionActionDelegate

        webExtObserver = { ext: WebExtension? ->
            if (ext != null) {
                uiHandler.post {
                    connectToAppWebExtension(ext)
                }
                appWebExtension.unsubscribe(webExtObserver)
            }
        }
        appWebExtension.subscribe(webExtObserver)
    }

    private fun connectToAppWebExtension(extension: WebExtension) {
        Log.d(TAG, "connectToAppWebExtension")
        session.webExtensionController.setMessageDelegate(extension,
            object : MessageDelegate {
                override fun onMessage(nativeApp: String, message: Any,
                                       sender: WebExtension.MessageSender): GeckoResult<Any>? {
                    Log.d(TAG, "onMessage: $nativeApp, $message, $sender")
                    return null
                }

                override fun onConnect(port: WebExtension.Port) {
                    Log.d(TAG, "onConnect: $port")
                    appContentScriptPortDelegate = AppContentScriptPortDelegate(port, this@GeckoWebEngine).also {
                        port.setDelegate(it)
                    }
                }
            }, "browkorftv_content"
        )
        session.webExtensionController.setMessageDelegate(extension,
            object : MessageDelegate {
                override fun onMessage(nativeApp: String, message: Any,
                                       sender: WebExtension.MessageSender): GeckoResult<Any>? {
                    Log.d(TAG, "onMessage: $nativeApp, $message, $sender")
                    return null
                }

                override fun onConnect(port: WebExtension.Port) {
                    Log.d(TAG, "onConnect: $port")
                }
            }, "browkorftv")

        extension.setMessageDelegate(object : MessageDelegate {
            override fun onMessage(nativeApp: String, message: Any,
                                   sender: WebExtension.MessageSender): GeckoResult<Any>? {
                Log.d(TAG, "onMessage: $nativeApp, $message, $sender")
                return null
            }

            override fun onConnect(port: WebExtension.Port) {
                Log.d(TAG, "onConnect: $port")
                appWebExtensionBackgroundPortDelegate = AppWebExtensionBackgroundPortDelegate(port, this@GeckoWebEngine).also {
                    port.setDelegate(it)
                }
            }
        }, "browkorftv_bg")
    }

    override fun getWebEngineName(): String = ENGINE_NAME

    override fun saveState(): Any? {
        Log.d(TAG, "saveState")
        progressDelegate.sessionState?.let {
            return it
        }
        return null
    }

    override fun restoreState(savedInstanceState: Any) {
        Log.d(TAG, "restoreState")
        if (savedInstanceState is SessionState) {
            progressDelegate.sessionState = savedInstanceState
            if (!session.isOpen) {
                session.open(runtime)
            }
            session.restoreState(savedInstanceState)
        } else {
            throw IllegalArgumentException("savedInstanceState is not SessionState")
        }
    }

    override fun stateFromBytes(bytes: ByteArray): Any? {
        val jsString = String(bytes, Charsets.UTF_8)
        return SessionState.fromString(jsString)
    }

    override fun loadUrl(url: String) {
        Log.d(TAG, "loadUrl($url)")
        if (!session.isOpen) {
            session.open(runtime)
        }

        val settings = AppContext.settings

        if (HOME_URL_ALIAS == url) {
            when (settings.homePageModeEnum) {
                HomePageMode.BLANK -> {
                    // nothing to do
                }
                HomePageMode.CUSTOM, HomePageMode.SEARCH_ENGINE -> {
                    session.loadUri(settings.homePage)
                }
                HomePageMode.HOME_PAGE -> {
                    session.loadUri(HOME_PAGE_URL)
                }
            }
        } else {
            session.loadUri(url)
        }
    }

    override fun canGoForward(): Boolean {
        return navigationDelegate.canGoForward
    }

    override fun goForward() {
        session.goForward()
    }

    override fun canZoomIn(): Boolean {
        return true
    }

    override fun zoomIn() {
        //appWebExtensionPortDelegate?.port?.postMessage(JSONObject("{\"action\":\"zoomIn\"}"))
        webView?.cursorDrawerDelegate?.tryZoomIn()
    }

    override fun canZoomOut(): Boolean {
        return true
    }

    override fun zoomOut() {
        //appWebExtensionPortDelegate?.port?.postMessage(JSONObject("{\"action\":\"zoomOut\"}"))
        webView?.cursorDrawerDelegate?.tryZoomOut()
    }

    override fun zoomBy(zoomBy: Float) {
        
    }

    override fun evaluateJavascript(script: String) {
        session.loadUri("javascript:$script")
    }

    override fun setNetworkAvailable(connected: Boolean) {
        //nop
    }

    override fun getView(): View? {
        return webView
    }

    override fun getOrCreateView(activityContext: Context): View {
        Log.d(TAG, "getOrCreateView()")
        if (webView == null) {
            val geckoView = weakRefToSingleGeckoView.get()
            webView = if (geckoView == null) {
                Log.i(TAG, "Creating new GeckoView")
                val wv = GeckoViewWithVirtualCursor(activityContext)
                weakRefToSingleGeckoView = WeakReference(wv)
                wv
            } else {
                geckoView
            }.also { wv ->
                wv.cursorDrawerDelegate.textSelectionCallback = this
                wv.cursorDrawerDelegate.callback = this
            }
        }
        return webView!!
    }

    override fun canGoBack(): Boolean {
        return navigationDelegate.canGoBack
    }

    override fun goBack() {
        session.goBack()
    }

    override fun reload() {
        val settings = AppContext.settings
        val adblockEnabled = tab.adblock ?: settings.adBlockEnabled
        if (session.settings.useTrackingProtection != adblockEnabled) {
            session.settings.useTrackingProtection = adblockEnabled
        }
        session.reload()
    }

    override fun onFilePicked(resultCode: Int, data: Intent?) {
        promptDelegate.onFileCallbackResult(resultCode, data)
    }

    override fun onResume() {
        if (!session.isOpen) {
            session.open(runtime)
            progressDelegate.sessionState?.let {
                session.restoreState(it)
            }
        }
        session.setFocused(true)
    }

    override fun onPause() {
        session.setFocused(false)
        mediaSessionDelegate.mediaSession?.let {
            if (!mediaSessionDelegate.paused) {
                it.pause()
            }
        }
    }

    override fun onUpdateAdblockSetting(newState: Boolean) {
        
    }

    override fun hideFullscreenView() {
        session.exitFullScreen()
    }

    override fun togglePlayback() {
        mediaSessionDelegate.mediaSession?.let {
            if (mediaSessionDelegate.paused) {
                it.play()
            } else {
                it.pause()
            }
        }
    }

    override fun stopPlayback() {
        mediaSessionDelegate.mediaSession?.stop()
    }

    override fun rewind() {
        mediaSessionDelegate.mediaSession?.seekBackward()
    }

    override fun fastForward() {
        mediaSessionDelegate.mediaSession?.seekForward()
    }

    override suspend fun renderThumbnail(bitmap: Bitmap?): Bitmap? {
        return webView?.renderThumbnail(bitmap)
    }

    override fun onAttachToWindow(
        callback: WebEngineWindowProviderCallback,
        parent: ViewGroup, fullscreenViewParent: ViewGroup
    ) {
        Log.d(TAG, "onAttachToWindow()")
        this.callback = callback
        val webView = this.webView ?: throw IllegalStateException("WebView is null")
        val previousSession = webView.session
        if (previousSession != null && previousSession != session) {
            Log.d(TAG, "Closing previous session")
            previousSession.setActive(false)
            webView.releaseSession()
        }
        webView.coverUntilFirstPaint(Color.WHITE)
        webView.setSession(session)
        if (session.isOpen && previousSession != null && previousSession != session) {
            Log.d(TAG, "Activating session")
            session.setActive(true)
            session.reload()
        }
    }

    override fun onDetachFromWindow(completely: Boolean, destroyTab: Boolean) {
        Log.d(TAG, "onDetachFromWindow()")
        callback = null
        val webView = this.webView
        if (webView != null) {
            val session = webView.session
            if (session == this.session) {
                Log.d(TAG, "Closing session")
                session.setActive(false)
                webView.releaseSession()
            }
        }
        if (completely) {
            this.webView = null
        }
        if (destroyTab) {
            Log.d(TAG, "Closing session completely")
            mediaSessionDelegate.mediaSession?.stop()
            session.close()
        }
    }

    override fun trimMemory() {
    }

    override fun onPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray): Boolean {
        return permissionDelegate.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun isSameSession(internalRepresentation: Any): Boolean {
        return internalRepresentation is GeckoSession && internalRepresentation == session
    }

    override fun onTextSelectionStart(x: Int, y: Int) {
        appContentScriptPortDelegate?.clearSelection()
        appContentScriptPortDelegate?.updateSelection(x, y, webView!!.width, webView!!.height)
    }

    override fun onTextSelectionMove(x: Int, y: Int) {
        appContentScriptPortDelegate?.updateSelection(x, y, webView!!.width, webView!!.height)
    }

    override fun onTextSelectionEnd(x: Int, y: Int) {
        appContentScriptPortDelegate?.processSelection { selectedText: String, editable: Boolean ->
            callback?.onSelectedTextActionRequested(selectedText, editable)
        }
    }

    override fun onTextSelectionCancel() {
        appContentScriptPortDelegate?.clearSelection()
    }

    override fun replaceSelection(newText: String) {
        appContentScriptPortDelegate?.replaceSelection(newText)
    }

    override fun onLongPress(x: Int, y: Int) {
        callback?.onContextMenu(webView!!.cursorDrawerDelegate, navigationDelegate.locationURL,
            null, null, null, null, null, x, y)
    }
}