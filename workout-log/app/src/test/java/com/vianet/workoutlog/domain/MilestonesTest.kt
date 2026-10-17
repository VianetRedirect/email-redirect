package com.vianet.workoutlog.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MilestonesTest {

    @Test
    fun `keys are unique`() {
        assertEquals(Milestones.all.size, Milestones.all.map { it.key }.toSet().size)
    }

    @Test
    fun `thresholds ascend within each type`() {
        MilestoneType.entries.forEach { type ->
            val thresholds = Milestones.all.filter { it.type == type }.map { it.threshold }
            assertEquals("$type", thresholds, thresholds.sorted())
        }
    }

    @Test
    fun `crossing a threshold reports exactly the new milestones`() {
        val before = ProgressStats(totalVolumeKg = 900.0, workoutCount = 4)
        val after = ProgressStats(totalVolumeKg = 1_600.0, workoutCount = 5)
        val reached = Milestones.newlyReached(before, after).map { it.key }
        assertEquals(listOf("volume_1000", "volume_1500", "workouts_5"), reached)
    }

    @Test
    fun `next milestone of each type is the smallest unreached`() {
        val stats = ProgressStats(totalVolumeKg = 3_000.0, totalDistanceKm = 0.0, workoutCount = 12, streakWeeks = 0)
        val next = Milestones.nextOfEachType(stats).associate { it.type to it.key }
        assertEquals("volume_5000", next[MilestoneType.VOLUME])
        assertEquals("distance_5_0", next[MilestoneType.DISTANCE])
        assertEquals("workouts_25", next[MilestoneType.WORKOUTS])
        assertEquals("streak_2", next[MilestoneType.STREAK])
    }

    @Test
    fun `progress is clamped`() {
        val m = Milestones.byKey("volume_1000")!!
        assertEquals(0.5f, m.progress(ProgressStats(totalVolumeKg = 500.0)), 1e-6f)
        assertEquals(1f, m.progress(ProgressStats(totalVolumeKg = 5_000.0)), 1e-6f)
        assertTrue(m.isReached(ProgressStats(totalVolumeKg = 1_000.0)))
    }

    @Test
    fun `thousands grouping and value formatting`() {
        assertEquals("1,500", groupThousands(1_500))
        assertEquals("12", groupThousands(12))
        assertEquals("10,000,000", groupThousands(10_000_000))
        assertEquals("1,500 kg", formatMilestoneValue(MilestoneType.VOLUME, 1_500.0))
        assertEquals("21.1 km", formatMilestoneValue(MilestoneType.DISTANCE, 21.1))
        assertEquals("1 workout", formatMilestoneValue(MilestoneType.WORKOUTS, 1.0))
        assertEquals("4 weeks", formatMilestoneValue(MilestoneType.STREAK, 4.0))
    }
}
