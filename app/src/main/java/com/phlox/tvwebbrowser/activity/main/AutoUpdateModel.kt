package com.phlox.tvwebbrowser.activity.main

import com.phlox.tvwebbrowser.BuildConfig
import com.phlox.tvwebbrowser.Config
import com.phlox.tvwebbrowser.TVBro
import com.phlox.tvwebbrowser.utils.UpdateChecker
import com.phlox.tvwebbrowser.utils.activemodel.ActiveModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*

class AutoUpdateModel: ActiveModel() {
    val config = TVBro.config
    var needToShowUpdateDlgAgain: Boolean = false
    val updateChecker = UpdateChecker(BuildConfig.VERSION_CODE)
    var lastUpdateNotificationTime: Calendar
    var needAutoCheckUpdates: Boolean
        get() = config.autoCheckUpdates
        set(value) { config.autoCheckUpdates = value }

    init {
        lastUpdateNotificationTime = if (config.prefs.contains(Config.LAST_UPDATE_USER_NOTIFICATION_TIME_KEY))
            Calendar.getInstance().apply { timeInMillis = config.prefs.getLong(Config.LAST_UPDATE_USER_NOTIFICATION_TIME_KEY, 0) } else
            Calendar.getInstance()
    }

    fun checkUpdate(force: Boolean, onDoneCallback: () -> Unit) = modelScope.launch(Dispatchers.Main) {
        if (updateChecker.versionCheckResult == null || force) {
            launch(Dispatchers.IO) {
                try {
                    updateChecker.check("https://raw.githubusercontent.com/truefedex/tv-bro/master/latest_version.json",
                        arrayOf(config.updateChannel))
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }.join()
        }
        onDoneCallback()
    }

    fun saveAutoCheckUpdates(need: Boolean) {
        config.autoCheckUpdates = need
    }
}