package com.vianet.workoutlog.data

import android.content.Context
import android.content.SharedPreferences
import com.vianet.workoutlog.domain.WeightUnit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** User preferences, backed by SharedPreferences and exposed as state flows. */
class Settings(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    private val _weightUnit = MutableStateFlow(
        runCatching { WeightUnit.valueOf(prefs.getString(KEY_UNIT, WeightUnit.KG.name) ?: WeightUnit.KG.name) }
            .getOrDefault(WeightUnit.KG)
    )
    val weightUnit: StateFlow<WeightUnit> = _weightUnit.asStateFlow()

    private val _defaultRestSeconds = MutableStateFlow(prefs.getInt(KEY_REST, 90))
    val defaultRestSeconds: StateFlow<Int> = _defaultRestSeconds.asStateFlow()

    private val _autoStartRest = MutableStateFlow(prefs.getBoolean(KEY_AUTO_REST, true))
    val autoStartRest: StateFlow<Boolean> = _autoStartRest.asStateFlow()

    private val _weeklyGoal = MutableStateFlow(prefs.getInt(KEY_WEEKLY_GOAL, 3))
    val weeklyGoal: StateFlow<Int> = _weeklyGoal.asStateFlow()

    private val _remindersEnabled = MutableStateFlow(prefs.getBoolean(KEY_REMINDERS, true))
    val remindersEnabled: StateFlow<Boolean> = _remindersEnabled.asStateFlow()

    private val _remindAfterDays = MutableStateFlow(prefs.getInt(KEY_REMIND_AFTER_DAYS, 2))
    val remindAfterDays: StateFlow<Int> = _remindAfterDays.asStateFlow()

    private val _reminderHour = MutableStateFlow(prefs.getInt(KEY_REMINDER_HOUR, 18))
    val reminderHour: StateFlow<Int> = _reminderHour.asStateFlow()

    fun setWeightUnit(unit: WeightUnit) {
        prefs.edit().putString(KEY_UNIT, unit.name).apply()
        _weightUnit.value = unit
    }

    fun setDefaultRestSeconds(seconds: Int) {
        val clamped = seconds.coerceIn(10, 600)
        prefs.edit().putInt(KEY_REST, clamped).apply()
        _defaultRestSeconds.value = clamped
    }

    fun setAutoStartRest(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_REST, enabled).apply()
        _autoStartRest.value = enabled
    }

    fun setWeeklyGoal(workouts: Int) {
        val clamped = workouts.coerceIn(1, 7)
        prefs.edit().putInt(KEY_WEEKLY_GOAL, clamped).apply()
        _weeklyGoal.value = clamped
    }

    fun setRemindersEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_REMINDERS, enabled).apply()
        _remindersEnabled.value = enabled
    }

    fun setRemindAfterDays(days: Int) {
        val clamped = days.coerceIn(1, 14)
        prefs.edit().putInt(KEY_REMIND_AFTER_DAYS, clamped).apply()
        _remindAfterDays.value = clamped
    }

    fun setReminderHour(hour: Int) {
        val clamped = hour.coerceIn(0, 23)
        prefs.edit().putInt(KEY_REMINDER_HOUR, clamped).apply()
        _reminderHour.value = clamped
    }

    /** Date (epoch day) of the last reminder sent, so one nudge per day is the limit. */
    var lastReminderEpochDay: Long
        get() = prefs.getLong(KEY_LAST_REMINDER_DAY, -1L)
        set(value) = prefs.edit().putLong(KEY_LAST_REMINDER_DAY, value).apply()

    private companion object {
        const val KEY_UNIT = "weight_unit"
        const val KEY_REST = "default_rest_seconds"
        const val KEY_AUTO_REST = "auto_start_rest"
        const val KEY_WEEKLY_GOAL = "weekly_goal"
        const val KEY_REMINDERS = "reminders_enabled"
        const val KEY_REMIND_AFTER_DAYS = "remind_after_days"
        const val KEY_REMINDER_HOUR = "reminder_hour"
        const val KEY_LAST_REMINDER_DAY = "last_reminder_day"
    }
}
