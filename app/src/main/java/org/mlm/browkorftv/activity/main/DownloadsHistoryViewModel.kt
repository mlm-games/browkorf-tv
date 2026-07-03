package org.mlm.browkorftv.activity.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import org.mlm.browkorftv.model.Download
import org.mlm.browkorftv.model.dao.DownloadDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DownloadsHistoryViewModel(
    private val downloadDao: DownloadDao
) : ViewModel() {

    private val _items = MutableStateFlow<List<Download>>(emptyList())
    val items: StateFlow<List<Download>> = _items.asStateFlow()

    private var loading = false

    fun deleteAll() = viewModelScope.launch(Dispatchers.IO) {
        downloadDao.deleteAll()
        _items.value = emptyList()
    }

    fun loadNextItems() = viewModelScope.launch(Dispatchers.IO) {
        if (loading) return@launch
        loading = true

        val offset = _items.value.size.toLong()
        val newItems = downloadDao.allByLimitOffset(offset)

        if (newItems.isNotEmpty()) {
            _items.value += newItems
        }

        loading = false
    }
}