package org.mlm.browkorftv.activity.main

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.*
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.*
import android.provider.Settings
import android.util.Log
import android.util.Patterns
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.webkit.CookieManager
import android.webkit.MimeTypeMap
import android.webkit.URLUtil
import android.webkit.WebStorage
import android.widget.Toast
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.webkit.URLUtilCompat
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.mlm.browkorftv.R
import org.mlm.browkorftv.activity.IncognitoModeMainActivity
import org.mlm.browkorftv.compose.ComposeMenuActivity
import org.mlm.browkorftv.compose.ui.MainOverlay
import org.mlm.browkorftv.compose.ui.components.CursorMenuAction
import org.mlm.browkorftv.compose.ui.components.LinkAction
import org.mlm.browkorftv.model.Download
import org.mlm.browkorftv.model.HostConfig
import org.mlm.browkorftv.model.WebTabState
import org.mlm.browkorftv.service.downloads.DownloadService
import org.mlm.browkorftv.settings.AppSettings
import org.mlm.browkorftv.settings.AppSettings.Companion.HOME_PAGE_URL
import org.mlm.browkorftv.settings.SettingsManager
import org.mlm.browkorftv.singleton.shortcuts.ShortcutMgr
import org.mlm.browkorftv.updates.UpdateDialogs
import org.mlm.browkorftv.updates.UpdatesEvent
import org.mlm.browkorftv.updates.UpdatesViewModel
import org.mlm.browkorftv.utils.EdgeToEdgeViews
import org.mlm.browkorftv.utils.Utils
import org.mlm.browkorftv.utils.VoiceSearchHelper
import org.mlm.browkorftv.webengine.WebEngine
import org.mlm.browkorftv.webengine.WebEngineFactory
import org.mlm.browkorftv.webengine.WebEngineWindowProviderCallback
import org.mlm.browkorftv.widgets.cursor.CursorDrawerDelegate
import org.mlm.browkorftv.widgets.cursor.CursorLayout
import java.io.File
import java.io.InputStream
import java.io.UnsupportedEncodingException
import java.net.URL
import java.net.URLEncoder
import java.util.Locale
import kotlin.system.exitProcess

open class MainActivity : AppCompatActivity() {

    companion object {
        private val TAG = MainActivity::class.java.simpleName
        const val MY_PERMISSIONS_REQUEST_POST_NOTIFICATIONS_ACCESS = 10003
        const val MY_PERMISSIONS_REQUEST_EXTERNAL_STORAGE_ACCESS = 10004
        const val KEY_PROCESS_ID_TO_KILL = "proc_id_to_kill"
        private const val COMMON_REQUESTS_START_CODE = 10100
        const val ACTION_INSTALL_APK = "org.mlm.browkorftv.ACTION_INSTALL_APK"
        const val EXTRA_FILE_PATH = "file_path_extra"
    }

    // Compose-first: real View containers kept as fields
    private lateinit var webContainer: CursorLayout
    private lateinit var fullscreenContainer: CursorLayout

    // Show a blocking overlay
    private val blockingUi = MutableStateFlow(false)

    // Koin ViewModels
    private val mainViewModel: MainViewModel by viewModel()
    private val tabsViewModel: TabsViewModel by viewModel()
    private val settingsViewModel: SettingsViewModel by viewModel()
    private val updatesViewModel: UpdatesViewModel by viewModel()
    private val browserUiViewModel: BrowserUiViewModel by viewModel()
    private val favoritesViewModel: FavoritesViewModel by viewModel()

    // Singletons / Managers
    private val adBlockRepository: AdBlockRepository by inject()
    private val downloadsManager: DownloadsManager by inject()
    protected val settingsManager: SettingsManager by inject()
    private val shortcutMgr: ShortcutMgr by inject()

    protected val settings: AppSettings get() = settingsManager.current

    private var running: Boolean = false

    private val voiceSearchHelper = VoiceSearchHelper(this)
    private var lastCommonRequestsCode = COMMON_REQUESTS_START_CODE
    private var downloadService: DownloadService? = null
    private var downloadIntent: Download? = null
    var openUrlInExternalAppDialog: AlertDialog? = null

    private var pendingApkToInstall: File? = null

    private val filePickerLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            tabsViewModel.currentTab.value?.webEngine?.onFilePicked(result.resultCode, result.data)
        }

    private val menuActivityLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val url = result.data?.getStringExtra(ComposeMenuActivity.KEY_PICKED_URL)
                if (!url.isNullOrBlank()) navigate(url)
                browserUiViewModel.setMenuVisibility(false)
            }
        }

    private val unknownSourcesLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (packageManager.canRequestPackageInstalls()) {
                pendingApkToInstall?.let { file ->
                    launchInstallAPKIntent(file)
                }
            }
            pendingApkToInstall = null
        }

    private var networkCallback: ConnectivityManager.NetworkCallback? = null

    private data class ContextMenuCtx(
        val tab: WebTabState,
        val windowProvider: WebEngineWindowProviderCallback,
        val cursorDrawer: CursorDrawerDelegate,
        val baseUri: String?,
        val linkUri: String?,
        val srcUri: String?
    )

    private var lastCtxMenu: ContextMenuCtx? = null

    private fun currentLinkUrl(): String? {
        val ctx = lastCtxMenu ?: return null
        return (ctx.linkUri ?: ctx.srcUri)?.trim('"')
    }

    val cursorActionHandler: (CursorMenuAction) -> Unit = { action ->
        val ctx = lastCtxMenu
        when (action) {
            CursorMenuAction.Dismiss -> {
                browserUiViewModel.hideCursorMenu()
                lastCtxMenu = null
            }
            CursorMenuAction.Grab -> {
                ctx?.cursorDrawer?.goToGrabMode()
                browserUiViewModel.hideCursorMenu()
            }
            CursorMenuAction.TextSelect -> {
                ctx?.cursorDrawer?.goToTextSelectionMode()
                browserUiViewModel.hideCursorMenu()
            }
            CursorMenuAction.ZoomIn -> ctx?.tab?.webEngine?.zoomIn()
            CursorMenuAction.ZoomOut -> ctx?.tab?.webEngine?.zoomOut()
            CursorMenuAction.LinkActions -> browserUiViewModel.showLinkActions()
        }
    }

    val linkActionHandler: (LinkAction) -> Unit = let@{ action ->
        val ctx = lastCtxMenu ?: return@let
        val url = currentLinkUrl()
        when (action) {
            LinkAction.Refresh -> ctx.tab.webEngine.reload()
            LinkAction.OpenInNewTab -> if (url != null) ctx.windowProvider.onOpenInNewTabRequested(url, true)
            LinkAction.OpenExternal -> if (url != null) ctx.windowProvider.onOpenInExternalAppRequested(url)
            LinkAction.Download -> if (url != null) ctx.windowProvider.onDownloadRequested(url)
            LinkAction.Copy -> if (url != null) ctx.windowProvider.onCopyTextToClipboardRequested(url)
            LinkAction.Share -> if (url != null) ctx.windowProvider.onShareUrlRequested(url)
        }
        browserUiViewModel.hideLinkActions()
    }

    private fun handleInstallRequest(file: File) {
        if (!packageManager.canRequestPackageInstalls()) {
            pendingApkToInstall = file
            val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                data = "package:$packageName".toUri()
            }
            unknownSourcesLauncher.launch(intent)
        } else {
            launchInstallAPKIntent(file)
        }
    }

    private fun launchInstallAPKIntent(file: File) {
        val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(file.extension)
        val apkURI = FileProvider.getUriForFile(
            this,
            applicationContext.packageName + ".provider",
            file
        )

        val install = Intent(Intent.ACTION_INSTALL_PACKAGE).apply {
            setDataAndType(apkURI, mimeType)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        try {
            startActivity(install)
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(this, R.string.error, Toast.LENGTH_SHORT).show()
        }
    }

    private fun registerNetworkCallback() {
        val cm = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager

        val cb = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) = updateNetworkState(true)
            override fun onLost(network: Network) = updateNetworkState(false)
        }
        networkCallback = cb
        cm.registerDefaultNetworkCallback(cb)

        val isConnected = isNetworkAvailable(cm)
        updateNetworkState(isConnected)
    }

    private fun unregisterNetworkCallback() {
        val cm = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
        val cb = networkCallback ?: return
        runCatching { cm.unregisterNetworkCallback(cb) }
        networkCallback = null
    }

    private fun isNetworkAvailable(cm: ConnectivityManager): Boolean {
        val network = cm.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private fun updateNetworkState(connected: Boolean) {
        runOnUiThread {
            val tab = tabsViewModel.currentTab.value ?: return@runOnUiThread
            tab.webEngine.setNetworkAvailable(connected)
        }
    }

    // --- IMPORTANT: focus handoff helpers (cursor safety) ---
    private fun focusWeb() {
        // For WebView engine: focus should end up in WebViewEx so CursorLayout can intercept DPAD
        // For Gecko engine: focus should end up in GeckoViewWithVirtualCursor
        tabsViewModel.currentTab.value?.webEngine?.getView()?.requestFocus()
    }

    private fun clearWebFocus() {
        tabsViewModel.currentTab.value?.webEngine?.getView()?.clearFocus()
        webContainer.clearFocus()
        fullscreenContainer.clearFocus()
    }

    @SuppressLint("SetJavaScriptEnabled")
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val incognitoMode = settings.incognitoMode
        Log.d(TAG, "onCreate incognitoMode: $incognitoMode")

        if (incognitoMode xor (this is IncognitoModeMainActivity)) {
            switchProcess(incognitoMode, intent?.extras)
            finish()
            return
        }

        val pidToKill = intent?.getIntExtra(KEY_PROCESS_ID_TO_KILL, -1) ?: -1
        if (pidToKill != -1) Process.killProcess(pidToKill)

        if (incognitoMode) {
            mainViewModel.prepareSwitchToIncognito()
        }

        // --- Create stable CursorLayout containers (no XML) ---
        webContainer = CursorLayout(this).apply {
            // WebView engine uses CursorLayout drawing + key interception
            setWillNotDraw(false)
            isFocusable = true
            isFocusableInTouchMode = true
        }

        fullscreenContainer = CursorLayout(this).apply {
            setWillNotDraw(false)
            setBackgroundColor(android.graphics.Color.BLACK)
            visibility = View.INVISIBLE
            isFocusable = true
            isFocusableInTouchMode = true
        }

        // Back handling (same as before)
        onBackPressedDispatcher.addCallback(this) {
            handleAppBackLogic()
        }

        // Compose-first UI
        setContent {
            val uiState by browserUiViewModel.uiState.collectAsStateWithLifecycle()
            val isBlocking by blockingUi.collectAsStateWithLifecycle()

            // When menu becomes visible: clear Web focus so DPAD goes to Compose
            LaunchedEffect(uiState.isMenuVisible, uiState.isFullscreen) {
                if (uiState.isMenuVisible && !uiState.isFullscreen) {
                    clearWebFocus()
                }
            }

            Box(Modifier.fillMaxSize()) {

                // 1) Web container (bottom layer)
                AndroidView(
                    factory = { webContainer },
                    modifier = Modifier.fillMaxSize()
                )

                // 2) Fullscreen container (middle layer)
                AndroidView(
                    factory = { fullscreenContainer },
                    modifier = Modifier.fillMaxSize()
                )

                // 3) Compose overlay (top layer)
                MainOverlay(
                    uiVm = browserUiViewModel,
                    tabsVm = tabsViewModel,
                    onNavigate = { url -> navigate(url); hideMenuOverlay() },
                    onMenuAction = { action ->
                        when (action) {
                            "history" -> showHistory()
                            "downloads" -> showDownloads()
                            "favorites" -> showFavorites()
                            "settings" -> showSettings()
                            "voice" -> initiateVoiceSearch()
                            "close" -> closeWindow()
                            "incognito" -> toggleIncognitoMode()
                        }
                    },
                    onTabSelected = { tab ->
                        tabsViewModel.changeTab(
                            tab,
                            { t -> createWebView(t) },
                            webContainer,
                            fullscreenContainer,
                            WebEngineCallback(tab)
                        )
                        hideMenuOverlay()
                    },
                    onCloseTab = { tab -> closeTab(tab) },
                    onAddTab = {
                        openInNewTab(settings.homePage, tabsViewModel.tabsStates.value.size)
                    },
                    onBack = { navigateBack() },
                    onForward = { tabsViewModel.currentTab.value?.webEngine?.goForward() },
                    onRefresh = { refresh() },
                    onHome = { navigate(settings.homePage) },
                    onZoomIn = { tabsViewModel.currentTab.value?.webEngine?.zoomIn() },
                    onZoomOut = { tabsViewModel.currentTab.value?.webEngine?.zoomOut() },
                    onToggleAdBlock = { toggleAdBlockForTab() },
                    onTogglePopupBlock = { lifecycleScope.launch { showPopupBlockOptions() } },
                    onCursorMenuAction = cursorActionHandler,
                    onDismissLinkActions = { browserUiViewModel.hideLinkActions() },
                    onLinkAction = linkActionHandler,
                    getLinkCapabilities = {
                        val ctx = lastCtxMenu
                        val url = (ctx?.linkUri ?: ctx?.srcUri)?.trim('"')
                        val hasUrl = !url.isNullOrBlank()
                        val isWebUrl = hasUrl && (url.startsWith("http://") || url.startsWith("https://"))
                        Pair(isWebUrl, hasUrl)
                    },
                )

                voiceSearchHelper.VoiceSearchUI()

                if (isBlocking) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.45f))
                            .focusable(true) // eats DPAD while blocking
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .size(64.dp)
                                .align(androidx.compose.ui.Alignment.Center)
                        )
                    }
                }
            }
        }

        EdgeToEdgeViews.enable(this, findViewById(android.R.id.content))

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {

                launch {
                    settingsManager.userAgentFlow.collectLatest { userAgent ->
                        for (tab in tabsViewModel.tabsStates.value) {
                            tab.webEngine.userAgentString = userAgent
                        }
                    }
                }

                launch {
                    settingsManager.incognitoModeFlow.collectLatest { isIncognito ->
                        browserUiViewModel.setIncognitoMode(isIncognito)
                    }
                }

                launch {
                    settingsManager.adBlockEnabledFlow.collectLatest { isAdBlock ->
                        browserUiViewModel.setAdBlockEnabled(isAdBlock)
                    }
                }

                launch {
                    settingsManager.themeFlow.collectLatest { theme ->
                        WebEngineFactory.onThemeSettingUpdated(theme)
                    }
                }

                launch {
                    settingsManager.keepScreenOnFlow.collectLatest { keepOn ->
                        if (keepOn) window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                        else window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    }
                }

                launch {
                    tabsViewModel.currentTab.collect { tab ->
                        tab?.let { onWebViewUpdated(it) }
                    }
                }

                // If tabs become empty and WebView engine, clear container children
                launch {
                    tabsViewModel.tabsStates.collect { tabs ->
                        if (tabs.isEmpty() && !settings.isWebEngineGecko) {
                            webContainer.removeAllViews()
                        }
                    }
                }

                // Mirror menu visibility into the AndroidView world (for thumbnails + focus)
                launch {
                    browserUiViewModel.uiState.collect { state ->
                        if (state.isMenuVisible) showMenuOverlay(updateVm = false)
                        else hideMenuOverlay(updateVm = false)
                    }
                }

                // Updates events (unchanged)
                launch {
                    updatesViewModel.events.collectLatest { e ->
                        when (e) {
                            is UpdatesEvent.ShowUpdateAvailable -> {
                                UpdateDialogs.showUpdateAvailableDialog(
                                    activity = this@MainActivity,
                                    info = e.info,
                                    onDownload = {
                                        val (dlg, pb) = UpdateDialogs.showDownloadProgressDialog(this@MainActivity)
                                        val job = lifecycleScope.launch {
                                            updatesViewModel.state.collect { st ->
                                                UpdateDialogs.updateProgressBar(pb, st.downloadProgress)
                                                if (!st.isDownloading) {
                                                    dlg.dismiss()
                                                    this.cancel()
                                                }
                                            }
                                        }
                                        dlg.setOnDismissListener { job.cancel() }
                                        updatesViewModel.downloadAndRequestInstall(e.info)
                                    }
                                )
                            }

                            is UpdatesEvent.RequestInstallApk -> handleInstallRequest(e.file)

                            is UpdatesEvent.ToastMessage -> UpdateDialogs.toast(this@MainActivity, e.message)
                        }
                    }
                }
            }
        }

        // Ensure containers are attached before engine initialization.
        webContainer.post { loadState() }
    }

    fun closeWindow() {
        Log.d(TAG, "closeWindow")
        lifecycleScope.launch {
            if (settings.incognitoMode) {
                toggleIncognitoMode(andSwitchProcess = false).join()
            }
            finish()
        }
    }

    fun showDownloads() {
        val intent = Intent(this, ComposeMenuActivity::class.java)
        intent.putExtra(ComposeMenuActivity.EXTRA_START_ROUTE, ComposeMenuActivity.ROUTE_DOWNLOADS)
        startActivity(intent)
    }

    fun showHistory() {
        val intent = Intent(this, ComposeMenuActivity::class.java)
        intent.putExtra(ComposeMenuActivity.EXTRA_START_ROUTE, ComposeMenuActivity.ROUTE_HISTORY)
        menuActivityLauncher.launch(intent)
        browserUiViewModel.setMenuVisibility(false)
    }

    fun showFavorites() {
        val intent = Intent(this, ComposeMenuActivity::class.java)
        intent.putExtra(ComposeMenuActivity.EXTRA_START_ROUTE, ComposeMenuActivity.ROUTE_FAVORITES)
        menuActivityLauncher.launch(intent)
        browserUiViewModel.setMenuVisibility(false)
    }

    fun showSettings() {
        val intent = Intent(this, ComposeMenuActivity::class.java)
        intent.putExtra(ComposeMenuActivity.EXTRA_START_ROUTE, ComposeMenuActivity.ROUTE_SETTINGS)
        startActivity(intent)
    }

    private fun handleAppBackLogic() {
        val ui = browserUiViewModel.uiState.value

        if (ui.isFullscreen) {
            tabsViewModel.currentTab.value?.webEngine?.hideFullscreenView()
            return
        }
        if (ui.isLinkActionsVisible) {
            browserUiViewModel.hideLinkActions()
            return
        }
        if (ui.isCursorMenuVisible) {
            browserUiViewModel.hideCursorMenu()
            lastCtxMenu = null
            return
        }
        if (webContainer.consumeBackIfCursorModeActive()) {
            return
        }
        toggleMenu()
    }

    fun navigateBack(goHomeIfNoHistory: Boolean = false) {
        val currentTab = tabsViewModel.currentTab.value
        if (currentTab != null && currentTab.webEngine.canGoBack()) {
            currentTab.webEngine.goBack()
        } else if (goHomeIfNoHistory) {
            navigate(settings.homePage)
        } else if (!browserUiViewModel.uiState.value.isMenuVisible) {
            browserUiViewModel.setMenuVisibility(true)
        } else {
            browserUiViewModel.setMenuVisibility(false)
        }
    }

    fun refresh() {
        tabsViewModel.currentTab.value?.webEngine?.reload()
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy")
        if (tabsViewModel.tabsStates.value.isNotEmpty()) {
            tabsViewModel.onDetachActivity()
        }
        super.onDestroy()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)

        intent.data?.let { uri ->
            openInNewTab(
                uri.toString(),
                tabsViewModel.tabsStates.value.size,
                needToHideMenuOverlay = true,
                navigateImmediately = true
            )
        }

        if (intent.action == ACTION_INSTALL_APK) {
            val path = intent.getStringExtra(EXTRA_FILE_PATH)
            if (path != null) handleInstallRequest(File(path))
        }
    }

    private fun loadState() = lifecycleScope.launch(Dispatchers.Main) {
        blockingUi.value = true

        WebEngineFactory.initialize(this@MainActivity, webContainer, settingsManager)

        mainViewModel.loadState().join()
        tabsViewModel.loadState().join()

        if (!running) {
            blockingUi.value = false
            return@launch
        }

        val intentUri = intent.data
        val tabs = tabsViewModel.tabsStates.value

        if (intentUri == null) {
            if (tabs.isEmpty()) {
                openInNewTab(settings.homePage, 0, needToHideMenuOverlay = true, navigateImmediately = true)
            } else {
                val selected = tabs.firstOrNull { it.selected } ?: tabs.first()
                changeTab(selected)
            }
        } else {
            openInNewTab(intentUri.toString(), tabs.size, needToHideMenuOverlay = true, navigateImmediately = true)
        }

        val currentTab = tabsViewModel.currentTab.value
        if (currentTab == null || currentTab.url == settings.homePage) {
            browserUiViewModel.setMenuVisibility(true)
        }

        updatesViewModel.checkAutoIfNeeded()

        blockingUi.value = false

        // If menu is not visible after load, ensure web focus is correct
        if (!browserUiViewModel.uiState.value.isMenuVisible) {
            focusWeb()
        }
    }

    private fun openInNewTab(
        url: String?,
        index: Int = 0,
        navigateImmediately: Boolean = true,
        needToHideMenuOverlay: Boolean = true,
    ): WebEngine? {
        if (url == null) return null
        val tab = WebTabState(url = url, incognito = settings.incognitoMode)
        createWebView(tab) ?: return null

        tabsViewModel.addNewTab(tab, index)
        changeTab(tab)

        if (navigateImmediately) navigate(url)
        if (needToHideMenuOverlay) browserUiViewModel.setMenuVisibility(false)

        return tab.webEngine
    }

    private fun closeTab(tab: WebTabState?) {
        if (tab == null) return
        val tabs = tabsViewModel.tabsStates.value
        val position = tabs.indexOf(tab)

        when {
            tabs.size == 1 -> openInNewTab(settings.homePage, 0, needToHideMenuOverlay = true, navigateImmediately = true)
            position > 0 -> changeTab(tabs[position - 1])
            else -> changeTab(tabs[position + 1])
        }

        tabsViewModel.onCloseTab(tab)
        browserUiViewModel.setMenuVisibility(false)
    }

    private fun changeTab(newTab: WebTabState) {
        tabsViewModel.changeTab(
            newTab,
            { tab: WebTabState -> createWebView(tab) },
            webContainer,
            fullscreenContainer,
            WebEngineCallback(newTab)
        )
        // If menu is hidden, keep focus in web so cursor works immediately
        if (!browserUiViewModel.uiState.value.isMenuVisible) {
            focusWeb()
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun createWebView(tab: WebTabState): View? {
        val webView: View = try {
            tab.webEngine.getOrCreateView(this)
        } catch (e: Throwable) {
            e.printStackTrace()
            if (!settings.isWebEngineGecko) {
                AlertDialog.Builder(this)
                    .setTitle(R.string.error)
                    .setCancelable(false)
                    .setMessage(R.string.err_webview_can_not_link)
                    .setNegativeButton(R.string.exit) { _, _ -> finish() }
                    .show()
            }
            return null
        }

        settings.effectiveUserAgent?.let { ua ->
            tab.webEngine.userAgentString = ua
        }
        return webView
    }

    private fun onWebViewUpdated(tab: WebTabState) {
        // Ensure text selection callbacks go to current engine
        webContainer.cursorDrawerDelegate.textSelectionCallback = tab.webEngine
        fullscreenContainer.cursorDrawerDelegate.textSelectionCallback = tab.webEngine

        browserUiViewModel.updateUrl(tab.url)
        browserUiViewModel.updateNavState(tab.webEngine.canGoBack(), tab.webEngine.canGoForward())
        browserUiViewModel.updateAdBlockStats(
            tab.adblock ?: settings.adBlockEnabled,
            tab.blockedAds,
            tab.blockedPopups
        )
    }

    private fun onDownloadRequested(
        url: String,
        referer: String,
        originalDownloadFileName: String,
        userAgent: String?,
        mimeType: String? = null,
        operationAfterDownload: Download.OperationAfterDownload = Download.OperationAfterDownload.NOP,
        base64BlobData: String? = null,
        stream: InputStream? = null,
        size: Long = 0L
    ) {
        downloadIntent = Download(
            url, originalDownloadFileName, null, operationAfterDownload,
            mimeType, referer, userAgent, base64BlobData, stream, size
        )

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R &&
            checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), MY_PERMISSIONS_REQUEST_EXTERNAL_STORAGE_ACCESS)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), MY_PERMISSIONS_REQUEST_POST_NOTIFICATIONS_ACCESS)
        } else {
            startDownload()
        }
    }

    private fun startDownload() {
        val download = this.downloadIntent ?: return
        this.downloadIntent = null
        downloadService?.startDownload(download)
        onDownloadStarted(download.filename)
    }

    override fun onTrimMemory(level: Int) {
        for (tab in tabsViewModel.tabsStates.value) {
            if (!tab.selected) tab.trimMemory()
        }
        super.onTrimMemory(level)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (tabsViewModel.currentTab.value?.webEngine?.onPermissionsResult(requestCode, permissions, grantResults) == true) return
        if (grantResults.isEmpty()) return

        when (requestCode) {
            MY_PERMISSIONS_REQUEST_EXTERNAL_STORAGE_ACCESS,
            MY_PERMISSIONS_REQUEST_POST_NOTIFICATIONS_ACCESS -> {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) startDownload()
            }
            else -> super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }

    override fun onStart() {
        super.onStart()
        bindService(Intent(this, DownloadService::class.java), downloadServiceConnection, BIND_AUTO_CREATE)
    }

    override fun onStop() {
        super.onStop()
        runCatching { unbindService(downloadServiceConnection) }
        downloadService = null
    }

    override fun onResume() {
        running = true
        super.onResume()
        registerNetworkCallback()
        tabsViewModel.currentTab.value?.webEngine?.onResume()
    }

    override fun onPause() {
        tabsViewModel.currentTab.value?.apply {
            webEngine.onPause()
            onPause()
            lifecycleScope.launch { tabsViewModel.saveTab(this@apply) }
        }
        unregisterNetworkCallback()
        super.onPause()
        running = false
    }

    private fun toggleAdBlockForTab() {
        tabsViewModel.currentTab.value?.apply {
            val currentState = adblock ?: settings.adBlockEnabled
            val newState = !currentState
            adblock = newState
            webEngine.onUpdateAdblockSetting(newState)
            onWebViewUpdated(this)
            refresh()
        }
    }

    private suspend fun showPopupBlockOptions() {
        val tab = tabsViewModel.currentTab.value ?: return
        val currentHostConfig = tabsViewModel.findHostConfig(tab, false)
        val currentBlockPopupsLevelValue = currentHostConfig?.popupBlockLevel ?: HostConfig.DEFAULT_BLOCK_POPUPS_VALUE

        val hostName = currentHostConfig?.hostName ?: runCatching { URL(tab.url).host }.getOrDefault("")
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.block_popups_s, hostName))
            .setSingleChoiceItems(R.array.popup_blocking_level, currentBlockPopupsLevelValue) { dialog, itemId ->
                lifecycleScope.launch {
                    tabsViewModel.changePopupBlockingLevel(itemId, tab)
                    dialog.dismiss()
                }
            }
            .show()
    }

    fun navigate(url: String) {
        val tab = tabsViewModel.currentTab.value
        if (tab != null) {
            tab.url = url
            tab.webEngine.loadUrl(url)
        } else {
            openInNewTab(url, 0, needToHideMenuOverlay = true, navigateImmediately = true)
        }
    }

    fun search(aText: String) {
        var text = aText
        val trimmedLowercased = text.trim().lowercase(Locale.ROOT)

        if (Patterns.WEB_URL.matcher(text).matches() ||
            trimmedLowercased.startsWith("http://") ||
            trimmedLowercased.startsWith("https://")
        ) {
            if (!text.lowercase(Locale.ROOT).contains("://")) {
                text = "https://$text"
            }
            navigate(text)
        } else {
            val query = try {
                URLEncoder.encode(text, "utf-8")
            } catch (e: UnsupportedEncodingException) {
                e.printStackTrace()
                Utils.showToast(this, R.string.error)
                return
            }
            val searchUrl = settings.searchEngineURL.replace("[query]", query)
            navigate(searchUrl)
        }
    }

    fun toggleIncognitoMode() {
        toggleIncognitoMode(andSwitchProcess = true)
    }

    private fun toggleIncognitoMode(andSwitchProcess: Boolean) = lifecycleScope.launch(Dispatchers.Main) {
        Log.d(TAG, "toggleIncognitoMode andSwitchProcess: $andSwitchProcess")
        val becomingIncognitoMode = !settings.incognitoMode

        blockingUi.value = true

        if (!becomingIncognitoMode) {
            if (!settings.isWebEngineGecko) {
                withContext(Dispatchers.IO) {
                    WebStorage.getInstance().deleteAllData()
                    CookieManager.getInstance().removeAllCookies(null)
                    CookieManager.getInstance().flush()
                }
                WebEngineFactory.clearCache(this@MainActivity)
            }

            tabsViewModel.onCloseAllTabs().join()

            if (!settings.isWebEngineGecko) {
                mainViewModel.clearIncognitoData().join()
            }
        }

        settingsManager.setIncognitoMode(becomingIncognitoMode)

        blockingUi.value = false

        if (andSwitchProcess) switchProcess(becomingIncognitoMode)
    }

    private fun switchProcess(incognitoMode: Boolean, intentDataToCopy: Bundle? = null) {
        Log.d(TAG, "switchProcess incognitoMode: $incognitoMode")
        val activityClass = if (incognitoMode) IncognitoModeMainActivity::class.java else MainActivity::class.java
        val intent = Intent(this@MainActivity, activityClass)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            .putExtra(KEY_PROCESS_ID_TO_KILL, Process.myPid())

        intentDataToCopy?.let { intent.putExtras(it) }
        startActivity(intent)
        exitProcess(0)
    }

    fun toggleMenu() {
        browserUiViewModel.toggleMenu()
    }

    @SuppressLint("RestrictedApi")
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (shortcutMgr.handle(event, this, tabsViewModel.currentTab.value)) return true
        return super.dispatchKeyEvent(event)
    }

    private fun showMenuOverlay(updateVm: Boolean = true) {
        if (updateVm) browserUiViewModel.setMenuVisibility(true)

        // IMPORTANT: hide the live web content behind the menu (like old XML version)
        webContainer.visibility = View.INVISIBLE
        clearWebFocus()

        val currentTab = tabsViewModel.currentTab.value
        if (currentTab != null) {
            lifecycleScope.launch {
                val thumbnail = currentTab.webEngine.renderThumbnail(currentTab.thumbnail)
                if (thumbnail != null) {
                    currentTab.thumbnail = thumbnail
                    browserUiViewModel.updateThumbnail(thumbnail)
                }
                displayThumbnail(currentTab)
            }
        }
    }

    private suspend fun displayThumbnail(currentTab: WebTabState?) {
        if (currentTab != null) {
            if (tabByTitleIndex(tabsViewModel.tabsStates.value.indexOf(currentTab)) != currentTab) return

            if (currentTab.thumbnailHash != null) {
                withContext(Dispatchers.IO) {
                    val thumbnail = currentTab.loadThumbnail(applicationContext)
                    withContext(Dispatchers.Main) {
                        if (thumbnail != null) currentTab.thumbnail = thumbnail
                    }
                }
            }
        }
    }

    private fun hideMenuOverlay(updateVm: Boolean = true) {
        if (updateVm) browserUiViewModel.setMenuVisibility(false)

        browserUiViewModel.updateThumbnail(null)
        webContainer.visibility = View.VISIBLE

        // CRITICAL: restore focus so cursor reacts instantly
        focusWeb()
    }

    private fun onDownloadStarted(fileName: String) {
        Utils.showToast(
            this,
            getString(
                R.string.download_started,
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).toString() +
                        File.separator + fileName
            )
        )
        browserUiViewModel.setMenuVisibility(false)
    }

    fun initiateVoiceSearch() {
        browserUiViewModel.setMenuVisibility(false)
        voiceSearchHelper.initiateVoiceSearch(object : VoiceSearchHelper.Callback {
            override fun onResult(text: String?) {
                if (text == null) {
                    Utils.showToast(this@MainActivity, getString(R.string.can_not_recognize))
                    return
                }
                search(text)
                browserUiViewModel.setMenuVisibility(false)
            }
        })
    }

    private fun tabByTitleIndex(index: Int): WebTabState? {
        val tabs = tabsViewModel.tabsStates.value
        return if (index >= 0 && index < tabs.size) tabs[index] else null
    }

    private inner class WebEngineCallback(val tab: WebTabState) : WebEngineWindowProviderCallback {
        override fun getActivity(): Activity = this@MainActivity

        override fun onOpenInNewTabRequested(url: String, navigateImmediately: Boolean): WebEngine? {
            var index = tabsViewModel.tabsStates.value.indexOf(tabsViewModel.currentTab.value)
            index = if (index == -1) tabsViewModel.tabsStates.value.size else index + 1
            return openInNewTab(url, index, true, navigateImmediately)
        }

        override fun onDownloadRequested(url: String) {
            val fileName = url.toUri().lastPathSegment
            val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(MimeTypeMap.getFileExtensionFromUrl(url))
            this@MainActivity.onDownloadRequested(url, tab.url, fileName ?: "download", tab.webEngine.userAgentString, mimeType)
        }

        override fun onDownloadRequested(
            url: String,
            referer: String,
            originalDownloadFileName: String?,
            userAgent: String?,
            mimeType: String?,
            operationAfterDownload: Download.OperationAfterDownload,
            base64BlobData: String?,
            stream: InputStream?,
            size: Long,
            contentDisposition: String?
        ) {
            val fileName = (if (contentDisposition != null) URLUtilCompat.getFilenameFromContentDisposition(contentDisposition) else null)
                ?: URLUtilCompat.guessFileName(url, null, mimeType)
            this@MainActivity.onDownloadRequested(url, referer, fileName, userAgent, mimeType, operationAfterDownload, base64BlobData, stream, size)
        }

        override fun onDownloadRequested(
            url: String,
            userAgent: String?,
            contentDisposition: String,
            mimetype: String?,
            contentLength: Long
        ) {
            this@MainActivity.onDownloadRequested(
                url = url,
                referer = tab.url,
                originalDownloadFileName = URLUtilCompat.guessFileName(url, contentDisposition, mimetype),
                userAgent = userAgent,
                mimeType = mimetype,
                size = contentLength
            )
        }

        override fun onProgressChanged(newProgress: Int) {
            browserUiViewModel.updateProgress(newProgress)
        }

        override fun onReceivedTitle(title: String) {
            tab.title = title
            mainViewModel.onTabTitleUpdated(tab)
        }

        override fun requestPermissions(array: Array<String>): Int {
            val requestCode = lastCommonRequestsCode++
            this@MainActivity.requestPermissions(array, requestCode)
            return requestCode
        }

        override fun onShowFileChooser(intent: Intent): Boolean {
            return try {
                filePickerLauncher.launch(intent)
                true
            } catch (_: Exception) {
                false
            }
        }

        override fun onReceivedIcon(icon: Bitmap) { /* optional: wire to favicon store */ }

        override fun shouldOverrideUrlLoading(url: String): Boolean {
            tab.lastLoadingUrl = url
            val uri = try { url.toUri() } catch (_: Exception) { return true }
            if (uri.scheme == null) return true

            if (URLUtil.isNetworkUrl(url) ||
                uri.scheme.equals("javascript", true) ||
                uri.scheme.equals("data", true) ||
                uri.scheme.equals("about", true) ||
                uri.scheme.equals("blob", true)
            ) return false

            if (uri.scheme.equals("intent", true)) {
                onOpenInExternalAppRequested(url)
                return true
            }

            return try {
                val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                if (intent.resolveActivity(packageManager) != null) {
                    runOnUiThread { askUserAndOpenInExternalApp(url, intent) }
                    true
                } else false
            } catch (_: Exception) {
                true
            }
        }

        override fun onPageStarted(url: String?) {
            onWebViewUpdated(tab)
            tab.url = tab.webEngine.url ?: url ?: tab.url
            browserUiViewModel.updateUrl(tab.url)
            tab.blockedAds = 0
            tab.blockedPopups = 0
        }

        override fun onPageFinished(url: String?) {
            if (tabsViewModel.currentTab.value == null) return
            onWebViewUpdated(tab)
            tab.url = tab.webEngine.url ?: url ?: tab.url
            browserUiViewModel.updateUrl(tab.url)

            lifecycleScope.launch {
                val newThumbnail = tab.webEngine.renderThumbnail(tab.thumbnail)
                if (newThumbnail != null) {
                    tab.updateThumbnail(this@MainActivity, newThumbnail)
                    if (browserUiViewModel.uiState.value.isMenuVisible && tab == tabsViewModel.currentTab.value) {
                        displayThumbnail(tab)
                    }
                }
            }
        }

        override fun onPageCertificateError(url: String?) {}

        override fun isAd(url: Uri, acceptHeader: String?, baseUri: Uri): Boolean =
            adBlockRepository.isAd(url, acceptHeader, baseUri)

        override fun isAdBlockingEnabled(): Boolean =
            tabsViewModel.currentTab.value?.adblock ?: settings.adBlockEnabled

        override fun isDialogsBlockingEnabled(): Boolean =
            if (tab.url == HOME_PAGE_URL) false else shouldBlockNewWindow(dialog = true, userGesture = false)

        override fun shouldBlockNewWindow(dialog: Boolean, userGesture: Boolean): Boolean {
            val level = tab.cachedHostConfig?.popupBlockLevel ?: HostConfig.DEFAULT_BLOCK_POPUPS_VALUE
            return when (level) {
                HostConfig.POPUP_BLOCK_NONE -> false
                HostConfig.POPUP_BLOCK_DIALOGS -> dialog
                HostConfig.POPUP_BLOCK_NEW_AUTO_OPENED_TABS -> dialog || !userGesture
                else -> true
            }
        }

        override fun onBlockedAd(uri: String) {
            if (!settings.adBlockEnabled) return
            tab.blockedAds++
            browserUiViewModel.updateAdBlockStats(true, tab.blockedAds, tab.blockedPopups)
        }

        override fun onBlockedDialog(newTab: Boolean) {
            tab.blockedPopups++
            runOnUiThread {
                browserUiViewModel.updateAdBlockStats(true, tab.blockedAds, tab.blockedPopups)
                val msg = getString(if (newTab) R.string.new_tab_blocked else R.string.popup_dialog_blocked)
                browserUiViewModel.showNotification(R.drawable.outline_web_asset_off_24, msg)
            }
        }

        override fun onCreateWindow(dialog: Boolean, userGesture: Boolean): View? {
            if (shouldBlockNewWindow(dialog, userGesture)) {
                onBlockedDialog(!dialog)
                return null
            }
            val newTab = WebTabState(incognito = settings.incognitoMode)
            val webView = createWebView(newTab) ?: return null
            val currentTab = tabsViewModel.currentTab.value ?: return null
            val index = tabsViewModel.tabsStates.value.indexOf(currentTab) + 1
            tabsViewModel.addNewTab(newTab, index)
            changeTab(newTab)
            return webView
        }

        override fun closeWindow(internalRepresentation: Any) {
            for (t in tabsViewModel.tabsStates.value) {
                if (t.webEngine.isSameSession(internalRepresentation)) {
                    closeTab(t)
                    break
                }
            }
        }

        override fun onScaleChanged(oldScale: Float, newScale: Float) {
            tab.scale = newScale
        }

        override fun onCopyTextToClipboardRequested(url: String) {
            val clipBoard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
            clipBoard.setPrimaryClip(ClipData.newPlainText("URL", url))
            Toast.makeText(this@MainActivity, getString(R.string.copied_to_clipboard), Toast.LENGTH_SHORT).show()
        }

        override fun onShareUrlRequested(url: String) {
            val share = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, R.string.share_url)
                putExtra(Intent.EXTRA_TEXT, url)
            }
            runCatching { startActivity(share) }
        }

        override fun onOpenInExternalAppRequested(url: String) {
            val intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME)
            intent.addCategory(Intent.CATEGORY_BROWSABLE)
            if (intent.resolveActivity(packageManager)?.packageName != packageName) {
                runCatching { startActivity(intent) }
            }
        }

        override fun initiateVoiceSearch() = this@MainActivity.initiateVoiceSearch()

        override fun onPrepareForFullscreen() {
            browserUiViewModel.setFullscreen(true)

            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility =
                (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
        }

        override fun onExitFullscreen() {
            browserUiViewModel.setFullscreen(false)

            if (!settings.keepScreenOn) {
                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility =
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN

            // Restore focus to web view after exiting fullscreen
            focusWeb()
        }

        override fun onVisited(url: String) {
            if (!settings.incognitoMode) {
                mainViewModel.logVisitedHistory(
                    tabsViewModel.currentTab.value?.title,
                    url,
                    tabsViewModel.currentTab.value?.faviconHash
                )
            }
        }

        override fun onContextMenu(
            cursorDrawer: CursorDrawerDelegate,
            baseUri: String?,
            linkUri: String?,
            srcUri: String?,
            title: String?,
            altText: String?,
            textContent: String?,
            x: Int,
            y: Int
        ) {
            lastCtxMenu = ContextMenuCtx(tab, this, cursorDrawer, baseUri, linkUri, srcUri)
            browserUiViewModel.showCursorMenu(x, y)

            // If you actually want cursor context menu while browsing (menu hidden),
            // you likely want to open the menu here:
            // browserUiViewModel.setMenuVisibility(true)
        }

        override fun onSelectedTextActionRequested(selectedText: String, editable: Boolean) {
            val clipBoard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
            val actions = mutableListOf(R.string.copy)
            var textInClipboard: String? = null

            if (editable) {
                actions.add(R.string.cut)
                actions.add(R.string.delete)
                val primaryClip = clipBoard.primaryClip
                if (primaryClip != null && primaryClip.itemCount > 0) {
                    actions.add(R.string.paste)
                    textInClipboard = primaryClip.getItemAt(0).text.toString()
                }
            }

            actions.add(R.string.share)
            if (!selectedText.contains("\n")) actions.add(R.string.search)

            AlertDialog.Builder(this@MainActivity)
                .setItems(actions.map { getString(it) }.toTypedArray()) { _, which ->
                    when (actions[which]) {
                        R.string.copy -> {
                            clipBoard.setPrimaryClip(ClipData.newPlainText("text", selectedText))
                            Toast.makeText(this@MainActivity, getString(R.string.copied_to_clipboard), Toast.LENGTH_SHORT).show()
                        }
                        R.string.cut -> {
                            clipBoard.setPrimaryClip(ClipData.newPlainText("text", selectedText))
                            tab.webEngine.replaceSelection("")
                        }
                        R.string.delete -> tab.webEngine.replaceSelection("")
                        R.string.paste -> tab.webEngine.replaceSelection(textInClipboard!!)
                        R.string.share -> {
                            val share = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, selectedText)
                            }
                            runCatching { startActivity(share) }
                                .onFailure {
                                    Toast.makeText(this@MainActivity, R.string.external_app_open_error, Toast.LENGTH_SHORT).show()
                                }
                        }
                        R.string.search -> search(selectedText)
                    }
                }
                .show()
        }
    }

    private fun askUserAndOpenInExternalApp(url: String, intent: Intent) {
        if (openUrlInExternalAppDialog != null) return
        openUrlInExternalAppDialog = AlertDialog.Builder(this)
            .setTitle(R.string.site_asks_to_open_unknown_url)
            .setMessage(getString(R.string.site_asks_to_open_unknown_url_message) + "\n\n" + url)
            .setPositiveButton(R.string.yes) { _, _ -> runCatching { startActivity(intent) } }
            .setNegativeButton(R.string.no, null)
            .setOnDismissListener { openUrlInExternalAppDialog = null }
            .show()
    }

    private val downloadServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            val binder = service as? DownloadService.LocalBinder ?: return
            downloadService = binder.service
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            downloadService = null
        }
    }
}