package org.mlm.browkorftv.widgets.cursor

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.KeyEvent
import android.view.MotionEvent
import android.widget.FrameLayout
import org.mlm.browkorftv.utils.DPADNavigationEventsAdapter
import org.mlm.browkorftv.utils.NavigationReservedShortcutKeyCodes


/**
 * Created by PDT on 25.08.2016.
 */
class CursorLayout @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null):
    FrameLayout(context, attrs) {
    lateinit var cursorDrawerDelegate: CursorDrawerDelegate
    private val inputEventsAdapter = DPADNavigationEventsAdapter(
        onEmulatedKeyEvent = { keyEvent ->
            cursorDrawerDelegate.dispatchKeyEvent(keyEvent)
        }
    )

    init {
        init()
    }

    private fun init() {
        if (isInEditMode) {
            return
        }
        setWillNotDraw(false)
        cursorDrawerDelegate = CursorDrawerDelegate(context, this)
        cursorDrawerDelegate.init()
    }

    fun consumeBackIfCursorModeActive(): Boolean {
        // Delegate exits modes on ACTION_UP for BACK/ESC/B
        return cursorDrawerDelegate.dispatchKeyEvent(
            KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_BACK)
        )
    }

    override fun setWillNotDraw(willNotDraw: Boolean) {
        inputEventsAdapter.resetState()
        super.setWillNotDraw(willNotDraw)
    }

    override fun onSizeChanged(w: Int, h: Int, ow: Int, oh: Int) {
        super.onSizeChanged(w, h, ow, oh)
        if (isInEditMode || willNotDraw()) {
            return
        }
        cursorDrawerDelegate.onSizeChanged(w, h, ow, oh)
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (willNotDraw()) return super.dispatchKeyEvent(event)

        if (inputEventsAdapter.dispatchKeyEvent(event)) {
            return true
        }

        if (!NavigationReservedShortcutKeyCodes.dpadNavigationKeys.contains(event.keyCode) &&
            cursorDrawerDelegate.dispatchKeyEvent(event)
        ) {
            return true
        }

        return super.dispatchKeyEvent(event)
    }

    override fun dispatchGenericMotionEvent(event: MotionEvent): Boolean {
        if (willNotDraw()) return super.dispatchGenericMotionEvent(event)

        if (inputEventsAdapter.dispatchGenericMotionEvent(event)) {
            return true
        }

        return super.dispatchGenericMotionEvent(event)
    }

    override fun dispatchDraw(canvas: Canvas) {
        super.dispatchDraw(canvas)
        if (isInEditMode || willNotDraw()) {
            return
        }

        cursorDrawerDelegate.dispatchDraw(canvas)
    }
}
