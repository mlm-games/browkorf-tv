package com.phlox.tvwebbrowser.compose.aux.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.tv.material3.*
import com.phlox.tvwebbrowser.activity.main.FavoritesViewModel
import com.phlox.tvwebbrowser.compose.ui.theme.TvBroTheme
import org.koin.androidx.compose.koinViewModel

@Composable
fun FavoritesScreen(
    onBack: () -> Unit,
    onPickUrl: (String) -> Unit,
    onAddBookmark: () -> Unit,
    onEditBookmark: (Long) -> Unit,
    viewModel: FavoritesViewModel = koinViewModel()
) {
    val loading by viewModel.loading.collectAsState()
    val bookmarks by viewModel.bookmarks.collectAsState()

    LaunchedEffect(Unit) { viewModel.loadData() }

    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 48.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header with Actions
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            Text("Favorites", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.weight(1f))
            Button(onClick = onAddBookmark) { Text("Add Bookmark") }
            Button(onClick = onBack) { Text("Back") }
        }

        if (loading) {
            Box(Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                Text("Loading...")
            }
            return
        }

        if (bookmarks.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                Text("No bookmarks yet")
            }
            return
        }

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 20.dp)
        ) {
            items(bookmarks, key = { it.id }) { b ->
                // Custom ListItem for Favorites to include the "Edit" button
                FavoriteItem(
                    title = b.title ?: b.url.orEmpty(),
                    url = b.url.orEmpty(),
                    onOpen = { b.url?.let(onPickUrl) },
                    onEdit = { onEditBookmark(b.id) }
                )
            }
        }
    }
}

@Composable
private fun FavoriteItem(
    title: String,
    url: String,
    onOpen: () -> Unit,
    onEdit: () -> Unit
) {
    val colors = TvBroTheme.colors

    Surface(
        onClick = onOpen,
        modifier = Modifier.fillMaxWidth(),
        shape = ClickableSurfaceDefaults.shape(shape = MaterialTheme.shapes.medium),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = colors.buttonBackground,
            focusedContainerColor = colors.buttonBackgroundFocused,
            contentColor = colors.textPrimary,
            focusedContentColor = colors.textPrimary
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.01f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, maxLines = 1)
                Text(url, style = MaterialTheme.typography.bodySmall, maxLines = 1, color = colors.textSecondary)
            }

            Spacer(Modifier.width(16.dp))

            Button(
                onClick = onEdit,
                modifier = Modifier.size(width = 80.dp, height = 35.dp),
                contentPadding = PaddingValues(0.dp)
            ) {
                Text("Edit", style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}