package org.mlm.browkorftv.utils

import android.view.KeyEvent

/**
 * configurable shortcuts should not use these.
 */
object NavigationReservedShortcutKeyCodes {
    val dpadNavigationKeys: Set<Int> = intArrayOf(
        KeyEvent.KEYCODE_DPAD_LEFT,
        KeyEvent.KEYCODE_DPAD_RIGHT,
        KeyEvent.KEYCODE_DPAD_UP,
        KeyEvent.KEYCODE_DPAD_DOWN,
        KeyEvent.KEYCODE_DPAD_UP_LEFT,
        KeyEvent.KEYCODE_DPAD_UP_RIGHT,
        KeyEvent.KEYCODE_DPAD_DOWN_LEFT,
        KeyEvent.KEYCODE_DPAD_DOWN_RIGHT,
        KeyEvent.KEYCODE_DPAD_CENTER,
        KeyEvent.KEYCODE_ENTER,
        KeyEvent.KEYCODE_NUMPAD_ENTER,
        KeyEvent.KEYCODE_BUTTON_A
    ).toHashSet()

    val backNavigationKeys: Set<Int> = intArrayOf(
        KeyEvent.KEYCODE_BACK,
        KeyEvent.KEYCODE_BUTTON_B
    ).toHashSet()

    val reservedForUserShortcuts: Set<Int> = dpadNavigationKeys + backNavigationKeys
}
