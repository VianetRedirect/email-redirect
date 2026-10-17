package com.vianet.workoutlog.domain

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

/**
 * Weekly streak in the Duolingo spirit, but counted in weeks because nobody trains every day.
 * A week (Monday to Sunday) counts when it has at least [weeklyGoal] workouts.
 */
data class StreakInfo(
    /** Consecutive weeks hitting the goal, counting back from the most recent completed week.
     *  The current week is included once it hits the goal. */
    val currentWeeks: Int,
    val bestWeeks: Int,
    val thisWeekCount: Int,
    val weeklyGoal: Int,
    /** Days until the current week ends (0 on Sunday). */
    val daysLeftThisWeek: Int,
    val lastWorkoutDate: LocalDate?,
    val daysSinceLastWorkout: Int?,
) {
    val thisWeekGoalMet: Boolean get() = thisWeekCount >= weeklyGoal
    val workoutsStillNeeded: Int get() = (weeklyGoal - thisWeekCount).coerceAtLeast(0)

    /** True when there is a streak to lose and this week has not yet secured it. */
    val atRisk: Boolean get() = currentWeeks > 0 && !thisWeekGoalMet
}

fun epochMillisToDate(millis: Long, zone: ZoneId = ZoneId.systemDefault()): LocalDate =
    Instant.ofEpochMilli(millis).atZone(zone).toLocalDate()

fun computeStreak(workoutDates: List<LocalDate>, weeklyGoal: Int, today: LocalDate): StreakInfo {
    val goal = weeklyGoal.coerceAtLeast(1)
    val thisWeekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    val countsByWeek: Map<LocalDate, Int> = workoutDates
        .groupingBy { it.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)) }
        .eachCount()

    val thisWeekCount = countsByWeek[thisWeekStart] ?: 0

    // Current streak: walk back week by week. Start from this week if it already hit the goal,
    // otherwise from last week, so an unfinished week neither counts nor breaks the chain.
    var current = 0
    var week = if (thisWeekCount >= goal) thisWeekStart else thisWeekStart.minusWeeks(1)
    while ((countsByWeek[week] ?: 0) >= goal) {
        current++
        week = week.minusWeeks(1)
    }

    // Best streak over all history.
    var best = 0
    var run = 0
    var previous: LocalDate? = null
    countsByWeek.keys.sorted().forEach { w ->
        val hit = (countsByWeek[w] ?: 0) >= goal
        run = if (hit) {
            if (previous != null && ChronoUnit.WEEKS.between(previous, w) == 1L) run + 1 else 1
        } else {
            0
        }
        if (hit) previous = w else previous = null
        if (run > best) best = run
    }
    best = maxOf(best, current)

    val last = workoutDates.maxOrNull()
    val daysSince = last?.let { ChronoUnit.DAYS.between(it, today).toInt().coerceAtLeast(0) }
    val sunday = thisWeekStart.plusDays(6)

    return StreakInfo(
        currentWeeks = current,
        bestWeeks = best,
        thisWeekCount = thisWeekCount,
        weeklyGoal = goal,
        daysLeftThisWeek = ChronoUnit.DAYS.between(today, sunday).toInt().coerceAtLeast(0),
        lastWorkoutDate = last,
        daysSinceLastWorkout = daysSince,
    )
}

/** Picks the reminder wording for a nudge notification, or null when no nudge is due. */
fun reminderMessage(streak: StreakInfo, remindAfterDays: Int): Pair<String, String>? {
    val since = streak.daysSinceLastWorkout ?: return null
    if (since < remindAfterDays) return null
    val needed = streak.workoutsStillNeeded
    return when {
        streak.atRisk && streak.daysLeftThisWeek <= 1 && needed > 0 ->
            "Your ${streak.currentWeeks}-week streak ends ${if (streak.daysLeftThisWeek == 0) "tonight" else "tomorrow"}" to
                "$needed more ${if (needed == 1) "workout" else "workouts"} this week keeps it alive."
        streak.atRisk && needed > 0 ->
            "Keep the ${streak.currentWeeks}-week streak going" to
                "$needed more ${if (needed == 1) "workout" else "workouts"} this week. It's been $since days since your last one."
        else ->
            "It's been $since days" to "Time for a workout? Even a short one counts."
    }
}
