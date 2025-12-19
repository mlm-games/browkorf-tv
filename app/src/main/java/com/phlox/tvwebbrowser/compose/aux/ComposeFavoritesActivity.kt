package com.phlox.tvwebbrowser.compose.aux

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowCompat
import com.phlox.tvwebbrowser.compose.aux.ui.FavoritesScreen
import com.phlox.tvwebbrowser.compose.theme.TvBroComposeTheme

class ComposeFavoritesActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        super.onCreate(savedInstanceState)

        setContent {
            TvBroComposeTheme {
                FavoritesScreen(
                    onBack = { finish() },
                    onPickUrl = { url ->
                        setResult(RESULT_OK, Intent().putExtra(KEY_URL, url))
                        finish()
                    },
                    onAddBookmark = {
                        startActivity(Intent(this, ComposeBookmarkEditorActivity::class.java))
                    },
                    onEditBookmark = { id ->
                        startActivity(
                            Intent(this, ComposeBookmarkEditorActivity::class.java)
                                .putExtra(ComposeBookmarkEditorActivity.EXTRA_ID, id)
                        )
                    },
                    onEditHomeSlot = { order ->
                        startActivity(
                            Intent(this, ComposeBookmarkEditorActivity::class.java)
                                .putExtra(ComposeBookmarkEditorActivity.EXTRA_HOME_SLOT_ORDER, order)
                        )
                    }
                )
            }
        }
    }

    companion object {
        const val KEY_URL = "url"
    }
}