package com.phlox.tvwebbrowser.compose.aux.ui

import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.tv.material3.*
import com.phlox.tvwebbrowser.BuildConfig
import com.phlox.tvwebbrowser.model.Download
import com.phlox.tvwebbrowser.singleton.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun DownloadsScreen(
    onBack: () -> Unit,
) {
    val ctx = LocalContext.current

    var loading by remember { mutableStateOf(true) }
    var rows by remember { mutableStateOf<List<Download>>(emptyList()) }

    LaunchedEffect(Unit) {
        loading = true
        rows = withContext(Dispatchers.IO) {
            // Phase 4: show last N downloads
            AppDatabase.db.downloadDao().allByLimitOffset(0)
        }
        loading = false
    }

    val timeFmt = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }

    fun openDownload(d: Download) {
        val path = d.filepath ?: return
        val uri: Uri =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                Uri.parse(path)
            } else {
                val file = File(path)
                FileProvider.getUriForFile(ctx, BuildConfig.APPLICATION_ID + ".provider", file)
            }

        val mime = ctx.contentResolver.getType(uri) ?: "*/*"
        val i = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mime)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        runCatching { ctx.startActivity(i) }
    }

    Column(
        Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Downloads", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.weight(1f))
            Button(onClick = onBack) { Text("Back") }
        }

        if (loading) {
            Text("Loading…")
            return
        }

        if (rows.isEmpty()) {
            Text("No downloads")
            return
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(rows, key = { it.id }) { d ->
                Surface(onClick = { openDownload(d) }) {
                    Column(Modifier.fillMaxWidth().padding(12.dp)) {
                        Text(d.filename, maxLines = 1)
                        Text(d.url, maxLines = 1, style = MaterialTheme.typography.bodySmall)
                        Text("Time: ${timeFmt.format(Date(d.time))}", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}