package com.phlox.tvwebbrowser.compose.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import com.phlox.tvwebbrowser.R
import com.phlox.tvwebbrowser.compose.ui.theme.TvBroTheme

@Composable
fun ActionBar(
    currentUrl: String,
    isIncognito: Boolean,
    onClose: () -> Unit,
    onVoiceSearch: () -> Unit,
    onHistory: () -> Unit,
    onFavorites: () -> Unit,
    onDownloads: () -> Unit,
    onIncognitoToggle: () -> Unit,
    onSettings: () -> Unit,
    onUrlSubmit: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = TvBroTheme.colors
    var urlText by remember(currentUrl) { mutableStateOf(currentUrl) }
    var isUrlFocused by remember { mutableStateOf(false) }
    
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.topBarBackground)
            .padding(5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        // Close/Menu button
        TvBroIconButton(
            onClick = onClose,
            painter = painterResource(R.drawable.ic_close_grey_900_36dp),
            contentDescription = stringResource(R.string.close_application)
        )
        
        // Voice search
        TvBroIconButton(
            onClick = onVoiceSearch,
            painter = painterResource(R.drawable.ic_mic_none_grey_900_36dp),
            contentDescription = stringResource(R.string.voice_search)
        )
        
        // History
        TvBroIconButton(
            onClick = onHistory,
            painter = painterResource(R.drawable.ic_history_grey_900_36dp),
            contentDescription = stringResource(R.string.history)
        )
        
        // Favorites
        TvBroIconButton(
            onClick = onFavorites,
            painter = painterResource(R.drawable.ic_star_border_grey_900_36dp),
            contentDescription = stringResource(R.string.favorites)
        )
        
        // Downloads
        TvBroIconButton(
            onClick = onDownloads,
            painter = painterResource(R.drawable.ic_file_download_grey_900),
            contentDescription = stringResource(R.string.downloads)
        )
        
        // Incognito toggle
        TvBroIconButton(
            onClick = onIncognitoToggle,
            painter = painterResource(R.drawable.ic_incognito),
            contentDescription = stringResource(R.string.incognito_mode),
            checked = isIncognito
        )
        
        // Settings
        TvBroIconButton(
            onClick = onSettings,
            painter = painterResource(R.drawable.ic_settings_grey_900_24dp),
            contentDescription = stringResource(R.string.settings)
        )
        
        // URL bar
        Box(
            modifier = Modifier
                .weight(1f)
                .padding(start = 8.dp)
                .background(colors.topBarBackground)
                .onFocusChanged { isUrlFocused = it.isFocused }
                .then(
                    if (isUrlFocused) Modifier.border(
                        1.dp,
                        colors.focusBorder,
                        RoundedCornerShape(4.dp)
                    ) else Modifier
                )
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            BasicTextField(
                value = urlText,
                onValueChange = { urlText = it },
                modifier = Modifier.fillMaxWidth(),
                textStyle = TextStyle(
                    color = colors.textPrimary,
                    fontSize = 16.sp
                ),
                cursorBrush = SolidColor(colors.textPrimary),
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Uri,
                    imeAction = ImeAction.Search
                ),
                keyboardActions = KeyboardActions(
                    onSearch = { onUrlSubmit(urlText) }
                ),
                decorationBox = { innerTextField ->
                    if (urlText.isEmpty()) {
                        Text(
                            text = stringResource(R.string.url_prompt),
                            color = colors.textSecondary,
                            fontSize = 16.sp
                        )
                    }
                    innerTextField()
                }
            )
        }
    }
}