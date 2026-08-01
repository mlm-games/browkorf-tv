package org.mlm.browkorftv.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.tv.material3.*
import androidx.webkit.WebViewCompat
import org.mlm.browkorftv.BuildConfig
import org.mlm.browkorftv.R
import org.mlm.browkorftv.ui.components.BrowkorfTvIconButton

@Composable
fun AboutScreen(
    onBack: () -> Unit,
) {
    val ctx = LocalContext.current

    val engine = remember {
        val p = WebViewCompat.getCurrentWebViewPackage(ctx)
        "WebView ${p?.packageName ?: "unknown"} ${p?.versionName ?: "unknown"}"
    }

    Column(
        Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("About", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.weight(1f))
            BrowkorfTvIconButton(
                onClick = onBack,
                painter = painterResource(R.drawable.outline_chevron_forward_24),
                contentDescription = "Back"
            )
        }

        Surface {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text("App: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
                Text("Engine: $engine", style = MaterialTheme.typography.bodySmall)
                Text(
                    "targetSdk: ${ctx.applicationInfo.targetSdkVersion}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}