package com.phlox.tvwebbrowser.compose.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.*
import com.phlox.tvwebbrowser.compose.ui.theme.TvBroTheme

/**
 * Icon button matching TV Bro's button_bg_selector style
 */
@Composable
fun TvBroIconButton(
    onClick: () -> Unit,
    painter: Painter,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    checked: Boolean = false,
    badge: Int? = null
) {
    val colors = TvBroTheme.colors
    var isFocused by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        Surface(
            onClick = onClick,
            enabled = enabled,
            modifier = Modifier
                .size(48.dp)
                .onFocusChanged { isFocused = it.isFocused }
                .border(
                    width = 1.dp,
                    color = when {
                        isFocused -> colors.focusBorder
                        else -> colors.buttonCorner
                    },
                    shape = RoundedCornerShape(5.dp)
                ),
            shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(5.dp)),
            colors = ClickableSurfaceDefaults.colors(
                containerColor = when {
                    checked -> colors.buttonBackgroundFocused
                    else -> colors.buttonBackground
                },
                focusedContainerColor = colors.buttonBackgroundFocused,
                pressedContainerColor = colors.buttonBackgroundPressed,
                disabledContainerColor = colors.buttonBackgroundDisabled
            )
        ) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Icon(
                    painter = painter,
                    contentDescription = contentDescription,
                    modifier = Modifier.size(24.dp),
                    tint = if (enabled) colors.iconColor else colors.iconColorDisabled
                )
            }
        }

        // Badge
        if (badge != null && badge > 0) {
            Surface(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 4.dp, y = (-4).dp)
                    .defaultMinSize(minWidth = 20.dp),
                shape = RoundedCornerShape(50),
                colors = SurfaceDefaults.colors(
                    containerColor = colors.badgeBackground
                )
            ) {
                Text(
                    text = badge.toString(),
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                    fontSize = 12.sp,
                    color = colors.textPrimary
                )
            }
        }
    }
}

@Composable
fun TvBroIconButton(
    onClick: () -> Unit,
    imageVector: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    checked: Boolean = false,
    badge: Int? = null
) {
    val colors = TvBroTheme.colors
    var isFocused by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        Surface(
            onClick = onClick,
            enabled = enabled,
            modifier = Modifier
                .size(48.dp)
                .onFocusChanged { isFocused = it.isFocused }
                .border(
                    width = 1.dp,
                    color = when {
                        isFocused -> colors.focusBorder
                        else -> colors.buttonCorner
                    },
                    shape = RoundedCornerShape(5.dp)
                ),
            shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(5.dp)),
            colors = ClickableSurfaceDefaults.colors(
                containerColor = when {
                    checked -> colors.buttonBackgroundFocused
                    else -> colors.buttonBackground
                },
                focusedContainerColor = colors.buttonBackgroundFocused,
                pressedContainerColor = colors.buttonBackgroundPressed,
                disabledContainerColor = colors.buttonBackgroundDisabled
            )
        ) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = imageVector,
                    contentDescription = contentDescription,
                    modifier = Modifier.size(24.dp),
                    tint = if (enabled) colors.iconColor else colors.iconColorDisabled
                )
            }
        }

        // Badge
        if (badge != null && badge > 0) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 4.dp, y = (-4).dp)
                    .defaultMinSize(minWidth = 20.dp)
                    .background(colors.badgeBackground, RoundedCornerShape(50))
                    .border(1.dp, colors.badgeStroke, RoundedCornerShape(50))
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = badge.toString(),
                    fontSize = 12.sp,
                    color = colors.textPrimary
                )
            }
        }
    }
}

/**
 * Simple text button
 */
@Composable
fun TvBroButton(
    onClick: () -> Unit,
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val colors = TvBroTheme.colors
    var isFocused by remember { mutableStateOf(false) }

    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .onFocusChanged { isFocused = it.isFocused }
            .border(
                width = 1.dp,
                color = if (isFocused) colors.focusBorder else colors.buttonCorner,
                shape = RoundedCornerShape(5.dp)
            ),
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(5.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = colors.buttonBackground,
            focusedContainerColor = colors.buttonBackgroundFocused,
            pressedContainerColor = colors.buttonBackgroundPressed,
            disabledContainerColor = colors.buttonBackgroundDisabled
        )
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            color = if (enabled) colors.textPrimary else colors.textSecondary
        )
    }
}