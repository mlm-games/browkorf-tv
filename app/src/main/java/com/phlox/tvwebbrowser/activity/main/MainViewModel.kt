package com.phlox.tvwebbrowser.activity.main

import android.os.Build
import android.util.Log
import android.webkit.WebView
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.phlox.tvwebbrowser.BuildConfig
import com.phlox.tvwebbrowser.TVBro
import com.phlox.tvwebbrowser.model.FavoriteItem
import com.phlox.tvwebbrowser.model.HistoryItem
import com.phlox.tvwebbrowser.model.WebTabState
import com.phlox.tvwebbrowser.model.dao.FavoritesDao
import com.phlox.tvwebbrowser.model.dao.HistoryDao
import com.phlox.tvwebbrowser.settings.AppSettings
import com.phlox.tvwebbrowser.settings.HomePageLinksMode
import com.phlox.tvwebbrowser.settings.HomePageMode
import com.phlox.tvwebbrowser.settings.SettingsManager
import com.phlox.tvwebbrowser.utils.UpdateChecker
import com.phlox.tvwebbrowser.utils.deleteDirectory
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.File
import java.util.Calendar
import java.util.Locale

class MainViewModel(
    private val historyDao: HistoryDao,
    private val favoritesDao: FavoritesDao,
    private val settingsManager: SettingsManager,
    private val tabsViewModel: TabsViewModel
) : ViewModel() {

    companion object {
        var TAG: String = MainViewModel::class.java.simpleName
        const val WEB_VIEW_DATA_FOLDER = "app_webview"
        const val WEB_VIEW_CACHE_FOLDER = "WebView"
        const val WEB_VIEW_DATA_BACKUP_DIRECTORY_SUFFIX = "_backup"
        const val INCOGNITO_DATA_DIRECTORY_SUFFIX = "incognito"
    }

    var loaded = false
    var lastHistoryItem: HistoryItem? = null
    private var lastHistoryItemSaveJob: Job? = null

    private val settings: AppSettings get() = settingsManager.current

    fun loadState() = viewModelScope.launch(Dispatchers.Main) {
        if (loaded) return@launch
        checkVersionCodeAndRunMigrations()
        initHistory()
        // No need to manually loadHomePageLinks() anymore!
        loaded = true
    }

    private suspend fun checkVersionCodeAndRunMigrations() {
        if (settings.appVersionCodeMark != BuildConfig.VERSION_CODE) {
            settingsManager.setAppVersionCodeMark(BuildConfig.VERSION_CODE)
            withContext(Dispatchers.IO) {
                UpdateChecker.clearTempFilesIfAny(TVBro.instance)
            }
        }
    }

    private suspend fun initHistory() {
        val count = historyDao.count()
        if (count > 5000) {
            val c = Calendar.getInstance()
            c.add(Calendar.MONTH, -3)
            historyDao.deleteWhereTimeLessThan(c.time.time)
        }
        try {
            val result = historyDao.lastFlow().first()
            if (result.isNotEmpty()) {
                lastHistoryItem = result[0]
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun logVisitedHistory(title: String?, url: String, faviconHash: String?) {
        if ((url == lastHistoryItem?.url) || url == AppSettings.HOME_PAGE_URL || !url.startsWith("http", true)) {
            return
        }

        val now = System.currentTimeMillis()
        val minVisitedInterval = 5000L

        lastHistoryItem?.let {
            if ((!it.saved) && (it.time + minVisitedInterval) > now) {
                lastHistoryItemSaveJob?.cancel()
            }
        }

        val item = HistoryItem()
        item.url = url
        item.title = title ?: ""
        item.time = now
        item.favicon = faviconHash
        lastHistoryItem = item
        lastHistoryItemSaveJob = viewModelScope.launch(Dispatchers.Main) {
            delay(minVisitedInterval)
            item.id = historyDao.insert(item)
            item.saved = true
        }
    }



    fun onTabTitleUpdated(tab: WebTabState) {
        if (settings.incognitoMode) return
        val item = lastHistoryItem ?: return
        if (tab.url == item.url) {
            item.title = tab.title
            if (item.saved) {
                viewModelScope.launch(Dispatchers.Main) {
                    historyDao.updateTitle(item.id, item.title)
                }
            }
        }
    }

    fun prepareSwitchToIncognito() {
        if (settings.isWebEngineGecko) return
        //to isolate incognito mode data:
        //in api >= 28 we just use another directory for WebView data
        //on earlier apis we backup-ing existing WebView data directory
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val incognitoWebViewData = File(
                TVBro.instance.filesDir.parentFile!!.absolutePath +
                        "/" + WEB_VIEW_DATA_FOLDER + "_" + INCOGNITO_DATA_DIRECTORY_SUFFIX
            )
            if (incognitoWebViewData.exists()) {
                Log.i(TAG, "Looks like we already in incognito mode")
                return
            }
            WebView.setDataDirectorySuffix(INCOGNITO_DATA_DIRECTORY_SUFFIX)
        } else {
            val webViewData = File(
                TVBro.instance.filesDir.parentFile!!.absolutePath +
                        "/" + WEB_VIEW_DATA_FOLDER
            )
            val backupedWebViewData = File(
                TVBro.instance.filesDir.parentFile!!.absolutePath +
                        "/" + WEB_VIEW_DATA_FOLDER + WEB_VIEW_DATA_BACKUP_DIRECTORY_SUFFIX
            )
            if (backupedWebViewData.exists()) {
                Log.i(TAG, "Looks like we already in incognito mode")
                return
            }
            webViewData.renameTo(backupedWebViewData)
            val webViewCache =
                File(TVBro.instance.cacheDir.absolutePath + "/" + WEB_VIEW_CACHE_FOLDER)
            val backupedWebViewCache = File(
                TVBro.instance.cacheDir.absolutePath + "/" + WEB_VIEW_CACHE_FOLDER +
                        WEB_VIEW_DATA_BACKUP_DIRECTORY_SUFFIX
            )
            webViewCache.renameTo(backupedWebViewCache)
        }
    }

    fun clearIncognitoData() = viewModelScope.launch(Dispatchers.IO) {
        Log.d(TAG, "clearIncognitoData")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val webViewData = File(
                TVBro.instance.filesDir.parentFile!!.absolutePath +
                        "/" + WEB_VIEW_DATA_FOLDER + "_" + INCOGNITO_DATA_DIRECTORY_SUFFIX
            )
            deleteDirectory(webViewData)
            var webViewCache =
                File(
                    TVBro.instance.cacheDir.absolutePath + "/" + WEB_VIEW_CACHE_FOLDER +
                            "_" + INCOGNITO_DATA_DIRECTORY_SUFFIX
                )
            if (!webViewCache.exists()) {
                webViewCache = File(
                    TVBro.instance.cacheDir.absolutePath + "/" +
                            WEB_VIEW_CACHE_FOLDER.lowercase(Locale.getDefault()) +
                            "_" + INCOGNITO_DATA_DIRECTORY_SUFFIX
                )
            }
            deleteDirectory(webViewCache)
        } else {
            val webViewData = File(
                TVBro.instance.filesDir.parentFile!!.absolutePath +
                        "/" + WEB_VIEW_DATA_FOLDER
            )
            deleteDirectory(webViewData)
            val webViewCache =
                File(TVBro.instance.cacheDir.absolutePath + "/" + WEB_VIEW_CACHE_FOLDER)
            deleteDirectory(webViewCache)

            val backupedWebViewData = File(
                TVBro.instance.filesDir.parentFile!!.absolutePath +
                        "/" + WEB_VIEW_DATA_FOLDER + WEB_VIEW_DATA_BACKUP_DIRECTORY_SUFFIX
            )
            backupedWebViewData.renameTo(webViewData)
            val backupedWebViewCache = File(
                TVBro.instance.cacheDir.absolutePath + "/" + WEB_VIEW_CACHE_FOLDER +
                        WEB_VIEW_DATA_BACKUP_DIRECTORY_SUFFIX
            )
            backupedWebViewCache.renameTo(webViewCache)
        }
    }

    fun onHomePageLinkEdited(item: FavoriteItem) = viewModelScope.launch(Dispatchers.IO) {
        if (item.id == 0L) {
            favoritesDao.insert(item)
        } else {
            favoritesDao.update(item)
        }
    }
}