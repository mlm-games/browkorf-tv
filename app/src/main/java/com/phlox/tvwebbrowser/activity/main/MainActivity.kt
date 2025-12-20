package com.phlox.tvwebbrowser.activity.main

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.*
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.ConnectivityManager
import android.net.Uri
import android.os.*
import android.util.Log
import android.util.Patterns
import android.view.*
import android.view.KeyEvent.*
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.webkit.*
import android.widget.FrameLayout
import android.widget.PopupMenu
import android.widget.Toast
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.webkit.URLUtilCompat
import com.phlox.tvwebbrowser.R
import com.phlox.tvwebbrowser.activity.IncognitoModeMainActivity
import com.phlox.tvwebbrowser.activity.downloads.DownloadsManager
import com.phlox.tvwebbrowser.activity.main.dialogs.favorites.FavoriteEditorDialog
import com.phlox.tvwebbrowser.activity.main.view.CursorMenuView
import com.phlox.tvwebbrowser.compose.ComposeMenuActivity
import com.phlox.tvwebbrowser.compose.settings.SettingsViewModel
import com.phlox.tvwebbrowser.compose.ui.MainOverlay
import com.phlox.tvwebbrowser.databinding.ActivityMainBinding
import com.phlox.tvwebbrowser.model.*
import com.phlox.tvwebbrowser.service.downloads.DownloadService
import com.phlox.tvwebbrowser.settings.AppSettings
import com.phlox.tvwebbrowser.settings.AppSettings.Companion.HOME_PAGE_URL
import com.phlox.tvwebbrowser.settings.SettingsManager
import com.phlox.tvwebbrowser.singleton.shortcuts.ShortcutMgr
import com.phlox.tvwebbrowser.utils.*
import com.phlox.tvwebbrowser.webengine.WebEngine
import com.phlox.tvwebbrowser.webengine.WebEngineFactory
import com.phlox.tvwebbrowser.webengine.WebEngineWindowProviderCallback
import com.phlox.tvwebbrowser.widgets.NotificationView
import com.phlox.tvwebbrowser.widgets.cursor.CursorDrawerDelegate
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.io.File
import java.io.InputStream
import java.io.UnsupportedEncodingException
import java.net.URL
import java.net.URLEncoder
import java.util.*
import kotlin.system.exitProcess

open class MainActivity : AppCompatActivity() {
    companion object {
        private val TAG = MainActivity::class.java.simpleName
        const val VOICE_SEARCH_REQUEST_CODE = 10001
        const val MY_PERMISSIONS_REQUEST_POST_NOTIFICATIONS_ACCESS = 10003
        const val MY_PERMISSIONS_REQUEST_EXTERNAL_STORAGE_ACCESS = 10004
        const val PICK_FILE_REQUEST_CODE = 10005
        const val REQUEST_CODE_UNKNOWN_APP_SOURCES = 10007
        const val KEY_PROCESS_ID_TO_KILL = "proc_id_to_kill"
        private const val MY_PERMISSIONS_REQUEST_VOICE_SEARCH_PERMISSIONS = 10008
        private const val COMMON_REQUESTS_START_CODE = 10100
        private const val REQUEST_CODE_MENU_ACTIVITY = 10010
    }

    private lateinit var vb: ActivityMainBinding
    private lateinit var uiHandler: Handler

    // Koin Injections
    private val mainViewModel: MainViewModel by viewModel()
    private val tabsViewModel: TabsViewModel by viewModel()
    private val settingsViewModel: SettingsViewModel by viewModel()
    private val autoUpdateViewModel: AutoUpdateViewModel by viewModel()
    private val browserUiViewModel: BrowserUiViewModel by viewModel()

    private val favoritesViewModel: FavoritesViewModel by viewModel()

    // Singletons / Managers
    private val adBlockRepository: AdBlockRepository by inject()
    private val downloadsManager: DownloadsManager by inject()
    private val settingsManager: SettingsManager by inject()
    private val shortcutMgr: ShortcutMgr by inject()

    private val settings: AppSettings get() = settingsManager.current

    private var running: Boolean = false
    private var isFullscreen: Boolean = false

    private val voiceSearchHelper = VoiceSearchHelper(
        this, VOICE_SEARCH_REQUEST_CODE,
        MY_PERMISSIONS_REQUEST_VOICE_SEARCH_PERMISSIONS
    )
    private var lastCommonRequestsCode = COMMON_REQUESTS_START_CODE
    private var downloadService: DownloadService? = null
    private var downloadIntent: Download? = null
    var openUrlInExternalAppDialog: AlertDialog? = null
    private var linkActionsMenu: PopupMenu? = null

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
        if (pidToKill != -1) {
            Process.killProcess(pidToKill)
        }

        if (incognitoMode) {
            mainViewModel.prepareSwitchToIncognito()
        }

        uiHandler = Handler(Looper.getMainLooper())

        vb = ActivityMainBinding.inflate(layoutInflater)
        setContentView(vb.root)

        onBackPressedDispatcher.addCallback(this) {
            handleAppBackLogic()
        }
        EdgeToEdgeViews.enable(this, vb.rlRoot)

        vb.flWebViewContainer.visibility = View.VISIBLE

        vb.composeUiOverlay.setContent {
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
                        vb.flWebViewContainer,
                        vb.flFullscreenContainer,
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
                onTogglePopupBlock = { lifecycleScope.launch { showPopupBlockOptions() } }
            )
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Settings Observers
                launch {
                    settingsManager.userAgentFlow.collectLatest { userAgent ->
                        for (tab in tabsViewModel.tabsStates.value) {
                            tab.webEngine.userAgentString = userAgent
                        }
                    }
                }

                launch {
                    settingsManager.themeFlow.collectLatest { theme ->
                        WebEngineFactory.onThemeSettingUpdated(theme)
                    }
                }

                launch {
                    settingsManager.keepScreenOnFlow.collectLatest { keepOn ->
                        if (keepOn) {
                            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                        } else {
                            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                        }
                    }
                }

                // Logic Observers
                launch {
                    mainViewModel.homePageLinks.collect {
                        val currentUrl = tabsViewModel.currentTab.value?.url ?: return@collect
                        if (HOME_PAGE_URL == currentUrl) {
                            tabsViewModel.currentTab.value?.webEngine?.reload()
                        }
                    }
                }

                launch {
                    tabsViewModel.currentTab.collect { tab ->
                        tab?.let { onWebViewUpdated(it) }
                    }
                }

                // Keep UI State Synced
                launch {
                    tabsViewModel.tabsStates.collect { tabs ->
                        if (tabs.isEmpty() && !settings.isWebEngineGecko) {
                            vb.flWebViewContainer.removeAllViews()
                        }
                    }
                }

                // Sync Menu Visibility back to Views
                launch {
                    browserUiViewModel.uiState.collect { state ->
                        if (state.isMenuVisible) {
                            showMenuOverlay(false)
                        } else {
                            hideMenuOverlay(false)
                        }
                    }
                }
            }
        }

        loadState()
    }

    private val mConnectivityChangeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val cm = context.getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
            @Suppress("DEPRECATION")
            val activeNetwork = cm.activeNetworkInfo
            @Suppress("DEPRECATION")
            val isConnected = activeNetwork != null && activeNetwork.isConnectedOrConnecting
            val tab = tabsViewModel.currentTab.value ?: return
            tab.webEngine.setNetworkAvailable(isConnected)
        }
    }

    fun closeWindow() {
        Log.d(TAG, "closeWindow")
        lifecycleScope.launch {
            if (settings.incognitoMode) {
                toggleIncognitoMode(false).join()
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
        @Suppress("DEPRECATION")
        startActivityForResult(intent, REQUEST_CODE_MENU_ACTIVITY)
        browserUiViewModel.setMenuVisibility(false)
    }

    fun showFavorites() {
        val intent = Intent(this, ComposeMenuActivity::class.java)
        intent.putExtra(ComposeMenuActivity.EXTRA_START_ROUTE, ComposeMenuActivity.ROUTE_FAVORITES)
        @Suppress("DEPRECATION")
        startActivityForResult(intent, REQUEST_CODE_MENU_ACTIVITY)
        browserUiViewModel.setMenuVisibility(false)
    }

    fun showSettings() {
        val intent = Intent(this, ComposeMenuActivity::class.java)
        intent.putExtra(ComposeMenuActivity.EXTRA_START_ROUTE, ComposeMenuActivity.ROUTE_SETTINGS)
        startActivity(intent)
    }

    private fun handleAppBackLogic() {
        if (isFullscreen) {
            tabsViewModel.currentTab.value?.webEngine?.hideFullscreenView()
            return
        }
        if (vb.vCursorMenu.isVisible) {
            vb.vCursorMenu.close(CursorMenuView.CloseAnimation.ROTATE_OUT)
            return
        }
        if (vb.flWebViewContainer.consumeBackIfCursorModeActive()) {
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

    @SuppressLint("MissingSuperCall")
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        val intentUri = intent.data
        if (intentUri != null) {
            openInNewTab(
                intentUri.toString(), tabsViewModel.tabsStates.value.size,
                needToHideMenuOverlay = true,
                navigateImmediately = true
            )
        }
    }

    private fun loadState() = lifecycleScope.launch(Dispatchers.Main) {
        WebEngineFactory.initialize(this@MainActivity, vb.flWebViewContainer)

        vb.progressBarGeneric.visibility = View.VISIBLE
        vb.progressBarGeneric.requestFocus()

        mainViewModel.loadState().join()
        tabsViewModel.loadState().join()

        if (!running) {
            return@launch
        }

        vb.progressBarGeneric.visibility = View.GONE

        val intentUri = intent.data
        val tabs = tabsViewModel.tabsStates.value

        if (intentUri == null) {
            if (tabs.isEmpty()) {
                openInNewTab(
                    settings.homePage, 0,
                    needToHideMenuOverlay = true,
                    navigateImmediately = true
                )
            } else {
                var foundSelectedTab = false
                for (tab in tabs) {
                    if (tab.selected) {
                        changeTab(tab)
                        foundSelectedTab = true
                        break
                    }
                }
                if (!foundSelectedTab) {
                    changeTab(tabs[0])
                }
            }
        } else {
            openInNewTab(
                intentUri.toString(), tabs.size,
                needToHideMenuOverlay = true,
                navigateImmediately = true
            )
        }

        val currentTab = tabsViewModel.currentTab.value
        if (currentTab == null || currentTab.url == settings.homePage) {
            browserUiViewModel.setMenuVisibility(true)
        }

        if (autoUpdateViewModel.needAutoCheckUpdates &&
            autoUpdateViewModel.updateChecker.versionCheckResult == null &&
            !autoUpdateViewModel.lastUpdateNotificationTime.sameDay(Calendar.getInstance())
        ) {
            autoUpdateViewModel.checkUpdate(false) {
                if (autoUpdateViewModel.updateChecker.hasUpdate()) {
                    autoUpdateViewModel.showUpdateDialogIfNeeded(this@MainActivity)
                }
            }
        }
    }

    private fun openInNewTab(
        url: String?,
        index: Int = 0,
        navigateImmediately: Boolean = false,
        needToHideMenuOverlay: Boolean = true,
    ): WebEngine? {
        if (url == null) return null
        val tab = WebTabState(url = url, incognito = settings.incognitoMode)
        createWebView(tab) ?: return null

        tabsViewModel.addNewTab(tab, index)
        changeTab(tab)

        if (navigateImmediately) {
            navigate(url)
        }
        if (needToHideMenuOverlay) {
            browserUiViewModel.setMenuVisibility(false)
        }
        return tab.webEngine
    }

    private fun closeTab(tab: WebTabState?) {
        if (tab == null) return
        val tabs = tabsViewModel.tabsStates.value
        val position = tabs.indexOf(tab)

        when {
            tabs.size == 1 -> openInNewTab(
                settings.homePage, 0,
                needToHideMenuOverlay = true,
                navigateImmediately = true
            )
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
            vb.flWebViewContainer,
            vb.flFullscreenContainer,
            WebEngineCallback(newTab)
        )
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun createWebView(tab: WebTabState): View? {
        val webView: View
        try {
            webView = tab.webEngine.getOrCreateView(this)
        } catch (e: Throwable) {
            e.printStackTrace()
            if (!settings.isWebEngineGecko) {
                val dialogBuilder = AlertDialog.Builder(this)
                    .setTitle(R.string.error)
                    .setCancelable(false)
                    .setMessage(R.string.err_webview_can_not_link)
                    .setNegativeButton(R.string.exit) { _, _ -> finish() }
                dialogBuilder.show()
            }
            return null
        }

        val ua = settings.effectiveUserAgent
        if (ua != null) {
            tab.webEngine.userAgentString = ua
        }
        return webView
    }

    private fun onWebViewUpdated(tab: WebTabState) {
        vb.flWebViewContainer.cursorDrawerDelegate.textSelectionCallback = tab.webEngine
        vb.flFullscreenContainer.cursorDrawerDelegate.textSelectionCallback = tab.webEngine

        browserUiViewModel.updateUrl(tab.url)
        browserUiViewModel.updateNavState(
            tab.webEngine.canGoBack(),
            tab.webEngine.canGoForward()
        )
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
            requestPermissions(
                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                MY_PERMISSIONS_REQUEST_EXTERNAL_STORAGE_ACCESS
            )
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                MY_PERMISSIONS_REQUEST_POST_NOTIFICATIONS_ACCESS
            )
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
            if (!tab.selected) {
                tab.trimMemory()
            }
        }
        super.onTrimMemory(level)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        if (voiceSearchHelper.processPermissionsResult(requestCode, permissions, grantResults)) {
            return
        }
        if (tabsViewModel.currentTab.value?.webEngine?.onPermissionsResult(
                requestCode, permissions, grantResults
            ) == true
        ) return
        if (grantResults.isEmpty()) return
        when (requestCode) {
            MY_PERMISSIONS_REQUEST_EXTERNAL_STORAGE_ACCESS,
            MY_PERMISSIONS_REQUEST_POST_NOTIFICATIONS_ACCESS -> {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    startDownload()
                }
            }
            else -> super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (voiceSearchHelper.processActivityResult(requestCode, resultCode, data)) {
            return
        }
        when (requestCode) {
            PICK_FILE_REQUEST_CODE -> {
                tabsViewModel.currentTab.value?.webEngine?.onFilePicked(resultCode, data)
            }
            REQUEST_CODE_UNKNOWN_APP_SOURCES -> {
                autoUpdateViewModel.showUpdateDialogIfNeeded(this)
            }
            REQUEST_CODE_MENU_ACTIVITY -> if (resultCode == RESULT_OK) {
                val url = data?.getStringExtra(ComposeMenuActivity.KEY_PICKED_URL)
                if (!url.isNullOrBlank()) navigate(url)
                browserUiViewModel.setMenuVisibility(false)
            }
            else -> @Suppress("DEPRECATION") super.onActivityResult(requestCode, resultCode, data)
        }
    }

    override fun onStart() {
        super.onStart()
        bindService(
            Intent(this, DownloadService::class.java),
            downloadServiceConnection,
            BIND_AUTO_CREATE
        )
    }

    override fun onStop() {
        super.onStop()
        unbindService(downloadServiceConnection)
        downloadService = null
    }

    override fun onResume() {
        running = true
        super.onResume()
        @Suppress("DEPRECATION")
        val intentFilter = IntentFilter("android.net.conn.CONNECTIVITY_CHANGE")
        registerReceiver(mConnectivityChangeReceiver, intentFilter)
        tabsViewModel.currentTab.value?.webEngine?.onResume()
    }

    override fun onPause() {
        unregisterReceiver(mConnectivityChangeReceiver)
        tabsViewModel.currentTab.value?.apply {
            webEngine.onPause()
            onPause()
            runBlocking { tabsViewModel.saveTab(this@apply) }
        }

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
        val currentBlockPopupsLevelValue =
            currentHostConfig?.popupBlockLevel ?: HostConfig.DEFAULT_BLOCK_POPUPS_VALUE
        val hostName = currentHostConfig?.hostName ?: try {
            URL(tab.url).host
        } catch (e: Exception) {
            ""
        }
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
        val trimmedLowercased = text.trim { it <= ' ' }.lowercase(Locale.ROOT)
        if (Patterns.WEB_URL.matcher(text).matches() ||
            trimmedLowercased.startsWith("http://") ||
            trimmedLowercased.startsWith("https://")
        ) {
            if (!text.lowercase(Locale.ROOT).contains("://")) {
                text = "https://$text"
            }
            navigate(text)
        } else {
            var query: String?
            try {
                query = URLEncoder.encode(text, "utf-8")
            } catch (e1: UnsupportedEncodingException) {
                e1.printStackTrace()
                Utils.showToast(this, R.string.error)
                return
            }

            val searchUrl = settings.searchEngineURL.replace("[query]", query!!)
            navigate(searchUrl)
        }
    }

    fun toggleIncognitoMode() {
        toggleIncognitoMode(true)
    }

    private fun toggleIncognitoMode(andSwitchProcess: Boolean) = lifecycleScope.launch(Dispatchers.Main) {
        Log.d(TAG, "toggleIncognitoMode andSwitchProcess: $andSwitchProcess")
        val becomingIncognitoMode = !settings.incognitoMode

        vb.progressBarGeneric.visibility = View.VISIBLE

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

        vb.progressBarGeneric.visibility = View.GONE

        settingsManager.setIncognitoMode(becomingIncognitoMode)

        if (andSwitchProcess) {
            switchProcess(becomingIncognitoMode)
        }
    }

    private fun switchProcess(incognitoMode: Boolean, intentDataToCopy: Bundle? = null) {
        Log.d(TAG, "switchProcess incognitoMode: $incognitoMode")
        val activityClass = if (incognitoMode) IncognitoModeMainActivity::class.java
        else MainActivity::class.java
        val intent = Intent(this@MainActivity, activityClass)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        intent.putExtra(KEY_PROCESS_ID_TO_KILL, Process.myPid())
        intentDataToCopy?.let {
            intent.putExtras(it)
        }
        startActivity(intent)
        exitProcess(0)
    }

    fun toggleMenu() {
        browserUiViewModel.toggleMenu()
    }

    @SuppressLint("RestrictedApi")
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (shortcutMgr.handle(event, this, tabsViewModel.currentTab.value)) {
            return true
        }
        return super.dispatchKeyEvent(event)
    }

    private fun showMenuOverlay(updateVm: Boolean = true) {
        if (updateVm) browserUiViewModel.setMenuVisibility(true)

        vb.flWebViewContainer.visibility = View.INVISIBLE

        val currentTab = tabsViewModel.currentTab.value
        if (currentTab != null) {
            lifecycleScope.launch {
                currentTab.thumbnail = currentTab.webEngine.renderThumbnail(currentTab.thumbnail)
                displayThumbnail(currentTab)
            }
        }
    }

    private suspend fun displayThumbnail(currentTab: WebTabState?) {
        if (currentTab != null) {
            if (tabByTitleIndex(tabsViewModel.tabsStates.value.indexOf(currentTab)) != currentTab) return

            if (currentTab.thumbnailHash != null) {
                withContext(Dispatchers.IO) {
                    val thumbnail = currentTab.loadThumbnail()
                    withContext(Dispatchers.Main) {
                        if (thumbnail != null) {
                            currentTab.thumbnail = thumbnail
                        }
                    }
                }
            }
        }
    }

    private fun hideMenuOverlay(updateVm: Boolean = true) {
        if (updateVm) browserUiViewModel.setMenuVisibility(false)
        vb.flWebViewContainer.visibility = View.VISIBLE
        tabsViewModel.currentTab.value?.webEngine?.getView()?.requestFocus()
    }

    private fun onDownloadStarted(fileName: String) {
        Utils.showToast(
            this, getString(
                R.string.download_started,
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                    .toString() + File.separator + fileName
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

    private fun onEditHomePageBookmark(favoriteItem: FavoriteItem) {
        FavoriteEditorDialog(this, object : FavoriteEditorDialog.Callback {
            override fun onDone(item: FavoriteItem) {
                mainViewModel.onHomePageLinkEdited(item)
            }
        }, favoriteItem).show()
    }

    // Helper to find tab by index
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
            url: String, referer: String, originalDownloadFileName: String?, userAgent: String?,
            mimeType: String?, operationAfterDownload: Download.OperationAfterDownload,
            base64BlobData: String?, stream: InputStream?, size: Long, contentDisposition: String?
        ) {
            val fileName = (if (contentDisposition != null) URLUtilCompat.getFilenameFromContentDisposition(contentDisposition) else null)
                ?: URLUtilCompat.guessFileName(url, null, mimeType)
            this@MainActivity.onDownloadRequested(url, referer, fileName, userAgent, mimeType, operationAfterDownload, base64BlobData, stream, size)
        }

        override fun onDownloadRequested(url: String, userAgent: String?, contentDisposition: String, mimetype: String?, contentLength: Long) {
            this@MainActivity.onDownloadRequested(
                url = url, referer = tab.url,
                originalDownloadFileName = URLUtilCompat.guessFileName(url, contentDisposition, mimetype),
                userAgent = userAgent, mimeType = mimetype, size = contentLength
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
            try {
                @Suppress("DEPRECATION")
                startActivityForResult(intent, PICK_FILE_REQUEST_CODE)
            } catch (e: ActivityNotFoundException) {
                return false
            }
            return true
        }

        override fun onReceivedIcon(icon: Bitmap) { /* Handled by Flows */ }

        override fun shouldOverrideUrlLoading(url: String): Boolean {
            tab.lastLoadingUrl = url
            val uri = try { url.toUri() } catch (e: Exception) { return true }
            if (uri.scheme == null) return true
            if (URLUtil.isNetworkUrl(url) || uri.scheme.equals("javascript", true) ||
                uri.scheme.equals("data", true) || uri.scheme.equals("about", true) ||
                uri.scheme.equals("blob", true)
            ) return false
            if (uri.scheme.equals("intent", true)) {
                onOpenInExternalAppRequested(url)
                return true
            }
            return try {
                val intent = Intent(Intent.ACTION_VIEW, uri)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                if (intent.resolveActivity(packageManager) != null) {
                    runOnUiThread { askUserAndOpenInExternalApp(url, intent) }
                    true
                } else false
            } catch (e: Exception) { true }
        }

        override fun onPageStarted(url: String?) {
            onWebViewUpdated(tab)
            if (tab.webEngine.url != null) tab.url = tab.webEngine.url!!
            else if (url != null) tab.url = url
            browserUiViewModel.updateUrl(tab.url)
            tab.blockedAds = 0
            tab.blockedPopups = 0
        }

        override fun onPageFinished(url: String?) {
            if (tabsViewModel.currentTab.value == null) return
            onWebViewUpdated(tab)
            if (tab.webEngine.url != null) tab.url = tab.webEngine.url!!
            else if (url != null) tab.url = url
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

        override fun onPageCertificateError(url: String?) {
            // Can update UI state to show error color in address bar
        }

        override fun isAd(url: Uri, acceptHeader: String?, baseUri: Uri): Boolean? = adBlockRepository.isAd(url, acceptHeader, baseUri)

        override fun isAdBlockingEnabled(): Boolean {
            tabsViewModel.currentTab.value?.adblock?.apply { return this }
            return settings.adBlockEnabled
        }

        override fun isDialogsBlockingEnabled(): Boolean = if (tab.url == HOME_PAGE_URL) false else shouldBlockNewWindow(dialog = true, userGesture = false)

        override fun shouldBlockNewWindow(dialog: Boolean, userGesture: Boolean): Boolean {
            val hostConfig = runBlocking(Dispatchers.Main.immediate) { tabsViewModel.findHostConfig(tab, false) }
            val level = hostConfig?.popupBlockLevel ?: HostConfig.DEFAULT_BLOCK_POPUPS_VALUE
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
                NotificationView.showBottomRight(vb.rlRoot, R.drawable.ic_block_popups, msg)
            }
        }

        override fun onCreateWindow(dialog: Boolean, userGesture: Boolean): View? {
            if (shouldBlockNewWindow(dialog, userGesture)) {
                onBlockedDialog(!dialog)
                return null
            }
            val newTab = WebTabState(incognito = settings.incognitoMode)
            val webView = createWebView(newTab) ?: return null
            val currentTab = this@MainActivity.tabsViewModel.currentTab.value ?: return null
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
            try {
                startActivity(share)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        override fun onOpenInExternalAppRequested(url: String) {
            val intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME)
            intent.addCategory(Intent.CATEGORY_BROWSABLE)
            if (intent.resolveActivity(packageManager)?.packageName != packageName) {
                try {
                    startActivity(intent)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        override fun initiateVoiceSearch() = this@MainActivity.initiateVoiceSearch()

        override fun onEditHomePageBookmarkSelected(index: Int) {
            lifecycleScope.launch {
                val bookmark = mainViewModel.homePageLinks.value.firstOrNull { it.order == index }
                var favoriteItem: FavoriteItem? = bookmark?.favoriteId?.let {
                    favoritesViewModel.getFavoriteById(it)
                }

                if (favoriteItem == null) {
                    favoriteItem = FavoriteItem()
                    favoriteItem.title = bookmark?.title
                    favoriteItem.url = bookmark?.url
                    favoriteItem.order = index
                    favoriteItem.homePageBookmark = true
                    onEditHomePageBookmark(favoriteItem)
                } else {
                    AlertDialog.Builder(this@MainActivity)
                        .setTitle(R.string.bookmarks)
                        .setItems(arrayOf(getString(R.string.edit), getString(R.string.delete))) { _, which ->
                            when (which) {
                                0 -> onEditHomePageBookmark(favoriteItem)
                                1 -> mainViewModel.removeHomePageLink(bookmark!!)
                            }
                        }
                        .show()
                }
            }
        }

        override fun getHomePageLinks(): List<HomePageLink> = mainViewModel.homePageLinks.value

        override fun onPrepareForFullscreen() {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
            isFullscreen = true
        }

        override fun onExitFullscreen() {
            if (!settings.keepScreenOn) {
                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility =
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            isFullscreen = false
        }

        override fun onVisited(url: String) {
            if (!settings.incognitoMode) {
                mainViewModel.logVisitedHistory(tabsViewModel.currentTab.value?.title, url, tabsViewModel.currentTab.value?.faviconHash)
            }
        }

        override fun onContextMenu(
            cursorDrawer: CursorDrawerDelegate,
            baseUri: String?, linkUri: String?, srcUri: String?,
            title: String?, altText: String?, textContent: String?,
            x: Int, y: Int
        ) {
            vb.vCursorMenu.show(tab, this, cursorDrawer, baseUri, linkUri, srcUri, title, altText, textContent, x, y)
        }

        override fun suggestActionsForLink(
            baseUri: String?, linkUri: String?, srcUri: String?,
            title: String?, altText: String?, textContent: String?,
            x: Int, y: Int
        ) {
            val url = (linkUri ?: srcUri)?.removePrefix("\"")?.removeSuffix("\"")
            val isHTTPUrl = url != null && (url.startsWith("http://") || url.startsWith("https://"))
            val anchor = View(this@MainActivity).apply {
                layoutParams = FrameLayout.LayoutParams(1, 1).apply { setMargins(x, y, 0, 0) }
            }
            vb.flWebViewContainer.addView(anchor)
            linkActionsMenu = PopupMenu(this@MainActivity, anchor, Gravity.BOTTOM).also {
                it.inflate(R.menu.menu_link)
                it.menu.findItem(R.id.miOpenInNewTab).isVisible = isHTTPUrl
                it.menu.findItem(R.id.miOpenInExternalApp).isVisible = isHTTPUrl
                it.menu.findItem(R.id.miDownload).isVisible = isHTTPUrl
                it.menu.findItem(R.id.miCopyToClipboard).isVisible = url != null
                it.menu.findItem(R.id.miShare).isVisible = url != null
                it.setOnMenuItemClickListener { menuItem ->
                    when (menuItem.itemId) {
                        R.id.miRefreshPage -> tab.webEngine.reload()
                        R.id.miOpenInNewTab -> onOpenInNewTabRequested(url!!, true)
                        R.id.miOpenInExternalApp -> onOpenInExternalAppRequested(url!!)
                        R.id.miDownload -> onDownloadRequested(url!!)
                        R.id.miCopyToClipboard -> onCopyTextToClipboardRequested(url!!)
                        R.id.miShare -> onShareUrlRequested(url!!)
                    }
                    true
                }
                it.setOnDismissListener {
                    vb.flWebViewContainer.removeView(anchor)
                    linkActionsMenu = null
                }
                it.show()
            }
        }

        override fun markBookmarkRecommendationAsUseful(bookmarkOrder: Int) = mainViewModel.markBookmarkRecommendationAsUseful(bookmarkOrder)

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
            if (!selectedText.contains("\n")) {
                actions.add(R.string.search)
            }
            AlertDialog.Builder(this@MainActivity)
                .setItems(actions.map { getString(it) }.toTypedArray()) { _: DialogInterface, which: Int ->
                    val action = actions[which]
                    when (action) {
                        R.string.copy -> {
                            val clipData = ClipData.newPlainText("text", selectedText)
                            clipBoard.setPrimaryClip(clipData)
                            Toast.makeText(this@MainActivity, getString(R.string.copied_to_clipboard), Toast.LENGTH_SHORT).show()
                        }
                        R.string.cut -> {
                            val clipData = ClipData.newPlainText("text", selectedText)
                            clipBoard.setPrimaryClip(clipData)
                            tab.webEngine.replaceSelection("")
                        }
                        R.string.delete -> tab.webEngine.replaceSelection("")
                        R.string.paste -> tab.webEngine.replaceSelection(textInClipboard!!)
                        R.string.share -> {
                            val share = Intent(Intent.ACTION_SEND)
                            share.type = "text/plain"
                            share.putExtra(Intent.EXTRA_TEXT, selectedText)
                            try {
                                startActivity(share)
                            } catch (e: Exception) {
                                e.printStackTrace()
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
            .setPositiveButton(R.string.yes) { _, _ ->
                try {
                    startActivity(intent)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            .setNegativeButton(R.string.no, null)
            .setOnDismissListener { openUrlInExternalAppDialog = null }
            .show()
    }

    private val downloadServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            val binder = service as? DownloadService.LocalBinder ?: return
            downloadService = binder.service
        }

        override fun onServiceDisconnected(p0: ComponentName?) {
            downloadService = null
        }
    }
}