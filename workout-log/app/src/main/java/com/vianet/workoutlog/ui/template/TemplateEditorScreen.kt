package com.vianet.workoutlog.ui.template

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vianet.workoutlog.data.TemplateRow
import com.vianet.workoutlog.ui.appViewModel
import com.vianet.workoutlog.ui.common.CompactNumberField
import com.vianet.workoutlog.ui.common.ExercisePickerDialog
import com.vianet.workoutlog.ui.common.RestPickerDialog
import com.vianet.workoutlog.ui.common.restLabel

@Composable
fun TemplateEditorScreen(templateId: Long?, onDone: () -> Unit) {
    val viewModel: TemplateEditorViewModel = appViewModel(key = "template-${templateId ?: "new"}") {
        TemplateEditorViewModel(it.repository, templateId)
    }
    val exercises by viewModel.exercises.collectAsStateWithLifecycle()
    var showPicker by remember { mutableStateOf(false) }
    var restEditIndex by remember { mutableStateOf<Int?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (templateId == null) "New template" else "Edit template") },
                navigationIcon = {
                    IconButton(onClick = onDone) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
                },
                actions = {
                    TextButton(onClick = { viewModel.save(onDone) }, enabled = viewModel.canSave) { Text("Save") }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(key = "name") {
                OutlinedTextField(
                    value = viewModel.name,
                    onValueChange = { viewModel.name = it },
                    label = { Text("Template name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            item(key = "notes") {
                OutlinedTextField(
                    value = viewModel.notes,
                    onValueChange = { viewModel.notes = it },
                    label = { Text("Notes (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                )
            }
            itemsIndexed(viewModel.rows, key = { index, row -> "${row.exerciseId}-$index" }) { index, row ->
                TemplateRowCard(
                    row = row,
                    isFirst = index == 0,
                    isLast = index == viewModel.rows.lastIndex,
                    onChange = { viewModel.updateRow(index, it) },
                    onRemove = { viewModel.removeRow(index) },
                    onMoveUp = { viewModel.move(index, -1) },
                    onMoveDown = { viewModel.move(index, 1) },
                    onEditRest = { restEditIndex = index },
                )
            }
            item(key = "add") {
                OutlinedButton(onClick = { showPicker = true }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Add exercise")
                }
            }
            item(key = "save") {
                Button(onClick = { viewModel.save(onDone) }, enabled = viewModel.canSave, modifier = Modifier.fillMaxWidth()) {
                    Text("Save template")
                }
            }
            item(key = "spacer") { Spacer(Modifier.height(32.dp)) }
        }
    }

    if (showPicker) {
        ExercisePickerDialog(
            exercises = exercises,
            onPick = { viewModel.addExercise(it); showPicker = false },
            onDismiss = { showPicker = false },
            onCreate = { name, bodyPart, category, trackingType ->
                viewModel.createExercise(name, bodyPart, category, trackingType)
                showPicker = false
            },
        )
    }

    restEditIndex?.let { index ->
        val row = viewModel.rows.getOrNull(index)
        if (row == null) {
            restEditIndex = null
        } else {
            RestPickerDialog(
                current = row.restSeconds ?: 90,
                onPick = { seconds ->
                    viewModel.updateRow(index, row.copy(restSeconds = seconds))
                    restEditIndex = null
                },
                onDismiss = { restEditIndex = null },
            )
        }
    }
}

@Composable
private fun TemplateRowCard(
    row: TemplateRow,
    isFirst: Boolean,
    isLast: Boolean,
    onChange: (TemplateRow) -> Unit,
    onRemove: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onEditRest: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(start = 16.dp, end = 4.dp, top = 8.dp, bottom = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(row.exerciseName, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                IconButton(onClick = onMoveUp, enabled = !isFirst) { Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Move up") }
                IconButton(onClick = onMoveDown, enabled = !isLast) { Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Move down") }
                IconButton(onClick = onRemove) { Icon(Icons.Default.Delete, contentDescription = "Remove") }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                LabelledField(label = "Sets") {
                    CompactNumberField(
                        value = row.targetSets.toString(),
                        onValueChange = { text -> onChange(row.copy(targetSets = text.toIntOrNull() ?: 0)) },
                        placeholder = "3",
                        modifier = Modifier.width(64.dp),
                    )
                }
                LabelledField(label = "Reps") {
                    CompactNumberField(
                        value = row.targetReps?.toString() ?: "",
                        onValueChange = { text -> onChange(row.copy(targetReps = text.toIntOrNull())) },
                        placeholder = "-",
                        modifier = Modifier.width(64.dp),
                    )
                }
                LabelledField(label = "Rest") {
                    OutlinedButton(onClick = onEditRest) {
                        Text(row.restSeconds?.let { restLabel(it) } ?: "Default")
                    }
                }
            }
        }
    }
}

@Composable
private fun LabelledField(label: String, content: @Composable () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(4.dp))
        content()
    }
}
