package com.phlox.tvwebbrowser.activity.main.view

import android.content.Context
import android.transition.TransitionManager
import android.util.AttributeSet
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.view.inputmethod.InputMethodManager
import android.widget.ImageButton
import android.widget.LinearLayout
import com.phlox.tvwebbrowser.R
import com.phlox.tvwebbrowser.activity.downloads.DownloadsManager
import com.phlox.tvwebbrowser.databinding.ViewActionbarBinding
import com.phlox.tvwebbrowser.model.Download
import com.phlox.tvwebbrowser.settings.AppSettings
import com.phlox.tvwebbrowser.settings.AppSettings.Companion.HOME_PAGE_URL
import com.phlox.tvwebbrowser.settings.SettingsManager
import com.phlox.tvwebbrowser.utils.Utils
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class ActionBar @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : LinearLayout(context, attrs), KoinComponent, DownloadsManager.Listener {

    private val vb = ViewActionbarBinding.inflate(LayoutInflater.from(context), this)
    var callback: Callback? = null
    private var downloadAnimation: Animation? = null

    // Inject dependencies via Koin
    private val downloadsManager: DownloadsManager by inject()
    private val settingsManager: SettingsManager by inject()

    private var extendedAddressBarMode = false

    private val settings: AppSettings get() = settingsManager.current

    interface Callback {
        fun closeWindow()
        fun showDownloads()
        fun showFavorites()
        fun showHistory()
        fun showSettings()
        fun initiateVoiceSearch()
        fun search(text: String)
        fun onExtendedAddressBarMode()
        fun onUrlInputDone()
        fun toggleIncognitoMode()
    }

    private val etUrlFocusChangeListener = OnFocusChangeListener { _, focused ->
        if (focused) {
            enterExtendedAddressBarMode()
            val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(vb.etUrl, InputMethodManager.SHOW_IMPLICIT)
            postDelayed({ vb.etUrl.selectAll() }, 500)
        }
    }

    private val etUrlKeyListener = OnKeyListener { _, _, keyEvent ->
        when (keyEvent.keyCode) {
            KeyEvent.KEYCODE_ENTER -> {
                if (keyEvent.action == KeyEvent.ACTION_UP) {
                    val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.hideSoftInputFromWindow(vb.etUrl.windowToken, 0)
                    callback?.search(vb.etUrl.text.toString())
                    dismissExtendedAddressBarMode()
                    callback?.onUrlInputDone()
                }
                return@OnKeyListener true
            }
        }
        false
    }

    init {
        init()
    }

    fun init() {
        orientation = HORIZONTAL
        if (isInEditMode) return

        val incognitoMode = settings.incognitoMode

        vb.ibMenu.setOnClickListener { callback?.closeWindow() }
        vb.ibDownloads.setOnClickListener { callback?.showDownloads() }
        vb.ibFavorites.setOnClickListener { callback?.showFavorites() }
        vb.ibHistory.setOnClickListener { callback?.showHistory() }
        vb.ibIncognito.setOnClickListener { callback?.toggleIncognitoMode() }
        vb.ibSettings.setOnClickListener { callback?.showSettings() }

        if (Utils.isFireTV(context)) {
            vb.ibMenu.nextFocusRightId = R.id.ibHistory
            removeView(vb.ibVoiceSearch)
        } else {
            vb.ibVoiceSearch.setOnClickListener { callback?.initiateVoiceSearch() }
        }

        vb.ibIncognito.isChecked = incognitoMode
        vb.etUrl.onFocusChangeListener = etUrlFocusChangeListener
        vb.etUrl.setOnKeyListener(etUrlKeyListener)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (!isInEditMode) {
            downloadsManager.registerListener(this)
            updateDownloadAnimation()
        }
    }

    override fun onDetachedFromWindow() {
        if (!isInEditMode) {
            downloadsManager.unregisterListener(this)
        }
        super.onDetachedFromWindow()
    }

    // Fixed return types: post returns Boolean, but interface expects Unit.
    override fun onDownloadUpdated(downloadInfo: Download) {
        post { updateDownloadAnimation() }
    }

    override fun onDownloadError(downloadInfo: Download, responseCode: Int, responseMessage: String) {
        post { updateDownloadAnimation() }
    }

    override fun onAllDownloadsComplete() {
        post { updateDownloadAnimation() }
    }

    private fun updateDownloadAnimation() {
        if (downloadsManager.activeDownloads.isNotEmpty()) {
            if (downloadAnimation == null) {
                downloadAnimation = AnimationUtils.loadAnimation(context, R.anim.infinite_fadeinout_anim)
                vb.ibDownloads.startAnimation(downloadAnimation)
            }
        } else {
            downloadAnimation?.apply {
                this.reset()
                vb.ibDownloads.clearAnimation()
                downloadAnimation = null
            }
        }
    }

    fun setAddressBoxText(text: String) {
        if (text == HOME_PAGE_URL) {
            vb.etUrl.setText("")
        } else {
            vb.etUrl.setText(text)
        }
    }

    fun setAddressBoxTextColor(color: Int) {
        vb.etUrl.setTextColor(color)
    }

    private fun enterExtendedAddressBarMode() {
        if (extendedAddressBarMode) return
        extendedAddressBarMode = true
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            if (child is ImageButton) {
                child.visibility = GONE
            }
        }
        TransitionManager.beginDelayedTransition(this)
        callback?.onExtendedAddressBarMode()
    }

    fun dismissExtendedAddressBarMode() {
        if (!extendedAddressBarMode) return
        extendedAddressBarMode = false
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            if (child is ImageButton) {
                child.visibility = VISIBLE
            }
        }
    }

    fun catchFocus() {
        vb.ibMenu.requestFocus()
    }
}