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

data class HomeSlotState(
    val order: Int,
    val existing: FavoriteItem? = null,
    val title: String = "",
    val url: String = "",
    val loading: Boolean = false
)

class HomePageSlotEditorViewModel(
    private val repo: FavoritesRepository,
    private val dispatchers: DispatcherProvider
) : ViewModel() {

    private val _state = MutableStateFlow(HomeSlotState(order = 0))
    val state: StateFlow<HomeSlotState> = _state.asStateFlow()

    fun load(order: Int) {
        viewModelScope.launch(dispatchers.main) {
            _state.value = HomeSlotState(order = order, loading = true)
            val home = withContext(dispatchers.io) { repo.getAll(homePageBookmarks = true) }
            val existing = home.firstOrNull { it.order == order }
            _state.value = HomeSlotState(
                order = order,
                existing = existing,
                title = existing?.title.orEmpty(),
                url = existing?.url.orEmpty(),
                loading = false
            )
        }
    }

    fun setTitle(v: String) { _state.value = _state.value.copy(title = v) }
    fun setUrl(v: String) { _state.value = _state.value.copy(url = v) }

    fun save(onDone: () -> Unit) {
        val s = _state.value
        viewModelScope.launch(dispatchers.main) {
            withContext(dispatchers.io) {
                val url = s.url.trim()
                if (url.isBlank()) {
                    s.existing?.let { repo.delete(it) }
                    return@withContext
                }

                val item = (s.existing ?: FavoriteItem()).apply {
                    title = s.title.trim().ifBlank { url }
                    this.url = normalizeUrl(url)
                    homePageBookmark = true
                    order = s.order
                    parent = 0
                }

                if (item.id == 0L) item.id = repo.insert(item) else repo.update(item)
            }
            onDone()
        }
    }

    fun deleteSlot(onDone: () -> Unit) {
        val ex = _state.value.existing ?: run { onDone(); return }
        viewModelScope.launch(dispatchers.main) {
            withContext(dispatchers.io) { repo.delete(ex) }
            onDone()
        }
    }

    private fun normalizeUrl(url: String): String {
        return if (url.matches(Regex("^[A-Za-z][A-Za-z0-9+.-]*://.*$"))) url else "https://$url"
    }
}