package com.phlox.tvwebbrowser.compose.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.phlox.tvwebbrowser.Config
import com.phlox.tvwebbrowser.core.DispatcherProvider
import com.phlox.tvwebbrowser.data.tabs.TabsRepository
import com.phlox.tvwebbrowser.model.WebTabState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.max

class TabsViewModel(
    private val repo: TabsRepository,
    private val config: Config,
    private val dispatchers: DispatcherProvider,
) : ViewModel() {

    private val _tabs = MutableStateFlow<List<WebTabState>>(emptyList())
    val tabs: StateFlow<List<WebTabState>> = _tabs.asStateFlow()

    private val _currentTab = MutableStateFlow<WebTabState?>(null)
    val currentTab: StateFlow<WebTabState?> = _currentTab.asStateFlow()

    private fun incognito(): Boolean = config.incognitoMode

    fun load() {
        viewModelScope.launch(dispatchers.main) {
            val list = withContext(dispatchers.io) { repo.loadTabs(incognito()) }.toMutableList()

            if (list.isEmpty()) {
                val tab = WebTabState(
                    url = Config.HOME_URL_ALIAS,
                    title = "",
                    selected = true,
                    incognito = incognito(),
                    position = 0
                )
                tab.id = withContext(dispatchers.io) { repo.insert(tab) }
                list.add(tab)
            }

            // Ensure exactly one selected tab
            val selected = list.firstOrNull { it.selected } ?: list.first()
            list.forEach { it.selected = (it.id == selected.id) }

            // Ensure positions are contiguous
            list.forEachIndexed { i, t -> t.position = i }

            _tabs.value = list
            _currentTab.value = selected

            // Persist selection + positions
            viewModelScope.launch(dispatchers.io) {
                repo.unselectAll(incognito())
                repo.update(selected)
                repo.updatePositions(list)
            }
        }
    }

    fun select(tab: WebTabState) {
        val list = _tabs.value.toMutableList()
        val idx = list.indexOfFirst { it.id == tab.id }
        if (idx == -1) return

        val selected = list[idx]
        list.forEach { it.selected = (it.id == selected.id) }

        _tabs.value = list
        _currentTab.value = selected

        viewModelScope.launch(dispatchers.io) {
            repo.unselectAll(incognito())
            repo.update(selected)
        }
    }

    fun newTab(url: String, select: Boolean = true, insertAt: Int = _tabs.value.size): WebTabState {
        val list = _tabs.value.toMutableList()
        val tab = WebTabState(
            url = url,
            title = url,
            selected = false,
            incognito = incognito(),
            position = insertAt.coerceIn(0, list.size)
        )

        list.add(tab.position, tab)
        list.forEachIndexed { i, t -> t.position = i }
        if (select) list.forEach { it.selected = (it === tab) }

        _tabs.value = list
        if (select) _currentTab.value = tab

        viewModelScope.launch(dispatchers.io) {
            tab.id = repo.insert(tab)
            if (select) {
                repo.unselectAll(incognito())
                tab.selected = true
                repo.update(tab)
            } else {
                repo.update(tab)
            }
            repo.updatePositions(list)
        }

        return tab
    }

    fun close(tab: WebTabState) {
        val list = _tabs.value.toMutableList()
        val idx = list.indexOfFirst { it.id == tab.id }
        if (idx == -1) return

        val removed = list.removeAt(idx)
        val wasSelected = removed.selected

        if (list.isEmpty()) {
            _tabs.value = emptyList()
            _currentTab.value = null

            viewModelScope.launch(dispatchers.io) {
                repo.delete(removed)
                removed.removeFiles()
            }

            // recreate a default tab
            load()
            return
        }

        list.forEachIndexed { i, t -> t.position = i }

        val newSelected = if (wasSelected) {
            list[max(0, idx - 1)]
        } else {
            _currentTab.value?.let { cur -> list.find { it.id == cur.id } } ?: list.first()
        }

        list.forEach { it.selected = (it.id == newSelected.id) }

        _tabs.value = list
        _currentTab.value = newSelected

        viewModelScope.launch(dispatchers.io) {
            repo.delete(removed)
            removed.removeFiles()
            repo.unselectAll(incognito())
            repo.update(newSelected)
            repo.updatePositions(list)
        }
    }

    fun persistTab(tab: WebTabState) {
        viewModelScope.launch(dispatchers.io) {
            tab.saveWebViewStateToFile()
            if (tab.id == 0L) tab.id = repo.insert(tab) else repo.update(tab)
        }
    }

    fun updateTitle(tab: WebTabState, title: String) {
        tab.title = title
        // re-emit list to trigger UI updates
        _tabs.value = _tabs.value.toList()
        if (_currentTab.value?.id == tab.id) _currentTab.value = tab
        persistTab(tab)
    }

    fun updateUrl(tab: WebTabState, url: String) {
        tab.url = url
        _tabs.value = _tabs.value.toList()
        if (_currentTab.value?.id == tab.id) _currentTab.value = tab
        persistTab(tab)
    }

    fun move(tab: WebTabState, delta: Int) {
        val list = _tabs.value.toMutableList()
        val from = list.indexOfFirst { it.id == tab.id }
        if (from == -1) return
        val to = (from + delta).coerceIn(0, list.lastIndex)
        if (from == to) return

        val item = list.removeAt(from)
        list.add(to, item)
        list.forEachIndexed { i, t -> t.position = i }

        _tabs.value = list

        viewModelScope.launch(dispatchers.io) {
            repo.updatePositions(list)
        }
    }
}