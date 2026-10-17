package com.vianet.workoutlog.data.db

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity(tableName = "exercises")
data class Exercise(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    /** Chest, Back, Legs, Shoulders, Arms, Core, Full body, Cardio */
    val bodyPart: String,
    /** Barbell, Dumbbell, Machine, Cable, Bodyweight, Other */
    val category: String,
    val isCustom: Boolean = false,
    /** [TrackingType.WEIGHT_REPS] or [TrackingType.DISTANCE_TIME]. */
    val trackingType: String = TrackingType.WEIGHT_REPS,
) {
    val isDistance: Boolean get() = trackingType == TrackingType.DISTANCE_TIME
}

object TrackingType {
    const val WEIGHT_REPS = "WEIGHT_REPS"
    const val DISTANCE_TIME = "DISTANCE_TIME"
}

@Entity(tableName = "templates")
data class Template(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
)

@Entity(
    tableName = "template_exercises",
    foreignKeys = [
        ForeignKey(entity = Template::class, parentColumns = ["id"], childColumns = ["templateId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = Exercise::class, parentColumns = ["id"], childColumns = ["exerciseId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("templateId"), Index("exerciseId")],
)
data class TemplateExercise(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val templateId: Long,
    val exerciseId: Long,
    val position: Int,
    val targetSets: Int = 3,
    val targetReps: Int? = null,
    /** Null means "use the default rest time from settings". */
    val restSeconds: Int? = null,
)

@Entity(
    tableName = "workouts",
    foreignKeys = [
        ForeignKey(entity = Template::class, parentColumns = ["id"], childColumns = ["templateId"], onDelete = ForeignKey.SET_NULL),
    ],
    indices = [Index("templateId"), Index("startedAt"), Index("endedAt")],
)
data class Workout(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val templateId: Long? = null,
    val name: String,
    val startedAt: Long,
    /** Null while the workout is still in progress. */
    val endedAt: Long? = null,
    val notes: String = "",
)

@Entity(
    tableName = "workout_exercises",
    foreignKeys = [
        ForeignKey(entity = Workout::class, parentColumns = ["id"], childColumns = ["workoutId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = Exercise::class, parentColumns = ["id"], childColumns = ["exerciseId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("workoutId"), Index("exerciseId")],
)
data class WorkoutExercise(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val workoutId: Long,
    val exerciseId: Long,
    val position: Int,
    val restSeconds: Int,
)

@Entity(
    tableName = "set_entries",
    foreignKeys = [
        ForeignKey(entity = WorkoutExercise::class, parentColumns = ["id"], childColumns = ["workoutExerciseId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("workoutExerciseId")],
)
data class SetEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val workoutExerciseId: Long,
    val setNumber: Int,
    val weightKg: Double? = null,
    val reps: Int? = null,
    /** Used by distance exercises instead of weight and reps. */
    val distanceKm: Double? = null,
    val durationSeconds: Int? = null,
    val completed: Boolean = false,
    val completedAt: Long? = null,
)

/** A milestone the user has reached, keyed by the milestone's id. */
@Entity(tableName = "achievements")
data class Achievement(
    @PrimaryKey val key: String,
    val achievedAt: Long,
    val workoutId: Long? = null,
)

// ---- Relations ----

data class TemplateExerciseWithExercise(
    @Embedded val templateExercise: TemplateExercise,
    @Relation(parentColumn = "exerciseId", entityColumn = "id")
    val exercise: Exercise,
)

data class TemplateWithExercises(
    @Embedded val template: Template,
    @Relation(entity = TemplateExercise::class, parentColumn = "id", entityColumn = "templateId")
    val exercises: List<TemplateExerciseWithExercise>,
) {
    val orderedExercises: List<TemplateExerciseWithExercise>
        get() = exercises.sortedBy { it.templateExercise.position }
}

data class WorkoutExerciseWithSets(
    @Embedded val workoutExercise: WorkoutExercise,
    @Relation(parentColumn = "exerciseId", entityColumn = "id")
    val exercise: Exercise,
    @Relation(parentColumn = "id", entityColumn = "workoutExerciseId")
    val sets: List<SetEntry>,
) {
    val orderedSets: List<SetEntry>
        get() = sets.sortedBy { it.setNumber }
}

data class WorkoutWithExercises(
    @Embedded val workout: Workout,
    @Relation(entity = WorkoutExercise::class, parentColumn = "id", entityColumn = "workoutId")
    val exercises: List<WorkoutExerciseWithSets>,
) {
    val orderedExercises: List<WorkoutExerciseWithSets>
        get() = exercises.sortedBy { it.workoutExercise.position }
}

/** One completed set of a given exercise together with the workout it belongs to. */
data class ExerciseSetRecord(
    val setId: Long,
    val weightKg: Double?,
    val reps: Int?,
    val distanceKm: Double?,
    val durationSeconds: Int?,
    val completedAt: Long?,
    val workoutId: Long,
    val startedAt: Long,
    val workoutName: String,
)
