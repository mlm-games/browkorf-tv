package org.mlm.browkorftv

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.lifecycleScope
import org.mlm.browkorftv.di.appModule
import org.mlm.browkorftv.network.ProxyManager
import org.mlm.browkorftv.settings.SettingsManager
import org.mlm.browkorftv.settings.Theme
import org.mlm.browkorftv.singleton.AppDatabase
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

class BrowkorfTV : Application(), Application.ActivityLifecycleCallbacks {
    companion object {
        lateinit var instance: BrowkorfTV
        const val CHANNEL_ID_DOWNLOADS: String = "downloads"
        val TAG = BrowkorfTV::class.simpleName
    }

    lateinit var threadPool: ThreadPoolExecutor
        private set

    var needToExitProcessAfterMainActivityFinish = false
    var needRestartMainActivityAfterExitingProcess = false

    private val settingsManager: SettingsManager by inject()
    private val database: AppDatabase by inject()

    override fun onCreate() {
        super.onCreate()
        instance = this

        startKoin {
            androidLogger()
            androidContext(this@BrowkorfTV)
            modules(appModule)
        }

        val maxThreadsInOfflineJobsPool = Runtime.getRuntime().availableProcessors()
        threadPool = ThreadPoolExecutor(
            0, maxThreadsInOfflineJobsPool, 20,
            TimeUnit.SECONDS, ArrayBlockingQueue(maxThreadsInOfflineJobsPool)
        )

        initWebEngineStuff()
        initNotificationChannels()

        applyTheme(settingsManager.current.theme)
        ProxyManager.apply(settingsManager.current)

        // Observe theme changes
        ProcessLifecycleOwner.get().lifecycleScope.launch {
            settingsManager.themeFlow.collectLatest { theme ->
                applyTheme(theme)
            }
        }

        ProcessLifecycleOwner.get().lifecycleScope.launch {
            settingsManager.proxyFlow.collectLatest { settings ->
                ProxyManager.apply(settings)
            }
        }

        registerActivityLifecycleCallbacks(this)
    }

    private fun applyTheme(theme: Theme) {
        when (theme) {
            Theme.Black -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            Theme.White -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            Theme.System -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        }
    }

    private fun initWebEngineStuff() {
        val cookieManager = java.net.CookieManager()
        java.net.CookieHandler.setDefault(cookieManager)
    }

    private fun initNotificationChannels() {
        val name = getString(R.string.downloads)
        val descriptionText = getString(R.string.downloads_notifications_description)
        val importance = NotificationManager.IMPORTANCE_LOW
        val channel = NotificationChannel(CHANNEL_ID_DOWNLOADS, name, importance).apply {
            description = descriptionText
        }
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    override fun onActivityCreated(activity: android.app.Activity, savedInstanceState: android.os.Bundle?) {}
    override fun onActivityStarted(activity: android.app.Activity) {}
    override fun onActivityResumed(activity: android.app.Activity) {}
    override fun onActivityPaused(activity: android.app.Activity) {}
    override fun onActivityStopped(activity: android.app.Activity) {}
    override fun onActivitySaveInstanceState(activity: android.app.Activity, outState: android.os.Bundle) {}

    override fun onActivityDestroyed(activity: android.app.Activity) {
        if (needToExitProcessAfterMainActivityFinish && activity is org.mlm.browkorftv.activity.main.MainActivity) {
            if (needRestartMainActivityAfterExitingProcess) {
                val intent = android.content.Intent(this, org.mlm.browkorftv.activity.main.MainActivity::class.java)
                intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK)
                startActivity(intent)
            }
            exitProcess(0)
        }
    }
}
