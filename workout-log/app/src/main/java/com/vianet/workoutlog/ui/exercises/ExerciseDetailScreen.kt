package com.vianet.workoutlog.ui.exercises

import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.vianet.workoutlog.data.Settings
import com.vianet.workoutlog.data.WorkoutRepository
import com.vianet.workoutlog.data.db.Exercise
import com.vianet.workoutlog.data.db.ExerciseSetRecord
import com.vianet.workoutlog.domain.WeightUnit
import com.vianet.workoutlog.domain.estimatedOneRepMax
import com.vianet.workoutlog.domain.formatClock
import com.vianet.workoutlog.domain.formatDay
import com.vianet.workoutlog.domain.formatKm
import com.vianet.workoutlog.domain.formatShortDay
import com.vianet.workoutlog.domain.formatWeight
import com.vianet.workoutlog.ui.appViewModel
import com.vianet.workoutlog.ui.common.EmptyState
import com.vianet.workoutlog.ui.common.StatTile
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class ExerciseDetailViewModel(repository: WorkoutRepository, settings: Settings, exerciseId: Long) : ViewModel() {
    val exercise: StateFlow<Exercise?> = repository.observeExercise(exerciseId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    val history: StateFlow<List<ExerciseSetRecord>> = repository.observeExerciseHistory(exerciseId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val weightUnit: StateFlow<WeightUnit> = settings.weightUnit
}

private data class WorkoutGroup(val workoutId: Long, val startedAt: Long, val name: String, val sets: List<ExerciseSetRecord>)

private fun ExerciseSetRecord.oneRepMax(): Double? {
    val w = weightKg ?: return null
    val r = reps ?: return null
    if (r <= 0) return null
    return estimatedOneRepMax(w, r)
}

@Composable
fun ExerciseDetailScreen(exerciseId: Long, onBack: () -> Unit) {
    val viewModel: ExerciseDetailViewModel = appViewModel(key = "exercise-$exerciseId") {
        ExerciseDetailViewModel(it.repository, it.settings, exerciseId)
    }
    val exercise by viewModel.exercise.collectAsStateWithLifecycle()
    val history by viewModel.history.collectAsStateWithLifecycle()
    val unit by viewModel.weightUnit.collectAsStateWithLifecycle()

    val groups = remember(history) {
        history.groupBy { it.workoutId }.map { (id, sets) ->
            WorkoutGroup(workoutId = id, startedAt = sets.first().startedAt, name = sets.first().workoutName, sets = sets)
        }.sortedByDescending { it.startedAt }
    }
    val heaviest = remember(history) {
        history.filter { it.weightKg != null && (it.reps ?: 0) > 0 }
            .maxWithOrNull(compareBy<ExerciseSetRecord> { it.weightKg ?: 0.0 }.thenBy { it.reps ?: 0 })
    }
    val bestOneRepMax = remember(history) { history.mapNotNull { it.oneRepMax() }.maxOrNull() }
    val isDistance = exercise?.isDistance == true
    val totalKm = remember(history) { history.sumOf { it.distanceKm ?: 0.0 } }
    val longestKm = remember(history) { history.maxOfOrNull { it.distanceKm ?: 0.0 } ?: 0.0 }
    val chartPoints = remember(groups) {
        groups.sortedBy { it.startedAt }.mapNotNull { g ->
            g.sets.mapNotNull { it.oneRepMax() }.maxOrNull()?.let { g.startedAt to it }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(exercise?.name ?: "Exercise") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            exercise?.let { ex ->
                item(key = "meta") {
                    Text("${ex.bodyPart} · ${ex.category}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            item(key = "stats") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (isDistance) {
                        StatTile(label = "Total", value = "${formatKm(totalKm)} km", modifier = Modifier.weight(1f))
                        StatTile(label = "Longest", value = "${formatKm(longestKm)} km", modifier = Modifier.weight(1f))
                    } else {
                        StatTile(
                            label = "Heaviest set",
                            value = heaviest?.let { "${formatWeight(it.weightKg, unit)} × ${it.reps}" } ?: "—",
                            modifier = Modifier.weight(1f),
                        )
                        StatTile(
                            label = "Best e1RM",
                            value = bestOneRepMax?.let { "${formatWeight(it, unit)} ${unit.label}" } ?: "—",
                            modifier = Modifier.weight(1f),
                        )
                    }
                    StatTile(label = "Sets", value = history.size.toString(), modifier = Modifier.weight(1f))
                }
            }
            if (!isDistance && chartPoints.size >= 2) {
                item(key = "chart") {
                    Column {
                        Text("Estimated 1RM over time", style = MaterialTheme.typography.titleSmall)
                        Spacer(Modifier.height(8.dp))
                        OneRepMaxChart(points = chartPoints, unit = unit)
                    }
                }
            }
            if (groups.isEmpty()) {
                item(key = "empty") {
                    EmptyState(title = "No history yet", body = "Sets you complete in a workout will appear here.")
                }
            }
            groups.forEach { group ->
                item(key = "group-${group.workoutId}") {
                    Column {
                        Spacer(Modifier.height(4.dp))
                        Text(group.name, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                        Text(formatDay(group.startedAt), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(4.dp))
                        group.sets.forEachIndexed { index, set ->
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                                Text(
                                    (index + 1).toString(),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.width(28.dp),
                                )
                                Text(
                                    if (isDistance) {
                                        "${formatKm(set.distanceKm)} km in ${formatClock(set.durationSeconds ?: 0)}"
                                    } else {
                                        "${formatWeight(set.weightKg, unit)} ${unit.label} × ${set.reps ?: "-"}"
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.weight(1f),
                                )
                                if (!isDistance) set.oneRepMax()?.let { e1rm ->
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

/** Simple line chart of best estimated one rep max per workout. */
@Composable
private fun OneRepMaxChart(points: List<Pair<Long, Double>>, unit: WeightUnit) {
    val lineColor = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
    val minY = points.minOf { it.second }
    val maxY = points.maxOf { it.second }
    val first = points.first()
    val last = points.last()

    Column {
        Row(modifier = Modifier.fillMaxWidth()) {
            Text("High ${formatWeight(maxY, unit)} ${unit.label}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.weight(1f))
            Text("Low ${formatWeight(minY, unit)} ${unit.label}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Canvas(modifier = Modifier.fillMaxWidth().height(160.dp).padding(vertical = 8.dp)) {
            val w = size.width
            val h = size.height
            val span = (maxY - minY).takeIf { it > 0.0 } ?: 1.0
            val minX = first.first
            val xSpan = (last.first - minX).takeIf { it > 0L } ?: 1L

            fun x(t: Long): Float = ((t - minX).toFloat() / xSpan.toFloat()) * w
            fun y(v: Double): Float = (h - ((v - minY) / span * h).toFloat()).coerceIn(0f, h)

            listOf(0f, 0.5f, 1f).forEach { f ->
                drawLine(gridColor, Offset(0f, h * f), Offset(w, h * f), strokeWidth = 1f)
            }
            val path = Path()
            points.forEachIndexed { index, (t, v) ->
                val px = x(t)
                val py = y(v)
                if (index == 0) path.moveTo(px, py) else path.lineTo(px, py)
            }
            drawPath(path, color = lineColor, style = Stroke(width = 4f, cap = StrokeCap.Round))
            points.forEach { (t, v) ->
                drawCircle(color = lineColor, radius = 7f, center = Offset(x(t), y(v)))
                drawCircle(color = Color.White, radius = 3f, center = Offset(x(t), y(v)))
            }
        }
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(formatShortDay(first.first), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.weight(1f))
            Text(formatShortDay(last.first), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
