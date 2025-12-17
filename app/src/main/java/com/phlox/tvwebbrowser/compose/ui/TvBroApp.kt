package com.phlox.tvwebbrowser.compose.ui

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.phlox.tvwebbrowser.compose.runtime.ActivityBrowserPlatform
import com.phlox.tvwebbrowser.compose.runtime.DownloadServiceConnector
import com.phlox.tvwebbrowser.compose.ui.nav.AppKey
import com.phlox.tvwebbrowser.compose.ui.theme.TvBroTheme

@Composable
fun TvBroApp(
    downloadsConnector: DownloadServiceConnector,
    platform: ActivityBrowserPlatform,
) {
    val backStack = rememberNavBackStack(AppKey.Browser)

    TvBroTheme {
        NavDisplay(
            backStack = backStack,
            entryDecorators = listOf(
                rememberSaveableStateHolderNavEntryDecorator(),
                rememberViewModelStoreNavEntryDecorator(),
            ),
            entryProvider = entryProvider {
                entry<AppKey.Browser> { BrowserScreen(backStack, platform, downloadsConnector) }
                entry<AppKey.Downloads> { DownloadsScreen(backStack) }
                entry<AppKey.History> { HistoryScreen(backStack) }
                entry<AppKey.Settings> { SettingsScreen(backStack) }
                entry<AppKey.Favorites> { FavoritesScreen(backStack) }
                entry<AppKey.HomePageSlotEditor> { key ->
                    HomePageSlotEditorScreen(backStack, key.order)
                }
                entry<AppKey.BookmarkEditor> { key ->
                    BookmarkEditorScreen(backStack, key.id)
                }
                entry<AppKey.Shortcuts> { ShortcutsScreen(backStack) }
                entry<AppKey.About> { AboutScreen(backStack) }
            }
        )
    }
}