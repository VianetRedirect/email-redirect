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

    /** Reading for a phone lying on its back, with its own tilt [bx],[by] on a surface tilted [sx],[sy]. */
    private fun lying(bx: Double, by: Double, sx: Double, sy: Double): Triple<Double, Double, Double> {
        val tx = rad(bx + sx)
        val ty = rad(by + sy)
        val gx = g * sin(tx)
        val gy = g * sin(ty)
        return Triple(gx, gy, kotlin.math.sqrt(g * g - gx * gx - gy * gy))
    }

    @Test fun reversalCancelsSurfaceAndKeepsCameraBump() {
        // Camera bump lifts the top end by 1.4 degrees and one side by 0.3; the table slopes too.
        val bumpX = 0.3; val bumpY = 1.4
        val t = LevelTracker()
        t.feed(lying(bumpX, bumpY, 0.8, -0.5))
        val first = t.sample()
        // Turned 180 degrees on the same spot: the table's slope reverses, the bump does not.
        t.feed(lying(bumpX, bumpY, -0.8, 0.5))
        val second = t.sample()
        val cal = none.fromReversal(first, second)!!
        // On a truly level surface it now reads level despite the bump.
        val r = t.feed(lying(bumpX, bumpY, 0.0, 0.0), cal)
        assertEquals(0.0, r.tiltX, 0.01)
        assertEquals(0.0, r.tiltY, 0.01)
        // And on the sloping table it reads the table's slope.
        val s = t.feed(lying(bumpX, bumpY, 0.8, -0.5), cal)
        assertEquals(0.8, s.tiltX, 0.02)
        assertEquals(-0.5, s.tiltY, 0.02)
    }

    @Test fun reversalOnAnEdge() {
        val t = LevelTracker()
        t.feed(upright(0.25 + 1.0)); val a = t.sample()   // phone error 0.25, surface +1.0
        t.feed(upright(0.25 - 1.0)); val b = t.sample()   // turned round: surface reads -1.0
        val cal = none.fromReversal(a, b)!!
        assertEquals(0.25, cal.edge(Edge.BOTTOM), 1e-9)
        assertEquals(0.0, t.feed(upright(0.25), cal).tilt, 1e-9)
    }

    @Test fun reversalRejectsDifferentSides() {
        val t = LevelTracker()
        t.feed(upright(0.0)); val a = t.sample()
        t.feed(Triple(0.0, 0.0, g)); val b = t.sample()
        assertEquals(null, none.fromReversal(a, b))
    }

    @Test fun displayRemap() {
        assertEquals(Pair(1.0, 2.0), LevelMath.remapForDisplay(1.0, 2.0, 0))
        assertEquals(Pair(-2.0, 1.0), LevelMath.remapForDisplay(1.0, 2.0, 1))
        assertEquals(Pair(-1.0, -2.0), LevelMath.remapForDisplay(1.0, 2.0, 2))
        assertEquals(Pair(2.0, -1.0), LevelMath.remapForDisplay(1.0, 2.0, 3))
    }
}
