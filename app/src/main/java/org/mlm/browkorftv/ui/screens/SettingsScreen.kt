package org.mlm.browkorftv.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.mlmgames.settings.core.SettingField
import io.github.mlmgames.settings.core.backup.ExportResult
import io.github.mlmgames.settings.core.backup.ImportResult
import io.github.mlmgames.settings.core.resources.AndroidStringResourceProvider
import io.github.mlmgames.settings.ui.AutoSettingsScreen
import io.github.mlmgames.settings.ui.CustomTypeHandler
import io.github.mlmgames.settings.ui.ProvideStringResources
import io.github.mlmgames.settings.ui.components.SettingsItem
import io.github.mlmgames.settings.ui.dialogs.DropdownSettingDialog
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import org.mlm.browkorftv.BuildConfig
import org.mlm.browkorftv.R
import org.mlm.browkorftv.settings.AppLanguage
import org.mlm.browkorftv.settings.AppSettings
import org.mlm.browkorftv.settings.AppSettingsSchema
import org.mlm.browkorftv.settings.LanguageSetting
import org.mlm.browkorftv.settings.SettingsManager
import org.mlm.browkorftv.settings.resolveSettingsResource
import org.mlm.browkorftv.ui.components.BrowkorfTopBar
import org.mlm.browkorftv.ui.components.BrowkorfTvIconButton

@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToShortcuts: () -> Unit = {}
) {
    val context = LocalContext.current
    val settingsManager: SettingsManager = koinInject()
    val settings by settingsManager.settingsState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val settingsStringProvider = remember(context) {
        AndroidStringResourceProvider(context, ::resolveSettingsResource)
    }

    // Backup/Restore Logic
    var showImportDialog by remember { mutableStateOf(false) }
    var importJsonContent by remember { mutableStateOf<String?>(null) }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val backupManager = settingsManager.backupManager
        scope.launch {
            when (val result = backupManager.export()) {
                is ExportResult.Success -> {
                    runCatching {
                        context.contentResolver.openOutputStream(uri)?.bufferedWriter().use { writer ->
                            checkNotNull(writer) { context.getString(R.string.export_failed) }
                            writer.write(result.json)
                        }
                    }.onSuccess {
                        snackbarHostState.showSnackbar(context.getString(R.string.backup_exported))
                    }.onFailure {
                        snackbarHostState.showSnackbar(
                            it.message ?: context.getString(R.string.export_failed),
                        )
                    }
                }
                is ExportResult.Error -> snackbarHostState.showSnackbar(
                    context.getString(R.string.export_failed),
                )
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            runCatching {
                context.contentResolver.openInputStream(uri)?.bufferedReader().use { reader ->
                    checkNotNull(reader) { context.getString(R.string.import_failed) }
                    reader.readText()
                }
            }.onSuccess { json ->
                importJsonContent = json
                showImportDialog = true
            }.onFailure {
                snackbarHostState.showSnackbar(
                    it.message ?: context.getString(R.string.import_failed),
                )
            }
        }
    }
    // ---

    ProvideStringResources(settingsStringProvider) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 48.dp, vertical = 24.dp)
            ) {
                BrowkorfTopBar(
                    title = stringResource(R.string.browkorf_settings),
                    onBack = onNavigateBack,
                    actions = {
                        BrowkorfTvIconButton(
                            onClick = { importLauncher.launch(arrayOf("application/json", "text/plain")) },
                            contentDescription = stringResource(R.string.browkorf_settings_import),
                            painter = painterResource(R.drawable.outline_source_notes_24),
                            modifier = Modifier.padding(2.dp)
                        )

                        BrowkorfTvIconButton(
                            onClick = { exportLauncher.launch("browkorf-tv-backup.json") },
                            contentDescription = stringResource(R.string.browkorf_settings_export),
                            painter = painterResource(R.drawable.outline_export_notes_24),
                            modifier = Modifier.padding(2.dp)
                        )

                        BrowkorfTvIconButton(
                            onClick = onNavigateToShortcuts,
                            contentDescription = stringResource(R.string.shortcuts),
                            painter = painterResource(R.drawable.outline_remote_gen_24),
                            modifier = Modifier.padding(2.dp)
                        )
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Settings Content
                AutoSettingsScreen(
                    schema = AppSettingsSchema,
                    value = settings.copy(languageIndex = AppLanguage.current().ordinal),
                    modifier = Modifier.weight(1f),
                    snackbarHostState = snackbarHostState,
                    onSet = { name, value ->
                        scope.launch {
                            if (!BuildConfig.GECKO_INCLUDED && name == "webEngineIndex") {
                                snackbarHostState.showSnackbar(
                                    message = context.getString(R.string.gecko_engine_not_included)
                                )
                                return@launch
                            }
                            settingsManager.set(name, value)
                            if (name == "languageIndex" && value is Int) {
                                AppLanguage.select(AppLanguage.entries.getOrElse(value) { AppLanguage.System })
                            }
                        }
                    },
                    customTypeHandlers = listOf(
                        CustomTypeHandler(
                            typeClass = LanguageSetting::class,
                            render = { field, _, value, enabled, onSet ->
                                val selectedIndex = getLanguageIndex(field, value)
                                    .coerceIn(0, AppLanguage.entries.lastIndex)
                                val systemDefault = stringResource(R.string.language_system_default)
                                var showDialog by remember(field.name) { mutableStateOf(false) }

                                SettingsItem(
                                    title = stringResource(R.string.language),
                                    subtitle = AppLanguage.entries.getOrElse(selectedIndex) {
                                        AppLanguage.System
                                    }.displayName ?: systemDefault,
                                    enabled = enabled,
                                    onClick = { showDialog = true }
                                )

                                if (showDialog) {
                                    DropdownSettingDialog(
                                        title = stringResource(R.string.language),
                                        options = AppLanguage.entries.map { it.displayName ?: systemDefault },
                                        selectedIndex = selectedIndex,
                                        onDismiss = { showDialog = false },
                                        onOptionSelected = { index ->
                                            onSet(field.name, index)
                                            showDialog = false
                                        }
                                    )
                                }
                            }
                        )
                    )
                )
            }

            if (showImportDialog) {
                importJsonContent?.let { json ->
                    io.github.mlmgames.settings.ui.dialogs.ImportSettingsDialog(
                        backupManager = settingsManager.backupManager,
                        jsonContent = json,
                        onImportComplete = { result ->
                            showImportDialog = false
                            importJsonContent = null
                            scope.launch {
                                when (result) {
                                    is ImportResult.Success -> {
                                        val languageIndex = settingsManager.settings.first().languageIndex
                                        AppLanguage.select(AppLanguage.entries.getOrElse(languageIndex) { AppLanguage.System })
                                        snackbarHostState.showSnackbar(
                                            context.getString(R.string.imported_settings, result.appliedCount),
                                        )
                                    }
                                    is ImportResult.Error ->
                                        snackbarHostState.showSnackbar(context.getString(R.string.import_failed))
                                }
                            }
                        },
                        onDismiss = {
                            showImportDialog = false
                            importJsonContent = null
                        }
                    )
                }
            }

            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp)
            )
        }
    }
}

@Suppress("UNCHECKED_CAST")
private fun getLanguageIndex(field: SettingField<AppSettings, *>, value: AppSettings): Int =
    (field as SettingField<AppSettings, Int>).get(value)
