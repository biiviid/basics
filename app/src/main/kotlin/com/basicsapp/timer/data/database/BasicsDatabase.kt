package com.basicsapp.timer.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.basicsapp.timer.data.models.Session
import com.basicsapp.timer.data.models.Solve

@Database(entities = [Session::class, Solve::class], version = 4, exportSchema = false)
abstract class BasicsDatabase : RoomDatabase() {
    abstract fun sessionDao(): SessionDao
    abstract fun solveDao(): SolveDao

    companion object {
        @Volatile
        private var INSTANCE: BasicsDatabase? = null

        fun getDatabase(context: Context): BasicsDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    BasicsDatabase::class.java,
                    "basics_database"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
