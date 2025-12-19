package com.phlox.tvwebbrowser.compose.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.phlox.tvwebbrowser.settings.AppSettings
import com.phlox.tvwebbrowser.settings.HomePageLinksMode
import com.phlox.tvwebbrowser.settings.HomePageMode
import com.phlox.tvwebbrowser.settings.SettingsManager
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val settingsManager: SettingsManager
) : ViewModel() {

    // Expose settings as a StateFlow
    val settingsState: StateFlow<AppSettings> = settingsManager.settingsState

    val currentSettings: AppSettings get() = settingsManager.current

    val userAgentStringTitles = arrayOf(
        "Default (recommended)", "Chrome (Desktop)", "Chrome (Mobile)",
        "Firefox (Desktop)", "Firefox (Mobile)", "Edge (Desktop)", "Custom"
    )

    fun setSearchEngineURL(url: String) {
        viewModelScope.launch {
            val index = AppSettings.SearchEnginesURLs.indexOf(url)
            if (index >= 0 && index < AppSettings.SearchEnginesURLs.size - 1) {
                settingsManager.setSearchEngine(index)
            } else {
                settingsManager.setSearchEngine(
                    index = AppSettings.SearchEnginesURLs.size - 1,
                    customUrl = url
                )
            }
        }
    }

    fun setHomePageProperties(
        homePageMode: HomePageMode,
        customHomePageUrl: String?,
        homePageLinksMode: HomePageLinksMode
    ) {
        viewModelScope.launch {
            settingsManager.setHomePageProperties(
                mode = homePageMode,
                customUrl = customHomePageUrl,
                linksMode = homePageLinksMode
            )
        }
    }
}