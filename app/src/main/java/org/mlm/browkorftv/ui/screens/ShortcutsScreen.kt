package org.mlm.browkorftv.ui.screens

import android.view.KeyEvent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.compose.koinInject
import org.mlm.browkorftv.R
import org.mlm.browkorftv.data.BookmarksRepository
import org.mlm.browkorftv.model.FavoriteItem
import org.mlm.browkorftv.singleton.shortcuts.Shortcut
import org.mlm.browkorftv.singleton.shortcuts.ShortcutMgr
import org.mlm.browkorftv.ui.SnackbarManager
import org.mlm.browkorftv.ui.components.BrowkorfTopBar
import org.mlm.browkorftv.ui.components.BrowkorfTvButton
import org.mlm.browkorftv.ui.components.BrowkorfTvListItem
import org.mlm.browkorftv.ui.theme.AppTheme
import org.mlm.browkorftv.utils.NavigationReservedShortcutKeyCodes

@Composable
fun ShortcutsScreen(
    onNavigateBack: () -> Unit
) {
    val colors = AppTheme.colors
    val context = LocalContext.current
    val shortcutMgr: ShortcutMgr = koinInject()
    val bookmarksRepository: BookmarksRepository = koinInject()

    val bindings by shortcutMgr.bindings.collectAsState()
    val bookmarkIds by shortcutMgr.bookmarkIds.collectAsState()
    val bookmarks by produceState<List<FavoriteItem>>(emptyList()) {
        value = withContext(Dispatchers.IO) {
            bookmarksRepository.getAll()
        }
    }
    val snackbarManager: SnackbarManager = koinInject()
    val scope = rememberCoroutineScope()

    var editingShortcut by remember { mutableStateOf<Shortcut?>(null) }
    var selectingBookmark by remember { mutableStateOf<Shortcut?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .padding(horizontal = 48.dp, vertical = 24.dp)
    ) {
        BrowkorfTopBar(
            title = stringResource(R.string.shortcuts),
            onBack = onNavigateBack
        )

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(Shortcut.entries) { shortcut ->
                val binding = bindings[shortcut] ?: shortcutMgr.bindingFor(shortcut)
                val bookmark = shortcut.bookmarkSlotNumber?.let {
                    bookmarks.firstOrNull { bookmark -> bookmark.id == (bookmarkIds[shortcut] ?: 0L) }
                }
                val headline = when {
                    bookmark != null -> bookmark.title?.ifBlank { bookmark.url.orEmpty() }
                        ?: bookmark.url.orEmpty()
                    shortcut.bookmarkSlotNumber != null -> {
                        "${stringResource(R.string.bookmarks)} ${shortcut.bookmarkSlotNumber}"
                    }
                    else -> stringResource(shortcut.titleResId)
                }
                val supportingText = if (shortcut.bookmarkSlotNumber != null) {
                    val keyText = Shortcut.shortcutKeysToString(shortcut, binding, context)
                    if (bookmark == null) {
                        "$keyText • ${stringResource(R.string.nothing)}"
                    } else {
                        "$keyText • ${bookmark.url.orEmpty()}"
                    }
                } else {
                    Shortcut.shortcutKeysToString(shortcut, binding, context)
                }
                BrowkorfTvListItem(
                    onClick = { editingShortcut = shortcut },
                    headline = headline,
                    supportingText = supportingText
                )
            }
        }
    }

    editingShortcut?.let { shortcut ->
        ShortcutEditDialog(
            shortcut = shortcut,
            onSetKey = { keyCode, modifiers ->
                if (NavigationReservedShortcutKeyCodes.reservedForUserShortcuts.contains(keyCode)) {
                    scope.launch {
                        @Suppress("LocalContextGetResourceValueCall")
                        val msg = context.getString(R.string.shortcut_key_reserved_for_navigation)
                        snackbarManager.show(message = msg,
                        )
                    }
                    return@ShortcutEditDialog
                }
                shortcutMgr.updateBinding(
                    shortcut = shortcut,
                    keyCode = keyCode,
                    modifiers = modifiers,
                    longPress = false
                )
                editingShortcut = null
            },
            onClearKey = {
                shortcutMgr.updateBinding(shortcut, keyCode = 0)
                editingShortcut = null
            },
            onSelectBookmark = if (shortcut.bookmarkSlotNumber != null) {
                {
                    selectingBookmark = shortcut
                    editingShortcut = null
                }
            } else {
                null
            },
            onDismiss = { editingShortcut = null }
        )
    }

    selectingBookmark?.let { shortcut ->
        BookmarkPickerDialog(
            bookmarks = bookmarks,
            selectedId = bookmarkIds[shortcut] ?: 0L,
            assignedIds = bookmarkIds
                .filterKeys { it != shortcut && it.bookmarkSlotNumber != null }
                .values
                .toSet(),
            onSelect = { id ->
                shortcutMgr.updateBookmark(shortcut, id)
                selectingBookmark = null
            },
            onClear = {
                shortcutMgr.updateBookmark(shortcut, 0L)
                selectingBookmark = null
            },
            onDismiss = { selectingBookmark = null }
        )
    }
}

@Composable
private fun ShortcutEditDialog(
    shortcut: Shortcut,
    onSetKey: (keyCode: Int, modifiers: Int) -> Unit,
    onClearKey: () -> Unit,
    onSelectBookmark: (() -> Unit)?,
    onDismiss: () -> Unit
) {
    val colors = AppTheme.colors
    var waitingForKey by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(dismissOnBackPress = !waitingForKey)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(48.dp)
                .then(
                    if (waitingForKey) {
                        Modifier.onKeyEvent { event ->
                            val native = event.nativeKeyEvent
                            if (native.action == KeyEvent.ACTION_DOWN) {
                                val code = native.keyCode
                                if (code != KeyEvent.KEYCODE_BACK && code != KeyEvent.KEYCODE_DPAD_CENTER) {
                                    val normalized = KeyEvent.normalizeMetaState(native.metaState)
                                    val mask =
                                        KeyEvent.META_ALT_ON or KeyEvent.META_CTRL_ON or KeyEvent.META_SHIFT_ON
                                    val mods = normalized and mask
                                    onSetKey(code, mods)
                                    return@onKeyEvent true
                                }
                            }
                            false
                        }
                    } else {
                        Modifier
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier.widthIn(max = 450.dp),
                shape = RoundedCornerShape(8.dp),
                color = colors.background
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (waitingForKey) {
                        Text(
                            text = stringResource(R.string.press_eny_key),
                            style = MaterialTheme.typography.titleLarge,
                            color = colors.textPrimary
                        )
                        BrowkorfTvButton(
                            onClick = { waitingForKey = false },
                            text = stringResource(R.string.cancel)
                        )
                    } else {
                        val actionTitle = if (shortcut.bookmarkSlotNumber != null) {
                            "${stringResource(R.string.bookmarks)} ${shortcut.bookmarkSlotNumber}"
                        } else {
                            stringResource(shortcut.titleResId)
                        }
                        Text(
                            text = stringResource(R.string.action) + ": " + actionTitle,
                            color = colors.textPrimary
                        )
                        if (onSelectBookmark != null) {
                            BrowkorfTvButton(
                                onClick = onSelectBookmark,
                                text = stringResource(R.string.select_bookmark),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            BrowkorfTvButton(
                                onClick = { waitingForKey = true },
                                text = stringResource(R.string.set_key_for_action),
                                modifier = Modifier.weight(1f)
                            )
                            BrowkorfTvButton(
                                onClick = onClearKey,
                                text = stringResource(R.string.clear),
                                modifier = Modifier.weight(1f)
                            )
                        }
                        BrowkorfTvButton(
                            onClick = onDismiss,
                            text = stringResource(R.string.cancel)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BookmarkPickerDialog(
    bookmarks: List<FavoriteItem>,
    selectedId: Long,
    assignedIds: Set<Long>,
    onSelect: (Long) -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit
) {
    val colors = AppTheme.colors
    val selectableBookmarks = bookmarks.filter { it.id == selectedId || it.id !in assignedIds }

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier.fillMaxSize().padding(48.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier.widthIn(max = 600.dp),
                shape = RoundedCornerShape(8.dp),
                color = colors.background
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = stringResource(R.string.select_bookmark),
                        style = MaterialTheme.typography.titleLarge,
                        color = colors.textPrimary
                    )
                    if (selectableBookmarks.isEmpty()) {
                        Text(
                            text = stringResource(R.string.nothing),
                            color = colors.textSecondary
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.heightIn(max = 420.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(selectableBookmarks, key = { it.id }) { bookmark ->
                                BrowkorfTvListItem(
                                    onClick = { onSelect(bookmark.id) },
                                    headline = bookmark.title?.ifBlank { bookmark.url.orEmpty() }
                                        ?: bookmark.url.orEmpty(),
                                    supportingText = bookmark.url
                                )
                            }
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        BrowkorfTvButton(
                            onClick = onClear,
                            text = stringResource(R.string.clear),
                            modifier = Modifier.weight(1f)
                        )
                        BrowkorfTvButton(
                            onClick = onDismiss,
                            text = stringResource(R.string.cancel),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}
