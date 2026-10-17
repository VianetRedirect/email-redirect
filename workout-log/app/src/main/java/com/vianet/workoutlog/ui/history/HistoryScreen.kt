package com.vianet.workoutlog.ui.history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.vianet.workoutlog.data.Settings
import com.vianet.workoutlog.data.WorkoutRepository
import com.vianet.workoutlog.data.db.WorkoutWithExercises
import com.vianet.workoutlog.domain.WeightUnit
import com.vianet.workoutlog.domain.completedCount
import com.vianet.workoutlog.domain.formatDay
import com.vianet.workoutlog.domain.formatDurationShort
import com.vianet.workoutlog.domain.formatKm
import com.vianet.workoutlog.domain.formatWeight
import com.vianet.workoutlog.domain.heaviestSet
import com.vianet.workoutlog.domain.totalVolume
import com.vianet.workoutlog.ui.appViewModel
import com.vianet.workoutlog.ui.common.EmptyState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class HistoryViewModel(repository: WorkoutRepository, settings: Settings) : ViewModel() {
    val workouts: StateFlow<List<WorkoutWithExercises>> = repository.observeCompletedWorkouts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val weightUnit: StateFlow<WeightUnit> = settings.weightUnit
}

@Composable
fun HistoryScreen(
    bottomBar: @Composable () -> Unit,
    onOpenWorkout: (Long) -> Unit,
) {
    val viewModel: HistoryViewModel = appViewModel { HistoryViewModel(it.repository, it.settings) }
    val workouts by viewModel.workouts.collectAsStateWithLifecycle()
    val unit by viewModel.weightUnit.collectAsStateWithLifecycle()

    Scaffold(
        topBar = { TopAppBar(title = { Text("History") }) },
        bottomBar = bottomBar,
    ) { padding ->
        if (workouts.isEmpty()) {
            Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                EmptyState(title = "No workouts yet", body = "Finished workouts show up here with their sets and volume.")
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(workouts, key = { it.workout.id }) { workout ->
                    WorkoutCard(workout = workout, unit = unit, onClick = { onOpenWorkout(workout.workout.id) })
                }
            }
        }
    }
}

@Composable
private fun WorkoutCard(workout: WorkoutWithExercises, unit: WeightUnit, onClick: () -> Unit) {
    val allSets = workout.exercises.flatMap { it.sets }
    val sets = allSets.completedCount()
    val volume = allSets.totalVolume()
    val duration = workout.workout.endedAt?.let { it - workout.workout.startedAt } ?: 0L

    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(workout.workout.name, style = MaterialTheme.typography.titleMedium)
            Text(
                "${formatDay(workout.workout.startedAt)} · ${formatDurationShort(duration)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("$sets ${if (sets == 1) "set" else "sets"}", style = MaterialTheme.typography.bodyMedium)
                Text("${formatWeight(volume, unit)} ${unit.label} volume", style = MaterialTheme.typography.bodyMedium)
            }
            Spacer(Modifier.height(8.dp))
            workout.orderedExercises.forEach { block ->
                val best = block.sets.heaviestSet()
                val km = block.sets.filter { it.completed }.sumOf { it.distanceKm ?: 0.0 }
                Text(
                    text = buildString {
                        append("${block.sets.completedCount()} × ${block.exercise.name}")
                        if (block.exercise.isDistance) {
                            if (km > 0) append("   ${formatKm(km)} km")
                        } else if (best != null) {
                            append("   best ${formatWeight(best.weightKg, unit)} ${unit.label} × ${best.reps}")
                        }
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
