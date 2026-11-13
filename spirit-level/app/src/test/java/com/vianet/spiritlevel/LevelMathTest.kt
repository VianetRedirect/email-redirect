package com.vianet.spiritlevel

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.cos
import kotlin.math.sin

class LevelMathTest {
    private val g = 9.81
    private val none = Calibration()
    private fun rad(d: Double) = Math.toRadians(d)

    /** Gravity reading for a phone stood on an edge, turned [phi] degrees in the screen plane. */
    private fun upright(phi: Double) = Triple(g * sin(rad(phi)), g * cos(rad(phi)), 0.0)

    private fun LevelTracker.feed(v: Triple<Double, Double, Double>, cal: Calibration = none) =
        update(v.first, v.second, v.third, cal)

    @Test fun standingOnBottomEdgeIsLevel() {
        val r = LevelTracker().feed(upright(0.0))
        assertFalse(r.flat)
        assertEquals(Edge.BOTTOM, r.edge)
        assertEquals(0.0, r.tilt, 1e-9)
    }

    @Test fun rightEndRaisedReadsPositive() {
        // Right-hand side higher: the gravity reaction leans towards +x.
        val r = LevelTracker().feed(upright(2.0))
        assertEquals(Edge.BOTTOM, r.edge)
        assertEquals(2.0, r.tilt, 1e-9)
    }

    @Test fun picksEachOfTheFourEdges() {
        val cases = mapOf(90.0 to Edge.LEFT, -90.0 to Edge.RIGHT, 180.0 to Edge.TOP, 0.0 to Edge.BOTTOM)
        for ((phi, edge) in cases) {
            val r = LevelTracker().feed(upright(phi + 1.5))
            assertEquals("phi=$phi", edge, r.edge)
            assertEquals("phi=$phi", 1.5, r.tilt, 1e-9)
        }
    }

    @Test fun topEdgeTiltAcrossTheWrapPoint() {
        val r = LevelTracker().feed(upright(-179.0))
        assertEquals(Edge.TOP, r.edge)
        assertEquals(1.0, r.tilt, 1e-9)
    }

    @Test fun edgeHasHysteresis() {
        val t = LevelTracker()
        t.feed(upright(0.0))
        assertEquals(Edge.BOTTOM, t.feed(upright(48.0)).edge)
        assertEquals(Edge.LEFT, t.feed(upright(52.0)).edge)
        assertEquals(Edge.LEFT, t.feed(upright(42.0)).edge)
        assertEquals(Edge.BOTTOM, t.feed(upright(38.0)).edge)
    }

    @Test fun lyingFlatUsesBullseye() {
        val r = LevelTracker().feed(Triple(0.0, 0.0, g))
        assertTrue(r.flat)
        assertEquals(0.0, r.headline, 1e-9)
    }

    @Test fun flatTiltDirections() {
        // Right side raised by 1 degree, far (top) side raised by 2 degrees.
        val gx = g * sin(rad(1.0))
        val gy = g * sin(rad(2.0))
        val gz = kotlin.math.sqrt(g * g - gx * gx - gy * gy)
        val r = LevelTracker().feed(Triple(gx, gy, gz))
        assertTrue(r.flat)
        assertEquals(1.0, r.tiltX, 0.02)
        assertEquals(2.0, r.tiltY, 0.02)
    }

    @Test fun flatReadingTurnsWithTheViewer() {
        // Last used on its left edge, so the device's +x now points away from the viewer.
        val t = LevelTracker(startEdge = Edge.LEFT)
        val r = t.feed(Triple(g * sin(rad(1.0)), 0.0, g * cos(rad(1.0))))
        assertTrue(r.flat)
        assertEquals(Edge.LEFT, r.edge)
        assertEquals(0.0, r.tiltX, 1e-9)
        assertEquals(1.0, r.tiltY, 1e-9)
    }

    @Test fun flatModeHasHysteresis() {
        val t = LevelTracker()
        fun lean(deg: Double) = Triple(0.0, g * sin(rad(deg)), g * cos(rad(deg)))
        assertFalse(t.feed(lean(45.0)).flat)
        assertTrue(t.feed(lean(35.0)).flat)
        assertTrue(t.feed(lean(45.0)).flat)
        assertFalse(t.feed(lean(55.0)).flat)
    }

    @Test fun zeroingOnlyAffectsThatSurface() {
        val t = LevelTracker()
        t.feed(upright(0.7))
        var cal = t.zeroHere(none)
        assertEquals(0.0, t.feed(upright(0.7), cal).tilt, 1e-9)
        // Other edges are untouched.
        assertEquals(0.7, LevelTracker().feed(upright(90.7), cal).tilt, 1e-9)
        cal = t.resetHere(cal)
        assertEquals(0.7, t.feed(upright(0.7), cal).tilt, 1e-9)
    }

    @Test fun slopePercent() {
        assertEquals(100.0, LevelMath.slopePercent(45.0), 1e-9)
        assertEquals(1.745, LevelMath.slopePercent(1.0), 1e-3)
    }

    @Test fun wrapRange() {
        assertEquals(180.0, LevelMath.wrap(-180.0), 1e-9)
        assertEquals(-170.0, LevelMath.wrap(190.0), 1e-9)
        assertEquals(10.0, LevelMath.wrap(370.0), 1e-9)
    }
}
