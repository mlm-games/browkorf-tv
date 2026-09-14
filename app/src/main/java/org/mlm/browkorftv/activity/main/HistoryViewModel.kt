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
        try {
            val items = if (searchQuery.isEmpty()) {
                historyDao.allByLimitOffset(offset)
            } else {
                val pattern = "%" + searchQuery
                    .replace("\\", "\\\\")
                    .replace("%", "\\%")
                    .replace("_", "\\_") + "%"
                historyDao.search(pattern, pattern)
            }

            _lastLoadedItems.value = items
        } finally {
            loading = false
        }
    }

    fun deleteItems(items: List<HistoryItem>) = viewModelScope.launch(Dispatchers.IO) {
        historyDao.delete(*items.toTypedArray())
        val ids = items.map { it.id }.toSet()
        _lastLoadedItems.value = _lastLoadedItems.value.filterNot { it.id in ids }
    }

    fun deleteAll() = viewModelScope.launch(Dispatchers.IO) {
        historyDao.deleteWhereTimeLessThan(Long.MAX_VALUE)
        _lastLoadedItems.value = emptyList()
    }
}