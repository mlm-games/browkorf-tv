package com.phlox.tvwebbrowser.activity.main.view.tabs

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.phlox.tvwebbrowser.R
import com.phlox.tvwebbrowser.databinding.ViewHorizontalWebtabItemBinding
import com.phlox.tvwebbrowser.model.WebTabState
import com.phlox.tvwebbrowser.settings.AppSettings
import com.phlox.tvwebbrowser.singleton.FaviconsPool
import com.phlox.tvwebbrowser.widgets.CheckableContainer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class TabsAdapter(private val tabsView: TabsView) : RecyclerView.Adapter<TabsAdapter.TabViewHolder>() {
    private val tabsCopy = ArrayList<WebTabState>()
    var current: Int = 0
    var listener: Listener? = null
    var checkedView: CheckableContainer? = null

    interface Listener {
        fun onTitleChanged(index: Int)
        fun onTitleSelected(index: Int)
        fun onAddNewTabSelected()
        fun closeTab(tabState: WebTabState?)
        fun openInNewTab(url: String, tabIndex: Int)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TabViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.view_horizontal_webtab_item, parent, false)
        return TabViewHolder(view)
    }

    override fun onBindViewHolder(holder: TabViewHolder, position: Int) {
        holder.bind(tabsCopy[position], position)
    }

    override fun getItemCount(): Int {
        return tabsCopy.size
    }

    // Accepts the list directly from the Activity/View
    fun submitList(newList: List<WebTabState>) {
        val tabsDiffUtilCallback = TabsDiffUtillCallback(tabsCopy, newList)
        val tabsDiffResult = DiffUtil.calculateDiff(tabsDiffUtilCallback)
        tabsCopy.clear()
        tabsCopy.addAll(newList)
        tabsDiffResult.dispatchUpdatesTo(this)
    }

    fun getTabAt(position: Int): WebTabState? {
        return if (position in 0 until tabsCopy.size) tabsCopy[position] else null
    }

    inner class TabViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val vb = ViewHorizontalWebtabItemBinding.bind(itemView)

        fun bind(tabState: WebTabState, position: Int) {
            vb.root.tag = tabState
            vb.tvTitle.text = tabState.title

            if (current == tabState.position) {
                checkedView?.isChecked = false
                vb.root.isChecked = true
                checkedView = vb.root
            } else {
                vb.root.isChecked = false
            }

            vb.ivFavicon.setImageResource(R.drawable.ic_launcher)

            val url = tabState.url
            if (url != AppSettings.HOME_PAGE_URL && url != AppSettings.HOME_URL_ALIAS) {
                val scope = (itemView.context as? AppCompatActivity)?.lifecycleScope
                scope?.launch(Dispatchers.Main) {
                    val currentTag = vb.root.tag as? WebTabState
                    if (currentTag != tabState) return@launch

                    val favicon = FaviconsPool.get(url)
                    if (favicon != null) {
                        vb.ivFavicon.setImageBitmap(favicon)
                    } else {
                        vb.ivFavicon.setImageResource(R.drawable.ic_launcher)
                    }
                }
            }

            vb.root.setOnFocusChangeListener { _, hasFocus ->
                if (hasFocus) {
                    if (current != tabState.position) {
                        current = tabState.position
                        listener?.onTitleChanged(position)
                        checkedView?.isChecked = false
                        vb.root.isChecked = true
                        checkedView = vb.root
                    }
                }
            }

            vb.root.setOnClickListener {
                listener?.onTitleSelected(tabState.position)
            }

            vb.root.setOnLongClickListener {
                tabsView.showTabOptions(tabState)
                true
            }
        }
    }
}