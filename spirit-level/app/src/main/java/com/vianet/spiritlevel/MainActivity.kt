package com.vianet.spiritlevel

import android.app.Activity
import android.content.Context
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

class MainActivity : Activity(), SensorEventListener {

    private lateinit var view: LevelView
    private lateinit var sensors: SensorManager
    private var accel: Sensor? = null

    private val prefs by lazy { getSharedPreferences("level", Context.MODE_PRIVATE) }
    private var cal = Calibration()
    private var buzzOn = true
    private var held = false
    private lateinit var tracker: LevelTracker

    // Smoothed gravity reading.
    private val g = DoubleArray(3)
    private var haveReading = false
    private var lastTimestamp = 0L

    private var lastReading: Reading? = null
    private var wasLevel = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        loadPrefs()
        tracker = LevelTracker(startEdge = Edge.entries[prefs.getInt("edge", 0).coerceIn(0, 3)])

        view = LevelView(this)
        view.listener = ::onButton
        setContentView(view)
        hideSystemBars()

        sensors = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accel = sensors.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        if (accel == null) view.toast("This phone has no accelerometer")
    }

    override fun onResume() {
        super.onResume()
        haveReading = false
        accel?.let { sensors.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME) }
    }

    override fun onPause() {
        super.onPause()
        sensors.unregisterListener(this)
        prefs.edit().putInt("edge", tracker.edge.ordinal).apply()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemBars()
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (held) return
        val v = event.values
        if (!haveReading) {
            for (i in 0..2) g[i] = v[i].toDouble()
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
        buzzIfLevel(r)
        view.show(r, held, buzzOn, cal.isSet(r.flat, r.edge))
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    private fun onButton(b: LevelView.Button, longPress: Boolean) {
        when (b) {
            LevelView.Button.HOLD -> {
                held = !held
                if (!held) haveReading = false
            }
            LevelView.Button.ZERO -> {
                if (held) { view.toast("Release HOLD first"); return }
                cal = if (longPress) tracker.resetHere(cal) else tracker.zeroHere(cal)
                saveCalibration()
                val where = if (tracker.flat) "lying flat" else "this edge"
                view.toast(if (longPress) "Zero cleared for $where" else "Zeroed for $where")
            }
            LevelView.Button.SOUND -> {
                buzzOn = !buzzOn
                prefs.edit().putBoolean("buzz", buzzOn).apply()
                if (buzzOn) buzz()
            }
        }
        lastReading?.let { r ->
            val fresh = if (b == LevelView.Button.ZERO) tracker.update(g[0], g[1], g[2], cal) else r
            lastReading = fresh
            view.show(fresh, held, buzzOn, cal.isSet(fresh.flat, fresh.edge))
        }
    }

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
        val edges = Edge.entries.associateWith { prefs.getFloat("edge_${it.name}", 0f).toDouble() }
            .filterValues { it != 0.0 }
        cal = Calibration(
            flatX = prefs.getFloat("flat_x", 0f).toDouble(),
            flatY = prefs.getFloat("flat_y", 0f).toDouble(),
            edges = edges,
        )
    }

    private fun saveCalibration() {
        prefs.edit().apply {
            putFloat("flat_x", cal.flatX.toFloat())
            putFloat("flat_y", cal.flatY.toFloat())
            for (e in Edge.entries) putFloat("edge_${e.name}", cal.edge(e).toFloat())
        }.apply()
    }
}
