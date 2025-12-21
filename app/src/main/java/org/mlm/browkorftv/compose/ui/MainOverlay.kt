package org.mlm.browkorftv.compose.ui

import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.mlm.browkorftv.activity.main.BrowserUiViewModel
import org.mlm.browkorftv.activity.main.TabsViewModel
import org.mlm.browkorftv.compose.ui.components.*
import org.mlm.browkorftv.compose.ui.theme.AppTheme
import org.mlm.browkorftv.model.WebTabState

@Composable
fun MainOverlay(
    uiVm: BrowserUiViewModel,
    tabsVm: TabsViewModel,
    onNavigate: (String) -> Unit,
    onMenuAction: (String) -> Unit, // "history", "downloads", "settings" etc.
    onTabSelected: (WebTabState) -> Unit,
    onCloseTab: (WebTabState) -> Unit,
    onAddTab: () -> Unit,
    onBack: () -> Unit,
    onForward: () -> Unit,
    onRefresh: () -> Unit,
    onHome: () -> Unit,
    onToggleAdBlock: () -> Unit,
    onTogglePopupBlock: () -> Unit,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    onCursorMenuAction: (CursorMenuAction) -> Unit,
    onDismissLinkActions: () -> Unit,
    onLinkAction: (LinkAction) -> Unit,
    getLinkCapabilities: () -> Pair<Boolean, Boolean>, // (canOpenUrlActions, canCopyShare)
) {
    val uiState by uiVm.uiState.collectAsStateWithLifecycle()
    val tabs by tabsVm.tabsStates.collectAsStateWithLifecycle()
    val currentTab by tabsVm.currentTab.collectAsStateWithLifecycle()

    AppTheme {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            AnimatedVisibility(
                visible = uiState.isMenuVisible && !uiState.isFullscreen,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                ) {
                    // Thumbnail in center
                    uiState.currentThumbnail?.let { bitmap ->
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier
                                .align(Alignment.Center)
                                .fillMaxWidth(),
//                                .aspectRatio(16f / 9f)
//                                .clip(RoundedCornerShape(8.dp))
//                                .border(
//                                    2.dp,
//                                    Color.White.copy(alpha = 0.3f),
//                                    RoundedCornerShape(8.dp)
//                                ),
//                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }

            // Top Bar Area (ActionBar + Tabs)
            AnimatedVisibility(
                visible = uiState.isMenuVisible && !uiState.isFullscreen,
                enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
                modifier = Modifier.align(Alignment.TopCenter)
            ) {
                Column {
                    // Progress Bar
                    BrowkorfTvProgressBar(progress = uiState.progress / 100f)
                    
                    // Action Bar
                    ActionBar(
                        currentUrl = uiState.url,
                        isIncognito = uiState.isIncognito,
                        onClose = { onMenuAction("close") },
                        onVoiceSearch = { onMenuAction("voice") },
                        onHistory = { onMenuAction("history") },
                        onFavorites = { onMenuAction("favorites") },
                        onDownloads = { onMenuAction("downloads") },
                        onIncognitoToggle = { onMenuAction("incognito") },
                        onSettings = { onMenuAction("settings") },
                        onUrlSubmit = onNavigate
                    )
                    
                    // Tabs
                    TabsRow(
                        tabs = tabs,
                        currentTabId = currentTab?.id,
                        onSelectTab = onTabSelected,
                        onAddTab = onAddTab
                    )
                }
            }

            // Bottom Bar Area
            AnimatedVisibility(
                visible = uiState.isMenuVisible && !uiState.isFullscreen,
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
                    popupBlockEnabled = true, // Simplified for now
                    blockedPopupsCount = uiState.blockedPopups,
                    onCloseTab = { currentTab?.let { onCloseTab(it) } },
                    onBack = onBack,
                    onForward = onForward,
                    onRefresh = onRefresh,
                    onZoomIn = onZoomIn,
                    onZoomOut = onZoomOut,
                    onToggleAdBlock = onToggleAdBlock,
                    onTogglePopupBlock = onTogglePopupBlock,
                    onHome = onHome
                )
            }
            NotificationHost(uiState.notification)

            if (uiState.isCursorMenuVisible && uiState.isMenuVisible && !uiState.isFullscreen) {
                CursorRadialMenu(
                    xPx = uiState.cursorMenuX,
                    yPx = uiState.cursorMenuY,
                    onAction = { action -> onCursorMenuAction(action) }
                )
            }

            val linkCaps = getLinkCapabilities()

            if (uiState.isLinkActionsVisible && !uiState.isFullscreen) {
                LinkActionsDialog(
                    canOpenUrlActions = linkCaps.first,   // computed by activity based on last link url
                    canCopyShare = linkCaps.second,
                    onDismiss = { onDismissLinkActions() },
                    onAction = { onLinkAction(it) }
                )
            }
        }
    }
}