package com.phlox.tvwebbrowser.compose.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.phlox.tvwebbrowser.BuildConfig
import com.phlox.tvwebbrowser.core.DispatcherProvider
import com.phlox.tvwebbrowser.data.ApkDownloader
import com.phlox.tvwebbrowser.data.UpdateInfo
import com.phlox.tvwebbrowser.data.UpdateRepository
import com.phlox.tvwebbrowser.data.settings.ConfigRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

data class UpdateUiState(
    val builtInAutoUpdate: Boolean = true,
    val autoCheck: Boolean = false,
    val channel: String = "release",
    val availableChannels: List<String> = listOf("release", "beta"),
    val checking: Boolean = false,
    val error: String? = null,
    val latest: UpdateInfo? = null,
    val hasUpdate: Boolean = false,
    val downloading: Boolean = false,
    val progress: Int = 0,
    val downloadedApk: File? = null
)

class UpdateViewModel(
    private val configRepo: ConfigRepository,
    private val repo: UpdateRepository,
    private val downloader: ApkDownloader,
    private val dispatchers: DispatcherProvider
) : ViewModel() {

    private val _state = MutableStateFlow(UpdateUiState())
    val state: StateFlow<UpdateUiState> = _state.asStateFlow()

    fun bindPrefsOnce() {
        viewModelScope.launch(dispatchers.main) {
            val auto = withContext(dispatchers.io) { configRepo.autoCheckUpdates.first() }
            val ch = withContext(dispatchers.io) { configRepo.updateChannel.first() }
            _state.value = _state.value.copy(autoCheck = auto, channel = ch)
        }
    }

    fun setAutoCheck(enabled: Boolean) {
        viewModelScope.launch(dispatchers.io) { configRepo.setAutoCheckUpdates(enabled) }
        _state.value = _state.value.copy(autoCheck = enabled)
    }

    fun setChannel(channel: String) {
        viewModelScope.launch(dispatchers.io) { configRepo.setUpdateChannel(channel) }
        _state.value = _state.value.copy(channel = channel)
    }

    fun check() {
        if (!_state.value.builtInAutoUpdate) return
        if (_state.value.checking) return

        viewModelScope.launch(dispatchers.main) {
            _state.value = _state.value.copy(checking = true, error = null)
            val ch = _state.value.channel

            val result = runCatching {
                withContext(dispatchers.io) {
                    repo.check(
                        currentVersionCode = BuildConfig.VERSION_CODE,
                        channelsToCheck = setOf(ch)
                    )
                }
            }

            result.onSuccess { info ->
                _state.value = _state.value.copy(
                    checking = false,
                    latest = info,
                    hasUpdate = info.latestVersionCode > BuildConfig.VERSION_CODE,
                    availableChannels = info.availableChannels
                )
            }.onFailure { e ->
                _state.value = _state.value.copy(checking = false, error = e.toString())
            }
        }
    }

    fun downloadTo(file: File) {
        val info = _state.value.latest ?: return
        if (_state.value.downloading) return
        if (!_state.value.hasUpdate) return

        viewModelScope.launch(dispatchers.main) {
            _state.value = _state.value.copy(downloading = true, progress = 0, error = null, downloadedApk = null)

            runCatching {
                // best effort cleanup
                if (file.exists()) file.delete()

                downloader.download(info.downloadUrl, file) { p ->
                    _state.value = _state.value.copy(progress = p)
                }
            }.onSuccess {
                _state.value = _state.value.copy(downloading = false, downloadedApk = file, progress = 100)
            }.onFailure { e ->
                _state.value = _state.value.copy(downloading = false, error = e.toString())
            }
        }
    }
}