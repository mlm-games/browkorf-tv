package org.mlm.browkorftv.ui.screens

import android.content.res.Resources
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.mlmgames.settings.core.actions.ActionRegistry
import io.github.mlmgames.settings.core.backup.ExportResult
import io.github.mlmgames.settings.core.backup.ImportResult
import io.github.mlmgames.settings.core.locale.AppLanguage
import io.github.mlmgames.settings.core.resources.AndroidStringResourceProvider
import io.github.mlmgames.settings.ui.AutoSettingsScreen
import io.github.mlmgames.settings.ui.ProvideStringResources
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import org.mlm.browkorftv.BuildConfig
import org.mlm.browkorftv.R
import org.mlm.browkorftv.network.ProxyLimitation
import org.mlm.browkorftv.network.ProxyResolution
import org.mlm.browkorftv.network.browsingLimitations
import org.mlm.browkorftv.network.resolveProxyConfig
import org.mlm.browkorftv.settings.applyAppLocale
import org.mlm.browkorftv.settings.currentAppLanguage
import org.mlm.browkorftv.settings.AppSettings
import org.mlm.browkorftv.settings.AppSettingsSchema
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
    val resources = LocalResources.current
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

    // A proxy the browser can't honour must not stay invisible (imported settings, restart)
    LaunchedEffect(Unit) {
        proxyWarning(settings, resources)?.let { snackbarHostState.showSnackbar(it) }
    }

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
                            checkNotNull(writer) { resources.getString(R.string.export_failed) }
                            writer.write(result.json)
                        }
                    }.onSuccess {
                        snackbarHostState.showSnackbar(resources.getString(R.string.backup_exported))
                    }.onFailure {
                        snackbarHostState.showSnackbar(
                            it.message ?: resources.getString(R.string.export_failed),
                        )
                    }
                }
                is ExportResult.Error -> snackbarHostState.showSnackbar(
                    resources.getString(R.string.export_failed),
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
                    checkNotNull(reader) { resources.getString(R.string.import_failed) }
                    reader.readText()
                }
            }.onSuccess { json ->
                importJsonContent = json
                showImportDialog = true
            }.onFailure {
                snackbarHostState.showSnackbar(
                    it.message ?: resources.getString(R.string.import_failed),
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
                    value = settings.copy(language = currentAppLanguage()),
                    modifier = Modifier.weight(1f),
                    snackbarHostState = snackbarHostState,
                    onSet = { name, value ->
                        scope.launch {
                            if (!BuildConfig.GECKO_INCLUDED && name == "webEngineIndex") {
                                snackbarHostState.showSnackbar(
                                    message = resources.getString(R.string.gecko_engine_not_included)
                                )
                                return@launch
                            }
                            settingsManager.set(name, value)
                            if (name == "language" && value is AppLanguage) {
                                applyAppLocale(value.languageTag)
                            }
                            if (name == "proxyEnabled" || name == "proxyUrl") {
                                val candidate = when (value) {
                                    is Boolean -> settings.copy(proxyEnabled = value)
                                    is String -> settings.copy(proxyUrl = value)
                                    else -> settings
                                }
                                proxyWarning(candidate, resources)?.let {
                                    snackbarHostState.showSnackbar(it)
                                }
                            }
                        }
                    },
                    onAction = { actionClass ->
                        if (!ActionRegistry.execute(actionClass)) {
                            snackbarHostState.showSnackbar(resources.getString(R.string.error))
                        }
                    }
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
                                        applyAppLocale(settingsManager.settings.first().language.languageTag)
                                        snackbarHostState.showSnackbar(
                                            resources.getString(R.string.imported_settings, result.appliedCount),
                                        )
                                    }
                                    is ImportResult.Error ->
                                        snackbarHostState.showSnackbar(resources.getString(R.string.import_failed))
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

private fun proxyWarning(settings: AppSettings, resources: Resources): String? =
    when (val resolution = resolveProxyConfig(settings)) {
        is ProxyResolution.Invalid -> resources.getString(R.string.setting_proxy_url_invalid)
        is ProxyResolution.Active -> when (resolution.config.browsingLimitations().firstOrNull()) {
            ProxyLimitation.SOCKS_FOR_BROWSING ->
                resources.getString(R.string.proxy_socks_browsing_unsupported)

            ProxyLimitation.CREDENTIALS_FOR_BROWSING ->
                resources.getString(R.string.proxy_credentials_browsing_unsupported)

            null -> null
        }

        ProxyResolution.Disabled -> null
    }
