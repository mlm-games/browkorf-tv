package com.phlox.tvwebbrowser.activity.main

import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.phlox.tvwebbrowser.TVBro
import com.phlox.tvwebbrowser.model.HostConfig
import com.phlox.tvwebbrowser.model.WebTabState
import com.phlox.tvwebbrowser.model.dao.HostsDao
import com.phlox.tvwebbrowser.model.dao.TabsDao
import com.phlox.tvwebbrowser.settings.SettingsManager
import com.phlox.tvwebbrowser.utils.Utils
import com.phlox.tvwebbrowser.webengine.WebEngineWindowProviderCallback
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URL

class TabsViewModel(
    private val tabsDao: TabsDao,
    private val hostsDao: HostsDao,
    private val settingsManager: SettingsManager
) : ViewModel() {

    private val _tabsStates = MutableStateFlow<List<WebTabState>>(emptyList())
    val tabsStates: StateFlow<List<WebTabState>> = _tabsStates.asStateFlow()

    private val _currentTab = MutableStateFlow<WebTabState?>(null)
    val currentTab: StateFlow<WebTabState?> = _currentTab.asStateFlow()

    private var loaded = false
    private var incognitoMode = settingsManager.current.incognitoMode

    fun loadState() = viewModelScope.launch(Dispatchers.Main) {
        val currentSettingsIncognito = settingsManager.current.incognitoMode
        if (loaded) {
            if (incognitoMode != currentSettingsIncognito) {
                incognitoMode = currentSettingsIncognito
                loaded = false
            } else {
                return@launch
            }
        }
        val tabs = tabsDao.getAll(incognitoMode)
        _tabsStates.value = ArrayList(tabs)
        loaded = true
    }

    suspend fun saveTab(tab: WebTabState) {
        val isIncognito = settingsManager.current.incognitoMode
        if (tab.selected) {
            tabsDao.unselectAll(isIncognito)
        }
        withContext(Dispatchers.IO) {
            tab.saveWebViewStateToFile()
        }
        if (tab.id != 0L) {
            tabsDao.update(tab)
        } else {
            tab.id = tabsDao.insert(tab)
        }
        loadState()
    }

    fun onCloseTab(tab: WebTabState) {
        tab.webEngine.onDetachFromWindow(completely = true, destroyTab = true)

        val newList = _tabsStates.value.toMutableList()
        newList.remove(tab)
        _tabsStates.value = newList

        viewModelScope.launch(Dispatchers.Main) {
            tabsDao.delete(tab)
            launch(Dispatchers.IO) { tab.removeFiles() }
        }
    }

    fun onCloseAllTabs() = viewModelScope.launch(Dispatchers.Main) {
        val tabsClone = ArrayList(_tabsStates.value)
        _tabsStates.value = emptyList()

        val isIncognito = settingsManager.current.incognitoMode
        tabsDao.deleteAll(isIncognito)

        withContext(Dispatchers.IO) {
            tabsClone.forEach { it.removeFiles() }
        }
    }

    fun onDetachActivity() {
        for (tab in _tabsStates.value) {
            tab.webEngine.onDetachFromWindow(completely = true, destroyTab = false)
        }
    }

    fun changeTab(
        newTab: WebTabState,
        webViewProvider: (tab: WebTabState) -> View?,
        webViewParent: ViewGroup,
        fullScreenViewParent: ViewGroup,
        webEngineWindowProviderCallback: WebEngineWindowProviderCallback
    ) {
        if (_currentTab.value == newTab && newTab.webEngine.getView() != null) return

        val currentList = _tabsStates.value
        currentList.forEach { it.selected = false }

        _currentTab.value?.apply {
            webEngine.onDetachFromWindow(completely = false, destroyTab = false)
            onPause()
            viewModelScope.launch { saveTab(this@apply) }
        }

        newTab.selected = true
        _currentTab.value = newTab

        var wv = newTab.webEngine.getView()
        var needReloadUrl = false
        if (wv == null) {
            wv = webViewProvider(newTab)
            if (wv == null) return
            needReloadUrl = !newTab.restoreWebView()
        }

        newTab.webEngine.onAttachToWindow(
            webEngineWindowProviderCallback,
            webViewParent,
            fullScreenViewParent
        )

        if (needReloadUrl) {
            newTab.webEngine.loadUrl(newTab.url)
        }
        newTab.webEngine.setNetworkAvailable(Utils.isNetworkConnected(TVBro.instance))
    }

    suspend fun findHostConfig(tab: WebTabState, createIfNotFound: Boolean): HostConfig? {
        val currentHostName = try {
            URL(tab.url).host
        } catch (e: Exception) {
            return null
        }
        var hostConfig = tab.cachedHostConfig
        if (hostConfig == null || hostConfig.hostName != currentHostName) {
            hostConfig = hostsDao.findByHostName(currentHostName)

            if (hostConfig == null && createIfNotFound) {
                hostConfig = HostConfig(currentHostName)
                hostConfig.id = hostsDao.insert(hostConfig)
            }
            tab.cachedHostConfig = hostConfig
        }
        return hostConfig
    }

    suspend fun changePopupBlockingLevel(newLevel: Int, tab: WebTabState) {
        val hostConfig = findHostConfig(tab, true) ?: return
        hostConfig.popupBlockLevel = newLevel
        hostsDao.update(hostConfig)
    }

    fun addNewTab(tab: WebTabState, index: Int) {
        val list = _tabsStates.value.toMutableList()
        if (index >= 0 && index <= list.size) {
            list.add(index, tab)
        } else {
            list.add(tab)
        }
        _tabsStates.value = list
    }
}