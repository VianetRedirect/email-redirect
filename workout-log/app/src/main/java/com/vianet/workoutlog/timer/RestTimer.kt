package com.vianet.workoutlog.timer

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.max

/**
 * Process-wide rest timer state. The UI reads [state]; [RestTimerService] keeps the
 * countdown alive in a foreground notification and fires the alert when it reaches zero.
 */
object RestTimer {
    data class State(val totalSeconds: Int, val endAtMillis: Long) {
        fun remainingSeconds(now: Long = System.currentTimeMillis()): Int =
            ((endAtMillis - now + 999) / 1000).toInt().coerceAtLeast(0)

        fun progress(now: Long = System.currentTimeMillis()): Float =
            if (totalSeconds <= 0) 0f else (remainingSeconds(now).toFloat() / totalSeconds).coerceIn(0f, 1f)
    }

    private val _state = MutableStateFlow<State?>(null)
    val state: StateFlow<State?> = _state.asStateFlow()

    fun start(context: Context, seconds: Int) {
        val total = seconds.coerceAtLeast(1)
        _state.value = State(totalSeconds = total, endAtMillis = System.currentTimeMillis() + total * 1000L)
        val intent = Intent(context, RestTimerService::class.java).setAction(RestTimerService.ACTION_START)
        ContextCompat.startForegroundService(context, intent)
    }

    /** Adds or removes seconds from the running timer. Never drops below one second remaining. */
    fun adjust(deltaSeconds: Int) {
        val current = _state.value ?: return
        val now = System.currentTimeMillis()
        val newEnd = max(now + 1000L, current.endAtMillis + deltaSeconds * 1000L)
        val newTotal = max(1, current.totalSeconds + deltaSeconds)
        _state.value = State(totalSeconds = newTotal, endAtMillis = newEnd)
    }

    fun skip() {
        _state.value = null
    }
}
