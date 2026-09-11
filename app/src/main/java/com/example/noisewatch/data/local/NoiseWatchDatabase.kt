package com.example.noisewatch.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [IncidentEntity::class],
    version = 2,
    exportSchema = false
)
abstract class NoiseWatchDatabase : RoomDatabase() {

    abstract fun incidentDao(): IncidentDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE incidents ADD COLUMN locationAccuracyMeters REAL")
            }
        }

        @Volatile
        private var INSTANCE: NoiseWatchDatabase? = null

        fun getInstance(context: Context): NoiseWatchDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    NoiseWatchDatabase::class.java,
                    "noisewatch.db"
                )
                .addMigrations(MIGRATION_1_2)
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
