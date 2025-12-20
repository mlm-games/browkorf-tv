package com.phlox.tvwebbrowser.compose

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.core.view.WindowCompat
import com.phlox.tvwebbrowser.compose.aux.ui.*
import com.phlox.tvwebbrowser.compose.settings.ui.SettingsScreen
import com.phlox.tvwebbrowser.compose.ui.theme.TvBroComposeTheme
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay

class ComposeMenuActivity : ComponentActivity() {

    companion object {
        const val EXTRA_START_ROUTE = "start_route"
        const val ROUTE_SETTINGS = "settings"
        const val ROUTE_DOWNLOADS = "downloads"
        const val ROUTE_HISTORY = "history"
        const val ROUTE_FAVORITES = "favorites"

        const val KEY_PICKED_URL = "picked_url"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        super.onCreate(savedInstanceState)

        val startRouteStr = intent.getStringExtra(EXTRA_START_ROUTE) ?: ROUTE_SETTINGS
        val startDest: MenuRoute = when (startRouteStr) {
            ROUTE_DOWNLOADS -> Downloads
            ROUTE_HISTORY -> History
            ROUTE_FAVORITES -> Favorites
            else -> Settings
        }

        setContent {
            TvBroComposeTheme {
                MenuNavigation(startDest)
            }
        }
    }

    @Composable
    fun MenuNavigation(startDestination: MenuRoute) {
        // Navigation 3: The backstack is just a state object you control
        val backStack = rememberNavBackStack(startDestination)

        NavDisplay(
            backStack = backStack,
            onBack = {
                if (backStack.size > 1) {
                    backStack.removeAt(backStack.lastIndex)
                } else {
                    finish()
                }
            },
            entryProvider = entryProvider {
                // Map Keys (Routes) to UI Entries<!--citation:1-->

                entry<Settings> {
                    SettingsScreen(onNavigateBack = { finish() })
                }

                entry<Downloads> {
                    DownloadsScreen(onBack = { finish() })
                }

                entry<History> {
                    HistoryScreen(
                        onBack = { finish() },
                        onPickUrl = { url -> returnResult(url) }
                    )
                }

                entry<Favorites> {
                    FavoritesScreen(
                        onBack = { finish() },
                        onPickUrl = { url -> returnResult(url) },
                        onAddBookmark = { backStack.add(BookmarkEditor()) },
                        onEditBookmark = { id -> backStack.add(BookmarkEditor(id = id)) },
                        onEditHomeSlot = { order -> backStack.add(BookmarkEditor(homeSlotOrder = order)) }
                    )
                }

                entry<BookmarkEditor> { key ->
                    BookmarkEditorScreen(
                        id = key.id,
                        homeSlotOrder = key.homeSlotOrder,
                        onDone = { backStack.removeAt(backStack.lastIndex) }
                    )
                }
            }
        )
    }

    private fun returnResult(url: String) {
        setResult(RESULT_OK, Intent().putExtra(KEY_PICKED_URL, url))
        finish()
    }
}

@Serializable
sealed interface MenuRoute : NavKey

@Serializable data object Settings : MenuRoute
@Serializable data object Downloads : MenuRoute
@Serializable data object History : MenuRoute
@Serializable data object Favorites : MenuRoute
@Serializable data object About : MenuRoute

@Serializable
data class BookmarkEditor(
    val id: Long? = null,
    val homeSlotOrder: Int? = null
) : MenuRoute