package com.phlox.tvwebbrowser.singleton.shortcuts

import android.content.Context
import android.content.SharedPreferences
import com.phlox.tvwebbrowser.TVBro
import com.phlox.tvwebbrowser.Config
import java.util.HashMap

class ShortcutMgr private constructor() {

    interface ShortcutHandler {
        fun toggleMenu()
        fun navigateBack()
        fun navigateHome()
        fun refreshPage()
        fun voiceSearch()
    }

    private val shortcuts: MutableMap<Int, Shortcut>
    private val prefs: SharedPreferences

    init {
        shortcuts = HashMap()
        prefs = TVBro.instance.getSharedPreferences(PREFS_SHORTCUTS, Context.MODE_PRIVATE)

        for (shortcut in Shortcut.values()) {
            shortcut.keyCode = prefs.getInt(shortcut.prefsKey, shortcut.keyCode)
            if (shortcut.keyCode != 0) {
                shortcuts[shortcut.keyCode] = shortcut
            }
        }
    }

    fun save(shortcut: Shortcut) {
        var oldKey = 0
        for ((key, value) in shortcuts) {
            if (value == shortcut) oldKey = key
        }
        if (oldKey != 0) shortcuts.remove(oldKey)

        if (shortcut.keyCode != 0) shortcuts[shortcut.keyCode] = shortcut

        prefs.edit()
            .putInt(shortcut.prefsKey, shortcut.keyCode)
            .apply()
    }

    fun findForId(id: Int): Shortcut? {
        for ((_, value) in shortcuts) {
            if (value.itemId == id) return value
        }
        return Shortcut.findForMenu(id)
    }

    fun canProcessKeyCode(keyCode: Int): Boolean = shortcuts[keyCode] != null

    fun process(keyCode: Int, handler: ShortcutHandler): Boolean {
        val shortcut = shortcuts[keyCode] ?: return false
        when (shortcut) {
            Shortcut.MENU -> handler.toggleMenu()
            Shortcut.NAVIGATE_BACK -> handler.navigateBack()
            Shortcut.NAVIGATE_HOME -> handler.navigateHome()
            Shortcut.REFRESH_PAGE -> handler.refreshPage()
            Shortcut.VOICE_SEARCH -> handler.voiceSearch()
        }
        return true
    }

    companion object {
        const val PREFS_SHORTCUTS = "shortcuts"
        private var instance: ShortcutMgr? = null

        @Synchronized
        fun getInstance(): ShortcutMgr {
            if (instance == null) instance = ShortcutMgr()
            return instance!!
        }
    }
}