package com.phlox.tvwebbrowser

import android.app.Application
import com.phlox.tvwebbrowser.di.*
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class TVBro : Application() {

    override fun onCreate() {
        super.onCreate()
        instance = this
        val prefs = getSharedPreferences(MAIN_PREFS_NAME, MODE_PRIVATE)
        config = Config(prefs)

        startKoin {
            androidContext(this@TVBro)
            modules(
                tvBroCoreModule,
                browserBusModule,
                roomModule,
                tabsModule,
                shortcutsModule,
                favoritesUiModule,
                updateModule,
                dataStoreModule,
                browserDataModule,
                adblockModule,
                downloadsModule,
                historyUiModule
            )
        }
    }

    companion object {
        lateinit var instance: TVBro
        lateinit var config: Config
        const val MAIN_PREFS_NAME = "main"
    }
}