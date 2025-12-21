package org.mlm.browkorftv.activity.main

import android.content.Context
import android.net.Uri
import android.os.Build
import android.util.Log
import org.mlm.browkorftv.model.Download
import org.mlm.browkorftv.model.dao.DownloadDao
import org.mlm.browkorftv.service.downloads.DownloadTask
import org.mlm.browkorftv.service.downloads.FileDownloadTask
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Manages active downloads.
 */
class DownloadsManager(
    private val downloadDao: DownloadDao,
    private val context: Context
) {
    // Thread-safe list for active tasks
    val activeDownloads = CopyOnWriteArrayList<DownloadTask>()
    private val listeners = CopyOnWriteArrayList<Listener>()

    // Scope for background operations like database deletion
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    interface Listener {
        fun onDownloadUpdated(downloadInfo: Download)
        fun onDownloadError(downloadInfo: Download, responseCode: Int, responseMessage: String)
        fun onAllDownloadsComplete()
    }

    fun registerListener(listener: Listener) {
        listeners.add(listener)
    }

    fun unregisterListener(listener: Listener) {
        listeners.remove(listener)
    }

    fun deleteItem(download: Download) {
        scope.launch {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val contentResolver = context.contentResolver
                try {
                    val rowsDeleted = contentResolver.delete(Uri.parse(download.filepath), null)
                    if (rowsDeleted < 1) {
                        Log.e(FileDownloadTask.TAG, "Failed to delete file from MediaStore")
                    }
                } catch (e: Exception) {
                    Log.w(FileDownloadTask.TAG, "Failed to delete file from MediaStore", e)
                }
            } else {
                val file = File(download.filepath)
                if (file.exists()) {
                    file.delete()
                }
            }
            downloadDao.delete(download)
        }
    }

    fun cancelDownload(download: Download) {
        for (task in activeDownloads) {
            if (task.downloadInfo.id == download.id) {
                task.downloadInfo.cancelled = true
                break
            }
        }
    }

    fun notifyListenersAboutError(task: DownloadTask, responseCode: Int, responseMessage: String) {
        for (listener in listeners) {
            listener.onDownloadError(task.downloadInfo, responseCode, responseMessage)
        }
    }

    fun notifyListenersAboutDownloadProgress(task: DownloadTask) {
        for (listener in listeners) {
            listener.onDownloadUpdated(task.downloadInfo)
        }
    }

    fun onDownloadEnded(task: DownloadTask) {
        activeDownloads.remove(task)
        if (activeDownloads.isEmpty()) {
            for (listener in listeners) {
                listener.onAllDownloadsComplete()
            }
        }
    }
}