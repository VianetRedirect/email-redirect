package com.vianet.spiritlevel

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.os.SystemClock
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.tanh

/**
 * Draws the level. Everything is drawn in a "viewer" frame that is turned to match whichever edge
 * of the phone is down, so the tube always lies along the ground and the text is always upright.
 */
class LevelView(context: Context) : View(context) {

    enum class Button { HOLD, ZERO, SOUND }

    var listener: ((Button, longPress: Boolean) -> Unit)? = null

    private var reading = Reading(false, Edge.BOTTOM, 0.0, 0.0, 0.0)
    private var held = false
    private var buzzOn = true
    private var calibrated = false

    /** Drawing rotation, eased towards the target so turning the phone animates smoothly. */
    private var shownRotation = 0f
    private var lastFrame = 0L

    private var toastText: String? = null
    private var toastUntil = 0L

    private val density = resources.displayMetrics.density
    private fun dp(v: Float) = v * density

    // Colours
    private val cBg = Color.parseColor("#101418")
    private val cPanel = Color.parseColor("#1B242C")
    private val cLine = Color.parseColor("#3A4954")
    private val cText = Color.parseColor("#E8EEF2")
    private val cDim = Color.parseColor("#8A9AA6")
    private val cLevel = Color.parseColor("#7CF29A")
    private val cNear = Color.parseColor("#F2B33D")
    private val cFluid = Color.parseColor("#C9E86B")

    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val text = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create("sans-serif-condensed", Typeface.BOLD)
    }

    private val toLocal = Matrix()
    private val buttonRects = LinkedHashMap<Button, RectF>()
    private var safeInset = 0f

    private val gestures = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
        override fun onDown(e: MotionEvent) = true
        override fun onSingleTapUp(e: MotionEvent): Boolean {
            hit(e)?.let { listener?.invoke(it, false) }
            return true
        }
        override fun onLongPress(e: MotionEvent) {
            hit(e)?.let { listener?.invoke(it, true) }
        }
    })

    init {
        setBackgroundColor(cBg)
        setOnApplyWindowInsetsListener { _, insets ->
            @Suppress("DEPRECATION")
            val cut = insets.displayCutout
            @Suppress("DEPRECATION")
            safeInset = maxOf(
                insets.systemWindowInsetTop, insets.systemWindowInsetBottom,
                insets.systemWindowInsetLeft, insets.systemWindowInsetRight,
                cut?.safeInsetTop ?: 0, cut?.safeInsetBottom ?: 0,
                cut?.safeInsetLeft ?: 0, cut?.safeInsetRight ?: 0,
            ).toFloat()
            invalidate()
            insets
        }
    }

    fun show(r: Reading, held: Boolean, buzzOn: Boolean, calibrated: Boolean) {
        reading = r
        this.held = held
        this.buzzOn = buzzOn
        this.calibrated = calibrated
        invalidate()
    }

    fun toast(msg: String) {
        toastText = msg
        toastUntil = SystemClock.uptimeMillis() + 1800
        invalidate()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean = gestures.onTouchEvent(event)

    private fun hit(e: MotionEvent): Button? {
        val p = floatArrayOf(e.x, e.y)
        toLocal.mapPoints(p)
        val slop = dp(8f)
        return buttonRects.entries.firstOrNull { (_, r) ->
            p[0] >= r.left - slop && p[0] <= r.right + slop && p[1] >= r.top - slop && p[1] <= r.bottom + slop
        }?.key
    }

    private fun colourFor(deg: Double): Int = when {
        abs(deg) < LevelMath.LEVEL_DEG -> cLevel
        abs(deg) < 1.0 -> cNear
        else -> cText
    }

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0 || h <= 0) return

        // Ease the drawing rotation towards the target, taking the short way round.
        val now = SystemClock.uptimeMillis()
        val dt = if (lastFrame == 0L) 1f else ((now - lastFrame) / 1000f).coerceIn(0f, 0.1f)
        lastFrame = now
        val target = reading.edge.rotation.toFloat()
        val diff = ((target - shownRotation) % 360f + 540f) % 360f - 180f
        shownRotation += diff * min(1f, dt * 12f)
        if (abs(target - shownRotation) % 360f < 0.5f) shownRotation = target
        val animating = abs(((target - shownRotation) % 360f + 540f) % 360f - 180f) > 0.5f

        // Viewer frame: width/height swap when the phone is on its side.
        val sideways = abs(abs(target) - 90f) < 1f
        val lw = if (sideways) h else w
        val lh = if (sideways) w else h

        canvas.save()
        canvas.translate(w / 2, h / 2)
        canvas.rotate(shownRotation)
        canvas.translate(-lw / 2, -lh / 2)
        // Inverse transform for touch, using the final rotation so buttons stay tappable mid-turn.
        Matrix().apply {
            postTranslate(-lw / 2, -lh / 2)
            postRotate(target)
            postTranslate(w / 2, h / 2)
            invert(toLocal)
        }

        val pad = max(safeInset, dp(16f)) + dp(8f)
        if (reading.flat) drawFlat(canvas, lw, lh, pad) else drawEdge(canvas, lw, lh, pad)
        drawButtons(canvas, lw, lh, pad)
        drawToast(canvas, lw, lh, pad, now)
        canvas.restore()

        if (animating || (toastText != null && now < toastUntil)) postInvalidateOnAnimation()
    }

    // ---- Layout ----

    private val buttonH get() = dp(52f)

    /** Free space between the header and the button row, in the viewer frame. */
    private fun contentArea(w: Float, h: Float, pad: Float) =
        RectF(pad, pad + dp(40f), w - pad, h - pad - buttonH - dp(34f))

    // ---- Edge mode: a tube level along the edge that is down ----

    private fun drawEdge(c: Canvas, w: Float, h: Float, pad: Float) {
        val tilt = reading.tilt
        val area = contentArea(w, h, pad)
        val unit = min(w, h)
        val cx = w / 2
        val colour = colourFor(tilt)

        val tubeW = min(area.width(), unit * 1.6f)
        val tubeH = min(max(dp(56f), unit * 0.16f), area.height() * 0.2f)
        val ty = area.top + area.height() * 0.68f

        // A long faint line that stays truly horizontal, so you can see the real level line.
        stroke.color = cLine
        stroke.strokeWidth = dp(1.5f)
        c.save()
        c.rotate((-tilt).toFloat(), cx, ty)
        c.drawLine(-w, ty, 2 * w, ty, stroke)
        c.restore()

        drawHeader(c, w, pad, if (held) "HOLD" else "ON EDGE")
        val big = min(unit * 0.2f, area.height() * 0.28f)
        drawAngle(c, cx, area.top + area.height() * 0.36f, big, tilt, colour)

        val tube = RectF(cx - tubeW / 2, ty - tubeH / 2, cx + tubeW / 2, ty + tubeH / 2)
        drawTube(c, tube, tilt, colour)

        // Says which end to lower.
        text.textSize = max(dp(14f), unit * 0.04f)
        text.color = cDim
        val hint = when {
            abs(tilt) < LevelMath.LEVEL_DEG -> "level"
            tilt > 0 -> "right end high"
            else -> "left end high"
        }
        c.drawText(hint, cx, tube.bottom + dp(14f) + text.textSize, text)
    }

    private fun drawTube(c: Canvas, tube: RectF, tilt: Double, colour: Int) {
        val r = tube.height() / 2
        // Body.
        fill.shader = null
        fill.color = cPanel
        c.drawRoundRect(tube, r, r, fill)
        // Fluid.
        val inner = RectF(tube).apply { inset(dp(4f), dp(4f)) }
        fill.shader = LinearGradient(0f, inner.top, 0f, inner.bottom,
            Color.argb(110, Color.red(cFluid), Color.green(cFluid), Color.blue(cFluid)),
            Color.argb(50, Color.red(cFluid), Color.green(cFluid), Color.blue(cFluid)), Shader.TileMode.CLAMP)
        c.drawRoundRect(inner, inner.height() / 2, inner.height() / 2, fill)
        fill.shader = null

        // Bubble: moves to the high end, most sensitive near zero.
        val bubbleW = inner.height() * 1.9f
        val bubbleH = inner.height() * 0.78f
        val travel = inner.width() / 2 - bubbleW / 2 - dp(2f)
        val offset = (tanh(tilt / 5.0) * travel).toFloat()
        val bx = inner.centerX() + offset
        val by = inner.centerY()
        val bubble = RectF(bx - bubbleW / 2, by - bubbleH / 2, bx + bubbleW / 2, by + bubbleH / 2)
        fill.shader = RadialGradient(bx - bubbleW * 0.15f, by - bubbleH * 0.2f, bubbleW * 0.6f,
            Color.WHITE, colour, Shader.TileMode.CLAMP)
        c.drawRoundRect(bubble, bubbleH / 2, bubbleH / 2, fill)
        fill.shader = null

        // Centre marks, just wider than the bubble.
        stroke.color = cText
        stroke.strokeWidth = dp(3f)
        val markX = bubbleW / 2 + dp(5f)
        for (sx in floatArrayOf(-1f, 1f)) {
            val x = inner.centerX() + sx * markX
            c.drawLine(x, tube.top - dp(6f), x, tube.bottom + dp(6f), stroke)
        }
        // Scale ticks at 1, 2, 5 and 10 degrees.
        stroke.strokeWidth = dp(1.5f)
        stroke.color = cDim
        for (deg in doubleArrayOf(1.0, 2.0, 5.0, 10.0)) {
            for (sx in floatArrayOf(-1f, 1f)) {
                val x = inner.centerX() + sx * (tanh(deg / 5.0) * travel).toFloat()
                c.drawLine(x, tube.top - dp(2f), x, tube.top + dp(6f), stroke)
                c.drawLine(x, tube.bottom - dp(6f), x, tube.bottom + dp(2f), stroke)
            }
        }
        // Outline.
        stroke.color = cLine
        stroke.strokeWidth = dp(2f)
        c.drawRoundRect(tube, r, r, stroke)
    }

    // ---- Flat mode: a bullseye ----

    private fun drawFlat(c: Canvas, w: Float, h: Float, pad: Float) {
        val area = contentArea(w, h, pad)
        val total = reading.headline
        val colour = colourFor(total)
        drawHeader(c, w, pad, if (held) "HOLD" else "FLAT")

        // Portrait: readout above the bullseye. Landscape: readout on the left, bullseye on the right.
        val wide = area.width() > area.height() * 1.2f
        val textBox: RectF
        val cx: Float
        val cy: Float
        val radius: Float
        if (wide) {
            textBox = RectF(area.left, area.top, area.centerX(), area.bottom)
            radius = min(area.height(), area.width() / 2) / 2 - dp(4f)
            cx = area.centerX() + area.width() / 4
            cy = area.centerY()
        } else {
            val split = area.top + area.height() * 0.34f
            textBox = RectF(area.left, area.top, area.right, split)
            radius = min(area.width(), area.bottom - split) / 2 - dp(4f)
            cx = area.centerX()
            cy = (split + area.bottom) / 2
        }
        val big = min(min(w, h) * 0.2f, textBox.height() * 0.42f)
        val baseline = textBox.centerY() + big * 0.05f
        drawAngle(c, textBox.centerX(), baseline, big, total, colour)
        text.textSize = big * 0.24f
        text.color = cDim
        c.drawText("X %+.1f°    Y %+.1f°".format(reading.tiltX, reading.tiltY),
            textBox.centerX(), baseline + big * 0.33f + text.textSize * 1.5f, text)

        fill.color = cPanel
        c.drawCircle(cx, cy, radius, fill)
        fill.shader = RadialGradient(cx, cy, radius,
            Color.argb(30, Color.red(cFluid), Color.green(cFluid), Color.blue(cFluid)),
            Color.argb(90, Color.red(cFluid), Color.green(cFluid), Color.blue(cFluid)), Shader.TileMode.CLAMP)
        c.drawCircle(cx, cy, radius - dp(4f), fill)
        fill.shader = null

        val bubbleR = radius * 0.16f
        val travel = radius - bubbleR - dp(6f)

        // Rings at 1, 2 and 5 degrees, using the same scale as the bubble.
        stroke.strokeWidth = dp(1.5f)
        stroke.color = cLine
        for (deg in doubleArrayOf(1.0, 2.0, 5.0)) c.drawCircle(cx, cy, (tanh(deg / 5.0) * travel).toFloat(), stroke)
        c.drawLine(cx - radius, cy, cx + radius, cy, stroke)
        c.drawLine(cx, cy - radius, cx, cy + radius, stroke)
        stroke.color = cText
        stroke.strokeWidth = dp(3f)
        c.drawCircle(cx, cy, bubbleR + dp(5f), stroke)

        // Bubble moves to the high side.
        val mag = hypot(reading.tiltX, reading.tiltY)
        val scale = if (mag < 1e-9) 0.0 else tanh(mag / 5.0) * travel / mag
        val bx = cx + (reading.tiltX * scale).toFloat()
        val by = cy - (reading.tiltY * scale).toFloat()
        fill.shader = RadialGradient(bx - bubbleR * 0.3f, by - bubbleR * 0.3f, bubbleR * 1.2f,
            Color.WHITE, colour, Shader.TileMode.CLAMP)
        c.drawCircle(bx, by, bubbleR, fill)
        fill.shader = null

        stroke.color = cLine
        stroke.strokeWidth = dp(2f)
        c.drawCircle(cx, cy, radius, stroke)
    }

    // ---- Shared pieces ----

    private fun drawHeader(c: Canvas, w: Float, pad: Float, label: String) {
        text.textSize = dp(15f)
        text.color = if (held) cNear else cDim
        val extra = if (calibrated) "  ·  ZEROED" else ""
        c.drawText(label + extra, w / 2, pad + text.textSize, text)
    }

    /** Big angle with the slope (or LEVEL) underneath. [size] is the big text size. */
    private fun drawAngle(c: Canvas, x: Float, y: Float, size: Float, deg: Double, colour: Int) {
        val shown = if (abs(deg) < 0.05) 0.0 else deg
        text.color = colour
        text.textSize = size
        c.drawText("%.1f°".format(abs(shown)), x, y, text)
        text.textSize = size * 0.24f
        val level = abs(deg) < LevelMath.LEVEL_DEG
        text.color = if (level) cLevel else cDim
        val sub = if (level) "LEVEL" else "%.1f%% slope".format(abs(LevelMath.slopePercent(shown)))
        c.drawText(sub, x, y + size * 0.33f, text)
    }

    private fun drawButtons(c: Canvas, w: Float, h: Float, pad: Float) {
        val labels = linkedMapOf(
            Button.HOLD to if (held) "RELEASE" else "HOLD",
            Button.ZERO to "ZERO",
            Button.SOUND to if (buzzOn) "BUZZ ON" else "BUZZ OFF",
        )
        val gap = dp(10f)
        val bh = buttonH
        val bw = min((w - 2 * pad - gap * 2) / 3f, dp(150f))
        val total = bw * 3 + gap * 2
        var x = (w - total) / 2
        val y = h - pad - bh
        text.textSize = dp(15f)
        buttonRects.clear()
        for ((b, label) in labels) {
            val r = RectF(x, y, x + bw, y + bh)
            buttonRects[b] = r
            val active = (b == Button.HOLD && held) || (b == Button.SOUND && buzzOn) || (b == Button.ZERO && calibrated)
            fill.color = if (active) Color.parseColor("#2C3A44") else cPanel
            c.drawRoundRect(r, bh / 2, bh / 2, fill)
            stroke.color = if (b == Button.HOLD && held) cNear else cLine
            stroke.strokeWidth = dp(1.5f)
            c.drawRoundRect(r, bh / 2, bh / 2, stroke)
            text.color = if (b == Button.HOLD && held) cNear else cText
            c.drawText(label, r.centerX(), r.centerY() + text.textSize * 0.35f, text)
            x += bw + gap
        }
        text.textSize = dp(11f)
        text.color = cDim
        c.drawText("Hold ZERO to clear this side's zero", w / 2, y - dp(10f), text)
    }

    private fun drawToast(c: Canvas, w: Float, h: Float, pad: Float, now: Long) {
        val msg = toastText ?: return
        if (now >= toastUntil) { toastText = null; return }
        text.textSize = dp(15f)
        val tw = text.measureText(msg) + dp(32f)
        val y = h - pad - buttonH - dp(56f)
        val r = RectF(w / 2 - tw / 2, y - dp(22f), w / 2 + tw / 2, y + dp(14f))
        fill.color = Color.parseColor("#E6263139")
        c.drawRoundRect(r, dp(18f), dp(18f), fill)
        text.color = cText
        c.drawText(msg, w / 2, y + dp(1f), text)
    }
}
