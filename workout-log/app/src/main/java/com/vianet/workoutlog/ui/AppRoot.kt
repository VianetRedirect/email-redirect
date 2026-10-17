package com.vianet.workoutlog.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.vianet.workoutlog.AppContainer
import com.vianet.workoutlog.WorkoutLogApp
import com.vianet.workoutlog.ui.awards.AwardsScreen
import com.vianet.workoutlog.ui.exercises.ExerciseDetailScreen
import com.vianet.workoutlog.ui.exercises.ExercisesScreen
import com.vianet.workoutlog.ui.history.HistoryScreen
import com.vianet.workoutlog.ui.history.WorkoutDetailScreen
import com.vianet.workoutlog.ui.settings.SettingsScreen
import com.vianet.workoutlog.ui.start.StartScreen
import com.vianet.workoutlog.ui.template.TemplateEditorScreen
import com.vianet.workoutlog.ui.workout.ActiveWorkoutScreen

object Routes {
    const val START = "start"
    const val HISTORY = "history"
    const val EXERCISES = "exercises"
    const val AWARDS = "awards"
    const val SETTINGS = "settings"
    const val WORKOUT = "workout/{workoutId}"
    const val WORKOUT_DETAIL = "workoutDetail/{workoutId}"
    const val TEMPLATE = "template/{templateId}"
    const val EXERCISE_DETAIL = "exercise/{exerciseId}"

    fun workout(id: Long) = "workout/$id"
    fun workoutDetail(id: Long) = "workoutDetail/$id"
    fun template(id: Long?) = "template/${id ?: -1L}"
    fun exercise(id: Long) = "exercise/$id"
}

private data class Tab(val route: String, val label: String, val icon: ImageVector)

private val tabs = listOf(
    Tab(Routes.START, "Start", Icons.Default.PlayArrow),
    Tab(Routes.HISTORY, "History", Icons.Default.History),
    Tab(Routes.EXERCISES, "Exercises", Icons.Default.FitnessCenter),
    Tab(Routes.AWARDS, "Awards", Icons.Default.EmojiEvents),
)

/** Builds a ViewModel with access to the app container. [key] separates instances that take different ids. */
@Composable
inline fun <reified VM : ViewModel> appViewModel(key: String? = null, crossinline create: (AppContainer) -> VM): VM {
    val container = (LocalContext.current.applicationContext as WorkoutLogApp).container
    val factory = object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = create(container) as T
    }
    return viewModel(key = key, factory = factory)
}

@Composable
fun AppRoot() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    val bottomBar: @Composable () -> Unit = { AppBottomBar(navController = navController, currentRoute = currentRoute) }

    NavHost(navController = navController, startDestination = Routes.START) {
        composable(Routes.START) {
            StartScreen(
                bottomBar = bottomBar,
                onOpenWorkout = { id -> navController.navigate(Routes.workout(id)) },
                onEditTemplate = { id -> navController.navigate(Routes.template(id)) },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
            )
        }
        composable(Routes.HISTORY) {
            HistoryScreen(
                bottomBar = bottomBar,
                onOpenWorkout = { id -> navController.navigate(Routes.workoutDetail(id)) },
            )
        }
        composable(Routes.EXERCISES) {
            ExercisesScreen(
                bottomBar = bottomBar,
                onOpenExercise = { id -> navController.navigate(Routes.exercise(id)) },
            )
        }
        composable(Routes.AWARDS) {
            AwardsScreen(bottomBar = bottomBar)
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
        composable(
            route = Routes.WORKOUT,
            arguments = listOf(navArgument("workoutId") { type = NavType.LongType }),
        ) { entry ->
            val workoutId = entry.arguments?.getLong("workoutId") ?: 0L
            ActiveWorkoutScreen(
                workoutId = workoutId,
                onBack = { navController.popBackStack() },
                onWorkoutClosed = {
                    navController.navigate(Routes.START) {
                        popUpTo(Routes.START) { inclusive = true }
                        launchSingleTop = true
                    }
                },
            )
        }
        composable(
            route = Routes.WORKOUT_DETAIL,
            arguments = listOf(navArgument("workoutId") { type = NavType.LongType }),
        ) { entry ->
            val workoutId = entry.arguments?.getLong("workoutId") ?: 0L
            WorkoutDetailScreen(workoutId = workoutId, onBack = { navController.popBackStack() })
        }
        composable(
            route = Routes.TEMPLATE,
            arguments = listOf(navArgument("templateId") { type = NavType.LongType }),
        ) { entry ->
            val raw = entry.arguments?.getLong("templateId") ?: -1L
            TemplateEditorScreen(templateId = raw.takeIf { it >= 0 }, onDone = { navController.popBackStack() })
        }
        composable(
            route = Routes.EXERCISE_DETAIL,
            arguments = listOf(navArgument("exerciseId") { type = NavType.LongType }),
        ) { entry ->
            val exerciseId = entry.arguments?.getLong("exerciseId") ?: 0L
            ExerciseDetailScreen(exerciseId = exerciseId, onBack = { navController.popBackStack() })
        }
    }
}

@Composable
private fun AppBottomBar(navController: NavHostController, currentRoute: String?) {
    NavigationBar {
        tabs.forEach { tab ->
            NavigationBarItem(
                selected = currentRoute == tab.route,
                onClick = {
                    if (currentRoute != tab.route) {
                        navController.navigate(tab.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                icon = { Icon(tab.icon, contentDescription = null) },
                label = { Text(tab.label) },
            )
        }
    }
}
