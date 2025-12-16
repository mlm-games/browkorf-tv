package com.phlox.tvwebbrowser.compose.runtime
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import com.phlox.tvwebbrowser.service.downloads.DownloadService
import java.util.concurrent.atomic.AtomicReference

class DownloadServiceConnector(private val context: Context) {
    private val serviceRef = AtomicReference<DownloadService?>(null)
    val service: DownloadService? get() = serviceRef.get()
    private val conn = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, binder: IBinder) {
            serviceRef.set((binder as DownloadService.LocalBinder).service)
        }
        override fun onServiceDisconnected(name: ComponentName) { serviceRef.set(null) }
    }
    fun bind() = context.bindService(Intent(context, DownloadService::class.java), conn, Context.BIND_AUTO_CREATE)
    fun unbind() { runCatching { context.unbindService(conn) }; serviceRef.set(null) }
}