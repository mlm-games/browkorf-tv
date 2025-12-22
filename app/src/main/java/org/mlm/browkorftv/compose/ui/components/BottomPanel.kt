package org.mlm.browkorftv.compose.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.mlm.browkorftv.R
import org.mlm.browkorftv.compose.ui.theme.AppTheme

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
    val colors = AppTheme.colors

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
        BrowkorfTvIconButton(
            onClick = onCloseTab,
            painter = painterResource(R.drawable.outline_close_24),
            contentDescription = stringResource(R.string.close_tab),
            modifier = Modifier.padding(3.dp)
        )

        // Back
        // The 'enabled' property in BrowkorfTvIconButton handles the grey-out/alpha automatically
        BrowkorfTvIconButton(
            onClick = onBack,
            painter = painterResource(R.drawable.outline_chevron_backward_24),
            contentDescription = stringResource(R.string.navigate_back),
            enabled = canGoBack,
            modifier = Modifier.padding(3.dp)
        )

        // Forward
        BrowkorfTvIconButton(
            onClick = onForward,
            painter = painterResource(R.drawable.outline_chevron_forward_24),
            contentDescription = stringResource(R.string.navigate_forward),
            enabled = canGoForward,
            modifier = Modifier.padding(3.dp)
        )

        // Refresh
        BrowkorfTvIconButton(
            onClick = onRefresh,
            painter = painterResource(R.drawable.outline_refresh_24),
            contentDescription = stringResource(R.string.refresh_page),
            modifier = Modifier.padding(3.dp)
        )

        // Zoom in
        BrowkorfTvIconButton(
            onClick = onZoomIn,
            painter = painterResource(R.drawable.outline_zoom_in_24),
            contentDescription = stringResource(R.string.zoom_in),
            enabled = canZoomIn,
            modifier = Modifier.padding(3.dp)
        )

        // Zoom out
        BrowkorfTvIconButton(
            onClick = onZoomOut,
            painter = painterResource(R.drawable.outline_zoom_out_24),
            contentDescription = stringResource(R.string.zoom_out),
            enabled = canZoomOut,
            modifier = Modifier.padding(3.dp)
        )

        // AdBlock toggle
        BrowkorfTvIconButton(
            onClick = onToggleAdBlock,
            painter = painterResource(
                if (adBlockEnabled) R.drawable.outline_security_24
                else R.drawable.outline_gpp_bad_24
            ),
            contentDescription = stringResource(R.string.toggle_ads_blocking),
            checked = adBlockEnabled,
            badgeCount = if (blockedAdsCount > 0) blockedAdsCount else null,
            modifier = Modifier.padding(3.dp)
        )

        // Popup block toggle
        BrowkorfTvIconButton(
            onClick = onTogglePopupBlock,
            painter = painterResource(R.drawable.outline_web_asset_off_24),
            contentDescription = stringResource(R.string.block_popups),
            checked = popupBlockEnabled,
            badgeCount = if (blockedPopupsCount > 0) blockedPopupsCount else null,
            modifier = Modifier.padding(3.dp)
        )

        // Home
        BrowkorfTvIconButton(
            onClick = onHome,
            painter = painterResource(R.drawable.outline_home_24),
            contentDescription = stringResource(R.string.navigate_home),
            modifier = Modifier.padding(3.dp)
        )
    }
}