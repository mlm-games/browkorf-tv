package com.phlox.tvwebbrowser.compose.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.net.toUri
import androidx.tv.material3.*
import com.phlox.tvwebbrowser.R
import com.phlox.tvwebbrowser.compose.ui.theme.TvBroTheme

@Composable
fun LinkActionsDialog(
    href: String,
    onOpen: () -> Unit,
    onOpenInNewTab: () -> Unit,
    onCopy: () -> Unit,
    onShare: () -> Unit,
    onOpenExternal: () -> Unit,
    onDownload: () -> Unit,
    onDismiss: () -> Unit
) {
    val colors = TvBroTheme.colors
    val scheme = remember(href) { 
        runCatching { href.toUri().scheme?.lowercase() }.getOrNull() 
    }
    val isHttpLike = scheme == "http" || scheme == "https"

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(48.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .widthIn(max = 500.dp)
                    .border(
                        width = 1.dp,
                        color = colors.buttonCorner,
                        shape = RoundedCornerShape(8.dp)
                    ),
                shape = RoundedCornerShape(8.dp),
                colors = SurfaceDefaults.colors(colors.background),
                tonalElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Title
                    Text(
                        text = stringResource(R.string.link_options),
                        style = MaterialTheme.typography.titleLarge,
                        color = colors.textPrimary
                    )

                    // URL display
                    Text(
                        text = href,
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                colors.topBarBackground,
                                RoundedCornerShape(4.dp)
                            )
                            .padding(8.dp)
                    )

                    Spacer(Modifier.height(8.dp))

                    // Copy & Share row
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        TvBroButton(
                            onClick = onCopy,
                            text = stringResource(R.string.copy_to_clipboard),
                            modifier = Modifier.weight(1f)
                        )
                        TvBroButton(
                            onClick = onShare,
                            text = stringResource(R.string.share_url),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // HTTP-specific actions
                    if (isHttpLike) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            TvBroButton(
                                onClick = onOpen,
                                text = stringResource(R.string.open_new_tab),
                                modifier = Modifier.weight(1f)
                            )
                            TvBroButton(
                                onClick = onOpenInNewTab,
                                text = stringResource(R.string.open_in_new_tab),
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            TvBroButton(
                                onClick = onOpenExternal,
                                text = stringResource(R.string.open_in_external_application),
                                modifier = Modifier.weight(1f)
                            )
                            TvBroButton(
                                onClick = onDownload,
                                text = stringResource(R.string.download),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    } else {
                        // Non-HTTP links
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            TvBroButton(
                                onClick = onOpenExternal,
                                text = stringResource(R.string.open_in_external_application),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    // Cancel button
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.CenterEnd
                    ) {
                        TvBroButton(
                            onClick = onDismiss,
                            text = stringResource(R.string.cancel)
                        )
                    }
                }
            }
        }
    }
}