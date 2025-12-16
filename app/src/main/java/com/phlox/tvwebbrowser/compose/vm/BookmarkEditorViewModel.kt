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

data class BookmarkEditorState(
    val id: Long? = null,
    val title: String = "",
    val url: String = "",
    val loading: Boolean = false
)

class BookmarkEditorViewModel(
    private val repo: FavoritesRepository,
    private val dispatchers: DispatcherProvider
) : ViewModel() {

    private val _state = MutableStateFlow(BookmarkEditorState())
    val state: StateFlow<BookmarkEditorState> = _state.asStateFlow()

    fun load(id: Long?) {
        viewModelScope.launch(dispatchers.main) {
            if (id == null) {
                _state.value = BookmarkEditorState(id = null, title = "", url = "", loading = false)
                return@launch
            }
            _state.value = BookmarkEditorState(id = id, loading = true)
            val item = withContext(dispatchers.io) { repo.getById(id) }
            _state.value = BookmarkEditorState(
                id = id,
                title = item?.title.orEmpty(),
                url = item?.url.orEmpty(),
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
                if (url.isBlank()) return@withContext

                val normalized = normalizeUrl(url)
                val item = FavoriteItem().apply {
                    id = s.id ?: 0L
                    title = s.title.trim().ifBlank { normalized }
                    this.url = normalized
                    parent = 0
                    homePageBookmark = false
                }

                if (item.id == 0L) repo.insert(item) else repo.update(item)
            }
            onDone()
        }
    }

    fun delete(onDone: () -> Unit) {
        val id = _state.value.id ?: run { onDone(); return }
        viewModelScope.launch(dispatchers.main) {
            withContext(dispatchers.io) { repo.delete(id) }
            onDone()
        }
    }

    private fun normalizeUrl(url: String): String {
        return if (url.matches(Regex("^[A-Za-z][A-Za-z0-9+.-]*://.*$"))) url else "https://$url"
    }
}