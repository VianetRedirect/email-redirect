package com.vianet.workoutlog.domain

import com.vianet.workoutlog.data.db.SetEntry

/** Epley estimate of a one rep max. A single rep is taken at face value. */
fun estimatedOneRepMax(weightKg: Double, reps: Int): Double {
    if (reps <= 0) return 0.0
    if (reps == 1) return weightKg
    return weightKg * (1 + reps / 30.0)
}

fun SetEntry.estimatedOneRepMax(): Double? {
    val w = weightKg ?: return null
    val r = reps ?: return null
    if (r <= 0) return null
    return estimatedOneRepMax(w, r)
}

fun SetEntry.volume(): Double = (weightKg ?: 0.0) * (reps ?: 0)

fun List<SetEntry>.totalVolume(): Double = filter { it.completed }.sumOf { it.volume() }

fun List<SetEntry>.completedCount(): Int = count { it.completed }

/** Highest estimated one rep max across completed sets, or null if none have weight and reps. */
fun List<SetEntry>.bestEstimatedOneRepMax(): Double? =
    filter { it.completed }.mapNotNull { it.estimatedOneRepMax() }.maxOrNull()

/** Heaviest completed set by weight, ties broken by reps. */
fun List<SetEntry>.heaviestSet(): SetEntry? =
    filter { it.completed && it.weightKg != null && (it.reps ?: 0) > 0 }
        .maxWithOrNull(compareBy<SetEntry> { it.weightKg ?: 0.0 }.thenBy { it.reps ?: 0 })

/** True when this set beats every estimated one rep max in [previousBest]. */
fun isPersonalRecord(set: SetEntry, previousBest: Double?): Boolean {
    val e1rm = set.estimatedOneRepMax() ?: return false
    if (previousBest == null) return e1rm > 0
    return e1rm > previousBest + 1e-9
}
