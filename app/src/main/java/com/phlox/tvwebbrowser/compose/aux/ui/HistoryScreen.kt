package com.phlox.tvwebbrowser.compose.aux.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.phlox.tvwebbrowser.activity.history.HistoryViewModel
import org.koin.androidx.compose.koinViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HistoryScreen(
    onBack: () -> Unit,
    onPickUrl: (String) -> Unit,
    viewModel: HistoryViewModel = koinViewModel()
) {
    val rows by viewModel.lastLoadedItems.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadItems()
    }

    val timeFmt = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val dateFmt = remember { SimpleDateFormat.getDateInstance() }

    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("History", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.weight(1f))
            Button(onClick = onBack) { Text("Back") }
        }

        if (rows.isEmpty()) {
            Text("No history")
            return
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(rows, key = { it.id }) { item ->
                Surface(onClick = { onPickUrl(item.url) }) {
                    Column(Modifier.fillMaxWidth().padding(12.dp)) {
                        Text(item.title.ifBlank { item.url }, maxLines = 1)
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(timeFmt.format(Date(item.time)), style = MaterialTheme.typography.bodySmall)
                            Text(dateFmt.format(Date(item.time)), style = MaterialTheme.typography.bodySmall)
                        }
                        Text(item.url, maxLines = 1, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}