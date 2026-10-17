package com.vianet.workoutlog.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FormatTest {

    @Test
    fun `clock formatting`() {
        assertEquals("0:45", formatClock(45))
        assertEquals("1:30", formatClock(90))
        assertEquals("1:00:00", formatClock(3600))
        assertEquals("0:00", formatClock(-5))
    }

    @Test
    fun `short duration`() {
        assertEquals("25m", formatDurationShort(25 * 60_000L))
        assertEquals("1h 2m", formatDurationShort(62 * 60_000L))
    }

    @Test
    fun `weight formatting trims decimals`() {
        assertEquals("60", formatWeight(60.0, WeightUnit.KG))
        assertEquals("62.5", formatWeight(62.5, WeightUnit.KG))
        assertEquals("2.25", formatWeight(2.25, WeightUnit.KG))
        assertEquals("", formatWeight(null, WeightUnit.KG))
    }

    @Test
    fun `weight converts to pounds for display`() {
        assertEquals("220.46", formatWeight(100.0, WeightUnit.LB))
    }

    @Test
    fun `parsing accepts comma decimals and rejects junk`() {
        assertEquals(62.5, parseWeight("62,5", WeightUnit.KG)!!, 1e-9)
        assertEquals(45.359237, parseWeight("100", WeightUnit.LB)!!, 1e-6)
        assertNull(parseWeight("", WeightUnit.KG))
        assertNull(parseWeight("abc", WeightUnit.KG))
        assertNull(parseWeight("-5", WeightUnit.KG))
    }
}
