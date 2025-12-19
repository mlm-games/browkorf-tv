package com.phlox.tvwebbrowser.service.downloads

import android.app.Notification
import android.app.NotificationManager
import android.app.Service
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.text.format.Formatter
import android.util.Log
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.content.FileProvider
import com.phlox.tvwebbrowser.R
import com.phlox.tvwebbrowser.TVBro
import com.phlox.tvwebbrowser.activity.downloads.DownloadsManager
import com.phlox.tvwebbrowser.model.Download
import com.phlox.tvwebbrowser.model.dao.DownloadDao
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.File
import java.util.Date
import java.util.concurrent.Executors

class DownloadService : Service(), KoinComponent {

    private val downloadsManager: DownloadsManager by inject()
    private val downloadDao: DownloadDao by inject()

    private val executor = Executors.newCachedThreadPool()
    private val handler = Handler(Looper.getMainLooper())
    private var notificationBuilder: NotificationCompat.Builder? = null
    private lateinit var notificationManager: NotificationManager

    internal var downloadTasksListener: DownloadTask.Callback = object : DownloadTask.Callback {
        val MIN_NOTIFY_TIMEOUT = 100
        private var lastNotifyTime = System.currentTimeMillis()

        override fun onProgress(task: DownloadTask) {
            val now = System.currentTimeMillis()
            if (now - lastNotifyTime > MIN_NOTIFY_TIMEOUT) {
                lastNotifyTime = now
                handler.post {
                    downloadsManager.notifyListenersAboutDownloadProgress(task)
                    notificationManager.notify(DOWNLOAD_NOTIFICATION_ID, updateNotification())
                }
            }
        }

        override fun onError(task: DownloadTask, responseCode: Int, responseMessage: String) {
            downloadDao.update(task.downloadInfo)
            handler.post {
                downloadsManager.notifyListenersAboutError(task, responseCode, responseMessage)
                onTaskEnded(task)
            }
        }

        override fun onDone(task: DownloadTask) {
            downloadDao.update(task.downloadInfo)
            handler.post {
                downloadsManager.notifyListenersAboutDownloadProgress(task)
                onTaskEnded(task)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    private fun updateNotification(): Notification {
        var title = ""
        var downloaded = 0L
        var total = 0L
        var hasUnknownSizedFiles = false

        for (download in downloadsManager.activeDownloads) {
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
            Formatter.formatShortFileSize(this, downloaded) + " of " +
                    Formatter.formatShortFileSize(this, total)
        }
        if (notificationBuilder == null) {
            notificationBuilder = NotificationCompat.Builder(this, TVBro.CHANNEL_ID_DOWNLOADS)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setSmallIcon(R.drawable.ic_launcher)
        }
        notificationBuilder!!.setContentTitle(title)
            .setContentText(description)
            .setSmallIcon(R.drawable.ic_launcher)
        if (hasUnknownSizedFiles || total == 0L) {
            notificationBuilder!!.setProgress(0, 0, true)
        } else {
            notificationBuilder!!.setProgress(100, (downloaded * 100 / total).toInt(), false)
        }
        return notificationBuilder!!.build()
    }

    override fun onBind(intent: Intent): IBinder {
        return LocalBinder(this)
    }

    private fun onTaskEnded(task: DownloadTask) {
        when (task.downloadInfo.operationAfterDownload) {
            Download.OperationAfterDownload.INSTALL -> {
                val canInstallFromOtherSources = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    packageManager.canRequestPackageInstalls()
                } else {
                    Settings.Secure.getInt(this.contentResolver, Settings.Secure.INSTALL_NON_MARKET_APPS) == 1
                }
                if (canInstallFromOtherSources) {
                    launchInstallAPKActivity(this, task.downloadInfo)
                }
            }
            Download.OperationAfterDownload.NOP -> {
            }
        }
        downloadsManager.onDownloadEnded(task)
        if (downloadsManager.activeDownloads.isEmpty()) {
            stopForeground(true) // TODO: Copy from fluffy
            stopSelf()
        }
    }

    fun launchInstallAPKActivity(context: Context, download: Download) {
        val file = File(download.filepath)
        val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(file.extension)
        val apkURI = FileProvider.getUriForFile(
            context,
            context.applicationContext.packageName + ".provider", file
        )

        val install = Intent(Intent.ACTION_INSTALL_PACKAGE)
        install.setDataAndType(apkURI, mimeType)
        install.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        try {
            context.startActivity(install)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(context, R.string.error, Toast.LENGTH_SHORT).show()
        }
    }

    fun startDownload(download: Download) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            val extPos = download.filename.lastIndexOf(".")
            val hasExt = extPos != -1
            var ext: String? = null
            var prefix: String? = null
            if (hasExt) {
                ext = download.filename.substring(extPos + 1)
                prefix = download.filename.substring(0, extPos)
            }
            var fileName = download.filename
            var i = 0
            while (File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).absolutePath + File.separator + fileName
                ).exists()
            ) {
                i++
                fileName = if (hasExt) {
                    prefix + "_(" + i + ")." + ext
                } else {
                    download.filename + "_(" + i + ")"
                }
            }
            download.filename = fileName

            if (Environment.MEDIA_MOUNTED != Environment.getExternalStorageState()) {
                Toast.makeText(this, R.string.storage_not_mounted, Toast.LENGTH_SHORT).show()
                return
            }
            val downloadsDir =
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (!downloadsDir.exists() && !downloadsDir.mkdirs()) {
                Toast.makeText(this, R.string.can_not_create_downloads, Toast.LENGTH_SHORT)
                    .show()
                return
            }
            download.filepath = downloadsDir.toString() + File.separator + fileName
        }

        download.time = Date().time
        Log.d(TAG, "Start to downloading url: ${download.url}")

        val downloadTask = if (download.base64BlobData != null) {
            BlobDownloadTask(download, download.base64BlobData!!, downloadDao, downloadTasksListener)
        } else if (download.stream != null) {
            StreamDownloadTask(download, download.stream!!, downloadDao, downloadTasksListener)
        } else {
            FileDownloadTask(download, download.userAgentString, downloadDao, downloadTasksListener)
        }

        downloadsManager.activeDownloads.add(downloadTask)
        executor.execute(downloadTask)

        startService(Intent(this, DownloadService::class.java))
        startForeground(DOWNLOAD_NOTIFICATION_ID, updateNotification())
    }

    class LocalBinder(val service: DownloadService) : Binder()

    companion object {
        val TAG: String = DownloadService::class.java.simpleName
        const val DOWNLOAD_NOTIFICATION_ID = 101101
    }
}