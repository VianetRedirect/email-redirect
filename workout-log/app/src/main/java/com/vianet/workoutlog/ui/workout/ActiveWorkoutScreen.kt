package com.vianet.workoutlog.ui.workout

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vianet.workoutlog.data.db.SetEntry
import com.vianet.workoutlog.domain.Milestone
import com.vianet.workoutlog.domain.WeightUnit
import com.vianet.workoutlog.domain.formatClock
import com.vianet.workoutlog.domain.formatDurationMillis
import com.vianet.workoutlog.domain.formatKm
import com.vianet.workoutlog.domain.formatSecondsAsMinutes
import com.vianet.workoutlog.domain.formatWeight
import com.vianet.workoutlog.domain.isPersonalRecord
import com.vianet.workoutlog.domain.parseKm
import com.vianet.workoutlog.domain.parseMinutesToSeconds
import com.vianet.workoutlog.domain.parseWeight
import com.vianet.workoutlog.share.ShareApp
import com.vianet.workoutlog.ui.appViewModel
import com.vianet.workoutlog.ui.common.CelebrationDialog
import com.vianet.workoutlog.ui.common.CompactNumberField
import com.vianet.workoutlog.ui.common.ConfirmDialog
import com.vianet.workoutlog.ui.common.EmptyState
import com.vianet.workoutlog.ui.common.ExercisePickerDialog
import com.vianet.workoutlog.ui.common.RestPickerDialog
import com.vianet.workoutlog.ui.common.RestTimerBar
import com.vianet.workoutlog.ui.common.restLabel
import kotlinx.coroutines.delay

@Composable
fun ActiveWorkoutScreen(
    workoutId: Long,
    onBack: () -> Unit,
    onWorkoutClosed: () -> Unit,
) {
    val appContext = LocalContext.current.applicationContext
    val viewModel: ActiveWorkoutViewModel = appViewModel(key = "workout-$workoutId") {
        ActiveWorkoutViewModel(it.repository, it.settings, appContext, workoutId)
    }
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val unit by viewModel.weightUnit.collectAsStateWithLifecycle()
    val restTimer by viewModel.restTimer.collectAsStateWithLifecycle()
    val exercises by viewModel.exercises.collectAsStateWithLifecycle()

    var showPicker by remember { mutableStateOf(false) }
    var showFinish by remember { mutableStateOf(false) }
    var showCancel by remember { mutableStateOf(false) }
    var showRename by remember { mutableStateOf(false) }
    var restEditFor by remember { mutableStateOf<ExerciseBlockUi?>(null) }
    var removeBlock by remember { mutableStateOf<ExerciseBlockUi?>(null) }
    var celebrate by remember { mutableStateOf<List<Milestone>>(emptyList()) }

    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(workoutId) {
        while (true) {
            now = System.currentTimeMillis()
            delay(1000)
        }
    }

    val current = ui
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(current?.workout?.name ?: "Workout", style = MaterialTheme.typography.titleMedium)
                        current?.let {
                            Text(
                                formatDurationMillis(now - it.workout.startedAt),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
                },
                actions = {
                    TextButton(onClick = { showRename = true }) { Text("Rename") }
                    Button(onClick = { showFinish = true }, modifier = Modifier.padding(end = 8.dp)) { Text("Finish") }
                },
            )
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            restTimer?.let { state ->
                RestTimerBar(state = state, onAdjust = viewModel::adjustRest, onSkip = viewModel::skipRest)
            }
            if (current == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 32.dp),
                ) {
                    if (current.blocks.isEmpty()) {
                        item(key = "empty") {
                            EmptyState(title = "No exercises yet", body = "Add an exercise to start logging sets.")
                        }
                    }
                    current.blocks.forEach { block ->
                        item(key = "header-${block.block.workoutExercise.id}") {
                            ExerciseHeader(
                                block = block,
                                onEditRest = { restEditFor = block },
                                onRemove = { removeBlock = block },
                                onStartRest = { viewModel.startRest(block) },
                            )
                        }
                        item(key = "columns-${block.block.workoutExercise.id}") {
                            SetColumnHeader(unit = unit, distance = block.exercise.isDistance)
                        }
                        items(block.sets, key = { "set-${it.id}" }) { set ->
                            if (block.exercise.isDistance) {
                                DistanceSetRow(
                                    set = set,
                                    block = block,
                                    onValuesChange = { km, secs -> viewModel.updateDistanceValues(set, km, secs) },
                                    onToggle = { viewModel.toggleCompleted(set, block) },
                                    onDelete = { viewModel.deleteSet(set) },
                                )
                            } else {
                                SetRow(
                                    set = set,
                                    block = block,
                                    unit = unit,
                                    onValuesChange = { w, r -> viewModel.updateSetValues(set, w, r) },
                                    onToggle = { viewModel.toggleCompleted(set, block) },
                                    onDelete = { viewModel.deleteSet(set) },
                                )
                            }
                        }
                        item(key = "add-set-${block.block.workoutExercise.id}") {
                            TextButton(
                                onClick = { viewModel.addSet(block) },
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null)
                                Spacer(Modifier.width(4.dp))
                                Text("Add set")
                            }
                            Spacer(Modifier.height(8.dp))
                        }
                    }
                    item(key = "add-exercise") {
                        OutlinedButton(
                            onClick = { showPicker = true },
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Add exercise")
                        }
                    }
                    item(key = "cancel") {
                        TextButton(
                            onClick = { showCancel = true },
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        ) { Text("Cancel workout") }
                    }
                }
            }
        }
    }

    if (showPicker) {
        ExercisePickerDialog(
            exercises = exercises,
            onPick = { viewModel.addExercise(it); showPicker = false },
            onDismiss = { showPicker = false },
            onCreate = { name, bodyPart, category, trackingType ->
                viewModel.createAndAddExercise(name, bodyPart, category, trackingType)
                showPicker = false
            },
        )
    }

    restEditFor?.let { block ->
        RestPickerDialog(
            current = block.restSeconds,
            onPick = { seconds -> viewModel.setRest(block, seconds); restEditFor = null },
            onDismiss = { restEditFor = null },
        )
    }

    removeBlock?.let { block ->
        ConfirmDialog(
            title = "Remove ${block.exercise.name}?",
            text = "Its sets from this workout will be removed.",
            confirmLabel = "Remove",
            destructive = true,
            onConfirm = { viewModel.removeExercise(block); removeBlock = null },
            onDismiss = { removeBlock = null },
        )
    }

    if (showFinish) {
        val completed = current?.completedSets ?: 0
        ConfirmDialog(
            title = "Finish workout?",
            text = if (completed == 0) {
                "No sets were ticked, so this workout will be discarded."
            } else {
                "$completed completed ${if (completed == 1) "set" else "sets"} will be saved. Unticked sets are dropped."
            },
            confirmLabel = if (completed == 0) "Discard" else "Finish",
            destructive = completed == 0,
            onConfirm = {
                showFinish = false
                viewModel.finish { result ->
                    if (result.newMilestones.isEmpty()) onWorkoutClosed() else celebrate = result.newMilestones
                }
            },
            onDismiss = { showFinish = false },
        )
    }

    if (showCancel) {
        ConfirmDialog(
            title = "Cancel workout?",
            text = "Everything logged in this workout will be deleted.",
            confirmLabel = "Delete workout",
            destructive = true,
            onConfirm = {
                showCancel = false
                viewModel.cancel { onWorkoutClosed() }
            },
            onDismiss = { showCancel = false },
        )
    }

    if (celebrate.isNotEmpty()) {
        val context = LocalContext.current
        CelebrationDialog(
            milestones = celebrate,
            onShare = { m ->
                ShareApp.shareText(context, "${m.emoji} Milestone unlocked in Workout Log: ${m.title}. ${m.detail}")
            },
            onDismiss = {
                celebrate = emptyList()
                onWorkoutClosed()
            },
        )
    }

    if (showRename && current != null) {
        var name by remember { mutableStateOf(current.workout.name) }
        AlertDialog(
            onDismissRequest = { showRename = false },
            title = { Text("Workout name") },
            text = { OutlinedTextField(value = name, onValueChange = { name = it }, singleLine = true) },
            confirmButton = { Button(onClick = { viewModel.rename(name); showRename = false }) { Text("Save") } },
            dismissButton = { TextButton(onClick = { showRename = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun ExerciseHeader(
    block: ExerciseBlockUi,
    onEditRest: () -> Unit,
    onRemove: () -> Unit,
    onStartRest: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp, top = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            block.exercise.name,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f),
        )
        AssistChip(
            onClick = onEditRest,
            label = { Text(restLabel(block.restSeconds)) },
            leadingIcon = { Icon(Icons.Default.Timer, contentDescription = "Rest time", modifier = Modifier.size(16.dp)) },
        )
        Box {
            IconButton(onClick = { menuOpen = true }) { Icon(Icons.Default.MoreVert, contentDescription = "More") }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                DropdownMenuItem(text = { Text("Start rest timer") }, onClick = { menuOpen = false; onStartRest() })
                DropdownMenuItem(text = { Text("Remove exercise") }, onClick = { menuOpen = false; onRemove() })
            }
        }
    }
}

@Composable
private fun SetColumnHeader(unit: WeightUnit, distance: Boolean) {
    val style = MaterialTheme.typography.labelMedium
    val color = MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("Set", style = style, color = color, modifier = Modifier.width(36.dp), textAlign = TextAlign.Center)
        Text("Previous", style = style, color = color, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
        Text(if (distance) "km" else unit.label, style = style, color = color, modifier = Modifier.width(72.dp), textAlign = TextAlign.Center)
        Spacer(Modifier.width(8.dp))
        Text(if (distance) "min" else "Reps", style = style, color = color, modifier = Modifier.width(64.dp), textAlign = TextAlign.Center)
        Spacer(Modifier.width(8.dp))
        Spacer(Modifier.width(40.dp))
    }
}

@Composable
private fun SetRow(
    set: SetEntry,
    block: ExerciseBlockUi,
    unit: WeightUnit,
    onValuesChange: (Double?, Int?) -> Unit,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
) {
    val previous = block.previousFor(set.setNumber)
    // Local text so typing never fights with the database round trip.
    var weightText by remember(set.id, set.completed, unit) { mutableStateOf(formatWeight(set.weightKg, unit)) }
    var repsText by remember(set.id, set.completed) { mutableStateOf(set.reps?.toString() ?: "") }

    val isRecord = set.completed && isPersonalRecord(set, block.context.previousBestOneRepMax)

    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                onDelete()
                true
            } else {
                false
            }
        },
    )

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            Box(
                modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.error).padding(end = 24.dp),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Icon(Icons.Default.Delete, contentDescription = "Delete set", tint = MaterialTheme.colorScheme.onError)
            }
        },
    ) {
        val rowBackground = if (set.completed) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
        } else {
            MaterialTheme.colorScheme.background
        }
        Row(
            modifier = Modifier.fillMaxWidth().background(rowBackground).padding(horizontal = 16.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.size(36.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(set.setNumber.toString(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = previous?.let { "${formatWeight(it.weightKg, unit)} × ${it.reps ?: "-"}" } ?: "—",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                if (isRecord) {
                    Text(
                        "PR",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
            CompactNumberField(
                value = weightText,
                onValueChange = { text ->
                    weightText = text
                    onValuesChange(parseWeight(text, unit), repsText.toIntOrNull())
                },
                placeholder = previous?.let { formatWeight(it.weightKg, unit) } ?: "",
                decimal = true,
                highlighted = set.completed,
                modifier = Modifier.width(72.dp),
            )
            Spacer(Modifier.width(8.dp))
            CompactNumberField(
                value = repsText,
                onValueChange = { text ->
                    repsText = text
                    onValuesChange(parseWeight(weightText, unit), text.toIntOrNull())
                },
                placeholder = previous?.reps?.toString() ?: "",
                highlighted = set.completed,
                modifier = Modifier.width(64.dp),
            )
            Spacer(Modifier.width(8.dp))
            FilledIconButton(
                onClick = onToggle,
                modifier = Modifier.size(40.dp),
                shape = RoundedCornerShape(10.dp),
                colors = if (set.completed) {
                    IconButtonDefaults.filledIconButtonColors()
                } else {
                    IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
            ) {
                Icon(Icons.Default.Check, contentDescription = if (set.completed) "Completed" else "Mark complete")
            }
        }
    }
}

@Composable
private fun DistanceSetRow(
    set: SetEntry,
    block: ExerciseBlockUi,
    onValuesChange: (Double?, Int?) -> Unit,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
) {
    val previous = block.previousFor(set.setNumber)
    var kmText by remember(set.id, set.completed) { mutableStateOf(formatKm(set.distanceKm)) }
    var minText by remember(set.id, set.completed) { mutableStateOf(formatSecondsAsMinutes(set.durationSeconds)) }

    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                onDelete()
                true
            } else {
                false
            }
        },
    )

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            Box(
                modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.error).padding(end = 24.dp),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Icon(Icons.Default.Delete, contentDescription = "Delete set", tint = MaterialTheme.colorScheme.onError)
            }
        },
    ) {
        val rowBackground = if (set.completed) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
        } else {
            MaterialTheme.colorScheme.background
        }
        Row(
            modifier = Modifier.fillMaxWidth().background(rowBackground).padding(horizontal = 16.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(modifier = Modifier.size(36.dp), contentAlignment = Alignment.Center) {
                Text(set.setNumber.toString(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            Text(
                text = previous?.let { "${formatKm(it.distanceKm)} km · ${formatClock(it.durationSeconds ?: 0)}" } ?: "—",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
            CompactNumberField(
                value = kmText,
                onValueChange = { text ->
                    kmText = text
                    onValuesChange(parseKm(text), parseMinutesToSeconds(minText))
                },
                placeholder = previous?.let { formatKm(it.distanceKm) } ?: "",
                decimal = true,
                highlighted = set.completed,
                modifier = Modifier.width(72.dp),
            )
            Spacer(Modifier.width(8.dp))
            CompactNumberField(
                value = minText,
                onValueChange = { text ->
                    minText = text
                    onValuesChange(parseKm(kmText), parseMinutesToSeconds(text))
                },
                placeholder = previous?.let { formatSecondsAsMinutes(it.durationSeconds) } ?: "",
                decimal = true,
                highlighted = set.completed,
                modifier = Modifier.width(64.dp),
            )
            Spacer(Modifier.width(8.dp))
            FilledIconButton(
                onClick = onToggle,
                modifier = Modifier.size(40.dp),
                shape = RoundedCornerShape(10.dp),
                colors = if (set.completed) {
                    IconButtonDefaults.filledIconButtonColors()
                } else {
                    IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
            ) {
                Icon(Icons.Default.Check, contentDescription = if (set.completed) "Completed" else "Mark complete")
            }
        }
    }
}
