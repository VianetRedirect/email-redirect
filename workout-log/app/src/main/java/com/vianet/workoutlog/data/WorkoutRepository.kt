package com.vianet.workoutlog.data

import androidx.room.withTransaction
import com.vianet.workoutlog.data.db.Achievement
import com.vianet.workoutlog.data.db.AppDatabase
import com.vianet.workoutlog.data.db.Exercise
import com.vianet.workoutlog.data.db.ExerciseSetRecord
import com.vianet.workoutlog.data.db.SeedData
import com.vianet.workoutlog.data.db.SetEntry
import com.vianet.workoutlog.data.db.Template
import com.vianet.workoutlog.data.db.TemplateExercise
import com.vianet.workoutlog.data.db.TemplateWithExercises
import com.vianet.workoutlog.data.db.Workout
import com.vianet.workoutlog.data.db.WorkoutExercise
import com.vianet.workoutlog.data.db.WorkoutWithExercises
import com.vianet.workoutlog.domain.Milestone
import com.vianet.workoutlog.domain.Milestones
import com.vianet.workoutlog.domain.ProgressStats
import com.vianet.workoutlog.domain.StreakInfo
import com.vianet.workoutlog.domain.computeStreak
import com.vianet.workoutlog.domain.epochMillisToDate
import com.vianet.workoutlog.domain.isReached
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/** A row in the template editor: which exercise, how many sets, target reps and rest. */
data class TemplateRow(
    val exerciseId: Long,
    val exerciseName: String,
    val targetSets: Int,
    val targetReps: Int?,
    val restSeconds: Int?,
)

/** Outcome of finishing a workout. */
data class FinishResult(
    val saved: Boolean,
    val newMilestones: List<Milestone> = emptyList(),
)

class WorkoutRepository(private val db: AppDatabase) {
    private val exerciseDao = db.exerciseDao()
    private val templateDao = db.templateDao()
    private val workoutDao = db.workoutDao()
    private val achievementDao = db.achievementDao()

    // ---- Seed ----

    suspend fun seedIfEmpty() {
        if (exerciseDao.count() > 0) return
        db.withTransaction {
            val ids = exerciseDao.insertAll(SeedData.exercises)
            val idByName = SeedData.exercises.map { it.name }.zip(ids).toMap()
            SeedData.templates.forEach { (name, rows) ->
                val templateId = templateDao.insert(Template(name = name))
                val entries = rows.mapIndexedNotNull { index, (exerciseName, sets, reps) ->
                    val exerciseId = idByName[exerciseName] ?: return@mapIndexedNotNull null
                    TemplateExercise(
                        templateId = templateId,
                        exerciseId = exerciseId,
                        position = index,
                        targetSets = sets,
                        targetReps = reps,
                    )
                }
                templateDao.insertExercises(entries)
            }
        }
    }

    // ---- Exercises ----

    fun observeExercises(): Flow<List<Exercise>> = exerciseDao.observeAll()
    fun observeExercise(id: Long): Flow<Exercise?> = exerciseDao.observe(id)
    fun observeExerciseHistory(id: Long): Flow<List<ExerciseSetRecord>> = exerciseDao.observeHistory(id)

    suspend fun createExercise(name: String, bodyPart: String, category: String, trackingType: String): Long =
        exerciseDao.insert(
            Exercise(name = name.trim(), bodyPart = bodyPart, category = category, isCustom = true, trackingType = trackingType)
        )

    suspend fun updateExercise(exercise: Exercise) = exerciseDao.update(exercise)

    // ---- Templates ----

    fun observeTemplates(): Flow<List<TemplateWithExercises>> = templateDao.observeAllWithExercises()
    suspend fun getTemplate(id: Long): TemplateWithExercises? = templateDao.getWithExercises(id)
    suspend fun deleteTemplate(id: Long) = templateDao.deleteById(id)

    /** Creates or replaces a template. Returns the template id. */
    suspend fun saveTemplate(id: Long?, name: String, notes: String, rows: List<TemplateRow>): Long = db.withTransaction {
        val templateId = if (id == null) {
            templateDao.insert(Template(name = name.trim(), notes = notes))
        } else {
            val existing = templateDao.getWithExercises(id)?.template
            if (existing == null) {
                templateDao.insert(Template(name = name.trim(), notes = notes))
            } else {
                templateDao.update(existing.copy(name = name.trim(), notes = notes))
                templateDao.deleteExercisesFor(id)
                id
            }
        }
        templateDao.insertExercises(
            rows.mapIndexed { index, row ->
                TemplateExercise(
                    templateId = templateId,
                    exerciseId = row.exerciseId,
                    position = index,
                    targetSets = row.targetSets.coerceAtLeast(1),
                    targetReps = row.targetReps,
                    restSeconds = row.restSeconds,
                )
            }
        )
        templateId
    }

    // ---- Workouts ----

    fun observeActiveWorkout(): Flow<Workout?> = workoutDao.observeActive()
    suspend fun getActiveWorkout(): Workout? = workoutDao.getActive()
    fun observeWorkout(id: Long): Flow<WorkoutWithExercises?> = workoutDao.observeWithExercises(id)
    fun observeCompletedWorkouts(): Flow<List<WorkoutWithExercises>> = workoutDao.observeCompleted()

    /** Starts a workout, from a template when [templateId] is given. Returns the workout id. */
    suspend fun startWorkout(templateId: Long?, defaultRestSeconds: Int): Long = db.withTransaction {
        val template = templateId?.let { templateDao.getWithExercises(it) }
        val workoutId = workoutDao.insertWorkout(
            Workout(
                templateId = template?.template?.id,
                name = template?.template?.name ?: "Workout",
                startedAt = System.currentTimeMillis(),
            )
        )
        template?.orderedExercises?.forEachIndexed { index, row ->
            val workoutExerciseId = workoutDao.insertWorkoutExercise(
                WorkoutExercise(
                    workoutId = workoutId,
                    exerciseId = row.exercise.id,
                    position = index,
                    restSeconds = row.templateExercise.restSeconds ?: defaultRestSeconds,
                )
            )
            repeat(row.templateExercise.targetSets.coerceAtLeast(1)) { n ->
                workoutDao.insertSet(SetEntry(workoutExerciseId = workoutExerciseId, setNumber = n + 1))
            }
        }
        workoutId
    }

    suspend fun addExerciseToWorkout(workoutId: Long, exerciseId: Long, restSeconds: Int, initialSets: Int = 3): Long =
        db.withTransaction {
            val position = workoutDao.workoutExercisesFor(workoutId).size
            val id = workoutDao.insertWorkoutExercise(
                WorkoutExercise(workoutId = workoutId, exerciseId = exerciseId, position = position, restSeconds = restSeconds)
            )
            repeat(initialSets) { n -> workoutDao.insertSet(SetEntry(workoutExerciseId = id, setNumber = n + 1)) }
            id
        }

    suspend fun removeWorkoutExercise(workoutExerciseId: Long) = workoutDao.deleteWorkoutExercise(workoutExerciseId)

    suspend fun updateWorkoutExercise(workoutExercise: WorkoutExercise) = workoutDao.updateWorkoutExercise(workoutExercise)

    suspend fun addSet(workoutExerciseId: Long): Long = db.withTransaction {
        val existing = workoutDao.setsFor(workoutExerciseId)
        val last = existing.lastOrNull()
        workoutDao.insertSet(
            SetEntry(
                workoutExerciseId = workoutExerciseId,
                setNumber = (last?.setNumber ?: 0) + 1,
                weightKg = last?.weightKg,
                reps = last?.reps,
                distanceKm = last?.distanceKm,
                durationSeconds = last?.durationSeconds,
            )
        )
    }

    suspend fun updateSet(set: SetEntry) = workoutDao.updateSet(set)

    suspend fun deleteSet(setId: Long) = db.withTransaction {
        val set = workoutDao.getSet(setId) ?: return@withTransaction
        workoutDao.deleteSet(setId)
        // Keep set numbers contiguous so the list reads 1, 2, 3.
        workoutDao.setsFor(set.workoutExerciseId).forEachIndexed { index, s ->
            if (s.setNumber != index + 1) workoutDao.updateSet(s.copy(setNumber = index + 1))
        }
    }

    suspend fun previousSets(exerciseId: Long, excludeWorkoutId: Long): List<SetEntry> =
        workoutDao.previousSets(exerciseId, excludeWorkoutId)

    suspend fun completedSetsBefore(exerciseId: Long, excludeWorkoutId: Long): List<SetEntry> =
        workoutDao.completedSetsFor(exerciseId, excludeWorkoutId)

    suspend fun updateWorkout(workout: Workout) = workoutDao.updateWorkout(workout)

    /**
     * Finishes the workout: drops sets that were never ticked and exercises left empty.
     * When nothing was completed the workout is deleted and [FinishResult.saved] is false.
     * Otherwise any milestones crossed by this workout are recorded and returned.
     */
    suspend fun finishWorkout(workoutId: Long, weeklyGoal: Int): FinishResult {
        val before = progressStats(weeklyGoal)
        val saved = db.withTransaction {
            val workout = workoutDao.getWithExercises(workoutId) ?: return@withTransaction false
            if (workoutDao.completedSetCount(workoutId) == 0) {
                workoutDao.deleteWorkout(workoutId)
                return@withTransaction false
            }
            workout.exercises.forEach { block ->
                workoutDao.deleteIncompleteSets(block.workoutExercise.id)
                val remaining = workoutDao.setsFor(block.workoutExercise.id)
                if (remaining.isEmpty()) {
                    workoutDao.deleteWorkoutExercise(block.workoutExercise.id)
                } else {
                    remaining.forEachIndexed { index, s ->
                        if (s.setNumber != index + 1) workoutDao.updateSet(s.copy(setNumber = index + 1))
                    }
                }
            }
            // Re-pack exercise positions after any removals.
            workoutDao.workoutExercisesFor(workoutId).forEachIndexed { index, we ->
                if (we.position != index) workoutDao.updateWorkoutExercise(we.copy(position = index))
            }
            workoutDao.updateWorkout(workout.workout.copy(endedAt = System.currentTimeMillis()))
            true
        }
        if (!saved) return FinishResult(saved = false)

        val after = progressStats(weeklyGoal)
        val already = achievementDao.getAll().map { it.key }.toSet()
        val fresh = Milestones.newlyReached(before, after).filter { it.key !in already }
        // Anything reached but never recorded (e.g. a milestone added in an update) is stored quietly.
        val now = System.currentTimeMillis()
        val quiet = Milestones.all.filter { it.isReached(after) && it.key !in already && it !in fresh }
        achievementDao.insertAll(
            fresh.map { Achievement(key = it.key, achievedAt = now, workoutId = workoutId) } +
                quiet.map { Achievement(key = it.key, achievedAt = now, workoutId = null) }
        )
        return FinishResult(saved = true, newMilestones = fresh)
    }

    // ---- Progress, streaks and achievements ----

    suspend fun streak(weeklyGoal: Int, today: LocalDate = LocalDate.now()): StreakInfo {
        val dates = workoutDao.completedWorkoutStartTimes().map { epochMillisToDate(it) }
        return computeStreak(dates, weeklyGoal, today)
    }

    fun observeWorkoutStartTimes(): Flow<List<Long>> = workoutDao.observeCompletedWorkoutStartTimes()

    suspend fun progressStats(weeklyGoal: Int): ProgressStats = ProgressStats(
        totalVolumeKg = workoutDao.totalVolumeKg(),
        totalDistanceKm = workoutDao.totalDistanceKm(),
        workoutCount = workoutDao.completedWorkoutCount(),
        streakWeeks = streak(weeklyGoal).currentWeeks,
    )

    fun observeAchievements(): Flow<List<Achievement>> = achievementDao.observeAll()

    suspend fun deleteWorkout(workoutId: Long) = workoutDao.deleteWorkout(workoutId)
}
