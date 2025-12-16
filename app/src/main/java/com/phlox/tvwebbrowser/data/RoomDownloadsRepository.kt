package com.phlox.tvwebbrowser.data

import android.net.Uri
import android.os.Build
import com.phlox.tvwebbrowser.TVBro
import com.phlox.tvwebbrowser.model.Download
import com.phlox.tvwebbrowser.model.dao.DownloadDao
import java.io.File

class RoomDownloadsRepository(
    private val dao: DownloadDao
) : DownloadsRepository {

    override suspend fun page(offset: Long): List<Download> =
        dao.allByLimitOffset(offset)

    override suspend fun delete(download: Download) {
        val path = download.filepath
        if (path != null && path.isNotBlank()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                try {
                    TVBro.instance.contentResolver.delete(Uri.parse(path), null, null)
                } catch (e: Exception) {
                    // Fallback or ignore if already gone
                }
            } else {
                File(path).delete()
            }
        }
        dao.delete(download)
    }
}