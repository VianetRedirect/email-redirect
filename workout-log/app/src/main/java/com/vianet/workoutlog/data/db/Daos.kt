package com.vianet.workoutlog.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ExerciseDao {
    @Query("SELECT * FROM exercises ORDER BY name COLLATE NOCASE")
    fun observeAll(): Flow<List<Exercise>>

    @Query("SELECT * FROM exercises WHERE id = :id")
    fun observe(id: Long): Flow<Exercise?>

    @Query("SELECT * FROM exercises WHERE id = :id")
    suspend fun get(id: Long): Exercise?

    @Query("SELECT COUNT(*) FROM exercises")
    suspend fun count(): Int

    @Insert
    suspend fun insert(exercise: Exercise): Long

    @Insert
    suspend fun insertAll(exercises: List<Exercise>): List<Long>

    @Update
    suspend fun update(exercise: Exercise)

    @Query(
        """
        SELECT s.id AS setId, s.weightKg AS weightKg, s.reps AS reps,
               s.distanceKm AS distanceKm, s.durationSeconds AS durationSeconds, s.completedAt AS completedAt,
               w.id AS workoutId, w.startedAt AS startedAt, w.name AS workoutName
        FROM set_entries s
        JOIN workout_exercises we ON we.id = s.workoutExerciseId
        JOIN workouts w ON w.id = we.workoutId
        WHERE we.exerciseId = :exerciseId AND s.completed = 1 AND w.endedAt IS NOT NULL
        ORDER BY w.startedAt DESC, s.setNumber ASC
        """
    )
    fun observeHistory(exerciseId: Long): Flow<List<ExerciseSetRecord>>
}

@Dao
interface TemplateDao {
    @Transaction
    @Query("SELECT * FROM templates ORDER BY createdAt ASC")
    fun observeAllWithExercises(): Flow<List<TemplateWithExercises>>

    @Transaction
    @Query("SELECT * FROM templates WHERE id = :id")
    suspend fun getWithExercises(id: Long): TemplateWithExercises?

    @Query("SELECT COUNT(*) FROM templates")
    suspend fun count(): Int

    @Query("SELECT * FROM templates WHERE name = :name LIMIT 1")
    suspend fun findByName(name: String): Template?

    @Insert
    suspend fun insert(template: Template): Long

    @Update
    suspend fun update(template: Template)

    @Delete
    suspend fun delete(template: Template)

    @Query("DELETE FROM templates WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Insert
    suspend fun insertExercises(exercises: List<TemplateExercise>)

    @Query("DELETE FROM template_exercises WHERE templateId = :templateId")
    suspend fun deleteExercisesFor(templateId: Long)
}

@Dao
interface WorkoutDao {
    @Transaction
    @Query("SELECT * FROM workouts WHERE id = :id")
    fun observeWithExercises(id: Long): Flow<WorkoutWithExercises?>

    @Transaction
    @Query("SELECT * FROM workouts WHERE id = :id")
    suspend fun getWithExercises(id: Long): WorkoutWithExercises?

    @Query("SELECT * FROM workouts WHERE endedAt IS NULL ORDER BY startedAt DESC LIMIT 1")
    fun observeActive(): Flow<Workout?>

    @Query("SELECT * FROM workouts WHERE endedAt IS NULL ORDER BY startedAt DESC LIMIT 1")
    suspend fun getActive(): Workout?

    @Transaction
    @Query("SELECT * FROM workouts WHERE endedAt IS NOT NULL ORDER BY startedAt DESC")
    fun observeCompleted(): Flow<List<WorkoutWithExercises>>

    @Insert
    suspend fun insertWorkout(workout: Workout): Long

    @Update
    suspend fun updateWorkout(workout: Workout)

    @Query("DELETE FROM workouts WHERE id = :id")
    suspend fun deleteWorkout(id: Long)

    @Insert
    suspend fun insertWorkoutExercise(workoutExercise: WorkoutExercise): Long

    @Update
    suspend fun updateWorkoutExercise(workoutExercise: WorkoutExercise)

    @Query("DELETE FROM workout_exercises WHERE id = :id")
    suspend fun deleteWorkoutExercise(id: Long)

    @Query("SELECT * FROM workout_exercises WHERE workoutId = :workoutId ORDER BY position")
    suspend fun workoutExercisesFor(workoutId: Long): List<WorkoutExercise>

    @Insert
    suspend fun insertSet(set: SetEntry): Long

    @Update
    suspend fun updateSet(set: SetEntry)

    @Query("SELECT * FROM set_entries WHERE id = :id")
    suspend fun getSet(id: Long): SetEntry?

    @Query("DELETE FROM set_entries WHERE id = :id")
    suspend fun deleteSet(id: Long)

    @Query("SELECT * FROM set_entries WHERE workoutExerciseId = :workoutExerciseId ORDER BY setNumber")
    suspend fun setsFor(workoutExerciseId: Long): List<SetEntry>

    @Query("DELETE FROM set_entries WHERE workoutExerciseId = :workoutExerciseId AND completed = 0")
    suspend fun deleteIncompleteSets(workoutExerciseId: Long)

    @Query(
        """
        SELECT COUNT(*) FROM set_entries s
        JOIN workout_exercises we ON we.id = s.workoutExerciseId
        WHERE we.workoutId = :workoutId AND s.completed = 1
        """
    )
    suspend fun completedSetCount(workoutId: Long): Int

    /** Completed sets of this exercise from the most recent finished workout that included it. */
    @Query(
        """
        SELECT s.* FROM set_entries s
        JOIN workout_exercises we ON we.id = s.workoutExerciseId
        WHERE we.exerciseId = :exerciseId AND s.completed = 1 AND we.workoutId = (
            SELECT w.id FROM workouts w
            JOIN workout_exercises we2 ON we2.workoutId = w.id
            WHERE we2.exerciseId = :exerciseId AND w.endedAt IS NOT NULL AND w.id != :excludeWorkoutId
            ORDER BY w.startedAt DESC LIMIT 1
        )
        ORDER BY we.position ASC, s.setNumber ASC
        """
    )
    suspend fun previousSets(exerciseId: Long, excludeWorkoutId: Long): List<SetEntry>

    // ---- Totals for milestones and streaks ----

    @Query(
        """
        SELECT COALESCE(SUM(s.weightKg * s.reps), 0) FROM set_entries s
        JOIN workout_exercises we ON we.id = s.workoutExerciseId
        JOIN workouts w ON w.id = we.workoutId
        WHERE s.completed = 1 AND w.endedAt IS NOT NULL AND s.weightKg IS NOT NULL AND s.reps IS NOT NULL
        """
    )
    suspend fun totalVolumeKg(): Double

    @Query(
        """
        SELECT COALESCE(SUM(s.distanceKm), 0) FROM set_entries s
        JOIN workout_exercises we ON we.id = s.workoutExerciseId
        JOIN workouts w ON w.id = we.workoutId
        WHERE s.completed = 1 AND w.endedAt IS NOT NULL AND s.distanceKm IS NOT NULL
        """
    )
    suspend fun totalDistanceKm(): Double

    @Query("SELECT COUNT(*) FROM workouts WHERE endedAt IS NOT NULL")
    suspend fun completedWorkoutCount(): Int

    @Query("SELECT startedAt FROM workouts WHERE endedAt IS NOT NULL ORDER BY startedAt ASC")
    suspend fun completedWorkoutStartTimes(): List<Long>

    @Query("SELECT startedAt FROM workouts WHERE endedAt IS NOT NULL ORDER BY startedAt ASC")
    fun observeCompletedWorkoutStartTimes(): Flow<List<Long>>

    /** Every completed set of this exercise from finished workouts, other than the one given. */
    @Query(
        """
        SELECT s.* FROM set_entries s
        JOIN workout_exercises we ON we.id = s.workoutExerciseId
        JOIN workouts w ON w.id = we.workoutId
        WHERE we.exerciseId = :exerciseId AND s.completed = 1 AND w.endedAt IS NOT NULL AND w.id != :excludeWorkoutId
        """
    )
    suspend fun completedSetsFor(exerciseId: Long, excludeWorkoutId: Long): List<SetEntry>
}

@Dao
interface AchievementDao {
    @Query("SELECT * FROM achievements ORDER BY achievedAt DESC")
    fun observeAll(): Flow<List<Achievement>>

    @Query("SELECT * FROM achievements")
    suspend fun getAll(): List<Achievement>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(achievements: List<Achievement>)
}
