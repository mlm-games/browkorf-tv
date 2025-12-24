package org.mlm.browkorftv.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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

@Composable
fun MainOverlay(
    uiVm: BrowserUiViewModel,
    tabsVm: TabsViewModel,
    menuVisible: Boolean,

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

    Box(modifier = Modifier.fillMaxSize()) {

        AnimatedVisibility(
            visible = menuVisible && !uiState.isFullscreen,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                uiState.currentThumbnail?.let { bitmap ->
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .fillMaxWidth()
                            .aspectRatio(16f / 9f)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop,
                    )
                }
            }
        }

        // Top Bar Area
        AnimatedVisibility(
            visible = menuVisible && !uiState.isFullscreen,
            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Column {
                BrowkorfTvProgressBar(progress = uiState.progress / 100f)

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
            visible = menuVisible && !uiState.isFullscreen,
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
                onHome = onHome
            )
        }

        // Keep these always (same behavior as your current code)
        NotificationHost(uiState.notification)

        if (uiState.isCursorMenuVisible && !uiState.isFullscreen) {
            CursorRadialMenu(
                xPx = uiState.cursorMenuX,
                yPx = uiState.cursorMenuY,
                onAction = onCursorMenuAction
            )
        }

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