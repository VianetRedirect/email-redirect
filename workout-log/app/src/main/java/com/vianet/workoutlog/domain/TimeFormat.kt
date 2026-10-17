package com.vianet.workoutlog.domain

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** 95 seconds -> "1:35", 3600 -> "1:00:00". */
fun formatClock(totalSeconds: Int): String {
    val s = totalSeconds.coerceAtLeast(0)
    val hours = s / 3600
    val minutes = (s % 3600) / 60
    val seconds = s % 60
    return if (hours > 0) {
        String.format(Locale.UK, "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.UK, "%d:%02d", minutes, seconds)
    }
}

fun formatDurationMillis(millis: Long): String = formatClock((millis / 1000).toInt())

/** 90 -> "1:30", 60 -> "1:00", 45 -> "0:45". Used for rest-time labels. */
fun formatRest(seconds: Int): String = formatClock(seconds)

/** Short duration for history cards: 3725 seconds -> "1h 2m", 1500 -> "25m". */
fun formatDurationShort(millis: Long): String {
    val totalMinutes = (millis / 60_000).toInt().coerceAtLeast(0)
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
}

private val dayFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE d MMM yyyy", Locale.UK)
private val shortDayFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM", Locale.UK)
private val timeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.UK)

fun formatDay(epochMillis: Long, zone: ZoneId = ZoneId.systemDefault()): String =
    dayFormatter.format(Instant.ofEpochMilli(epochMillis).atZone(zone))

fun formatShortDay(epochMillis: Long, zone: ZoneId = ZoneId.systemDefault()): String =
    shortDayFormatter.format(Instant.ofEpochMilli(epochMillis).atZone(zone))

fun formatTime(epochMillis: Long, zone: ZoneId = ZoneId.systemDefault()): String =
    timeFormatter.format(Instant.ofEpochMilli(epochMillis).atZone(zone))
