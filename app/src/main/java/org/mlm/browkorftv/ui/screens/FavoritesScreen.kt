package org.mlm.browkorftv.ui.screens

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.tv.material3.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.androidx.compose.koinViewModel
import org.mlm.browkorftv.activity.main.FavoritesViewModel
import org.mlm.browkorftv.singleton.FaviconsPool
import org.mlm.browkorftv.ui.components.BrowkorfTvClickableSurface
import org.mlm.browkorftv.ui.components.BrowkorfTvIconButton
import org.mlm.browkorftv.ui.theme.AppTheme
import org.mlm.browkorftv.R as AppR
import org.mlm.browkorftv.common.R as CommonR

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
    var editingEnabled by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) { viewModel.loadData() }

    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 48.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(stringResource(AppR.string.favorites), style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.weight(1f))
            BrowkorfTvIconButton(
                onClick = { editingEnabled = !editingEnabled },
                painter = painterResource(
                    if (editingEnabled) AppR.drawable.outline_lock_24
                    else AppR.drawable.outline_lock_open_24
                ),
                contentDescription = stringResource(
                    if (editingEnabled) AppR.string.lock_bookmark_editing
                    else AppR.string.unlock_bookmark_editing
                ),
                checked = editingEnabled
            )
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
            itemsIndexed(bookmarks, key = { _, bookmark -> bookmark.id }) { index, bookmark ->
                FavoriteItem(
                    title = bookmark.title ?: bookmark.url.orEmpty(),
                    url = bookmark.url.orEmpty(),
                    editingEnabled = editingEnabled,
                    canMoveUp = index > 0,
                    canMoveDown = index < bookmarks.lastIndex,
                    onOpen = { bookmark.url?.let(onPickUrl) },
                    onEdit = { onEditBookmark(bookmark.id) },
                    onDelete = { viewModel.deleteFavorite(bookmark.id) },
                    onMoveUp = { viewModel.moveFavorite(bookmark.id, -1) },
                    onMoveDown = { viewModel.moveFavorite(bookmark.id, 1) }
                )
            }
        }
    }
}

@Composable
private fun FavoriteItem(
    title: String,
    url: String,
    editingEnabled: Boolean,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onOpen: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit
) {
    val colors = AppTheme.colors
    val actionColors = ButtonDefaults.colors(
        containerColor = colors.buttonBackground,
        focusedContainerColor = colors.buttonBackgroundFocused,
        contentColor = colors.textPrimary,
        focusedContentColor = colors.textPrimary
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BookmarkFavicon(url)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.titleMedium, maxLines = 1)
                    Text(
                        url,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        color = colors.textSecondary
                    )
                }
            }
        }

        if (editingEnabled) {
            Spacer(Modifier.width(8.dp))

            BrowkorfTvIconButton(
                onClick = onEdit,
                painter = painterResource(CommonR.drawable.outline_movie_edit_24),
                contentDescription = stringResource(AppR.string.edit),
                colors = actionColors
            )

            Spacer(Modifier.width(8.dp))

            BrowkorfTvIconButton(
                onClick = onMoveUp,
                painter = painterResource(AppR.drawable.outline_arrow_upward_24),
                contentDescription = stringResource(AppR.string.move_up),
                enabled = canMoveUp,
                colors = actionColors
            )

            Spacer(Modifier.width(8.dp))

            BrowkorfTvIconButton(
                onClick = onMoveDown,
                painter = painterResource(AppR.drawable.outline_arrow_downward_24),
                contentDescription = stringResource(AppR.string.move_down),
                enabled = canMoveDown,
                colors = actionColors
            )

            Spacer(Modifier.width(8.dp))

            BrowkorfTvIconButton(
                onClick = onDelete,
                painter = painterResource(CommonR.drawable.outline_bookmark_remove_24),
                contentDescription = stringResource(AppR.string.remove),
                colors = actionColors
            )
        }
    }
}

@Composable
private fun BookmarkFavicon(url: String) {
    val colors = AppTheme.colors
    val favicon by produceState<Bitmap?>(initialValue = null, key1 = url) {
        value = null
        value = withContext(Dispatchers.IO) {
            FaviconsPool.get(url)
        }
    }

    Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) {
        favicon?.let { bitmap ->
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Fit
            )
        } ?: Icon(
            painter = painterResource(AppR.drawable.outline_public_24),
            contentDescription = null,
            modifier = Modifier.size(28.dp),
            tint = colors.iconColorDisabled
        )
    }
}
