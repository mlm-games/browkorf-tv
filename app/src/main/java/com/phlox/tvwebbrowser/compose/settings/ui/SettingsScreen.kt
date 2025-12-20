package com.phlox.tvwebbrowser.compose.settings.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.tv.material3.*
import com.phlox.tvwebbrowser.AppContext
import com.phlox.tvwebbrowser.settings.AdBlock
import com.phlox.tvwebbrowser.settings.AppSettings
import com.phlox.tvwebbrowser.settings.AppSettingsSchema
import com.phlox.tvwebbrowser.settings.General
import com.phlox.tvwebbrowser.settings.HomePage
import com.phlox.tvwebbrowser.settings.Search
import com.phlox.tvwebbrowser.settings.Updates
import com.phlox.tvwebbrowser.settings.UserAgent
import com.phlox.tvwebbrowser.settings.WebEngine
import io.github.mlmgames.settings.core.resources.AndroidStringResourceProvider
import io.github.mlmgames.settings.ui.AutoSettingsScreen
import io.github.mlmgames.settings.ui.CategoryConfig
import io.github.mlmgames.settings.ui.ProvideStringResources
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val settingsManager = remember { AppContext.provideSettingsManager() }
    val settings by settingsManager.settings.collectAsState(AppSettings())
    val scope = rememberCoroutineScope()

    ProvideStringResources(AndroidStringResourceProvider(context)) {
        // Replaced Mobile Scaffold with TV-optimized Column layout
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 48.dp, vertical = 24.dp)
        ) {
            // TV Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Button(onClick = onNavigateBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    Spacer(Modifier.width(8.dp))
                    Text("Back")
                }
                Text("Settings", style = MaterialTheme.typography.headlineSmall)
            }

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