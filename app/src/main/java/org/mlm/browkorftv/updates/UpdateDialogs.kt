package org.mlm.browkorftv.updates

import android.app.Activity
import android.widget.ProgressBar
import android.widget.TextView
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
        onSettings: () -> Unit = {},
        onDismiss: () -> Unit = {},
        onShown: (AlertDialog) -> Unit = {}
    ): Boolean {
        if (!info.hasUpdate(BuildConfig.VERSION_NAME)) return false

        val message = info.changelog.replace("#", "")

        val textView = TextView(activity).apply {
            val pad = Utils.D2P(activity, 25f).toInt()
            setPadding(pad, pad, pad, pad)
            text = message
        }

        val dialog = AlertDialog.Builder(activity)
            .setTitle(R.string.new_version_dialog_title)
            .setView(textView)
            .setPositiveButton(R.string.download) { _, _ -> onDownload() }
            .setNegativeButton(R.string.later) { _, _ -> onLater() }
            .setNeutralButton(R.string.browkorf_settings) { _, _ -> onSettings() }
            .create()
        dialog.setOnCancelListener { onDismiss() }
        dialog.show()
        onShown(dialog)
        return true
    }

    fun showDownloadProgressDialog(
        activity: Activity,
        onCancel: () -> Unit = {}
    ): Pair<AlertDialog, ProgressBar> {
        val progressBar = ProgressBar(activity, null, android.R.attr.progressBarStyleHorizontal).apply {
            isIndeterminate = true
            val padding = 40
            setPadding(padding, padding, padding, padding)
        }

        val dialog = AlertDialog.Builder(activity)
            .setTitle(R.string.downloading_file)
            .setView(progressBar)
            .setNegativeButton(R.string.cancel) { _, _ -> onCancel() }
            .setCancelable(true)
            .create()

        dialog.setOnCancelListener { onCancel() }
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
}
