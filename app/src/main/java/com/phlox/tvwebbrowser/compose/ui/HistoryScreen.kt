package com.phlox.tvwebbrowser.compose.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.tv.material3.*
import com.phlox.tvwebbrowser.compose.runtime.BrowserCommand
import com.phlox.tvwebbrowser.compose.runtime.BrowserCommandBus
import com.phlox.tvwebbrowser.compose.ui.nav.AppKey
import com.phlox.tvwebbrowser.compose.vm.HistoryUiItem
import com.phlox.tvwebbrowser.compose.vm.HistoryViewModel
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

@Composable
fun HistoryScreen(backStack: NavBackStack<NavKey>) {
    val vm: HistoryViewModel = koinViewModel()
    val bus: BrowserCommandBus = koinInject()
    val state by vm.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { vm.loadFirstPage() }

    MaterialTheme {
        Column(Modifier.fillMaxSize().padding(18.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("History", style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.weight(1f))
                Button(onClick = { vm.clearAll() }) { Text("Clear all") }
                Button(onClick = { backStack.removeLastOrNull() }) { Text("Back") }
            }

            Spacer(Modifier.height(12.dp))

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                state.items.forEach { it ->
                    when (it) {
                        is HistoryUiItem.Header -> item(key = "h:${it.key}") {
                            Surface(tonalElevation = 2.dp) {
                                Text(
                                    modifier = Modifier.fillMaxWidth().padding(10.dp),
                                    text = it.title,
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                        }

                        is HistoryUiItem.Row -> item(key = "i:${it.item.id}") {
                            val item = it.item
                            Surface(
                                onClick = {
                                    bus.trySend(BrowserCommand.Navigate(item.url, inNewTab = false))
                                    backStack.removeLastOrNull()
                                }
                            ) {
                                Column(Modifier.fillMaxWidth().padding(12.dp)) {
                                    Text(item.title.ifBlank { item.url }, maxLines = 1)
                                    Text(item.url, maxLines = 1, style = MaterialTheme.typography.bodySmall)
                                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Button(onClick = {
                                            bus.trySend(BrowserCommand.Navigate(item.url, inNewTab = false))
                                            backStack.removeLastOrNull()
                                        }) { Text("Open") }

                                        Button(onClick = { vm.delete(item) }) { Text("Delete") }
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