package com.phlox.tvwebbrowser.compose.aux

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowCompat
import com.phlox.tvwebbrowser.activity.history.HistoryActivity
import com.phlox.tvwebbrowser.compose.aux.ui.HistoryScreen
import com.phlox.tvwebbrowser.compose.theme.TvBroComposeTheme

class ComposeHistoryActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        super.onCreate(savedInstanceState)

        setContent {
            TvBroComposeTheme {
                HistoryScreen(
                    onBack = { finish() },
                    onPickUrl = { url ->
                        setResult(
                            Activity.RESULT_OK,
                            Intent().putExtra(HistoryActivity.KEY_URL, url)
                        )
                        finish()
                    }
                )
            }
        }
    }
}