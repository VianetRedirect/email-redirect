package com.vianetredirect.firetvremote

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import android.util.TypedValue
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.min

/**
 * The big navigation ring. Drawn by hand so it is one view, one touch target and zero layout passes:
 * four wedges for up/right/down/left and a centre button for select.
 * A tap sends keyDownUp; holding sends keyDown on hold and keyUp on release so the Fire TV scrolls.
 */
class DpadView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    interface Listener {
        fun onKey(action: String, keyActionType: String)
    }

    var listener: Listener? = null

    private val density = resources.displayMetrics.density
    private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = context.getColor(R.color.surface) }
    private val pressedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = context.getColor(R.color.surface_pressed) }
    private val centerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = context.getColor(R.color.surface_center) }
    private val gapPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = context.getColor(R.color.bg) }
    private val arrowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = context.getColor(R.color.text); style = Paint.Style.FILL }
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = context.getColor(R.color.text_dim)
        textSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 14f, resources.displayMetrics)
        textAlign = Paint.Align.CENTER
    }
    private val oval = RectF()
    private val arrow = Path()

    private var cx = 0f
    private var cy = 0f
    private var outerR = 0f
    private var innerR = 0f

    // 0 up, 1 right, 2 down, 3 left, 4 centre, -1 nothing
    private var activeRegion = -1
    private var holding = false
    private val actions = arrayOf("dpad_up", "dpad_right", "dpad_down", "dpad_left", "select")
    private val holdDelayMs = ViewConfiguration.getLongPressTimeout().toLong()
    private val holdRunnable = Runnable {
        val r = activeRegion
        if (r >= 0) {
            holding = true
            performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            listener?.onKey(actions[r], "keyDown")
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        cx = w / 2f
        cy = h / 2f
        outerR = min(min(w, h) / 2f - 4f * density, 175f * density)
        innerR = outerR * 0.38f
    }

    override fun onDraw(canvas: Canvas) {
        if (outerR <= 0f) return
        oval.set(cx - outerR, cy - outerR, cx + outerR, cy + outerR)
        canvas.drawOval(oval, ringPaint)
        if (activeRegion in 0..3) {
            val start = when (activeRegion) {
                0 -> 225f
                1 -> -45f
                2 -> 45f
                else -> 135f
            }
            canvas.drawArc(oval, start, 90f, true, pressedPaint)
        }
        canvas.drawCircle(cx, cy, innerR + 3f * density, gapPaint)
        canvas.drawCircle(cx, cy, innerR, if (activeRegion == 4) pressedPaint else centerPaint)

        val d = outerR * 0.70f
        drawArrow(canvas, cx, cy - d, 0f)
        drawArrow(canvas, cx + d, cy, 90f)
        drawArrow(canvas, cx, cy + d, 180f)
        drawArrow(canvas, cx - d, cy, 270f)

        canvas.drawText("OK", cx, cy + labelPaint.textSize / 3f, labelPaint)
    }

    private fun drawArrow(canvas: Canvas, x: Float, y: Float, rotation: Float) {
        val s = 7f * density
        arrow.reset()
        arrow.moveTo(0f, -s)
        arrow.lineTo(s * 1.15f, s * 0.7f)
        arrow.lineTo(-s * 1.15f, s * 0.7f)
        arrow.close()
        canvas.save()
        canvas.translate(x, y)
        canvas.rotate(rotation)
        canvas.drawPath(arrow, arrowPaint)
        canvas.restore()
    }

    private fun regionAt(x: Float, y: Float): Int {
        val dx = x - cx
        val dy = y - cy
        val dist = hypot(dx, dy)
        if (dist <= innerR) return 4
        if (dist > outerR * 1.1f) return -1
        val deg = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble()))
        return when {
            deg > -45 && deg <= 45 -> 1
            deg > 45 && deg <= 135 -> 2
            deg > -135 && deg <= -45 -> 0
            else -> 3
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                val r = regionAt(event.x, event.y)
                if (r < 0) return false
                activeRegion = r
                holding = false
                performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                postDelayed(holdRunnable, holdDelayMs)
                invalidate()
                return true
            }
            MotionEvent.ACTION_UP -> {
                release(true)
                return true
            }
            MotionEvent.ACTION_CANCEL -> {
                release(false)
                return true
            }
        }
        return activeRegion >= 0
    }

    private fun release(tap: Boolean) {
        removeCallbacks(holdRunnable)
        val r = activeRegion
        activeRegion = -1
        invalidate()
        if (r < 0) return
        if (holding) {
            holding = false
            listener?.onKey(actions[r], "keyUp")
        } else if (tap) {
            listener?.onKey(actions[r], "keyDownUp")
        }
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }
}
