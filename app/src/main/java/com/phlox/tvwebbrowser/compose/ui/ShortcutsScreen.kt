package com.phlox.tvwebbrowser.compose.ui

import android.view.KeyEvent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.tv.material3.Button
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.phlox.tvwebbrowser.compose.runtime.ShortcutCaptureController
import com.phlox.tvwebbrowser.compose.ui.nav.AppKey
import com.phlox.tvwebbrowser.singleton.shortcuts.Shortcut
import com.phlox.tvwebbrowser.singleton.shortcuts.ShortcutMgr
import org.koin.compose.koinInject

@Composable
fun ShortcutsScreen(backStack: NavBackStack<NavKey>) {
    val mgr: ShortcutMgr = koinInject()
    val capture: ShortcutCaptureController = koinInject()
    val capturing by capture.capturing.collectAsState()

    MaterialTheme {
        Column(Modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Shortcuts", style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.weight(1f))
                Button(onClick = { backStack.removeLastOrNull() }) { Text("Back") }
            }

            capturing?.let {
                Surface(tonalElevation = 6.dp) {
                    Text(
                        modifier = Modifier.padding(12.dp),
                        text = "Press any key to bind: ${it.name}"
                    )
                }
            }

            ShortcutRow("Menu", mgr.findForId(Shortcut.MENU.itemId)!!, mgr, capture)
            ShortcutRow("Navigate back", mgr.findForId(Shortcut.NAVIGATE_BACK.itemId)!!, mgr, capture)
            ShortcutRow("Home", mgr.findForId(Shortcut.NAVIGATE_HOME.itemId)!!, mgr, capture)
            ShortcutRow("Refresh", mgr.findForId(Shortcut.REFRESH_PAGE.itemId)!!, mgr, capture)
            ShortcutRow("Voice search", mgr.findForId(Shortcut.VOICE_SEARCH.itemId)!!, mgr, capture)
        }
    }
}

@Composable
private fun ShortcutRow(
    title: String,
    shortcut: Shortcut,
    mgr: ShortcutMgr,
    capture: ShortcutCaptureController
) {
    Surface {
        Row(
            Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Column(Modifier.weight(1f)) {
                Text(title, maxLines = 1)
                Text(
                    text = if (shortcut.keyCode == 0) "Not set" else KeyEvent.keyCodeToString(shortcut.keyCode),
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Button(onClick = { capture.start(shortcut) }) { Text("Set") }
            Button(onClick = {
                shortcut.keyCode = 0
                mgr.save(shortcut)
            }) { Text("Clear") }
        }
    }
}