package org.mlm.browkorftv.settings

import android.content.Context
import android.os.Build
import io.github.mlmgames.settings.core.SettingsRepository
import io.github.mlmgames.settings.core.backup.SettingsBackupManager
import io.github.mlmgames.settings.core.datastore.createSettingsDataStore
import io.github.mlmgames.settings.core.managers.MigrationManager
import io.github.mlmgames.settings.core.managers.ResetManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.*
import org.mlm.browkorftv.utils.Utils

class SettingsManager private constructor(context: Context) {

    companion object {
        private const val DATASTORE_NAME = "app_settings"
        private const val OLD_PREFS_NAME = "main"

        @Volatile
        private var instance: SettingsManager? = null

        fun getInstance(context: Context): SettingsManager {
            return instance ?: synchronized(this) {
                instance ?: SettingsManager(context.applicationContext).also { instance = it }
            }
        }
    }

    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val dataStore = createSettingsDataStore(appContext, DATASTORE_NAME)

    val repository = SettingsRepository(
        dataStore = dataStore,
        schema = AppSettingsSchema
    )

    val migrationManager = MigrationManager(dataStore, currentVersion = 1)
    val resetManager = ResetManager(dataStore, AppSettingsSchema)

    val backupManager = SettingsBackupManager<AppSettings>(
        dataStore = dataStore,
        schema = AppSettingsSchema,
        appId = appContext.packageName,
        schemaVersion = 1,
    )

    val settings: Flow<AppSettings> = repository.flow
    val current: AppSettings get() = settingsState.value

    val settingsState: StateFlow<AppSettings> = settings
        .stateIn(scope, SharingStarted.Eagerly, AppSettings())

    val directionalNavModeFlow: Flow<Boolean> =
        settings.map { it.directionalNavMode }.distinctUntilChanged()

    val themeFlow: Flow<Theme> =
        settings.map { it.theme }.distinctUntilChanged()

    val forceDarkWebpageFlow: Flow<Boolean> =
        settings.map { it.forceDarkWebpage }.distinctUntilChanged()

    val keepScreenOnFlow: Flow<Boolean> =
        settings.map { it.keepScreenOn }.distinctUntilChanged()

    val incognitoModeFlow: Flow<Boolean> =
        settings.map { it.incognitoMode }.distinctUntilChanged()

    val adBlockEnabledFlow: Flow<Boolean> =
        settings.map { it.adBlockEnabled }.distinctUntilChanged()

    val webEngineFlow: Flow<String> =
        settings.map { it.webEngine }.distinctUntilChanged()

    val searchEngineURLFlow: Flow<String> =
        settings.map { it.searchEngineURL }.distinctUntilChanged()

    val userAgentFlow: Flow<String?> =
        settings.map { it.effectiveUserAgent }.distinctUntilChanged()

    val bookmarksFlow: Flow<List<BookmarkEntry>> =
        settings.map { it.bookmarks.sortedByDescending(BookmarkEntry::id) }.distinctUntilChanged()

    val showContextMenuOnLongPressFlow: Flow<Boolean> =
        settings.map { it.showContextMenuOnLongPress }.distinctUntilChanged()

    val proxyFlow: Flow<AppSettings> =
        settings.distinctUntilChanged { old, new ->
            old.proxyEnabled == new.proxyEnabled && old.proxyUrl == new.proxyUrl
        }

    suspend fun update(transform: (AppSettings) -> AppSettings) {
        repository.update(transform)
    }

    suspend fun set(name: String, value: Any) {
        repository.set(name, value)
    }

    suspend fun setTheme(theme: Theme) {
        update { it.copy(theme = theme) }
    }

    suspend fun setKeepScreenOn(value: Boolean) {
        update { it.copy(keepScreenOn = value) }
    }

    suspend fun setIncognitoMode(value: Boolean) {
        update { it.copy(incognitoMode = value) }
    }

    suspend fun setSearchEngine(index: Int, customUrl: String? = null) {
        update { s ->
            var updated = s.copy(searchEngineIndex = index)
            if (customUrl != null) {
                updated = updated.copy(searchEngineCustomUrl = customUrl)
            }

            if (s.homePageMode == HomePageMode.SearchEngine) {
                val home = AppSettings.searchEngineHomeUrl(
                    index = updated.searchEngineIndex,
                    customSearchUrl = updated.searchEngineCustomUrl
                )
                updated = updated.copy(homePage = home)
            }
            updated
        }
    }

    suspend fun setHomePageProperties(mode: HomePageMode, customUrl: String? = null) {
        update { s ->
            val home = when (mode) {
                HomePageMode.SearchEngine -> AppSettings.searchEngineHomeUrl(
                    index = s.searchEngineIndex,
                    customSearchUrl = s.searchEngineCustomUrl
                )

                HomePageMode.Custom -> customUrl ?: AppSettings.HOME_URL_ALIAS
                HomePageMode.HomePage, HomePageMode.Blank -> AppSettings.HOME_URL_ALIAS
            }
            s.copy(homePageMode = mode, homePage = home)
        }
    }

    suspend fun setWebEngine(index: Int) {
        update { it.copy(webEngineIndex = index) }
    }

    suspend fun setAdBlockEnabled(enabled: Boolean) {
        update { it.copy(adBlockEnabled = enabled) }
    }

    suspend fun setAdBlockListLastUpdate(timestamp: Long) {
        update { it.copy(adBlockListLastUpdate = timestamp) }
    }

    suspend fun setIncognitoModeHintSuppress(value: Boolean) {
        update { it.copy(incognitoModeHintSuppress = value) }
    }

    suspend fun setAppVersionCodeMark(version: Int) {
        update { it.copy(appVersionCodeMark = version) }
    }

    suspend fun setNotificationAboutEngineChangeShown(version: Int) {
        update { it.copy(notificationAboutEngineChangeShown = version) }
    }

    suspend fun setInitialBookmarksSuggestionsLoaded(value: Boolean) {
        update { it.copy(initialBookmarksSuggestionsLoaded = value) }
    }

    private fun extractBaseUrl(url: String): String {
        val regex = """^https?://[^#?/]+""".toRegex()
        return regex.find(url)?.value ?: AppSettings.HOME_URL_ALIAS
    }


    suspend fun setUserAgent(index: Int, customUA: String? = null) {
        update { settings ->
            settings.copy(
                userAgentIndex = index,
                userAgentCustom = customUA
            )
        }
    }

    suspend fun getBookmarks(): List<BookmarkEntry> {
        return current.bookmarks.sortedByDescending { it.id }
    }

    suspend fun getBookmark(id: Long): BookmarkEntry? {
        return current.bookmarks.firstOrNull { it.id == id }
    }

    suspend fun upsertBookmark(bookmark: BookmarkEntry): BookmarkEntry {
        var saved: BookmarkEntry? = null

        update { state ->
            val items = state.bookmarks.toMutableList()
            val resolved = if (bookmark.id == 0L) {
                bookmark.copy(id = (items.maxOfOrNull { it.id } ?: 0L) + 1L)
            } else {
                bookmark
            }

            val existingIndex = items.indexOfFirst { it.id == resolved.id }
            if (existingIndex >= 0) {
                items[existingIndex] = resolved
            } else {
                items.add(resolved)
            }

            saved = resolved
            state.copy(bookmarks = items.sortedByDescending { it.id })
        }

        return checkNotNull(saved)
    }

    suspend fun deleteBookmark(id: Long) {
        update { state ->
            state.copy(bookmarks = state.bookmarks.filterNot { it.id == id })
        }
    }

    suspend fun replaceBookmarks(bookmarks: List<BookmarkEntry>) {
        update { state ->
            state.copy(bookmarks = bookmarks.sortedByDescending { it.id })
        }
    }

    suspend fun markBookmarksMigratedFromRoom() {
        update { it.copy(bookmarksMigratedFromRoom = true) }
    }

    // Helper function (move to Utils ig)
    fun canRecommendGeckoView(context: Context): Boolean {
        val deviceRAM = Utils.memInfo(
            context
        ).totalMem
        val cpuHas64Bit = Build.SUPPORTED_64_BIT_ABIS.isNotEmpty()
        val cpuCores = Runtime.getRuntime().availableProcessors()
        val threeGB = 3_000_000_000L
        return deviceRAM >= threeGB && cpuHas64Bit && cpuCores >= 6
    }
}
