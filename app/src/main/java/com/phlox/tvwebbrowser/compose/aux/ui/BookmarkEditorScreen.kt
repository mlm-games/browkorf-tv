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
import kotlinx.coroutines.withContext

@Composable
fun BookmarkEditorScreen(
    id: Long?,
    homeSlotOrder: Int?, // null => normal bookmark; 0..7 => home slot editor
    onDone: () -> Unit,
) {
    val scope = rememberCoroutineScope()

    var loading by remember { mutableStateOf(true) }
    var existingId by remember { mutableStateOf<Long?>(null) }
    var title by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }

    var editTitle by remember { mutableStateOf(false) }
    var editUrl by remember { mutableStateOf(false) }

    suspend fun load() {
        loading = true
        val dao = AppDatabase.db.favoritesDao()

        val item: FavoriteItem? = withContext(Dispatchers.IO) {
            when {
                id != null -> dao.getById(id)
                homeSlotOrder != null -> {
                    // Find slot item by filtering home bookmarks
                    dao.getHomePageBookmarks().firstOrNull { it.order == homeSlotOrder }
                }
                else -> null
            }
        }

        existingId = item?.id?.takeIf { it != 0L }
        title = item?.title.orEmpty()
        url = item?.url.orEmpty()
        loading = false
    }

    LaunchedEffect(id, homeSlotOrder) { load() }

    fun normalizeUrl(s: String): String {
        val t = s.trim()
        if (t.isBlank()) return ""
        return if (t.matches(Regex("^[A-Za-z][A-Za-z0-9+.-]*://.*$"))) t else "https://$t"
    }

    Column(
        Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            val header =
                when {
                    homeSlotOrder != null -> "Edit Home Slot #$homeSlotOrder"
                    id == null -> "New Bookmark"
                    else -> "Edit Bookmark"
                }

            Text(header, style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.weight(1f))
            Button(onClick = onDone) { Text("Back") }
        }

        if (loading) {
            Text("Loading…")
            return
        }

        Surface {
            Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Title", style = MaterialTheme.typography.titleMedium)
                Text(title.ifBlank { "—" }, style = MaterialTheme.typography.bodySmall)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(onClick = { editTitle = true }) { Text("Edit title") }
                    Button(onClick = { title = "" }) { Text("Clear") }
                }
            }
        }

        Surface {
            Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("URL", style = MaterialTheme.typography.titleMedium)
                Text(url.ifBlank { "—" }, style = MaterialTheme.typography.bodySmall)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(onClick = { editUrl = true }) { Text("Edit URL") }
                    Button(onClick = { url = "" }) { Text("Clear") }
                }
            }
        }

        // Save / Delete / Clear slot
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(onClick = {
                scope.launch {
                    withContext(Dispatchers.IO) {
                        val dao = AppDatabase.db.favoritesDao()
                        val norm = normalizeUrl(url)
                        if (norm.isBlank()) return@withContext

                        val item = (existingId?.let { dao.getById(it) } ?: FavoriteItem()).apply {
                            this.title = title.trim().ifBlank { norm }
                            this.url = norm
                            this.parent = 0

                            if (homeSlotOrder != null) {
                                this.homePageBookmark = true
                                this.order = homeSlotOrder
                            } else {
                                this.homePageBookmark = false
                            }
                        }

                        if (item.id == 0L) dao.insert(item) else dao.update(item)
                    }
                    onDone()
                }
            }) { Text("Save") }

            if (existingId != null) {
                Button(onClick = {
                    scope.launch {
                        withContext(Dispatchers.IO) {
                            AppDatabase.db.favoritesDao().delete(existingId!!)
                        }
                        onDone()
                    }
                }) { Text("Delete") }
            }

            if (homeSlotOrder != null) {
                Button(onClick = {
                    scope.launch {
                        withContext(Dispatchers.IO) {
                            val dao = AppDatabase.db.favoritesDao()
                            // Clear slot: delete only that slot record if present
                            val existing = dao.getHomePageBookmarks().firstOrNull { it.order == homeSlotOrder }
                            if (existing != null) dao.delete(existing)
                        }
                        onDone()
                    }
                }) { Text("Clear slot") }
            }
        }
    }

    if (editTitle) {
        TextEntryDialog(
            title = "Edit title",
            initial = title,
            hint = "Title",
            onDismiss = { editTitle = false },
            onConfirm = {
                title = it
                editTitle = false
            }
        )
    }

    if (editUrl) {
        TextEntryDialog(
            title = "Edit URL",
            initial = url,
            hint = "https://example.com",
            onDismiss = { editUrl = false },
            onConfirm = {
                url = it
                editUrl = false
            }
        )
    }
}