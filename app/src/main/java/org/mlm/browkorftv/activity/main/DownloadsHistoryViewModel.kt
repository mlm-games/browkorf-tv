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

    // Cache all loaded items
    private val _allItems = ArrayList<Download>()
    val allItems: List<Download> get() = _allItems

    // Emit only the newly loaded chunk for the adapter to append
    private val _newlyLoadedItems = MutableStateFlow<List<Download>>(emptyList())
    val newlyLoadedItems: StateFlow<List<Download>> = _newlyLoadedItems.asStateFlow()

    private var loading = false

    fun loadNextItems() = viewModelScope.launch(Dispatchers.IO) {
        if (loading) {
            return@launch
        }
        loading = true

        val offset = _allItems.size.toLong()
        val newItems = downloadDao.allByLimitOffset(offset)

        if (newItems.isNotEmpty()) {
            _allItems.addAll(newItems)
            _newlyLoadedItems.value = newItems
        }

        loading = false
    }
}