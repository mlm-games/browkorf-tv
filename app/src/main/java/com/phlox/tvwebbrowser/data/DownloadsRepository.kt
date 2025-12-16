package com.phlox.tvwebbrowser.data

import com.phlox.tvwebbrowser.model.Download

interface DownloadsRepository {
    suspend fun page(offset: Long): List<Download>
    suspend fun delete(download: Download)
}