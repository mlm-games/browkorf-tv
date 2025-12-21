package org.mlm.browkorftv.updates

import android.app.Activity
import android.text.Html
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import org.mlm.browkorftv.R
import org.mlm.browkorftv.BuildConfig
import org.mlm.browkorftv.utils.Utils

object UpdateDialogs {

    fun showUpdateAvailableDialog(
        activity: Activity,
        info: UpdateInfo,
        onDownload: () -> Unit,
        onLater: () -> Unit = {},
        onSettings: () -> Unit = {}
    ) {
        if (!info.hasUpdate(BuildConfig.VERSION_CODE)) return

        val message = buildString {
            val entries = info.changelogSince(BuildConfig.VERSION_CODE)
            for (e in entries) {
                append("<b>${e.versionName}</b><br>")
                append(e.changes.replace("\n", "<br>"))
                append("<br><br>")
            }
        }

        val textView = TextView(activity).apply {
            val pad = Utils.D2P(activity, 25f).toInt()
            setPadding(pad, pad, pad, pad)
            text = Html.fromHtml(message, Html.FROM_HTML_MODE_COMPACT)
        }

        AlertDialog.Builder(activity)
            .setTitle(R.string.new_version_dialog_title)
            .setView(textView)
            .setPositiveButton(R.string.download) { _, _ -> onDownload() }
            .setNegativeButton(R.string.later) { _, _ -> onLater() }
            .setNeutralButton(R.string.settings) { _, _ -> onSettings() }
            .show()
    }

    fun showDownloadProgressDialog(
        activity: Activity
    ): Pair<AlertDialog, ProgressBar> {
        val progressBar = ProgressBar(activity, null, android.R.attr.progressBarStyleHorizontal).apply {
            isIndeterminate = true
            val padding = 40
            setPadding(padding, padding, padding, padding)
        }

        val dialog = AlertDialog.Builder(activity)
            .setTitle(R.string.downloading_file)
            .setView(progressBar)
            .setCancelable(false)
            .create()

        dialog.show()
        return dialog to progressBar
    }

    fun updateProgressBar(pb: ProgressBar, p: DownloadProgress?) {
        if (p == null) return
        if (p.totalBytes == null || p.percent == null) {
            pb.isIndeterminate = true
        } else {
            pb.isIndeterminate = false
            pb.max = 100
            pb.setProgress(p.percent, true)
        }
    }

    fun toast(activity: Activity, msg: String) {
        Toast.makeText(activity, msg, Toast.LENGTH_SHORT).show()
    }
}