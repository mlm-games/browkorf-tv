package com.phlox.tvwebbrowser.di

import com.phlox.tvwebbrowser.TVBro
import com.phlox.tvwebbrowser.Config
import com.phlox.tvwebbrowser.compose.vm.BrowserDataViewModel
import com.phlox.tvwebbrowser.compose.vm.DownloadsViewModel
import com.phlox.tvwebbrowser.compose.vm.HistoryViewModel
import com.phlox.tvwebbrowser.compose.vm.TabsViewModel
import com.phlox.tvwebbrowser.core.DefaultDispatcherProvider
import com.phlox.tvwebbrowser.core.DispatcherProvider
import com.phlox.tvwebbrowser.data.AdblockRepository
import com.phlox.tvwebbrowser.data.history.HistoryRepository
import com.phlox.tvwebbrowser.data.history.RoomHistoryRepository
import com.phlox.tvwebbrowser.data.settings.ConfigRepository
import com.phlox.tvwebbrowser.data.settings.provideTvBroDataStore
import com.phlox.tvwebbrowser.data.tabs.RoomTabsRepository
import com.phlox.tvwebbrowser.data.tabs.TabsRepository
import com.phlox.tvwebbrowser.singleton.AppDatabase
import com.phlox.tvwebbrowser.compose.runtime.BrowserCommandBus
import com.phlox.tvwebbrowser.compose.runtime.ShortcutCaptureController
import com.phlox.tvwebbrowser.compose.vm.BookmarkEditorViewModel
import com.phlox.tvwebbrowser.compose.vm.FavoritesViewModel
import com.phlox.tvwebbrowser.compose.vm.HomePageSlotEditorViewModel
import com.phlox.tvwebbrowser.compose.vm.UpdateViewModel
import com.phlox.tvwebbrowser.data.ApkDownloader
import com.phlox.tvwebbrowser.data.DownloadsRepository
import com.phlox.tvwebbrowser.data.FavoritesRepository
import com.phlox.tvwebbrowser.data.RecommendationsRepository
import com.phlox.tvwebbrowser.data.RoomDownloadsRepository
import com.phlox.tvwebbrowser.data.RoomFavoritesRepository
import com.phlox.tvwebbrowser.data.UpdateRepository
import com.phlox.tvwebbrowser.singleton.shortcuts.ShortcutMgr
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val tvBroCoreModule = module {
    single { TVBro.config }
    single<DispatcherProvider> { DefaultDispatcherProvider() }
}

val updateModule = module {
    single { UpdateRepository() }
    single { ApkDownloader() }
    viewModel {
        UpdateViewModel(
            configRepo = get(),
            repo = get(),
            downloader = get(),
            dispatchers = get()
        )
    }
}

val roomModule = module {
    single { AppDatabase.db }
    single { get<AppDatabase>().tabsDao() }
    single { get<AppDatabase>().historyDao() }
    single { get<AppDatabase>().favoritesDao() }
    single { get<AppDatabase>().hostsDao() }
    single { get<AppDatabase>().downloadsDao() }
}

val dataStoreModule = module {
    single { provideTvBroDataStore(androidContext()) }
    single { ConfigRepository(get(), get()) }
}

val tabsModule = module {
    single<TabsRepository> { RoomTabsRepository(get()) }
    viewModel { TabsViewModel(repo = get(), config = get(), dispatchers = get()) }
}

val browserBusModule = module {
    single { BrowserCommandBus() }
}

val favoritesUiModule = module {
    viewModel { FavoritesViewModel(repo = get(), dispatchers = get()) }
    viewModel { HomePageSlotEditorViewModel(repo = get(), dispatchers = get()) }
    viewModel { BookmarkEditorViewModel(repo = get(), dispatchers = get()) }
}

val shortcutsModule = module {
    single { ShortcutMgr.getInstance() }
    single { ShortcutCaptureController() }
}

val dispatchersModule = module {
    single<DispatcherProvider> { DefaultDispatcherProvider() }
}



val browserDataModule = module {
    single<HistoryRepository> { RoomHistoryRepository(get()) }
    single<FavoritesRepository> { RoomFavoritesRepository(get()) }
    single { RecommendationsRepository() }
    viewModel {
        BrowserDataViewModel(
            config = get(),
            historyRepo = get(),
            favoritesRepo = get(),
            recommendationsRepo = get(),
            dispatchers = get()
        )
    }
}

val adblockModule = module {
    single {
        AdblockRepository(
            appFilesDir = androidContext().filesDir,
            config = get(),
            configRepo = get(),
            dispatchers = get()
        )
    }
}

val downloadsModule = module {
    single<DownloadsRepository> { RoomDownloadsRepository(get()) }
    viewModel { DownloadsViewModel(repo = get(), dispatchers = get()) }
}

val historyUiModule = module {
    viewModel { HistoryViewModel(repo = get(), dispatchers = get()) }
}