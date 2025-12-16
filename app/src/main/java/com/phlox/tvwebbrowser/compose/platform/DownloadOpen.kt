package com.phlox.tvwebbrowser.compose.platform

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import com.phlox.tvwebbrowser.model.Download
import java.io.File

object DownloadOpen {
    fun open(context: Context, download: Download) {
        val path = download.filepath ?: return
        val uri: Uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Uri.parse(path)
        } else {
            val file = File(path)
            FileProvider.getUriForFile(context, context.packageName + ".provider", file)
        }

        val mime = context.contentResolver.getType(uri) ?: "*/*"
        val i = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mime)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        try {
            context.startActivity(i)
        } catch (e: Exception) {
            // Toast or log error
        }
    }
}