package org.mlm.browkorftv.di

import androidx.room.Room
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import org.mlm.browkorftv.activity.main.*
import org.mlm.browkorftv.core.DefaultDispatcherProvider
import org.mlm.browkorftv.core.DispatcherProvider
import org.mlm.browkorftv.settings.SettingsManager
import org.mlm.browkorftv.singleton.AppDatabase
import org.mlm.browkorftv.singleton.shortcuts.ShortcutMgr
import org.mlm.browkorftv.updates.*

val appModule = module {

    // Dispatchers
    single<DispatcherProvider> { DefaultDispatcherProvider() }

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

    // Core singletons
    single { SettingsManager.getInstance(androidContext()) }
    single { ShortcutMgr.getInstance() }

    // Repos / managers
    single { AdBlockRepository(get(), androidContext()) }
    single { DownloadsManager(get(), androidContext()) }

    single<UpdateApi> { JsonUpdateApi(get()) }
    single { UpdateRepository(get()) }
    single { UpdateInstaller(androidContext(), get()) }

    // ViewModels
    viewModel { SettingsViewModel(get()) }
    viewModel { TabsViewModel(get(), get(), get(), androidContext()) }
    viewModel { MainViewModel(get(), get(), get(), get()) }
    viewModel { HistoryViewModel(get()) }
    viewModel { DownloadsHistoryViewModel(get()) }
    viewModel { FavoritesViewModel(get()) }
    viewModel { BrowserUiViewModel() }
    viewModel { UpdatesViewModel(get(), get(), get()) }
}