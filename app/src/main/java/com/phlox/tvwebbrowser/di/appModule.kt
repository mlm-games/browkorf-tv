package com.phlox.tvwebbrowser.di

import androidx.room.Room
import com.phlox.tvwebbrowser.activity.downloads.DownloadsHistoryViewModel
import com.phlox.tvwebbrowser.activity.downloads.DownloadsManager
import com.phlox.tvwebbrowser.activity.history.HistoryViewModel
import com.phlox.tvwebbrowser.activity.main.AdBlockRepository
import com.phlox.tvwebbrowser.activity.main.AutoUpdateViewModel
import com.phlox.tvwebbrowser.activity.main.BookmarkEditorViewModel
import com.phlox.tvwebbrowser.activity.main.BrowserUiViewModel
import com.phlox.tvwebbrowser.activity.main.FavoritesViewModel
import com.phlox.tvwebbrowser.activity.main.HomePageSlotEditorViewModel
import com.phlox.tvwebbrowser.activity.main.MainViewModel
import com.phlox.tvwebbrowser.activity.main.TabsViewModel
import com.phlox.tvwebbrowser.activity.main.UpdateViewModel
import com.phlox.tvwebbrowser.compose.settings.SettingsViewModel
import com.phlox.tvwebbrowser.settings.SettingsManager
import com.phlox.tvwebbrowser.singleton.AppDatabase
import com.phlox.tvwebbrowser.singleton.shortcuts.ShortcutMgr
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    // Database
    single {
        Room.databaseBuilder(androidContext(), AppDatabase::class.java, "main.db")
            .allowMainThreadQueries() // Legacy support
            .build()
    }

    // DAOs
    single { get<AppDatabase>().downloadDao() }
    single { get<AppDatabase>().historyDao() }
    single { get<AppDatabase>().favoritesDao() }
    single { get<AppDatabase>().tabsDao() }
    single { get<AppDatabase>().hostsDao() }

    // Core Singletons
    single { SettingsManager.getInstance(androidContext()) }
    single { ShortcutMgr.getInstance() }

    single { AdBlockRepository(get(), androidContext()) }
    single { DownloadsManager(get(), androidContext()) }

    // --- ViewModels ---
    viewModel { SettingsViewModel(get()) }
    viewModel { TabsViewModel(get(), get(), get()) }
    viewModel { MainViewModel(get(), get(), get(), get()) }
    viewModel { AutoUpdateViewModel(get()) }
    viewModel { HistoryViewModel(get()) }
    viewModel { DownloadsHistoryViewModel(get()) }
    viewModel { FavoritesViewModel(get()) }
    viewModel { BookmarkEditorViewModel(it.get(), get()) }
    viewModel { HomePageSlotEditorViewModel(it.get(), get()) }
    viewModel { UpdateViewModel(get()) }
    viewModel { BrowserUiViewModel() }
}