package org.mlm.browkorftv.activity.main

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import org.mlm.browkorftv.BrowkorfTV
import org.mlm.browkorftv.model.HostConfig
import org.mlm.browkorftv.model.WebTabState
import org.mlm.browkorftv.model.dao.HostsDao
import org.mlm.browkorftv.model.dao.TabsDao
import org.mlm.browkorftv.settings.SettingsManager
import org.mlm.browkorftv.utils.Utils
import org.mlm.browkorftv.webengine.WebEngineWindowProviderCallback
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
    private val settingsManager: SettingsManager,
    private val appContext: Context
) : ViewModel() {

    private val _tabsStates = MutableStateFlow<List<WebTabState>>(emptyList())
    val tabsStates: StateFlow<List<WebTabState>> = _tabsStates.asStateFlow()

    private val _currentTab = MutableStateFlow<WebTabState?>(null)
    val currentTab: StateFlow<WebTabState?> = _currentTab.asStateFlow()

    private var loaded = false
    private var incognitoMode = settingsManager.current.incognitoMode

    fun loadState() = viewModelScope.launch {
        val currentSettingsIncognito = settingsManager.current.incognitoMode
        if (loaded) {
            if (incognitoMode != currentSettingsIncognito) {
                incognitoMode = currentSettingsIncognito
                loaded = false
            } else return@launch
        }

        val tabs = withContext(Dispatchers.IO) { tabsDao.getAll(incognitoMode) }
        _tabsStates.value = ArrayList(tabs)
        loaded = true
    }

    suspend fun saveTab(tab: WebTabState) {
        val isIncognito = settingsManager.current.incognitoMode
        if (tab.selected) {
            withContext(Dispatchers.IO) { tabsDao.unselectAll(isIncognito) }
        }
        withContext(Dispatchers.IO) {
            tab.saveWebViewStateToFile(appContext)
            if (tab.id != 0L) tabsDao.update(tab) else tab.id = tabsDao.insert(tab)
        }
        loadState()
    }

    fun onCloseTab(tab: WebTabState) {
        tab.webEngine.onDetachFromWindow(completely = true, destroyTab = true)

        val newList = _tabsStates.value.toMutableList()
        newList.remove(tab)
        _tabsStates.value = newList

        viewModelScope.launch {
            withContext(Dispatchers.IO) { tabsDao.delete(tab) }
            withContext(Dispatchers.IO) { tab.removeFiles(appContext) }
        }
    }

    fun onCloseAllTabs() = viewModelScope.launch {
        val tabsClone = ArrayList(_tabsStates.value)
        _tabsStates.value = emptyList()

        val isIncognito = settingsManager.current.incognitoMode
        withContext(Dispatchers.IO) { tabsDao.deleteAll(isIncognito) }
        withContext(Dispatchers.IO) { tabsClone.forEach { it.removeFiles(appContext) } }
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
            needReloadUrl = !newTab.restoreWebView(appContext)
        }

        newTab.webEngine.onAttachToWindow(
            webEngineWindowProviderCallback,
            webViewParent,
            fullScreenViewParent
        )

        if (needReloadUrl) {
            newTab.webEngine.loadUrl(newTab.url)
        }
        newTab.webEngine.setNetworkAvailable(Utils.isNetworkConnected(BrowkorfTV.instance))
    }

    suspend fun findHostConfig(tab: WebTabState, createIfNotFound: Boolean): HostConfig? {
        val currentHostName = try {
            URL(tab.url).host
        } catch (_: Exception) {
            return null
        }

        var hostConfig = tab.cachedHostConfig
        if (hostConfig == null || hostConfig.hostName != currentHostName) {
            hostConfig = withContext(Dispatchers.IO) {
                hostsDao.findByHostName(currentHostName)
            }

            if (hostConfig == null && createIfNotFound) {
                hostConfig = HostConfig(currentHostName)
                hostConfig.id = withContext(Dispatchers.IO) { hostsDao.insert(hostConfig) }
            }
            tab.cachedHostConfig = hostConfig
        }
        return hostConfig
    }

    suspend fun changePopupBlockingLevel(newLevel: Int, tab: WebTabState) {
        val hostConfig = findHostConfig(tab, true) ?: return
        hostConfig.popupBlockLevel = newLevel
        withContext(Dispatchers.IO) { hostsDao.update(hostConfig) }
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