package org.mlm.browkorftv.activity.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
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

    fun loadData() = viewModelScope.launch(Dispatchers.IO) {
        _loading.value = true
        _bookmarks.value = bookmarksRepository.getAll()
        _loading.value = false
    }

    suspend fun getFavoriteById(id: Long): FavoriteItem? {
        return bookmarksRepository.getById(id)
    }

    fun saveFavorite(item: FavoriteItem) = viewModelScope.launch(Dispatchers.IO) {
        bookmarksRepository.upsert(item)
        _bookmarks.value = bookmarksRepository.getAll()
    }

    fun deleteFavorite(id: Long) = viewModelScope.launch(Dispatchers.IO) {
        bookmarksRepository.delete(id)
        _bookmarks.value = bookmarksRepository.getAll()
    }
    
    fun deleteFavorite(item: FavoriteItem) = viewModelScope.launch(Dispatchers.IO) {
        bookmarksRepository.delete(item.id)
        _bookmarks.value = bookmarksRepository.getAll()
    }
}
