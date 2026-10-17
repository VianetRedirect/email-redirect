package com.vianet.workoutlog.domain

import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToLong

enum class WeightUnit(val label: String, val kgPerUnit: Double) {
    KG("kg", 1.0),
    LB("lb", 0.45359237);

    fun fromKg(kg: Double): Double = kg / kgPerUnit
    fun toKg(value: Double): Double = value * kgPerUnit
}

/** Formats a kg value in the chosen unit, trimming needless decimals: 60 -> "60", 62.5 -> "62.5". */
fun formatWeight(kg: Double?, unit: WeightUnit): String {
    if (kg == null) return ""
    val value = unit.fromKg(kg)
    val rounded = (value * 100).roundToLong() / 100.0
    return if (abs(rounded - rounded.roundToLong()) < 0.005) {
        rounded.roundToLong().toString()
    } else {
        String.format(Locale.UK, "%.2f", rounded).trimEnd('0').trimEnd('.')
    }
}

/** Parses user input typed in the chosen unit and returns kilograms, or null when blank or invalid. */
fun parseWeight(text: String, unit: WeightUnit): Double? {
    val cleaned = text.trim().replace(',', '.')
    if (cleaned.isEmpty()) return null
    val value = cleaned.toDoubleOrNull() ?: return null
    if (value < 0) return null
    return unit.toKg(value)
}

/** Formats kilometres with up to two decimals, trimming zeros: 5 -> "5", 2.5 -> "2.5". */
fun formatKm(km: Double?): String {
    if (km == null) return ""
    val rounded = (km * 100).roundToLong() / 100.0
    return if (abs(rounded - rounded.roundToLong()) < 0.005) {
        rounded.roundToLong().toString()
    } else {
        String.format(Locale.UK, "%.2f", rounded).trimEnd('0').trimEnd('.')
    }
}

fun parseKm(text: String): Double? {
    val cleaned = text.trim().replace(',', '.')
    if (cleaned.isEmpty()) return null
    val value = cleaned.toDoubleOrNull() ?: return null
    return if (value < 0) null else value
}

/** Minutes typed as "12" or "12.5" become seconds. */
fun parseMinutesToSeconds(text: String): Int? {
    val cleaned = text.trim().replace(',', '.')
    if (cleaned.isEmpty()) return null
    val minutes = cleaned.toDoubleOrNull() ?: return null
    return if (minutes < 0) null else (minutes * 60).roundToLong().toInt()
}

/** Seconds back to the minutes text the user typed: 750 -> "12.5", 720 -> "12". */
fun formatSecondsAsMinutes(seconds: Int?): String {
    if (seconds == null) return ""
    val minutes = seconds / 60.0
    val rounded = (minutes * 100).roundToLong() / 100.0
    return if (abs(rounded - rounded.roundToLong()) < 0.005) {
        rounded.roundToLong().toString()
    } else {
        String.format(Locale.UK, "%.2f", rounded).trimEnd('0').trimEnd('.')
    }
}
