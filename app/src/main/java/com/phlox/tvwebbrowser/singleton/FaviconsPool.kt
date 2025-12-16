package com.phlox.tvwebbrowser.singleton

import android.graphics.Bitmap
import com.phlox.tvwebbrowser.model.HostConfig
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

object FaviconsPool {
    fun get(host: String): Bitmap? {
        // Safe bridge for legacy sync callers
        return runBlocking {
            getSuspend(host)
        }
    }

    suspend fun getSuspend(host: String): Bitmap? = withContext(Dispatchers.IO) {
        val dao = AppDatabase.db.hostsDao()
        val config = dao.findByHostName(host)
        config?.favicon as Bitmap?
    }

    @OptIn(DelicateCoroutinesApi::class)
    fun set(host: String, favicon: Bitmap?) {
        GlobalScope.launch(Dispatchers.IO) {
            val dao = AppDatabase.db.hostsDao()
            val existing = dao.findByHostName(host) ?: HostConfig( host )
            existing.favicon = favicon as String?
            dao.insert(existing)
        }
    }
}