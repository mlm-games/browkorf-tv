package com.phlox.tvwebbrowser.compose.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.tv.material3.*
import com.phlox.tvwebbrowser.compose.platform.DownloadOpen
import com.phlox.tvwebbrowser.compose.vm.DownloadsUiItem
import com.phlox.tvwebbrowser.compose.vm.DownloadsViewModel
import com.phlox.tvwebbrowser.compose.ui.nav.AppKey
import org.koin.androidx.compose.koinViewModel

@Composable
fun DownloadsScreen(backStack: NavBackStack<NavKey>) {
    val vm: DownloadsViewModel = koinViewModel()
    val state by vm.state.collectAsStateWithLifecycle()
    val ctx = LocalContext.current

    LaunchedEffect(Unit) { vm.loadFirstPage() }

    MaterialTheme {
        Column(Modifier.fillMaxSize().padding(18.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Downloads", style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.weight(1f))
                Button(onClick = { backStack.removeLastOrNull() }) { Text("Back") }
            }

            Spacer(Modifier.height(12.dp))

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                state.items.forEach { item ->
                    when (item) {
                        is DownloadsUiItem.Header -> item(key = "h:${item.key}") {
                            Surface(tonalElevation = 2.dp) {
                                Text(
                                    modifier = Modifier.fillMaxWidth().padding(10.dp),
                                    text = item.title,
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                        }

                        is DownloadsUiItem.Row -> item(key = "d:${item.download.id}") {
                            val d = item.download
                            Surface(
                                onClick = { DownloadOpen.open(ctx, d) }
                            ) {
                                Column(Modifier.fillMaxWidth().padding(12.dp)) {
                                    Text(d.filename, maxLines = 1)
                                    Text(d.url, maxLines = 1, style = MaterialTheme.typography.bodySmall)
                                    Text("Size: ${d.bytesReceived}/${if (d.size <= 0) "?" else d.size}",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Button(onClick = { DownloadOpen.open(ctx, d) }) { Text("Open") }
                                        Button(onClick = { vm.delete(d) }) { Text("Delete") }
                                    }
                                }
                            }
                        }
                    }
                }

                item(key = "more") {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                        Button(
                            enabled = !state.loading,
                            onClick = { vm.loadMore() }
                        ) {
                            Text(if (state.loading) "Loading…" else "Load more")
                        }
                    }
                }
            }
        }
    }
}