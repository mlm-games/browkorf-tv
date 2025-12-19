package com.phlox.tvwebbrowser.singleton

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.phlox.tvwebbrowser.model.*
import com.phlox.tvwebbrowser.model.dao.*
import com.phlox.tvwebbrowser.model.util.Converters

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