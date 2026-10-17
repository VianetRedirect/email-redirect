package com.vianet.workoutlog.ui.workout

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vianet.workoutlog.data.FinishResult
import com.vianet.workoutlog.data.Settings
import com.vianet.workoutlog.data.WorkoutRepository
import com.vianet.workoutlog.data.db.Exercise
import com.vianet.workoutlog.data.db.SetEntry
import com.vianet.workoutlog.data.db.Workout
import com.vianet.workoutlog.data.db.WorkoutExerciseWithSets
import com.vianet.workoutlog.domain.WeightUnit
import com.vianet.workoutlog.domain.bestEstimatedOneRepMax
import com.vianet.workoutlog.timer.RestTimer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Previous performance for one exercise, loaded once per workout. */
data class ExerciseContext(
    val previousSets: List<SetEntry>,
    val previousBestOneRepMax: Double?,
)

data class ExerciseBlockUi(
    val block: WorkoutExerciseWithSets,
    val context: ExerciseContext,
) {
    val exercise: Exercise get() = block.exercise
    val sets: List<SetEntry> get() = block.orderedSets
    val restSeconds: Int get() = block.workoutExercise.restSeconds

    /** The set from the last session with the same set number, else the last one logged. */
    fun previousFor(setNumber: Int): SetEntry? =
        context.previousSets.getOrNull(setNumber - 1) ?: context.previousSets.lastOrNull()
}

data class ActiveWorkoutUi(
    val workout: Workout,
    val blocks: List<ExerciseBlockUi>,
) {
    val completedSets: Int get() = blocks.sumOf { b -> b.sets.count { it.completed } }
}

class ActiveWorkoutViewModel(
    private val repository: WorkoutRepository,
    private val settings: Settings,
    private val appContext: Context,
    private val workoutId: Long,
) : ViewModel() {

    private val contexts = MutableStateFlow<Map<Long, ExerciseContext>>(emptyMap())
    private val workoutFlow = repository.observeWorkout(workoutId)

    val weightUnit: StateFlow<WeightUnit> = settings.weightUnit
    val restTimer: StateFlow<RestTimer.State?> = RestTimer.state

    val ui: StateFlow<ActiveWorkoutUi?> = combine(workoutFlow, contexts) { workout, ctx ->
        workout?.let { w ->
            ActiveWorkoutUi(
                workout = w.workout,
                blocks = w.orderedExercises.map { block ->
                    ExerciseBlockUi(block, ctx[block.exercise.id] ?: ExerciseContext(emptyList(), null))
                },
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val exercises: StateFlow<List<Exercise>> = repository.observeExercises()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        viewModelScope.launch {
            workoutFlow
                .map { w -> w?.exercises?.map { it.exercise.id }?.toSet() ?: emptySet() }
                .distinctUntilChanged()
                .collect { ids ->
                    val missing = ids - contexts.value.keys
                    if (missing.isEmpty()) return@collect
                    val loaded = missing.associateWith { exerciseId ->
                        ExerciseContext(
                            previousSets = repository.previousSets(exerciseId, workoutId),
                            previousBestOneRepMax = repository.completedSetsBefore(exerciseId, workoutId).bestEstimatedOneRepMax(),
                        )
                    }
                    contexts.value = contexts.value + loaded
                }
        }
    }

    fun updateSetValues(set: SetEntry, weightKg: Double?, reps: Int?) {
        if (set.weightKg == weightKg && set.reps == reps) return
        viewModelScope.launch { repository.updateSet(set.copy(weightKg = weightKg, reps = reps)) }
    }

    fun updateDistanceValues(set: SetEntry, distanceKm: Double?, durationSeconds: Int?) {
        if (set.distanceKm == distanceKm && set.durationSeconds == durationSeconds) return
        viewModelScope.launch { repository.updateSet(set.copy(distanceKm = distanceKm, durationSeconds = durationSeconds)) }
    }

    /** Ticks or unticks a set. Blank fields take the previous session's numbers when ticked. */
    fun toggleCompleted(set: SetEntry, block: ExerciseBlockUi) {
        viewModelScope.launch {
            if (set.completed) {
                repository.updateSet(set.copy(completed = false, completedAt = null))
            } else {
                val previous = block.previousFor(set.setNumber)
                val filled = if (block.exercise.isDistance) {
                    set.copy(
                        distanceKm = set.distanceKm ?: previous?.distanceKm,
                        durationSeconds = set.durationSeconds ?: previous?.durationSeconds,
                    )
                } else {
                    set.copy(weightKg = set.weightKg ?: previous?.weightKg, reps = set.reps ?: previous?.reps)
                }
                repository.updateSet(filled.copy(completed = true, completedAt = System.currentTimeMillis()))
                if (settings.autoStartRest.value) RestTimer.start(appContext, block.restSeconds)
            }
        }
    }

    fun addSet(block: ExerciseBlockUi) {
        viewModelScope.launch { repository.addSet(block.block.workoutExercise.id) }
    }

    fun deleteSet(set: SetEntry) {
        viewModelScope.launch { repository.deleteSet(set.id) }
    }

    fun addExercise(exercise: Exercise) {
        viewModelScope.launch { repository.addExerciseToWorkout(workoutId, exercise.id, settings.defaultRestSeconds.value) }
    }

    fun createAndAddExercise(name: String, bodyPart: String, category: String, trackingType: String) {
        viewModelScope.launch {
            val id = repository.createExercise(name, bodyPart, category, trackingType)
            repository.addExerciseToWorkout(workoutId, id, settings.defaultRestSeconds.value)
        }
    }

    fun removeExercise(block: ExerciseBlockUi) {
        viewModelScope.launch { repository.removeWorkoutExercise(block.block.workoutExercise.id) }
    }

    fun setRest(block: ExerciseBlockUi, seconds: Int) {
        viewModelScope.launch { repository.updateWorkoutExercise(block.block.workoutExercise.copy(restSeconds = seconds)) }
    }

    fun rename(name: String) {
        val workout = ui.value?.workout ?: return
        viewModelScope.launch { repository.updateWorkout(workout.copy(name = name.trim().ifEmpty { "Workout" })) }
    }

    fun updateNotes(notes: String) {
        val workout = ui.value?.workout ?: return
        viewModelScope.launch { repository.updateWorkout(workout.copy(notes = notes)) }
    }

    fun adjustRest(delta: Int) = RestTimer.adjust(delta)
    fun skipRest() = RestTimer.skip()

    fun startRest(block: ExerciseBlockUi) = RestTimer.start(appContext, block.restSeconds)

    /** Finishes the workout. The result says whether it was saved and which milestones it unlocked. */
    fun finish(onDone: (FinishResult) -> Unit) {
        viewModelScope.launch {
            RestTimer.skip()
            onDone(repository.finishWorkout(workoutId, settings.weeklyGoal.value))
        }
    }

    fun cancel(onDone: () -> Unit) {
        viewModelScope.launch {
            RestTimer.skip()
            repository.deleteWorkout(workoutId)
            onDone()
        }
    }
}
