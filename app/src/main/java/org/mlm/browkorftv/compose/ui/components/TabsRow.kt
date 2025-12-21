package org.mlm.browkorftv.compose.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.*
import org.mlm.browkorftv.R
import org.mlm.browkorftv.compose.ui.theme.AppTheme
import org.mlm.browkorftv.model.WebTabState
import org.mlm.browkorftv.singleton.FaviconsPool
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun TabsRow(
    tabs: List<WebTabState>,
    currentTabId: Long?,
    onSelectTab: (WebTabState) -> Unit,
    onAddTab: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = AppTheme.colors

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.topBarBackground)
    ) {
        // Tabs list
        LazyRow(
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 5.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            contentPadding = PaddingValues(horizontal = 5.dp)
        ) {
            itemsIndexed(tabs, key = { index, tab ->
                if (tab.id != 0L) tab.id else -(index + 1).toLong()
            }) { index, tab ->                TabItem(
                    tab = tab,
                    isSelected = tab.id == currentTabId,
                    onClick = { onSelectTab(tab) }
                )
            }
        }

        // Add tab button
        AddTabButton(
            onClick = onAddTab,
            modifier = Modifier.padding(5.dp)
        )
    }

    // Bottom spacer
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(10.dp)
            .background(colors.topBarBackground2)
    )
}

@Composable
private fun TabItem(
    tab: WebTabState,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val colors = AppTheme.colors
    var isFocused by remember { mutableStateOf(false) }

    // Load Favicon Async
    val favicon by produceState<Bitmap?>(initialValue = null, key1 = tab.url) {
        value = withContext(Dispatchers.IO) {
            FaviconsPool.get(tab.url)
        }
    }

    val containerColor = when {
        isSelected -> colors.tabBackgroundSelected
        else -> colors.tabBackground
    }

    Surface(
        onClick = onClick,
        modifier = Modifier
            .width(200.dp)
            .height(50.dp)
            .onFocusChanged { isFocused = it.isFocused },
        shape = ClickableSurfaceDefaults.shape(
            RoundedCornerShape(topStart = 5.dp, topEnd = 5.dp)
        ),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = containerColor,
            focusedContainerColor = colors.buttonBackgroundFocused
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (favicon != null) {
                Image(
                    bitmap = favicon!!.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    contentScale = ContentScale.Fit
                )
            } else {
                Icon(
                    painter = painterResource(R.drawable.ic_launcher), // Fallback to launcher or ic_not_available
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = colors.iconColorDisabled
                )
            }

            Spacer(Modifier.width(8.dp))

            Text(
                text = tab.title.ifBlank { tab.url.ifBlank { "New Tab" } },
                color = if (isSelected || isFocused)
                    colors.tabTextColorSelected
                else
                    colors.tabTextColor,
                fontSize = 16.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun AddTabButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = AppTheme.colors
    var isFocused by remember { mutableStateOf(false) }

    Surface(
        onClick = onClick,
        modifier = modifier
            .size(40.dp)
            .onFocusChanged { isFocused = it.isFocused },
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(5.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = colors.buttonBackground,
            focusedContainerColor = colors.buttonBackgroundFocused
        )
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = "+",
                fontSize = 26.sp,
                color = colors.textPrimary
            )
        }
    }
}