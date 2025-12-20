package com.phlox.tvwebbrowser.activity.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.phlox.tvwebbrowser.model.FavoriteItem
import com.phlox.tvwebbrowser.model.dao.FavoritesDao
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class BookmarkEditorState(
    val title: String = "",
    val url: String = "",
    val saved: Boolean = false
)

class BookmarkEditorViewModel(
    private val bookmarkId: Long,
    private val favoritesDao: FavoritesDao
) : ViewModel() {

    private val _state = MutableStateFlow(BookmarkEditorState())
    val state = _state.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving = _isSaving.asStateFlow()

    init {
        if (bookmarkId >= 0) {
            loadBookmark()
        }
    }

    private fun loadBookmark() = viewModelScope.launch {
        favoritesDao.getById(bookmarkId)?.let { item ->
            _state.update { it.copy(title = item.title ?: "", url = item.url ?: "") }
        }
    }

    fun save(title: String, url: String) = viewModelScope.launch {
        _isSaving.value = true
        try {
            if (bookmarkId >= 0L) {
                val existing = favoritesDao.getById(bookmarkId)
                existing?.let {
                    it.title = title
                    it.url = url
                    favoritesDao.update(it)
                }
            } else {
                val item = FavoriteItem().apply {
                    this.title = title
                    this.url = url
                }
                favoritesDao.insert(item)
            }
            _state.update { it.copy(saved = true) }
        } finally {
            _isSaving.value = false
        }
    }
}