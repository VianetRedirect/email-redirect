package com.vianet.workoutlog.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [Exercise::class, Template::class, TemplateExercise::class, Workout::class, WorkoutExercise::class, SetEntry::class, Achievement::class],
    version = 1,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun exerciseDao(): ExerciseDao
    abstract fun templateDao(): TemplateDao
    abstract fun workoutDao(): WorkoutDao
    abstract fun achievementDao(): AchievementDao

    companion object {
        fun build(context: Context): AppDatabase =
            Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "workout-log.db")
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build()
    }
}
