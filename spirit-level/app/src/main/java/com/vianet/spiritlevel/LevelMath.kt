package com.vianet.spiritlevel

import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

/**
 * The edge of the phone that is resting on (or pointing at) the ground.
 *
 * [rotation] is how far the drawing is turned, in degrees clockwise on screen, so that text and the
 * tube read the right way up for someone looking at the phone in that position. It is also the
 * in-plane gravity angle (see [LevelMath.inPlaneAngle]) at which that edge is exactly level.
 */
enum class Edge(val rotation: Int) {
    BOTTOM(0),
    LEFT(90),
    TOP(180),
    RIGHT(-90);

    companion object {
        fun nearest(angle: Double): Edge {
            val quarter = ((LevelMath.wrap(angle) / 90.0).roundToInt() + 4) % 4
            return when (quarter) {
                0 -> BOTTOM
                1 -> LEFT
                2 -> TOP
                else -> RIGHT
            }
        }
    }
}

/** One frame of level output. All angles are in degrees, already corrected by calibration. */
data class Reading(
    /** True when the phone is lying on its back (bullseye), false when it is stood on an edge (tube). */
    val flat: Boolean,
    /** The edge facing down, or when flat, the last edge used. Sets which way the drawing faces. */
    val edge: Edge,
    /** Edge mode: tilt along the tube. Positive means the viewer's right-hand end is higher. */
    val tilt: Double,
    /** Flat mode: tilt left/right as the viewer sees it. Positive means the right side is higher. */
    val tiltX: Double,
    /** Flat mode: tilt away/towards the viewer. Positive means the far side is higher. */
    val tiltY: Double,
) {
    /** The single number to show: tube tilt on an edge, or total tilt from flat. */
    val headline: Double get() = if (flat) hypot(tiltX, tiltY) else tilt
}

object LevelMath {
    /** Lying-flat detection, with a gap between the two so the mode does not flicker at 45 degrees. */
    const val ENTER_FLAT_DEG = 40.0
    const val LEAVE_FLAT_DEG = 50.0

    /** Swap to a new edge once the phone is turned this far past the current one. */
    const val EDGE_SWITCH_DEG = 50.0

    /** Within this many degrees counts as level. */
    const val LEVEL_DEG = 0.2

    /** Wraps an angle into the range -180 (exclusive) to 180 (inclusive). */
    fun wrap(deg: Double): Double {
        var a = deg % 360.0
        if (a <= -180.0) a += 360.0
        if (a > 180.0) a -= 360.0
        return a
    }

    /**
     * Angle of gravity within the screen plane. 0 when the phone stands upright on its bottom edge,
     * 90 on its left edge, -90 on its right edge and 180 upside down on its top edge.
     *
     * Android reports the reaction to gravity, so the reading points away from the ground:
     * a positive x reading means the right-hand side of the phone is the higher side.
     */
    fun inPlaneAngle(gx: Double, gy: Double): Double = Math.toDegrees(atan2(gx, gy))

    /** How far the screen is from facing straight up: 0 lying flat, 90 standing on an edge. */
    fun inclination(gx: Double, gy: Double, gz: Double): Double =
        Math.toDegrees(atan2(hypot(gx, gy), abs(gz)))

    /** Turns device-frame x/y values into the viewer's right/up for a given edge. */
    fun toViewer(x: Double, y: Double, edge: Edge): Pair<Double, Double> {
        val a = Math.toRadians(edge.rotation.toDouble())
        val c = cos(a).roundToInt().toDouble()
        val s = sin(a).roundToInt().toDouble()
        return Pair(x * c - y * s, x * s + y * c)
    }

    /** Slope as a percentage (rise over run x 100). Matches mm per 10 cm, or cm per metre. */
    fun slopePercent(deg: Double): Double = tan(Math.toRadians(deg)) * 100.0

    fun magnitude(gx: Double, gy: Double, gz: Double): Double = sqrt(gx * gx + gy * gy + gz * gz)

    /**
     * Turns sensor x/y (relative to the display's natural orientation) into screen x/y for the
     * current display rotation in quarter turns (Surface.ROTATION_0..270 = 0..3). Needed on
     * foldables such as the OnePlus Open, where the inner screen can be turned against the sensor.
     */
    fun remapForDisplay(x: Double, y: Double, quarterTurns: Int): Pair<Double, Double> =
        when (((quarterTurns % 4) + 4) % 4) {
            1 -> Pair(-y, x)
            2 -> Pair(-x, -y)
            3 -> Pair(y, -x)
            else -> Pair(x, y)
        }
}

/** Averaged raw (uncalibrated) readings from one moment, used for the two-step calibration. */
data class Sample(
    val flat: Boolean,
    val edge: Edge,
    val edgeTilt: Double,
    val flatX: Double,
    val flatY: Double,
)

/** Per-surface zero offsets, in degrees. Edge offsets are kept separately for each of the four edges. */
data class Calibration(
    val flatX: Double = 0.0,
    val flatY: Double = 0.0,
    val edges: Map<Edge, Double> = emptyMap(),
) {
    fun edge(e: Edge): Double = edges[e] ?: 0.0
    fun isSet(flat: Boolean, e: Edge): Boolean = if (flat) flatX != 0.0 || flatY != 0.0 else edge(e) != 0.0

    /**
     * Two-step ("reversal") calibration. [first] is taken, the phone is turned 180 degrees on the
     * same spot, then [second] is taken. Any slope in the surface reverses between the two and
     * cancels out, leaving only the phone's own error, such as the camera bump lifting one end
     * when it lies on its back. Returns null if the two readings are not from the same side.
     */
    fun fromReversal(first: Sample, second: Sample): Calibration? {
        if (first.flat != second.flat || first.edge != second.edge) return null
        return if (first.flat) {
            copy(flatX = (first.flatX + second.flatX) / 2, flatY = (first.flatY + second.flatY) / 2)
        } else {
            copy(edges = edges + (first.edge to (first.edgeTilt + second.edgeTilt) / 2))
        }
    }
}

/**
 * Works out which way the phone is resting and the tilt readings from a smoothed gravity vector.
 * Keeps a little state so the mode and facing edge only change once the phone has clearly moved.
 */
class LevelTracker(startEdge: Edge = Edge.BOTTOM, startFlat: Boolean = false) {
    var flat: Boolean = startFlat
        private set
    var edge: Edge = startEdge
        private set

    /** Raw readings before calibration, used when the user zeros the level. */
    var rawEdgeTilt = 0.0
        private set
    var rawFlatX = 0.0
        private set
    var rawFlatY = 0.0
        private set

    fun update(gx: Double, gy: Double, gz: Double, cal: Calibration): Reading {
        val g = LevelMath.magnitude(gx, gy, gz)
        val incline = LevelMath.inclination(gx, gy, gz)
        flat = if (flat) incline < LevelMath.LEAVE_FLAT_DEG else incline < LevelMath.ENTER_FLAT_DEG

        val phi = LevelMath.inPlaneAngle(gx, gy)
        // Only re-pick the facing edge while stood up; lying flat keeps the last one so text stays put.
        if (!flat && g > 0 && kotlin.math.hypot(gx, gy) > 0.3 * g) {
            if (abs(LevelMath.wrap(phi - edge.rotation)) > LevelMath.EDGE_SWITCH_DEG) {
                edge = Edge.nearest(phi)
            }
        }

        // Edge mode: tilt within the screen plane relative to the resting edge.
        // Positive phi turn means the viewer's right-hand end is the higher end.
        rawEdgeTilt = LevelMath.wrap(phi - edge.rotation)
        val tilt = rawEdgeTilt - cal.edge(edge)

        // Flat mode: tilt about each screen axis, measured in the device frame so the zero point
        // does not depend on which way the viewer is standing, then turned to face the viewer.
        val z = abs(gz).coerceAtLeast(1e-6)
        rawFlatX = Math.toDegrees(atan2(gx, z))
        rawFlatY = Math.toDegrees(atan2(gy, z))
        val (vx, vy) = LevelMath.toViewer(rawFlatX - cal.flatX, rawFlatY - cal.flatY, edge)

        return Reading(flat = flat, edge = edge, tilt = tilt, tiltX = vx, tiltY = vy)
    }

    /** Calibration that would make the current position read zero, keeping the other surfaces' zeros. */
    fun zeroHere(cal: Calibration): Calibration =
        if (flat) cal.copy(flatX = rawFlatX, flatY = rawFlatY)
        else cal.copy(edges = cal.edges + (edge to rawEdgeTilt))

    fun sample(): Sample = Sample(flat, edge, rawEdgeTilt, rawFlatX, rawFlatY)

    fun resetHere(cal: Calibration): Calibration =
        if (flat) cal.copy(flatX = 0.0, flatY = 0.0)
        else cal.copy(edges = cal.edges - edge)
}
