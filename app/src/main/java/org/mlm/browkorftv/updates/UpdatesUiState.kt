package org.mlm.browkorftv.updates

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.mlm.browkorftv.BuildConfig
import org.mlm.browkorftv.settings.SettingsManager
import org.mlm.browkorftv.utils.sameDay
import java.io.File
import java.util.Calendar

data class UpdatesUiState(
    val isChecking: Boolean = false,
    val lastResult: UpdateResult? = null,
    val isDownloading: Boolean = false,
    val downloadProgress: DownloadProgress? = null
)

sealed interface UpdatesEvent {
    data class ShowUpdateAvailable(val info: UpdateInfo) : UpdatesEvent
    data class RequestInstallApk(val file: File) : UpdatesEvent
    data class ToastMessage(val message: String) : UpdatesEvent
}

class UpdatesViewModel(
    private val settingsManager: SettingsManager,
    private val repository: UpdateRepository,
    private val installer: UpdateInstaller
) : ViewModel() {

    private val _state = MutableStateFlow(UpdatesUiState())
    val state: StateFlow<UpdatesUiState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<UpdatesEvent>(extraBufferCapacity = 16)
    val events: SharedFlow<UpdatesEvent> = _events.asSharedFlow()

    private var lastCheckedDay: Calendar? = null

    val needAutoCheckUpdates: Boolean
        get() = settingsManager.current.autoCheckUpdates && BuildConfig.BUILT_IN_AUTO_UPDATE

    fun checkAutoIfNeeded() {
        if (!needAutoCheckUpdates) return

        val now = Calendar.getInstance()
        val last = Calendar.getInstance().apply { timeInMillis = settingsManager.current.lastUpdateUserNotificationTime }

        if (last.timeInMillis > 0 && last.sameDay(now)) return

        check(forced = false, emitDialogEventIfUpdate = true)
    }

    fun checkManual(force: Boolean = true) {
        check(forced = force, emitDialogEventIfUpdate = true)
    }

    private fun check(forced: Boolean, emitDialogEventIfUpdate: Boolean) {
        viewModelScope.launch {
            if (_state.value.isChecking) return@launch
            _state.update { it.copy(isChecking = true, lastResult = null) }

            val result = repository.checkForUpdates(
                currentVersionName = BuildConfig.VERSION_NAME,
                prerelease = settingsManager.current.updateChannelIndex != 0
            )

            settingsManager.update { it.copy(lastUpdateUserNotificationTime = Calendar.getInstance().timeInMillis) }

            _state.update { it.copy(isChecking = false, lastResult = result) }

            if (emitDialogEventIfUpdate && result is UpdateResult.HasUpdate) {
                _events.emit(UpdatesEvent.ShowUpdateAvailable(result.info))
            }
        }
    }

    fun downloadAndRequestInstall(info: UpdateInfo) {
        viewModelScope.launch {
            if (_state.value.isDownloading) return@launch

            _state.update { it.copy(isDownloading = true, downloadProgress = null) }

            try {
                installer.downloadApk(info.downloadUrl)
                    .collect { (progress, fileOrNull) ->
                        _state.update { it.copy(downloadProgress = progress) }
                        if (progress.done && fileOrNull != null) {
                            _events.emit(UpdatesEvent.RequestInstallApk(fileOrNull))
                        }
                    }
            } catch (t: Throwable) {
                _events.emit(UpdatesEvent.ToastMessage("Download failed: ${t.message ?: t.javaClass.simpleName}"))
            } finally {
                _state.update { it.copy(isDownloading = false) }
            }
        }
    }
}
