package org.mlm.browkorftv.settings

import android.os.Build
import org.mlm.browkorftv.AppContext
import org.mlm.browkorftv.utils.Utils
import io.github.mlmgames.settings.core.annotations.*
import io.github.mlmgames.settings.core.types.*

@CategoryDefinition(order = 0)
object General

@CategoryDefinition(order = 1)
object HomePage

@CategoryDefinition(order = 2)
object Search

@CategoryDefinition(order = 3)
object UserAgent

@CategoryDefinition(order = 4)
object WebEngine

@CategoryDefinition(order = 5)
object AdBlock

@CategoryDefinition(order = 6)
object Updates

// Internal category - not shown in UI, just for grouping
@CategoryDefinition(order = 100)
object Internal


@SchemaVersion(version = 1)
data class AppSettings(


    @Setting(
        title = "Theme",
        category = General::class,
        type = Dropdown::class,
        key = "theme",
        options = ["System", "Light", "Dark"]
    )
    val theme: Int = Theme.SYSTEM.ordinal,

    @Setting(
        title = "Keep Screen On",
        description = "Prevent screen from turning off while browsing",
        category = General::class,
        type = Toggle::class,
        key = "keep_screen_on"
    )
    val keepScreenOn: Boolean = false,

    @Setting(
        title = "Incognito Mode",
        description = "Browse without saving history",
        category = General::class,
        type = Toggle::class,
        key = "incognito_mode"
    )
    val incognitoMode: Boolean = false,

    @Setting(
        title = "Allow Autoplay Media",
        category = General::class,
        type = Toggle::class,
        key = "allow_autoplay_media"
    )
    val allowAutoplayMedia: Boolean = false,



    @Setting(
        title = "Home Page Mode",
        category = HomePage::class,
        type = Dropdown::class,
        key = "home_page_mode",
        options = ["Home Page", "Search Engine", "Custom", "Blank"]
    )
    val homePageMode: Int = HomePageMode.HOME_PAGE.ordinal,

    @Setting(
        title = "Home Page Links",
        category = HomePage::class,
        type = Dropdown::class,
        key = "home_page_suggestions_mode",
        options = ["Bookmarks", "Latest History", "Most Visited"]
    )
    val homePageLinksMode: Int = HomePageLinksMode.BOOKMARKS.ordinal,

    @Setting(
        title = "Custom Home Page URL",
        category = HomePage::class,
        type = TextInput::class,
        key = "home_page",
        dependsOn = "homePageMode" // Only relevant when mode is CUSTOM
    )
    val homePage: String = HOME_URL_ALIAS,



    @Setting(
        title = "Search Engine",
        category = Search::class,
        type = Dropdown::class,
        key = "search_engine_url",
        options = ["Google", "Bing", "Yahoo!", "DuckDuckGo", "Yandex", "Startpage", "Custom"]
    )
    val searchEngineIndex: Int = 0,

    @Persisted(key = "search_engine_custom_url")
    val searchEngineCustomUrl: String = "",



    @Setting(
        title = "User Agent",
        category = UserAgent::class,
        type = Dropdown::class,
        key = "user_agent_index",
        options = ["Default (recommended)", "Chrome (Desktop)", "Chrome (Mobile)",
            "Firefox (Desktop)", "Firefox (Mobile)", "Edge (Desktop)", "Custom"]
    )
    val userAgentIndex: Int = 0,

    @Persisted(key = "user_agent")
    val userAgentCustom: String? = null,



    @Setting(
        title = "Web Engine",
        description = "GeckoView recommended for devices with 3GB+ RAM",
        category = WebEngine::class,
        type = Dropdown::class,
        key = "web_engine",
        options = ["GeckoView", "WebView"]
    )
    @RequiresConfirmation(
        title = "Change Web Engine?",
        message = "App will restart to apply changes",
        isDangerous = true
    )
    val webEngineIndex: Int = -1, // -1 means not set, will use default



    @Setting(
        title = "Enable Ad Blocker",
        category = AdBlock::class,
        type = Toggle::class,
        key = "adblock_enabled"
    )
    val adBlockEnabled: Boolean = true,

    @Setting(
        title = "Ad Block List URL",
        category = AdBlock::class,
        type = TextInput::class,
        key = "adblock_list_url",
        dependsOn = "adBlockEnabled"
    )
    val adBlockListURL: String = DEFAULT_ADBLOCK_LIST_URL,



    @Setting(
        title = "Auto Check Updates",
        category = Updates::class,
        type = Toggle::class,
        key = "auto_check_updates"
    )
    val autoCheckUpdates: Boolean = true, // Will be overridden based on install source

    @Setting(
        title = "Update Channel",
        category = Updates::class,
        type = Dropdown::class,
        key = "update_channel",
        options = ["Release", "Beta"],
        dependsOn = "autoCheckUpdates"
    )
    val updateChannelIndex: Int = 0,



    @Persisted(key = "incognito_mode_hint_suppress")
    @NoReset
    val incognitoModeHintSuppress: Boolean = false,

    @Persisted(key = "last_update_notif")
    @NoReset
    val lastUpdateUserNotificationTime: Long = 0L,

    @Persisted(key = "adblock_last_update")
    @NoReset
    val adBlockListLastUpdate: Long = 0L,

    @Persisted(key = "app_web_extension_version")
    @NoReset
    val appWebExtensionVersion: Int = 0,

    @Persisted(key = "notification_about_engine_change_shown")
    @NoReset
    val notificationAboutEngineChangeShown: Int = 0,

    @Persisted(key = "app_version_code_mark")
    @NoReset
    val appVersionCodeMark: Int = 0,

    @Persisted(key = "initial_bookmarks_suggestions_loaded")
    @NoReset
    val initialBookmarksSuggestionsLoaded: Boolean = false,

    // Migration helper - tracks if we migrated from SharedPreferences
    @Persisted(key = "__migrated_from_shared_prefs__")
    @NoReset
    val migratedFromSharedPrefs: Boolean = false,
) {
    companion object {
        const val HOME_URL_ALIAS = "about:home"
        const val DEFAULT_ADBLOCK_LIST_URL = "https://easylist.to/easylist/easylist.txt"
        const val HOME_PAGE_URL = "https://zombiefox.github.io/nightTab/" // wanted ddg-lite by default but, well no bookmarks...

        const val ENGINE_GECKO_VIEW = "GeckoView"
        const val ENGINE_WEB_VIEW = "WebView"

        val SearchEnginesTitles = arrayOf("Google", "Bing", "Yahoo!", "DuckDuckGo", "Yandex", "Startpage", "Custom")
        val SearchEnginesURLs = listOf(
            "https://www.google.com/search?q=[query]",
            "https://www.bing.com/search?q=[query]",
            "https://search.yahoo.com/search?p=[query]",
            "https://duckduckgo.com/?q=[query]",
            "https://yandex.com/search/?text=[query]",
            "https://www.startpage.com/sp/search?query=[query]",
            "" // Custom
        )

        val UserAgentStrings = listOf(
            "", // Default
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Safari/537.36",
            "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36",
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:127.0) Gecko/20100101 Firefox/127.0",
            "Mozilla/5.0 (Android 11; Mobile; rv:125.0) Gecko/125.0 Firefox/125.0",
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Safari/537.36 Edg/125.0.0.0",
            "" // Custom
        )

        val SupportedWebEngines = arrayOf(ENGINE_GECKO_VIEW, ENGINE_WEB_VIEW)
        val UpdateChannels = arrayOf("release", "beta")
    }



    val themeEnum: Theme
        get() = Theme.entries.getOrElse(theme) { Theme.SYSTEM }

    val homePageModeEnum: HomePageMode
        get() = HomePageMode.entries.getOrElse(homePageMode) { HomePageMode.HOME_PAGE }

    val homePageLinksModeEnum: HomePageLinksMode
        get() = HomePageLinksMode.entries.getOrElse(homePageLinksMode) { HomePageLinksMode.BOOKMARKS }

    val searchEngineURL: String
        get() = if (searchEngineIndex < SearchEnginesURLs.size - 1) {
            SearchEnginesURLs[searchEngineIndex]
        } else {
            searchEngineCustomUrl
        }

    val effectiveUserAgent: String?
        get() = when {
            userAgentIndex == 0 -> null // Default
            userAgentIndex < UserAgentStrings.size - 1 -> UserAgentStrings[userAgentIndex]
            else -> userAgentCustom
        }

    val webEngine: String
        get() = SupportedWebEngines.getOrElse(webEngineIndex) {
            if (canRecommendGeckoView()) ENGINE_GECKO_VIEW else ENGINE_WEB_VIEW
        }

    val updateChannel: String
        get() = UpdateChannels.getOrElse(updateChannelIndex) { "release" }

    val isWebEngineGecko: Boolean
        get() = webEngine == ENGINE_GECKO_VIEW

    val isWebEngineNotSet: Boolean
        get() = webEngineIndex == -1

    fun guessSearchEngineName(): String {
        return if (searchEngineIndex < SearchEnginesTitles.size - 1) {
            SearchEnginesTitles[searchEngineIndex].lowercase()
        } else {
            "custom"
        }
    }
}



enum class Theme {
    SYSTEM, WHITE, BLACK
}

enum class HomePageMode {
    HOME_PAGE, SEARCH_ENGINE, CUSTOM, BLANK
}

enum class HomePageLinksMode {
    BOOKMARKS, LATEST_HISTORY, MOST_VISITED
}

// Helper function (move to Utils ig)
fun canRecommendGeckoView(): Boolean {
    val deviceRAM = Utils.memInfo(
        AppContext.get()
    ).totalMem
    val cpuHas64Bit = Build.SUPPORTED_64_BIT_ABIS.isNotEmpty()
    val cpuCores = Runtime.getRuntime().availableProcessors()
    val threeGB = 3_000_000_000L
    return deviceRAM >= threeGB && cpuHas64Bit && cpuCores >= 6
}