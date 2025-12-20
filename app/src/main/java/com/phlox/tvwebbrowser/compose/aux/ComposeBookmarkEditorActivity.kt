package com.phlox.tvwebbrowser.compose.aux

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowCompat
import com.phlox.tvwebbrowser.compose.aux.ui.BookmarkEditorScreen
import com.phlox.tvwebbrowser.compose.ui.theme.TvBroComposeTheme

class ComposeBookmarkEditorActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        super.onCreate(savedInstanceState)

        val id = intent.getLongExtra(EXTRA_ID, 0L).takeIf { it != 0L }
        val homeSlotOrder = intent.getIntExtra(EXTRA_HOME_SLOT_ORDER, -1).takeIf { it >= 0 }

        setContent {
            TvBroComposeTheme {
                BookmarkEditorScreen(
                    id = id,
                    homeSlotOrder = homeSlotOrder,
                    onDone = { finish() }
                )
            }
        }
    }

    companion object {
        const val EXTRA_ID = "id"
        const val EXTRA_HOME_SLOT_ORDER = "homeSlotOrder" // 0..7 for home slots
    }
}