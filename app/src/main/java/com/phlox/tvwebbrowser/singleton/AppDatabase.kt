package com.phlox.tvwebbrowser.singleton

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.phlox.tvwebbrowser.TVBro
import com.phlox.tvwebbrowser.model.*
import com.phlox.tvwebbrowser.model.dao.*
import com.phlox.tvwebbrowser.model.util.Converters

@Database(
    entities = [
        WebTabState::class,
        HistoryItem::class,
        Download::class,
        FavoriteItem::class,
        HostConfig::class
    ],
    version = 14,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun tabsDao(): TabsDao
    abstract fun historyDao(): HistoryDao
    abstract fun downloadsDao(): DownloadDao
    abstract fun favoritesDao(): FavoritesDao
    abstract fun hostsDao(): HostsDao

    companion object {
        val db: AppDatabase by lazy {
            Room.databaseBuilder(
                TVBro.instance,
                AppDatabase::class.java,
                "main.db"
            )
                .build()
        }
    }
}