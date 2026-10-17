package com.vianet.workoutlog.ui.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.vianet.workoutlog.data.db.Exercise
import com.vianet.workoutlog.data.db.TrackingType

val bodyParts = listOf("Chest", "Back", "Legs", "Shoulders", "Arms", "Core", "Full body", "Cardio")
val categories = listOf("Barbell", "Dumbbell", "Machine", "Cable", "Bodyweight", "Other")

/** Full-screen searchable list used wherever an exercise is chosen. */
@Composable
fun ExercisePickerDialog(
    exercises: List<Exercise>,
    onPick: (Exercise) -> Unit,
    onDismiss: () -> Unit,
    onCreate: (name: String, bodyPart: String, category: String, trackingType: String) -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var showCreate by remember { mutableStateOf(false) }

    val filtered = remember(exercises, query) {
        val q = query.trim()
        if (q.isEmpty()) exercises else exercises.filter { it.name.contains(q, ignoreCase = true) || it.bodyPart.contains(q, ignoreCase = true) }
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Choose exercise") },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, contentDescription = "Close") }
                    },
                    actions = {
                        IconButton(onClick = { showCreate = true }) { Icon(Icons.Default.Add, contentDescription = "New exercise") }
                    },
                )
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
                    EmptyState(
                        title = "No match",
                        body = "Tap + to add \"${query.trim()}\" as a new exercise.",
                    )
                }
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(filtered, key = { it.id }) { exercise ->
                        ListItem(
                            headlineContent = { Text(exercise.name) },
                            supportingContent = {
                                Text("${exercise.bodyPart} · ${exercise.category}${if (exercise.isDistance) " · distance" else ""}")
                            },
                            modifier = Modifier.clickable { onPick(exercise) },
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }

    if (showCreate) {
        CreateExerciseDialog(
            initialName = query.trim(),
            onDismiss = { showCreate = false },
            onCreate = { name, bodyPart, category, trackingType ->
                showCreate = false
                onCreate(name, bodyPart, category, trackingType)
            },
        )
    }
}

@Composable
fun CreateExerciseDialog(
    initialName: String = "",
    onDismiss: () -> Unit,
    onCreate: (name: String, bodyPart: String, category: String, trackingType: String) -> Unit,
) {
    var name by rememberSaveable { mutableStateOf(initialName) }
    var bodyPart by rememberSaveable { mutableStateOf(bodyParts.first()) }
    var category by rememberSaveable { mutableStateOf(categories.first()) }
    var distance by rememberSaveable { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New exercise") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text("Body part", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    bodyParts.forEach { part ->
                        FilterChip(selected = bodyPart == part, onClick = { bodyPart = part }, label = { Text(part) })
                    }
                }
                Text("Equipment", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    categories.forEach { c ->
                        FilterChip(selected = category == c, onClick = { category = c }, label = { Text(c) })
                    }
                }
                Text("Log as", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(selected = !distance, onClick = { distance = false }, label = { Text("Weight and reps") })
                    FilterChip(selected = distance, onClick = { distance = true }, label = { Text("Distance and time") })
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onCreate(name.trim(), bodyPart, category, if (distance) TrackingType.DISTANCE_TIME else TrackingType.WEIGHT_REPS)
                },
                enabled = name.isNotBlank(),
            ) { Text("Add") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

/** Dialog for picking a rest length in seconds. */
@Composable
fun RestPickerDialog(current: Int, onPick: (Int) -> Unit, onDismiss: () -> Unit) {
    val presets = listOf(30, 45, 60, 90, 120, 150, 180, 240, 300)
    var custom by rememberSaveable { mutableStateOf(current.toString()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rest time") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    presets.forEach { s ->
                        FilterChip(selected = current == s, onClick = { onPick(s) }, label = { Text(restLabel(s)) })
                    }
                }
                Spacer(Modifier.height(4.dp))
                OutlinedTextField(
                    value = custom,
                    onValueChange = { v -> if (v.length <= 4 && v.all { it.isDigit() }) custom = v },
                    label = { Text("Custom (seconds)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            Button(onClick = { custom.toIntOrNull()?.let { if (it in 5..3600) onPick(it) } }) { Text("Set") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

fun restLabel(seconds: Int): String {
    val m = seconds / 60
    val s = seconds % 60
    return when {
        m == 0 -> "${s}s"
        s == 0 -> "${m}m"
        else -> "${m}m ${s}s"
    }
}
