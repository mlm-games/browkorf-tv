package com.phlox.tvwebbrowser.activity.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.phlox.tvwebbrowser.model.FavoriteItem
import com.phlox.tvwebbrowser.model.dao.FavoritesDao
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomePageSlotEditorState(
    val title: String = "",
    val url: String = "",
    val canDelete: Boolean = false,
    val saved: Boolean = false
)

class HomePageSlotEditorViewModel(
    private val slotOrder: Int,
    private val favoritesDao: FavoritesDao
) : ViewModel() {

    private val _state = MutableStateFlow(HomePageSlotEditorState())
    val state = _state.asStateFlow()

    private var existingItem: FavoriteItem? = null

    init {
        loadSlot()
    }

    private fun loadSlot() = viewModelScope.launch {
        // Logic: Find the item with home_page_bookmark=1 and specific order
        val allHome = favoritesDao.getHomePageBookmarks()
        existingItem = allHome.find { it.order == slotOrder }
        
        existingItem?.let { item ->
            _state.update { 
                it.copy(title = item.title ?: "", url = item.url ?: "", canDelete = true) 
            }
        }
    }

    fun save(title: String, url: String) = viewModelScope.launch {
        val item = existingItem ?: FavoriteItem().apply { 
            this.order = slotOrder
            this.homePageBookmark = true
            this.parent = 0
        }
        item.title = title
        item.url = url
        
        if (item.id == 0L) {
            favoritesDao.insert(item)
        } else {
            favoritesDao.update(item)
        }
        _state.update { it.copy(saved = true) }
    }

    fun delete() = viewModelScope.launch {
        existingItem?.let { favoritesDao.delete(it) }
        _state.update { it.copy(saved = true) }
    }
}