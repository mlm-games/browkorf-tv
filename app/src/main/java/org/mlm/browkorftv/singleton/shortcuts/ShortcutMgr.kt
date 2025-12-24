package org.mlm.browkorftv.singleton.shortcuts

import android.content.Context
import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import androidx.annotation.UiThread
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.mlm.browkorftv.activity.main.MainActivity
import org.mlm.browkorftv.model.WebTabState
import org.mlm.browkorftv.settings.AppSettings.Companion.HOME_URL_ALIAS
import org.mlm.browkorftv.webengine.WebEngine
import androidx.core.content.edit

class ShortcutMgr(
    context: Context
) {
    data class Binding(
        val keyCode: Int,
        val modifiers: Int,
        val longPress: Boolean
    )

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_SHORTCUTS, Context.MODE_PRIVATE)

    private val uiHandler = Handler(Looper.getMainLooper())

    private val _bindings = MutableStateFlow(loadAllBindings())
    val bindings: StateFlow<Map<Shortcut, Binding>> = _bindings.asStateFlow()

    private var trackingShortcuts: List<Shortcut>? = null

    private fun loadAllBindings(): Map<Shortcut, Binding> {
        val map = LinkedHashMap<Shortcut, Binding>()
        for (s in Shortcut.entries) {
            val keyCode = prefs.getInt(s.prefsKey, s.defaultKeyCode)
            val modifiers = prefs.getInt("${s.prefsKey}_mod", s.defaultModifiers)
            val longPress = prefs.getBoolean("${s.prefsKey}_lp", s.defaultLongPress)
            map[s] = Binding(keyCode, modifiers, longPress)
        }
        return map
    }

    fun bindingFor(shortcut: Shortcut): Binding =
        _bindings.value[shortcut] ?: Binding(
            shortcut.defaultKeyCode,
            shortcut.defaultModifiers,
            shortcut.defaultLongPress
        )

    /**
     * Update a shortcut binding. If keyCode==0 => unassign (also clears modifiers/longPress).
     */
    fun updateBinding(
        shortcut: Shortcut,
        keyCode: Int,
        modifiers: Int = 0,
        longPress: Boolean = false
    ) {
        if (keyCode == 0) {
            prefs.edit {
                remove(shortcut.prefsKey)
                    .remove("${shortcut.prefsKey}_mod")
                    .remove("${shortcut.prefsKey}_lp")
            }

            _bindings.value = _bindings.value.toMutableMap().apply {
                put(shortcut, Binding(0, 0, false))
            }
            return
        }

        prefs.edit {
            putInt(shortcut.prefsKey, keyCode)
                .putInt("${shortcut.prefsKey}_mod", modifiers)
                .putBoolean("${shortcut.prefsKey}_lp", longPress)
        }

        _bindings.value = _bindings.value.toMutableMap().apply {
            put(shortcut, Binding(keyCode, modifiers, longPress))
        }
    }

    /**
     * Only keep ALT/CTRL/SHIFT.
     * (Some devices add other meta bits; equality matching becomes unreliable unless normalized.)
     */
    private fun eventModifiers(event: KeyEvent): Int {
        val normalized = KeyEvent.normalizeMetaState(event.metaState)
        val mask = KeyEvent.META_ALT_ON or KeyEvent.META_CTRL_ON or KeyEvent.META_SHIFT_ON
        return normalized and mask
    }

    private fun shortcutsForEvent(event: KeyEvent): List<Shortcut> {
        val mods = eventModifiers(event)
        val map = _bindings.value
        return Shortcut.entries.filter { s ->
            val b = map[s] ?: return@filter false
            b.keyCode != 0 && b.keyCode == event.keyCode && b.modifiers == mods
        }
    }

    private fun isAnyShortcutForEvent(event: KeyEvent): Boolean {
        val mods = eventModifiers(event)
        val map = _bindings.value
        return Shortcut.entries.any { s ->
            val b = map[s] ?: return@any false
            b.keyCode != 0 && b.keyCode == event.keyCode && b.modifiers == mods
        }
    }

    @UiThread
    fun process(shortcut: Shortcut, mainActivity: MainActivity, webEngine: WebEngine?) {
        when (shortcut) {
            Shortcut.MENU -> mainActivity.toggleMenu() // should open/close overlay menu now

            Shortcut.NAVIGATE_BACK -> mainActivity.navigateBack()
            Shortcut.NAVIGATE_HOME -> mainActivity.navigate(HOME_URL_ALIAS)
            Shortcut.REFRESH_PAGE -> mainActivity.refresh()
            Shortcut.VOICE_SEARCH -> mainActivity.initiateVoiceSearch()

            Shortcut.PLAY_PAUSE -> webEngine?.togglePlayback()
            Shortcut.MEDIA_STOP -> webEngine?.stopPlayback()
            Shortcut.MEDIA_REWIND -> webEngine?.rewind()
            Shortcut.MEDIA_FAST_FORWARD -> webEngine?.fastForward()
        }
    }

    private fun onKeyDown(event: KeyEvent, mainActivity: MainActivity, tab: WebTabState?): Boolean {
        if (!isAnyShortcutForEvent(event)) return false

        trackingShortcuts = shortcutsForEvent(event)
        if (event.repeatCount == 0) event.startTracking()

        if (event.isLongPress) {
            return onKeyLongPress(event, mainActivity, tab)
        }
        return true
    }

    private fun onKeyUp(event: KeyEvent, mainActivity: MainActivity, tab: WebTabState?): Boolean {
        val tracking = trackingShortcuts ?: return false
        val map = _bindings.value
        val mods = eventModifiers(event)

        for (shortcut in tracking) {
            val b = map[shortcut] ?: continue
            if (b.longPress) continue
            if (b.modifiers != mods) continue

            uiHandler.post { process(shortcut, mainActivity, tab?.webEngine) }
            trackingShortcuts = null
            return true
        }
        return false
    }

    private fun onKeyLongPress(
        event: KeyEvent,
        mainActivity: MainActivity,
        tab: WebTabState?
    ): Boolean {
        val tracking = trackingShortcuts ?: return false
        val map = _bindings.value
        val mods = eventModifiers(event)

        for (shortcut in tracking) {
            val b = map[shortcut] ?: continue
            if (!b.longPress) continue
            if (b.modifiers != mods) continue

            uiHandler.post { process(shortcut, mainActivity, tab?.webEngine) }
            trackingShortcuts = null
            return true
        }
        return false
    }

    fun handle(event: KeyEvent, mainActivity: MainActivity, tab: WebTabState?): Boolean {
        return when (event.action) {
            KeyEvent.ACTION_DOWN -> onKeyDown(event, mainActivity, tab)
            KeyEvent.ACTION_UP -> onKeyUp(event, mainActivity, tab)
            else -> false
        }
    }

    companion object {
        const val PREFS_SHORTCUTS = "shortcuts"
    }
}