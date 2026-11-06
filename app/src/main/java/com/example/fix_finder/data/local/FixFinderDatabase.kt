package com.example.fix_finder.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [LocalReportEntity::class], version = 2, exportSchema = false)
abstract class FixFinderDatabase : RoomDatabase() {

    abstract fun reportDao(): ReportDao

    companion object {
        @Volatile
        private var INSTANCE: FixFinderDatabase? = null

        fun getInstance(context: Context): FixFinderDatabase? {
            return INSTANCE ?: synchronized(this) {
                try {
                    val instance = Room.databaseBuilder(
                        context.applicationContext,
                        FixFinderDatabase::class.java,
                        "fix_finder_db"
                    ).fallbackToDestructiveMigration().build()
                    INSTANCE = instance
                    instance
                } catch (_: Throwable) {
                    null
                }
            }
        }
    }
}
