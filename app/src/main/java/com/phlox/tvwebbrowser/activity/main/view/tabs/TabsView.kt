package com.phlox.tvwebbrowser.activity.main.view.tabs

import android.app.AlertDialog
import android.content.Context
import android.graphics.Rect
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.RelativeLayout
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.phlox.tvwebbrowser.R
import com.phlox.tvwebbrowser.activity.main.TabsViewModel
import com.phlox.tvwebbrowser.compose.settings.SettingsViewModel
import com.phlox.tvwebbrowser.databinding.ViewTabsBinding
import com.phlox.tvwebbrowser.model.WebTabState
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class TabsView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : RelativeLayout(context, attrs), KoinComponent {

    private var vb = ViewTabsBinding.inflate(LayoutInflater.from(context), this)

    private val tabsViewModel: TabsViewModel by inject()
    private val settingsViewModel: SettingsViewModel by inject()

    val adapter: TabsAdapter = TabsAdapter(this)

    var current: Int by adapter::current
    var listener: TabsAdapter.Listener? by adapter::listener

    init {
        init()
    }

    fun init() {
        if (isInEditMode) return
        adapter.tabsModel = tabsViewModel // Adapter needs updating to accept ViewModel
        vb.rvTabs.layoutManager = LinearLayoutManager(context, RecyclerView.HORIZONTAL, false)
        vb.btnAdd.setOnClickListener {
            listener?.onAddNewTabSelected()
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (isInEditMode) return

        val lifecycleOwner = findViewTreeLifecycleOwner() ?: return

        lifecycleOwner.lifecycleScope.launch {
            tabsViewModel.tabsStates.collect {
                adapter.onTabListChanged() // Refreshes from ViewModel internally
                scrollToSeeCurrentTab()
            }
        }

        lifecycleOwner.lifecycleScope.launch {
            tabsViewModel.currentTab.collect { tab ->
                val index = tabsViewModel.tabsStates.value.indexOf(tab)
                if (index != -1 && current != index) {
                    adapter.notifyItemChanged(current)
                    current = index
                    adapter.notifyItemChanged(index)
                    scrollToSeeCurrentTab()
                }
            }
        }

        vb.rvTabs.adapter = adapter
    }

    fun showTabOptions(tab: WebTabState) {
        val tabIndex = tabsViewModel.tabsStates.value.indexOf(tab)
        AlertDialog.Builder(context)
            .setTitle(R.string.tabs)
            .setItems(R.array.tabs_options) { _, i ->
                when (i) {
                    0 -> listener?.openInNewTab(settingsViewModel.currentSettings.homePage, tabIndex + 1)
                    1 -> listener?.closeTab(tab)
                    2 -> {
                        tabsViewModel.onCloseAllTabs()
                        listener?.openInNewTab(settingsViewModel.currentSettings.homePage, 0)
                    }
                    // Move Left/Right logic would need ViewModel support for swapping
                    // For now, these might need to be implemented in TabsViewModel
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
                    val index = tabsViewModel.tabsStates.value.indexOf(tab)
                    if (index == current && !child.hasFocus()) {
                        child.requestFocus()
                    }
                }
            }
        } else {
            super.onFocusChanged(gainFocus, direction, previouslyFocusedRect)
        }
    }

    fun onTabTitleUpdated(tab: WebTabState) {
        val tabIndex = tabsViewModel.tabsStates.value.indexOf(tab)
        adapter.notifyItemChanged(tabIndex)
    }

    fun onFavIconUpdated(tab: WebTabState) {
        val tabIndex = tabsViewModel.tabsStates.value.indexOf(tab)
        adapter.notifyItemChanged(tabIndex)
    }

    private fun scrollToSeeCurrentTab() {
        val lm = (vb.rvTabs.layoutManager as LinearLayoutManager)
        if (current < lm.findFirstCompletelyVisibleItemPosition() ||
            current > lm.findLastCompletelyVisibleItemPosition()
        ) {
            vb.rvTabs.scrollToPosition(current)
        }
    }
}