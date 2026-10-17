package com.vianet.workoutlog.data.db

/** Built-in exercise library and starter templates, inserted on first launch. */
object SeedData {

    private fun e(name: String, bodyPart: String, category: String) = Exercise(name = name, bodyPart = bodyPart, category = category)
    private fun d(name: String, category: String) =
        Exercise(name = name, bodyPart = "Cardio", category = category, trackingType = TrackingType.DISTANCE_TIME)

    val exercises: List<Exercise> = listOf(
        // Chest
        e("Bench Press", "Chest", "Barbell"),
        e("Incline Bench Press", "Chest", "Barbell"),
        e("Dumbbell Bench Press", "Chest", "Dumbbell"),
        e("Incline Dumbbell Press", "Chest", "Dumbbell"),
        e("Chest Press Machine", "Chest", "Machine"),
        e("Cable Fly", "Chest", "Cable"),
        e("Pec Deck", "Chest", "Machine"),
        e("Push Up", "Chest", "Bodyweight"),
        e("Dip", "Chest", "Bodyweight"),
        // Back
        e("Deadlift", "Back", "Barbell"),
        e("Romanian Deadlift", "Back", "Barbell"),
        e("Barbell Row", "Back", "Barbell"),
        e("Dumbbell Row", "Back", "Dumbbell"),
        e("Lat Pulldown", "Back", "Cable"),
        e("Seated Cable Row", "Back", "Cable"),
        e("Pull Up", "Back", "Bodyweight"),
        e("Chin Up", "Back", "Bodyweight"),
        e("Face Pull", "Back", "Cable"),
        e("Straight Arm Pulldown", "Back", "Cable"),
        // Legs
        e("Back Squat", "Legs", "Barbell"),
        e("Front Squat", "Legs", "Barbell"),
        e("Leg Press", "Legs", "Machine"),
        e("Hack Squat", "Legs", "Machine"),
        e("Bulgarian Split Squat", "Legs", "Dumbbell"),
        e("Walking Lunge", "Legs", "Dumbbell"),
        e("Leg Extension", "Legs", "Machine"),
        e("Lying Leg Curl", "Legs", "Machine"),
        e("Seated Leg Curl", "Legs", "Machine"),
        e("Hip Thrust", "Legs", "Barbell"),
        e("Standing Calf Raise", "Legs", "Machine"),
        e("Seated Calf Raise", "Legs", "Machine"),
        e("Goblet Squat", "Legs", "Dumbbell"),
        // Shoulders
        e("Overhead Press", "Shoulders", "Barbell"),
        e("Seated Dumbbell Shoulder Press", "Shoulders", "Dumbbell"),
        e("Shoulder Press Machine", "Shoulders", "Machine"),
        e("Lateral Raise", "Shoulders", "Dumbbell"),
        e("Cable Lateral Raise", "Shoulders", "Cable"),
        e("Rear Delt Fly", "Shoulders", "Dumbbell"),
        e("Upright Row", "Shoulders", "Barbell"),
        e("Front Raise", "Shoulders", "Dumbbell"),
        // Arms
        e("Barbell Curl", "Arms", "Barbell"),
        e("Dumbbell Curl", "Arms", "Dumbbell"),
        e("Hammer Curl", "Arms", "Dumbbell"),
        e("Preacher Curl", "Arms", "Machine"),
        e("Cable Curl", "Arms", "Cable"),
        e("Tricep Pushdown", "Arms", "Cable"),
        e("Overhead Tricep Extension", "Arms", "Cable"),
        e("Skull Crusher", "Arms", "Barbell"),
        e("Close Grip Bench Press", "Arms", "Barbell"),
        // Core
        e("Plank", "Core", "Bodyweight"),
        e("Hanging Leg Raise", "Core", "Bodyweight"),
        e("Cable Crunch", "Core", "Cable"),
        e("Ab Wheel Rollout", "Core", "Other"),
        e("Russian Twist", "Core", "Other"),
        // Full body / cardio
        e("Kettlebell Swing", "Full body", "Other"),
        e("Farmer's Walk", "Full body", "Dumbbell"),
        // Cardio, logged as distance and time
        d("Treadmill Walk", "Machine"),
        d("Treadmill Run", "Machine"),
        d("Rowing Machine", "Machine"),
        d("Exercise Bike", "Machine"),
        d("Elliptical", "Machine"),
        d("Stair Climber", "Machine"),
        d("Outdoor Walk", "Bodyweight"),
        d("Outdoor Run", "Bodyweight"),
        d("Cycling", "Other"),
    )

    /** Each starter template as a list of (exercise name, target sets, target reps). */
    val templates: List<Pair<String, List<Triple<String, Int, Int>>>> = listOf(
        "Push" to listOf(
            Triple("Bench Press", 4, 8),
            Triple("Incline Dumbbell Press", 3, 10),
            Triple("Seated Dumbbell Shoulder Press", 3, 10),
            Triple("Lateral Raise", 3, 15),
            Triple("Cable Fly", 3, 12),
            Triple("Tricep Pushdown", 3, 12),
        ),
        "Pull" to listOf(
            Triple("Deadlift", 3, 5),
            Triple("Lat Pulldown", 3, 10),
            Triple("Seated Cable Row", 3, 10),
            Triple("Face Pull", 3, 15),
            Triple("Dumbbell Curl", 3, 12),
            Triple("Hammer Curl", 3, 12),
        ),
        "Legs" to listOf(
            Triple("Back Squat", 4, 6),
            Triple("Romanian Deadlift", 3, 8),
            Triple("Leg Press", 3, 10),
            Triple("Leg Extension", 3, 12),
            Triple("Lying Leg Curl", 3, 12),
            Triple("Standing Calf Raise", 4, 15),
        ),
        "Full Body" to listOf(
            Triple("Back Squat", 3, 8),
            Triple("Bench Press", 3, 8),
            Triple("Barbell Row", 3, 8),
            Triple("Overhead Press", 3, 10),
            Triple("Plank", 3, 1),
        ),
    )
}
