package com.vianet.workoutlog.domain

enum class MilestoneType(val label: String) {
    VOLUME("Total weight lifted"),
    DISTANCE("Total distance"),
    WORKOUTS("Workouts completed"),
    STREAK("Weekly streak"),
}

data class Milestone(
    val key: String,
    val type: MilestoneType,
    val threshold: Double,
    val title: String,
    val detail: String,
    val emoji: String,
)

/** Everything the milestone checks need, in one place. */
data class ProgressStats(
    val totalVolumeKg: Double = 0.0,
    val totalDistanceKm: Double = 0.0,
    val workoutCount: Int = 0,
    val streakWeeks: Int = 0,
) {
    fun valueFor(type: MilestoneType): Double = when (type) {
        MilestoneType.VOLUME -> totalVolumeKg
        MilestoneType.DISTANCE -> totalDistanceKm
        MilestoneType.WORKOUTS -> workoutCount.toDouble()
        MilestoneType.STREAK -> streakWeeks.toDouble()
    }
}

fun Milestone.isReached(stats: ProgressStats): Boolean = stats.valueFor(type) >= threshold

fun Milestone.progress(stats: ProgressStats): Float =
    if (threshold <= 0) 1f else (stats.valueFor(type) / threshold).toFloat().coerceIn(0f, 1f)

object Milestones {

    private fun volume(kg: Int, title: String, detail: String, emoji: String) =
        Milestone("volume_$kg", MilestoneType.VOLUME, kg.toDouble(), title, detail, emoji)

    private fun distance(km: Double, title: String, detail: String, emoji: String) =
        Milestone("distance_${km.toString().replace('.', '_')}", MilestoneType.DISTANCE, km, title, detail, emoji)

    private fun workouts(n: Int, title: String, detail: String, emoji: String) =
        Milestone("workouts_$n", MilestoneType.WORKOUTS, n.toDouble(), title, detail, emoji)

    private fun streak(weeks: Int, title: String, detail: String, emoji: String) =
        Milestone("streak_$weeks", MilestoneType.STREAK, weeks.toDouble(), title, detail, emoji)

    val all: List<Milestone> = listOf(
        // Cumulative weight lifted (sum of weight x reps over every completed set)
        volume(250, "A grizzly bear", "250 kg lifted in total: about the weight of an adult grizzly bear.", "🐻"),
        volume(500, "A grand piano", "500 kg lifted in total: a concert grand piano.", "🎹"),
        volume(1_000, "A saltwater crocodile", "1,000 kg lifted in total: a big saltwater crocodile.", "🐊"),
        volume(1_500, "A family car", "1,500 kg lifted in total: a typical family hatchback.", "🚗"),
        volume(2_500, "A Range Rover", "2,500 kg lifted in total: a fully loaded Range Rover.", "🚙"),
        volume(5_000, "An African elephant", "5,000 kg lifted in total: an adult African elephant.", "🐘"),
        volume(12_000, "A double-decker bus", "12,000 kg lifted in total: a London double-decker bus.", "🚌"),
        volume(20_000, "A fire engine", "20,000 kg lifted in total: a fully kitted fire engine.", "🚒"),
        volume(44_000, "An articulated lorry", "44,000 kg lifted in total: a fully loaded artic at the UK limit.", "🚛"),
        volume(80_000, "A Boeing 737", "80,000 kg lifted in total: a Boeing 737 at take-off weight.", "✈️"),
        volume(150_000, "A blue whale", "150,000 kg lifted in total: the biggest animal that has ever lived.", "🐋"),
        volume(225_000, "The Statue of Liberty", "225,000 kg lifted in total: Lady Liberty, copper and iron included.", "🗽"),
        volume(400_000, "A Boeing 747", "400,000 kg lifted in total: a jumbo jet ready for take-off.", "🛫"),
        volume(1_000_000, "One million kilos", "1,000,000 kg lifted in total. Seven figures. Ridiculous.", "💎"),
        volume(2_000_000, "The Space Shuttle", "2,000,000 kg lifted in total: the Space Shuttle stack on the launch pad.", "🚀"),
        volume(10_000_000, "The Eiffel Tower", "10,000,000 kg lifted in total: the whole Eiffel Tower.", "🗼"),

        // Cumulative distance from cardio sets
        distance(5.0, "A parkrun", "5 km covered in total: one parkrun done.", "🏃"),
        distance(10.0, "10 km", "10 km covered in total: into double figures.", "🔟"),
        distance(21.1, "A half marathon", "21.1 km covered in total: a half marathon.", "🥈"),
        distance(42.2, "A marathon", "42.2 km covered in total: a full marathon.", "🏅"),
        distance(100.0, "A century", "100 km covered in total.", "💯"),
        distance(344.0, "London to Paris", "344 km covered in total: London to Paris as the crow flies.", "🗼"),
        distance(650.0, "London to Edinburgh", "650 km covered in total: the length of the East Coast Main Line.", "🏴"),
        distance(1_400.0, "Land's End to John o' Groats", "1,400 km covered in total: the length of Britain by road.", "🇬🇧"),
        distance(4_500.0, "Coast to coast USA", "4,500 km covered in total: New York to Los Angeles.", "🌎"),
        distance(10_000.0, "Ten thousand", "10,000 km covered in total.", "🌍"),
        distance(40_075.0, "Around the world", "40,075 km covered in total: the circumference of the Earth.", "🌐"),

        // Workouts completed
        workouts(1, "First one done", "Your first workout is in the book.", "🎉"),
        workouts(5, "Five workouts", "Five workouts logged. This is a habit forming.", "✋"),
        workouts(10, "Ten workouts", "Ten workouts logged.", "🔥"),
        workouts(25, "Twenty-five workouts", "Twenty-five workouts logged.", "⚡"),
        workouts(50, "Fifty workouts", "Fifty workouts logged. Half a century.", "🏆"),
        workouts(100, "One hundred workouts", "One hundred workouts logged.", "👑"),
        workouts(200, "Two hundred workouts", "Two hundred workouts logged.", "🦾"),
        workouts(365, "A year of workouts", "365 workouts logged: one for every day of the year.", "📅"),
        workouts(500, "Five hundred workouts", "Five hundred workouts logged.", "🌟"),
        workouts(1_000, "One thousand workouts", "One thousand workouts logged. Legendary.", "🐐"),

        // Weekly streak
        streak(2, "Two-week streak", "Hit your weekly goal two weeks running.", "🔥"),
        streak(4, "One-month streak", "Four weeks in a row.", "🌙"),
        streak(8, "Two-month streak", "Eight weeks in a row.", "🧱"),
        streak(12, "Three-month streak", "Twelve weeks in a row. A full training block.", "🏗️"),
        streak(26, "Six-month streak", "Twenty-six weeks in a row.", "🛡️"),
        streak(52, "One-year streak", "Fifty-two weeks in a row. A whole year.", "🎖️"),
    )

    fun byKey(key: String): Milestone? = all.firstOrNull { it.key == key }

    /** Milestones reached under [after] that were not reached under [before], in threshold order. */
    fun newlyReached(before: ProgressStats, after: ProgressStats): List<Milestone> =
        all.filter { !it.isReached(before) && it.isReached(after) }
            .sortedWith(compareBy({ it.type.ordinal }, { it.threshold }))

    /** The next unreached milestone of each type, for progress bars. */
    fun nextOfEachType(stats: ProgressStats): List<Milestone> =
        MilestoneType.entries.mapNotNull { type ->
            all.filter { it.type == type && !it.isReached(stats) }.minByOrNull { it.threshold }
        }
}

/** Formats a milestone value for display: 1,500 kg or 21.1 km. */
fun formatMilestoneValue(type: MilestoneType, value: Double): String = when (type) {
    MilestoneType.VOLUME -> "${groupThousands(value.toLong())} kg"
    MilestoneType.DISTANCE -> if (value == value.toLong().toDouble()) "${groupThousands(value.toLong())} km" else "$value km"
    MilestoneType.WORKOUTS -> "${value.toLong()} ${if (value.toLong() == 1L) "workout" else "workouts"}"
    MilestoneType.STREAK -> "${value.toLong()} ${if (value.toLong() == 1L) "week" else "weeks"}"
}

fun groupThousands(value: Long): String {
    val digits = value.toString()
    val sb = StringBuilder()
    digits.reversed().forEachIndexed { index, c ->
        if (index > 0 && index % 3 == 0) sb.append(',')
        sb.append(c)
    }
    return sb.reverse().toString()
}
