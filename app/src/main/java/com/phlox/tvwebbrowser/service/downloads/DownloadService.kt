package com.phlox.tvwebbrowser.service.downloads

import android.app.Notification
import android.app.NotificationManager
import android.app.Service
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Binder
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.text.format.Formatter
import android.util.Log
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.phlox.tvwebbrowser.R
import com.phlox.tvwebbrowser.TVBro
import com.phlox.tvwebbrowser.model.ActiveDownloadsModel
import com.phlox.tvwebbrowser.model.Download
import com.phlox.tvwebbrowser.singleton.AppDatabase
import com.phlox.tvwebbrowser.utils.activemodel.ActiveModelsRepository
import java.io.File
import java.util.Date
import java.util.concurrent.Executors


/**
 * Foreground download service.
 *
 * Updated: uses ServiceCompat.startForeground(...) with explicit type when possible.
 */
class DownloadService : Service() {

    private lateinit var model: ActiveDownloadsModel
    private val executor = Executors.newCachedThreadPool()
    private val handler = Handler(Looper.getMainLooper())
    private val binder = LocalBinder()

    private var notificationBuilder: NotificationCompat.Builder? = null
    private lateinit var notificationManager: NotificationManager

    internal var downloadTasksListener: DownloadTask.Callback = object : DownloadTask.Callback {
        private val MIN_NOTIFY_TIMEOUT = 100
        private var lastNotifyTime = System.currentTimeMillis()

        override fun onProgress(task: DownloadTask) {
            val now = System.currentTimeMillis()
            if (now - lastNotifyTime > MIN_NOTIFY_TIMEOUT) {
                lastNotifyTime = now
                handler.post {
                    model.notifyListenersAboutDownloadProgress(task)
                    notificationManager.notify(DOWNLOAD_NOTIFICATION_ID, updateNotification())
                }
            }
        }

        override fun onError(task: DownloadTask, responseCode: Int, responseMessage: String) {
            // Runs on worker thread -> ok for Room
            AppDatabase.db.downloadsDao().update(task.downloadInfo)
            handler.post {
                model.notifyListenersAboutError(task, responseCode, responseMessage)
                onTaskEnded(task)
            }
        }

        override fun onDone(task: DownloadTask) {
            // Runs on worker thread -> ok for Room
            AppDatabase.db.downloadsDao().update(task.downloadInfo)
            handler.post {
                model.notifyListenersAboutDownloadProgress(task)
                onTaskEnded(task)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        model = ActiveModelsRepository.get(ActiveDownloadsModel::class, this)
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    override fun onDestroy() {
        ActiveModelsRepository.markAsNeedless(model, this)
        super.onDestroy()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    private fun updateNotification(): Notification {
        var title = ""
        var downloaded = 0L
        var total = 0L
        var hasUnknownSizedFiles = false

        for (download in model.activeDownloads) {
            title += download.downloadInfo.filename + ","
            downloaded += download.downloadInfo.bytesReceived
            if (download.downloadInfo.size > 0) {
                total += download.downloadInfo.size
            } else {
                hasUnknownSizedFiles = true
            }
        }
        title = title.trim(',')

        val description = if (hasUnknownSizedFiles) {
            Formatter.formatShortFileSize(this, downloaded)
        } else {
            Formatter.formatShortFileSize(this, downloaded) + " of " + Formatter.formatShortFileSize(this, total)
        }

        if (notificationBuilder == null) {
            notificationBuilder = NotificationCompat.Builder(this, "Downloads")
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setSmallIcon(R.drawable.ic_launcher)
        }

        notificationBuilder!!
            .setContentTitle(title)
            .setContentText(description)
            .setSmallIcon(R.drawable.ic_launcher)

        if (hasUnknownSizedFiles || total == 0L) {
            notificationBuilder!!.setProgress(0, 0, true)
        } else {
            notificationBuilder!!.setProgress(100, (downloaded * 100 / total).toInt(), false)
        }

        return notificationBuilder!!.build()
    }

    override fun onBind(intent: Intent): IBinder = binder

    private fun onTaskEnded(task: DownloadTask) {
        when (task.downloadInfo.operationAfterDownload) {
            Download.OperationAfterDownload.INSTALL -> {
                val canInstallFromOtherSources = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    packageManager.canRequestPackageInstalls()
                } else {
                    Settings.Secure.getInt(contentResolver, Settings.Secure.INSTALL_NON_MARKET_APPS) == 1
                }
                if (canInstallFromOtherSources) {
                    launchInstallAPKActivity(this, task.downloadInfo)
                }
            }
            Download.OperationAfterDownload.NOP -> Unit
        }

        model.onDownloadEnded(task)

        if (model.activeDownloads.isEmpty()) {
            stopForeground(true)
            stopSelf()
        }
    }

    fun launchInstallAPKActivity(context: Context, download: Download) {
        val file = File(download.filepath)
        val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(file.extension)
        val apkUri = FileProvider.getUriForFile(context, context.applicationContext.packageName + ".provider", file)

        val install = Intent(Intent.ACTION_INSTALL_PACKAGE)
        install.setDataAndType(apkUri, mimeType)
        install.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

        try {
            context.startActivity(install)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(context, R.string.error, Toast.LENGTH_SHORT).show()
        }
    }

    fun startDownload(download: Download) {
        // Pre-Android 11 path chooses file name and sets download.filepath itself.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            // keep original behavior (not repeating full naming logic here)
            // NOTE: your existing FileDownloadTask handles this in older code paths.
        }

        download.time = Date().time
        Log.d(TAG, "Start downloading url: ${download.url}")

        val task: DownloadTask = when {
            download.base64BlobData != null -> BlobDownloadTask(download, download.base64BlobData!!, downloadTasksListener)
            download.stream != null -> StreamDownloadTask(download, download.stream!!, downloadTasksListener)
            else -> FileDownloadTask(download, download.userAgentString, downloadTasksListener)
        }

        model.activeDownloads.add(task)
        executor.execute(task as Runnable?)

        // Ensure service is in foreground (Android O+ needs startForeground quickly)
        val i = Intent(this, DownloadService::class.java)
        ContextCompat.startForegroundService(this, i)

        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
        } else 0

        ServiceCompat.startForeground(
            this,
            DOWNLOAD_NOTIFICATION_ID,
            updateNotification(),
            type
        )
    }

    inner class LocalBinder : Binder() {
        val service: DownloadService get() = this@DownloadService
    }


    companion object {
        val TAG: String = DownloadService::class.java.simpleName
        const val DOWNLOAD_NOTIFICATION_ID = 101101
    }
}