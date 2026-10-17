package com.vianet.workoutlog.ui.start

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vianet.workoutlog.data.db.TemplateWithExercises
import com.vianet.workoutlog.data.db.Workout
import com.vianet.workoutlog.domain.formatDurationMillis
import com.vianet.workoutlog.ui.appViewModel
import com.vianet.workoutlog.ui.common.ConfirmDialog
import com.vianet.workoutlog.ui.common.EmptyState
import kotlinx.coroutines.delay

@Composable
fun StartScreen(
    bottomBar: @Composable () -> Unit,
    onOpenWorkout: (Long) -> Unit,
    onEditTemplate: (Long?) -> Unit,
    onOpenSettings: () -> Unit,
) {
    val viewModel: StartViewModel = appViewModel { StartViewModel(it.repository, it.settings) }
    val templates by viewModel.templates.collectAsStateWithLifecycle()
    val activeWorkout by viewModel.activeWorkout.collectAsStateWithLifecycle()

    var templateToDelete by remember { mutableStateOf<TemplateWithExercises?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Start workout") },
                actions = {
                    IconButton(onClick = onOpenSettings) { Icon(Icons.Default.Settings, contentDescription = "Settings") }
                },
            )
        },
        bottomBar = bottomBar,
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            activeWorkout?.let { workout ->
                item(key = "active") {
                    ActiveWorkoutCard(workout = workout, onResume = { onOpenWorkout(workout.id) })
                }
            }
            if (activeWorkout == null) {
                item(key = "quick") {
                    Button(
                        onClick = { viewModel.startWorkout(null, onOpenWorkout) },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Start an empty workout") }
                }
            }
            item(key = "templates-header") {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text("Templates", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    IconButton(onClick = { onEditTemplate(null) }) { Icon(Icons.Default.Add, contentDescription = "New template") }
                }
            }
            if (templates.isEmpty()) {
                item(key = "empty") {
                    EmptyState(title = "No templates yet", body = "Tap + to build a workout you can start with one tap.")
                }
            }
            items(templates, key = { it.template.id }) { template ->
                TemplateCard(
                    template = template,
                    enabled = activeWorkout == null,
                    onStart = { viewModel.startWorkout(template.template.id, onOpenWorkout) },
                    onEdit = { onEditTemplate(template.template.id) },
                    onDelete = { templateToDelete = template },
                )
            }
            item(key = "spacer") { Spacer(Modifier.height(24.dp)) }
        }
    }

    templateToDelete?.let { template ->
        ConfirmDialog(
            title = "Delete template?",
            text = "\"${template.template.name}\" will be removed. Past workouts are kept.",
            confirmLabel = "Delete",
            destructive = true,
            onConfirm = {
                viewModel.deleteTemplate(template.template.id)
                templateToDelete = null
            },
            onDismiss = { templateToDelete = null },
        )
    }
}

@Composable
private fun ActiveWorkoutCard(workout: Workout, onResume: () -> Unit) {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(workout.id) {
        while (true) {
            now = System.currentTimeMillis()
            delay(1000)
        }
    }
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Workout in progress", style = MaterialTheme.typography.labelMedium)
                Text(workout.name, style = MaterialTheme.typography.titleMedium)
                Text(formatDurationMillis(now - workout.startedAt), style = MaterialTheme.typography.bodyMedium)
            }
            Button(onClick = onResume) { Text("Resume") }
        }
    }
}

@Composable
private fun TemplateCard(
    template: TemplateWithExercises,
    enabled: Boolean,
    onStart: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    val exercises = template.orderedExercises
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(start = 16.dp, end = 4.dp, top = 8.dp, bottom = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(template.template.name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                Box {
                    IconButton(onClick = { menuOpen = true }) { Icon(Icons.Default.MoreVert, contentDescription = "More") }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(text = { Text("Edit") }, onClick = { menuOpen = false; onEdit() })
                        DropdownMenuItem(text = { Text("Delete") }, onClick = { menuOpen = false; onDelete() })
                    }
                }
            }
            Text(
                text = exercises.joinToString(", ") { "${it.templateExercise.targetSets} × ${it.exercise.name}" }
                    .ifEmpty { "No exercises yet" },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(end = 12.dp),
            )
            Spacer(Modifier.height(10.dp))
            OutlinedButton(onClick = onStart, enabled = enabled, modifier = Modifier.padding(end = 12.dp)) {
                Text(if (enabled) "Start" else "Finish current workout first")
            }
        }
    }
}
