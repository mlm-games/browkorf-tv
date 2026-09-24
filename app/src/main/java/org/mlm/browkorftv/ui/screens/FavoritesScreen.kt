package org.mlm.browkorftv.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import org.mlm.browkorftv.R as AppR
import org.mlm.browkorftv.common.R as CommonR
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.tv.material3.*
import org.mlm.browkorftv.activity.main.FavoritesViewModel
import org.mlm.browkorftv.ui.components.BrowkorfTvClickableSurface
import org.mlm.browkorftv.ui.components.BrowkorfTvIconButton
import org.mlm.browkorftv.ui.theme.AppTheme
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
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(stringResource(AppR.string.favorites), style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.weight(1f))
            BrowkorfTvIconButton(
                onClick = onAddBookmark,
                painter = painterResource(AppR.drawable.outline_add_24),
                contentDescription = stringResource(AppR.string.add_bookmark)
            )
            BrowkorfTvIconButton(
                onClick = onBack,
                painter = painterResource(AppR.drawable.outline_chevron_forward_24),
                contentDescription = stringResource(AppR.string.navigate_back)
            )
        }

        if (loading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(stringResource(AppR.string.loading))
            }
            return
        }

        if (bookmarks.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(stringResource(AppR.string.no_bookmarks_yet))
            }
            return
        }

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 20.dp)
        ) {
            items(bookmarks, key = { it.id }) { b ->
                val index = bookmarks.indexOfFirst { it.id == b.id }
                FavoriteItem(
                    title = b.title ?: b.url.orEmpty(),
                    url = b.url.orEmpty(),
                    canMoveUp = index > 0,
                    canMoveDown = index >= 0 && index < bookmarks.lastIndex,
                    onOpen = { b.url?.let(onPickUrl) },
                    onEdit = { onEditBookmark(b.id) },
                    onDelete = { viewModel.deleteFavorite(b.id) },
                    onMoveUp = { viewModel.moveFavorite(b.id, -1) },
                    onMoveDown = { viewModel.moveFavorite(b.id, 1) }
                )
            }
        }
    }
}

@Composable
private fun FavoriteItem(
    title: String,
    url: String,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onOpen: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit
) {
    val colors = AppTheme.colors

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically // Align items vertically
    ) {
        BrowkorfTvClickableSurface(
            onClick = onOpen,
            modifier = Modifier.weight(1f),
            shape = ClickableSurfaceDefaults.shape(shape = MaterialTheme.shapes.medium),
            colors = ClickableSurfaceDefaults.colors(
                containerColor = colors.buttonBackground,
                focusedContainerColor = colors.buttonBackgroundFocused,
                contentColor = colors.textPrimary,
                focusedContentColor = colors.textPrimary
            ),
            scale = ClickableSurfaceDefaults.scale(focusedScale = 1.01f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Text(title, style = MaterialTheme.typography.titleMedium, maxLines = 1)
                Text(
                    url,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    color = colors.textSecondary
                )
            }
        }

        Spacer(Modifier.width(8.dp))

        BrowkorfTvIconButton(
            onClick = onEdit,
            painter = painterResource(CommonR.drawable.outline_movie_edit_24),
            contentDescription = stringResource(AppR.string.edit),
            colors = ButtonDefaults.colors(
                containerColor = colors.buttonBackground,
                focusedContainerColor = colors.buttonBackgroundFocused,
                contentColor = colors.textPrimary,
                focusedContentColor = colors.textPrimary
            )
        )

        Spacer(Modifier.width(8.dp))

        BrowkorfTvIconButton(
            onClick = onMoveUp,
            painter = painterResource(AppR.drawable.outline_chevron_backward_24),
            contentDescription = stringResource(AppR.string.move_up),
            enabled = canMoveUp,
            colors = ButtonDefaults.colors(
                containerColor = colors.buttonBackground,
                focusedContainerColor = colors.buttonBackgroundFocused,
                contentColor = colors.textPrimary,
                focusedContentColor = colors.textPrimary
            )
        )

        Spacer(Modifier.width(8.dp))

        BrowkorfTvIconButton(
            onClick = onMoveDown,
            painter = painterResource(AppR.drawable.outline_chevron_forward_24),
            contentDescription = stringResource(AppR.string.move_down),
            enabled = canMoveDown,
            colors = ButtonDefaults.colors(
                containerColor = colors.buttonBackground,
                focusedContainerColor = colors.buttonBackgroundFocused,
                contentColor = colors.textPrimary,
                focusedContentColor = colors.textPrimary
            )
        )

        Spacer(Modifier.width(8.dp))

        BrowkorfTvIconButton(
            onClick = onDelete,
            painter = painterResource(CommonR.drawable.outline_bookmark_remove_24),
            contentDescription = stringResource(AppR.string.remove),
            colors = ButtonDefaults.colors(
                containerColor = colors.buttonBackground,
                focusedContainerColor = colors.buttonBackgroundFocused,
                contentColor = colors.textPrimary,
                focusedContentColor = colors.textPrimary
            )
        )
    }
}