package org.mlm.browkorftv.settings

import org.mlm.browkorftv.R

internal fun resolveSettingsResource(key: String): Int = when (key) {
    BrowkorfSettingsKeys.CATEGORY_GENERAL -> R.string.category_general
    BrowkorfSettingsKeys.CATEGORY_HOME_PAGE -> R.string.category_home_page
    BrowkorfSettingsKeys.CATEGORY_SEARCH -> R.string.category_search
    BrowkorfSettingsKeys.CATEGORY_USER_AGENT -> R.string.category_user_agent
    BrowkorfSettingsKeys.CATEGORY_WEB_ENGINE -> R.string.category_web_engine
    BrowkorfSettingsKeys.CATEGORY_AD_BLOCK -> R.string.category_ad_block
    BrowkorfSettingsKeys.CATEGORY_UPDATES -> R.string.category_updates
    BrowkorfSettingsKeys.CATEGORY_PROXY -> R.string.category_proxy

    BrowkorfSettingsKeys.THEME -> R.string.theme
    BrowkorfSettingsKeys.LANGUAGE -> R.string.language
    BrowkorfSettingsKeys.FORCE_DARK_WEBPAGE -> R.string.setting_force_dark_webpage
    BrowkorfSettingsKeys.KEEP_SCREEN_ON -> R.string.setting_keep_screen_on
    BrowkorfSettingsKeys.INCOGNITO_MODE -> R.string.incognito_mode
    BrowkorfSettingsKeys.ALLOW_AUTOPLAY_MEDIA -> R.string.setting_allow_autoplay_media
    BrowkorfSettingsKeys.HOME_PAGE_MODE -> R.string.setting_home_page_mode
    BrowkorfSettingsKeys.CUSTOM_HOME_PAGE_URL -> R.string.setting_custom_home_page_url
    BrowkorfSettingsKeys.SEARCH_ENGINE -> R.string.setting_search_engine
    BrowkorfSettingsKeys.CUSTOM_SEARCH_ENGINE_URL -> R.string.setting_custom_search_engine_url
    BrowkorfSettingsKeys.USER_AGENT -> R.string.setting_user_agent
    BrowkorfSettingsKeys.WEB_ENGINE -> R.string.setting_web_engine
    BrowkorfSettingsKeys.AD_BLOCK_ENABLED -> R.string.setting_ad_block
    BrowkorfSettingsKeys.AD_BLOCK_LIST_URL -> R.string.setting_ad_block_list_url
    BrowkorfSettingsKeys.AUTO_CHECK_UPDATES -> R.string.setting_auto_check_updates
    BrowkorfSettingsKeys.UPDATE_CHANNEL -> R.string.setting_update_channel
    BrowkorfSettingsKeys.CHECK_FOR_UPDATES -> R.string.check_for_updates
    BrowkorfSettingsKeys.USE_HTTP_PROXY -> R.string.setting_use_http_proxy
    BrowkorfSettingsKeys.PROXY_URL -> R.string.setting_proxy_url
    BrowkorfSettingsKeys.DIRECTIONAL_NAVIGATION_MODE -> R.string.setting_directional_navigation_mode
    BrowkorfSettingsKeys.NEW_TAB_BUTTON_AT_START -> R.string.setting_new_tab_button_at_start
    BrowkorfSettingsKeys.CONTEXT_MENU_ON_LONG_PRESS -> R.string.setting_context_menu_on_long_press
    BrowkorfSettingsKeys.SINGLE_TAB_MODE -> R.string.setting_single_tab_mode

    BrowkorfSettingsKeys.FORCE_DARK_WEBPAGE_DESCRIPTION -> R.string.setting_force_dark_webpage_description
    BrowkorfSettingsKeys.KEEP_SCREEN_ON_DESCRIPTION -> R.string.setting_keep_screen_on_description
    BrowkorfSettingsKeys.INCOGNITO_MODE_DESCRIPTION -> R.string.setting_incognito_mode_description
    BrowkorfSettingsKeys.WEB_ENGINE_DESCRIPTION -> R.string.setting_web_engine_description
    BrowkorfSettingsKeys.CHECK_FOR_UPDATES_DESCRIPTION -> R.string.check_for_updates_description
    BrowkorfSettingsKeys.USE_HTTP_PROXY_DESCRIPTION -> R.string.setting_use_http_proxy_description
    BrowkorfSettingsKeys.PROXY_URL_DESCRIPTION -> R.string.setting_proxy_url_description
    BrowkorfSettingsKeys.DIRECTIONAL_NAVIGATION_MODE_DESCRIPTION -> R.string.setting_directional_navigation_mode_description
    BrowkorfSettingsKeys.NEW_TAB_BUTTON_AT_START_DESCRIPTION -> R.string.setting_new_tab_button_at_start_description
    BrowkorfSettingsKeys.CONTEXT_MENU_ON_LONG_PRESS_DESCRIPTION -> R.string.setting_context_menu_on_long_press_description
    BrowkorfSettingsKeys.SINGLE_TAB_MODE_DESCRIPTION -> R.string.setting_single_tab_mode_description

    BrowkorfSettingsKeys.THEME_OPTIONS -> R.array.themes
    BrowkorfSettingsKeys.HOME_PAGE_MODE_OPTIONS -> R.array.home_page_modes
    BrowkorfSettingsKeys.SEARCH_ENGINE_OPTIONS -> R.array.search_engine_options
    BrowkorfSettingsKeys.USER_AGENT_OPTIONS -> R.array.user_agent_options
    BrowkorfSettingsKeys.WEB_ENGINE_OPTIONS -> R.array.web_engine_options
    BrowkorfSettingsKeys.UPDATE_CHANNEL_OPTIONS -> R.array.update_channel_options

    BrowkorfSettingsKeys.WEB_ENGINE_CONFIRMATION_TITLE -> R.string.change_web_engine
    BrowkorfSettingsKeys.WEB_ENGINE_CONFIRMATION_MESSAGE -> R.string.need_restart_message
    else -> 0
}
