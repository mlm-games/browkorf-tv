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
import com.phlox.tvwebbrowser.activity.downloads.DownloadsHistoryViewModel
import com.phlox.tvwebbrowser.compose.ui.theme.TvBroTheme
import com.phlox.tvwebbrowser.model.Download
import org.koin.androidx.compose.koinViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun DownloadsScreen(
    onBack: () -> Unit,
    viewModel: DownloadsHistoryViewModel = koinViewModel()
) {
    val ctx = LocalContext.current
    val newlyLoaded by viewModel.newlyLoadedItems.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadNextItems()
    }

    val rows = viewModel.allItems
    val timeFmt = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }

    fun openDownload(d: Download) {
        val path = d.filepath
        if (path.isEmpty()) return
        val uri: Uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
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
            .padding(horizontal = 48.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            Text("Downloads", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.weight(1f))
            Button(onClick = onBack) { Text("Back") }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 20.dp)
        ) {
            items(rows, key = { it.id }) { d ->
                TvBroListItem(
                    onClick = { openDownload(d) },
                    headline = d.filename,
                    supportingText = "${timeFmt.format(Date(d.time))} • ${d.url}"
                )
            }
        }
    }
}

/**
 * Reusable TV List Item
 */
@Composable
fun TvBroListItem(
    onClick: () -> Unit,
    headline: String,
    supportingText: String? = null,
    modifier: Modifier = Modifier
) {
    val colors = TvBroTheme.colors

    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = ClickableSurfaceDefaults.shape(shape = MaterialTheme.shapes.medium),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = colors.buttonBackground, // Use your theme vars
            focusedContainerColor = colors.buttonBackgroundFocused,
            contentColor = colors.textPrimary,
            focusedContentColor = colors.textPrimary
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.02f) // Subtle zoom on focus
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Text(
                text = headline,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1
            )
            if (supportingText != null) {
                Text(
                    text = supportingText,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary,
                    maxLines = 1
                )
            }
        }
    }
}