package com.phlox.tvwebbrowser.activity.downloads

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.text.format.Formatter
import android.view.LayoutInflater
import android.widget.FrameLayout
import android.widget.ProgressBar
import android.widget.TextView
import com.phlox.tvwebbrowser.R
import com.phlox.tvwebbrowser.model.Download
import java.text.SimpleDateFormat
import java.util.Date

class DownloadListItemView(
    context: Context,
    private val viewType: Int
) : FrameLayout(context) {

    private var defaultTextColor: Int = 0
    private var tvDate: TextView? = null
    private var tvTitle: TextView? = null
    private var tvURL: TextView? = null
    private var tvTime: TextView? = null
    private var progressBar: ProgressBar? = null
    private var progressBar2: ProgressBar? = null
    private var tvSize: TextView? = null

    var download: Download? = null
        set(value) {
            field = value
            if (field == null) return
            when (viewType) {
                DownloadListAdapter.VIEW_TYPE_HEADER -> {
                    val df = SimpleDateFormat.getDateInstance()
                    tvDate!!.text = df.format(Date(field!!.time))
                }
                DownloadListAdapter.VIEW_TYPE_DOWNLOAD_ITEM -> {
                    tvTitle!!.text = field!!.filename
                    tvURL!!.text = field!!.url
                    val sdf = SimpleDateFormat("HH:mm")
                    tvTime!!.text = sdf.format(Date(field!!.time))
                    updateUI(field!!)
                }
            }
        }

    init {
        LayoutInflater.from(context).inflate(
            if (viewType == DownloadListAdapter.VIEW_TYPE_HEADER)
                R.layout.view_history_header_item
            else
                R.layout.view_download_item, this
        )
        when (viewType) {
            DownloadListAdapter.VIEW_TYPE_HEADER -> tvDate = findViewById(R.id.tvDate)
            DownloadListAdapter.VIEW_TYPE_DOWNLOAD_ITEM -> {
                tvTitle = findViewById(R.id.tvTitle)
                tvURL = findViewById(R.id.tvURL)
                tvTime = findViewById(R.id.tvTime)
                tvSize = findViewById(R.id.tvSize)
                defaultTextColor = tvSize!!.currentTextColor
                progressBar = findViewById(R.id.progressBar)
                progressBar2 = findViewById(R.id.progressBar2)
            }
        }
    }

    @SuppressLint("SetTextI18n")
    fun updateUI(download: Download) {
        if (tvTitle?.text != download.filename) {
            tvTitle?.text = download.filename
        }
        this.download?.size = download.size
        this.download?.bytesReceived = download.bytesReceived
        tvSize!!.setTextColor(defaultTextColor)
        if (download.size == Download.CANCELLED_MARK) {
            tvSize!!.setText(R.string.cancelled)
            progressBar!!.visibility = INVISIBLE
            progressBar2!!.visibility = GONE
        } else if (download.size == Download.BROKEN_MARK) {
            tvSize!!.setText(R.string.error)
            tvSize!!.setTextColor(Color.RED)
            progressBar!!.visibility = INVISIBLE
            progressBar2!!.visibility = GONE
        } else if (download.size == 0L) {
            tvSize!!.text = Formatter.formatShortFileSize(context, download.bytesReceived)
            progressBar!!.visibility = INVISIBLE
            progressBar2!!.visibility = VISIBLE
        } else if (download.size > 0) {
            if (download.size == download.bytesReceived) {
                tvSize!!.text = Formatter.formatShortFileSize(context, download.size)
                progressBar!!.visibility = INVISIBLE
                progressBar2!!.visibility = GONE
            } else {
                tvSize!!.text = Formatter.formatShortFileSize(context, download.bytesReceived) + "/\n" +
                        Formatter.formatShortFileSize(context, download.size)
                progressBar!!.visibility = VISIBLE
                progressBar2!!.visibility = GONE
                if (download.bytesReceived > download.size) {
                    download.size = 0L // wrong value from server - ignore it for future updates
                } else {
                    progressBar!!.progress = (download.bytesReceived * 100 / download.size).toInt()
                }
            }
        }
    }
}