package org.mlm.browkorftv.ui

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

data class SnackbarEvent(
    val message: String,
    val actionLabel: String? = null,
    val withDismissAction: Boolean = false,
    val errorDetails: String? = null
)

class SnackbarManager {
    private val _events = MutableSharedFlow<SnackbarEvent>(
        extraBufferCapacity = 32,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val events: SharedFlow<SnackbarEvent> = _events

    fun show(message: String, actionLabel: String? = null, withDismissAction: Boolean = false) {
        _events.tryEmit(SnackbarEvent(message, actionLabel, withDismissAction))
    }

    fun postError(message: String, errorDetails: String?) {
        _events.tryEmit(
            SnackbarEvent(
                message = message,
                actionLabel = "Copy Error",
                withDismissAction = true,
                errorDetails = errorDetails
            )
        )
    }
}