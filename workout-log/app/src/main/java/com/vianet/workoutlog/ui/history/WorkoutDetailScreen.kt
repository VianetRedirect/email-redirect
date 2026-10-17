package com.vianet.workoutlog.ui.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.vianet.workoutlog.data.Settings
import com.vianet.workoutlog.data.WorkoutRepository
import com.vianet.workoutlog.data.db.WorkoutWithExercises
import com.vianet.workoutlog.domain.WeightUnit
import com.vianet.workoutlog.domain.completedCount
import com.vianet.workoutlog.domain.estimatedOneRepMax
import com.vianet.workoutlog.domain.formatClock
import com.vianet.workoutlog.domain.formatDay
import com.vianet.workoutlog.domain.formatKm
import com.vianet.workoutlog.domain.formatDurationShort
import com.vianet.workoutlog.domain.formatTime
import com.vianet.workoutlog.domain.formatWeight
import com.vianet.workoutlog.domain.totalVolume
import com.vianet.workoutlog.ui.appViewModel
import com.vianet.workoutlog.ui.common.ConfirmDialog
import com.vianet.workoutlog.ui.common.StatTile
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class WorkoutDetailViewModel(
    private val repository: WorkoutRepository,
    settings: Settings,
    private val workoutId: Long,
) : ViewModel() {
    val workout: StateFlow<WorkoutWithExercises?> = repository.observeWorkout(workoutId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    val weightUnit: StateFlow<WeightUnit> = settings.weightUnit

    fun delete(onDone: () -> Unit) {
        viewModelScope.launch {
            repository.deleteWorkout(workoutId)
            onDone()
        }
    }
}

@Composable
fun WorkoutDetailScreen(workoutId: Long, onBack: () -> Unit) {
    val viewModel: WorkoutDetailViewModel = appViewModel(key = "workout-detail-$workoutId") {
        WorkoutDetailViewModel(it.repository, it.settings, workoutId)
    }
    val workout by viewModel.workout.collectAsStateWithLifecycle()
    val unit by viewModel.weightUnit.collectAsStateWithLifecycle()
    var showDelete by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(workout?.workout?.name ?: "Workout") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
                },
                actions = {
                    IconButton(onClick = { showDelete = true }) { Icon(Icons.Default.Delete, contentDescription = "Delete workout") }
                },
            )
        },
    ) { padding ->
        val current = workout
        if (current == null) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            val allSets = current.exercises.flatMap { it.sets }
            val duration = current.workout.endedAt?.let { it - current.workout.startedAt } ?: 0L
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item(key = "when") {
                    Text(
                        "${formatDay(current.workout.startedAt)} at ${formatTime(current.workout.startedAt)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                item(key = "stats") {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatTile(label = "Duration", value = formatDurationShort(duration), modifier = Modifier.weight(1f))
                        StatTile(label = "Sets", value = allSets.completedCount().toString(), modifier = Modifier.weight(1f))
                        StatTile(label = "Volume", value = "${formatWeight(allSets.totalVolume(), unit)} ${unit.label}", modifier = Modifier.weight(1f))
                    }
                }
                if (current.workout.notes.isNotBlank()) {
                    item(key = "notes") {
                        Text(current.workout.notes, style = MaterialTheme.typography.bodyMedium)
                    }
                }
                current.orderedExercises.forEach { block ->
                    item(key = "ex-${block.workoutExercise.id}") {
                        Column {
                            Spacer(Modifier.height(4.dp))
                            Text(block.exercise.name, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.height(4.dp))
                            block.orderedSets.forEach { set ->
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                                    Text(
                                        set.setNumber.toString(),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.width(28.dp),
                                    )
                                    Text(
                                        if (block.exercise.isDistance) {
                                            "${formatKm(set.distanceKm)} km in ${formatClock(set.durationSeconds ?: 0)}"
                                        } else {
                                            "${formatWeight(set.weightKg, unit)} ${unit.label} × ${set.reps ?: "-"}"
                                        },
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.weight(1f),
                                    )
                                    set.estimatedOneRepMax()?.let { e1rm ->
                                        Text(
                                            "e1RM ${formatWeight(e1rm, unit)}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDelete) {
        ConfirmDialog(
            title = "Delete workout?",
            text = "This removes the workout and its sets from your history.",
            confirmLabel = "Delete",
            destructive = true,
            onConfirm = {
                showDelete = false
                viewModel.delete(onBack)
            },
            onDismiss = { showDelete = false },
        )
    }
}
