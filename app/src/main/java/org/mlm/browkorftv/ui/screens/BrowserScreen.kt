package org.mlm.browkorftv.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import kotlinx.coroutines.delay
import org.mlm.browkorftv.AppKey
import org.mlm.browkorftv.activity.main.BrowserUiViewModel
import org.mlm.browkorftv.activity.main.TabsViewModel
import org.mlm.browkorftv.model.WebTabState
import org.mlm.browkorftv.ui.components.ActionBar
import org.mlm.browkorftv.ui.components.BottomNavigationPanel
import org.mlm.browkorftv.ui.components.BrowkorfTvProgressBar
import org.mlm.browkorftv.ui.components.CursorMenuAction
import org.mlm.browkorftv.ui.components.CursorRadialMenu
import org.mlm.browkorftv.ui.components.LinkAction
import org.mlm.browkorftv.ui.components.LinkActionsDialog
import org.mlm.browkorftv.ui.components.NotificationHost
import org.mlm.browkorftv.ui.components.TabsRow
import org.mlm.browkorftv.widgets.cursor.CursorLayout

@Composable
fun BrowserScreen(
    webContainer: CursorLayout,
    fullscreenContainer: CursorLayout,

    uiVm: BrowserUiViewModel,
    tabsVm: TabsViewModel,
    viewModelStoreOwner: ViewModelStoreOwner,

    isBlocking: Boolean,
    snackbarHostState: SnackbarHostState,

    setCursorEnabled: (Boolean) -> Unit,
    clearWebFocus: () -> Unit,
    focusWeb: () -> Unit,
    postFocusWeb: () -> Unit,
    consumeBackIfCursorModeActive: () -> Boolean,

    showMenuOverlay: () -> Unit,
    hideMenuOverlay: () -> Unit,

    onNavigateOrSearch: (String) -> Unit,
    onCloseWindow: () -> Unit,
    onToggleIncognito: () -> Unit,
    onVoiceSearch: () -> Unit,

    onTabSelected: (WebTabState) -> Unit,
    onCloseTab: (WebTabState) -> Unit,
    onAddTab: () -> Unit,

    onBack: () -> Unit,
    onForward: () -> Unit,
    onRefresh: () -> Unit,
    onHome: () -> Unit,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    onToggleAdBlock: () -> Unit,
    onTogglePopupBlock: () -> Unit,

    onCursorMenuAction: (CursorMenuAction) -> Unit,
    onDismissLinkActions: () -> Unit,
    onLinkAction: (LinkAction) -> Unit,
    getLinkCapabilities: () -> Pair<Boolean, Boolean>,

    voiceSearchUi: @Composable () -> Unit
) {
    val backStack = rememberNavBackStack(AppKey.Browser)
    val uiState by uiVm.uiState.collectAsStateWithLifecycle()
    val currentKey = backStack.lastOrNull()

    // Handle WebView pause/resume when Browser enters/leaves composition
    LaunchedEffect(currentKey) {
        val tab = tabsVm.currentTab.value
        if (currentKey == AppKey.Browser) {
            // Entering Browser destination
            tab?.webEngine?.onResume()
            setCursorEnabled(true)
            postFocusWeb()
        } else {
            // Leaving Browser to another destination
            setCursorEnabled(false)
            clearWebFocus()
            tab?.webEngine?.onPause()
            // Keep engine attached but paused - don't destroy
            // tab?.webEngine?.onDetachFromWindow(completely = false, destroyTab = false)
        }
    }

    // Track menu visibility changes for thumbnail/overlay effects
    LaunchedEffect(uiState.isMenuVisible) {
        if (uiState.isMenuVisible) showMenuOverlay() else hideMenuOverlay()
    }

    LaunchedEffect(currentKey) {
        val tab = tabsVm.currentTab.value
        if (currentKey == AppKey.Browser) {
            // Entering Browser destination
            tab?.webEngine?.onResume()
            // Cursor/focus handled by overlay state effect below
        } else {
            // Leaving Browser to another destination
            setCursorEnabled(false)
            clearWebFocus()
            tab?.webEngine?.onPause()
        }
    }

// Handle cursor and focus based on overlay state (menu, cursor menu, link actions)
    LaunchedEffect(
        currentKey,
        uiState.isMenuVisible,
        uiState.isCursorMenuVisible,
        uiState.isLinkActionsVisible,
        uiState.isFullscreen
    ) {
        if (currentKey != AppKey.Browser) return@LaunchedEffect

        val overlayActive =
            (uiState.isMenuVisible || uiState.isCursorMenuVisible || uiState.isLinkActionsVisible)
                    && !uiState.isFullscreen

        setCursorEnabled(!overlayActive)
        if (overlayActive) {
            clearWebFocus()
        } else {
            postFocusWeb()
        }
    }

    LaunchedEffect(uiState.isMenuVisible) { // Currently useless but later
        if (uiState.isMenuVisible) showMenuOverlay() else hideMenuOverlay()
    }

    NavDisplay(
        backStack = backStack,
        onBack = {
            // When no BackHandler consumes the back press
            if (backStack.size > 1) {
                backStack.removeAt(backStack.lastIndex)
            } else {
                onCloseWindow()
            }
        },
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator(
                viewModelStoreOwner = viewModelStoreOwner,
                removeViewModelStoreOnPop = { true }
            )
        ),
        entryProvider = entryProvider {
            entry<AppKey.Browser> {
                val tabs by tabsVm.tabsStates.collectAsStateWithLifecycle()
                val currentTab by tabsVm.currentTab.collectAsStateWithLifecycle()

                val menuVisible = uiState.isMenuVisible && !uiState.isFullscreen

                val menuFocusRequester = remember { FocusRequester() }

                LaunchedEffect(menuVisible) {
                    if (menuVisible) {
                        delay(100) // Wait for AnimatedVisibility content
                        runCatching { menuFocusRequester.requestFocus() }
                    }
                }

                // Browser-specific back handling
                BackHandler(enabled = true) {
                    when {
                        uiState.isFullscreen -> {
                            tabsVm.currentTab.value?.webEngine?.hideFullscreenView()
                        }

                        uiState.isLinkActionsVisible -> {
                            uiVm.hideLinkActions()
                        }

                        uiState.isCursorMenuVisible -> {
                            uiVm.hideCursorMenu()
                            postFocusWeb()
                        }

                        uiState.isMenuVisible -> {
                            uiVm.hideMenu()
                            postFocusWeb()
                        }

                        consumeBackIfCursorModeActive() -> {
                            // Cursor mode consumed back
                        }

                        else -> {
                            uiVm.showMenu()
                        }
                    }
                }

                Box(Modifier.fillMaxSize()) {
                    // Web container
                    AndroidView(
                        factory = { webContainer },
                        modifier = Modifier.fillMaxSize()
                    )

                    // Fullscreen container
                    AndroidView(
                        factory = { fullscreenContainer },
                        modifier = Modifier.fillMaxSize()
                    )

                    // Top Bar
                    AnimatedVisibility(
                        visible = menuVisible,
                        enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
                        exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
                        modifier = Modifier.align(Alignment.TopCenter)
                    ) {
                        Column {
                            BrowkorfTvProgressBar(progress = uiState.progress / 100f)

                            ActionBar(
                                modifier = Modifier.focusRequester(menuFocusRequester),
                                currentUrl = uiState.url,
                                isIncognito = uiState.isIncognito,
                                onClose = onCloseWindow,
                                onVoiceSearch = onVoiceSearch,
                                onHistory = { backStack.add(AppKey.History) },
                                onFavorites = { backStack.add(AppKey.Favorites) },
                                onDownloads = { backStack.add(AppKey.Downloads) },
                                onIncognitoToggle = onToggleIncognito,
                                onSettings = { backStack.add(AppKey.Settings) },
                                onUrlSubmit = { url ->
                                    onNavigateOrSearch(url)
                                    uiVm.hideMenu()
                                }
                            )

                            TabsRow(
                                tabs = tabs,
                                currentTabId = currentTab?.id,
                                onSelectTab = { tab ->
                                    onTabSelected(tab)
                                    uiVm.hideMenu()
                                },
                                onAddTab = onAddTab
                            )
                        }
                    }

                    // Bottom Bar
                    AnimatedVisibility(
                        visible = menuVisible,
                        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                        modifier = Modifier.align(Alignment.BottomCenter)
                    ) {
                        BottomNavigationPanel(
                            canGoBack = uiState.canGoBack,
                            canGoForward = uiState.canGoForward,
                            canZoomIn = true,
                            canZoomOut = true,
                            adBlockEnabled = uiState.isAdBlockEnabled,
                            blockedAdsCount = uiState.blockedAds,
                            popupBlockEnabled = true,
                            blockedPopupsCount = uiState.blockedPopups,
                            onCloseTab = { currentTab?.let { onCloseTab(it) } },
                            onBack = onBack,
                            onForward = onForward,
                            onRefresh = onRefresh,
                            onZoomIn = onZoomIn,
                            onZoomOut = onZoomOut,
                            onToggleAdBlock = onToggleAdBlock,
                            onTogglePopupBlock = onTogglePopupBlock,
                            onHome = {
                                onHome()
                                uiVm.hideMenu()
                            }
                        )
                    }

                    // Notifications
                    NotificationHost(uiState.notification)

                    // Cursor Menu
                    if (uiState.isCursorMenuVisible && !uiState.isFullscreen) {
                        CursorRadialMenu(
                            xPx = uiState.cursorMenuX,
                            yPx = uiState.cursorMenuY,
                            onAction = onCursorMenuAction
                        )
                    }

                    // Link Actions Dialog
                    val linkCaps = getLinkCapabilities()
                    if (uiState.isLinkActionsVisible && !uiState.isFullscreen) {
                        LinkActionsDialog(
                            canOpenUrlActions = linkCaps.first,
                            canCopyShare = linkCaps.second,
                            onDismiss = onDismissLinkActions,
                            onAction = onLinkAction
                        )
                    }
                }
            }

            entry<AppKey.History> {
                HistoryScreen(
                    onBack = {
                        backStack.removeAt(backStack.lastIndex)
                        uiVm.showMenu() // Show menu when returning
                    },
                    onPickUrl = { url ->
                        onNavigateOrSearch(url)
                        backStack.clear()
                        backStack.add(AppKey.Browser)
                        uiVm.hideMenu()
                    }
                )
            }

            entry<AppKey.Downloads> {
                DownloadsScreen(
                    onBack = {
                        backStack.removeAt(backStack.lastIndex)
                        uiVm.showMenu()
                    }
                )
            }

            entry<AppKey.Favorites> {
                FavoritesScreen(
                    onBack = {
                        backStack.removeAt(backStack.lastIndex)
                        uiVm.showMenu()
                    },
                    onPickUrl = { url ->
                        onNavigateOrSearch(url)
                        backStack.clear()
                        backStack.add(AppKey.Browser)
                        uiVm.hideMenu()
                    },
                    onAddBookmark = { backStack.add(AppKey.BookmarkEditor()) },
                    onEditBookmark = { id -> backStack.add(AppKey.BookmarkEditor(id)) }
                )
            }

            entry<AppKey.Settings> {
                SettingsScreen(
                    onNavigateBack = {
                        backStack.removeAt(backStack.lastIndex)
                        uiVm.showMenu()
                    }
                )
            }

            entry<AppKey.Shortcuts> {
                ShortcutsScreen(
                    onDone = {
                        backStack.removeAt(backStack.lastIndex)
                        uiVm.showMenu()
                    }
                )
            }

            entry<AppKey.BookmarkEditor> { key ->
                BookmarkEditorScreen(
                    id = key.id,
                    onDone = {
                        backStack.removeAt(backStack.lastIndex)
                        // Stay on Favorites if we came from there
                    }
                )
            }
        }
    )

    voiceSearchUi()

    if (isBlocking) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.45f))
                .focusable(true)
        ) {
            CircularProgressIndicator(
                modifier = Modifier
                    .size(64.dp)
                    .align(Alignment.Center)
            )
        }
    }

    // Snackbar
    SnackbarHost(
        hostState = snackbarHostState,
        modifier = Modifier
    )
}