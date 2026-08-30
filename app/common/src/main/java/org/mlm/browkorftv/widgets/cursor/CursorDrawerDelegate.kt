package org.mlm.browkorftv.widgets.cursor

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Point
import android.graphics.PointF
import android.graphics.Rect
import android.os.SystemClock
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import org.mlm.browkorftv.utils.Utils
import kotlin.math.abs

class CursorDrawerDelegate(val context: Context, val surface: View) {
    var enabled: Boolean = true
    var isLongPressMenuEnabled: Boolean = true

    /**
     * When true, D-pad directions are passed through to the webpage as arrow key events
     * instead of moving the cursor. Useful for games and interactive web apps.
     */
    var directionalNavMode: Boolean = false
        set(value) {
            if (field == value) return
            field = value
            // Stop any ongoing cursor movement
            stopCursorMovement()
            surface.invalidate()
        }

    private var cursorRadius: Int = 0
    private var cursorRadiusPressed: Int = 0
    private var maxCursorSpeed: Float = 0f
    private var scrollStartPadding = 100
    private var cursorStrokeWidth: Float = 0f
    private val cursorDirection = Point(0, 0)
    val cursorPosition = PointF(0f, 0f)
    private val cursorSpeed = PointF(0f, 0f)
    private val paint = Paint()
    private var lastCursorUpdate = 0L  // Start with 0 so cursor is hidden initially
    private var dpadCenterPressed = false
    internal var tmpPointF = PointF()
    var callback: Callback? = null
    var customScrollCallback: CustomScrollCallback? = null
    var textSelectionCallback: TextSelectionCallback? = null
    var directionalNavCallback: DirectionalNavCallback? = null
    private val cursorHideRunnable = Runnable { surface.invalidate() }
    private var scrollHackStarted = false
    private val scrollHackCoords = PointF()
    private val scrollHackActiveRect = Rect()
    private var grabMode = false
    var textSelectionMode = false
    private var downTime: Long = 0L

    /**
     * When true, the long-press context menu is active.
     * Center button events should be consumed, D-pad events pass through for menu navigation.
     */
    private var longPressMenuActive = false

    /**
     * Tracks if we're currently in a center button press sequence.
     * Used to properly handle the UP event after long press.
     */
    private var centerButtonDownTime = 0L

    private val longPressRunnable = Runnable {
        if (!isLongPressMenuEnabled) return@Runnable
        // Cancel any ongoing touch on the surface first
        if (dpadCenterPressed) {
            dispatchMotionEvent(cursorPosition.x, cursorPosition.y, MotionEvent.ACTION_CANCEL)
            dpadCenterPressed = false
        }

        surface.keyDispatcherState.reset(this@CursorDrawerDelegate)
        grabMode = false
        longPressMenuActive = true

        // Stop cursor movement
        stopCursorMovement()

        callback?.onLongPress(cursorPosition.x.toInt(), cursorPosition.y.toInt())
    }

    private val isCursorDisappear: Boolean
        get() {
            if (lastCursorUpdate == 0L) return true
            val newTime = SystemClock.uptimeMillis()
            return newTime - lastCursorUpdate > CURSOR_DISAPPEAR_TIMEOUT
        }

    /**
     * Check if cursor should be hidden (menu active or directional nav mode)
     */
    private val shouldHideCursor: Boolean
        get() = longPressMenuActive || directionalNavMode

    /**
     * Check if cursor movement should be blocked
     */
    private val isCursorMovementBlocked: Boolean
        get() = longPressMenuActive || directionalNavMode

    interface Callback {
        fun onLongPress(x: Int, y: Int)
    }

    interface CustomScrollCallback {
        fun onScroll(scrollX: Int, scrollY: Int): Boolean
    }

    interface TextSelectionCallback {
        fun onTextSelectionStart(x: Int, y: Int)
        fun onTextSelectionMove(x: Int, y: Int)
        fun onTextSelectionEnd(x: Int, y: Int)
        fun onTextSelectionCancel()
    }

    /**
     * Callback for directional navigation mode - sends key events to webpage
     */
    interface DirectionalNavCallback {
        fun onDirectionalKey(keyCode: Int, action: Int): Boolean
    }

    fun init() {
        paint.isAntiAlias = true
    }

    private fun stopCursorMovement() {
        cursorDirection.set(0, 0)
        cursorSpeed.set(0f, 0f)
        surface.removeCallbacks(cursorUpdateRunnable)
        surface.removeCallbacks(longPressRunnable)
        if (dpadCenterPressed) {
            dispatchMotionEvent(cursorPosition.x, cursorPosition.y, MotionEvent.ACTION_CANCEL)
            dpadCenterPressed = false
        }
        if (scrollHackStarted) {
            dispatchMotionEvent(scrollHackCoords.x, scrollHackCoords.y, MotionEvent.ACTION_CANCEL)
            scrollHackStarted = false
        }
    }

    /**
     * Call this when the long-press menu (cursor menu) is dismissed
     * to re-enable normal input handling.
     */
    fun onMenuDismissed() {
        longPressMenuActive = false
        centerButtonDownTime = 0L
        surface.postInvalidate()
    }

    /**
     * Check if the long-press menu is currently active
     */
    fun isMenuActive(): Boolean = longPressMenuActive

    fun onSizeChanged(w: Int, h: Int, ow: Int, oh: Int) {
        cursorStrokeWidth = (w / 400).toFloat()
        cursorRadius = w / 100
        cursorRadiusPressed = cursorRadius - Utils.D2P(context, 5f).toInt()
        maxCursorSpeed = (w / 25).toFloat()
        scrollStartPadding = w / 15

        cursorPosition.set(w / 2.0f, h / 2.0f)
        scrollHackActiveRect.set(0, 0, w, h)
        scrollHackActiveRect.inset(SCROLL_HACK_PADDING, SCROLL_HACK_PADDING)
        surface.postDelayed(cursorHideRunnable, CURSOR_DISAPPEAR_TIMEOUT.toLong())
    }

    fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (!enabled) return false

        val keyCode = event.keyCode
        val action = event.action

        // Handle long press menu active state
        if (longPressMenuActive) {
            when (keyCode) {
                // Center/Enter buttons - CONSUME to prevent click on WebView underneath
                KeyEvent.KEYCODE_DPAD_CENTER,
                KeyEvent.KEYCODE_ENTER,
                KeyEvent.KEYCODE_NUMPAD_ENTER,
                KeyEvent.KEYCODE_BUTTON_A -> {
                    // Consume but don't process - let Compose handle menu selection
                    return false
                }

                // D-pad navigation - let pass through for menu navigation
                KeyEvent.KEYCODE_DPAD_LEFT,
                KeyEvent.KEYCODE_DPAD_RIGHT,
                KeyEvent.KEYCODE_DPAD_UP,
                KeyEvent.KEYCODE_DPAD_DOWN -> {
                    return false
                }

                // Back/Escape - let pass through to dismiss menu
                KeyEvent.KEYCODE_ESCAPE,
                KeyEvent.KEYCODE_BUTTON_B,
                KeyEvent.KEYCODE_BACK -> {
                    return false
                }
            }
            // Consume all other keys while menu is active
            return true
        }

        // Handle directional navigation mode
        if (directionalNavMode) {
            when (keyCode) {
                KeyEvent.KEYCODE_DPAD_LEFT,
                KeyEvent.KEYCODE_DPAD_RIGHT,
                KeyEvent.KEYCODE_DPAD_UP,
                KeyEvent.KEYCODE_DPAD_DOWN -> {
                    // Pass to callback for injection into WebView
                    directionalNavCallback?.onDirectionalKey(keyCode, action)
                    // Always consume to prevent any cursor movement
                    return true
                }
            }
            // Other keys in directional mode - don't intercept
        }

        // Normal cursor mode handling
        when (keyCode) {
            KeyEvent.KEYCODE_ESCAPE,
            KeyEvent.KEYCODE_BUTTON_B,
            KeyEvent.KEYCODE_BACK -> {
                if (grabMode || textSelectionMode) {
                    if (action == KeyEvent.ACTION_UP) {
                        if (grabMode) {
                            exitGrabMode()
                        } else if (textSelectionMode) {
                            exitTextSelectionMode(cancel = true)
                        }
                    }
                    return true
                }
                return false
            }

            KeyEvent.KEYCODE_DPAD_LEFT -> {
                if (action == KeyEvent.ACTION_DOWN) {
                    handleDirectionKeyEvent(event, -1, UNCHANGED, true)
                } else if (action == KeyEvent.ACTION_UP) {
                    handleDirectionKeyEvent(event, 0, UNCHANGED, false)
                }
                return true
            }

            KeyEvent.KEYCODE_DPAD_RIGHT -> {
                if (action == KeyEvent.ACTION_DOWN) {
                    handleDirectionKeyEvent(event, 1, UNCHANGED, true)
                } else if (action == KeyEvent.ACTION_UP) {
                    handleDirectionKeyEvent(event, 0, UNCHANGED, false)
                }
                return true
            }

            KeyEvent.KEYCODE_DPAD_UP -> {
                if (action == KeyEvent.ACTION_DOWN) {
                    handleDirectionKeyEvent(event, UNCHANGED, -1, true)
                } else if (action == KeyEvent.ACTION_UP) {
                    handleDirectionKeyEvent(event, UNCHANGED, 0, false)
                }
                return true
            }

            KeyEvent.KEYCODE_DPAD_DOWN -> {
                if (action == KeyEvent.ACTION_DOWN) {
                    handleDirectionKeyEvent(event, UNCHANGED, 1, true)
                } else if (action == KeyEvent.ACTION_UP) {
                    handleDirectionKeyEvent(event, UNCHANGED, 0, false)
                }
                return true
            }

            KeyEvent.KEYCODE_DPAD_UP_LEFT -> {
                if (action == KeyEvent.ACTION_DOWN) {
                    handleDirectionKeyEvent(event, -1, -1, true)
                } else if (action == KeyEvent.ACTION_UP) {
                    handleDirectionKeyEvent(event, 0, 0, false)
                }
                return true
            }

            KeyEvent.KEYCODE_DPAD_UP_RIGHT -> {
                if (action == KeyEvent.ACTION_DOWN) {
                    handleDirectionKeyEvent(event, 1, -1, true)
                } else if (action == KeyEvent.ACTION_UP) {
                    handleDirectionKeyEvent(event, 0, 0, false)
                }
                return true
            }

            KeyEvent.KEYCODE_DPAD_DOWN_LEFT -> {
                if (action == KeyEvent.ACTION_DOWN) {
                    handleDirectionKeyEvent(event, -1, 1, true)
                } else if (action == KeyEvent.ACTION_UP) {
                    handleDirectionKeyEvent(event, 0, 0, false)
                }
                return true
            }

            KeyEvent.KEYCODE_DPAD_DOWN_RIGHT -> {
                if (action == KeyEvent.ACTION_DOWN) {
                    handleDirectionKeyEvent(event, 1, 1, true)
                } else if (action == KeyEvent.ACTION_UP) {
                    handleDirectionKeyEvent(event, 0, 0, false)
                }
                return true
            }

            KeyEvent.KEYCODE_DPAD_CENTER,
            KeyEvent.KEYCODE_ENTER,
            KeyEvent.KEYCODE_NUMPAD_ENTER,
            KeyEvent.KEYCODE_BUTTON_A -> {
                if (directionalNavMode) {
                    return false  // Passes through to WebView
                }
                if (action == KeyEvent.ACTION_DOWN) {
                    if (event.repeatCount > 0) {
                        // Already tracking, ignore repeats
                        return true
                    }

                    if (grabMode) {
                        exitGrabMode()
                        return true
                    }

                    if (textSelectionMode) {
                        return true
                    }

                    // Start tracking this press
                    centerButtonDownTime = SystemClock.uptimeMillis()
                    surface.keyDispatcherState.startTracking(event, this)

                    if (!isCursorDisappear && !directionalNavMode && isLongPressMenuEnabled && !dpadCenterPressed) {
                        dpadCenterPressed = true
                        dispatchMotionEvent(
                            cursorPosition.x,
                            cursorPosition.y,
                            MotionEvent.ACTION_DOWN
                        )
                        surface.postInvalidate()
                        surface.postDelayed(longPressRunnable, LONG_PRESS_TIMEOUT)
                    } else if (!isLongPressMenuEnabled && !isCursorDisappear && !directionalNavMode) {
                        if (!dpadCenterPressed) {
                            dpadCenterPressed = true
                            dispatchMotionEvent(
                                cursorPosition.x,
                                cursorPosition.y,
                                MotionEvent.ACTION_DOWN
                            )
                            surface.postInvalidate()
                        }
                    }
                    return true

                } else if (action == KeyEvent.ACTION_UP) {
                    surface.keyDispatcherState.handleUpEvent(event)
                    surface.removeCallbacks(longPressRunnable)

                    // If long press menu became active, just consume the UP
                    if (longPressMenuActive) {
                        dpadCenterPressed = false
                        centerButtonDownTime = 0L
                        return true
                    }

                    if (textSelectionMode) {
                        exitTextSelectionMode(cancel = false)
                        return true
                    }

                    if (isCursorDisappear && !directionalNavMode) {
                        // Show cursor
                        lastCursorUpdate = SystemClock.uptimeMillis()
                        surface.postInvalidate()
                        return true
                    }

                    if (dpadCenterPressed) {
                        dispatchMotionEvent(
                            cursorPosition.x,
                            cursorPosition.y,
                            MotionEvent.ACTION_UP
                        )
                        dpadCenterPressed = false
                        surface.postInvalidate()
                    }

                    centerButtonDownTime = 0L
                    return true
                }
            }
        }
        return false
    }

    private fun dispatchMotionEvent(x: Float, y: Float, action: Int, pointerId: Int = 0) {
        // Don't dispatch touch events if menu is active (except CANCEL to clean up)
        if (longPressMenuActive && action != MotionEvent.ACTION_CANCEL) return

        // Don't dispatch touch events in directional nav mode
        if (directionalNavMode && action != MotionEvent.ACTION_CANCEL) return

        if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_POINTER_DOWN) {
            downTime = SystemClock.uptimeMillis()
        }
        val eventTime = SystemClock.uptimeMillis()
        val properties = arrayOfNulls<MotionEvent.PointerProperties>(1)
        val pp1 = MotionEvent.PointerProperties()
        pp1.id = pointerId
        pp1.toolType = MotionEvent.TOOL_TYPE_FINGER
        properties[0] = pp1
        val pointerCoords = arrayOfNulls<MotionEvent.PointerCoords>(1)
        val pc1 = MotionEvent.PointerCoords()
        pc1.x = x
        pc1.y = y
        pc1.pressure = 1f
        pc1.size = 1f
        pointerCoords[0] = pc1
        val motionEvent = MotionEvent.obtain(
            downTime, eventTime,
            action, 1, properties,
            pointerCoords, 0, 0, 1f, 1f, 0, 0, 0, 0
        )
        try {
            surface.dispatchTouchEvent(motionEvent)
        } finally {
            motionEvent.recycle()
        }
    }

    private fun handleDirectionKeyEvent(event: KeyEvent, x: Int, y: Int, keyDown: Boolean) {
        // Don't process direction keys if cursor movement is blocked
        if (isCursorMovementBlocked) return

        lastCursorUpdate = SystemClock.uptimeMillis()

        if (keyDown) {
            if (surface.keyDispatcherState.isTracking(event)) {
                return
            }
            surface.removeCallbacks(cursorUpdateRunnable)
            surface.post(cursorUpdateRunnable)
            surface.keyDispatcherState.startTracking(event, this)
        } else {
            surface.keyDispatcherState.handleUpEvent(event)
            cursorSpeed.set(0f, 0f)
            if (scrollHackStarted) {
                dispatchMotionEvent(
                    scrollHackCoords.x,
                    scrollHackCoords.y,
                    MotionEvent.ACTION_CANCEL
                )
                scrollHackStarted = false
            }
        }

        cursorDirection.set(
            if (x == UNCHANGED) cursorDirection.x else x,
            if (y == UNCHANGED) cursorDirection.y else y
        )
    }

    private fun scrollWebViewBy(scrollX: Int, scrollY: Int) {
        if (scrollX == 0 && scrollY == 0) return

        @Suppress("SimplifyBooleanWithConstants")
        if ((scrollX != 0 && surface.canScrollHorizontally(scrollX)) ||
            (scrollY != 0 && surface.canScrollVertically(scrollY))
        ) {
            surface.scrollTo(surface.scrollX + scrollX, surface.scrollY + scrollY)
        } else if (customScrollCallback?.onScroll(scrollX, scrollY) == true) {
            return
        } else if (USE_SCROLL_HACK && !dpadCenterPressed) {
            var justStarted = false
            if (!scrollHackStarted) {
                scrollHackCoords.set(
                    cursorPosition.x.coerceIn(
                        scrollHackActiveRect.left.toFloat(),
                        scrollHackActiveRect.right.toFloat()
                    ),
                    cursorPosition.y.coerceIn(
                        scrollHackActiveRect.top.toFloat(),
                        scrollHackActiveRect.bottom.toFloat()
                    )
                )
                dispatchMotionEvent(scrollHackCoords.x, scrollHackCoords.y, MotionEvent.ACTION_DOWN)
                scrollHackStarted = true
                justStarted = true
            }
            scrollHackCoords.x -= scrollX
            scrollHackCoords.y -= scrollY
            if (scrollHackCoords.x < scrollHackActiveRect.left ||
                scrollHackCoords.x >= scrollHackActiveRect.right ||
                scrollHackCoords.y < scrollHackActiveRect.top ||
                scrollHackCoords.y >= scrollHackActiveRect.bottom
            ) {
                scrollHackCoords.x += scrollX
                scrollHackCoords.y += scrollY
                dispatchMotionEvent(
                    scrollHackCoords.x,
                    scrollHackCoords.y,
                    MotionEvent.ACTION_CANCEL
                )
                scrollHackStarted = false
                if (!justStarted) {
                    scrollWebViewBy(scrollX, scrollY)
                }
                return
            }
            dispatchMotionEvent(scrollHackCoords.x, scrollHackCoords.y, MotionEvent.ACTION_MOVE)
        }
    }

    fun dispatchDraw(canvas: Canvas) {
        // Don't draw cursor if it should be hidden
        if (shouldHideCursor) return

        if (grabMode || textSelectionMode || !isCursorDisappear) {
            val cx = cursorPosition.x
            val cy = cursorPosition.y
            val radius = if (dpadCenterPressed) cursorRadiusPressed else cursorRadius

            paint.color = when {
                grabMode -> Color.argb(128, 200, 200, 255)
                textSelectionMode -> Color.argb(128, 200, 255, 200)
                else -> Color.argb(128, 255, 255, 255)
            }
            paint.style = Paint.Style.FILL
            canvas.drawCircle(cx, cy, radius.toFloat(), paint)

            paint.color = Color.GRAY
            paint.strokeWidth = cursorStrokeWidth
            paint.style = Paint.Style.STROKE
            canvas.drawCircle(cx, cy, radius.toFloat(), paint)

            if (grabMode) {
                val halfRadius = radius.toFloat() / 2
                canvas.drawLine(cx - halfRadius, cy, cx + halfRadius, cy, paint)
                canvas.drawLine(cx, cy - halfRadius, cx, cy + halfRadius, paint)
            }
        }
    }

    fun goToGrabMode() {
        grabMode = true
        surface.postInvalidate()
    }

    fun exitGrabMode() {
        dispatchMotionEvent(cursorPosition.x, cursorPosition.y, MotionEvent.ACTION_UP)
        dpadCenterPressed = false
        grabMode = false
        surface.postInvalidate()
    }

    fun goToTextSelectionMode() {
        textSelectionMode = true
        dpadCenterPressed = false
        textSelectionCallback?.onTextSelectionStart(
            cursorPosition.x.toInt(),
            cursorPosition.y.toInt()
        )
        surface.postInvalidate()
    }

    fun exitTextSelectionMode(cancel: Boolean) {
        textSelectionMode = false
        if (cancel) {
            textSelectionCallback?.onTextSelectionCancel()
        } else {
            textSelectionCallback?.onTextSelectionEnd(
                cursorPosition.x.toInt(),
                cursorPosition.y.toInt()
            )
        }
        surface.postInvalidate()
    }

    private val cursorUpdateRunnable = object : Runnable {
        override fun run() {
            // Check if we should stop at the start of each frame
            if (!enabled || isCursorMovementBlocked) {
                stopCursorMovement()
                return
            }

            surface.removeCallbacks(cursorHideRunnable)

            val newTime = SystemClock.uptimeMillis()
            val dTime = newTime - lastCursorUpdate
            lastCursorUpdate = newTime

            val accelerationFactor = 0.05f * dTime

            cursorSpeed.x = (cursorSpeed.x + cursorDirection.x.toFloat()
                .coerceIn(-1f, 1f) * accelerationFactor)
                .coerceIn(-maxCursorSpeed, maxCursorSpeed)

            cursorSpeed.y = (cursorSpeed.y + cursorDirection.y.toFloat()
                .coerceIn(-1f, 1f) * accelerationFactor)
                .coerceIn(-maxCursorSpeed, maxCursorSpeed)

            if (abs(cursorSpeed.x) < 0.1f) cursorSpeed.x = 0f
            if (abs(cursorSpeed.y) < 0.1f) cursorSpeed.y = 0f

            if (cursorDirection.x == 0 && cursorDirection.y == 0 &&
                cursorSpeed.x == 0f && cursorSpeed.y == 0f
            ) {
                if (scrollHackStarted) {
                    dispatchMotionEvent(scrollHackCoords.x, scrollHackCoords.y, MotionEvent.ACTION_CANCEL)
                    scrollHackStarted = false
                }
                surface.postDelayed(cursorHideRunnable, CURSOR_DISAPPEAR_TIMEOUT.toLong())
                return
            }

            tmpPointF.set(cursorPosition)
            cursorPosition.offset(cursorSpeed.x, cursorSpeed.y)
            surface.removeCallbacks(longPressRunnable)

            cursorPosition.x = cursorPosition.x.coerceIn(0f, (surface.width - 1).toFloat())
            cursorPosition.y = cursorPosition.y.coerceIn(0f, (surface.height - 1).toFloat())

            if (tmpPointF != cursorPosition) {
                when {
                    dpadCenterPressed -> {
                        dispatchMotionEvent(
                            cursorPosition.x,
                            cursorPosition.y,
                            MotionEvent.ACTION_MOVE
                        )
                    }

                    textSelectionMode -> {
                        textSelectionCallback?.onTextSelectionMove(
                            cursorPosition.x.toInt(),
                            cursorPosition.y.toInt()
                        )
                    }
                }
            }

            var dx = 0
            var dy = 0
            if (cursorPosition.y > surface.height - scrollStartPadding) {
                if (cursorSpeed.y > 0) dy = cursorSpeed.y.toInt()
            } else if (cursorPosition.y < scrollStartPadding) {
                if (cursorSpeed.y < 0) dy = cursorSpeed.y.toInt()
            }
            if (cursorPosition.x > surface.width - scrollStartPadding) {
                if (cursorSpeed.x > 0) dx = cursorSpeed.x.toInt()
            } else if (cursorPosition.x < scrollStartPadding) {
                if (cursorSpeed.x < 0) dx = cursorSpeed.x.toInt()
            }
            if (dx != 0 || dy != 0) {
                scrollWebViewBy(dx, dy)
            }

            surface.invalidate()
            surface.postOnAnimation(this)
        }
    }

    fun tryZoomIn() {
        generateZoomGesture(true)
    }

    fun tryZoomOut() {
        generateZoomGesture(false)
    }

    private var pinchZoomStartTime = 0L
    private val pinchZoomDuration = 1000
    private var pinchZoomIn = true
    private val zoomFactor = 0.1f

    private fun getPointerAction(action: Int, pointerIndex: Int): Int {
        return action or (pointerIndex shl MotionEvent.ACTION_POINTER_INDEX_SHIFT)
    }

    private fun generateZoomGesture(pinchZoomIn: Boolean) {
        if (pinchZoomStartTime != 0L) return

        this.pinchZoomIn = pinchZoomIn
        this.pinchZoomStartTime = SystemClock.uptimeMillis()
        val deltaX = zoomFactor / 2f * surface.height
        val deltaY = zoomFactor / 2f * surface.height
        val deltaX2 = deltaX / 2f
        val deltaY2 = deltaY / 2f
        val startPoint1: PointF = if (pinchZoomIn) {
            PointF(surface.width / 2f - deltaX2, surface.height / 2f - deltaY2)
        } else {
            PointF(surface.width / 2f - deltaX, surface.height / 2f - deltaY)
        }
        val startPoint2: PointF = if (pinchZoomIn) {
            PointF(surface.width / 2f + deltaX2, surface.height / 2f + deltaY2)
        } else {
            PointF(surface.width / 2f + deltaX, surface.height / 2f + deltaY)
        }

        val properties = arrayOfNulls<MotionEvent.PointerProperties>(2)
        val pp1 = MotionEvent.PointerProperties()
        pp1.id = 0
        pp1.toolType = MotionEvent.TOOL_TYPE_FINGER
        val pp2 = MotionEvent.PointerProperties()
        pp2.id = 1
        pp2.toolType = MotionEvent.TOOL_TYPE_FINGER
        properties[0] = pp1
        properties[1] = pp2

        val pointerCoords = arrayOfNulls<MotionEvent.PointerCoords>(2)
        val pc1 = MotionEvent.PointerCoords()
        pc1.x = startPoint1.x
        pc1.y = startPoint1.y
        pc1.pressure = 1f
        pc1.size = 1f
        val pc2 = MotionEvent.PointerCoords()
        pc2.x = startPoint2.x
        pc2.y = startPoint2.y
        pc2.pressure = 1f
        pc2.size = 1f
        pointerCoords[0] = pc1
        pointerCoords[1] = pc2

        var event = MotionEvent.obtain(
            pinchZoomStartTime, pinchZoomStartTime,
            MotionEvent.ACTION_DOWN, 1, properties,
            pointerCoords, 0, 0, 1f, 1f, 0, 0, 0, 0
        )
        surface.dispatchTouchEvent(event)
        event.recycle()

        event = MotionEvent.obtain(
            pinchZoomStartTime, pinchZoomStartTime,
            getPointerAction(MotionEvent.ACTION_POINTER_DOWN, 1), 2,
            properties, pointerCoords, 0, 0, 1f, 1f, 0, 0, 0, 0
        )
        surface.dispatchTouchEvent(event)
        event.recycle()

        surface.postOnAnimation(pinchZoomRunnable)
    }

    private val pinchZoomRunnable: Runnable by lazy {
        object : Runnable {
            override fun run() {
                if (pinchZoomStartTime == 0L) return

                val deltaX = zoomFactor / 2 * surface.height
                val deltaY = zoomFactor / 2 * surface.height
                val deltaX2 = deltaX / 2
                val deltaY2 = deltaY / 2
                val startPoint1: PointF = if (pinchZoomIn) {
                    PointF(surface.width / 2f - deltaX2, surface.height / 2f - deltaY2)
                } else {
                    PointF(surface.width / 2f - deltaX, surface.height / 2f - deltaY)
                }
                val startPoint2: PointF = if (pinchZoomIn) {
                    PointF(surface.width / 2f + deltaX2, surface.height / 2f + deltaY2)
                } else {
                    PointF(surface.width / 2f + deltaX, surface.height / 2f + deltaY)
                }
                val endPoint1: PointF = if (pinchZoomIn) {
                    PointF(surface.width / 2f - deltaX, surface.height / 2f - deltaY)
                } else {
                    PointF(surface.width / 2f - deltaX2, surface.height / 2f - deltaY2)
                }
                val endPoint2: PointF = if (pinchZoomIn) {
                    PointF(surface.width / 2f + deltaX, surface.height / 2f + deltaY)
                } else {
                    PointF(surface.width / 2f + deltaX2, surface.height / 2f + deltaY2)
                }

                val properties = arrayOfNulls<MotionEvent.PointerProperties>(2)
                val pp1 = MotionEvent.PointerProperties()
                pp1.id = 0
                pp1.toolType = MotionEvent.TOOL_TYPE_FINGER
                val pp2 = MotionEvent.PointerProperties()
                pp2.id = 1
                pp2.toolType = MotionEvent.TOOL_TYPE_FINGER
                properties[0] = pp1
                properties[1] = pp2
                val pointerCoords = arrayOfNulls<MotionEvent.PointerCoords>(2)
                val pc1 = MotionEvent.PointerCoords()
                val pc2 = MotionEvent.PointerCoords()
                pc1.pressure = 1f
                pc1.size = 1f
                pc2.pressure = 1f
                pc2.size = 1f

                val now = SystemClock.uptimeMillis()
                if (now - pinchZoomStartTime < pinchZoomDuration) {
                    val progress = (now - pinchZoomStartTime).toFloat() / pinchZoomDuration
                    pc1.x = startPoint1.x + (endPoint1.x - startPoint1.x) * progress
                    pc1.y = startPoint1.y + (endPoint1.y - startPoint1.y) * progress
                    pc2.x = startPoint2.x + (endPoint2.x - startPoint2.x) * progress
                    pc2.y = startPoint2.y + (endPoint2.y - startPoint2.y) * progress
                    pointerCoords[0] = pc1
                    pointerCoords[1] = pc2
                    val event = MotionEvent.obtain(
                        pinchZoomStartTime, now,
                        MotionEvent.ACTION_MOVE, 2, properties,
                        pointerCoords, 0, 0, 1f, 1f, 0, 0, 0, 0
                    )
                    surface.dispatchTouchEvent(event)
                    event.recycle()
                    surface.postOnAnimation(this)
                } else {
                    pc1.x = endPoint1.x
                    pc1.y = endPoint1.y
                    pc2.x = endPoint2.x
                    pc2.y = endPoint2.y
                    pointerCoords[0] = pc1
                    pointerCoords[1] = pc2
                    var event = MotionEvent.obtain(
                        pinchZoomStartTime, now,
                        getPointerAction(MotionEvent.ACTION_POINTER_UP, 1), 2, properties,
                        pointerCoords, 0, 0, 1f, 1f, 0, 0, 0, 0
                    )
                    surface.dispatchTouchEvent(event)
                    event.recycle()

                    event = MotionEvent.obtain(
                        pinchZoomStartTime, now,
                        MotionEvent.ACTION_UP, 1, properties,
                        pointerCoords, 0, 0, 1f, 1f, 0, 0, 0, 0
                    )
                    surface.dispatchTouchEvent(event)
                    event.recycle()
                    pinchZoomStartTime = 0
                }
            }
        }
    }

    companion object {
        private const val UNCHANGED = Integer.MIN_VALUE
        private const val CURSOR_DISAPPEAR_TIMEOUT = 5000
        private const val USE_SCROLL_HACK = true
        private const val SCROLL_HACK_PADDING = 300

        //100ms more to let underlying view handle long press first
        //idea to let geckoview handle long press first (and receive ContentDelegate.onContextMenu callback)
        //and if it doesn't handle it, then we handle it as long press
        private val LONG_PRESS_TIMEOUT = ViewConfiguration.getLongPressTimeout() + 100L
    }
}
