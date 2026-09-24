package org.mlm.browkorftv.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.SurfaceDefaults
import org.mlm.browkorftv.R
import org.mlm.browkorftv.ui.theme.AppTheme

enum class LinkAction { Refresh, OpenInNewTab, OpenInCurrentTab, OpenExternal, Copy, Download, Share }

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
    singleTabMode: Boolean = false,
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
                Text(stringResource(R.string.link_actions), style = MaterialTheme.typography.titleLarge)

                LinkActionButton(stringResource(R.string.refresh_page)) { onAction(LinkAction.Refresh) }

                if (canOpenUrlActions) {
                    if (singleTabMode) {
                        LinkActionButton(stringResource(R.string.open_in_current_tab)) {
                            onAction(LinkAction.OpenInCurrentTab)
                        }
                    } else {
                        LinkActionButton(stringResource(R.string.open_in_new_tab)) {
                            onAction(LinkAction.OpenInNewTab)
                        }
                    }
                    LinkActionButton(stringResource(R.string.open_in_external_application)) {
                        onAction(LinkAction.OpenExternal)
                    }
                    LinkActionButton(stringResource(R.string.download)) { onAction(LinkAction.Download) }
                }

                if (canCopyShare) {
                    LinkActionButton(stringResource(R.string.copy)) { onAction(LinkAction.Copy) }
                    LinkActionButton(stringResource(R.string.share)) { onAction(LinkAction.Share) }
                }

                LinkActionButton(stringResource(R.string.close), onDismiss)
            }
        }
    }
}