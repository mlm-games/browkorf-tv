package com.phlox.tvwebbrowser.compose.ui

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.tv.material3.*
import com.phlox.tvwebbrowser.Config
import com.phlox.tvwebbrowser.compose.runtime.BrowserCommand
import com.phlox.tvwebbrowser.compose.runtime.BrowserCommandBus
import com.phlox.tvwebbrowser.compose.ui.nav.AppKey
import com.phlox.tvwebbrowser.data.AdblockRepository
import com.phlox.tvwebbrowser.data.settings.ConfigRepository
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@Composable
fun SettingsScreen(backStack: NavBackStack<NavKey>) {
    val repo: ConfigRepository = koinInject()
    val adblockRepo: AdblockRepository = koinInject()
    val bus: BrowserCommandBus = koinInject()

    val scope = rememberCoroutineScope()

    val adblock by repo.adblockEnabled.collectAsStateWithLifecycle(initialValue = true)
    val engine by repo.webEngine.collectAsStateWithLifecycle(initialValue = Config.ENGINE_WEB_VIEW)
    val autoplay by repo.allowAutoplayMedia.collectAsStateWithLifecycle(initialValue = false)
    val keepOn by repo.keepScreenOn.collectAsStateWithLifecycle(initialValue = false)

    val adblockState by adblockRepo.state.collectAsStateWithLifecycle()

    MaterialTheme {
        Column(Modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Settings", style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.weight(1f))
                Button(onClick = { backStack.removeLastOrNull() }) { Text("Back") }
            }

            Surface(onClick = {
                scope.launch {
                    repo.setAdblockEnabled(!adblock)
                    bus.trySend(BrowserCommand.AdblockChanged(!adblock))
                }
            }) {
                Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Adblock")
                    Text(if (adblock) "ON" else "OFF")
                }
            }

            Surface(onClick = {
                val next = if (engine == Config.ENGINE_GECKO_VIEW) Config.ENGINE_WEB_VIEW else Config.ENGINE_GECKO_VIEW
                scope.launch { repo.setWebEngine(next) }
            }) {
                Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Web Engine")
                    Text(engine)
                }
            }

            Surface(onClick = { scope.launch { repo.setAllowAutoplayMedia(!autoplay) } }) {
                Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Allow autoplay media")
                    Text(if (autoplay) "ON" else "OFF")
                }
            }

            Surface(onClick = { scope.launch { repo.setKeepScreenOn(!keepOn) } }) {
                Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Keep screen on")
                    Text(if (keepOn) "ON" else "OFF")
                }
            }

            Surface(onClick = { bus.trySend(BrowserCommand.ForceAdblockUpdate) }) {
                Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Update adblock list")
                    Text(if (adblockState.loading) "Updating…" else "Run")
                }
            }

            adblockState.error?.let { Text("Adblock error: $it", style = MaterialTheme.typography.bodySmall) }

            Spacer(Modifier.weight(1f))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(onClick = { backStack.add(AppKey.About) }) { Text("About & Updates") }
                Button(onClick = { backStack.add(AppKey.Shortcuts) }) { Text("Shortcuts") }
            }
        }
    }
}