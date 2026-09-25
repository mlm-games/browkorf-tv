package org.mlm.browkorftv.updates

import androidx.lifecycle.ViewModel
import io.github.mlmgames.settings.core.actions.ActionRegistry
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.mlm.browkorftv.BuildConfig
import org.mlm.browkorftv.settings.AppSettings
import org.mlm.browkorftv.settings.CheckForUpdatesAction
import org.mlm.browkorftv.settings.SettingsManager
import org.mlm.browkorftv.utils.sameDay
import android.os.SystemClock
import java.io.File
import java.util.Calendar
import java.util.concurrent.atomic.AtomicBoolean

data class UpdatesUiState(
    val isChecking: Boolean = false,
    val lastResult: UpdateResult? = null,
    val isDownloading: Boolean = false,
    val downloadProgress: DownloadProgress? = null,
    val availableUpdate: UpdateInfo? = null,
    val pendingInstallApk: File? = null,
    val errorMessage: String? = null
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
    private companion object {
        const val ERROR_RETRY_DELAY_MS = 30L * 60L * 1000L
    }

    private val _state = MutableStateFlow(UpdatesUiState())
    val state: StateFlow<UpdatesUiState> = _state.asStateFlow()

    private val checkInProgress = AtomicBoolean(false)
    private val downloadInProgress = AtomicBoolean(false)
    private var downloadJob: Job? = null
    private var lastCheckFailureElapsed = 0L

    init {
        ActionRegistry.registerAction(CheckForUpdatesAction::class, CheckForUpdatesAction)
        viewModelScope.launch {
            if (!BuildConfig.BUILT_IN_AUTO_UPDATE) return@launch
            try {
                installer.recoverPendingUpdate()?.let { file ->
                    _state.update {
                        if (it.pendingInstallApk == null && !it.isDownloading) {
                            it.copy(pendingInstallApk = file)
                        } else {
                            it
                        }
                    }
                }
            } catch (t: CancellationException) {
                throw t
            } catch (_: Throwable) {
            }
        }
    }

    fun checkAutoIfNeeded() {
        viewModelScope.launch {
            try {
                val settings = settingsManager.settings.first()
                if (!BuildConfig.BUILT_IN_AUTO_UPDATE || !settings.autoCheckUpdates) return@launch
                val now = SystemClock.elapsedRealtime()
                if (lastCheckFailureElapsed > 0L &&
                    now - lastCheckFailureElapsed < ERROR_RETRY_DELAY_MS
                ) {
                    return@launch
                }
                check(settings, force = false)
            } catch (t: CancellationException) {
                throw t
            } catch (t: Throwable) {
                lastCheckFailureElapsed = SystemClock.elapsedRealtime()
                showError("Update check failed", t)
            }
        }
    }

    fun checkManual(force: Boolean = true) {
        if (!BuildConfig.BUILT_IN_AUTO_UPDATE) return
        viewModelScope.launch {
            try {
                check(settingsManager.settings.first(), force)
            } catch (t: CancellationException) {
                throw t
            } catch (t: Throwable) {
                lastCheckFailureElapsed = SystemClock.elapsedRealtime()
                showError("Update check failed", t)
            }
        }
    }

    private suspend fun check(settings: AppSettings, force: Boolean) {
        if (!checkInProgress.compareAndSet(false, true)) return

        try {
            if (!force && wasCheckedToday(settings)) return
            _state.update {
                it.copy(
                    isChecking = true,
                    lastResult = null,
                    availableUpdate = null,
                    errorMessage = null
                )
            }

            val result = repository.checkForUpdates(
                currentVersionName = BuildConfig.VERSION_NAME,
                prerelease = settings.updateChannelIndex != 0,
                variantName = BuildConfig.FLAVOR
            )

            when (result) {
                is UpdateResult.Error -> {
                    lastCheckFailureElapsed = SystemClock.elapsedRealtime()
                    _state.update {
                        it.copy(
                            isChecking = false,
                            lastResult = result,
                            errorMessage = errorText("Update check failed", result.throwable)
                        )
                    }
                }

                UpdateResult.NoUpdate -> {
                    lastCheckFailureElapsed = 0L
                    val timestampError = saveCheckTimestamp()
                    _state.update {
                        it.copy(
                            isChecking = false,
                            lastResult = result,
                            availableUpdate = null,
                            errorMessage = timestampError
                        )
                    }
                }

                is UpdateResult.HasUpdate -> {
                    lastCheckFailureElapsed = 0L
                    val timestampError = saveCheckTimestamp()
                    _state.update {
                        it.copy(
                            isChecking = false,
                            lastResult = result,
                            availableUpdate = result.info,
                            errorMessage = timestampError
                        )
                    }
                }
            }
        } catch (t: CancellationException) {
            throw t
        } catch (t: Throwable) {
            lastCheckFailureElapsed = SystemClock.elapsedRealtime()
            _state.update {
                it.copy(
                    isChecking = false,
                    errorMessage = errorText("Update check failed", t)
                )
            }
        } finally {
            _state.update { state ->
                if (state.isChecking) state.copy(isChecking = false) else state
            }
            checkInProgress.set(false)
        }
    }

    fun downloadAndRequestInstall(info: UpdateInfo) {
        if (_state.value.pendingInstallApk != null) return
        if (!downloadInProgress.compareAndSet(false, true)) return

        val job: Job = viewModelScope.launch {
            var completed = false
            var cancelled = false
            _state.update {
                it.copy(
                    isDownloading = true,
                    downloadProgress = null,
                    availableUpdate = null,
                    errorMessage = null
                )
            }

            try {
                installer.downloadApk(info).collect { event ->
                    when (event) {
                        is UpdateDownloadEvent.Progress -> {
                            _state.update { it.copy(downloadProgress = event.progress) }
                        }

                        is UpdateDownloadEvent.Completed -> {
                            try {
                                installer.persistPendingUpdate(event.file)
                            } catch (t: CancellationException) {
                                throw t
                            } catch (_: Throwable) {
                            }
                            completed = true
                            _state.update {
                                it.copy(
                                    isDownloading = false,
                                    downloadProgress = event.progress,
                                    pendingInstallApk = event.file,
                                    availableUpdate = null,
                                    errorMessage = null
                                )
                            }
                        }
                    }
                }
            } catch (t: CancellationException) {
                cancelled = true
                throw t
            } catch (t: Throwable) {
                _state.update {
                    it.copy(
                        isDownloading = false,
                        availableUpdate = info,
                        errorMessage = errorText("Download failed", t)
                    )
                }
            } finally {
                if (!completed && _state.value.isDownloading) {
                    _state.update {
                        it.copy(
                            isDownloading = false,
                            availableUpdate = if (cancelled) null else info,
                            errorMessage = if (cancelled) null else "Download ended unexpectedly"
                        )
                    }
                }
            }
        }
        downloadJob = job
        job.invokeOnCompletion {
            downloadJob = null
            downloadInProgress.set(false)
        }
    }

    fun cancelDownload() {
        downloadJob?.cancel()
    }

    fun clearAvailableUpdate(info: UpdateInfo? = null) {
        _state.update {
            if (info == null || it.availableUpdate == info) {
                it.copy(availableUpdate = null)
            } else {
                it
            }
        }
    }

    fun restorePendingInstall(file: File) {
        if (!BuildConfig.BUILT_IN_AUTO_UPDATE || !file.isFile) return
        _state.update {
            if (it.pendingInstallApk == null) it.copy(pendingInstallApk = file) else it
        }
    }

    fun clearPendingInstall(file: File? = null) {
        var shouldClear = false
        _state.update {
            if (file == null || it.pendingInstallApk?.absolutePath == file.absolutePath) {
                shouldClear = true
                it.copy(pendingInstallApk = null)
            } else {
                it
            }
        }
        if (shouldClear) {
            viewModelScope.launch {
                try {
                    installer.clearPendingUpdate(file)
                } catch (t: CancellationException) {
                    throw t
                } catch (_: Throwable) {
                }
            }
        }
    }

    fun clearError(message: String? = null) {
        _state.update {
            if (message == null || it.errorMessage == message) {
                it.copy(errorMessage = null)
            } else {
                it
            }
        }
    }

    private suspend fun saveCheckTimestamp(): String? {
        return try {
            settingsManager.update {
                it.copy(lastUpdateUserNotificationTime = Calendar.getInstance().timeInMillis)
            }
            null
        } catch (t: CancellationException) {
            throw t
        } catch (t: Throwable) {
            errorText("Could not save update check time", t)
        }
    }

    private fun wasCheckedToday(settings: AppSettings): Boolean {
        val now = Calendar.getInstance()
        val last = Calendar.getInstance().apply {
            timeInMillis = settings.lastUpdateUserNotificationTime
        }
        return last.timeInMillis > 0L && last.sameDay(now)
    }

    private fun showError(prefix: String, throwable: Throwable) {
        _state.update {
            it.copy(errorMessage = errorText(prefix, throwable))
        }
    }

    private fun errorText(prefix: String, throwable: Throwable): String {
        val detail = throwable.message?.takeIf { it.isNotBlank() }
            ?: throwable.javaClass.simpleName
        return "$prefix: $detail"
    }
}
