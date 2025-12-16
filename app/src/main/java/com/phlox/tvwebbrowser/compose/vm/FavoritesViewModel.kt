package com.phlox.tvwebbrowser.compose.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.phlox.tvwebbrowser.core.DispatcherProvider
import com.phlox.tvwebbrowser.data.FavoritesRepository
import com.phlox.tvwebbrowser.model.FavoriteItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class FavoritesUiState(
    val bookmarks: List<FavoriteItem> = emptyList(),
    val homePage: List<FavoriteItem> = emptyList(),
    val loading: Boolean = false
)

class FavoritesViewModel(
    private val repo: FavoritesRepository,
    private val dispatchers: DispatcherProvider
) : ViewModel() {

    private val _state = MutableStateFlow(FavoritesUiState())
    val state: StateFlow<FavoritesUiState> = _state.asStateFlow()

    fun load() {
        viewModelScope.launch(dispatchers.main) {
            _state.value = _state.value.copy(loading = true)
            val home = withContext(dispatchers.io) { repo.getAll(homePageBookmarks = true) }
            val bookmarks = withContext(dispatchers.io) { repo.getAll(homePageBookmarks = false) }

            _state.value = FavoritesUiState(
                bookmarks = bookmarks,
                homePage = home.sortedBy { it.order },
                loading = false
            )
        }
    }

    fun delete(item: FavoriteItem) {
        viewModelScope.launch(dispatchers.main) {
            withContext(dispatchers.io) { repo.delete(item) }
            load()
        }
    }
}