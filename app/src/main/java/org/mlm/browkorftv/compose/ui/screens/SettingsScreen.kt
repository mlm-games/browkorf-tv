package org.mlm.browkorftv.compose.ui.screens

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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.Text
import io.github.mlmgames.settings.core.resources.AndroidStringResourceProvider
import io.github.mlmgames.settings.ui.AutoSettingsScreen
import io.github.mlmgames.settings.ui.CategoryConfig
import io.github.mlmgames.settings.ui.ProvideStringResources
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import org.mlm.browkorftv.BuildConfig
import org.mlm.browkorftv.compose.ui.components.BrowkorfTopBar
import org.mlm.browkorftv.compose.ui.theme.AppTheme
import org.mlm.browkorftv.settings.AdBlock
import org.mlm.browkorftv.settings.AppSettingsSchema
import org.mlm.browkorftv.settings.General
import org.mlm.browkorftv.settings.HomePage
import org.mlm.browkorftv.settings.Search
import org.mlm.browkorftv.settings.SettingsManager
import org.mlm.browkorftv.settings.Updates
import org.mlm.browkorftv.settings.UserAgent
import org.mlm.browkorftv.settings.WebEngine

@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val settingsManager: SettingsManager = koinInject()
    val settings by settingsManager.settingsState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    val snackbarHostState = remember { SnackbarHostState() }

    ProvideStringResources(AndroidStringResourceProvider(context)) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 48.dp, vertical = 24.dp)
            ) {
                BrowkorfTopBar(title = "Settings", onBack = onNavigateBack)

                // Settings Content
                Box(modifier = Modifier.weight(1f)) {
                    if (!BuildConfig.GECKO_INCLUDED) {
                        Text(
                            "GeckoView engine is not included in the FOSS build.",
                            color = AppTheme.colors.textSecondary
                        )
                        Spacer(Modifier.height(12.dp))
                    }
                    AutoSettingsScreen(
                        schema = AppSettingsSchema,
                        value = settings,
                        modifier = Modifier.fillMaxSize(),
                        snackbarHostState = snackbarHostState,
                        onSet = { name, value ->
                            scope.launch {
                                if (!BuildConfig.GECKO_INCLUDED && name == "webEngineIndex") {
                                    snackbarHostState.showSnackbar(
                                        message = "GeckoView engine is not included in this build."
                                    )
                                    return@launch
                                }
                                settingsManager.set(name, value)
                            }
                        },
                        categoryConfigs = listOf(
                            CategoryConfig(General::class, "General"),
                            CategoryConfig(HomePage::class, "Home Page"),
                            CategoryConfig(Search::class, "Search"),
                            CategoryConfig(UserAgent::class, "User Agent"),
                            CategoryConfig(WebEngine::class, "Web Engine"),
                            CategoryConfig(AdBlock::class, "Ad Blocker"),
                            CategoryConfig(Updates::class, "Updates"),
                        )
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