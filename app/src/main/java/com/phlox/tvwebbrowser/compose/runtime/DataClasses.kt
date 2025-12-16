package com.phlox.tvwebbrowser.compose.runtime
import com.phlox.tvwebbrowser.model.HomePageLink
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow

sealed interface BrowserUiEvent {
    data object OpenDownloads : BrowserUiEvent
    data object OpenHistory : BrowserUiEvent
    data object OpenSettings : BrowserUiEvent
    data class ShowLinkActions(val href: String, val x: Int, val y: Int) : BrowserUiEvent
    data class EditHomePageBookmark(val index: Int) : BrowserUiEvent
    data class HomePageLinksUpdated(val links: List<HomePageLink>) : BrowserUiEvent
    data class Toast(val message: String) : BrowserUiEvent
}

data class BrowserChromeState(
    val url: String = "",
    val title: String = "",
    val progress: Int = 0,
    val isFullscreen: Boolean = false,
    val blockedAds: Int = 0,
    val blockedPopups: Int = 0,
)

data class VoiceUiState(
    val active: Boolean = false,
    val partialText: String = "",
    val rmsDb: Float = 0f,
    val error: String? = null
)

sealed interface BrowserCommand {
    data class Navigate(val url: String, val inNewTab: Boolean = false) : BrowserCommand
    data class Toast(val message: String) : BrowserCommand

    data class AdblockChanged(val enabled: Boolean) : BrowserCommand
    data object ForceAdblockUpdate : BrowserCommand

    data object Back : BrowserCommand
    data object Forward : BrowserCommand
    data object Reload : BrowserCommand
    data object Home : BrowserCommand
    data object StartVoiceSearch : BrowserCommand
    data object ToggleQuickMenu : BrowserCommand

    data object OpenFavorites : BrowserCommand
    data object OpenDownloads : BrowserCommand
    data object OpenHistory : BrowserCommand
    data object OpenSettings : BrowserCommand
    data object OpenShortcuts : BrowserCommand
    data object OpenAbout : BrowserCommand
}

class BrowserCommandBus {
    private val channel = Channel<BrowserCommand>(Channel.BUFFERED)

    val events = channel.receiveAsFlow()
    fun trySend(cmd: BrowserCommand) { channel.trySend(cmd) }
}