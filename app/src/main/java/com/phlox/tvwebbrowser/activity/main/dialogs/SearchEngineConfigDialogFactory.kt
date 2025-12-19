package com.phlox.tvwebbrowser.activity.main.dialogs

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.*
import androidx.appcompat.app.AlertDialog
import com.phlox.tvwebbrowser.R
import com.phlox.tvwebbrowser.activity.main.SettingsModel
import com.phlox.tvwebbrowser.settings.AppSettings
import androidx.core.view.isGone

/**
 * Created by fedex on 18.01.17.
 */

object SearchEngineConfigDialogFactory {
    interface Callback {
        fun onDone(url: String)
    }

    fun show(context: Context, settings: SettingsModel, cancellable: Boolean, callback: Callback) {
        val currentUrl = settings.current.searchEngineURL
        var selected = AppSettings.SearchEnginesURLs.indexOf(currentUrl)
        if (selected == -1) {
            selected = AppSettings.SearchEnginesURLs.size - 1 // Custom
        }

        val builder = AlertDialog.Builder(context)

        val view = LayoutInflater.from(context).inflate(R.layout.dialog_search_engine, null)
        val etUrl = view.findViewById<EditText>(R.id.etUrl)
        val llUrl = view.findViewById<LinearLayout>(R.id.llURL)

        val adapter = ArrayAdapter(
            context,
            android.R.layout.simple_spinner_item,
            AppSettings.SearchEnginesTitles
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        val spEngine = view.findViewById<Spinner>(R.id.spEngine)
        spEngine.adapter = adapter

        if (selected != -1 && selected < AppSettings.SearchEnginesURLs.size - 1) {
            spEngine.setSelection(selected)
            etUrl.setText(AppSettings.SearchEnginesURLs[selected])
        } else {
            spEngine.setSelection(AppSettings.SearchEnginesTitles.size - 1)
            llUrl.visibility = View.VISIBLE
            etUrl.setText(currentUrl)
            etUrl.requestFocus()
        }

        spEngine.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                if (position == AppSettings.SearchEnginesTitles.size - 1 && llUrl.isGone) {
                    llUrl.visibility = View.VISIBLE
                    llUrl.startAnimation(AnimationUtils.loadAnimation(context, android.R.anim.fade_in))
                    etUrl.requestFocus()
                }
                if (position < AppSettings.SearchEnginesURLs.size) {
                    etUrl.setText(AppSettings.SearchEnginesURLs[position])
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        builder.setView(view)
            .setCancelable(cancellable)
            .setTitle(R.string.engine)
            .setPositiveButton(R.string.save) { _, _ ->
                val url = etUrl.text.toString()
                settings.setSearchEngineURL(url)
                callback.onDone(url)
            }
            .show()
    }
}