package org.mlm.browkorftv.activity.main

import android.content.ContentResolver
import android.content.Intent
import android.net.Uri

object IncomingIntents {
    fun isViewableWebIntent(intent: Intent): Boolean {
        if (intent.action != Intent.ACTION_VIEW) return false
        val uri = intent.data ?: return false
        val scheme = uri.scheme?.lowercase() ?: return false
        if (scheme == "http" || scheme == "https") return true
        if (scheme == "content" || scheme == "file") {
            val type = intent.type?.lowercase() ?: return false
            return type.startsWith("text/html")
                || type.startsWith("application/xhtml")
                || type.startsWith("text/plain")
                || type.startsWith("application/xml")
                || type == "*/*"
        }
        return false
    }

    fun takePersistablePermissionIfNeeded(
        resolver: ContentResolver,
        packageName: String,
        intent: Intent,
        uri: Uri,
        grantUriPermission: (String, Uri, Int) -> Unit = { _, _, _ -> },
    ) {
        if (uri.scheme != "content") return
        if (intent.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION == 0) return
        try {
            resolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        } catch (_: Exception) {
            try {
                grantUriPermission(packageName, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (_: Exception) {
            }
        }
    }
}
