package com.phlox.tvwebbrowser.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import com.phlox.tvwebbrowser.Config
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ConfigRepository(
    private val ds: DataStore<Preferences>,
    private val legacy: Config
) {
    private val adblockKey = booleanPreferencesKey(Config.ADBLOCK_ENABLED_PREF_KEY)
    private val adblockListUrlKey = stringPreferencesKey(Config.ADBLOCK_LIST_URL_KEY)
    private val adblockLastUpdateKey = longPreferencesKey(Config.ADBLOCK_LAST_UPDATE_LIST_KEY)

    private val webEngineKey = stringPreferencesKey(Config.WEB_ENGINE)
    private val autoplayKey = booleanPreferencesKey(Config.ALLOW_AUTOPLAY_MEDIA)
    private val keepScreenOnKey = booleanPreferencesKey(Config.KEEP_SCREEN_ON_KEY)

    private val autoCheckUpdatesKey = booleanPreferencesKey(Config.AUTO_CHECK_UPDATES_KEY)
    private val updateChannelKey = stringPreferencesKey(Config.UPDATE_CHANNEL_KEY)

    val adblockEnabled: Flow<Boolean> =
        ds.data.map { it[adblockKey] ?: legacy.adBlockEnabled }

    val adblockListUrl: Flow<String> =
        ds.data.map { it[adblockListUrlKey] ?: legacy.adBlockListURL.value }

    val adblockLastUpdate: Flow<Long> =
        ds.data.map { it[adblockLastUpdateKey] ?: legacy.adBlockListLastUpdate }

    val webEngine: Flow<String> =
        ds.data.map { it[webEngineKey] ?: legacy.webEngine }

    val allowAutoplayMedia: Flow<Boolean> =
        ds.data.map { it[autoplayKey] ?: legacy.allowAutoplayMedia }

    val keepScreenOn: Flow<Boolean> =
        ds.data.map { it[keepScreenOnKey] ?: legacy.keepScreenOn }

    val autoCheckUpdates: Flow<Boolean> =
        ds.data.map { it[autoCheckUpdatesKey] ?: legacy.autoCheckUpdates }

    val updateChannel: Flow<String> =
        ds.data.map { it[updateChannelKey] ?: legacy.updateChannel }

    suspend fun setAdblockEnabled(enabled: Boolean) {
        ds.edit { it[adblockKey] = enabled }
        legacy.adBlockEnabled = enabled
    }

    suspend fun setAdblockListUrl(url: String) {
        ds.edit { it[adblockListUrlKey] = url }
        legacy.adBlockListURL.value = url
    }

    suspend fun setAdblockLastUpdate(epochMillis: Long) {
        ds.edit { it[adblockLastUpdateKey] = epochMillis }
        legacy.adBlockListLastUpdate = epochMillis
    }

    suspend fun setWebEngine(engine: String) {
        ds.edit { it[webEngineKey] = engine }
        legacy.webEngine = engine
    }

    suspend fun setAllowAutoplayMedia(enabled: Boolean) {
        ds.edit { it[autoplayKey] = enabled }
        legacy.allowAutoplayMedia = enabled
    }

    suspend fun setKeepScreenOn(enabled: Boolean) {
        ds.edit { it[keepScreenOnKey] = enabled }
        legacy.keepScreenOn = enabled
    }

    suspend fun setAutoCheckUpdates(enabled: Boolean) {
        ds.edit { it[autoCheckUpdatesKey] = enabled }
        legacy.autoCheckUpdates = enabled
    }

    suspend fun setUpdateChannel(channel: String) {
        ds.edit { it[updateChannelKey] = channel }
        legacy.updateChannel = channel
    }
}