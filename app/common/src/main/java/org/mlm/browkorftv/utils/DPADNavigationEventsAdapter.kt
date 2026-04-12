package org.mlm.browkorftv.utils

import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent

/**
 * Normalizes DPAD-style navigation events coming from either KeyEvents or controller motion axes.
 * Emits synthetic KeyEvents via [onEmulatedKeyEvent].
 */
class DPADNavigationEventsAdapter(
    private val onEmulatedKeyEvent: (KeyEvent) -> Boolean,
    private val motionAxesTranslationEnabled: () -> Boolean = { true },
    private val isSoftwareKeyboardVisible: () -> Boolean = { false },
) {
    private val allowedKeyCodes: Set<Int> = NavigationReservedShortcutKeyCodes.dpadNavigationKeys

    private var keyboardDirectionalActive = false

    private enum class DpadChannel { KEY, MOTION }

    private val lastDpadDownEventTimeByChannel = LongArray(DpadChannel.entries.size) { -1L }
    private val suppressOtherChannelsTimeoutMs: Long = 1000L

    private var hatXState = 0
    private var hatYState = 0

    private fun isKeyAllowed(keyCode: Int): Boolean = allowedKeyCodes.contains(keyCode)

    private fun isControllerSource(source: Int): Boolean {
        return (source and InputDevice.SOURCE_JOYSTICK) != 0 ||
            (source and InputDevice.SOURCE_GAMEPAD) != 0
    }

    private fun sourceHasDpad(source: Int): Boolean {
        return (source and InputDevice.SOURCE_DPAD) != 0
    }

    private fun shouldSkipMotionTranslation(event: MotionEvent): Boolean {
        if (sourceHasDpad(event.source) || event.getAxisValue(MotionEvent.AXIS_HAT_X) != 0f || event.getAxisValue(MotionEvent.AXIS_HAT_Y) != 0f) {
            return true
        }

        val inputDevice = event.device ?: return false
        val hasTouchPad = inputDevice.motionRanges.any { range ->
            range.axis == MotionEvent.AXIS_X || range.axis == MotionEvent.AXIS_Y
        }
        return hasTouchPad
    }

    private fun shouldSuppress(from: DpadChannel, eventTime: Long): Boolean {
        fun recent(other: DpadChannel): Boolean {
            val t = lastDpadDownEventTimeByChannel[other.ordinal]
            if (t < 0L) return false
            val dt = eventTime - t
            return dt in 0L..suppressOtherChannelsTimeoutMs
        }

        return when (from) {
            DpadChannel.KEY -> recent(DpadChannel.MOTION)
            DpadChannel.MOTION -> recent(DpadChannel.KEY)
        }
    }

    private fun markDpadDownGenerated(from: DpadChannel, eventTime: Long) {
        lastDpadDownEventTimeByChannel[from.ordinal] = eventTime
    }

    private fun emitSimpleKeyPress(keyCode: Int, sourceEvent: MotionEvent): Boolean {
        val now = sourceEvent.eventTime
        val down = KeyEvent(
            sourceEvent.downTime,
            now,
            KeyEvent.ACTION_DOWN,
            keyCode,
            0,
            0,
            sourceEvent.deviceId,
            0,
            0,
            sourceEvent.source
        )
        val up = KeyEvent(
            sourceEvent.downTime,
            now,
            KeyEvent.ACTION_UP,
            keyCode,
            0,
            0,
            sourceEvent.deviceId,
            0,
            0,
            sourceEvent.source
        )
        val downHandled = onEmulatedKeyEvent(down)
        val upHandled = onEmulatedKeyEvent(up)
        return downHandled || upHandled
    }

    fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (isSoftwareKeyboardVisible() && event.keyCode in intArrayOf(
                KeyEvent.KEYCODE_DPAD_LEFT,
                KeyEvent.KEYCODE_DPAD_RIGHT,
                KeyEvent.KEYCODE_DPAD_UP,
                KeyEvent.KEYCODE_DPAD_DOWN,
                KeyEvent.KEYCODE_DPAD_CENTER,
                KeyEvent.KEYCODE_ENTER,
                KeyEvent.KEYCODE_NUMPAD_ENTER,
                KeyEvent.KEYCODE_BUTTON_A
            )) {
            return false
        }

        if (!isKeyAllowed(event.keyCode)) return false

        val fromController = isControllerSource(event.source)
        if (fromController && keyboardDirectionalActive && event.keyCode in intArrayOf(
                KeyEvent.KEYCODE_DPAD_LEFT,
                KeyEvent.KEYCODE_DPAD_RIGHT,
                KeyEvent.KEYCODE_DPAD_UP,
                KeyEvent.KEYCODE_DPAD_DOWN
            )) {
            return false
        }

        if (event.action == KeyEvent.ACTION_DOWN && !fromController && event.repeatCount == 0 && event.keyCode in intArrayOf(
                KeyEvent.KEYCODE_DPAD_LEFT,
                KeyEvent.KEYCODE_DPAD_RIGHT,
                KeyEvent.KEYCODE_DPAD_UP,
                KeyEvent.KEYCODE_DPAD_DOWN,
                KeyEvent.KEYCODE_DPAD_UP_LEFT,
                KeyEvent.KEYCODE_DPAD_UP_RIGHT,
                KeyEvent.KEYCODE_DPAD_DOWN_LEFT,
                KeyEvent.KEYCODE_DPAD_DOWN_RIGHT
            )) {
            keyboardDirectionalActive = true
        }
        if (event.action == KeyEvent.ACTION_UP && !fromController) {
            keyboardDirectionalActive = false
        }

        if (event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0) {
            if (shouldSuppress(DpadChannel.KEY, event.eventTime)) return false
            markDpadDownGenerated(DpadChannel.KEY, event.eventTime)
        }

        return onEmulatedKeyEvent(event)
    }

    fun dispatchGenericMotionEvent(event: MotionEvent): Boolean {
        if (!motionAxesTranslationEnabled()) return false
        if (isSoftwareKeyboardVisible()) return false

        if (event.action != MotionEvent.ACTION_MOVE &&
            event.action != MotionEvent.ACTION_HOVER_MOVE &&
            event.action != MotionEvent.ACTION_SCROLL
        ) {
            return false
        }

        if (!isControllerSource(event.source)) return false
        if (keyboardDirectionalActive) return false
        if (shouldSkipMotionTranslation(event)) return false

        val now = event.eventTime
        var emitted = false

        val hatX = event.getAxisValue(MotionEvent.AXIS_HAT_X)
        val hatY = event.getAxisValue(MotionEvent.AXIS_HAT_Y)
        val axisX = event.getAxisValue(MotionEvent.AXIS_X)
        val axisY = event.getAxisValue(MotionEvent.AXIS_Y)

        val deadZone = 0.5f

        val nextHatX = when {
            hatX > deadZone -> 1
            hatX < -deadZone -> -1
            else -> 0
        }
        val nextHatY = when {
            hatY > deadZone -> 1
            hatY < -deadZone -> -1
            else -> 0
        }
        val nextAxisX = when {
            axisX > deadZone -> 1
            axisX < -deadZone -> -1
            else -> 0
        }
        val nextAxisY = when {
            axisY > deadZone -> 1
            axisY < -deadZone -> -1
            else -> 0
        }

        val nextX = if (nextHatX != 0) nextHatX else nextAxisX
        val nextY = if (nextHatY != 0) nextHatY else nextAxisY

        fun emitPressIfChanged(prev: Int, next: Int, positiveKey: Int, negativeKey: Int): Boolean {
            if (prev == next || next == 0) return false
            if (shouldSuppress(DpadChannel.MOTION, now)) return false
            markDpadDownGenerated(DpadChannel.MOTION, now)

            val keyCode = if (next > 0) positiveKey else negativeKey
            return emitSimpleKeyPress(keyCode, event)
        }

        if (emitPressIfChanged(hatXState, nextX, KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent.KEYCODE_DPAD_LEFT)) {
            emitted = true
        }
        if (emitPressIfChanged(hatYState, nextY, KeyEvent.KEYCODE_DPAD_DOWN, KeyEvent.KEYCODE_DPAD_UP)) {
            emitted = true
        }

        hatXState = nextX
        hatYState = nextY
        return emitted
    }

    fun resetState() {
        keyboardDirectionalActive = false
        hatXState = 0
        hatYState = 0
        for (i in lastDpadDownEventTimeByChannel.indices) {
            lastDpadDownEventTimeByChannel[i] = -1L
        }
    }
}
