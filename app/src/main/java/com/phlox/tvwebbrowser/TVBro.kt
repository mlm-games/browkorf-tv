package com.phlox.tvwebbrowser

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.phlox.tvwebbrowser.di.appModule
import com.phlox.tvwebbrowser.model.HostConfig
import com.phlox.tvwebbrowser.settings.SettingsManager
import com.phlox.tvwebbrowser.settings.Theme
import com.phlox.tvwebbrowser.singleton.AppDatabase
import com.phlox.tvwebbrowser.singleton.FaviconsPool
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import kotlin.system.exitProcess

class TVBro : Application(), Application.ActivityLifecycleCallbacks {
    companion object {
        lateinit var instance: TVBro
        const val CHANNEL_ID_DOWNLOADS: String = "downloads"
        val TAG = TVBro::class.simpleName
    }

    lateinit var threadPool: ThreadPoolExecutor
        private set

    var needToExitProcessAfterMainActivityFinish = false
    var needRestartMainActivityAfterExitingProcess = false

    // Lazy inject for usage in onCreate
    private val settingsManager: SettingsManager by inject()
    private val database: AppDatabase by inject()

    override fun onCreate() {
        super.onCreate()
        instance = this

        // 1. Start Koin
        startKoin {
            androidLogger()
            androidContext(this@TVBro)
            modules(appModule)
        }

        // 2. Initialize AppContext (Legacy bridge)
        AppContext.init(this)

        // 3. Thread Pool
        val maxThreadsInOfflineJobsPool = Runtime.getRuntime().availableProcessors()
        threadPool = ThreadPoolExecutor(
            0, maxThreadsInOfflineJobsPool, 20,
            TimeUnit.SECONDS, ArrayBlockingQueue(maxThreadsInOfflineJobsPool)
        )

        // 4. Initialize Engines and Channels
        initWebEngineStuff()
        initNotificationChannels()

        // 5. Apply Theme
        applyTheme(settingsManager.current.themeEnum)

        // Observe theme changes
        ProcessLifecycleOwner.get().lifecycleScope.launch {
            settingsManager.themeFlow.collectLatest { theme ->
                applyTheme(theme)
            }
        }

        registerActivityLifecycleCallbacks(this)
    }

    private fun applyTheme(theme: Theme) {
        when (theme) {
            Theme.BLACK -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            Theme.WHITE -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            Theme.SYSTEM -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        }
    }

    private fun initWebEngineStuff() {
        try {
            Class.forName("com.phlox.tvwebbrowser.webengine.webview.WebViewWebEngine")
        } catch (e: ClassNotFoundException) {
            throw AssertionError(e)
        }
        try {
            Class.forName("com.phlox.tvwebbrowser.webengine.gecko.GeckoWebEngine")
        } catch (e: ClassNotFoundException) {
            // GeckoWebEngine not found
        }

        val cookieManager = java.net.CookieManager()
        java.net.CookieHandler.setDefault(cookieManager)

        // Bridge Koin Database to Legacy FaviconsPool
        FaviconsPool.databaseDelegate = object : FaviconsPool.DatabaseDelegate {
            override fun findByHostName(host: String): HostConfig? {
                return database.hostsDao().findByHostName(host)
            }

            override suspend fun update(hostConfig: HostConfig) {
                database.hostsDao().update(hostConfig)
            }

            override suspend fun insert(newHostConfig: HostConfig) {
                database.hostsDao().insert(newHostConfig)
            }
        }
    }

    private fun initNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = getString(R.string.downloads)
            val descriptionText = getString(R.string.downloads_notifications_description)
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(CHANNEL_ID_DOWNLOADS, name, importance).apply {
                description = descriptionText
            }
            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onActivityCreated(activity: android.app.Activity, savedInstanceState: android.os.Bundle?) {}
    override fun onActivityStarted(activity: android.app.Activity) {}
    override fun onActivityResumed(activity: android.app.Activity) {}
    override fun onActivityPaused(activity: android.app.Activity) {}
    override fun onActivityStopped(activity: android.app.Activity) {}
    override fun onActivitySaveInstanceState(activity: android.app.Activity, outState: android.os.Bundle) {}

    override fun onActivityDestroyed(activity: android.app.Activity) {
        if (needToExitProcessAfterMainActivityFinish && activity is com.phlox.tvwebbrowser.activity.main.MainActivity) {
            if (needRestartMainActivityAfterExitingProcess) {
                val intent = android.content.Intent(this, com.phlox.tvwebbrowser.activity.main.MainActivity::class.java)
                intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK)
                startActivity(intent)
            }
            exitProcess(0)
        }
    }
}