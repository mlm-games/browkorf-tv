package org.mlm.browkorftv.webengine.gecko

import android.content.Context
import android.content.Context.INPUT_METHOD_SERVICE
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Point
import android.graphics.PointF
import android.graphics.Rect
import android.os.SystemClock
import android.util.AttributeSet
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.MotionEvent.PointerProperties
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import org.mlm.browkorftv.utils.Utils
import org.mlm.browkorftv.utils.dip2px
import org.mlm.browkorftv.widgets.cursor.CursorDrawerDelegate
import org.mozilla.geckoview.ScreenLength


class GeckoViewWithVirtualCursor @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null):
    GeckoViewEx(context, attrs) {
    lateinit var cursorDrawerDelegate: CursorDrawerDelegate

    private var inputMethodManager: InputMethodManager? = null

    init {
        init()
    }

    private fun init() {
        if (isInEditMode) {
            return
        }
        setWillNotDraw(false)
        overScrollMode = OVER_SCROLL_NEVER
        inputMethodManager = context.getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager?
        cursorDrawerDelegate = CursorDrawerDelegate(context, this)
        cursorDrawerDelegate.customScrollCallback = object : CursorDrawerDelegate.CustomScrollCallback {
            override fun onScroll(scrollX: Int, scrollY: Int): Boolean {
                return session?.let {
                    it.panZoomController.scrollBy(ScreenLength.fromPixels(scrollX.dip2px(context).toDouble()), ScreenLength.fromPixels(scrollY.dip2px(context).toDouble()))
                    true
                } ?: false
            }
        }
        cursorDrawerDelegate.init()
    }

    override fun onSizeChanged(w: Int, h: Int, ow: Int, oh: Int) {
        super.onSizeChanged(w, h, ow, oh)
        if (isInEditMode) {
            return
        }
        cursorDrawerDelegate.onSizeChanged(w, h, ow, oh)
    }

    override fun onGenericMotionEvent(event: MotionEvent?): Boolean {
        return false
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (inputMethodManager?.isAcceptingText == true && event.keyCode == KeyEvent.KEYCODE_ESCAPE) {
            return super.dispatchKeyEvent(event)
        }

        if (cursorDrawerDelegate.dispatchKeyEvent(event)) {
            return true
        }

        when (event.keyCode) {
            KeyEvent.KEYCODE_ESCAPE, KeyEvent.KEYCODE_BUTTON_B, KeyEvent.KEYCODE_BACK -> {
                return false//prevent capturing this keys by geckoview and let activity handle them
            }
        }

        return super.dispatchKeyEvent(event)
    }

    override fun dispatchDraw(canvas: Canvas) {
        super.dispatchDraw(canvas)
        if (isInEditMode) {
            return
        }

        cursorDrawerDelegate.dispatchDraw(canvas)
    }
}