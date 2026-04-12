package org.mlm.browkorftv.utils

import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import kotlin.math.max

/**
 * To avoid duplicate handling, events emitted by one channel suppress the other for a short time window.
 */
class BackNavigationEventsAdapter(
    private val onEmulatedBackEvent: () -> Boolean,
    private val longPressTimeoutMs: Int = DEFAULT_LONG_PRESS_TIMEOUT_MS,
) {
    init {
        require(longPressTimeoutMs > 0) { "longPressTimeoutMs must be > 0" }
    }

    private val allowedKeyCodes: Set<Int> = NavigationReservedShortcutKeyCodes.backNavigationKeys

    private var lastBackDown: Boolean = false
    private var backKeyDownTime: Long = 0L
    private var backLongPressEmitted: Boolean = false

    private enum class BackChannel { KEY, MOTION }

    private var pendingBackDown: Boolean = false
    private val lastBackDownEventTimeByChannel = LongArray(BackChannel.entries.size) { -1L }
    private val suppressOtherChannelsTimeoutMs: Long = 1000L

    private fun isKeyAllowed(keyCode: Int): Boolean = allowedKeyCodes.contains(keyCode)

    private fun isControllerSource(source: Int): Boolean {
        return (source and InputDevice.SOURCE_JOYSTICK) != 0 ||
            (source and InputDevice.SOURCE_GAMEPAD) != 0
    }

    private fun shouldSuppress(from: BackChannel, eventTime: Long): Boolean {
        fun recent(other: BackChannel): Boolean {
            val t = lastBackDownEventTimeByChannel[other.ordinal]
            if (t < 0L) return false
            val dt = eventTime - t
            return dt in 0L..suppressOtherChannelsTimeoutMs
        }

        return when (from) {
            BackChannel.KEY -> recent(BackChannel.MOTION)
            BackChannel.MOTION -> recent(BackChannel.KEY)
        }
    }

    private fun markBackDownGenerated(from: BackChannel, eventTime: Long) {
        lastBackDownEventTimeByChannel[from.ordinal] = eventTime
    }

    fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (!isKeyAllowed(event.keyCode)) return false
        if (shouldSuppress(BackChannel.KEY, event.eventTime)) return false

        return when (event.action) {
            KeyEvent.ACTION_DOWN -> {
                if (event.repeatCount == 0) {
                    pendingBackDown = true
                    markBackDownGenerated(BackChannel.KEY, event.eventTime)
                }
                false
            }

            KeyEvent.ACTION_UP -> {
                if (pendingBackDown) {
                    pendingBackDown = false
                    onEmulatedBackEvent()
                } else {
                    false
                }
            }

            else -> false
        }
    }

    fun dispatchGenericMotionEvent(event: MotionEvent): Boolean {
        if (!isControllerSource(event.source)) return false

        val buttonState = event.buttonState
        val backPressedNow =
            (buttonState and MotionEvent.BUTTON_SECONDARY) != 0 ||
                (buttonState and MotionEvent.BUTTON_BACK) != 0

        var emittedAny = false
        val suppressed = shouldSuppress(BackChannel.MOTION, event.eventTime)

        if (backPressedNow && !lastBackDown) {
            backKeyDownTime = event.downTime
            backLongPressEmitted = false
            if (!suppressed) {
                pendingBackDown = true
                markBackDownGenerated(BackChannel.MOTION, event.eventTime)
            }
        } else if (!backPressedNow && lastBackDown) {
            if (!suppressed && pendingBackDown) {
                pendingBackDown = false
                emittedAny = onEmulatedBackEvent()
            }
            backLongPressEmitted = false
        } else if (backPressedNow && lastBackDown && !backLongPressEmitted) {
            val elapsed = max(0L, event.eventTime - backKeyDownTime)
            if (elapsed >= longPressTimeoutMs) {
                backLongPressEmitted = true
            }
        }

        lastBackDown = backPressedNow
        return emittedAny
    }

    fun resetState() {
        lastBackDown = false
        backKeyDownTime = 0L
        backLongPressEmitted = false
        pendingBackDown = false
        for (i in lastBackDownEventTimeByChannel.indices) {
            lastBackDownEventTimeByChannel[i] = -1L
        }
    }

    companion object {
        private const val DEFAULT_LONG_PRESS_TIMEOUT_MS: Int = 500
    }
}
