package org.mlm.browkorftv.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
private fun TvIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    content: @Composable BoxScope.() -> Unit
) {
    val isPressed by interactionSource.collectIsPressedAsState()
    var wasPressed by remember { mutableStateOf(false) }

    LaunchedEffect(isPressed) {
        if (wasPressed && !isPressed) {
            onClick()
        }
        wasPressed = isPressed
    }

    IconButton(
        onClick = {},
        modifier = modifier,
        interactionSource = interactionSource,
        content = content
    )
}

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
    val dismissInteractionSource = remember { MutableInteractionSource() }
    val zoomOutInteractionSource = remember { MutableInteractionSource() }
    val zoomInInteractionSource = remember { MutableInteractionSource() }
    val textSelectInteractionSource = remember { MutableInteractionSource() }
    val linkActionsInteractionSource = remember { MutableInteractionSource() }
    val grabInteractionSource = remember { MutableInteractionSource() }

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

                TvIconButton(
                    onClick = { onAction(CursorMenuAction.Dismiss) },
                    interactionSource = dismissInteractionSource,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .focusRequester(focusRequester)
                ) {
                    Icon(painterResource(R.drawable.outline_close_24), contentDescription = "Close")
                }

                TvIconButton(
                    onClick = { onAction(CursorMenuAction.ZoomOut) },
                    interactionSource = zoomOutInteractionSource,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .offset(x = -radius, y = 0.dp)
                ) {
                    Icon(
                        painterResource(R.drawable.outline_zoom_out_24),
                        contentDescription = "Zoom Out"
                    )
                }

                TvIconButton(
                    onClick = { onAction(CursorMenuAction.ZoomIn) },
                    interactionSource = zoomInInteractionSource,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .offset(x = radius, y = 0.dp)
                ) {
                    Icon(
                        painterResource(R.drawable.outline_zoom_in_24),
                        contentDescription = "Zoom In"
                    )
                }

                TvIconButton(
                    onClick = { onAction(CursorMenuAction.TextSelect) },
                    interactionSource = textSelectInteractionSource,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .offset(x = 0.dp, y = radius)
                ) {
                    Icon(
                        painterResource(R.drawable.outline_text_select_start_24),
                        contentDescription = "Text Selection"
                    )
                }

                TvIconButton(
                    onClick = { onAction(CursorMenuAction.LinkActions) },
                    interactionSource = linkActionsInteractionSource,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .offset(x = 0.dp, y = -radius)
                ) {
                    Icon(
                        painterResource(R.drawable.outline_menu_open_24),
                        contentDescription = "Link Actions"
                    )
                }

                TvIconButton(
                    onClick = { onAction(CursorMenuAction.Grab) },
                    interactionSource = grabInteractionSource,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .offset(x = -radius, y = -radius)
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