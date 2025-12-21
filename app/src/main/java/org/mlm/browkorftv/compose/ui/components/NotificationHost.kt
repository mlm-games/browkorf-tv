package org.mlm.browkorftv.compose.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Surface
import androidx.tv.material3.SurfaceDefaults
import androidx.tv.material3.Text
import org.mlm.browkorftv.compose.ui.theme.AppTheme

@Composable
fun NotificationHost(notification: org.mlm.browkorftv.activity.main.NotificationUi?) {
    val c = AppTheme.colors
    AnimatedVisibility(
        visible = notification != null,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        if (notification == null) return@AnimatedVisibility

        Box(Modifier.fillMaxSize()) {
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
                colors = SurfaceDefaults.colors(c.topBarBackground, contentColor = c.textPrimary)
            ) {
                Row(
                    Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        painter = painterResource(notification.iconRes),
                        contentDescription = null,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(notification.message, maxLines = 1)
                }
            }
        }
    }
}