package com.basicsapp.timer.di

import android.content.Context
import com.basicsapp.timer.data.database.BasicsDatabase
import com.basicsapp.timer.data.database.SessionDao
import com.basicsapp.timer.data.database.SolveDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): BasicsDatabase {
        return BasicsDatabase.getDatabase(context)
    }

    @Provides
    fun provideSessionDao(database: BasicsDatabase): SessionDao {
        return database.sessionDao()
    }

    @Provides
    fun provideSolveDao(database: BasicsDatabase): SolveDao {
        return database.solveDao()
    }
}
