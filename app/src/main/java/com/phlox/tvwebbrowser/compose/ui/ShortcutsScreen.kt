package com.phlox.tvwebbrowser.compose.ui

import android.view.KeyEvent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.tv.material3.*
import com.phlox.tvwebbrowser.R
import com.phlox.tvwebbrowser.compose.ui.components.TvBroButton
import com.phlox.tvwebbrowser.compose.ui.theme.TvBroTheme
import com.phlox.tvwebbrowser.singleton.shortcuts.Shortcut
import com.phlox.tvwebbrowser.singleton.shortcuts.ShortcutMgr

@Composable
fun ShortcutsScreen(
    backStack: NavBackStack<NavKey>
) {
    val colors = TvBroTheme.colors
    val shortcutMgr = remember { ShortcutMgr.getInstance() }
    
    // Force recomposition when shortcuts change
    var refreshTrigger by remember { mutableIntStateOf(0) }
    val shortcuts = remember(refreshTrigger) { shortcutMgr.findForId(-1) /*Dummy call*/; Shortcut.entries }

    var editingShortcut by remember { mutableStateOf<Shortcut?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .padding(22.dp)
    ) {
        Text(
            text = stringResource(R.string.shortcuts),
            style = MaterialTheme.typography.headlineMedium,
            color = colors.textPrimary
        )
        Text(
            text = stringResource(R.string.shortcuts),
            style = MaterialTheme.typography.bodyMedium,
            color = colors.textSecondary,
            modifier = Modifier.padding(top = 8.dp, bottom = 16.dp)
        )

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(shortcuts) { shortcut ->
                ShortcutItemRow(shortcut = shortcut, onClick = { editingShortcut = shortcut })
            }
        }

        Spacer(Modifier.height(16.dp))
        TvBroButton(
            onClick = { 
                if (backStack.size > 1) backStack.removeAt(backStack.lastIndex) 
            },
            text = stringResource(R.string.navigate_back)
        )
    }

    editingShortcut?.let { shortcut ->
        ShortcutEditDialog(
            shortcut = shortcut,
            onSetKey = { keyCode ->
                shortcut.keyCode = keyCode
                shortcutMgr.save(shortcut)
                refreshTrigger++
                editingShortcut = null
            },
            onClearKey = {
                shortcut.keyCode = 0
                shortcutMgr.save(shortcut)
                refreshTrigger++
                editingShortcut = null
            },
            onDismiss = { editingShortcut = null }
        )
    }
}

@Composable
private fun ShortcutItemRow(shortcut: Shortcut, onClick: () -> Unit) {
    val colors = TvBroTheme.colors
    var isFocused by remember { mutableStateOf(false) }
    val keyName = remember(shortcut.keyCode) {
        if (shortcut.keyCode != 0) KeyEvent.keyCodeToString(shortcut.keyCode).removePrefix("KEYCODE_") else "—"
    }

    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .onFocusChanged { isFocused = it.isFocused }
            .border(
                width = 1.dp,
                color = if (isFocused) colors.focusBorder else colors.buttonCorner,
                shape = RoundedCornerShape(5.dp)
            ),
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(5.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = colors.background,
            focusedContainerColor = colors.buttonBackgroundFocused
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(15.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(shortcut.titleResId),
                color = colors.textPrimary,
                fontSize = 18.sp,
                modifier = Modifier.weight(1f)
            )
            Text(text = keyName, color = colors.textSecondary, fontSize = 18.sp)
        }
    }
}

@Composable
private fun ShortcutEditDialog(
    shortcut: Shortcut,
    onSetKey: (Int) -> Unit,
    onClearKey: () -> Unit,
    onDismiss: () -> Unit
) {
    val colors = TvBroTheme.colors
    var waitingForKey by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(dismissOnBackPress = !waitingForKey)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(48.dp)
                .then(if (waitingForKey) Modifier.onKeyEvent { event ->
                    if (event.nativeKeyEvent.action == KeyEvent.ACTION_DOWN) {
                        val code = event.nativeKeyEvent.keyCode
                        if (code != KeyEvent.KEYCODE_BACK && code != KeyEvent.KEYCODE_DPAD_CENTER) {
                            onSetKey(code)
                            return@onKeyEvent true
                        }
                    }
                    false
                } else Modifier),
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
                        TvBroButton(onClick = { waitingForKey = false }, text = stringResource(R.string.cancel))
                    } else {
                        Text(
                            text = stringResource(R.string.action) + ": " + stringResource(shortcut.titleResId),
                            color = colors.textPrimary
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            TvBroButton(onClick = { waitingForKey = true }, text = stringResource(R.string.set_key_for_action), modifier = Modifier.weight(1f))
                            TvBroButton(onClick = onClearKey, text = stringResource(R.string.clear), modifier = Modifier.weight(1f))
                        }
                        TvBroButton(onClick = onDismiss, text = stringResource(R.string.cancel))
                    }
                }
            }
        }
    }
}