package com.phlox.tvwebbrowser.activity.history

import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.AbsListView
import android.widget.AdapterView
import android.widget.ImageButton
import android.widget.PopupMenu
import androidx.activity.addCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.phlox.tvwebbrowser.R
import com.phlox.tvwebbrowser.databinding.ActivityHistoryBinding
import com.phlox.tvwebbrowser.utils.BaseAnimationListener
import com.phlox.tvwebbrowser.utils.Utils
import com.phlox.tvwebbrowser.utils.VoiceSearchHelper
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel

class HistoryActivity : AppCompatActivity(), AdapterView.OnItemClickListener, AdapterView.OnItemLongClickListener {

    private lateinit var vb: ActivityHistoryBinding
    private var ibDelete: ImageButton? = null
    private var adapter: HistoryAdapter? = null

    // Koin Injection
    private val historyViewModel: HistoryViewModel by viewModel()

    private val voiceSearchHelper = VoiceSearchHelper(
        this, VOICE_SEARCH_REQUEST_CODE,
        VOICE_SEARCH_PERMISSIONS_REQUEST_CODE
    )

    internal var onListScrollListener: AbsListView.OnScrollListener = object : AbsListView.OnScrollListener {
        override fun onScrollStateChanged(view: AbsListView, scrollState: Int) {}

        override fun onScroll(view: AbsListView, firstVisibleItem: Int, visibleItemCount: Int, totalItemCount: Int) {
            if (totalItemCount != 0 && firstVisibleItem + visibleItemCount >= totalItemCount - 1 && "" == historyViewModel.searchQuery) {
                historyViewModel.loadItems(adapter!!.realCount)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        vb = ActivityHistoryBinding.inflate(layoutInflater)
        setContentView(vb.root)

        ibDelete = findViewById(R.id.ibDelete)

        adapter = HistoryAdapter()
        vb.listView.adapter = adapter

        vb.listView.setOnScrollListener(onListScrollListener)
        vb.listView.onItemClickListener = this
        vb.listView.onItemLongClickListener = this

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                historyViewModel.lastLoadedItems.collect { items ->
                    if (items.isNotEmpty()) {
                        adapter!!.addItems(items)
                        vb.listView.requestFocus()
                    }
                }
            }
        }

        onBackPressedDispatcher.addCallback(this) {
            if (adapter!!.isMultiselectMode) {
                adapter!!.isMultiselectMode = false
                updateMenu()
            } else {
                isEnabled = false
                onBackPressedDispatcher.onBackPressed()
            }
        }

        historyViewModel.loadItems()
    }

    private fun showDeleteDialog(deleteAll: Boolean) {
        if (adapter!!.items.isEmpty() || (adapter!!.selectedItems.isEmpty() && !deleteAll)) return
        AlertDialog.Builder(this)
            .setTitle(R.string.delete)
            .setMessage(if (deleteAll) R.string.msg_delete_history_all else R.string.msg_delete_history)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                if (deleteAll) {
                    historyViewModel.deleteAll()
                    adapter!!.erase()
                } else {
                    val selected = adapter!!.selectedItems
                    historyViewModel.deleteItems(selected)
                    adapter!!.remove(selected)
                }
            }
            .setNeutralButton(android.R.string.cancel) { _, _ -> }
            .show()
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        when (event.keyCode) {
            KeyEvent.KEYCODE_SEARCH -> {
                if (event.action == KeyEvent.ACTION_UP) {
                    voiceSearchHelper.initiateVoiceSearch(object : VoiceSearchHelper.Callback {
                        override fun onResult(text: String?) {
                            if (text == null) {
                                Utils.showToast(this@HistoryActivity, getString(R.string.can_not_recognize))
                                return
                            }
                            adapter!!.erase()
                            historyViewModel.searchQuery = text
                            historyViewModel.loadItems()
                        }
                    })
                }
                return true
            }
        }
        return super.dispatchKeyEvent(event)
    }

    // ... (onActivityResult and onRequestPermissionsResult same as before) ...

    override fun onItemClick(parent: AdapterView<*>, view: View, position: Int, id: Long) {
        val hi = (view as HistoryItemView).historyItem
        if (hi!!.isDateHeader) return
        if (adapter!!.isMultiselectMode) {
            view.setSelection(!hi.selected)
            updateMenu()
        } else {
            val resultIntent = Intent()
            resultIntent.putExtra(KEY_URL, hi.url)
            setResult(RESULT_OK, resultIntent)
            finish()
        }
    }

    override fun onItemLongClick(adapterView: AdapterView<*>, view: View, i: Int, l: Long): Boolean {
        if (adapter!!.isMultiselectMode) return false
        adapter!!.isMultiselectMode = true
        val v = view as HistoryItemView
        v.setSelection(true)
        updateMenu()
        return true
    }

    private fun updateMenu() {
        val selection = adapter!!.selectedItems
        if (selection.isEmpty()) {
            if (ibDelete!!.visibility == View.GONE) return
            val anim = AnimationUtils.loadAnimation(this, R.anim.right_menu_out_anim)
            anim.setAnimationListener(object : BaseAnimationListener() {
                override fun onAnimationEnd(animation: Animation) {
                    ibDelete!!.visibility = View.GONE
                }
            })
            ibDelete!!.startAnimation(anim)
        } else {
            if (ibDelete!!.visibility == View.VISIBLE) return
            ibDelete!!.visibility = View.VISIBLE
            ibDelete!!.startAnimation(AnimationUtils.loadAnimation(this, R.anim.right_menu_in_anim))
        }
    }

    fun onClearHistoryClick(view: View) {
        showDeleteDialog(true)
    }

    fun onClearHistoryItemsClick(view: View) {
        showDeleteDialog(false)
    }

    companion object {
        private const val VOICE_SEARCH_REQUEST_CODE = 10001
        private const val VOICE_SEARCH_PERMISSIONS_REQUEST_CODE = 10002
        const val KEY_URL = "url"
    }
}