package com.phlox.tvwebbrowser.compose.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.phlox.tvwebbrowser.core.DispatcherProvider
import com.phlox.tvwebbrowser.data.DownloadsRepository
import com.phlox.tvwebbrowser.model.Download
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

sealed interface DownloadsUiItem {
    data class Header(val key: String, val title: String) : DownloadsUiItem
    data class Row(val download: Download) : DownloadsUiItem
}

data class DownloadsUiState(
    val items: List<DownloadsUiItem> = emptyList(),
    val loadedCount: Long = 0,
    val loading: Boolean = false
)

class DownloadsViewModel(
    private val repo: DownloadsRepository,
    private val dispatchers: DispatcherProvider
) : ViewModel() {

    private val _state = MutableStateFlow(DownloadsUiState())
    val state: StateFlow<DownloadsUiState> = _state.asStateFlow()

    fun loadFirstPage() {
        _state.value = DownloadsUiState()
        loadMore()
    }

    fun loadMore() {
        if (_state.value.loading) return
        viewModelScope.launch(dispatchers.main) {
            _state.value = _state.value.copy(loading = true)
            val offset = _state.value.loadedCount
            val page = withContext(dispatchers.io) { repo.page(offset) }

            val merged = buildSectionedList(
                existing = _state.value.items,
                newRows = page
            )

            _state.value = _state.value.copy(
                items = merged,
                loadedCount = offset + page.count { !it.isDateHeader }, // isDateHeader is part of Download model logic, but here we count rows
                loading = false
            )
        }
    }

    fun delete(download: Download) {
        viewModelScope.launch(dispatchers.main) {
            withContext(dispatchers.io) { repo.delete(download) }
            loadFirstPage()
        }
    }

    private fun buildSectionedList(
        existing: List<DownloadsUiItem>,
        newRows: List<Download>
    ): List<DownloadsUiItem> {
        if (newRows.isEmpty()) return existing

        val result = existing.toMutableList()
        var lastHeaderKey = (existing.lastOrNull { it is DownloadsUiItem.Header } as? DownloadsUiItem.Header)?.key

        for (d in newRows) {
            val key = dayKey(d.time)
            if (key != lastHeaderKey) {
                lastHeaderKey = key
                result.add(DownloadsUiItem.Header(key, dayTitle(d.time)))
            }
            result.add(DownloadsUiItem.Row(d))
        }
        return result
    }

    private fun dayKey(time: Long): String {
        val c = Calendar.getInstance().apply { timeInMillis = time }
        return "${c.get(Calendar.YEAR)}-${c.get(Calendar.DAY_OF_YEAR)}"
    }

    private fun dayTitle(time: Long): String {
        val c = Calendar.getInstance().apply { timeInMillis = time }
        return java.text.SimpleDateFormat.getDateInstance().format(c.time)
    }
}