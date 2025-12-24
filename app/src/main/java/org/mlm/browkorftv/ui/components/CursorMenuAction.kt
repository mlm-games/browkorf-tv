package org.mlm.browkorftv.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Icon
import androidx.tv.material3.IconButton
import androidx.tv.material3.Surface
import androidx.compose.ui.res.painterResource
import androidx.tv.material3.SurfaceDefaults
import kotlinx.coroutines.delay
import org.mlm.browkorftv.R
import org.mlm.browkorftv.ui.theme.AppTheme

enum class CursorMenuAction { Grab, TextSelect, ZoomIn, ZoomOut, LinkActions, Dismiss }

@Composable
fun CursorRadialMenu(
    xPx: Int,
    yPx: Int,
    onAction: (CursorMenuAction) -> Unit,
) {
    val c = AppTheme.colors
    val size = 180.dp
    val radius = 70.dp
    val density = LocalDensity.current

    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        delay(100)
        runCatching { focusRequester.requestFocus() }
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Surface(
            modifier = Modifier.offset {
                // center around x/y
                val half = with(density) { (size / 2).roundToPx() }
                IntOffset(xPx - half, yPx - half)
            },
            colors = SurfaceDefaults.colors(Color.Transparent, c.textPrimary),
            shape = RoundedCornerShape(4.dp)
        ) {
            Box(Modifier.size(size)) {

                // Center (Dismiss)
                IconButton(
                    onClick = { onAction(CursorMenuAction.Dismiss) },
                    modifier = Modifier
                        .align(Alignment.Center)
                        .focusRequester(focusRequester)
                ) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                }

                // Zoom out (left)
                IconButton(
                    onClick = { onAction(CursorMenuAction.ZoomOut) },
                    modifier = Modifier
                        .align(Alignment.Center)
                        .offset(x = -radius, y = 0.dp)
                ) {
                    Icon(
                        painterResource(R.drawable.outline_zoom_out_24),
                        contentDescription = "Zoom Out"
                    )
                }

                // Zoom in (right)
                IconButton(
                    onClick = { onAction(CursorMenuAction.ZoomIn) },
                    modifier = Modifier
                        .align(Alignment.Center)
                        .offset(x = radius, y = 0.dp)
                ) {
                    Icon(
                        painterResource(R.drawable.outline_zoom_in_24),
                        contentDescription = "Zoom In"
                    )
                }

                // Text select (bottom)
                IconButton(
                    onClick = { onAction(CursorMenuAction.TextSelect) },
                    modifier = Modifier
                        .align(Alignment.Center)
                        .offset(x = 0.dp, y = radius)
                ) {
                    Icon(
                        painterResource(R.drawable.outline_text_select_start_24),
                        contentDescription = "Text Selection"
                    )
                }

                // Link actions (top)
                IconButton(
                    onClick = { onAction(CursorMenuAction.LinkActions) },
                    modifier = Modifier
                        .align(Alignment.Center)
                        .offset(x = 0.dp, y = -radius)
                ) {
                    Icon(
                        painterResource(R.drawable.outline_menu_open_24),
                        contentDescription = "Link Actions"
                    )
                }

                // Grab mode (not needed rn?: make long-press or add as extra icon)
                // If you want it visible, add one more direction.
                IconButton(
                    onClick = { onAction(CursorMenuAction.Grab) },
                    modifier = Modifier
                        .align(Alignment.Center)
                        .offset(x = -radius, y = -radius / 2)
                ) {
                    Icon(
                        painterResource(R.drawable.outline_grab_24),
                        contentDescription = "Grab Mode"
                    )
                }
            }
        }
    }
}