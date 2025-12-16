package com.phlox.tvwebbrowser.compose

import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.phlox.tvwebbrowser.compose.runtime.ActivityBrowserPlatform
import com.phlox.tvwebbrowser.compose.runtime.BrowserCommand
import com.phlox.tvwebbrowser.compose.runtime.BrowserCommandBus
import com.phlox.tvwebbrowser.compose.runtime.DownloadServiceConnector
import com.phlox.tvwebbrowser.compose.runtime.ShortcutCaptureController
import com.phlox.tvwebbrowser.compose.ui.TvBroApp
import com.phlox.tvwebbrowser.singleton.shortcuts.ShortcutMgr
import org.koin.android.ext.android.inject

class ComposeTvBroActivity : ComponentActivity() {

    private lateinit var downloadsConnector: DownloadServiceConnector
    private lateinit var platform: ActivityBrowserPlatform

    private val bus: BrowserCommandBus by inject()
    private val capture: ShortcutCaptureController by inject()
    private val shortcutMgr: ShortcutMgr by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        downloadsConnector = DownloadServiceConnector(this)
        platform = ActivityBrowserPlatform(this)

        // Handle deep link on launch
        intent?.data?.toString()?.let { url ->
            bus.trySend(BrowserCommand.Navigate(url, inNewTab = false))
        }

        setContent {
            TvBroApp(
                downloadsConnector = downloadsConnector,
                platform = platform
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intent.data?.toString()?.let { url ->
            bus.trySend(BrowserCommand.Navigate(url, inNewTab = false))
        }
    }

    override fun onStart() {
        super.onStart()
        downloadsConnector.bind()
    }

    override fun onStop() {
        downloadsConnector.unbind()
        super.onStop()
    }

    override fun onDestroy() {
        platform.dispose()
        super.onDestroy()
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        // 1) Capture mode: bind next key (on key UP)
        val capturing = capture.capturing.value
        if (capturing != null) {
            if (event.action == KeyEvent.ACTION_UP) {
                capturing.keyCode = event.keyCode
                shortcutMgr.save(capturing)
                capture.stop()
                bus.trySend(
                    BrowserCommand.Toast(
                        "Bound ${capturing.name} to ${KeyEvent.keyCodeToString(event.keyCode)}"
                    )
                )
            }
            return true
        }

        // 2) Execute shortcut on key UP
        if (event.action == KeyEvent.ACTION_UP && shortcutMgr.canProcessKeyCode(event.keyCode)) {
            return shortcutMgr.process(event.keyCode, object : ShortcutMgr.ShortcutHandler {
                override fun toggleMenu() { bus.trySend(BrowserCommand.ToggleQuickMenu) }
                override fun navigateBack() { bus.trySend(BrowserCommand.Back) }
                override fun navigateHome() { bus.trySend(BrowserCommand.Home) }
                override fun refreshPage() { bus.trySend(BrowserCommand.Reload) }
                override fun voiceSearch() { bus.trySend(BrowserCommand.StartVoiceSearch) }
            })
        }

        return super.dispatchKeyEvent(event)
    }
}