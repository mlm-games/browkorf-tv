package com.phlox.tvwebbrowser.compose.ui

import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import androidx.tv.material3.MaterialTheme
import com.phlox.tvwebbrowser.compose.runtime.ActivityBrowserPlatform
import com.phlox.tvwebbrowser.compose.runtime.DownloadServiceConnector
import com.phlox.tvwebbrowser.compose.ui.nav.AppKey

@Composable
fun TvBroApp(
    downloadsConnector: DownloadServiceConnector,
    platform: ActivityBrowserPlatform
) {
    // Explicitly creating a stack of NavKey to match the library's expectations
    // and the updated Screen signatures.
    val backStack = rememberNavBackStack(AppKey.Browser)

    MaterialTheme {
        NavDisplay(
            backStack = backStack,
            entryProvider = entryProvider {
                // Ensure all these screens accept NavBackStack<NavKey>
                entry<AppKey.Browser> { BrowserScreen(backStack, platform, downloadsConnector) }
                entry<AppKey.Downloads> { DownloadsScreen(backStack) }
                entry<AppKey.History> { HistoryScreen(backStack) }
                entry<AppKey.Settings> { SettingsScreen(backStack) }

                entry<AppKey.Favorites> { FavoritesScreen(backStack) }
                entry<AppKey.HomePageSlotEditor> { key -> HomePageSlotEditorScreen(backStack, key.order) }
                entry<AppKey.BookmarkEditor> { key -> BookmarkEditorScreen(backStack, key.id) }

                entry<AppKey.Shortcuts> { ShortcutsScreen(backStack) }
                entry<AppKey.About> { AboutScreen(backStack) }
            }
        )
    }
}