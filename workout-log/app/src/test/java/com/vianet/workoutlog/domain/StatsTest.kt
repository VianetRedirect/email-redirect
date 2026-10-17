package com.vianet.workoutlog.domain

import com.vianet.workoutlog.data.db.SetEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StatsTest {

    private fun set(weight: Double?, reps: Int?, completed: Boolean = true) =
        SetEntry(id = 0, workoutExerciseId = 1, setNumber = 1, weightKg = weight, reps = reps, completed = completed)

    @Test
    fun `single rep is taken at face value`() {
        assertEquals(100.0, estimatedOneRepMax(100.0, 1), 1e-9)
    }

    @Test
    fun `epley formula for multiple reps`() {
        // 100 kg x 10 reps -> 100 * (1 + 10/30) = 133.33
        assertEquals(133.333, estimatedOneRepMax(100.0, 10), 0.001)
    }

    @Test
    fun `zero reps gives zero`() {
        assertEquals(0.0, estimatedOneRepMax(100.0, 0), 1e-9)
    }

    @Test
    fun `volume only counts completed sets`() {
        val sets = listOf(set(60.0, 10), set(60.0, 10, completed = false), set(80.0, 5))
        assertEquals(1000.0, sets.totalVolume(), 1e-9)
        assertEquals(2, sets.completedCount())
    }

    @Test
    fun `best one rep max ignores sets without numbers`() {
        val sets = listOf(set(null, 10), set(60.0, null), set(80.0, 5), set(100.0, 1))
        assertEquals(100.0, sets.bestEstimatedOneRepMax()!!, 1e-9)
    }

    @Test
    fun `heaviest set breaks ties by reps`() {
        val sets = listOf(set(80.0, 5), set(80.0, 8), set(70.0, 12))
        assertEquals(8, sets.heaviestSet()!!.reps)
    }

    @Test
    fun `personal record detection`() {
        assertTrue(isPersonalRecord(set(100.0, 5), previousBest = 110.0))
        assertFalse(isPersonalRecord(set(100.0, 3), previousBest = 110.0))
        assertTrue(isPersonalRecord(set(50.0, 5), previousBest = null))
        assertFalse(isPersonalRecord(set(null, 5), previousBest = null))
    }

    @Test
    fun `no best when nothing completed`() {
        assertNull(listOf(set(80.0, 5, completed = false)).bestEstimatedOneRepMax())
    }
}
