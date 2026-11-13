package com.vianet.spiritlevel

import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.hypot

class MainActivity : Activity(), SensorEventListener {

    private lateinit var view: LevelView
    private lateinit var sensors: SensorManager
    private var accel: Sensor? = null
    private var hinge: Sensor? = null

    private val prefs by lazy { getSharedPreferences("level", Context.MODE_PRIVATE) }
    private var cal = Calibration()
    private var buzzOn = true
    private var held = false
    private lateinit var tracker: LevelTracker

    /**
     * Foldables lie differently on their back when open (the camera bump is on one half only),
     * and the screen edges are different edges, so open and folded each keep their own calibration.
     */
    private var profile = ""

    /** Quarter turns between the sensor's frame and the screen (non-zero on some foldable inner screens). */
    private var displayTurns = 0

    /** Hinge angle in degrees, or null if the phone has no hinge sensor. */
    private var hingeAngle: Float? = null

    // Smoothed gravity reading, in screen axes.
    private val g = DoubleArray(3)
    private var haveReading = false
    private var lastTimestamp = 0L

    private var lastReading: Reading? = null
    private var wasLevel = false

    // ---- Two-step calibration ----

    private enum class Step { NONE, SAMPLING_1, WAIT_TURN, SAMPLING_2 }

    private var step = Step.NONE
    private var firstSample: Sample? = null
    private var sampleStart = 0L
    private val samples = ArrayList<Sample>()
    private var movedAt = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        profile = profileFor(resources.configuration)
        loadPrefs()
        tracker = LevelTracker(startEdge = Edge.entries[prefs.getInt("edge", 0).coerceIn(0, 3)])

        view = LevelView(this)
        view.listener = ::onButton
        setContentView(view)
        hideSystemBars()

        sensors = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accel = sensors.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        if (Build.VERSION.SDK_INT >= 30) hinge = sensors.getDefaultSensor(Sensor.TYPE_HINGE_ANGLE)
        if (accel == null) view.toast("This phone has no accelerometer")
    }

    override fun onResume() {
        super.onResume()
        haveReading = false
        displayTurns = currentDisplayTurns()
        accel?.let { sensors.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME) }
        hinge?.let { sensors.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL) }
    }

    override fun onPause() {
        super.onPause()
        sensors.unregisterListener(this)
        cancelCalibration(silent = true)
        prefs.edit().putInt("edge", tracker.edge.ordinal).apply()
    }

    /** Folding or unfolding: swap calibration set and re-read the screen's rotation against the sensor. */
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        displayTurns = currentDisplayTurns()
        val p = profileFor(newConfig)
        if (p != profile) {
            profile = p
            cancelCalibration(silent = true)
            loadPrefs()
            haveReading = false
            view.toast(if (p.isEmpty()) "Folded: using folded calibration" else "Open: using open calibration")
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemBars()
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (Build.VERSION.SDK_INT >= 30 && event.sensor.type == Sensor.TYPE_HINGE_ANGLE) {
            hingeAngle = event.values[0]
            refresh()
            return
        }
        if (held) return
        val (sx, sy) = LevelMath.remapForDisplay(event.values[0].toDouble(), event.values[1].toDouble(), displayTurns)
        val v = doubleArrayOf(sx, sy, event.values[2].toDouble())
        if (!haveReading) {
            for (i in 0..2) g[i] = v[i]
            haveReading = true
        } else {
            // Low-pass filter with a 0.2 s time constant, independent of the sensor rate.
            val dt = ((event.timestamp - lastTimestamp) / 1e9).coerceIn(0.0, 0.5)
            val a = 1.0 - exp(-dt / 0.2)
            for (i in 0..2) g[i] += a * (v[i] - g[i])
        }
        lastTimestamp = event.timestamp

        val r = tracker.update(g[0], g[1], g[2], cal)
        lastReading = r
        collectSample(event.timestamp)
        if (step == Step.NONE) buzzIfLevel(r)
        refresh()
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    private val halfFolded: Boolean
        get() = hingeAngle?.let { it in 15f..165f } ?: false

    private fun refresh() {
        val r = lastReading ?: return
        val (banner, progress) = when {
            halfFolded -> "Open the phone fully or close it\nfor an accurate reading" to -1f
            step == Step.SAMPLING_1 || step == Step.SAMPLING_2 -> {
                val n = if (step == Step.SAMPLING_1) 1 else 2
                val moved = lastTimestamp - movedAt < 1_500_000_000L
                (if (moved) "It moved. Keep the phone still…" else "Step $n of 2: measuring, keep still…") to sampleProgress()
            }
            step == Step.WAIT_TURN -> turnInstruction(r) to -1f
            else -> null to -1f
        }
        view.show(r, held, buzzOn, cal.isSet(r.flat, r.edge), banner, progress)
    }

    private fun turnInstruction(r: Reading): String =
        if (r.flat) "Turn the phone round 180° on the same spot\n(top end where the bottom was),\nthen tap CALIBRATE"
        else "Turn the phone to face the other way,\nsame edge down, on the same spot,\nthen tap CALIBRATE"

    // ---- Buttons ----

    private fun onButton(b: LevelView.Button, longPress: Boolean) {
        when (b) {
            LevelView.Button.HOLD -> {
                if (step != Step.NONE) {
                    cancelCalibration(silent = false)
                    return
                }
                held = !held
                if (!held) haveReading = false
            }
            LevelView.Button.ZERO -> onCalibrate(longPress)
            LevelView.Button.SOUND -> {
                buzzOn = !buzzOn
                prefs.edit().putBoolean("buzz", buzzOn).apply()
                if (buzzOn) buzz()
            }
        }
        refresh()
    }

    private fun onCalibrate(longPress: Boolean) {
        if (held) { view.toast("Release HOLD first"); return }
        if (longPress) {
            cancelCalibration(silent = true)
            cal = tracker.resetHere(cal)
            saveCalibration()
            rereadNow()
            view.toast(if (tracker.flat) "Calibration cleared for lying flat" else "Calibration cleared for this edge")
            return
        }
        if (halfFolded) { view.toast("Open the phone fully or close it first"); return }
        when (step) {
            Step.NONE -> startSampling(Step.SAMPLING_1)
            Step.WAIT_TURN -> startSampling(Step.SAMPLING_2)
            else -> Unit // already measuring
        }
    }

    private fun startSampling(s: Step) {
        step = s
        samples.clear()
        sampleStart = 0L
    }

    private fun cancelCalibration(silent: Boolean) {
        if (step == Step.NONE) return
        step = Step.NONE
        firstSample = null
        samples.clear()
        if (!silent) view.toast("Calibration cancelled")
    }

    // ---- Sampling for calibration ----

    private val settleNs = 700_000_000L
    private val measureNs = 1_500_000_000L

    private fun sampleProgress(): Float =
        if (sampleStart == 0L || lastTimestamp == 0L) 0f
        else ((lastTimestamp - sampleStart).toFloat() / (settleNs + measureNs)).coerceIn(0f, 1f)

    private fun collectSample(now: Long) {
        if (step != Step.SAMPLING_1 && step != Step.SAMPLING_2) return
        if (halfFolded) { sampleStart = 0L; samples.clear(); return }
        if (sampleStart == 0L) sampleStart = now
        if (now - sampleStart < settleNs) return
        val s = tracker.sample()

        // Start the window again if the phone moved or changed side.
        val first = samples.firstOrNull()
        if (first != null) {
            val moved = first.flat != s.flat || first.edge != s.edge ||
                (if (s.flat) hypot(s.flatX - first.flatX, s.flatY - first.flatY) else abs(s.edgeTilt - first.edgeTilt)) > 0.3
            if (moved) {
                samples.clear()
                sampleStart = now
                movedAt = now
                return
            }
        }
        samples.add(s)
        if (now - sampleStart < settleNs + measureNs) return

        val avg = Sample(
            flat = s.flat, edge = s.edge,
            edgeTilt = samples.map { it.edgeTilt }.average(),
            flatX = samples.map { it.flatX }.average(),
            flatY = samples.map { it.flatY }.average(),
        )
        samples.clear()
        if (step == Step.SAMPLING_1) {
            firstSample = avg
            step = Step.WAIT_TURN
            buzz()
            return
        }
        val newCal = firstSample?.let { cal.fromReversal(it, avg) }
        step = Step.NONE
        firstSample = null
        if (newCal == null) {
            view.toast("That was a different side from step 1. Try again.")
            return
        }
        cal = newCal
        saveCalibration()
        rereadNow()
        buzz()
        view.toast(
            if (avg.flat) "Calibrated. Camera bump tilt: %.1f°".format(hypot(cal.flatX, cal.flatY))
            else "Calibrated. Edge error: %.1f°".format(abs(cal.edge(avg.edge)))
        )
    }

    private fun rereadNow() {
        if (haveReading) lastReading = tracker.update(g[0], g[1], g[2], cal)
    }

    // ---- Helpers ----

    private fun buzzIfLevel(r: Reading) {
        val a = abs(r.headline)
        if (!wasLevel && a < LevelMath.LEVEL_DEG) {
            wasLevel = true
            if (buzzOn) buzz()
        } else if (wasLevel && a > LevelMath.LEVEL_DEG * 3) {
            wasLevel = false
        }
    }

    private fun buzz() {
        val vib: Vibrator? = if (Build.VERSION.SDK_INT >= 31) {
            (getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
        vib?.vibrate(VibrationEffect.createOneShot(35, VibrationEffect.DEFAULT_AMPLITUDE))
    }

    private fun currentDisplayTurns(): Int =
        if (Build.VERSION.SDK_INT >= 30) display?.rotation ?: 0
        else @Suppress("DEPRECATION") windowManager.defaultDisplay.rotation

    /** Wide screens (a foldable's inner screen) count as open. */
    private fun profileFor(c: Configuration) = if (c.smallestScreenWidthDp >= 600) "open_" else ""

    private fun hideSystemBars() {
        if (Build.VERSION.SDK_INT >= 30) {
            window.setDecorFitsSystemWindows(false)
            window.insetsController?.let {
                it.hide(WindowInsets.Type.systemBars())
                it.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or android.view.View.SYSTEM_UI_FLAG_FULLSCREEN
                or android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or android.view.View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)
        }
    }

    private fun loadPrefs() {
        buzzOn = prefs.getBoolean("buzz", true)
        val edges = Edge.entries.associateWith { prefs.getFloat("${profile}edge_${it.name}", 0f).toDouble() }
            .filterValues { it != 0.0 }
        cal = Calibration(
            flatX = prefs.getFloat("${profile}flat_x", 0f).toDouble(),
            flatY = prefs.getFloat("${profile}flat_y", 0f).toDouble(),
            edges = edges,
        )
    }

    private fun saveCalibration() {
        prefs.edit().apply {
            putFloat("${profile}flat_x", cal.flatX.toFloat())
            putFloat("${profile}flat_y", cal.flatY.toFloat())
            for (e in Edge.entries) putFloat("${profile}edge_${e.name}", cal.edge(e).toFloat())
        }.apply()
    }
}
