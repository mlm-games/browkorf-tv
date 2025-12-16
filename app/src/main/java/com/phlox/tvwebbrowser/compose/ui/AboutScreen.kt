package com.phlox.tvwebbrowser.compose.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.tv.material3.*
import androidx.webkit.WebViewCompat
import com.phlox.tvwebbrowser.BuildConfig
import com.phlox.tvwebbrowser.TVBro
import com.phlox.tvwebbrowser.compose.platform.ApkInstall
import com.phlox.tvwebbrowser.compose.ui.nav.AppKey
import com.phlox.tvwebbrowser.compose.vm.UpdateViewModel
import org.koin.androidx.compose.koinViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AboutScreen(backStack: NavBackStack<NavKey>) {
    val ctx = LocalContext.current
    val activity = remember(ctx) {
        generateSequence(ctx) { (it as? ContextWrapper)?.baseContext }
            .filterIsInstance<Activity>()
            .firstOrNull()
    } // TODO: use any one out of the 3 ways

    val vm: UpdateViewModel = koinViewModel()
    val state by vm.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        vm.bindPrefsOnce()
        if (state.builtInAutoUpdate) vm.check()
    }

    MaterialTheme {
        Column(Modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("About", style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.weight(1f))
                Button(onClick = { backStack.removeLastOrNull() }) { Text("Back") }
            }

            Text("App: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")

            val engineText = remember {
                if (TVBro.config.isWebEngineGecko()) {
                    "GeckoView: " +
                            org.mozilla.geckoview.BuildConfig.MOZ_APP_VERSION +
                            "." + org.mozilla.geckoview.BuildConfig.MOZ_APP_BUILDID +
                            "-" + org.mozilla.geckoview.BuildConfig.MOZ_UPDATE_CHANNEL
                } else {
                    val p = WebViewCompat.getCurrentWebViewPackage(ctx)
                    "WebView: ${(p?.packageName ?: "unknown")} ${(p?.versionName ?: "unknown")}"
                }
            }
            Text(engineText, style = MaterialTheme.typography.bodySmall)

            Spacer(Modifier.height(6.dp))
            Text("Updates", style = MaterialTheme.typography.titleMedium)

            if (!state.builtInAutoUpdate) {
                Text("Built-in updater disabled for this build.", style = MaterialTheme.typography.bodySmall)
            } else {
                Surface(onClick = { vm.setAutoCheck(!state.autoCheck) }) {
                    Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Auto-check updates")
                        Text(if (state.autoCheck) "ON" else "OFF")
                    }
                }

                Surface(onClick = {
                    val list = state.availableChannels.ifEmpty { listOf("release", "beta") }
                    val idx = list.indexOf(state.channel).takeIf { it >= 0 } ?: 0
                    vm.setChannel(list[(idx + 1) % list.size])
                    vm.check()
                }) {
                    Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Channel")
                        Text(state.channel)
                    }
                }

                Button(onClick = { vm.check() }, enabled = !state.checking) {
                    Text(if (state.checking) "Checking…" else "Check now")
                }

                state.error?.let { Text("Error: $it", style = MaterialTheme.typography.bodySmall) }

                state.latest?.let { info ->
                    val hasUpdate = state.hasUpdate
                    Text(
                        text = if (hasUpdate) "New version: ${info.latestVersionName}" else "No updates found",
                        style = MaterialTheme.typography.bodyLarge
                    )

                    if (hasUpdate && activity != null) {
                        val apkFile = remember { File(ctx.cacheDir, "update.apk") }

                        Button(
                            enabled = !state.downloading,
                            onClick = {
                                if (!ApkInstall.canInstallUnknownApps(activity)) {
                                    ApkInstall.openUnknownAppsSettings(activity)
                                } else {
                                    vm.downloadTo(apkFile)
                                }
                            }
                        ) {
                            Text(if (state.downloading) "Downloading ${state.progress}%" else "Download update")
                        }

                        state.downloadedApk?.let { f ->
                            Button(onClick = { ApkInstall.installApk(activity, f) }) {
                                Text("Install")
                            }
                        }

                        Spacer(Modifier.height(6.dp))
                        Text("Changelog (latest 5):", style = MaterialTheme.typography.titleSmall)

                        val changes = remember(info) {
                            info.changelog
                                .filter { it.versionCode > BuildConfig.VERSION_CODE }
                                .sortedByDescending { it.versionCode }
                                .take(5)
                                .joinToString("\n\n") { "${it.versionName}\n${it.changes}" }
                        }
                        Text(changes.ifBlank { "—" }, style = MaterialTheme.typography.bodySmall)

                        val ts = remember {
                            SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
                        }
                        Text("Checked: $ts", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}