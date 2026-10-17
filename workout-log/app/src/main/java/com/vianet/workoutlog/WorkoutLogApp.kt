package com.vianet.workoutlog

import android.app.Application
import com.vianet.workoutlog.data.Settings
import com.vianet.workoutlog.data.WorkoutRepository
import com.vianet.workoutlog.data.db.AppDatabase
import com.vianet.workoutlog.reminders.Reminders
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** Hand-rolled dependency container: one database, one repository, one settings store. */
class AppContainer(app: Application) {
    val database: AppDatabase = AppDatabase.build(app)
    val repository: WorkoutRepository = WorkoutRepository(database)
    val settings: Settings = Settings(app)
}

class WorkoutLogApp : Application() {
    lateinit var container: AppContainer
        private set

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        appScope.launch { container.repository.seedIfEmpty() }
        Reminders.createChannel(this)
        if (container.settings.remindersEnabled.value) {
            Reminders.schedule(this, container.settings.reminderHour.value, replace = false)
        }
    }
}
