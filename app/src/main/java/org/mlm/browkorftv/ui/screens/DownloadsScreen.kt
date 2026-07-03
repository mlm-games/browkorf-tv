package org.mlm.browkorftv.ui.screens

import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.*
import org.mlm.browkorftv.BuildConfig
import org.mlm.browkorftv.R
import org.mlm.browkorftv.activity.main.DownloadsHistoryViewModel
import org.mlm.browkorftv.model.Download
import org.koin.androidx.compose.koinViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import androidx.core.net.toUri
import org.mlm.browkorftv.ui.components.BrowkorfTvListItem

@Composable
fun DownloadsScreen(
    onBack: () -> Unit,
    viewModel: DownloadsHistoryViewModel = koinViewModel()
) {
    val ctx = LocalContext.current
    val rows by viewModel.items.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.loadNextItems()
    }

    val timeFmt = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }

    fun openDownload(d: Download) {
        val path = d.filepath
        if (path.isEmpty()) return
        val uri: Uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            path.toUri()
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
            .padding(horizontal = 48.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Downloads", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.weight(1f))
            if (rows.isNotEmpty()) {
                IconButton(
                    onClick = {
                        viewModel.deleteAll()
                    }
                ) { Icon(
                    painterResource(R.drawable.outline_clear_all_24),
                    "Clear All",
                    tint = MaterialTheme.colorScheme.error
                ) }
            }
            IconButton(onClick = onBack) { Icon(painterResource(R.drawable.outline_chevron_forward_24),"Back") }
        }

        if (rows.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No downloads", style = MaterialTheme.typography.bodyLarge)
            }
            return
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 20.dp)
        ) {
            items(rows, key = { it.id }) { d ->
                BrowkorfTvListItem(
                    onClick = { openDownload(d) },
                    headline = d.filename,
                    supportingText = "${timeFmt.format(Date(d.time))} • ${d.url}"
                )
            }
        }
    }
}
