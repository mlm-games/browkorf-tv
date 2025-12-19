package com.phlox.tvwebbrowser.compose.aux.ui

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.tv.material3.*
import com.phlox.tvwebbrowser.model.FavoriteItem
import com.phlox.tvwebbrowser.singleton.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

@Composable
fun FavoritesScreen(
    onBack: () -> Unit,
    onPickUrl: (String) -> Unit,
    onAddBookmark: () -> Unit,
    onEditBookmark: (Long) -> Unit,
    onEditHomeSlot: (Int) -> Unit,
) {
    var loading by remember { mutableStateOf(true) }
    var bookmarks by remember { mutableStateOf<List<FavoriteItem>>(emptyList()) }
    var homeSlots by remember { mutableStateOf<List<FavoriteItem>>(emptyList()) }

    suspend fun load() {
        loading = true
        val dao = AppDatabase.db.favoritesDao()
        homeSlots = withContext(Dispatchers.IO) { dao.getHomePageBookmarks() }
        bookmarks = withContext(Dispatchers.IO) { dao.getAll(homePageBookmarks = false) }
        loading = false
    }

    LaunchedEffect(Unit) { load() }

    Column(
        Modifier
            .fillMaxSize()
            .padding(24.dp),
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

        // Show 8 slots (0..7). Each opens the home-slot editor.
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
                                Text(
                                    item?.title ?: "Empty",
                                    maxLines = 1
                                )
                                Text(
                                    item?.url ?: "",
                                    maxLines = 1,
                                    style = MaterialTheme.typography.bodySmall
                                )
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

        // Minimal list (no lazy list needed for small sets; keep it simple for now)
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

        Spacer(Modifier.height(8.dp))
        Button(onClick = { runBlocking {launch {load()} } }) { Text("Reload") } // handy while iterating
    }
}