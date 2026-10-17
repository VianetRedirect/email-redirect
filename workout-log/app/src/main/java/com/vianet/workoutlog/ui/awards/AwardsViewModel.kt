package com.vianet.workoutlog.ui.awards

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vianet.workoutlog.data.Settings
import com.vianet.workoutlog.data.WorkoutRepository
import com.vianet.workoutlog.data.db.Achievement
import com.vianet.workoutlog.domain.ProgressStats
import com.vianet.workoutlog.domain.StreakInfo
import com.vianet.workoutlog.domain.computeStreak
import com.vianet.workoutlog.domain.epochMillisToDate
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate

data class AwardsUi(
    val streak: StreakInfo,
    val stats: ProgressStats,
    val achievedAt: Map<String, Long>,
)

class AwardsViewModel(repository: WorkoutRepository, settings: Settings) : ViewModel() {

    private val streakFlow = combine(repository.observeWorkoutStartTimes(), settings.weeklyGoal) { times, goal ->
        computeStreak(times.map { epochMillisToDate(it) }, goal, LocalDate.now())
    }

    // Totals are re-read whenever the workout list changes.
    private val statsFlow = combine(repository.observeWorkoutStartTimes(), streakFlow) { _, streak ->
        repository.progressStats(streak.weeklyGoal).copy(streakWeeks = streak.currentWeeks)
    }

    private val achievementsFlow = repository.observeAchievements()
        .map { list: List<Achievement> -> list.associate { it.key to it.achievedAt } }

    val ui: StateFlow<AwardsUi?> = combine(streakFlow, statsFlow, achievementsFlow) { streak, stats, achieved ->
        AwardsUi(streak = streak, stats = stats, achievedAt = achieved)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}
