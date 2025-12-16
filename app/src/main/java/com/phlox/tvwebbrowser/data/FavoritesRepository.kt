package com.phlox.tvwebbrowser.data

import com.phlox.tvwebbrowser.model.FavoriteItem

interface FavoritesRepository {
    suspend fun getAll(homePageBookmarks: Boolean = false): List<FavoriteItem>
    suspend fun getHomePageBookmarks(): List<FavoriteItem>
    suspend fun getById(id: Long): FavoriteItem?

    suspend fun insert(item: FavoriteItem): Long
    suspend fun update(item: FavoriteItem)
    suspend fun delete(item: FavoriteItem)
    suspend fun delete(id: Long)
    suspend fun markAsUseful(favoriteId: Long)
}