package org.mlm.browkorftv.singleton

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import org.mlm.browkorftv.model.Download
import org.mlm.browkorftv.model.FavoriteItem
import org.mlm.browkorftv.model.HistoryItem
import org.mlm.browkorftv.model.HostConfig
import org.mlm.browkorftv.model.WebTabState
import org.mlm.browkorftv.model.dao.DownloadDao
import org.mlm.browkorftv.model.dao.FavoritesDao
import org.mlm.browkorftv.model.dao.HistoryDao
import org.mlm.browkorftv.model.dao.HostsDao
import org.mlm.browkorftv.model.dao.TabsDao
import org.mlm.browkorftv.model.util.Converters

@Database(entities = [
    Download::class, FavoriteItem::class,
    HistoryItem::class, WebTabState::class,
    HostConfig::class
], version = 19)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun downloadDao(): DownloadDao
    abstract fun historyDao(): HistoryDao
    abstract fun favoritesDao(): FavoritesDao
    abstract fun tabsDao(): TabsDao
    abstract fun hostsDao(): HostsDao
}