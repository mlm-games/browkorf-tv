package org.mlm.browkorftv.activity.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.mlm.browkorftv.settings.AppSettings
import org.mlm.browkorftv.settings.HomePageLinksMode
import org.mlm.browkorftv.settings.HomePageMode
import org.mlm.browkorftv.settings.SettingsManager

class SettingsViewModel(
    private val settingsManager: SettingsManager
) : ViewModel() {

    val settingsState: StateFlow<AppSettings> = settingsManager.settingsState

    val currentSettings: AppSettings get() = settingsManager.current

    fun setSearchEngineURL(url: String) {
        viewModelScope.launch {
            val index = AppSettings.Companion.SearchEnginesURLs.indexOf(url)
            if (index >= 0 && index < AppSettings.Companion.SearchEnginesURLs.size - 1) {
                settingsManager.setSearchEngine(index)
            } else {
                settingsManager.setSearchEngine(
                    index = AppSettings.Companion.SearchEnginesURLs.size - 1,
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