package com.phlox.tvwebbrowser.compose.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.phlox.tvwebbrowser.compose.ui.theme.TvBroTheme

@Composable
fun TvBroProgressBar(
    progress: Int,
    modifier: Modifier = Modifier
) {
    val colors = TvBroTheme.colors

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(3.dp)
            .background(colors.topBarBackground)
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(progress / 100f)
                .background(colors.progressTint)
        )
    }
}