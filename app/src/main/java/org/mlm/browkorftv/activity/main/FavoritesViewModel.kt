package org.mlm.browkorftv.activity.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.mlm.browkorftv.data.BookmarksRepository
import org.mlm.browkorftv.model.FavoriteItem

class FavoritesViewModel(
    private val bookmarksRepository: BookmarksRepository
) : ViewModel() {

    private val _bookmarks = MutableStateFlow<List<FavoriteItem>>(emptyList())
    val bookmarks: StateFlow<List<FavoriteItem>> = _bookmarks.asStateFlow()

    private val _loading = MutableStateFlow(true)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private var loadJob: Job? = null

    fun loadData() {
        if (loadJob != null) return
        loadJob = viewModelScope.launch(Dispatchers.IO) {
            bookmarksRepository.getAll()
            bookmarksRepository.observeAll().collect {
                _bookmarks.value = it
                _loading.value = false
            }
        }
    }

    suspend fun getFavoriteById(id: Long): FavoriteItem? {
        return bookmarksRepository.getById(id)
    }

    fun saveFavorite(item: FavoriteItem) = viewModelScope.launch(Dispatchers.IO) {
        bookmarksRepository.upsert(item)
    }

    fun deleteFavorite(id: Long) = viewModelScope.launch(Dispatchers.IO) {
        bookmarksRepository.delete(id)
    }

    fun moveFavorite(id: Long, delta: Int) = viewModelScope.launch(Dispatchers.IO) {
        bookmarksRepository.move(id, delta)
    }
}
