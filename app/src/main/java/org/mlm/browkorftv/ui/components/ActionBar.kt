package org.mlm.browkorftv.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import kotlinx.coroutines.delay
import org.mlm.browkorftv.R
import org.mlm.browkorftv.ui.theme.AppTheme.colors

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
    val colors = colors
    var urlTextFieldValue by remember(currentUrl) {
        mutableStateOf(TextFieldValue(currentUrl))
    }
    var isUrlFocused by remember { mutableStateOf(false) }
    var hasSelectedAll by remember { mutableStateOf(false) }

    LaunchedEffect(isUrlFocused) {
        if (isUrlFocused && !hasSelectedAll) {
            delay(100)
            urlTextFieldValue = urlTextFieldValue.copy(
                selection = TextRange(0, urlTextFieldValue.text.length)
            )
            hasSelectedAll = true
        }
    }

    fun onUrlTextChange(newValue: TextFieldValue) {
        urlTextFieldValue = newValue
        hasSelectedAll = false
    }

    fun onUrlSubmit() {
        onUrlSubmit(urlTextFieldValue.text)
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.topBarBackground)
            .padding(5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        // Close/Menu button
        BrowkorfTvIconButton(
            onClick = onClose,
            painter = painterResource(R.drawable.outline_close_24),
            contentDescription = stringResource(R.string.close_application)
        )

        BrowkorfTvIconButton(
            onClick = onVoiceSearch,
            painter = painterResource(R.drawable.outline_mic_24),
            contentDescription = stringResource(R.string.voice_search)
        )

        // History
        BrowkorfTvIconButton(
            onClick = onHistory,
            painter = painterResource(R.drawable.outline_history_2_24),
            contentDescription = stringResource(R.string.history)
        )

        BrowkorfTvIconButton(
            onClick = onFavorites,
            painter = painterResource(R.drawable.outline_favorite_24),
            contentDescription = stringResource(R.string.favorites)
        )

        BrowkorfTvIconButton(
            onClick = onDownloads,
            painter = painterResource(R.drawable.outline_download_24),
            contentDescription = stringResource(R.string.downloads)
        )

        // Incognito toggle
        BrowkorfTvIconButton(
            onClick = onIncognitoToggle,
            painter = painterResource(R.drawable.ic_incognito),
            contentDescription = stringResource(R.string.incognito_mode),
            checked = isIncognito
        )

        // Settings
        BrowkorfTvIconButton(
            onClick = onSettings,
            painter = painterResource(R.drawable.outline_settings_24),
            contentDescription = stringResource(R.string.settings),
            modifier = Modifier.selectedBackground(isIncognito)
        )

        // URL bar
        Box(
            modifier = Modifier
                .weight(1f)
                .padding(start = 8.dp)
                .background(colors.topBarBackground)
                .onFocusChanged { focusState ->
                    isUrlFocused = focusState.isFocused
                    if (!focusState.isFocused) {
                        hasSelectedAll = false
                    }
                }
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
                value = urlTextFieldValue,
                onValueChange = { onUrlTextChange(it) },
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
                    onSearch = { onUrlSubmit() }
                ),
                decorationBox = { innerTextField ->
                    if (urlTextFieldValue.text.isEmpty()) {
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

@Composable
fun Modifier.selectedBackground(isSelected: Boolean): Modifier {
    return if (isSelected) {
        this.background(colors.textPrimary.copy(alpha = 0.2f), CircleShape)
    } else {
        this
    }
}
