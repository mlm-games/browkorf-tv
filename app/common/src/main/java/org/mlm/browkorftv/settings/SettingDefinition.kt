package org.mlm.browkorftv.settings

import io.github.mlmgames.settings.core.annotations.*
import io.github.mlmgames.settings.core.resources.SettingsTextKeys
import io.github.mlmgames.settings.core.types.*
import androidx.core.net.toUri
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.Serializable

@CategoryDefinition(order = 0, titleKey = BrowkorfSettingsKeys.CATEGORY_GENERAL)
object General

@CategoryDefinition(order = 1, titleKey = BrowkorfSettingsKeys.CATEGORY_HOME_PAGE)
object HomePage

@CategoryDefinition(order = 2, titleKey = BrowkorfSettingsKeys.CATEGORY_SEARCH)
object Search

@CategoryDefinition(order = 3, titleKey = BrowkorfSettingsKeys.CATEGORY_USER_AGENT)
object UserAgent

@CategoryDefinition(order = 4, titleKey = BrowkorfSettingsKeys.CATEGORY_WEB_ENGINE)
object WebEngine

@CategoryDefinition(order = 5, titleKey = BrowkorfSettingsKeys.CATEGORY_AD_BLOCK)
object AdBlock

@CategoryDefinition(order = 6, titleKey = BrowkorfSettingsKeys.CATEGORY_UPDATES)
object Updates

@CategoryDefinition(order = 7, titleKey = BrowkorfSettingsKeys.CATEGORY_PROXY)
object Proxy

// Internal category - not shown in UI, just for grouping
@CategoryDefinition(order = 100)
object Internal

@OptIn(InternalSerializationApi::class)
@Serializable
data class BookmarkEntry(
    val id: Long = 0L,
    val title: String = "",
    val url: String = "",
    val parent: Long = 0L,
    val homePageBookmark: Boolean = false,
    val useful: Boolean = false,
    val sortOrder: Long = 0L,
)


@SchemaVersion(version = 1)
data class AppSettings(


    @Setting(
        title = "Theme",
        titleKey = BrowkorfSettingsKeys.THEME,
        category = General::class,
        type = Dropdown::class,
        key = "theme",
        options = ["System", "Light", "Dark"],
        optionsKey = BrowkorfSettingsKeys.THEME_OPTIONS
    )
    val theme: Theme = Theme.System,

    @Setting(
        title = "Language",
        titleKey = BrowkorfSettingsKeys.LANGUAGE,
        category = General::class,
        type = LanguageSetting::class,
        key = "language_index"
    )
    val languageIndex: Int = 0,

    @Setting(
        title = "Force Dark Webpage",
        titleKey = BrowkorfSettingsKeys.FORCE_DARK_WEBPAGE,
        description = "Apply dark theme to web pages when using dark mode",
        descriptionKey = BrowkorfSettingsKeys.FORCE_DARK_WEBPAGE_DESCRIPTION,
        category = General::class,
        type = Toggle::class,
        key = "force_dark_webpage"
    )
    val forceDarkWebpage: Boolean = true,

    @Setting(
        title = "Keep Screen On",
        titleKey = BrowkorfSettingsKeys.KEEP_SCREEN_ON,
        description = "Prevent screen from turning off while browsing",
        descriptionKey = BrowkorfSettingsKeys.KEEP_SCREEN_ON_DESCRIPTION,
        category = General::class,
        type = Toggle::class,
        key = "keep_screen_on"
    )
    val keepScreenOn: Boolean = false,

    @Setting(
        title = "Incognito Mode",
        titleKey = BrowkorfSettingsKeys.INCOGNITO_MODE,
        description = "Browse without saving history",
        descriptionKey = BrowkorfSettingsKeys.INCOGNITO_MODE_DESCRIPTION,
        category = General::class,
        type = Toggle::class,
        key = "incognito_mode"
    )
    val incognitoMode: Boolean = false,

    @Setting(
        title = "Allow Autoplay Media",
        titleKey = BrowkorfSettingsKeys.ALLOW_AUTOPLAY_MEDIA,
        category = General::class,
        type = Toggle::class,
        key = "allow_autoplay_media"
    )
    val allowAutoplayMedia: Boolean = false,


    @Setting(
        title = "Home Page Mode",
        titleKey = BrowkorfSettingsKeys.HOME_PAGE_MODE,
        category = HomePage::class,
        type = Dropdown::class,
        key = "home_page_mode",
        optionsKey = BrowkorfSettingsKeys.HOME_PAGE_MODE_OPTIONS
    )
    val homePageMode: HomePageMode = HomePageMode.HomePage,

    @Setting(
        title = "Custom Home Page URL",
        titleKey = BrowkorfSettingsKeys.CUSTOM_HOME_PAGE_URL,
        category = HomePage::class,
        type = TextInput::class,
        key = "home_page",
        dependsOn = "homePageMode" // Only relevant when mode is Custom
    )
    val homePage: String = HOME_URL_ALIAS,


    @Setting(
        title = "Search Engine",
        titleKey = BrowkorfSettingsKeys.SEARCH_ENGINE,
        category = Search::class,
        type = Dropdown::class,
        key = "search_engine_url",
        options = ["DuckDuckGo Lite", "Google", "Bing", "Yahoo!", "DuckDuckGo", "Yandex", "Startpage", "Custom"],
        optionsKey = BrowkorfSettingsKeys.SEARCH_ENGINE_OPTIONS
    )
    val searchEngineIndex: Int = 0,

    @Setting(
        title = "Custom Search Engine URL",
        titleKey = BrowkorfSettingsKeys.CUSTOM_SEARCH_ENGINE_URL,
        category = Search::class,
        type = TextInput::class,
        key = "search_engine_custom_url",
        dependsOn = "searchEngineIndex" // Only relevant when mode is Custom
    )
    val searchEngineCustomUrl: String = "",

    @Setting(
        title = "User Agent",
        titleKey = BrowkorfSettingsKeys.USER_AGENT,
        category = UserAgent::class,
        type = Dropdown::class,
        key = "user_agent_index",
        options = ["Default (recommended)", "Chrome (Desktop)", "Chrome (Mobile)",
            "Firefox (Desktop)", "Firefox (Mobile)", "Edge (Desktop)", "Custom"],
        optionsKey = BrowkorfSettingsKeys.USER_AGENT_OPTIONS
    )
    val userAgentIndex: Int = 0,

    @Persisted(key = "user_agent")
    val userAgentCustom: String? = null,


    @Setting(
        title = "Web Engine",
        titleKey = BrowkorfSettingsKeys.WEB_ENGINE,
        description = "GeckoView recommended for devices with 3GB+ RAM",
        descriptionKey = BrowkorfSettingsKeys.WEB_ENGINE_DESCRIPTION,
        category = WebEngine::class,
        type = Dropdown::class,
        key = "web_engine",
        options = ["GeckoView", "WebView"],
        optionsKey = BrowkorfSettingsKeys.WEB_ENGINE_OPTIONS
    )
    @RequiresConfirmation(
        title = "Change Web Engine?",
        titleKey = BrowkorfSettingsKeys.WEB_ENGINE_CONFIRMATION_TITLE,
        message = "You should restart to apply changes",
        messageKey = BrowkorfSettingsKeys.WEB_ENGINE_CONFIRMATION_MESSAGE,
        confirmTextKey = SettingsTextKeys.CONFIRM,
        cancelTextKey = SettingsTextKeys.CANCEL,
        isDangerous = true
    )
    val webEngineIndex: Int = -1, // -1 means not set, will use default


    @Setting(
        title = "Enable Ad Blocker",
        titleKey = BrowkorfSettingsKeys.AD_BLOCK_ENABLED,
        category = AdBlock::class,
        type = Toggle::class,
        key = "adblock_enabled"
    )
    val adBlockEnabled: Boolean = true,

    @Setting(
        title = "Ad Block List URL",
        titleKey = BrowkorfSettingsKeys.AD_BLOCK_LIST_URL,
        category = AdBlock::class,
        type = TextInput::class,
        key = "adblock_list_url",
        dependsOn = "adBlockEnabled"
    )
    val adBlockListURL: String = DEFAULT_ADBLOCK_LIST_URL,


    @Setting(
        title = "Auto Check Updates",
        titleKey = BrowkorfSettingsKeys.AUTO_CHECK_UPDATES,
        category = Updates::class,
        type = Toggle::class,
        key = "auto_check_updates"
    )
    val autoCheckUpdates: Boolean = true, // Will be overridden based on install source

    @Setting(
        title = "Update Channel",
        titleKey = BrowkorfSettingsKeys.UPDATE_CHANNEL,
        category = Updates::class,
        type = Dropdown::class,
        key = "update_channel",
        options = ["Release", "Prerelease"],
        optionsKey = BrowkorfSettingsKeys.UPDATE_CHANNEL_OPTIONS,
        dependsOn = "autoCheckUpdates"
    )
    val updateChannelIndex: Int = 0,

    @Setting(
        title = "Check for updates",
        titleKey = BrowkorfSettingsKeys.CHECK_FOR_UPDATES,
        description = "Check now for a newer version",
        descriptionKey = BrowkorfSettingsKeys.CHECK_FOR_UPDATES_DESCRIPTION,
        category = Updates::class,
        type = Button::class,
        platforms = [SettingPlatform.ANDROID]
    )
    @ActionHandler(CheckForUpdatesAction::class)
    val checkForUpdates: Unit = Unit,

    @Setting(
        title = "Use HTTP Proxy",
        titleKey = BrowkorfSettingsKeys.USE_HTTP_PROXY,
        description = "Route traffic through HTTP proxy (e.g. Clash on 127.0.0.1:7890). Restart may be required for WebView",
        descriptionKey = BrowkorfSettingsKeys.USE_HTTP_PROXY_DESCRIPTION,
        category = Proxy::class,
        type = Toggle::class,
        key = "proxy_enabled"
    )
    val proxyEnabled: Boolean = false,

    @Setting(
        title = "Proxy URL",
        titleKey = BrowkorfSettingsKeys.PROXY_URL,
        description = "http://127.0.0.1:7890 (http/socks5). Leave blank to disable",
        descriptionKey = BrowkorfSettingsKeys.PROXY_URL_DESCRIPTION,
        category = Proxy::class,
        type = TextInput::class,
        key = "proxy_url",
        dependsOn = "proxyEnabled"
    )
    val proxyUrl: String = "http://127.0.0.1:7890",

    @Setting(
        title = "Directional Navigation Mode",
        titleKey = BrowkorfSettingsKeys.DIRECTIONAL_NAVIGATION_MODE,
        description = "Send arrow keys to webpage instead of moving cursor (for games/apps)",
        descriptionKey = BrowkorfSettingsKeys.DIRECTIONAL_NAVIGATION_MODE_DESCRIPTION,
        category = General::class,
        type = Toggle::class,
        key = "directional_nav_mode"
    )
    val directionalNavMode: Boolean = false,

    @Setting(
        title = "New Tab Button At The Start",
        titleKey = BrowkorfSettingsKeys.NEW_TAB_BUTTON_AT_START,
        description = "Places the + button before the tab list instead of after it",
        descriptionKey = BrowkorfSettingsKeys.NEW_TAB_BUTTON_AT_START_DESCRIPTION,
        category = General::class,
        type = Toggle::class,
        key = "new_tab_button_before_tabs"
    )
    val newTabButtonBeforeTabs: Boolean = false,

    @Setting(
        title = "Context menu on long press",
        titleKey = BrowkorfSettingsKeys.CONTEXT_MENU_ON_LONG_PRESS,
        description = "disable to allow page actions on long press",
        descriptionKey = BrowkorfSettingsKeys.CONTEXT_MENU_ON_LONG_PRESS_DESCRIPTION,
        category = General::class,
        type = Toggle::class,
        key = "show_context_menu_on_long_press"
    )
    val showContextMenuOnLongPress: Boolean = true,

    @Setting(
        title = "Single Tab Mode",
        titleKey = BrowkorfSettingsKeys.SINGLE_TAB_MODE,
        description = "Reuses the current tab for every link or intent",
        descriptionKey = BrowkorfSettingsKeys.SINGLE_TAB_MODE_DESCRIPTION,
        category = General::class,
        type = Toggle::class,
        key = "single_tab_mode"
    )
    val singleTabMode: Boolean = false,


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

    @Persisted(key = "bookmarks")
    val bookmarks: List<BookmarkEntry> = emptyList(),

    @Persisted(key = "__bookmarks_migrated_from_room__")
    @NoReset
    val bookmarksMigratedFromRoom: Boolean = false,

    // Migration helper - tracks if we migrated from SharedPreferences
    @Persisted(key = "__migrated_from_shared_prefs__")
    @NoReset
    val migratedFromSharedPrefs: Boolean = false,
) {
    companion object {
        const val HOME_URL_ALIAS = "about:home"
        const val DEFAULT_ADBLOCK_LIST_URL = "https://easylist.to/easylist/easylist.txt"
        const val HOME_PAGE_URL =
            "https://lite.duckduckgo.com/lite" // is fine for now (until the startpage is good enough to use ig)

        const val ENGINE_GECKO_VIEW = "GeckoView"
        const val ENGINE_WEB_VIEW = "WebView"

        val SearchEnginesTitles =
            arrayOf(
                "DuckDuckGo Lite",
                "Google",
                "Bing",
                "Yahoo!",
                "DuckDuckGo",
                "Yandex",
                "Startpage",
                "Custom"
            )
        val SearchEnginesURLs = listOf(
            "https://lite.duckduckgo.com/lite?q=[query]",
            "https://www.google.com/search?q=[query]",
            "https://www.bing.com/search?q=[query]",
            "https://search.yahoo.com/search?p=[query]",
            "https://duckduckgo.com/?q=[query]",
            "https://yandex.com/search/?text=[query]",
            "https://www.startpage.com/sp/search?query=[query]",
            "" // Custom
        )

        val SearchEnginesHomeURLs = listOf(
            "https://lite.duckduckgo.com/lite",
            "https://www.google.com/",
            "https://www.bing.com/",
            "https://search.yahoo.com/",
            "https://duckduckgo.com/",
            "https://yandex.com/",
            "https://www.startpage.com/",
            ""
        )

        fun searchEngineHomeUrl(index: Int, customSearchUrl: String): String {
            if (index in 0 until SearchEnginesHomeURLs.lastIndex) return SearchEnginesHomeURLs[index]

            val raw = customSearchUrl.trim()
            if (raw.isBlank()) return HOME_URL_ALIAS

            return try {
                val u = raw.replace("[query]", "").toUri()
                val scheme = u.scheme ?: "https"
                val auth = u.encodedAuthority ?: return HOME_URL_ALIAS
                "$scheme://$auth/"
            } catch (e: Throwable) {
                e.printStackTrace()
                HOME_URL_ALIAS
            }
        }

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
    }

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
            ENGINE_WEB_VIEW
        }

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


object CheckForUpdatesAction : SettingAction

class LanguageSetting

enum class Theme {
    System, White, Black
}

enum class HomePageMode {
    HomePage, SearchEngine, Custom, Blank, Bookmarks
}
