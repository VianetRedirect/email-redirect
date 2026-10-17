package com.vianet.workoutlog.ui.settings

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vianet.workoutlog.data.Settings
import com.vianet.workoutlog.domain.WeightUnit
import com.vianet.workoutlog.reminders.Reminders
import com.vianet.workoutlog.share.ShareApp
import com.vianet.workoutlog.ui.appViewModel
import com.vianet.workoutlog.ui.common.restLabel
import java.util.Locale

class SettingsViewModel(val settings: Settings) : ViewModel()

@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val viewModel: SettingsViewModel = appViewModel { SettingsViewModel(it.settings) }
    val settings = viewModel.settings
    val context = LocalContext.current
    val unit by settings.weightUnit.collectAsStateWithLifecycle()
    val rest by settings.defaultRestSeconds.collectAsStateWithLifecycle()
    val autoRest by settings.autoStartRest.collectAsStateWithLifecycle()
    val weeklyGoal by settings.weeklyGoal.collectAsStateWithLifecycle()
    val remindersOn by settings.remindersEnabled.collectAsStateWithLifecycle()
    val remindAfter by settings.remindAfterDays.collectAsStateWithLifecycle()
    val reminderHour by settings.reminderHour.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            // ---- Share ----
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Share the app", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Send the app itself to a friend's phone. They open the file to install it. No store or account needed.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            val ok = ShareApp.shareApk(context)
                            if (!ok) Toast.makeText(context, "Could not read the installed app file", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Send app")
                    }
                    OutlinedButton(onClick = { ShareApp.shareLink(context) }, modifier = Modifier.weight(1f)) {
                        Text("Share link")
                    }
                }
            }

            HorizontalDivider()

            // ---- Weekly goal ----
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Weekly goal", style = MaterialTheme.typography.titleMedium)
                Text(
                    "$weeklyGoal ${if (weeklyGoal == 1) "workout" else "workouts"} a week",
                    style = MaterialTheme.typography.headlineSmall,
                )
                Slider(
                    value = weeklyGoal.toFloat(),
                    onValueChange = { settings.setWeeklyGoal(it.toInt()) },
                    valueRange = 1f..7f,
                    steps = 5,
                )
                Text(
                    "A week counts towards your streak when you hit this many workouts, Monday to Sunday.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            HorizontalDivider()

            // ---- Reminders ----
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Workout reminders", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "A nudge when you have gone quiet, with a warning when your streak is about to end.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(
                        checked = remindersOn,
                        onCheckedChange = { on ->
                            settings.setRemindersEnabled(on)
                            if (on) Reminders.schedule(context, settings.reminderHour.value, replace = true) else Reminders.cancel(context)
                        },
                    )
                }
                if (remindersOn) {
                    Text(
                        "Remind me after $remindAfter ${if (remindAfter == 1) "day" else "days"} without a workout",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Slider(
                        value = remindAfter.toFloat(),
                        onValueChange = { settings.setRemindAfterDays(it.toInt()) },
                        valueRange = 1f..7f,
                        steps = 5,
                    )
                    Text(
                        "Check at ${String.format(Locale.UK, "%02d:00", reminderHour)}",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Slider(
                        value = reminderHour.toFloat(),
                        onValueChange = { settings.setReminderHour(it.toInt()) },
                        onValueChangeFinished = { Reminders.schedule(context, settings.reminderHour.value, replace = true) },
                        valueRange = 6f..22f,
                        steps = 15,
                    )
                }
            }

            HorizontalDivider()

            // ---- Units ----
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Weight unit", style = MaterialTheme.typography.titleMedium)
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    WeightUnit.entries.forEachIndexed { index, option ->
                        SegmentedButton(
                            selected = unit == option,
                            onClick = { settings.setWeightUnit(option) },
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = WeightUnit.entries.size),
                        ) { Text(option.label) }
                    }
                }
                Text(
                    "Weights are stored in kilograms and converted for display. Distance is always kilometres.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            HorizontalDivider()

            // ---- Rest ----
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Default rest between sets", style = MaterialTheme.typography.titleMedium)
                Text(restLabel(rest), style = MaterialTheme.typography.headlineSmall)
                Slider(
                    value = rest.toFloat(),
                    onValueChange = { settings.setDefaultRestSeconds((it / 15f).toInt() * 15) },
                    valueRange = 15f..300f,
                    steps = 18,
                )
                Text(
                    "Used for new exercises. Each exercise in a workout can override it.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Start rest timer automatically", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Begins the countdown when you tick a set.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(checked = autoRest, onCheckedChange = { settings.setAutoStartRest(it) })
            }

            HorizontalDivider()

            Text(
                "Workout Log keeps everything on this phone. Nothing is uploaded.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
