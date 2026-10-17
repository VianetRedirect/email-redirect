package com.vianet.workoutlog.timer

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.vianet.workoutlog.MainActivity
import com.vianet.workoutlog.R
import com.vianet.workoutlog.domain.formatClock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Foreground service that shows the rest countdown and alerts when it ends. */
class RestTimerService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var job: Job? = null

    private val notificationManager: NotificationManager
        get() = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createChannels()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_ADD_30 -> RestTimer.adjust(30)
            ACTION_SKIP -> RestTimer.skip()
        }
        val state = RestTimer.state.value
        // Always go foreground first: a startForegroundService() call must be answered promptly.
        startForegroundCompat(buildCountdownNotification(state?.remainingSeconds() ?: 0))
        if (state == null) {
            stopForegroundAndSelf()
            return START_NOT_STICKY
        }
        if (job?.isActive != true) {
            job = scope.launch { runCountdown() }
        }
        return START_NOT_STICKY
    }

    private suspend fun runCountdown() {
        while (true) {
            val state = RestTimer.state.value
            if (state == null) {
                stopForegroundAndSelf()
                return
            }
            val remaining = state.remainingSeconds()
            if (remaining <= 0) {
                RestTimer.skip()
                alertFinished()
                stopForegroundAndSelf()
                return
            }
            notificationManager.notify(COUNTDOWN_NOTIFICATION_ID, buildCountdownNotification(remaining))
            delay(500)
        }
    }

    private fun alertFinished() {
        vibrate()
        val notification = NotificationCompat.Builder(this, DONE_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Rest over")
            .setContentText("Time for your next set")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setDefaults(NotificationCompat.DEFAULT_SOUND or NotificationCompat.DEFAULT_VIBRATE)
            .setAutoCancel(true)
            .setContentIntent(openAppIntent())
            .build()
        notificationManager.notify(DONE_NOTIFICATION_ID, notification)
    }

    private fun vibrate() {
        val vibrator: Vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        if (!vibrator.hasVibrator()) return
        val pattern = longArrayOf(0, 300, 150, 300, 150, 500)
        vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
    }

    private fun buildCountdownNotification(remainingSeconds: Int): Notification =
        NotificationCompat.Builder(this, COUNTDOWN_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Rest timer")
            .setContentText("${formatClock(remainingSeconds)} remaining")
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setContentIntent(openAppIntent())
            .addAction(0, "+30s", serviceIntent(ACTION_ADD_30, 1))
            .addAction(0, "Skip", serviceIntent(ACTION_SKIP, 2))
            .build()

    private fun openAppIntent(): PendingIntent {
        val intent = Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        return PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    }

    private fun serviceIntent(action: String, requestCode: Int): PendingIntent {
        val intent = Intent(this, RestTimerService::class.java).setAction(action)
        return PendingIntent.getForegroundService(
            this, requestCode, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    private fun startForegroundCompat(notification: Notification) {
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        } else {
            0
        }
        ServiceCompat.startForeground(this, COUNTDOWN_NOTIFICATION_ID, notification, type)
    }

    private fun stopForegroundAndSelf() {
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun createChannels() {
        val countdown = NotificationChannel(COUNTDOWN_CHANNEL_ID, "Rest countdown", NotificationManager.IMPORTANCE_LOW).apply {
            description = "Live countdown while resting between sets"
            setShowBadge(false)
        }
        val done = NotificationChannel(DONE_CHANNEL_ID, "Rest finished", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "Alert when the rest timer reaches zero"
            enableVibration(true)
        }
        notificationManager.createNotificationChannel(countdown)
        notificationManager.createNotificationChannel(done)
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        const val ACTION_START = "com.vianet.workoutlog.timer.START"
        const val ACTION_ADD_30 = "com.vianet.workoutlog.timer.ADD_30"
        const val ACTION_SKIP = "com.vianet.workoutlog.timer.SKIP"
        const val COUNTDOWN_CHANNEL_ID = "rest_countdown"
        const val DONE_CHANNEL_ID = "rest_done"
        const val COUNTDOWN_NOTIFICATION_ID = 1
        const val DONE_NOTIFICATION_ID = 2
    }
}
