package com.vianet.workoutlog.ui.template

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vianet.workoutlog.data.TemplateRow
import com.vianet.workoutlog.data.WorkoutRepository
import com.vianet.workoutlog.data.db.Exercise
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TemplateEditorViewModel(
    private val repository: WorkoutRepository,
    private val templateId: Long?,
) : ViewModel() {

    var name by mutableStateOf("")
    var notes by mutableStateOf("")
    val rows = mutableStateListOf<TemplateRow>()
    var loaded by mutableStateOf(templateId == null)
        private set

    val exercises: StateFlow<List<Exercise>> = repository.observeExercises()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        if (templateId != null) {
            viewModelScope.launch {
                repository.getTemplate(templateId)?.let { t ->
                    name = t.template.name
                    notes = t.template.notes
                    rows.clear()
                    rows.addAll(
                        t.orderedExercises.map {
                            TemplateRow(
                                exerciseId = it.exercise.id,
                                exerciseName = it.exercise.name,
                                targetSets = it.templateExercise.targetSets,
                                targetReps = it.templateExercise.targetReps,
                                restSeconds = it.templateExercise.restSeconds,
                            )
                        }
                    )
                }
                loaded = true
            }
        }
    }

    fun addExercise(exercise: Exercise) {
        rows.add(TemplateRow(exerciseId = exercise.id, exerciseName = exercise.name, targetSets = 3, targetReps = null, restSeconds = null))
    }

    fun createExercise(name: String, bodyPart: String, category: String, trackingType: String) {
        viewModelScope.launch {
            val id = repository.createExercise(name, bodyPart, category, trackingType)
            rows.add(TemplateRow(exerciseId = id, exerciseName = name.trim(), targetSets = 3, targetReps = null, restSeconds = null))
        }
    }

    fun updateRow(index: Int, row: TemplateRow) {
        if (index in rows.indices) rows[index] = row
    }

    fun removeRow(index: Int) {
        if (index in rows.indices) rows.removeAt(index)
    }

    fun move(index: Int, delta: Int) {
        val target = index + delta
        if (index !in rows.indices || target !in rows.indices) return
        val row = rows.removeAt(index)
        rows.add(target, row)
    }

    val canSave: Boolean
        get() = name.isNotBlank() && rows.isNotEmpty()

    fun save(onSaved: () -> Unit) {
        if (!canSave) return
        viewModelScope.launch {
            repository.saveTemplate(templateId, name, notes, rows.toList())
            onSaved()
        }
    }
}
