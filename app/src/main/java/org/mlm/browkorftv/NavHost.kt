package org.mlm.browkorftv

import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
sealed interface AppKey : NavKey {
    @Serializable
    data object Browser : AppKey

    @Serializable
    data object History : AppKey

    @Serializable
    data object Downloads : AppKey

    @Serializable
    data object Favorites : AppKey

    @Serializable
    data object Settings : AppKey

    @Serializable
    data object Shortcuts : AppKey

    @Serializable
    data object About : AppKey

    @Serializable
    data class BookmarkEditor(val id: Long? = null) : AppKey
}

private fun NavBackStack<NavKey>.popOrClose(onCloseOverlay: () -> Unit) {
    if (size > 1) removeAt(lastIndex) else onCloseOverlay()
}