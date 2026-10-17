package com.vianet.workoutlog.reminders

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.vianet.workoutlog.MainActivity
import com.vianet.workoutlog.R
import com.vianet.workoutlog.WorkoutLogApp
import com.vianet.workoutlog.domain.reminderMessage
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.TimeUnit

/** Runs about once a day and nudges the user when they have gone quiet. */
class ReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as? WorkoutLogApp ?: return Result.success()
        val settings = app.container.settings
        val repository = app.container.repository
        if (!settings.remindersEnabled.value) return Result.success()

        val today = LocalDate.now()
        if (settings.lastReminderEpochDay == today.toEpochDay()) return Result.success()
        if (repository.getActiveWorkout() != null) return Result.success()

        val streak = repository.streak(settings.weeklyGoal.value, today)
        val message = reminderMessage(streak, settings.remindAfterDays.value) ?: return Result.success()

        Reminders.notify(applicationContext, message.first, message.second)
        settings.lastReminderEpochDay = today.toEpochDay()
        return Result.success()
    }
}

object Reminders {
    const val CHANNEL_ID = "reminders"
    private const val NOTIFICATION_ID = 3
    private const val WORK_NAME = "daily-reminder"

    fun createChannel(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(CHANNEL_ID, "Workout reminders", NotificationManager.IMPORTANCE_DEFAULT).apply {
            description = "Nudges when you have not trained for a while"
        }
        manager.createNotificationChannel(channel)
    }

    /** Schedules the daily check at [hour] o'clock. Safe to call repeatedly. */
    fun schedule(context: Context, hour: Int, replace: Boolean) {
        val now = LocalDateTime.now()
        var next = now.toLocalDate().atTime(LocalTime.of(hour.coerceIn(0, 23), 0))
        if (!next.isAfter(now)) next = next.plusDays(1)
        val delay = Duration.between(now, next)

        val request = PeriodicWorkRequestBuilder<ReminderWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(delay.toMillis(), TimeUnit.MILLISECONDS)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            if (replace) ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE else ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }

    fun notify(context: Context, title: String, text: String) {
        createChannel(context)
        val openApp = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(openApp)
            .build()
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, notification)
    }
}
