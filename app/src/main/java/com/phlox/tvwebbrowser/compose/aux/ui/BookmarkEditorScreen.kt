package com.phlox.tvwebbrowser.compose.aux.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.phlox.tvwebbrowser.activity.main.FavoritesViewModel
import com.phlox.tvwebbrowser.model.FavoriteItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.androidx.compose.koinViewModel

@Composable
fun BookmarkEditorScreen(
    id: Long?,
    homeSlotOrder: Int?,
    onDone: () -> Unit,
    viewModel: FavoritesViewModel = koinViewModel()
) {
    val scope = rememberCoroutineScope()
    var loading by remember { mutableStateOf(true) }
    var existingId by remember { mutableStateOf<Long?>(null) }
    var title by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }

    var editTitle by remember { mutableStateOf(false) }
    var editUrl by remember { mutableStateOf(false) }

    LaunchedEffect(id, homeSlotOrder) {
        loading = true
        val item = withContext(Dispatchers.IO) {
            when {
                id != null -> viewModel.getFavoriteById(id)
                homeSlotOrder != null -> viewModel.getHomeSlotByOrder(homeSlotOrder)
                else -> null
            }
        }
        existingId = item?.id?.takeIf { it != 0L }
        title = item?.title.orEmpty()
        url = item?.url.orEmpty()
        loading = false
    }

    fun normalizeUrl(s: String): String {
        val t = s.trim()
        if (t.isBlank()) return ""
        return if (t.matches(Regex("^[A-Za-z][A-Za-z0-9+.-]*://.*$"))) t else "https://$t"
    }

    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            val header = when {
                homeSlotOrder != null -> "Edit Home Slot #$homeSlotOrder"
                id == null -> "New Bookmark"
                else -> "Edit Bookmark"
            }
            Text(header, style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.weight(1f))
            Button(onClick = onDone) { Text("Back") }
        }

        if (loading) { Text("Loading…"); return }

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

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(onClick = {
                val norm = normalizeUrl(url)
                if (norm.isNotBlank()) {
                    val item = FavoriteItem().apply {
                        this.id = existingId ?: 0L
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
                    viewModel.saveFavorite(item)
                    onDone()
                }
            }) { Text("Save") }

            if (existingId != null) {
                Button(onClick = {
                    viewModel.deleteFavorite(existingId!!)
                    onDone()
                }) { Text("Delete") }
            }

            if (homeSlotOrder != null && existingId != null) {
                Button(onClick = {
                    viewModel.deleteFavorite(existingId!!)
                    onDone()
                }) { Text("Clear slot") }
            }
        }
    }

    if (editTitle) {
        TextEntryDialog(title = "Edit title", initial = title, hint = "Title", onDismiss = { editTitle = false }) {
            title = it; editTitle = false
        }
    }
    if (editUrl) {
        TextEntryDialog(title = "Edit URL", initial = url, hint = "https://example.com", onDismiss = { editUrl = false }) {
            url = it; editUrl = false
        }
    }
}