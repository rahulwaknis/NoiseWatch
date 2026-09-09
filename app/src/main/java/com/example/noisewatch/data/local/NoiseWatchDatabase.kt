package com.example.noisewatch.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [IncidentEntity::class],
    version = 1,
    exportSchema = false
)
abstract class NoiseWatchDatabase : RoomDatabase() {

    abstract fun incidentDao(): IncidentDao

    companion object {
        @Volatile
        private var INSTANCE: NoiseWatchDatabase? = null

        fun getInstance(context: Context): NoiseWatchDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    NoiseWatchDatabase::class.java,
                    "noisewatch.db"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
