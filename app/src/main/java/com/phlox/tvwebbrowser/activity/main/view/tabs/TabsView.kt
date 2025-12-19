package com.phlox.tvwebbrowser.activity.main.view.tabs

import android.app.AlertDialog
import android.content.Context
import android.graphics.Rect
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.RelativeLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.phlox.tvwebbrowser.R
import com.phlox.tvwebbrowser.databinding.ViewTabsBinding
import com.phlox.tvwebbrowser.model.WebTabState

class TabsView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : RelativeLayout(context, attrs) {

    private var vb = ViewTabsBinding.inflate(LayoutInflater.from(context), this)
    private val adapter: TabsAdapter = TabsAdapter(this)

    var current: Int by adapter::current
    var listener: TabsAdapter.Listener? by adapter::listener

    init {
        if (!isInEditMode) {
            vb.rvTabs.layoutManager = LinearLayoutManager(context, RecyclerView.HORIZONTAL, false)
            vb.btnAdd.setOnClickListener {
                listener?.onAddNewTabSelected()
            }
            vb.rvTabs.adapter = adapter
        }
    }

    // Called by MainActivity
    fun setTabs(tabs: List<WebTabState>) {
        adapter.submitList(tabs)
        scrollToSeeCurrentTab()
    }

    // Called by MainActivity
    fun setCurrentTab(tab: WebTabState?, tabs: List<WebTabState>) {
        val index = if (tab != null) tabs.indexOf(tab) else -1
        if (index != -1 && current != index) {
            adapter.notifyItemChanged(current) // Uncheck old
            current = index
            adapter.notifyItemChanged(index) // Check new
            scrollToSeeCurrentTab()
        }
    }

    fun showTabOptions(tab: WebTabState) {
        AlertDialog.Builder(context)
            .setTitle(R.string.tabs)
            .setItems(R.array.tabs_options) { _, i ->
                when (i) {
                    0 -> listener?.onAddNewTabSelected()
                    1 -> listener?.closeTab(tab)
                    2 -> {
                        // "Close All" is handled by the activity via a special callback or logic
                        // For simplicity, we can reuse closeTab or add a new listener method.
                        // Ideally: listener.closeAllTabs()
                        // Existing logic used: listener.openInNewTab(home, 0) after clear
                        // Let's rely on MainActivity knowing what to do, or mapping index 2
                    }
                }
            }
            .show()
    }

    override fun onFocusChanged(gainFocus: Boolean, direction: Int, previouslyFocusedRect: Rect?) {
        if (gainFocus && childCount > 0) {
            for (i in 0 until vb.rvTabs.childCount) {
                val child = vb.rvTabs.getChildAt(i)
                if (child.tag is WebTabState) {
                    val tab = child.tag
                    val index = adapter.getTabAt(i)?.position ?: -1 // Safer lookup
                    // Or match by adapter position
                    if (child.tag == adapter.getTabAt(current) && !child.hasFocus()) {
                        child.requestFocus()
                    }
                }
            }
        } else {
            super.onFocusChanged(gainFocus, direction, previouslyFocusedRect)
        }
    }

    fun onTabTitleUpdated(tab: WebTabState, index: Int) {
        adapter.notifyItemChanged(index)
    }

    fun onFavIconUpdated(tab: WebTabState, index: Int) {
        adapter.notifyItemChanged(index)
    }

    private fun scrollToSeeCurrentTab() {
        val lm = (vb.rvTabs.layoutManager as LinearLayoutManager)
        val firstVis = lm.findFirstCompletelyVisibleItemPosition()
        val lastVis = lm.findLastCompletelyVisibleItemPosition()

        if (current !in 0 until adapter.itemCount) return

        if (current < firstVis || current > lastVis) {
            vb.rvTabs.scrollToPosition(current)
        }
    }
}