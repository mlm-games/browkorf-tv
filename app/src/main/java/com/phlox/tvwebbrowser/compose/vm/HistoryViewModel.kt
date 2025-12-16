package com.phlox.tvwebbrowser.compose.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.phlox.tvwebbrowser.core.DispatcherProvider
import com.phlox.tvwebbrowser.data.history.HistoryRepository
import com.phlox.tvwebbrowser.model.HistoryItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

sealed interface HistoryUiItem {
    data class Header(val key: String, val title: String) : HistoryUiItem
    data class Row(val item: HistoryItem) : HistoryUiItem
}

data class HistoryUiState(
    val items: List<HistoryUiItem> = emptyList(),
    val loadedCount: Long = 0,
    val loading: Boolean = false,
    val query: String = ""
)

class HistoryViewModel(
    private val repo: HistoryRepository,
    private val dispatchers: DispatcherProvider
) : ViewModel() {

    private val _state = MutableStateFlow(HistoryUiState())
    val state: StateFlow<HistoryUiState> = _state.asStateFlow()

    fun loadFirstPage() {
        _state.value = HistoryUiState(query = _state.value.query)
        loadMore()
    }

    fun setQuery(q: String) {
        _state.value = _state.value.copy(query = q)
        loadFirstPage()
    }

    fun loadMore() {
        if (_state.value.loading) return
        viewModelScope.launch(dispatchers.main) {
            _state.value = _state.value.copy(loading = true)

            val q = _state.value.query.trim()
            val newRows = withContext(dispatchers.io) {
                if (q.isBlank()) repo.page(_state.value.loadedCount)
                else repo.search(q)
            }

            val merged = buildSectionedList(
                existing = if (q.isBlank()) _state.value.items else emptyList(),
                newRows = newRows
            )

            _state.value = _state.value.copy(
                items = merged,
                loadedCount = if (q.isBlank()) _state.value.loadedCount + newRows.size else newRows.size.toLong(),
                loading = false
            )
        }
    }

    fun delete(item: HistoryItem) {
        viewModelScope.launch(dispatchers.main) {
            withContext(dispatchers.io) { repo.delete(item) }
            loadFirstPage()
        }
    }

    fun clearAll() {
        viewModelScope.launch(dispatchers.main) {
            withContext(dispatchers.io) { repo.clearAll() }
            loadFirstPage()
        }
    }

    private fun buildSectionedList(existing: List<HistoryUiItem>, newRows: List<HistoryItem>): List<HistoryUiItem> {
        if (newRows.isEmpty()) return existing
        val out = existing.toMutableList()
        var lastHeaderKey = (existing.lastOrNull { it is HistoryUiItem.Header } as? HistoryUiItem.Header)?.key

        for (h in newRows) {
            val key = dayKey(h.time)
            if (key != lastHeaderKey) {
                lastHeaderKey = key
                out.add(HistoryUiItem.Header(key, dayTitle(h.time)))
            }
            out.add(HistoryUiItem.Row(h))
        }
        return out
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