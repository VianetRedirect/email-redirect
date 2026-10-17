package com.vianet.workoutlog.ui.awards

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vianet.workoutlog.domain.Milestone
import com.vianet.workoutlog.domain.MilestoneType
import com.vianet.workoutlog.domain.Milestones
import com.vianet.workoutlog.domain.ProgressStats
import com.vianet.workoutlog.domain.StreakInfo
import com.vianet.workoutlog.domain.formatMilestoneValue
import com.vianet.workoutlog.domain.formatShortDay
import com.vianet.workoutlog.domain.isReached
import com.vianet.workoutlog.domain.progress
import com.vianet.workoutlog.ui.appViewModel
import com.vianet.workoutlog.ui.common.StatTile

@Composable
fun AwardsScreen(bottomBar: @Composable () -> Unit) {
    val viewModel: AwardsViewModel = appViewModel { AwardsViewModel(it.repository, it.settings) }
    val ui by viewModel.ui.collectAsStateWithLifecycle()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Awards") }) },
        bottomBar = bottomBar,
    ) { padding ->
        val current = ui
        if (current == null) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }
        val reached = Milestones.all.filter { it.isReached(current.stats) }
        val locked = Milestones.all.filter { !it.isReached(current.stats) }
        val next = Milestones.nextOfEachType(current.stats)

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(key = "streak") { StreakCard(current.streak) }
            item(key = "totals") { TotalsRow(current.stats) }
            if (next.isNotEmpty()) {
                item(key = "next-header") { SectionTitle("Next up") }
                next.forEach { milestone ->
                    item(key = "next-${milestone.key}") { NextMilestoneCard(milestone, current.stats) }
                }
            }
            item(key = "unlocked-header") { SectionTitle("Unlocked (${reached.size} of ${Milestones.all.size})") }
            if (reached.isEmpty()) {
                item(key = "none") {
                    Text(
                        "Finish a workout to unlock your first award.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            reached.sortedByDescending { current.achievedAt[it.key] ?: 0L }.forEach { milestone ->
                item(key = "done-${milestone.key}") {
                    MilestoneRow(milestone = milestone, achievedAt = current.achievedAt[milestone.key], locked = false)
                }
            }
            item(key = "locked-header") { SectionTitle("Still to come") }
            locked.forEach { milestone ->
                item(key = "locked-${milestone.key}") {
                    MilestoneRow(milestone = milestone, achievedAt = null, locked = true)
                }
            }
            item(key = "spacer") { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
}

@Composable
private fun StreakCard(streak: StreakInfo) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("🔥", fontSize = 40.sp)
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "${streak.currentWeeks}-week streak",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    Text(
                        "Best: ${streak.bestWeeks} ${if (streak.bestWeeks == 1) "week" else "weeks"}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(
                "This week: ${streak.thisWeekCount} of ${streak.weeklyGoal} workouts",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(streak.weeklyGoal) { i ->
                    val done = i < streak.thisWeekCount
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .background(
                                if (done) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f),
                                CircleShape,
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (done) Text("✓", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            val message = when {
                streak.thisWeekGoalMet -> "Goal hit. Anything more this week is a bonus."
                streak.atRisk -> "${streak.workoutsStillNeeded} more in the next ${streak.daysLeftThisWeek + 1} days keeps the streak."
                streak.currentWeeks == 0 && streak.thisWeekCount > 0 -> "Hit the goal this week to start a streak."
                else -> "Weeks count when you hit your goal. Change the goal in Settings."
            }
            Text(message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
        }
    }
}

@Composable
private fun TotalsRow(stats: ProgressStats) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        StatTile(label = "Lifted", value = formatMilestoneValue(MilestoneType.VOLUME, stats.totalVolumeKg), modifier = Modifier.weight(1f))
        StatTile(label = "Distance", value = formatMilestoneValue(MilestoneType.DISTANCE, stats.totalDistanceKm), modifier = Modifier.weight(1f))
        StatTile(label = "Workouts", value = stats.workoutCount.toString(), modifier = Modifier.weight(1f))
    }
}

@Composable
private fun NextMilestoneCard(milestone: Milestone, stats: ProgressStats) {
    val value = stats.valueFor(milestone.type)
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(milestone.emoji, fontSize = 28.sp)
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(milestone.title, style = MaterialTheme.typography.titleMedium)
                    Text(
                        "${formatMilestoneValue(milestone.type, value)} of ${formatMilestoneValue(milestone.type, milestone.threshold)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    "${(milestone.progress(stats) * 100).toInt()}%",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Spacer(Modifier.height(10.dp))
            LinearProgressIndicator(progress = { milestone.progress(stats) }, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun MilestoneRow(milestone: Milestone, achievedAt: Long?, locked: Boolean) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (locked) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(44.dp).background(
                    if (locked) Color.Transparent else MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                    CircleShape,
                ),
                contentAlignment = Alignment.Center,
            ) {
                Text(if (locked) "🔒" else milestone.emoji, fontSize = 22.sp, textAlign = TextAlign.Center)
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    milestone.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = if (locked) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    if (locked) "${milestone.type.label}: ${formatMilestoneValue(milestone.type, milestone.threshold)}" else milestone.detail,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (achievedAt != null) {
                    Text(
                        "Unlocked ${formatShortDay(achievedAt)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}
