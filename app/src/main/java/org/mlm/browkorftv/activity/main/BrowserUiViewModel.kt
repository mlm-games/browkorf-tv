package org.mlm.browkorftv.activity.main

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class BrowserUiState(
    val url: String = "",
    val progress: Int = 0,
    val isProgressVisible: Boolean = false,
    val canGoBack: Boolean = false,
    val canGoForward: Boolean = false,

    val isIncognito: Boolean = false,

    val isAdBlockEnabled: Boolean = true,
    val blockedAds: Int = 0,
    val blockedPopups: Int = 0,

    val isFullscreen: Boolean = false,

    val currentThumbnail: Bitmap? = null,
    val notification: NotificationUi? = null,

    val isCursorMenuVisible: Boolean = false,
    val cursorMenuX: Int = 0, // px
    val cursorMenuY: Int = 0, // px

    val isLinkActionsVisible: Boolean = false,
    val isMenuVisible: Boolean = false,
)

data class NotificationUi(
    val iconRes: Int,
    val message: String
)


class BrowserUiViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(BrowserUiState())
    val uiState: StateFlow<BrowserUiState> = _uiState.asStateFlow()

    private var hideProgressJob: Job? = null

    fun updateUrl(url: String) {
        _uiState.update { it.copy(url = url) }
    }

    fun setIncognitoMode(enabled: Boolean) {
        _uiState.update { it.copy(isIncognito = enabled) }
    }

    fun setAdBlockEnabled(enabled: Boolean) {
        // counts (ads/popups) are usually updated by the Tab
        _uiState.update { it.copy(isAdBlockEnabled = enabled) }
    }

    fun updateProgress(progress: Int) {
        hideProgressJob?.cancel()
        _uiState.update { it.copy(progress = progress, isProgressVisible = true) }

        if (progress >= 100) {
            hideProgressJob = viewModelScope.launch {
                delay(1000)
                _uiState.update { it.copy(isProgressVisible = false) }
            }
        }
    }

    fun updateNavState(canGoBack: Boolean, canGoForward: Boolean) {
        _uiState.update { it.copy(canGoBack = canGoBack, canGoForward = canGoForward) }
    }

    fun updateAdBlockStats(enabled: Boolean, ads: Int, popups: Int) {
        _uiState.update {
            it.copy(
                isAdBlockEnabled = enabled,
                blockedAds = ads,
                blockedPopups = popups
            )
        }
    }

    fun setFullscreen(fullscreen: Boolean) {
        _uiState.update { it.copy(isFullscreen = fullscreen) }
    }

    fun updateThumbnail(bitmap: Bitmap?) {
        _uiState.update { it.copy(currentThumbnail = bitmap) }
    }

    fun showNotification(iconRes: Int, message: String) {
        _uiState.update { it.copy(notification = NotificationUi(iconRes, message)) }

        viewModelScope.launch {
            delay(3000)
            _uiState.update { s ->
                if (s.notification?.message == message) s.copy(notification = null) else s
            }
        }
    }

    fun showCursorMenu(x: Int, y: Int) {
        _uiState.update { it.copy(isCursorMenuVisible = true, cursorMenuX = x, cursorMenuY = y) }
    }

    fun hideCursorMenu() {
        _uiState.update { it.copy(isCursorMenuVisible = false) }
    }

    fun showLinkActions() {
        _uiState.update { it.copy(isLinkActionsVisible = true, isCursorMenuVisible = false) }
    }

    fun hideLinkActions() {
        _uiState.update { it.copy(isLinkActionsVisible = false) }
    }

    fun showMenu() {
        _uiState.update { it.copy(isMenuVisible = true) }
    }

    fun hideMenu() {
        _uiState.update { it.copy(isMenuVisible = false) }
    }

    fun toggleMenu() {
        _uiState.update { it.copy(isMenuVisible = !it.isMenuVisible) }
    }
}