package org.mlm.browkorftv.compose.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Icon
import androidx.tv.material3.IconButton
import androidx.tv.material3.Surface
import androidx.compose.ui.res.painterResource
import androidx.tv.material3.SurfaceDefaults
import org.mlm.browkorftv.R
import org.mlm.browkorftv.compose.ui.theme.AppTheme

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

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Surface(
            modifier = Modifier.offset {
                // center around x/y
                val half = with(density) { (size / 2).roundToPx() }
                IntOffset(xPx - half, yPx - half)
            },
            colors = SurfaceDefaults.colors(c.topBarBackground, c.textPrimary),
        ) {
            Box(Modifier.size(size)) {

                // Center (Dismiss)
                IconButton(
                    onClick = { onAction(CursorMenuAction.Dismiss) },
                    modifier = Modifier.align(androidx.compose.ui.Alignment.Center)
                ) {
                    Icon(painterResource(R.drawable.ic_circle_plus), contentDescription = "Close")
                }

                // Zoom out (left)
                IconButton(
                    onClick = { onAction(CursorMenuAction.ZoomOut) },
                    modifier = Modifier
                        .align(androidx.compose.ui.Alignment.Center)
                        .offset(x = -radius, y = 0.dp)
                ) {
                    Icon(painterResource(R.drawable.ic_zoom_out_gray_24dp), contentDescription = "Zoom Out")
                }

                // Zoom in (right)
                IconButton(
                    onClick = { onAction(CursorMenuAction.ZoomIn) },
                    modifier = Modifier
                        .align(androidx.compose.ui.Alignment.Center)
                        .offset(x = radius, y = 0.dp)
                ) {
                    Icon(painterResource(R.drawable.ic_zoom_in_gray_24dp), contentDescription = "Zoom In")
                }

                // Text select (bottom)
                IconButton(
                    onClick = { onAction(CursorMenuAction.TextSelect) },
                    modifier = Modifier
                        .align(androidx.compose.ui.Alignment.Center)
                        .offset(x = 0.dp, y = radius)
                ) {
                    Icon(painterResource(R.drawable.text_select_start_24), contentDescription = "Text Selection")
                }

                // Link actions (top)
                IconButton(
                    onClick = { onAction(CursorMenuAction.LinkActions) },
                    modifier = Modifier
                        .align(androidx.compose.ui.Alignment.Center)
                        .offset(x = 0.dp, y = -radius)
                ) {
                    Icon(painterResource(R.drawable.ic_menu_24), contentDescription = "Link Actions")
                }

                // Grab mode (optional: make long-press or add as extra icon)
                // If you want it visible, add one more direction.
                IconButton(
                    onClick = { onAction(CursorMenuAction.Grab) },
                    modifier = Modifier
                        .align(androidx.compose.ui.Alignment.Center)
                        .offset(x = -radius, y = -radius / 2)
                ) {
                    Icon(painterResource(R.drawable.ic_circle_plus), contentDescription = "Grab Mode")
                }
            }
        }
    }
}