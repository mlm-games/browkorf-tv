package org.mlm.browkorftv.ui.components

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.pointer.pointerInput

@Composable
fun Modifier.tvPointerClick(
    onClick: () -> Unit,
    enabled: Boolean = true,
    requestFocusOnPress: Boolean = true,
): Modifier {
    val focusRequester = remember { FocusRequester() }

    return this
        .then(
            if (requestFocusOnPress) {
                Modifier.focusRequester(focusRequester)
            } else {
                Modifier
            }
        )
        .pointerInput(enabled, onClick, requestFocusOnPress) {
            if (!enabled) return@pointerInput

            detectTapGestures(
                onPress = {
                    if (requestFocusOnPress) {
                        runCatching { focusRequester.requestFocus() }
                    }
                    tryAwaitRelease()
                },
                onTap = {
                    onClick()
                }
            )
        }
}
