package com.phlox.tvwebbrowser.activity.main.dialogs.favorites

import android.app.Dialog
import android.content.Context
import android.view.View
import android.widget.AdapterView
import android.widget.Button
import android.widget.ListView
import android.widget.ProgressBar
import android.widget.TextView
import com.phlox.tvwebbrowser.R
import com.phlox.tvwebbrowser.activity.main.dialogs.favorites.FavoriteEditorDialog
import com.phlox.tvwebbrowser.model.FavoriteItem
import com.phlox.tvwebbrowser.model.dao.FavoritesDao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class FavoritesDialog(
    context: Context,
    val scope: CoroutineScope, // Activity scope passed in
    private val callback: Callback,
    private val currentPageTitle: String?,
    private val currentPageUrl: String?
) : Dialog(context), FavoriteItemView.Listener, KoinComponent {

    private val favoritesDao: FavoritesDao by inject() // Inject DAO directly

    private var items: MutableList<FavoriteItem> = ArrayList()
    private val adapter = FavoritesListAdapter(items, this)

    private val tvPlaceholder: TextView
    private val listView: ListView
    private val btnAdd: Button
    private val btnEdit: Button
    private val pbLoading: ProgressBar

    interface Callback {
        fun onFavoriteChoosen(item: FavoriteItem?)
    }

    init {
        setCancelable(true)
        setContentView(R.layout.dialog_favorites)
        setTitle(R.string.bookmarks)

        tvPlaceholder = findViewById(R.id.tvPlaceholder)
        listView = findViewById(R.id.listView)
        btnAdd = findViewById(R.id.btnAdd)
        btnEdit = findViewById(R.id.btnEdit)
        pbLoading = findViewById(R.id.pbLoading)

        btnAdd.setOnClickListener { showAddItemDialog() }

        btnEdit.setOnClickListener {
            adapter.isEditMode = !adapter.isEditMode
            btnEdit.setText(if (adapter.isEditMode) R.string.done else R.string.edit)
            listView.itemsCanFocus = adapter.isEditMode
        }

        listView.onItemClickListener = AdapterView.OnItemClickListener { _, view, _, _ ->
            val item = (view as FavoriteItemView).favorite
            if (!item!!.isFolder) {
                callback.onFavoriteChoosen(item)
                dismiss()
            }
        }

        pbLoading.visibility = View.VISIBLE
        listView.visibility = View.GONE
        tvPlaceholder.visibility = View.GONE
        listView.adapter = adapter

        scope.launch(Dispatchers.Main) {
            items.addAll(favoritesDao.getAll())
            onItemsChanged()
            pbLoading.visibility = View.GONE
        }
    }

    private fun showAddItemDialog() {
        val newItem = FavoriteItem()
        newItem.title = currentPageTitle
        newItem.url = currentPageUrl
        FavoriteEditorDialog(context, object : FavoriteEditorDialog.Callback {
            override fun onDone(item: FavoriteItem) {
                onItemEdited(item)
            }
        }, newItem).show()
    }

    private fun onItemEdited(item: FavoriteItem) {
        pbLoading.visibility = View.VISIBLE
        listView.visibility = View.GONE
        tvPlaceholder.visibility = View.GONE
        scope.launch(Dispatchers.Main) {
            if (item.id == 0L) {
                val lastInsertRowId = favoritesDao.insert(item)
                item.id = lastInsertRowId
                items.add(0, item)
            } else {
                favoritesDao.update(item)
            }
            onItemsChanged()
        }
    }

    private fun onItemsChanged() {
        adapter.notifyDataSetChanged()
        pbLoading.visibility = View.GONE
        listView.visibility = if (items.isEmpty()) View.GONE else View.VISIBLE
        tvPlaceholder.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
    }

    override fun onDeleteClick(favorite: FavoriteItem) {
        scope.launch(Dispatchers.Main) {
            favoritesDao.delete(favorite)
            items.remove(favorite)
            onItemsChanged()
        }
    }

    override fun onEditClick(favorite: FavoriteItem) {
        FavoriteEditorDialog(context, object : FavoriteEditorDialog.Callback {
            override fun onDone(item: FavoriteItem) {
                onItemEdited(item)
            }
        }, favorite).show()
    }
}