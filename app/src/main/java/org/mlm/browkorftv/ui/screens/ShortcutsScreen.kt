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
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import org.mlm.browkorftv.R
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

    val bindings by shortcutMgr.bindings.collectAsState()
    val snackbarManager: SnackbarManager = koinInject()
    val scope = rememberCoroutineScope()

    var editingShortcut by remember { mutableStateOf<Shortcut?>(null) }

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
                BrowkorfTvListItem(
                    onClick = { editingShortcut = shortcut },
                    headline = stringResource(shortcut.titleResId),
                    supportingText = Shortcut.shortcutKeysToString(shortcut, binding, context)
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
                        snackbarManager.show(
                            message = context.getString(R.string.shortcut_key_reserved_for_navigation),
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
            onDismiss = { editingShortcut = null }
        )
    }
}

@Composable
private fun ShortcutEditDialog(
    shortcut: Shortcut,
    onSetKey: (keyCode: Int, modifiers: Int) -> Unit,
    onClearKey: () -> Unit,
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
                        Text(
                            text = stringResource(R.string.action) + ": " + stringResource(shortcut.titleResId),
                            color = colors.textPrimary
                        )
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
