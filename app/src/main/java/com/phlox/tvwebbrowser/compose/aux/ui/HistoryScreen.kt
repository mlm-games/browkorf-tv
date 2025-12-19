package com.phlox.tvwebbrowser.compose.aux.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.tv.material3.*
import com.phlox.tvwebbrowser.singleton.AppDatabase
import com.phlox.tvwebbrowser.model.HistoryItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HistoryScreen(
    onBack: () -> Unit,
    onPickUrl: (String) -> Unit,
) {
    var loading by remember { mutableStateOf(true) }
    var rows by remember { mutableStateOf<List<HistoryItem>>(emptyList()) }

    LaunchedEffect(Unit) {
        loading = true
        rows = withContext(Dispatchers.IO) {
            // Keep it simple for Phase 4: just show the most recent N entries
            AppDatabase.db.historyDao().last(300)
        }
        loading = false
    }

    val timeFmt = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val dateFmt = remember { SimpleDateFormat.getDateInstance() }

    Column(
        Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("History", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.weight(1f))
            Button(onClick = onBack) { Text("Back") }
        }

        if (loading) {
            Text("Loading…")
            return
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
                Surface(
                    onClick = { onPickUrl(item.url) }
                ) {
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