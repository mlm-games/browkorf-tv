package com.phlox.tvwebbrowser.compose.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.tv.material3.*
import com.phlox.tvwebbrowser.compose.ui.nav.AppKey
import com.phlox.tvwebbrowser.compose.vm.BookmarkEditorViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun BookmarkEditorScreen(backStack: NavBackStack<NavKey>, id: Long?) {
    val vm: BookmarkEditorViewModel = koinViewModel()
    val state by vm.state.collectAsStateWithLifecycle()

    LaunchedEffect(id) { vm.load(id) }

    MaterialTheme {
        Column(Modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(if (id == null) "New Bookmark" else "Edit Bookmark", style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.weight(1f))
                Button(onClick = { backStack.removeLastOrNull() }) { Text("Back") }
            }

            OutlinedTextField(
                value = state.title,
                onValueChange = vm::setTitle,
                label = { androidx.compose.material3.Text("Title") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = state.url,
                onValueChange = vm::setUrl,
                label = { androidx.compose.material3.Text("URL") },
                modifier = Modifier.fillMaxWidth()
            )

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(onClick = { vm.save { backStack.removeLastOrNull() } }) { Text("Save") }
                if (id != null) {
                    Button(onClick = { vm.delete { backStack.removeLastOrNull() } }) { Text("Delete") }
                }
            }
        }
    }
}