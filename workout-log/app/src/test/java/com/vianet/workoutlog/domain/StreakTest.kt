package com.vianet.workoutlog.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class StreakTest {

    // Wednesday 15 October 2025
    private val today: LocalDate = LocalDate.of(2025, 10, 15)

    private fun week(weeksAgo: Long, vararg dayOffsets: Int): List<LocalDate> {
        val monday = LocalDate.of(2025, 10, 13).minusWeeks(weeksAgo)
        return dayOffsets.map { monday.plusDays(it.toLong()) }
    }

    @Test
    fun `no workouts means no streak`() {
        val s = computeStreak(emptyList(), weeklyGoal = 3, today = today)
        assertEquals(0, s.currentWeeks)
        assertEquals(0, s.bestWeeks)
        assertNull(s.daysSinceLastWorkout)
        assertFalse(s.atRisk)
    }

    @Test
    fun `three past weeks at goal gives a three week streak while this week is open`() {
        val dates = week(3, 0, 2, 4) + week(2, 0, 2, 4) + week(1, 1, 3, 5) + week(0, 0)
        val s = computeStreak(dates, weeklyGoal = 3, today = today)
        assertEquals(3, s.currentWeeks)
        assertEquals(1, s.thisWeekCount)
        assertTrue(s.atRisk)
        assertEquals(2, s.workoutsStillNeeded)
        assertEquals(4, s.daysLeftThisWeek)
    }

    @Test
    fun `current week joins the streak once goal is met`() {
        val dates = week(1, 0, 2, 4) + week(0, 0, 1, 2)
        val s = computeStreak(dates, weeklyGoal = 3, today = today)
        assertEquals(2, s.currentWeeks)
        assertFalse(s.atRisk)
    }

    @Test
    fun `a missed week breaks the streak but best is remembered`() {
        val dates = week(5, 0, 2, 4) + week(4, 0, 2, 4) + week(3, 0, 2, 4) + week(1, 0, 2, 4)
        val s = computeStreak(dates, weeklyGoal = 3, today = today)
        assertEquals(1, s.currentWeeks)
        assertEquals(3, s.bestWeeks)
    }

    @Test
    fun `days since last workout`() {
        val s = computeStreak(listOf(today.minusDays(4)), weeklyGoal = 1, today = today)
        assertEquals(4, s.daysSinceLastWorkout)
    }

    @Test
    fun `reminder only fires after the chosen gap`() {
        val fresh = computeStreak(listOf(today.minusDays(1)), weeklyGoal = 3, today = today)
        assertNull(reminderMessage(fresh, remindAfterDays = 2))
        val stale = computeStreak(listOf(today.minusDays(3)), weeklyGoal = 3, today = today)
        assertNotNull(reminderMessage(stale, remindAfterDays = 2))
    }

    @Test
    fun `reminder warns about streak ending on sunday`() {
        val sunday = LocalDate.of(2025, 10, 19)
        val dates = week(1, 0, 2, 4) + listOf(sunday.minusDays(3))
        val s = computeStreak(dates, weeklyGoal = 3, today = sunday)
        val message = reminderMessage(s, remindAfterDays = 1)!!
        assertTrue(message.first.contains("ends tonight"))
    }
}
