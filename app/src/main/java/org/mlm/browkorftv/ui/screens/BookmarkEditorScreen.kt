package org.mlm.browkorftv.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.tv.material3.*
import org.mlm.browkorftv.activity.main.FavoritesViewModel
import org.mlm.browkorftv.model.FavoriteItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.androidx.compose.koinViewModel
import org.mlm.browkorftv.ui.components.TextEntryDialog
import org.mlm.browkorftv.ui.components.BrowkorfTopBar
import org.mlm.browkorftv.ui.components.BrowkorfTvButton
import org.mlm.browkorftv.ui.components.BrowkorfTvIconButton
import org.mlm.browkorftv.ui.components.BrowkorfTvListItem
import org.mlm.browkorftv.R
import org.mlm.browkorftv.common.R as CommonR


@Composable
fun BookmarkEditorScreen(
    id: Long?,
    initialTitle: String = "",
    initialUrl: String = "",
    onDone: () -> Unit,
    viewModel: FavoritesViewModel = koinViewModel()
) {
    var loading by remember { mutableStateOf(true) }
    var existingId by remember { mutableStateOf<Long?>(null) }
    var title by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    val setTitle = stringResource(R.string.set_title)
    val setUrl = stringResource(R.string.set_url)

    var editTitle by remember { mutableStateOf(false) }
    var editUrl by remember { mutableStateOf(false) }

    LaunchedEffect(id) {
        loading = true
        if (id != null) {
            val item = withContext(Dispatchers.IO) {
                viewModel.getFavoriteById(id)
            }
            existingId = item?.id?.takeIf { it != 0L }
            title = item?.title.orEmpty()
            url = item?.url.orEmpty()
        } else {
            title = initialTitle
            url = initialUrl
        }
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
        BrowkorfTopBar(
            title = if (id == null) {
                stringResource(R.string.new_bookmark)
            } else {
                stringResource(R.string.edit_bookmark)
            },
            onBack = onDone,
            actions = {
                val saveBookmark = {
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
                }

                // Save
                BrowkorfTvIconButton(
                    onClick = saveBookmark,
                    painter = painterResource(CommonR.drawable.outline_bookmark_check_24),
                    contentDescription = stringResource(R.string.save)
                )

                // Delete (only if editing)
                if (existingId != null) {
                    val deleteBookmark = {
                        viewModel.deleteFavorite(existingId!!)
                        onDone()
                    }
                    BrowkorfTvIconButton(
                        onClick = deleteBookmark,
                        painter = painterResource(CommonR.drawable.outline_delete_24),
                        contentDescription = stringResource(R.string.delete)
                    )
                }
            }
        )

        Spacer(Modifier.height(16.dp))

        if (loading) {
            Text(stringResource(R.string.loading))
            return
        }

        // Form Fields (Click row to edit)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            BrowkorfTvListItem(
                headline = stringResource(R.string.title),
                supportingText = title.ifBlank { setTitle },
                onClick = { editTitle = true }
            )

            BrowkorfTvListItem(
                headline = stringResource(R.string.url),
                supportingText = url.ifBlank { setUrl },
                onClick = { editUrl = true }
            )
        }

        // Action Buttons
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {

            BrowkorfTvButton(
                onClick = onDone,
                text = stringResource(R.string.cancel)
            )
        }
    }

    // Dialogs
    if (editTitle) {
        TextEntryDialog(
            title = stringResource(R.string.edit_title),
            initial = title,
            hint = stringResource(R.string.title),
            onDismiss = { editTitle = false },
            onConfirm = { title = it; editTitle = false }
        )
    }
    if (editUrl) {
        TextEntryDialog(
            title = stringResource(R.string.edit_url),
            initial = url,
            hint = "https://example.com",
            onDismiss = { editUrl = false },
            onConfirm = { url = it; editUrl = false }
        )
    }
}