package com.phlox.tvwebbrowser.activity.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.phlox.tvwebbrowser.BuildConfig
import com.phlox.tvwebbrowser.settings.SettingsManager
import com.phlox.tvwebbrowser.utils.UpdateChecker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class UpdateState(
    val hasUpdate: Boolean = false,
    val latestVersion: String? = null,
    val changelog: String? = null,
    val error: String? = null
)

class UpdateViewModel(
    private val settingsManager: SettingsManager
) : ViewModel() {

    private val updateChecker = UpdateChecker(BuildConfig.VERSION_CODE)
    
    private val _updateState = MutableStateFlow(UpdateState())
    val updateState = _updateState.asStateFlow()

    private val _isChecking = MutableStateFlow(false)
    val isChecking = _isChecking.asStateFlow()

    fun checkForUpdates() = viewModelScope.launch(Dispatchers.IO) {
        _isChecking.value = true
        try {
            updateChecker.check(
                "https://raw.githubusercontent.com/truefedex/tv-bro/master/latest_version.json",
                arrayOf(settingsManager.current.updateChannel)
            )
            
            val result = updateChecker.versionCheckResult
            if (result != null && result.latestVersionCode > BuildConfig.VERSION_CODE) {
                _updateState.value = UpdateState(
                    hasUpdate = true,
                    latestVersion = result.latestVersionName,
                    changelog = result.changelog.joinToString("\n") { it.changes }
                )
            } else {
                _updateState.value = UpdateState(hasUpdate = false)
            }
        } catch (e: Exception) {
            _updateState.value = UpdateState(error = e.message)
        } finally {
            _isChecking.value = false
        }
    }
}