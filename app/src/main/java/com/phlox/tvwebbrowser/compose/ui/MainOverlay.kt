package com.phlox.tvwebbrowser.compose.ui

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.phlox.tvwebbrowser.activity.main.BrowserUiViewModel
import com.phlox.tvwebbrowser.activity.main.TabsViewModel
import com.phlox.tvwebbrowser.compose.ui.components.*
import com.phlox.tvwebbrowser.compose.ui.theme.TvBroTheme
import com.phlox.tvwebbrowser.model.WebTabState
import org.koin.androidx.compose.koinViewModel

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
    onZoomOut: () -> Unit
) {
    val uiState by uiVm.uiState.collectAsStateWithLifecycle()
    val tabs by tabsVm.tabsStates.collectAsStateWithLifecycle()
    val currentTab by tabsVm.currentTab.collectAsStateWithLifecycle()

    TvBroTheme {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            // 1. Top Bar Area (ActionBar + Tabs)
            AnimatedVisibility(
                visible = uiState.isMenuVisible && !uiState.isFullscreen,
                enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
                modifier = Modifier.align(Alignment.TopCenter)
            ) {
                Column {
                    // Progress Bar
                    TvBroProgressBar(progress = uiState.progress)
                    
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

            // 2. Bottom Bar Area
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
        }
    }
}