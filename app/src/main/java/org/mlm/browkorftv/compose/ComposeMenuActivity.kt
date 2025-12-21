package org.mlm.browkorftv.compose

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.mlm.browkorftv.compose.ui.screens.SettingsScreen
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import org.mlm.browkorftv.compose.ui.theme.AppTheme
import org.mlm.browkorftv.settings.SettingsManager
import org.mlm.browkorftv.settings.Theme
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.mlm.browkorftv.compose.ui.screens.BookmarkEditorScreen
import org.mlm.browkorftv.compose.ui.screens.DownloadsScreen
import org.mlm.browkorftv.compose.ui.screens.FavoritesScreen
import org.mlm.browkorftv.compose.ui.screens.HistoryScreen

class ComposeMenuActivity : ComponentActivity(), KoinComponent {

    companion object {
        const val EXTRA_START_ROUTE = "start_route"
        const val ROUTE_SETTINGS = "settings"
        const val ROUTE_DOWNLOADS = "downloads"
        const val ROUTE_HISTORY = "history"
        const val ROUTE_FAVORITES = "favorites"

        const val KEY_PICKED_URL = "picked_url"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val settingsManager: SettingsManager by inject()
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
            val systemInDark = isSystemInDarkTheme()
            val settings by settingsManager.settingsState.collectAsStateWithLifecycle()

            val useDarkTheme = remember(settings.themeEnum, systemInDark) {
                when (settings.themeEnum) {
                    Theme.WHITE -> false
                    Theme.BLACK -> true
                    Theme.SYSTEM -> systemInDark
                }
            }

            AppTheme((useDarkTheme)) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(AppTheme.colors.topBarBackground)
                ) {
                    MenuNavigation(startDest)
                }
            }
        }
    }

    @Composable
    fun MenuNavigation(startDestination: MenuRoute) {
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
                    )
                }

                entry<BookmarkEditor> { key ->
                    BookmarkEditorScreen(
                        id = key.id,
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
) : MenuRoute