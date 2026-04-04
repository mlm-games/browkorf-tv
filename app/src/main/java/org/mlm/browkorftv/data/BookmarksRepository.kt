package org.mlm.browkorftv.data

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.mlm.browkorftv.model.FavoriteItem
import org.mlm.browkorftv.model.dao.FavoritesDao
import org.mlm.browkorftv.settings.BookmarkEntry
import org.mlm.browkorftv.settings.SettingsManager

class BookmarksRepository(
    private val settingsManager: SettingsManager,
    private val favoritesDao: FavoritesDao,
) {
    private val migrationMutex = Mutex()

    suspend fun getAll(): List<FavoriteItem> {
        ensureMigrated()
        return settingsManager.getBookmarks().map { it.toFavoriteItem() }
    }

    suspend fun getById(id: Long): FavoriteItem? {
        ensureMigrated()
        return settingsManager.getBookmark(id)?.toFavoriteItem()
    }

    suspend fun upsert(item: FavoriteItem): FavoriteItem {
        ensureMigrated()
        return settingsManager
            .upsertBookmark(item.toBookmarkEntry())
            .toFavoriteItem()
    }

    suspend fun delete(id: Long) {
        ensureMigrated()
        settingsManager.deleteBookmark(id)
    }

    private suspend fun ensureMigrated() {
        if (settingsManager.current.bookmarksMigratedFromRoom) return

        migrationMutex.withLock {
            if (settingsManager.current.bookmarksMigratedFromRoom) return

            val currentBookmarks = settingsManager.current.bookmarks
            if (currentBookmarks.isEmpty()) {
                val legacyBookmarks = favoritesDao.getAll()
                    .map { it.toBookmarkEntry() }
                    .sortedByDescending { it.id }

                if (legacyBookmarks.isNotEmpty()) {
                    settingsManager.replaceBookmarks(legacyBookmarks)
                }
            }

            settingsManager.markBookmarksMigratedFromRoom()
        }
    }

    private fun FavoriteItem.toBookmarkEntry(): BookmarkEntry = BookmarkEntry(
        id = id,
        title = title.orEmpty(),
        url = url.orEmpty(),
        parent = parent ?: 0L,
        homePageBookmark = homePageBookmark,
        useful = useful,
    )

    private fun BookmarkEntry.toFavoriteItem(): FavoriteItem = FavoriteItem().apply {
        id = this@toFavoriteItem.id
        title = this@toFavoriteItem.title
        url = this@toFavoriteItem.url
        parent = this@toFavoriteItem.parent
        homePageBookmark = this@toFavoriteItem.homePageBookmark
        useful = this@toFavoriteItem.useful
    }
}
