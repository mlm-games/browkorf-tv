package org.mlm.browkorftv.singleton.shortcuts

import android.content.Context
import android.view.KeyEvent
import org.mlm.browkorftv.R

enum class Shortcut(
    val titleResId: Int,
    val prefsKey: String,
    val defaultKeyCode: Int,
    val defaultModifiers: Int = 0,
    val defaultLongPress: Boolean = false
) {
    MENU(R.string.toggle_main_menu, "shortcut_menu", KeyEvent.KEYCODE_MENU),

    // These default to "unassigned" (0) so user can choose keys.
    NAVIGATE_BACK(R.string.navigate_back, "shortcut_nav_back", 0),
    NAVIGATE_HOME(R.string.navigate_home, "shortcut_nav_home", 0),

    REFRESH_PAGE(R.string.refresh_page, "shortcut_refresh_page", KeyEvent.KEYCODE_REFRESH),

    VOICE_SEARCH(R.string.voice_search, "shortcut_voice_search", KeyEvent.KEYCODE_SEARCH),

    PLAY_PAUSE(R.string.play_pause, "shortcut_play_pause", KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE),
    MEDIA_STOP(R.string.media_stop, "shortcut_media_stop", KeyEvent.KEYCODE_MEDIA_STOP),
    MEDIA_REWIND(R.string.media_rewind, "shortcut_media_rewind", KeyEvent.KEYCODE_MEDIA_REWIND),
    MEDIA_FAST_FORWARD(
        R.string.media_fast_forward,
        "shortcut_media_fast_forward",
        KeyEvent.KEYCODE_MEDIA_FAST_FORWARD
    );

    companion object {
        fun shortcutKeysToString(
            shortcut: Shortcut,
            binding: ShortcutMgr.Binding,
            context: Context
        ): String {
            if (binding.keyCode == 0) return "—"

            val sb = StringBuilder()
            if (binding.longPress) {
                sb.append(context.getString(R.string.long_press)).append(' ')
            }

            if (binding.modifiers != 0) {
                if (binding.modifiers and KeyEvent.META_ALT_ON != 0) sb.append("ALT+")
                if (binding.modifiers and KeyEvent.META_CTRL_ON != 0) sb.append("CTRL+")
                if (binding.modifiers and KeyEvent.META_SHIFT_ON != 0) sb.append("SHIFT+")
            }

            sb.append(KeyEvent.keyCodeToString(binding.keyCode).removePrefix("KEYCODE_"))
            return sb.toString()
        }
    }
}