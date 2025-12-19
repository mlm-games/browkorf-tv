package com.phlox.tvwebbrowser.compose.aux

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowCompat
import com.phlox.tvwebbrowser.compose.aux.ui.DownloadsScreen
import com.phlox.tvwebbrowser.compose.theme.TvBroComposeTheme

class ComposeDownloadsActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        super.onCreate(savedInstanceState)

        setContent {
            TvBroComposeTheme {
                DownloadsScreen(
                    onBack = { finish() }
                )
            }
        }
    }
}