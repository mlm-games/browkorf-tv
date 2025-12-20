package com.phlox.tvwebbrowser.compose.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.tv.material3.*
import com.phlox.tvwebbrowser.BuildConfig
import com.phlox.tvwebbrowser.R
import com.phlox.tvwebbrowser.TVBro
import com.phlox.tvwebbrowser.activity.main.UpdateViewModel
import com.phlox.tvwebbrowser.compose.ui.components.TvBroButton
import com.phlox.tvwebbrowser.compose.ui.theme.TvBroTheme
import com.phlox.tvwebbrowser.webengine.WebEngineFactory
import org.koin.androidx.compose.koinViewModel

@Composable
fun AboutScreen(
    backStack: NavBackStack<NavKey>
) {
    val colors = TvBroTheme.colors
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val updateVm: UpdateViewModel = koinViewModel()
    val updateState by updateVm.updateState.collectAsStateWithLifecycle()
    val isChecking by updateVm.isChecking.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .padding(22.dp)
            .verticalScroll(scrollState)
    ) {
        Text(text = stringResource(R.string.app_name_short), style = MaterialTheme.typography.headlineMedium, color = colors.textPrimary)
        Text(text = stringResource(R.string.web_browser_optimized_for_tvs), color = colors.textSecondary)
        
        Spacer(Modifier.height(20.dp))
        Text(text = stringResource(R.string.version_s, "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})"), color = colors.textPrimary)
        Text(text = "Engine: " + WebEngineFactory.getWebEngineVersionString(), color = colors.textSecondary)

        Spacer(Modifier.height(30.dp))
        
        // Update UI Logic
        if (isChecking) {
            Text("Checking...", color = colors.textSecondary)
        } else if (updateState.hasUpdate) {
            Text(stringResource(R.string.new_version_available_s, updateState.latestVersion ?: ""), color = colors.progressTint)
            TvBroButton(onClick = { /* Launch download Intent */ }, text = "Download")
        } else {
            TvBroButton(onClick = { updateVm.checkForUpdates() }, text = stringResource(R.string.auto_check_for_updates))
        }

        Spacer(Modifier.height(32.dp))
        TvBroButton(onClick = { if (backStack.size > 1) backStack.removeAt(backStack.lastIndex) }, text = stringResource(R.string.navigate_back))
    }
}