package com.phlox.tvwebbrowser.compose.ui.nav

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

sealed interface AppKey : NavKey {

    @Serializable data object Browser : AppKey
    @Serializable data object Downloads : AppKey
    @Serializable data object History : AppKey
    @Serializable data object Settings : AppKey

    @Serializable data object Favorites : AppKey
    @Serializable data class HomePageSlotEditor(val order: Int) : AppKey
    @Serializable data class BookmarkEditor(val id: Long? = null) : AppKey

    @Serializable data object Shortcuts : AppKey
    @Serializable data object About : AppKey
}