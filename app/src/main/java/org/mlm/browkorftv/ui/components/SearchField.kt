package org.mlm.browkorftv.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import org.mlm.browkorftv.R
import org.mlm.browkorftv.ui.theme.AppTheme

@Composable
fun SearchField(
    onSearch: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = AppTheme.colors
    var query by rememberSaveable { mutableStateOf("") }
    var focused by remember { mutableStateOf(false) }

    fun submit() {
        val value = query.trim()
        if (value.isNotEmpty()) {
            onSearch(value)
            query = ""
        }
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 56.dp)
                .background(
                    colors.buttonBackground,
                    RoundedCornerShape(8.dp)
                )
                .border(
                    width = if (focused) 2.dp else 1.dp,
                    color = if (focused) colors.focusBorder else colors.buttonBackground,
                    shape = RoundedCornerShape(8.dp)
                )
                .onFocusChanged { focused = it.isFocused }
                .padding(horizontal = 14.dp, vertical = 14.dp)
        ) {
            BasicTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                textStyle = TextStyle(
                    color = colors.textPrimary,
                    fontSize = 16.sp
                ),
                cursorBrush = SolidColor(colors.textPrimary),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { submit() }),
                decorationBox = { innerTextField ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 28.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        if (query.isEmpty()) {
                            Text(
                                text = stringResource(R.string.search_web),
                                color = colors.textSecondary,
                                fontSize = 16.sp
                            )
                        }
                        innerTextField()
                    }
                }
            )
        }

        BrowkorfTvIconButton(
            onClick = ::submit,
            painter = painterResource(R.drawable.outline_search_24),
            contentDescription = stringResource(R.string.search),
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}
