package org.mlm.browkorftv.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.SurfaceDefaults
import org.mlm.browkorftv.ui.theme.AppTheme

enum class LinkAction { Refresh, OpenInNewTab, OpenExternal, Copy, Download, Share }

@Composable
private fun LinkActionButton(
    text: String,
    onClick: () -> Unit,
) {
    BrowkorfTvButton(
        onClick = onClick,
        text = text,
        colors = ButtonDefaults.colors()
    )
}

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

                LinkActionButton("Refresh") { onAction(LinkAction.Refresh) }

                if (canOpenUrlActions) {
                    LinkActionButton("Open in new tab") { onAction(LinkAction.OpenInNewTab) }
                    LinkActionButton("Open in external app") { onAction(LinkAction.OpenExternal) }
                    LinkActionButton("Download") { onAction(LinkAction.Download) }
                }

                if (canCopyShare) {
                    LinkActionButton("Copy") { onAction(LinkAction.Copy) }
                    LinkActionButton("Share") { onAction(LinkAction.Share) }
                }

                LinkActionButton("Close", onDismiss)
            }
        }
    }
}