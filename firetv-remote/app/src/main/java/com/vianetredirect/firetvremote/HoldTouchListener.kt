package com.vianetredirect.firetvremote

import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration

/**
 * Turns a plain button into a remote key: tap = keyDownUp, press-and-hold = keyDown then keyUp on release.
 * Returns false from onTouch so the button still shows its own pressed state and ripple.
 */
class HoldTouchListener(private val onKey: (String) -> Unit) : View.OnTouchListener {

    private var active = false
    private var holding = false
    private val holdDelayMs = ViewConfiguration.getLongPressTimeout().toLong()
    private val holdRunnable = Runnable {
        if (active) {
            holding = true
            onKey("keyDown")
        }
    }

    override fun onTouch(v: View, event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                active = true
                holding = false
                v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                v.postDelayed(holdRunnable, holdDelayMs)
            }
            MotionEvent.ACTION_UP -> {
                v.removeCallbacks(holdRunnable)
                if (active) onKey(if (holding) "keyUp" else "keyDownUp")
                active = false
                holding = false
            }
            MotionEvent.ACTION_CANCEL -> {
                v.removeCallbacks(holdRunnable)
                if (active && holding) onKey("keyUp")
                active = false
                holding = false
            }
        }
        return false
    }
}
