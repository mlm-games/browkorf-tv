package com.phlox.tvwebbrowser.compose.aux.ui

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.tv.material3.*
import com.phlox.tvwebbrowser.activity.main.FavoritesViewModel
import com.phlox.tvwebbrowser.model.FavoriteItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.androidx.compose.koinViewModel

@Composable
fun BookmarkEditorScreen(
    id: Long?,
    onDone: () -> Unit,
    viewModel: FavoritesViewModel = koinViewModel()
) {
    var loading by remember { mutableStateOf(true) }
    var existingId by remember { mutableStateOf<Long?>(null) }
    var title by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }

    var editTitle by remember { mutableStateOf(false) }
    var editUrl by remember { mutableStateOf(false) }

    LaunchedEffect(id) {
        loading = true
        val item = withContext(Dispatchers.IO) {
            when {
                id != null -> viewModel.getFavoriteById(id)
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

    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 48.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Text(
            text = if (id == null) "New Bookmark" else "Edit Bookmark",
            style = MaterialTheme.typography.headlineSmall
        )

        if (loading) {
            Text("Loading…")
            return
        }

        // Form Fields (Click row to edit)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            TvBroListItem(
                headline = "Title",
                supportingText = title.ifBlank { "Set title..." },
                onClick = { editTitle = true }
            )

            TvBroListItem(
                headline = "URL",
                supportingText = url.ifBlank { "Set URL..." },
                onClick = { editUrl = true }
            )
        }

        // Action Buttons
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = {
                val norm = normalizeUrl(url)
                if (norm.isNotBlank()) {
                    val item = FavoriteItem().apply {
                        this.id = existingId ?: 0L
                        this.title = title.trim().ifBlank { norm }
                        this.url = norm
                        this.parent = 0
                        this.homePageBookmark = false
                    }
                    viewModel.saveFavorite(item)
                    onDone()
                }
            }) {
                Text("Save")
            }

            if (existingId != null) {
                Button(
                    onClick = {
                        viewModel.deleteFavorite(existingId!!)
                        onDone()
                    },
                    colors = ButtonDefaults.colors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    Text("Delete")
                }
            }

            Button(
                onClick = onDone,
                colors = ButtonDefaults.colors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            ) {
                Text("Cancel")
            }
        }
    }

    // Dialogs
    if (editTitle) {
        TextEntryDialog(
            title = "Edit title",
            initial = title,
            hint = "Title",
            onDismiss = { editTitle = false },
            onConfirm = { title = it; editTitle = false }
        )
    }
    if (editUrl) {
        TextEntryDialog(
            title = "Edit URL",
            initial = url,
            hint = "https://example.com",
            onDismiss = { editUrl = false },
            onConfirm = { url = it; editUrl = false }
        )
    }
}