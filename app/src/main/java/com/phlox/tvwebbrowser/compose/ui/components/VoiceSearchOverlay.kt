package com.phlox.tvwebbrowser.compose.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.*
import com.phlox.tvwebbrowser.R
import com.phlox.tvwebbrowser.compose.runtime.VoiceUiState
import com.phlox.tvwebbrowser.compose.ui.theme.TvBroTheme

@Composable
fun VoiceSearchOverlay(
    state: VoiceUiState,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = TvBroTheme.colors

    // Pulsing animation based on audio level
    val scale by animateFloatAsState(
        targetValue = 1f + (state.rmsDb / 20f).coerceIn(0f, 0.5f),
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "micScale"
    )

    // Fade animation
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(60.dp)
            .background(colors.topBarBackground),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Animated microphone icon
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .scale(scale)
                    .background(
                        color = colors.progressTint.copy(alpha = alpha * 0.3f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_mic_none_grey_900_36dp),
                    contentDescription = stringResource(R.string.voice_search),
                    modifier = Modifier.size(36.dp),
                    tint = colors.progressTint
                )
            }

            // Partial results text
            Text(
                text = state.partialText.ifBlank { 
                    stringResource(R.string.speak)
                },
                color = colors.textPrimary,
                fontSize = 20.sp,
                modifier = Modifier.weight(1f),
                maxLines = 1
            )

            // Cancel button
            TvBroIconButton(
                onClick = onCancel,
                painter = painterResource(R.drawable.ic_close_grey_900_24dp),
                contentDescription = stringResource(R.string.cancel)
            )
        }
    }
}