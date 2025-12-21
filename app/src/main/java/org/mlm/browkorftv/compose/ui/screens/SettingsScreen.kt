package org.mlm.browkorftv.compose.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.mlmgames.settings.core.resources.AndroidStringResourceProvider
import io.github.mlmgames.settings.ui.AutoSettingsScreen
import io.github.mlmgames.settings.ui.CategoryConfig
import io.github.mlmgames.settings.ui.ProvideStringResources
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import org.mlm.browkorftv.compose.ui.components.BrowkorfTopBar
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

    ProvideStringResources(AndroidStringResourceProvider(context)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 48.dp, vertical = 24.dp)
        ) {
            BrowkorfTopBar(title = "Settings", onBack = onNavigateBack)

            // Settings Content
            Box(modifier = Modifier.weight(1f)) {
                AutoSettingsScreen(
                    schema = AppSettingsSchema,
                    value = settings,
                    modifier = Modifier.fillMaxSize(),
                    onSet = { name, value ->
                        scope.launch {
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
    }
}