package com.phlox.tvwebbrowser.compose.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.phlox.tvwebbrowser.R
import com.phlox.tvwebbrowser.compose.ui.theme.TvBroTheme

@Composable
fun BottomNavigationPanel(
    canGoBack: Boolean,
    canGoForward: Boolean,
    canZoomIn: Boolean,
    canZoomOut: Boolean,
    adBlockEnabled: Boolean,
    blockedAdsCount: Int,
    popupBlockEnabled: Boolean,
    blockedPopupsCount: Int,
    onCloseTab: () -> Unit,
    onBack: () -> Unit,
    onForward: () -> Unit,
    onRefresh: () -> Unit,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    onToggleAdBlock: () -> Unit,
    onTogglePopupBlock: () -> Unit,
    onHome: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = TvBroTheme.colors
    
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(60.dp)
            .background(colors.topBarBackground)
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Close tab
        TvBroIconButton(
            onClick = onCloseTab,
            painter = painterResource(R.drawable.ic_close_grey_900_24dp),
            contentDescription = stringResource(R.string.close_tab),
            modifier = Modifier.padding(3.dp)
        )
        
        // Back
        TvBroIconButton(
            onClick = onBack,
            painter = painterResource(
                if (canGoBack) R.drawable.ic_arrow_back_grey_900_24dp 
                else R.drawable.ic_arrow_back_grey_400_24dp
            ),
            contentDescription = stringResource(R.string.navigate_back),
            enabled = canGoBack,
            modifier = Modifier.padding(3.dp)
        )
        
        // Forward
        TvBroIconButton(
            onClick = onForward,
            painter = painterResource(
                if (canGoForward) R.drawable.ic_arrow_forward_grey_900_24dp 
                else R.drawable.ic_arrow_forward_grey_400_24dp
            ),
            contentDescription = stringResource(R.string.navigate_forward),
            enabled = canGoForward,
            modifier = Modifier.padding(3.dp)
        )
        
        // Refresh
        TvBroIconButton(
            onClick = onRefresh,
            painter = painterResource(R.drawable.ic_refresh_grey_900_24dp),
            contentDescription = stringResource(R.string.refresh_page),
            modifier = Modifier.padding(3.dp)
        )
        
        // Zoom in
        TvBroIconButton(
            onClick = onZoomIn,
            painter = painterResource(
                if (canZoomIn) R.drawable.ic_zoom_in_black_24dp 
                else R.drawable.ic_zoom_in_gray_24dp
            ),
            contentDescription = stringResource(R.string.zoom_in),
            enabled = canZoomIn,
            modifier = Modifier.padding(3.dp)
        )
        
        // Zoom out
        TvBroIconButton(
            onClick = onZoomOut,
            painter = painterResource(
                if (canZoomOut) R.drawable.ic_zoom_out_black_24dp 
                else R.drawable.ic_zoom_out_gray_24dp
            ),
            contentDescription = stringResource(R.string.zoom_out),
            enabled = canZoomOut,
            modifier = Modifier.padding(3.dp)
        )
        
        // AdBlock toggle
        TvBroIconButton(
            onClick = onToggleAdBlock,
            painter = painterResource(
                if (adBlockEnabled) R.drawable.ic_adblock_on 
                else R.drawable.ic_adblock_off
            ),
            contentDescription = stringResource(R.string.toggle_ads_blocking),
            checked = adBlockEnabled,
            badgeCount = if (blockedAdsCount > 0) blockedAdsCount else null,
            modifier = Modifier.padding(3.dp)
        )
        
        // Popup block toggle
        TvBroIconButton(
            onClick = onTogglePopupBlock,
            painter = painterResource(R.drawable.ic_block_popups),
            contentDescription = stringResource(R.string.block_popups),
            checked = popupBlockEnabled,
            badgeCount = if (blockedPopupsCount > 0) blockedPopupsCount else null,
            modifier = Modifier.padding(3.dp)
        )
        
        // Home
        TvBroIconButton(
            onClick = onHome,
            painter = painterResource(R.drawable.ic_home_grey_900_24dp),
            contentDescription = stringResource(R.string.navigate_home),
            modifier = Modifier.padding(3.dp)
        )
    }
}