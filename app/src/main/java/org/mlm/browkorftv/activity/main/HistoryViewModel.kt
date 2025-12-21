package org.mlm.browkorftv.activity.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.mlm.browkorftv.model.HistoryItem
import org.mlm.browkorftv.model.dao.HistoryDao

class HistoryViewModel(
    private val historyDao: HistoryDao
) : ViewModel() {

    private val _lastLoadedItems = MutableStateFlow<List<HistoryItem>>(emptyList())
    val lastLoadedItems: StateFlow<List<HistoryItem>> = _lastLoadedItems.asStateFlow()

    private var loading = false
    var searchQuery = ""

    fun loadItems(offset: Long = 0) = viewModelScope.launch(Dispatchers.IO) {
        if (loading) return@launch
        loading = true

        val items = if (searchQuery.isEmpty()) {
            historyDao.allByLimitOffset(offset)
        } else {
            historyDao.search(searchQuery, searchQuery)
        }

        _lastLoadedItems.value = items
        loading = false
    }

    fun deleteItems(items: List<HistoryItem>) = viewModelScope.launch(Dispatchers.IO) {
        historyDao.delete(*items.toTypedArray())
    }

    fun deleteAll() = viewModelScope.launch(Dispatchers.IO) {
        historyDao.deleteWhereTimeLessThan(Long.MAX_VALUE)
    }
}