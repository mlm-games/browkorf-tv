package com.phlox.tvwebbrowser.compose.runtime

import com.phlox.tvwebbrowser.singleton.shortcuts.Shortcut
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ShortcutCaptureController {
    private val _capturing = MutableStateFlow<Shortcut?>(null)
    val capturing: StateFlow<Shortcut?> = _capturing.asStateFlow()

    fun start(shortcut: Shortcut) { _capturing.value = shortcut }
    fun stop() { _capturing.value = null }
}