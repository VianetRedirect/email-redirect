package com.vianet.workoutlog.ui.start

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vianet.workoutlog.data.Settings
import com.vianet.workoutlog.data.WorkoutRepository
import com.vianet.workoutlog.data.db.TemplateWithExercises
import com.vianet.workoutlog.data.db.Workout
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class StartViewModel(
    private val repository: WorkoutRepository,
    private val settings: Settings,
) : ViewModel() {

    val templates: StateFlow<List<TemplateWithExercises>> = repository.observeTemplates()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val activeWorkout: StateFlow<Workout?> = repository.observeActiveWorkout()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun startWorkout(templateId: Long?, onStarted: (Long) -> Unit) {
        viewModelScope.launch {
            val existing = repository.getActiveWorkout()
            val id = existing?.id ?: repository.startWorkout(templateId, settings.defaultRestSeconds.value)
            onStarted(id)
        }
    }

    fun deleteTemplate(id: Long) {
        viewModelScope.launch { repository.deleteTemplate(id) }
    }
}
