package com.phlox.tvwebbrowser.compose.ui

import android.net.Uri
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavBackStack
import androidx.tv.material3.*
import com.phlox.tvwebbrowser.activity.main.view.CursorLayout
import com.phlox.tvwebbrowser.compose.runtime.ActivityBrowserPlatform
import com.phlox.tvwebbrowser.compose.runtime.BrowserCommand
import com.phlox.tvwebbrowser.compose.runtime.BrowserCommandBus
import com.phlox.tvwebbrowser.compose.runtime.BrowserHost
import com.phlox.tvwebbrowser.compose.runtime.DownloadServiceConnector
import com.phlox.tvwebbrowser.compose.runtime.BrowserUiEvent
import com.phlox.tvwebbrowser.compose.ui.nav.AppKey
import com.phlox.tvwebbrowser.compose.vm.BrowserDataViewModel
import com.phlox.tvwebbrowser.compose.vm.TabsViewModel
import com.phlox.tvwebbrowser.webengine.WebEngineFactory
import com.phlox.tvwebbrowser.data.AdblockRepository
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import androidx.core.net.toUri
import androidx.navigation3.runtime.NavKey

@Composable
fun BrowserScreen(
    backStack: NavBackStack<NavKey>,
    platform: ActivityBrowserPlatform,
    downloadsConnector: DownloadServiceConnector
) {
    val activity = LocalActivity.current
    val context = LocalContext.current

    val tabsVm: TabsViewModel = koinViewModel()
    val browserDataVm: BrowserDataViewModel = koinViewModel()
    val adblockRepo: AdblockRepository = koinInject()
    val bus: BrowserCommandBus = koinInject()

    LaunchedEffect(Unit) {
        browserDataVm.loadOnce()
        adblockRepo.ensureLoaded(forceUpdate = false)
        tabsVm.load()
    }

    val tabs by tabsVm.tabs.collectAsStateWithLifecycle()
    val currentTab by tabsVm.currentTab.collectAsStateWithLifecycle()

    val host = remember {
        BrowserHost(
            activity = activity!!,
            tabsVm = tabsVm,
            browserDataVm = browserDataVm,
            downloadsConnector = downloadsConnector,
            platform = platform,
            adblockRepo = adblockRepo
        )
    }

    // Containers for engine views
    var webParent by remember { mutableStateOf<CursorLayout?>(null) }
    var fullscreenParent by remember { mutableStateOf<FrameLayout?>(null) }

    // UI overlays state
    var showQuickMenu by remember { mutableStateOf(false) }
    var pendingLink by remember { mutableStateOf<String?>(null) }

    // Initialize engines once container exists
    LaunchedEffect(webParent) {
        val p = webParent ?: return@LaunchedEffect
        WebEngineFactory.initialize(context, p)
    }

    // Provide containers to host and attach initial tab once
    LaunchedEffect(webParent, fullscreenParent) {
        val wp = webParent ?: return@LaunchedEffect
        val fp = fullscreenParent ?: return@LaunchedEffect

        host.setContainers(
            webParent = wp,
            fullscreenParent = fp,
            webViewProvider = { it.webEngine.getOrCreateView(context) }
        )
        host.ensureInitialTabAndAttachIfNeeded()
        currentTab?.let { host.attachTab(it) }
    }

    // Attach when current tab changes
    LaunchedEffect(currentTab) {
        currentTab?.let { host.attachTab(it) }
    }

    // Browser chrome state
    val chrome by host.chrome.collectAsStateWithLifecycle()

    // Handle events from WebEngines
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
                    sanitizeHref(e.href)?.let { pendingLink = it }
                }

                is BrowserUiEvent.HomePageLinksUpdated -> {
                    // not used now
                }
            }
        }
    }

    // Handle global commands (Shortcuts, other screens, etc.)
    LaunchedEffect(bus, host) {
        bus.events.collect { cmd ->
            when (cmd) {
                is BrowserCommand.Toast -> platform.toast(cmd.message)
                is BrowserCommand.Navigate -> {
                    if (cmd.inNewTab) host.onOpenInNewTabRequested(cmd.url, navigateImmediately = true)
                    else host.searchOrNavigate(cmd.url)
                }

                // Adblock
                is BrowserCommand.AdblockChanged -> {
                    currentTab?.webEngine?.onUpdateAdblockSetting(cmd.enabled)
                    if (com.phlox.tvwebbrowser.TVBro.config.isWebEngineGecko()) currentTab?.webEngine?.reload()
                }
                BrowserCommand.ForceAdblockUpdate -> {
                    adblockRepo.forceUpdateNow()
                }

                // Browser actions
                BrowserCommand.Back -> host.goBack()
                BrowserCommand.Forward -> host.goForward()
                BrowserCommand.Reload -> host.reload()
                BrowserCommand.Home -> host.home()
                BrowserCommand.StartVoiceSearch -> platform.startVoiceSearch()

                BrowserCommand.ToggleQuickMenu -> showQuickMenu = !showQuickMenu

                BrowserCommand.OpenFavorites -> { showQuickMenu = false; backStack.add(AppKey.Favorites) }
                BrowserCommand.OpenDownloads -> { showQuickMenu = false; backStack.add(AppKey.Downloads) }
                BrowserCommand.OpenHistory -> { showQuickMenu = false; backStack.add(AppKey.History) }
                BrowserCommand.OpenSettings -> { showQuickMenu = false; backStack.add(AppKey.Settings) }
                BrowserCommand.OpenShortcuts -> { showQuickMenu = false; backStack.add(AppKey.Shortcuts) }
                BrowserCommand.OpenAbout -> { showQuickMenu = false; backStack.add(AppKey.About) }
            }
        }
    }

    // Back dismisses overlays first
    BackHandler(enabled = pendingLink != null || showQuickMenu) {
        when {
            pendingLink != null -> pendingLink = null
            showQuickMenu -> showQuickMenu = false
        }
    }

    MaterialTheme {
        Box(Modifier.fillMaxSize()) {

            // Web container
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    FrameLayout(ctx).apply {
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
                }
            )

            // Top chrome
            if (!chrome.isFullscreen) {
                Column(Modifier.fillMaxWidth()) {
                    BrowserTopBar(
                        title = chrome.title.ifBlank { chrome.url },
                        progress = chrome.progress,
                        onBack = { host.goBack() },
                        onForward = { host.goForward() },
                        onReload = { host.reload() },
                        onHome = { host.home() },
                        onVoice = { platform.startVoiceSearch() },
                        onMenu = { showQuickMenu = !showQuickMenu }
                    )

                    TabsRow(
                        tabs = tabs,
                        onSelect = { host.attachTab(it) },
                        onNewTab = { host.onOpenInNewTabRequested("about:blank", navigateImmediately = true) }
                    )
                }
            }

            // Quick Menu overlay
            if (showQuickMenu && !chrome.isFullscreen) {
                QuickMenuOverlay(
                    onClose = { showQuickMenu = false },
                    onFavorites = { bus.trySend(BrowserCommand.OpenFavorites) },
                    onDownloads = { bus.trySend(BrowserCommand.OpenDownloads) },
                    onHistory = { bus.trySend(BrowserCommand.OpenHistory) },
                    onSettings = { bus.trySend(BrowserCommand.OpenSettings) },
                    onShortcuts = { bus.trySend(BrowserCommand.OpenShortcuts) },
                    onAbout = { bus.trySend(BrowserCommand.OpenAbout) }
                )
            }

            // Link actions overlay (Phase 10)
            pendingLink?.let { href ->
                LinkActionsOverlay(
                    href = href,
                    onOpen = {
                        pendingLink = null
                        host.searchOrNavigate(href)
                    },
                    onOpenInNewTab = {
                        pendingLink = null
                        host.onOpenInNewTabRequested(href, navigateImmediately = true)
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
}

@Composable
private fun BrowserTopBar(
    title: String,
    progress: Int,
    onBack: () -> Unit,
    onForward: () -> Unit,
    onReload: () -> Unit,
    onHome: () -> Unit,
    onVoice: () -> Unit,
    onMenu: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        tonalElevation = 2.dp
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(onClick = onBack) { Text("Back") }
            Button(onClick = onForward) { Text("Forward") }
            Button(onClick = onReload) { Text("Reload") }
            Button(onClick = onHome) { Text("Home") }
            Button(onClick = onVoice) { Text("Voice") }
            Button(onClick = onMenu) { Text("Menu") }

            Spacer(Modifier.width(16.dp))

            Column(Modifier.weight(1f)) {
                Text(title, maxLines = 1)
                Text("Progress: $progress%", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun TabsRow(
    tabs: List<com.phlox.tvwebbrowser.model.WebTabState>,
    onSelect: (com.phlox.tvwebbrowser.model.WebTabState) -> Unit,
    onNewTab: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Button(onClick = onNewTab) { Text("+ Tab") }
        tabs.take(6).forEach { tab ->
            val label = tab.title.ifBlank { tab.url }
            Surface(onClick = { onSelect(tab) }, tonalElevation = if (tab.selected) 3.dp else 0.dp) {
                Text(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    text = label.take(18),
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun QuickMenuOverlay(
    onClose: () -> Unit,
    onFavorites: () -> Unit,
    onDownloads: () -> Unit,
    onHistory: () -> Unit,
    onSettings: () -> Unit,
    onShortcuts: () -> Unit,
    onAbout: () -> Unit
) {
    Box(Modifier.fillMaxSize().padding(24.dp)) {
        Surface(tonalElevation = 8.dp, shape = MaterialTheme.shapes.large) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Menu", style = MaterialTheme.typography.titleLarge)
                Button(onClick = onFavorites) { Text("Favorites") }
                Button(onClick = onDownloads) { Text("Downloads") }
                Button(onClick = onHistory) { Text("History") }
                Button(onClick = onSettings) { Text("Settings") }
                Button(onClick = onShortcuts) { Text("Shortcuts") }
                Button(onClick = onAbout) { Text("About & Updates") }
                Button(onClick = onClose) { Text("Close") }
            }
        }
    }
}

@Composable
private fun LinkActionsOverlay(
    href: String,
    onOpen: () -> Unit,
    onOpenInNewTab: () -> Unit,
    onCopy: () -> Unit,
    onShare: () -> Unit,
    onOpenExternal: () -> Unit,
    onDownload: () -> Unit,
    onDismiss: () -> Unit
) {
    val scheme = remember(href) { runCatching { href.toUri().scheme?.lowercase() }.getOrNull() }
    val isHttpLike = scheme == "http" || scheme == "https"

    Box(Modifier.fillMaxSize().padding(24.dp)) {
        Surface(
            tonalElevation = 10.dp,
            shape = MaterialTheme.shapes.large,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Link options", style = MaterialTheme.typography.titleLarge)
                Text(href, maxLines = 2, style = MaterialTheme.typography.bodySmall)

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(onClick = onCopy) { Text("Copy") }
                    Button(onClick = onShare) { Text("Share") }
                }

                if (isHttpLike) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(onClick = onOpen) { Text("Open") }
                        Button(onClick = onOpenInNewTab) { Text("Open in new tab") }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(onClick = onOpenExternal) { Text("Open external") }
                        Button(onClick = onDownload) { Text("Download") }
                    }
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(onClick = onOpenExternal) { Text("Open external") }
                    }
                }

                Spacer(Modifier.height(8.dp))
                Button(onClick = onDismiss) { Text("Cancel") }
            }
        }
    }
}

private fun sanitizeHref(raw: String?): String? {
    val s = raw?.trim() ?: return null
    if (s.isBlank() || s == "null") return null
    return s.trim('"').takeIf { it.isNotBlank() && it != "null" }
}