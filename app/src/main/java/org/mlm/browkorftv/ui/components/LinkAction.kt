package org.mlm.browkorftv.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Button
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.SurfaceDefaults
import org.mlm.browkorftv.ui.theme.AppTheme

enum class LinkAction { Refresh, OpenInNewTab, OpenExternal, Copy, Download, Share }

@Composable
fun LinkActionsDialog(
    canOpenUrlActions: Boolean,
    canCopyShare: Boolean,
    onDismiss: () -> Unit,
    onAction: (LinkAction) -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = true)
    ) {
        val c = AppTheme.colors
        Surface(
            colors = SurfaceDefaults.colors(c.topBarBackground, contentColor = c.textPrimary),
            shape = RoundedCornerShape(5.dp)
        ) {
            Column(
                Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Link actions", style = MaterialTheme.typography.titleLarge)

                Button(onClick = { onAction(LinkAction.Refresh) }) { Text("Refresh") }

                if (canOpenUrlActions) {
                    Button(onClick = { onAction(LinkAction.OpenInNewTab) }) { Text("Open in new tab") }
                    Button(onClick = { onAction(LinkAction.OpenExternal) }) { Text("Open in external app") }
                    Button(onClick = { onAction(LinkAction.Download) }) { Text("Download") }
                }

                if (canCopyShare) {
                    Button(onClick = { onAction(LinkAction.Copy) }) { Text("Copy") }
                    Button(onClick = { onAction(LinkAction.Share) }) { Text("Share") }
                }

                Button(onClick = onDismiss) { Text("Close") }
            }
        }
    }
}