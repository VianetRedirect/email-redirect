package com.vianet.workoutlog.ui.exercises

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.vianet.workoutlog.data.WorkoutRepository
import com.vianet.workoutlog.data.db.Exercise
import com.vianet.workoutlog.ui.appViewModel
import com.vianet.workoutlog.ui.common.CreateExerciseDialog
import com.vianet.workoutlog.ui.common.EmptyState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ExercisesViewModel(private val repository: WorkoutRepository) : ViewModel() {
    val exercises: StateFlow<List<Exercise>> = repository.observeExercises()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun create(name: String, bodyPart: String, category: String, trackingType: String) {
        viewModelScope.launch { repository.createExercise(name, bodyPart, category, trackingType) }
    }
}

@Composable
fun ExercisesScreen(
    bottomBar: @Composable () -> Unit,
    onOpenExercise: (Long) -> Unit,
) {
    val viewModel: ExercisesViewModel = appViewModel { ExercisesViewModel(it.repository) }
    val exercises by viewModel.exercises.collectAsStateWithLifecycle()
    var query by rememberSaveable { mutableStateOf("") }
    var showCreate by remember { mutableStateOf(false) }

    val filtered = remember(exercises, query) {
        val q = query.trim()
        if (q.isEmpty()) exercises else exercises.filter { it.name.contains(q, ignoreCase = true) || it.bodyPart.contains(q, ignoreCase = true) }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Exercises") }) },
        bottomBar = bottomBar,
        floatingActionButton = {
            FloatingActionButton(onClick = { showCreate = true }) { Icon(Icons.Default.Add, contentDescription = "New exercise") }
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Search") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
            )
            if (filtered.isEmpty()) {
                EmptyState(title = "Nothing found", body = "Tap + to add a new exercise.")
            }
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(filtered, key = { it.id }) { exercise ->
                    ListItem(
                        headlineContent = { Text(exercise.name) },
                        supportingContent = {
                            Text("${exercise.bodyPart} · ${exercise.category}${if (exercise.isDistance) " · distance" else ""}")
                        },
                        modifier = Modifier.clickable { onOpenExercise(exercise.id) },
                    )
                    HorizontalDivider()
                }
            }
        }
    }

    if (showCreate) {
        CreateExerciseDialog(
            initialName = query.trim(),
            onDismiss = { showCreate = false },
            onCreate = { name, bodyPart, category, trackingType ->
                viewModel.create(name, bodyPart, category, trackingType)
                showCreate = false
            },
        )
    }
}
