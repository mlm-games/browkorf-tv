package org.mlm.browkorftv.di

import androidx.room.Room
import org.mlm.browkorftv.activity.main.DownloadsHistoryViewModel
import org.mlm.browkorftv.activity.main.DownloadsManager
import org.mlm.browkorftv.activity.main.HistoryViewModel
import org.mlm.browkorftv.activity.main.AdBlockRepository
import org.mlm.browkorftv.activity.main.AutoUpdateViewModel
import org.mlm.browkorftv.activity.main.BrowserUiViewModel
import org.mlm.browkorftv.activity.main.FavoritesViewModel
import org.mlm.browkorftv.activity.main.MainViewModel
import org.mlm.browkorftv.activity.main.TabsViewModel
import org.mlm.browkorftv.activity.main.UpdateViewModel
import org.mlm.browkorftv.activity.main.SettingsViewModel
import org.mlm.browkorftv.settings.SettingsManager
import org.mlm.browkorftv.singleton.AppDatabase
import org.mlm.browkorftv.singleton.shortcuts.ShortcutMgr
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    // Database
    single {
        Room.databaseBuilder(androidContext(), AppDatabase::class.java, "main.db")
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
    viewModel { TabsViewModel(get(), get(), get(), androidContext()) }
    viewModel { MainViewModel(get(), get(), get(), get()) }
    viewModel { AutoUpdateViewModel(get()) }
    viewModel { HistoryViewModel(get()) }
    viewModel { DownloadsHistoryViewModel(get()) }
    viewModel { FavoritesViewModel(get()) }
    viewModel { UpdateViewModel(get()) }
    viewModel { BrowserUiViewModel() }
}