package com.phlox.tvwebbrowser.compose.aux.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.phlox.tvwebbrowser.activity.main.FavoritesViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun FavoritesScreen(
    onBack: () -> Unit,
    onPickUrl: (String) -> Unit,
    onAddBookmark: () -> Unit,
    onEditBookmark: (Long) -> Unit,
    onEditHomeSlot: (Int) -> Unit,
    viewModel: FavoritesViewModel = koinViewModel()
) {
    val loading by viewModel.loading.collectAsState()
    val bookmarks by viewModel.bookmarks.collectAsState()
    val homeSlots by viewModel.homeSlots.collectAsState()

    LaunchedEffect(Unit) { viewModel.loadData() }

    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Favorites", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.weight(1f))
            Button(onClick = onAddBookmark) { Text("Add bookmark") }
            Button(onClick = onBack) { Text("Back") }
        }

        if (loading) {
            Text("Loading…")
            return
        }

        Text("Home page slots", style = MaterialTheme.typography.titleMedium)

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            for (row in 0..1) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    for (col in 0..3) {
                        val order = row * 4 + col
                        val item = homeSlots.firstOrNull { it.order == order }
                        Surface(
                            onClick = { onEditHomeSlot(order) },
                            tonalElevation = 2.dp,
                            modifier = Modifier.width(240.dp)
                        ) {
                            Column(Modifier.padding(12.dp)) {
                                Text("Slot $order", style = MaterialTheme.typography.bodySmall)
                                Text(item?.title ?: "Empty", maxLines = 1)
                                Text(item?.url ?: "", maxLines = 1, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        Text("Bookmarks", style = MaterialTheme.typography.titleMedium)

        if (bookmarks.isEmpty()) {
            Text("No bookmarks yet")
            return
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            bookmarks.take(40).forEach { b ->
                Surface(onClick = { b.url?.let(onPickUrl) }) {
                    Row(
                        Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(b.title ?: b.url.orEmpty(), maxLines = 1)
                            Text(b.url.orEmpty(), maxLines = 1, style = MaterialTheme.typography.bodySmall)
                        }
                        Button(onClick = { onEditBookmark(b.id) }) { Text("Edit") }
                    }
                }
            }
        }
    }
}