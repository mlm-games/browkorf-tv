package com.phlox.tvwebbrowser.compose.ui

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.tv.material3.Text
import com.phlox.tvwebbrowser.R
import com.phlox.tvwebbrowser.TVBro
import com.phlox.tvwebbrowser.activity.main.view.CursorLayout
import com.phlox.tvwebbrowser.compose.runtime.*
import com.phlox.tvwebbrowser.compose.ui.components.*
import com.phlox.tvwebbrowser.compose.ui.nav.AppKey
import com.phlox.tvwebbrowser.compose.ui.theme.TvBroTheme
import com.phlox.tvwebbrowser.compose.vm.BrowserDataViewModel
import com.phlox.tvwebbrowser.compose.vm.TabsViewModel
import com.phlox.tvwebbrowser.data.AdblockRepository
import com.phlox.tvwebbrowser.webengine.WebEngineFactory
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

@Composable
fun BrowserScreen(
    backStack: NavBackStack<NavKey>,
    platform: ActivityBrowserPlatform,
    downloadsConnector: DownloadServiceConnector,
) {
    val activity = LocalActivity.current
    val context = LocalContext.current
    val colors = TvBroTheme.colors

    val tabsVm: TabsViewModel = koinViewModel()
    val browserDataVm: BrowserDataViewModel = koinViewModel()
    val adblockRepo: AdblockRepository = koinInject()
    val bus: BrowserCommandBus = koinInject()

    // Legacy TVBro: menu mode vs browse mode
    var menuVisible by rememberSaveable { mutableStateOf(false) }

    // Link actions (context menu)
    var pendingLink by remember { mutableStateOf<String?>(null) }

    // Containers for engine views
    var webParent by remember { mutableStateOf<CursorLayout?>(null) }
    var fullscreenParent by remember { mutableStateOf<FrameLayout?>(null) }

    val tabs by tabsVm.tabs.collectAsStateWithLifecycle()
    val currentTab by tabsVm.currentTab.collectAsStateWithLifecycle()

    // Voice search state
    val voiceState by platform.voiceUiState.collectAsStateWithLifecycle()

    // Settings state for UI
    val adblockEnabled by remember {
        derivedStateOf { TVBro.config.adBlockEnabled }
    }
    val incognitoMode by remember {
        derivedStateOf { TVBro.config.incognitoMode }
    }

    val host = remember {
        BrowserHost(
            activity = activity!!,
            tabsVm = tabsVm,
            browserDataVm = browserDataVm,
            downloadsConnector = downloadsConnector,
            platform = platform,
            adblockRepo = adblockRepo,
        )
    }

    // Attach/detach host to platform
    DisposableEffect(host) {
        platform.attachHost(host)
        onDispose { platform.detachHost() }
    }

    fun focusBrowseSurface() {
        val engineView = tabsVm.currentTab.value?.webEngine?.getView()
        if (TVBro.config.isWebEngineGecko()) {
            engineView?.requestFocus()
        } else {
            // WebView engine: CursorLayout must have focus to own DPAD and synthesize touch.
            webParent?.requestFocus()
        }
    }

    LaunchedEffect(Unit) {
        host.startOnce()
        tabsVm.load()
    }

    // Initialize engines once container exists
    LaunchedEffect(webParent) {
        val p = webParent ?: return@LaunchedEffect
        WebEngineFactory.initialize(context, p)
    }

    // Provide containers to host once
    LaunchedEffect(webParent, fullscreenParent) {
        val wp = webParent ?: return@LaunchedEffect
        val fp = fullscreenParent ?: return@LaunchedEffect

        host.setContainers(
            webParent = wp,
            fullscreenParent = fp,
            webViewProvider = { it.webEngine.getOrCreateView(context) },
        )

        host.ensureInitialTabAndAttachIfNeeded()
        currentTab?.let { host.attachTab(it) }
        focusBrowseSurface()
    }

    // Attach on tab changes
    LaunchedEffect(currentTab) {
        currentTab?.let {
            host.attachTab(it)
            if (!menuVisible) focusBrowseSurface()
        }
    }

    val chrome by host.chrome.collectAsStateWithLifecycle()

    // Handle events from web engines
    LaunchedEffect(host) {
        host.events.collect { e ->
            when (e) {
                BrowserUiEvent.OpenDownloads -> backStack.add(AppKey.Downloads)
                BrowserUiEvent.OpenHistory -> backStack.add(AppKey.History)
                BrowserUiEvent.OpenSettings -> backStack.add(AppKey.Settings)

                is BrowserUiEvent.Toast -> platform.toast(e.message)

                is BrowserUiEvent.EditHomePageBookmark -> {
                    backStack.add(AppKey.HomePageSlotEditor(order = e.index))
                }

                is BrowserUiEvent.ShowLinkActions -> {
                    sanitizeHref(e.href)?.let {
                        pendingLink = it
                        menuVisible = true
                    }
                }

                is BrowserUiEvent.HomePageLinksUpdated -> { /* not used */ }
            }
        }
    }

    // Handle global commands (shortcuts, etc.)
    LaunchedEffect(bus, host) {
        bus.events.collect { cmd ->
            when (cmd) {
                is BrowserCommand.Toast -> platform.toast(cmd.message)

                is BrowserCommand.Navigate -> {
                    if (cmd.inNewTab) {
                        host.onOpenInNewTabRequested(cmd.url, navigateImmediately = true)
                    } else {
                        host.searchOrNavigate(cmd.url)
                    }
                }

                is BrowserCommand.AdblockChanged -> {
                    currentTab?.webEngine?.onUpdateAdblockSetting(cmd.enabled)
                    if (TVBro.config.isWebEngineGecko()) currentTab?.webEngine?.reload()
                }

                BrowserCommand.ForceAdblockUpdate -> adblockRepo.forceUpdateNow()

                BrowserCommand.Back -> host.goBack()
                BrowserCommand.Forward -> host.goForward()
                BrowserCommand.Reload -> host.reload()
                BrowserCommand.Home -> host.home()
                BrowserCommand.StartVoiceSearch -> platform.startVoiceSearch()

                BrowserCommand.ToggleQuickMenu -> {
                    menuVisible = !menuVisible
                    if (!menuVisible) focusBrowseSurface()
                }

                BrowserCommand.OpenFavorites -> backStack.add(AppKey.Favorites)
                BrowserCommand.OpenDownloads -> backStack.add(AppKey.Downloads)
                BrowserCommand.OpenHistory -> backStack.add(AppKey.History)
                BrowserCommand.OpenSettings -> backStack.add(AppKey.Settings)
                BrowserCommand.OpenShortcuts -> backStack.add(AppKey.Shortcuts)
                BrowserCommand.OpenAbout -> backStack.add(AppKey.About)
            }
        }
    }

    // Back closes context menu first, then closes menu, then returns to browsing
    BackHandler(enabled = pendingLink != null || menuVisible || voiceState.active) {
        when {
            voiceState.active -> platform.stopVoiceSearch()
            pendingLink != null -> pendingLink = null
            menuVisible -> {
                menuVisible = false
                focusBrowseSurface()
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        // WEB SURFACE (always present, visibility controlled)
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                FrameLayout(ctx).apply {
                    isFocusable = true
                    isFocusableInTouchMode = true
                    descendantFocusability = ViewGroup.FOCUS_AFTER_DESCENDANTS

                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )

                    val cursor = CursorLayout(ctx).also {
                        it.layoutParams = FrameLayout.LayoutParams(
                            FrameLayout.LayoutParams.MATCH_PARENT,
                            FrameLayout.LayoutParams.MATCH_PARENT
                        )
                    }

                    val fs = FrameLayout(ctx).also {
                        it.layoutParams = FrameLayout.LayoutParams(
                            FrameLayout.LayoutParams.MATCH_PARENT,
                            FrameLayout.LayoutParams.MATCH_PARENT
                        )
                        it.visibility = android.view.View.INVISIBLE
                    }

                    addView(cursor)
                    addView(fs)

                    webParent = cursor
                    fullscreenParent = fs
                }
            },
            update = {
                // Menu hides web surface so cursor doesn't fight focus
                webParent?.visibility = if (menuVisible && !chrome.isFullscreen) {
                    android.view.View.INVISIBLE
                } else {
                    android.view.View.VISIBLE
                }
            }
        )

        // Progress bar at very top (always visible when loading)
        if (chrome.progress in 1..99 && !chrome.isFullscreen) {
            TvBroProgressBar(
                progress = chrome.progress,
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
            )
        }

        // MENU MODE UI (web surface hidden/inert)
        if (menuVisible && !chrome.isFullscreen) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Action bar
                ActionBar(
                    currentUrl = chrome.url,
                    isIncognito = incognitoMode,
                    onClose = {
                        menuVisible = false
                        focusBrowseSurface()
                    },
                    onVoiceSearch = { platform.startVoiceSearch() },
                    onHistory = { backStack.add(AppKey.History) },
                    onFavorites = { backStack.add(AppKey.Favorites) },
                    onDownloads = { backStack.add(AppKey.Downloads) },
                    onIncognitoToggle = {
                        TVBro.config.incognitoMode = !TVBro.config.incognitoMode
                    },
                    onSettings = { backStack.add(AppKey.Settings) },
                    onUrlSubmit = { url ->
                        host.searchOrNavigate(url)
                        menuVisible = false
                        focusBrowseSurface()
                    }
                )

                // Tabs row
                TabsRow(
                    tabs = tabs,
                    currentTabId = currentTab?.id,
                    onSelectTab = { tab ->
                        host.attachTab(tab)
                        menuVisible = false
                        focusBrowseSurface()
                    },
                    onAddTab = {
                        host.onOpenInNewTabRequested("about:blank", navigateImmediately = true)
                        menuVisible = false
                        focusBrowseSurface()
                    }
                )

                // Quick menu content
                QuickMenuContent(
                    onClose = {
                        menuVisible = false
                        focusBrowseSurface()
                    },
                    onFavorites = { backStack.add(AppKey.Favorites) },
                    onDownloads = { backStack.add(AppKey.Downloads) },
                    onHistory = { backStack.add(AppKey.History) },
                    onSettings = { backStack.add(AppKey.Settings) },
                    onShortcuts = { backStack.add(AppKey.Shortcuts) },
                    onAbout = { backStack.add(AppKey.About) },
                    modifier = Modifier.weight(1f)
                )

                // Bottom navigation panel
                BottomNavigationPanel(
                    canGoBack = false,//chrome.canGoBack,
                    canGoForward = false, //TODO: chrome.canGoForward,
                    canZoomIn = true,
                    canZoomOut = true,
                    adBlockEnabled = adblockEnabled,
                    blockedAdsCount = chrome.blockedAds,
                    popupBlockEnabled = true,
                    blockedPopupsCount = chrome.blockedPopups,
                    onCloseTab = {
                        currentTab?.let { tabsVm.close(it) }
                    },
                    onBack = { host.goBack() },
                    onForward = { host.goForward() },
                    onRefresh = { host.reload() },
                    onZoomIn = { currentTab?.webEngine?.zoomIn() },
                    onZoomOut = { currentTab?.webEngine?.zoomOut() },
                    onToggleAdBlock = {
                        val newState = !TVBro.config.adBlockEnabled
                        TVBro.config.adBlockEnabled = newState
                        bus.trySend(BrowserCommand.AdblockChanged(newState))
                    },
                    onTogglePopupBlock = {
                        // TODO: toggle popup blocking
                    },
                    onHome = { host.home() }
                )
            }
        }

        // Voice search overlay
        if (voiceState.active) {
            VoiceSearchOverlay(
                state = voiceState,
                onCancel = { platform.stopVoiceSearch() },
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
            )
        }

        // Link actions dialog
        pendingLink?.let { href ->
            LinkActionsDialog(
                href = href,
                onOpen = {
                    pendingLink = null
                    menuVisible = false
                    host.searchOrNavigate(href)
                    focusBrowseSurface()
                },
                onOpenInNewTab = {
                    pendingLink = null
                    menuVisible = false
                    host.onOpenInNewTabRequested(href, navigateImmediately = true)
                    focusBrowseSurface()
                },
                onCopy = {
                    pendingLink = null
                    platform.copyToClipboard(href)
                },
                onShare = {
                    pendingLink = null
                    platform.shareText(href)
                },
                onOpenExternal = {
                    pendingLink = null
                    platform.openExternal(href)
                },
                onDownload = {
                    pendingLink = null
                    host.onDownloadRequested(href)
                },
                onDismiss = { pendingLink = null }
            )
        }
    }
}

/**
 * Quick menu content panel with navigation buttons
 */
@Composable
private fun QuickMenuContent(
    onClose: () -> Unit,
    onFavorites: () -> Unit,
    onDownloads: () -> Unit,
    onHistory: () -> Unit,
    onSettings: () -> Unit,
    onShortcuts: () -> Unit,
    onAbout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = TvBroTheme.colors

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.background)
            .padding(24.dp),
        contentAlignment = Alignment.TopStart
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Menu title
            Text(
                text = stringResource(R.string.menu),
                style = androidx.tv.material3.MaterialTheme.typography.titleLarge,
                color = colors.textPrimary
            )

            Spacer(Modifier.height(8.dp))

            // Menu items in a row layout for TV
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                MenuIconButton(
                    iconRes = R.drawable.ic_star_border_grey_900_36dp,
                    label = stringResource(R.string.favorites),
                    onClick = onFavorites
                )

                MenuIconButton(
                    iconRes = R.drawable.ic_file_download_grey_900,
                    label = stringResource(R.string.downloads),
                    onClick = onDownloads
                )

                MenuIconButton(
                    iconRes = R.drawable.ic_history_grey_900_36dp,
                    label = stringResource(R.string.history),
                    onClick = onHistory
                )

                MenuIconButton(
                    iconRes = R.drawable.ic_settings_grey_900_24dp,
                    label = stringResource(R.string.settings),
                    onClick = onSettings
                )
            }

            Spacer(Modifier.height(16.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                TvBroButton(
                    onClick = onShortcuts,
                    text = stringResource(R.string.shortcuts)
                )

                TvBroButton(
                    onClick = onAbout,
                    text = stringResource(R.string.version_and_updates)
                )
            }

            Spacer(Modifier.height(24.dp))

            TvBroButton(
                onClick = onClose,
                text = stringResource(R.string.close)
            )
        }
    }
}

/**
 * Menu icon button with label below
 */
@Composable
private fun MenuIconButton(
    iconRes: Int,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = TvBroTheme.colors

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        TvBroIconButton(
            onClick = onClick,
            painter = painterResource(iconRes),
            contentDescription = label
        )

        Text(
            text = label,
            color = colors.textSecondary,
            style = androidx.tv.material3.MaterialTheme.typography.bodySmall,
            maxLines = 1
        )
    }
}

private fun sanitizeHref(raw: String?): String? {
    val s = raw?.trim() ?: return null
    if (s.isBlank() || s == "null") return null
    return s.trim('"').takeIf { it.isNotBlank() && it != "null" }
}