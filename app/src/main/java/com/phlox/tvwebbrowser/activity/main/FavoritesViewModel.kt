package com.phlox.tvwebbrowser.activity.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.phlox.tvwebbrowser.model.FavoriteItem
import com.phlox.tvwebbrowser.model.dao.FavoritesDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class FavoritesViewModel(
    private val favoritesDao: FavoritesDao
) : ViewModel() {

    private val _bookmarks = MutableStateFlow<List<FavoriteItem>>(emptyList())
    val bookmarks: StateFlow<List<FavoriteItem>> = _bookmarks.asStateFlow()

    private val _homeSlots = MutableStateFlow<List<FavoriteItem>>(emptyList())
    val homeSlots: StateFlow<List<FavoriteItem>> = _homeSlots.asStateFlow()

    private val _loading = MutableStateFlow(true)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    fun loadData() = viewModelScope.launch(Dispatchers.IO) {
        _loading.value = true
        _homeSlots.value = favoritesDao.getHomePageBookmarks()
        _bookmarks.value = favoritesDao.getAll(homePageBookmarks = false)
        _loading.value = false
    }

    suspend fun getFavoriteById(id: Long): FavoriteItem? {
        // This is blocking/suspend, used for initializing editors
        return favoritesDao.getById(id)
    }
    
    suspend fun getHomeSlotByOrder(order: Int): FavoriteItem? {
        return favoritesDao.getHomePageBookmarks().firstOrNull { it.order == order }
    }

    fun saveFavorite(item: FavoriteItem) = viewModelScope.launch(Dispatchers.IO) {
        if (item.id == 0L) {
            favoritesDao.insert(item)
        } else {
            favoritesDao.update(item)
        }
        loadData() // Refresh list
    }

    fun deleteFavorite(id: Long) = viewModelScope.launch(Dispatchers.IO) {
        favoritesDao.delete(id)
        loadData()
    }
    
    fun deleteFavorite(item: FavoriteItem) = viewModelScope.launch(Dispatchers.IO) {
        favoritesDao.delete(item)
        loadData()
    }
}