package com.phlox.tvwebbrowser.compose.ui

import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
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
    onZoomOut: () -> Unit,
) {
    val uiState by uiVm.uiState.collectAsStateWithLifecycle()
    val tabs by tabsVm.tabsStates.collectAsStateWithLifecycle()
    val currentTab by tabsVm.currentTab.collectAsStateWithLifecycle()

    TvBroTheme {
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
                    TvBroProgressBar(progress = (uiState.progress / 100).toFloat())
                    
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
        }
    }
}